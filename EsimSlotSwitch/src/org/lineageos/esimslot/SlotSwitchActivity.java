/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.esimslot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * Lets the user pick what the second SIM interface uses: SIM tray 2 or the eUICC.
 *
 * <p>The platform only knows two physical slots on this RIL, so LPAs such as OpenEUICC cannot
 * offer the eUICC while the mux is on tray 2 (it is not visible then). This sends the same
 * setSimSlotMapping request One UI's SIM manager sends.
 */
public class SlotSwitchActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Mux.isSupported()) {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (Mux.isBusy()) {
            Toast.makeText(this, R.string.switch_pending, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int current = Mux.isOnEuicc() ? 1 : (Mux.isOnTray() ? 0 : -1);
        CharSequence[] choices = {getText(R.string.choice_psim), getText(R.string.choice_esim)};
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(R.string.tile_title)
                .setSingleChoiceItems(choices, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != current) {
                        select(which == 1);
                    } else {
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void select(boolean euicc) {
        // Also kept by the app, in case the shim could not record the choice.
        RestoreJobService.onUserChoice(this, euicc);
        Toast.makeText(this, R.string.switching, Toast.LENGTH_SHORT).show();
        // The request blocks until the RIL answers (a few seconds); don't hold the screen.
        final Context app = getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        finish();
        new Thread(() -> {
            int msg;
            try {
                msg = Mux.trySelect(app, euicc) ? R.string.switch_done : R.string.switch_pending;
            } catch (RuntimeException e) {
                Log.e(Mux.TAG, "switch to " + (euicc ? "eUICC" : "tray 2") + " failed", e);
                msg = R.string.switch_failed;
            }
            final int text = msg;
            main.post(() -> Toast.makeText(app, text, Toast.LENGTH_SHORT).show());
        }).start();
    }
}
