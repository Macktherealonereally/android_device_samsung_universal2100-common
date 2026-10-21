/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.perfprofile;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class PerfProfileFragment extends SettingsBasePreferenceFragment
        implements SelectorWithWidgetPreference.OnClickListener {

    private static final String KEY_PREFIX = "perf_profile_";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getPreferenceManager().getContext();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        for (int profile : PerfProfileUtils.ORDER) {
            SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(context);
            pref.setKey(KEY_PREFIX + profile);
            pref.setTitle(PerfProfileUtils.titleRes(profile));
            pref.setSummary(PerfProfileUtils.summaryRes(profile));
            pref.setPersistent(false);
            pref.setOnClickListener(this);
            screen.addPreference(pref);
        }

        FooterPreference footer = new FooterPreference(context);
        footer.setTitle(R.string.perf_profile_footer);
        screen.addPreference(footer);

        setPreferenceScreen(screen);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateChecked();
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference emitter) {
        for (int profile : PerfProfileUtils.ORDER) {
            if ((KEY_PREFIX + profile).equals(emitter.getKey())) {
                PerfProfileUtils.set(requireContext(), profile);
                break;
            }
        }
        updateChecked();
    }

    private void updateChecked() {
        int current = PerfProfileUtils.get();
        for (int profile : PerfProfileUtils.ORDER) {
            SelectorWithWidgetPreference pref = findPreference(KEY_PREFIX + profile);
            if (pref != null) {
                pref.setChecked(profile == current);
            }
        }
    }
}
