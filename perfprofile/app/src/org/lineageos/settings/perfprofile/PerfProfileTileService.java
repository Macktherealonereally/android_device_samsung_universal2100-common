/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfprofile;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Quick Settings tile: tap cycles Power save > Efficiency > Balanced > Performance.
 * On a locked device the tile only shows the current profile; changing it asks
 * for the unlock first.
 */
public class PerfProfileTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        update();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (isLocked()) {
            // Runs after the user unlocks; dropped if the bouncer is dismissed.
            unlockAndRun(this::cycleProfile);
        } else {
            cycleProfile();
        }
    }

    private void cycleProfile() {
        PerfProfileUtils.set(this, PerfProfileUtils.next(PerfProfileUtils.get()));
        update();
    }

    private void update() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        int profile = PerfProfileUtils.get();
        tile.setState(profile == PerfProfileUtils.BALANCED ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.setSubtitle(getString(PerfProfileUtils.titleRes(profile)));
        tile.updateTile();
    }
}
