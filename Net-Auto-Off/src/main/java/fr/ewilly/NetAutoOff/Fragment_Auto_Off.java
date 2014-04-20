package fr.ewilly.NetAutoOff;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.widget.NumberPicker;
import android.widget.Toast;

public class Fragment_Auto_Off extends PreferenceFragment {

    Activity mActivity;

    /**
     * Checks if the WiFi sleep policy will keep WiFi when the screen goes off
     *
     * @param context the context
     * @return true if WiFi will be kept on during sleep
     */
    private static boolean keepWiFiOn(Context context) {
        try {
            return ((sleepPolicySetToNever(context)) || (Settings.Global.WIFI_SLEEP_POLICY_NEVER == Settings.Global.getInt(
                    context.getContentResolver(), Settings.Global.WIFI_SLEEP_POLICY)));
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Set the Wifi sleep policy to never
     *
     * @param context the context
     */
    private static boolean sleepPolicySetToNever(final Context context) throws Settings.SettingNotFoundException {
        return Settings.Global.WIFI_SLEEP_POLICY_NEVER == Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.WIFI_SLEEP_POLICY);
    }

    /**
     * Choose the timeout
     *
     * @param context     the context
     * @param prefs       the SharedPreferences
     * @param p           the key
     * @param summary     the summary of the dialogue box
     * @param min         the minimal timeout
     * @param max         the maximal timeout
     * @param title       the title of the dialogue box
     * @param setting     the setting
     * @param def         the timeout to update
     * @param changeTitle if true the selected value will be wrote in the title otherwise in the summary
     */
    private static void showNumberPicker(final Context context, final SharedPreferences prefs, final Preference p, final int summary,
                                         final int min, final int max, final String title, final String setting, final int def, final boolean changeTitle) {
        final NumberPicker np = new NumberPicker(context);
        np.setMinValue(min);
        np.setMaxValue(max);
        np.setValue(prefs.getInt(setting, def));
        new AlertDialog.Builder(context).setTitle(title).setView(np).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                np.clearFocus();
                prefs.edit().putInt(setting, np.getValue()).apply();
                if (changeTitle)
                    // Update title
                    p.setTitle(context.getString(summary, np.getValue()));
                else
                    // Update summary
                    p.setSummary(context.getString(summary, np.getValue()));
            }
        }).create().show();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.auto_off);

        // Get a reference to the application default shared preferences.
        final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        assert prefs != null;

        // Get a reference to the different preference box
        final SwitchPreference auto_off = (SwitchPreference) findPreference("auto_off");
        assert auto_off != null;
        final CheckBoxPreference screen_off_wifi = (CheckBoxPreference) findPreference("screen_off_wifi");
        assert screen_off_wifi != null;
        final CheckBoxPreference screen_off_data_off = (CheckBoxPreference) findPreference("screen_off_data_off");
        assert screen_off_data_off != null;
        final CheckBoxPreference screen_off_data_mms = (CheckBoxPreference) findPreference("screen_off_data_mms");
        assert screen_off_data_mms != null;

        // Main switch
        auto_off.setChecked(getPackageManager().getComponentEnabledSetting(
                new ComponentName(mActivity, Receiver.class)) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        if (!auto_off.isChecked()) {
            PreferenceScreen ps = getPreferenceScreen();
            for (int i = 1; i < ps.getPreferenceCount(); i++) {
                ps.getPreference(i).setEnabled(false);
            }
        }
        auto_off.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean is_checked = (Boolean) newValue;
                PreferenceScreen ps = getPreferenceScreen();
                for (int i = 1; i < ps.getPreferenceCount(); i++) {
                    ps.getPreference(i).setEnabled(is_checked);
                }
                if (!is_checked) {
                    if (Receiver.LOG) Log.d(Receiver.LOG_TAG, "stopping service");
                    mActivity.stopService(new Intent(mActivity, ScreenChangeDetector.class));
                } else {
                    if (Receiver.LOG) Log.d(Receiver.LOG_TAG, "starting service");
                    mActivity.startService(new Intent(mActivity, ScreenChangeDetector.class));
                    getPackageManager().setComponentEnabledSetting(
                            new ComponentName(mActivity, ScreenChangeDetector.class),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
                    );
                }
                return true;
            }
        });

        // CheckBox WIFI OFF
        screen_off_wifi.setSummary(getString(R.string.for_at_least, prefs.getInt("screen_off_timeout_wifi", Receiver.TIMEOUT_SCREEN_OFF_WIFI)));
        screen_off_wifi.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    if (!keepWiFiOn(mActivity)) {
                        new AlertDialog.Builder(mActivity).setMessage(R.string.sleep_policy)
                                .setPositiveButton(R.string.adv_wifi_settings, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            startActivity(new Intent(Settings.ACTION_WIFI_IP_SETTINGS)
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                        } catch (Exception e) {
                                            Toast.makeText(mActivity, R.string.settings_not_found_, Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    }
                                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
                        return false;
                    }
                    showNumberPicker(mActivity, prefs, screen_off_wifi, R.string.for_at_least, 0, 60,
                            getString(R.string.minutes_before_turning_off_wifi), "screen_off_timeout_wifi",
                            Receiver.TIMEOUT_SCREEN_OFF_WIFI, false);
                }
                return true;
            }
        });

        // CheckBox DATA OFF
        screen_off_data_off.setSummary(getString(R.string.for_at_least, prefs.getInt("screen_off_timeout_data_off", Receiver.TIMEOUT_SCREEN_OFF_DATA_OFF)));
        screen_off_data_off.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    showNumberPicker(mActivity, prefs, screen_off_data_off, R.string.for_at_least, 0, 30,
                            getString(R.string.minutes_before_turning_off_data_off), "screen_off_timeout_data_off",
                            Receiver.TIMEOUT_SCREEN_OFF_DATA_OFF, false);
                    screen_off_data_mms.setChecked(false);
                }
                return true;
            }
        });

        // CheckBox DATA MMS
        screen_off_data_mms.setSummary(getString(R.string.for_at_least, prefs.getInt("screen_off_timeout_data_mms", Receiver.TIMEOUT_SCREEN_OFF_DATA_MMS)));
        screen_off_data_mms.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    showNumberPicker(mActivity, prefs, screen_off_data_mms, R.string.for_at_least, 0, 30,
                            getString(R.string.minutes_before_turning_off_data_mms), "screen_off_timeout_data_mms",
                            Receiver.TIMEOUT_SCREEN_OFF_DATA_MMS, false);
                    screen_off_data_off.setChecked(false);
                }
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Start.start(mActivity);
    }

    @Override
    public void onPause() {
        super.onPause();
        Start.start(mActivity);
    }

    /**
     * Returns the PackageManager from the owning Activity.
     */
    protected PackageManager getPackageManager() {
        return mActivity.getPackageManager();
    }
}
