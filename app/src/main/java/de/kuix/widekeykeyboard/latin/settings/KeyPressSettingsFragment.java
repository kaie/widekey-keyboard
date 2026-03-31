/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kuix.widekeykeyboard.latin.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.Preference;

import de.kuix.widekeykeyboard.R;
import de.kuix.widekeykeyboard.latin.AudioAndHapticFeedbackManager;

/**
 * "Preferences" settings sub screen.
 *
 * This settings sub screen handles the following input preferences.
 * - Vibrate on keypress
 * - Keypress vibration duration
 * - Sound on keypress
 * - Keypress sound volume
 * - Popup on keypress
 * - Key long press delay
 */
public final class KeyPressSettingsFragment extends SubScreenFragment {
    private Preference mDoubleTapTimeoutPref;
    private Preference mLongTapThresholdPref;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_key_press);

        final Context context = getActivity();

        // When we are called from the Settings application but we are not already running, some
        // singleton and utility classes may not have been initialized.  We have to call
        // initialization method of these classes here. See {@link LatinIME#onCreate()}.
        AudioAndHapticFeedbackManager.init(context);

        if (!AudioAndHapticFeedbackManager.getInstance().hasVibrator()) {
            removePreference(Settings.PREF_VIBRATE_ON);
        }

        mDoubleTapTimeoutPref = findPreference(Settings.PREF_DOUBLETAP_TIMEOUT);
        mLongTapThresholdPref = findPreference(Settings.PREF_LONG_TAP_THRESHOLD);

        setupKeypressSoundVolumeSettings();
        setupKeyLongpressTimeoutSettings();
        setupDoubleTapTimeoutSettings();
        setupLongTapThresholdSettings();
        updateTapModePrefsVisibility();
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        if (Settings.PREF_USE_LONG_TAP.equals(key)) {
            updateTapModePrefsVisibility();
            scaleLongpressTimeoutForMode(prefs);
        }
    }

    private void scaleLongpressTimeoutForMode(final SharedPreferences prefs) {
        final Resources res = getResources();
        final int current = Settings.readKeyLongpressTimeout(prefs, res);
        final boolean useLongTap = Settings.readUseLongTap(prefs);
        final int scaled = Math.round((useLongTap ? current * 1.5f : current / 1.5f) / 10) * 10;
        final int min = res.getInteger(R.integer.config_min_longpress_timeout);
        final int max = res.getInteger(R.integer.config_max_longpress_timeout);
        final int clamped = Math.max(min, Math.min(max, scaled));
        prefs.edit().putInt(Settings.PREF_KEY_LONGPRESS_TIMEOUT, clamped).apply();
        final SeekBarDialogPreference pref = (SeekBarDialogPreference) findPreference(
                Settings.PREF_KEY_LONGPRESS_TIMEOUT);
        if (pref != null) {
            pref.setSummary(res.getString(R.string.abbreviation_unit_milliseconds, clamped));
        }
    }

    private void updateTapModePrefsVisibility() {
        final boolean useLongTap = Settings.readUseLongTap(getSharedPreferences());
        final android.preference.PreferenceScreen screen = getPreferenceScreen();
        if (useLongTap) {
            screen.removePreference(mDoubleTapTimeoutPref);
            screen.addPreference(mLongTapThresholdPref);
        } else {
            screen.removePreference(mLongTapThresholdPref);
            screen.addPreference(mDoubleTapTimeoutPref);
        }
    }

    private void setupKeypressSoundVolumeSettings() {
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_KEYPRESS_SOUND_VOLUME);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            private static final float PERCENTAGE_FLOAT = 100.0f;

            private float getValueFromPercentage(final int percentage) {
                return percentage / PERCENTAGE_FLOAT;
            }

            private int getPercentageFromValue(final float floatValue) {
                return (int)(floatValue * PERCENTAGE_FLOAT);
            }

            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putFloat(key, getValueFromPercentage(value)).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return getPercentageFromValue(Settings.readKeypressSoundVolume(prefs));
            }

            @Override
            public int readDefaultValue(final String key) {
                return getPercentageFromValue(Settings.readDefaultKeypressSoundVolume());
            }

            @Override
            public String getValueText(final int value) {
                if (value < 0) {
                    return res.getString(R.string.settings_system_default);
                }
                return Integer.toString(value);
            }

            @Override
            public void feedbackValue(final int value) {
                AudioAndHapticFeedbackManager.getInstance().playSoundEffect(
                        AudioManager.FX_KEYPRESS_STANDARD, getValueFromPercentage(value));
            }
        });
    }

    private void setupKeyLongpressTimeoutSettings() {
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_KEY_LONGPRESS_TIMEOUT);
        if (pref == null) {
            return;
        }
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return Settings.readKeyLongpressTimeout(prefs, res);
            }

            @Override
            public int readDefaultValue(final String key) {
                return Settings.readDefaultKeyLongpressTimeout(res);
            }

            @Override
            public String getValueText(final int value) {
                return res.getString(R.string.abbreviation_unit_milliseconds, value);
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    private void setupDoubleTapTimeoutSettings() {
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_DOUBLETAP_TIMEOUT);
        if (pref == null) {
            return;
        }
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return Settings.readDoubleTapTimeout(prefs, res);
            }

            @Override
            public int readDefaultValue(final String key) {
                return res.getInteger(R.integer.config_default_doubletap_timeout);
            }

            @Override
            public String getValueText(final int value) {
                return res.getString(R.string.abbreviation_unit_milliseconds, value);
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }

    private void setupLongTapThresholdSettings() {
        final SharedPreferences prefs = getSharedPreferences();
        final Resources res = getResources();
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_LONG_TAP_THRESHOLD);
        if (pref == null) {
            return;
        }
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }

            @Override
            public int readValue(final String key) {
                return Settings.readLongTapThreshold(prefs, res);
            }

            @Override
            public int readDefaultValue(final String key) {
                return res.getInteger(R.integer.config_default_longtap_threshold);
            }

            @Override
            public String getValueText(final int value) {
                return res.getString(R.string.abbreviation_unit_milliseconds, value);
            }

            @Override
            public void feedbackValue(final int value) {}
        });
    }
}
