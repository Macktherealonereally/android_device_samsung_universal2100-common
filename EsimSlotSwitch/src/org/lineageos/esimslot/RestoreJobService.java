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
 *   <li>the mux is on tray 2 and tray 2 is empty, so a SIM card the user put in tray 2 always
 *       wins;</li>
 *   <li>that state held for a while: {@link #SETTLE_MS} with SIM 1 present, much longer with
 *       SIM 1 absent too, because then the tray may just be out for a SIM change;</li>
 *   <li>no call is active, and fewer than {@link #MAX_ATTEMPTS} attempts failed in a row.</li>
 * </ul>
 */
public class RestoreJobService extends JobService {
    private static final int JOB_ID = 1;

    static final long SETTLE_MS = 10_000;
    private static final long TRAY_OUT_MS = 90_000;
    private static final long VERIFY_MS = 30_000;
    private static final int MAX_ATTEMPTS = 3;

    private static final String PREF_PENDING_SINCE = "pending_since";
    private static final String PREF_ATTEMPTS = "attempts";
    private static final String PREF_SEEN_EUICC = "seen_euicc";

    static void schedule(Context context, long delayMs) {
        JobInfo job = new JobInfo.Builder(JOB_ID,
                new ComponentName(context, RestoreJobService.class))
                .setMinimumLatency(delayMs)
                .setOverrideDeadline(delayMs + 10_000)
                .build();
        // Replaces a pending check, so a burst of SIM events gives one check.
        context.getSystemService(JobScheduler.class).schedule(job);
    }

    static void resetForBoot(Context context) {
        prefs(context).edit().remove(PREF_PENDING_SINCE).remove(PREF_ATTEMPTS).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("restore", Context.MODE_PRIVATE);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // select() blocks until the RIL answers; keep it off the main thread.
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
        TelephonyManager tm = getSystemService(TelephonyManager.class);
        int muxSimState = tm.getSimState(Mux.MUX_SLOT);
        String target = Mux.userTarget();

        if (Mux.isOnEuicc()) {
            p.edit().putBoolean(PREF_SEEN_EUICC, true)
                    .remove(PREF_PENDING_SINCE).remove(PREF_ATTEMPTS).apply();
            return;
        }
        if (target.isEmpty() && Mux.isOnTray() && isCardPresent(muxSimState)) {
            // A SIM card in tray 2 is in use; don't treat the eUICC as the user's choice.
            p.edit().putBoolean(PREF_SEEN_EUICC, false).apply();
        }

        boolean wantEuicc = "esim".equals(target)
                || (target.isEmpty() && p.getBoolean(PREF_SEEN_EUICC, false));
        if (!wantEuicc || !Mux.isOnTray()
                || muxSimState != TelephonyManager.SIM_STATE_ABSENT) {
            p.edit().remove(PREF_PENDING_SINCE).apply();
            return;
        }

        TelecomManager telecom = getSystemService(TelecomManager.class);
        if (telecom != null && telecom.isInCall()) {
            schedule(this, SETTLE_MS);
            return;
        }

        long now = SystemClock.elapsedRealtime();
        long since = p.getLong(PREF_PENDING_SINCE, 0);
        if (since == 0 || since > now) {
            since = now;
            p.edit().putLong(PREF_PENDING_SINCE, now).apply();
        }
        boolean trayIn = tm.getSimState(0) != TelephonyManager.SIM_STATE_ABSENT;
        long needed = trayIn ? SETTLE_MS : TRAY_OUT_MS;
        if (now - since < needed) {
            schedule(this, needed - (now - since));
            return;
        }

        int attempts = p.getInt(PREF_ATTEMPTS, 0);
        if (attempts >= MAX_ATTEMPTS) {
            Log.w(Mux.TAG, "eUICC not selected after " + attempts + " attempts; giving up");
            return;
        }
        p.edit().putInt(PREF_ATTEMPTS, attempts + 1).remove(PREF_PENDING_SINCE).apply();

        Log.i(Mux.TAG, "SIM tray 2 is empty and the eUICC is not selected (user target '"
                + target + "'); selecting the eUICC again, attempt " + (attempts + 1));
        try {
            Mux.select(this, true);
        } catch (RuntimeException e) {
            Log.e(Mux.TAG, "selecting the eUICC failed", e);
        }
        schedule(this, VERIFY_MS);
    }

    private static boolean isCardPresent(int simState) {
        return simState != TelephonyManager.SIM_STATE_ABSENT
                && simState != TelephonyManager.SIM_STATE_UNKNOWN
                && simState != TelephonyManager.SIM_STATE_NOT_READY;
    }
}
