/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esimslot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-checks the mux after boot and after every SIM card state change (hot swap, modem reset). */
public class RestoreReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Mux.isSupported()) {
            return;
        }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            RestoreJobService.resetForBoot(context);
        }
        RestoreJobService.schedule(context, RestoreJobService.SETTLE_MS);
    }
}
