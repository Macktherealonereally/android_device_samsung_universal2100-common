/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esimslot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

/** Re-checks the mux after boot and after every SIM card state change (hot swap, modem reset). */
public class RestoreReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !TelephonyManager.ACTION_SIM_CARD_STATE_CHANGED.equals(action)) {
            return;
        }
        if (!Mux.isSupported()) {
            return;
        }
        RestoreJobService.onSimEvent(context);
        RestoreJobService.schedule(context, RestoreJobService.SETTLE_MS);
    }
}
