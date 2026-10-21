/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfprofile;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile: tap cycles Power save > Efficiency > Balanced > Performance. */
public class PerfProfileTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        update();
    }

    @Override
    public void onClick() {
        super.onClick();
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
