/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esimslot;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotMapping;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The tsds2 SIM2 / eSIM mux.
 *
 * <p>SIM tray 2 and the eUICC share the second SIM interface (logical slot 1). The Samsung
 * RIL reports the selection in ril.simslottype2 (1 = eUICC, 0 = tray). Switching goes through
 * setSimSlotMapping with One UI's layout, which libsec-ril-slotswitch turns into the modem's
 * slot switch; the shim also records the requested target in persist.ril.esim.user_target.
 */
final class Mux {
    static final String TAG = "EsimSlotSwitch";

    private static final String PROP_SLOTSWITCH = "persist.radio.esim.slotswitch";
    private static final String PROP_SLOT_TYPE = "ril.simslottype2";
    private static final String PROP_USER_TARGET = "persist.ril.esim.user_target";

    /** Logical slot (phone id) behind the mux. */
    static final int MUX_SLOT = 1;

    /** One UI's physical slot numbers in setSimSlotMapping. */
    private static final int PHYS_TRAY2 = 1;
    private static final int PHYS_EUICC = 2;

    // One switch at a time in this process (restore job and Settings entry). The modem runs
    // one switch state machine per SIM interface; a second request while one is pending
    // must never reach the RIL.
    private static final AtomicBoolean sBusy = new AtomicBoolean();
    private static volatile long sLastRequestMs = -1;

    private Mux() {}

    /**
     * True on a tsds2 device, in the system user. Secondary users and work profiles get their
     * own copy of this app; only one copy may ever switch.
     */
    static boolean isSupported() {
        return Process.myUserHandle().isSystem()
                && "tsds2".equals(SystemProperties.get(PROP_SLOTSWITCH));
    }

    static boolean isOnEuicc() {
        return "1".equals(SystemProperties.get(PROP_SLOT_TYPE));
    }

    static boolean isOnTray() {
        return "0".equals(SystemProperties.get(PROP_SLOT_TYPE));
    }

    /** "esim", "psim" or "" when nothing was switched since the last data wipe. */
    static String userTarget() {
        return SystemProperties.get(PROP_USER_TARGET);
    }

    static boolean isBusy() {
        return sBusy.get();
    }

    /** Milliseconds since the last switch request from this process ended, or -1. */
    static long msSinceLastRequest() {
        long last = sLastRequestMs;
        return last < 0 ? -1 : SystemClock.elapsedRealtime() - last;
    }

    /**
     * Sends the switch request and blocks until the RIL has answered (a few seconds).
     *
     * @return false, without sending anything, while another request is still pending
     * @throws RuntimeException when the switch is refused
     */
    static boolean trySelect(Context context, boolean euicc) {
        if (!sBusy.compareAndSet(false, true)) {
            return false;
        }
        try {
            TelephonyManager tm = context.getSystemService(TelephonyManager.class);
            tm.setSimSlotMapping(List.of(
                    new UiccSlotMapping(0, 0, 0),
                    new UiccSlotMapping(0, euicc ? PHYS_EUICC : PHYS_TRAY2, MUX_SLOT)));
            return true;
        } finally {
            sLastRequestMs = SystemClock.elapsedRealtime();
            sBusy.set(false);
        }
    }
}
