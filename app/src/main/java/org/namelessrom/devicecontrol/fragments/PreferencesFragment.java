/*
 *  Copyright (C) 2013 - 2014 Alexander "Evisceration" Martinz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.namelessrom.devicecontrol.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.negusoft.holoaccent.dialog.AccentAlertDialog;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.Logger;
import org.namelessrom.devicecontrol.MainActivity;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.services.BootUpService;
import org.namelessrom.devicecontrol.ui.preferences.CustomCheckBoxPreference;
import org.namelessrom.devicecontrol.ui.preferences.CustomPreference;
import org.namelessrom.devicecontrol.ui.views.AttachPreferenceFragment;
import org.namelessrom.devicecontrol.utils.AppHelper;
import org.namelessrom.devicecontrol.utils.PreferenceHelper;
import org.namelessrom.devicecontrol.utils.Scripts;
import org.namelessrom.devicecontrol.utils.Utils;
import org.namelessrom.devicecontrol.utils.constants.DeviceConstants;
import org.namelessrom.devicecontrol.utils.constants.PerformanceConstants;

import java.util.ArrayList;

public class PreferencesFragment extends AttachPreferenceFragment
        implements Preference.OnPreferenceChangeListener, DeviceConstants, PerformanceConstants {

    //==============================================================================================
    // App
    //==============================================================================================
    private CustomPreference         mColorPreference;
    private CustomCheckBoxPreference mMonkeyPref;

    //==============================================================================================
    // Tools
    //==============================================================================================
    private CustomPreference mFlasherConfig;

    //==============================================================================================
    // General
    //==============================================================================================
    private CustomPreference         mSetOnBoot;
    private CustomCheckBoxPreference mShowLauncher;
    private CustomCheckBoxPreference mSkipChecks;

    //==============================================================================================
    // Interface
    //==============================================================================================
    private CustomCheckBoxPreference mSwipeOnContent;
    private CustomCheckBoxPreference mDarkTheme;

    //==============================================================================================
    // Debug
    //==============================================================================================
    private CustomCheckBoxPreference mExtensiveLogging;

    @Override protected int getFragmentId() { return ID_PREFERENCES; }

    @Override public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml._device_control);

        mExtensiveLogging = (CustomCheckBoxPreference) findPreference(EXTENSIVE_LOGGING);
        if (mExtensiveLogging != null) {
            mExtensiveLogging.setChecked(PreferenceHelper.getBoolean(EXTENSIVE_LOGGING));
            mExtensiveLogging.setOnPreferenceChangeListener(this);
        }

        PreferenceCategory category = (PreferenceCategory) findPreference("prefs_general");
        mSetOnBoot = (CustomPreference) findPreference("prefs_set_on_boot");

        mShowLauncher = (CustomCheckBoxPreference) findPreference(SHOW_LAUNCHER);
        if (mShowLauncher != null) {
            if (Application.IS_NAMELESS) {
                mShowLauncher.setChecked(PreferenceHelper.getBoolean(SHOW_LAUNCHER, true));
                mShowLauncher.setOnPreferenceChangeListener(this);
            } else {
                category.removePreference(mShowLauncher);
            }
        }

        mSkipChecks = (CustomCheckBoxPreference) findPreference(SKIP_CHECKS);
        if (mSkipChecks != null) {
            mSkipChecks.setChecked(PreferenceHelper.getBoolean(SKIP_CHECKS));
            mSkipChecks.setOnPreferenceChangeListener(this);
        }

        category = (PreferenceCategory) findPreference("prefs_app");

        if (Utils.existsInFile(Scripts.BUILD_PROP, "ro.nameless.secret=1")) {
            mMonkeyPref = new CustomCheckBoxPreference(getActivity());
            mMonkeyPref.setKey("monkey");
            mMonkeyPref.setTitle(R.string.become_a_monkey);
            mMonkeyPref.setSummaryOn(R.string.is_monkey);
            mMonkeyPref.setSummaryOff(R.string.no_monkey);
            mMonkeyPref.setChecked(PreferenceHelper.getBoolean("monkey", false));
            mMonkeyPref.setOnPreferenceChangeListener(this);
            category.addPreference(mMonkeyPref);
        }

        //category = (PreferenceCategory) findPreference("prefs_tools");
        mFlasherConfig = (CustomPreference) findPreference("flasher_prefs");

        //category = (PreferenceCategory) findPreference("prefs_interface");
        mColorPreference = (CustomPreference) findPreference("pref_color");
        mColorPreference.setSummaryColor(Application.get().getAccentColor());

        mDarkTheme = (CustomCheckBoxPreference) findPreference("dark_theme");
        mDarkTheme.setChecked(PreferenceHelper.getBoolean(mDarkTheme.getKey(), true));
        mDarkTheme.setOnPreferenceChangeListener(this);

        mSwipeOnContent = (CustomCheckBoxPreference) findPreference("swipe_on_content");
        mSwipeOnContent.setChecked(PreferenceHelper.getBoolean(mSwipeOnContent.getKey()));
        mSwipeOnContent.setOnPreferenceChangeListener(this);

        setupVersionPreference();
    }

    private void setupVersionPreference() {
        final CustomPreference mVersion = (CustomPreference) findPreference("prefs_version");
        if (mVersion != null) {
            String title;
            String summary;
            try {
                final PackageManager pm = Application.get().getPackageManager();
                if (pm != null) {
                    final PackageInfo pInfo = pm.getPackageInfo(getActivity().getPackageName(), 0);
                    title = getString(R.string.app_version_name, pInfo.versionName);
                    summary = getString(R.string.app_version_code, pInfo.versionCode);
                } else {
                    throw new Exception("pm not null");
                }
            } catch (Exception ignored) {
                title = getString(R.string.app_version_name, getString(R.string.unknown));
                summary = getString(R.string.app_version_code, getString(R.string.unknown));
            }
            mVersion.setTitle(title);
            mVersion.setSummary(summary);
        }
    }

    @Override public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mExtensiveLogging == preference) {
            final boolean value = (Boolean) newValue;
            PreferenceHelper.setBoolean(EXTENSIVE_LOGGING, value);
            Logger.setEnabled(value);
            mExtensiveLogging.setChecked(value);
            return true;
        } else if (mShowLauncher == preference) {
            final boolean value = (Boolean) newValue;
            PreferenceHelper.setBoolean(SHOW_LAUNCHER, value);
            Application.get().toggleLauncherIcon(value);
            mShowLauncher.setChecked(value);
            return true;
        } else if (mSkipChecks == preference) {
            final boolean value = (Boolean) newValue;
            PreferenceHelper.setBoolean(SKIP_CHECKS, value);
            mSkipChecks.setChecked(value);
            return true;
        } else if (mMonkeyPref == preference) {
            final boolean value = (Boolean) newValue;
            PreferenceHelper.setBoolean("monkey", value);
            mMonkeyPref.setChecked(value);
            return true;
        } else if (mSwipeOnContent == preference) {
            final boolean value = (Boolean) newValue;
            PreferenceHelper.setBoolean(mSwipeOnContent.getKey(), value);
            mSwipeOnContent.setChecked(value);

            // update the menu
            MainActivity.setSwipeOnContent(value);
            return true;
        } else if (mDarkTheme == preference) {
            final boolean isDark = (Boolean) newValue;
            Application.get().setDarkTheme(isDark);
            PreferenceHelper.setBoolean(mDarkTheme.getKey(), isDark);
            mDarkTheme.setChecked(isDark);

            if (isDark) {
                Application.get().setAccentColor(getResources().getColor(R.color.accent));
            } else {
                Application.get().setAccentColor(getResources().getColor(R.color.accent_light));
            }
            PreferenceHelper.setInt("pref_color", Application.get().getAccentColor());

            // restart the activity to apply new theme
            Utils.restartActivity(getActivity());
            return true;
        }

        return false;
    }

    @Override public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            @NonNull final Preference preference) {
        if (mColorPreference == preference) {
            new ColorPickerDialogFragment(new OnColorPickedListener() {
                @Override public void onColorPicked(final int color) {
                    if (mColorPreference != null) mColorPreference.setSummaryColor(color);
                }
            }).show(getFragmentManager(), "color_picker");
            return true;
        } else if (TextUtils.equals("pref_donate", preference.getKey())) {
            return AppHelper.startExternalDonation(getActivity());
        } else if (mFlasherConfig == preference) {
            MainActivity.loadFragment(getActivity(), ID_TOOLS_FLASHER_PREFS);
            return true;
        } else if (mSetOnBoot == preference) {
            new SobDialogFragment().show(getActivity().getFragmentManager(), "sob_dialog_fragment");
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public static interface OnColorPickedListener {
        public void onColorPicked(final int color);
    }

    private class SobDialogFragment extends DialogFragment {
        final ArrayList<Integer> entries = new ArrayList<Integer>();

        public SobDialogFragment() {
            super();
            entries.add(R.string.device);
            entries.add(R.string.cpusettings);
            entries.add(R.string.gpusettings);
            entries.add(R.string.extras);
            entries.add(R.string.sysctl_vm);
            entries.add(R.string.low_memory_killer);

            if (Utils.fileExists(VDD_TABLE_FILE) || Utils.fileExists(UV_TABLE_FILE)) {
                entries.add(R.string.voltage_control);
            }
        }

        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int length = entries.size();
            final String[] items = new String[length];
            final boolean[] checked = new boolean[length];

            for (int i = 0; i < length; i++) {
                items[i] = getString(entries.get(i));
                checked[i] = isChecked(entries.get(i));
            }

            final AccentAlertDialog.Builder builder = new AccentAlertDialog.Builder(getActivity());

            builder.setTitle(R.string.reapply_on_boot);
            builder.setMultiChoiceItems(items, checked,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        @Override public void onClick(final DialogInterface dialogInterface,
                                final int item, final boolean isChecked) {
                            PreferenceHelper.setBoolean(getKey(entries.get(item)), isChecked);
                        }
                    });
            builder.setCancelable(true);
            builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int i) { }
            });

            return builder.create();
        }

        private boolean isChecked(final int entry) {
            return PreferenceHelper.getBoolean(getKey(entry), false);
        }

        private String getKey(final int entry) {
            switch (entry) {
                case R.string.device:
                    return BootUpService.SOB_DEVICE;
                case R.string.cpusettings:
                    return BootUpService.SOB_CPU;
                case R.string.gpusettings:
                    return BootUpService.SOB_GPU;
                case R.string.extras:
                    return BootUpService.SOB_EXTRAS;
                case R.string.sysctl_vm:
                    return BootUpService.SOB_SYSCTL;
                case R.string.low_memory_killer:
                    return BootUpService.SOB_LMK;
                case R.string.voltage_control:
                    return BootUpService.SOB_VOLTAGE;
                default:
                    return "-";
            }
        }
    }

}
