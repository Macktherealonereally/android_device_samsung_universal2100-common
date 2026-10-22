/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esimslot;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Puts the mux back on the eUICC when the modem fell back to an empty SIM tray 2.
 *
 * <p>After a SIM hot swap or a modem restart the RIL can leave the second SIM interface on
 * tray 2 although the user chose the eSIM; the eUICC then disappears until it is selected
 * again. This only ever switches towards the eUICC, and only when all of these hold:
 * <ul>
 *   <li>the last switch requested through the shim was "esim" (or, when nothing was switched
 *       since the last data wipe, the mux was last seen on the eUICC);</li>
 *   <li>no SIM card was seen in use in tray 2 since the eUICC was last selected. A tray-2
 *       card always wins: once one is seen, nothing is restored until the eUICC is selected
 *       again (by the user, or by whatever else switches the mux), even if the card is
 *       removed later or is slow to come back after a modem restart;</li>
 *   <li>the mux is on tray 2, tray 2 reads ABSENT, and nothing changed for
 *       {@link #STABLE_MS} after the last SIM event ({@link #STABLE_TRAY_OUT_MS} while SIM 1
 *       is absent too, as the tray may just be out for a SIM change);</li>
 *   <li>airplane mode is off and the radio is on, no call is active, no other switch is
 *       pending, and fewer than {@link #MAX_RESTORES_PER_BOOT} restores ran in this boot.</li>
 * </ul>
 */
public class RestoreJobService extends JobService {
    private static final String JOB_NAMESPACE = "org.lineageos.esimslot";
    private static final int JOB_ID = 0x45534d31; // "ESM1"

    static final long SETTLE_MS = 10_000;
    private static final long STABLE_MS = 60_000;
    private static final long STABLE_TRAY_OUT_MS = 180_000;
    private static final long VERIFY_MS = 30_000;
    private static final long IN_CALL_RECHECK_MS = 60_000;
    private static final long RADIO_OFF_RECHECK_MS = 120_000;
    private static final long MIN_GAP_MS = 60_000;
    private static final int MAX_RESTORES_PER_BOOT = 3;

    private static final String PREF_BOOT = "boot_count";
    private static final String PREF_LAST_EVENT = "last_event";
    private static final String PREF_RESTORES = "restores";
    private static final String PREF_SEEN_EUICC = "seen_euicc";
    private static final String PREF_TRAY_IN_USE = "tray_in_use";

    static void schedule(Context context, long delayMs) {
        JobInfo job = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, RestoreJobService.class))
                .setMinimumLatency(delayMs)
                .setOverrideDeadline(delayMs + 10_000)
                .build();
        // Replaces a pending check, so a burst of SIM events gives one check.
        jobScheduler(context).schedule(job);
    }

    private static JobScheduler jobScheduler(Context context) {
        return context.getSystemService(JobScheduler.class).forNamespace(JOB_NAMESPACE);
    }

    /** A SIM event: the stability window starts again. */
    static void onSimEvent(Context context) {
        prefs(context).edit().putLong(PREF_LAST_EVENT, SystemClock.elapsedRealtime()).apply();
    }

    /** An explicit choice in the Settings entry. */
    static void onUserChoice(Context context, boolean euicc) {
        prefs(context).edit().putBoolean(PREF_TRAY_IN_USE, !euicc).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("restore", Context.MODE_PRIVATE);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // trySelect() blocks until the RIL answers; keep it off the main thread.
        new Thread(() -> {
            try {
                check();
            } catch (RuntimeException e) {
                Log.e(Mux.TAG, "check failed", e);
            } finally {
                jobFinished(params, false);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private void check() {
        if (!Mux.isSupported()) {
            return;
        }
        SharedPreferences p = prefs(this);
        long now = SystemClock.elapsedRealtime();

        int boot = Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, 0);
        if (p.getInt(PREF_BOOT, -1) != boot) {
            p.edit().putInt(PREF_BOOT, boot).putLong(PREF_LAST_EVENT, now)
                    .remove(PREF_RESTORES).apply();
        }

        TelephonyManager tm = getSystemService(TelephonyManager.class);
        int muxSimState = tm.getSimState(Mux.MUX_SLOT);
        String target = Mux.userTarget();

        if (Mux.isOnEuicc()) {
            // Selected again since any tray-2 card was seen: the eSIM choice is armed.
            p.edit().putBoolean(PREF_SEEN_EUICC, true).putBoolean(PREF_TRAY_IN_USE, false)
                    .apply();
            return;
        }
        if (Mux.isOnTray() && isCardSeen(muxSimState)) {
            if (!p.getBoolean(PREF_TRAY_IN_USE, false)) {
                Log.i(Mux.TAG, "SIM card in tray 2 in use; not restoring the eUICC until it"
                        + " is selected again");
            }
            p.edit().putBoolean(PREF_TRAY_IN_USE, true).putBoolean(PREF_SEEN_EUICC, false)
                    .apply();
            return;
        }

        boolean wantEuicc = ("esim".equals(target)
                || (target.isEmpty() && p.getBoolean(PREF_SEEN_EUICC, false)))
                && !p.getBoolean(PREF_TRAY_IN_USE, false);
        if (!wantEuicc || !Mux.isOnTray()
                || muxSimState != TelephonyManager.SIM_STATE_ABSENT) {
            return;
        }

        if (Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0
                || tm.getRadioPowerState() != TelephonyManager.RADIO_POWER_ON) {
            // Card states mean little with the radio off; start the window again after it.
            p.edit().putLong(PREF_LAST_EVENT, now).apply();
            schedule(this, RADIO_OFF_RECHECK_MS);
            return;
        }

        TelecomManager telecom = getSystemService(TelecomManager.class);
        if (telecom != null && telecom.isInCall()) {
            schedule(this, IN_CALL_RECHECK_MS);
            return;
        }

        long gap = Mux.msSinceLastRequest();
        if (Mux.isBusy() || (gap >= 0 && gap < MIN_GAP_MS)) {
            schedule(this, VERIFY_MS);
            return;
        }

        boolean trayIn = tm.getSimState(0) != TelephonyManager.SIM_STATE_ABSENT;
        long needed = trayIn ? STABLE_MS : STABLE_TRAY_OUT_MS;
        long since = now - p.getLong(PREF_LAST_EVENT, now);
        if (since < 0) {
            since = 0;
        }
        if (since < needed) {
            schedule(this, needed - since);
            return;
        }

        int restores = p.getInt(PREF_RESTORES, 0);
        if (restores >= MAX_RESTORES_PER_BOOT) {
            if (restores == MAX_RESTORES_PER_BOOT) {
                Log.w(Mux.TAG, "eUICC restored " + restores + " times in this boot; not"
                        + " restoring again until the next boot");
                p.edit().putInt(PREF_RESTORES, restores + 1).apply();
            }
            return;
        }
        p.edit().putInt(PREF_RESTORES, restores + 1).putLong(PREF_LAST_EVENT, now).apply();

        Log.i(Mux.TAG, "SIM tray 2 is empty and the eUICC is not selected (user target '"
                + target + "'); selecting the eUICC again, restore " + (restores + 1) + "/"
                + MAX_RESTORES_PER_BOOT + " in this boot");
        try {
            if (!Mux.trySelect(this, true)) {
                Log.i(Mux.TAG, "another switch is pending; checking again later");
            }
        } catch (RuntimeException e) {
            Log.e(Mux.TAG, "selecting the eUICC failed", e);
        }
        schedule(this, VERIFY_MS);
    }

    /** A card that is there but maybe not usable yet: anything but absent or unknown. */
    private static boolean isCardSeen(int simState) {
        return simState != TelephonyManager.SIM_STATE_ABSENT
                && simState != TelephonyManager.SIM_STATE_UNKNOWN
                && simState != TelephonyManager.SIM_STATE_NOT_READY;
    }
}
