/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfprofile;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.SystemProperties;
import android.service.quicksettings.TileService;

/**
 * The selected profile lives in persist.sys.perf_profile. Changing it makes
 * init run /vendor/bin/perf-profile, which applies it to the power HAL.
 * Numbering follows the old LineageOS PerformanceManager.
 */
public final class PerfProfileUtils {

    static final String PROP = "persist.sys.perf_profile";

    static final int POWER_SAVE = 0;
    static final int BALANCED = 1;
    static final int PERFORMANCE = 2;
    static final int EFFICIENCY = 3;

    /** From the most to the least power saving; also the QS tile cycle order. */
    static final int[] ORDER = { POWER_SAVE, EFFICIENCY, BALANCED, PERFORMANCE };

    static final Uri SUMMARY_URI =
            Uri.parse("content://org.lineageos.settings.perfprofile.summary/getDynamicSummary");

    private PerfProfileUtils() {}

    static int get() {
        int profile = SystemProperties.getInt(PROP, BALANCED);
        return isValid(profile) ? profile : BALANCED;
    }

    static void set(Context context, int profile) {
        if (!isValid(profile) || profile == get()) {
            return;
        }
        SystemProperties.set(PROP, Integer.toString(profile));
        context.getContentResolver().notifyChange(SUMMARY_URI, null);
        TileService.requestListeningState(context,
                new ComponentName(context, PerfProfileTileService.class));
    }

    static int next(int profile) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == profile) {
                return ORDER[(i + 1) % ORDER.length];
            }
        }
        return BALANCED;
    }

    static int titleRes(int profile) {
        switch (profile) {
            case POWER_SAVE:
                return R.string.perf_profile_power_save;
            case PERFORMANCE:
                return R.string.perf_profile_performance;
            case EFFICIENCY:
                return R.string.perf_profile_efficiency;
            default:
                return R.string.perf_profile_balanced;
        }
    }

    static int summaryRes(int profile) {
        switch (profile) {
            case POWER_SAVE:
                return R.string.perf_profile_power_save_summary;
            case PERFORMANCE:
                return R.string.perf_profile_performance_summary;
            case EFFICIENCY:
                return R.string.perf_profile_efficiency_summary;
            default:
                return R.string.perf_profile_balanced_summary;
        }
    }

    private static boolean isValid(int profile) {
        return profile >= POWER_SAVE && profile <= EFFICIENCY;
    }
}
