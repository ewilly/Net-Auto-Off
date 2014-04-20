package fr.ewilly.NetAutoOff;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

public class Fragment_APN_User_Settings extends PreferenceFragment {

    Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.apn_user_settings);

        // Get a reference to the application default shared preferences.
        final SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
        assert prefs != null;

        // Get a reference to the different preference box
        final SwitchPreference wifi_on_off = (SwitchPreference) findPreference("wifi_on_off");
        assert wifi_on_off != null;
        final SwitchPreference data_on_off = (SwitchPreference) findPreference("data_on_off");
        assert data_on_off != null;
        final SwitchPreference apn_data = (SwitchPreference) findPreference("apn_data");
        assert apn_data != null;
        final EditTextPreference apn_data_to_mms_value = (EditTextPreference) findPreference("apn_data_to_mms_value");
        assert apn_data_to_mms_value != null;
        final EditTextPreference apn_data_to_default_value = (EditTextPreference) findPreference("apn_data_to_default_value");
        assert apn_data_to_default_value != null;

        // Wifi Switch
        // Get initial state
        updateCurrent_wifi(wifi_on_off);
        // Get state
        wifi_on_off.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @SuppressLint("StringFormatMatches")
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    Receiver.Change_WiFi(mActivity, "on");
                } else {
                    Receiver.Change_WiFi(mActivity, "off");
                }
                return true;
            }
        });

        // Data Switch
        // Get initial state
        updateCurrent_data(data_on_off);
        // Get state
        data_on_off.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @SuppressLint("StringFormatMatches")
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    Receiver.Change_Data(mActivity, "on");
                } else {
                    Receiver.Change_Data(mActivity, "off");
                }
                return true;
            }
        });

        // APN Switch
        apn_data.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @SuppressLint("StringFormatMatches")
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Integer defaultId = ChangeAPN.getDefaultId(mActivity);
                String newType;
                if ((Boolean) newValue) {
                    newType = prefs.getString("apn_data_to_default_value", mActivity.getString(R.string.apn_data_to_default_value));
                    ChangeAPN.setData(mActivity, defaultId, newType);
                } else {
                    newType = prefs.getString("apn_data_to_mms_value", mActivity.getString(R.string.apn_data_to_mms_value));
                    ChangeAPN.setData(mActivity, defaultId, newType);
                }
                defaultId = ChangeAPN.getDefaultId(mActivity);
                updateCurrent_apn(mActivity, defaultId);
                return true;
            }
        });

        // APN text box for mms type
        apn_data_to_mms_value.setSummary(apn_data_to_mms_value.getText());
        apn_data_to_mms_value.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                apn_data_to_mms_value.setSummary((String) newValue);
                return true;
            }
        });

        // APN text box for default type
        apn_data_to_default_value.setSummary(apn_data_to_default_value.getText());
        apn_data_to_default_value.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                apn_data_to_default_value.setSummary((String) newValue);
                return true;
            }
        });

        // Update all state
        Integer defaultId = ChangeAPN.getDefaultId(mActivity);
        updateCurrent_apn(mActivity, defaultId);
        updateCurrent_wifi((SwitchPreference) findPreference("wifi_on_off"));
        updateCurrent_data((SwitchPreference) findPreference("data_on_off"));
    }

    @Override
    public void onResume() {
        super.onResume();
        Start.start(mActivity);

        // Update all state
        Integer defaultId = ChangeAPN.getDefaultId(mActivity);
        updateCurrent_apn(mActivity, defaultId);
        updateCurrent_wifi((SwitchPreference) findPreference("wifi_on_off"));
        updateCurrent_data((SwitchPreference) findPreference("data_on_off"));
    }

    @Override
    public void onPause() {
        super.onPause();
        Start.start(mActivity);

        // Update all state
        Integer defaultId = ChangeAPN.getDefaultId(mActivity);
        updateCurrent_apn(mActivity, defaultId);
        updateCurrent_wifi((SwitchPreference) findPreference("wifi_on_off"));
        updateCurrent_data((SwitchPreference) findPreference("data_on_off"));
    }

    /**
     * Update the current APN value
     *
     * @param context   the context
     * @param defaultId the current APN Id
     */
    public void updateCurrent_apn(Context context, Integer defaultId) {
        SwitchPreference apn_data = (SwitchPreference) findPreference("apn_data");
        String type = null;
        if (defaultId != -1) {
            // Query current TYPE.
            type = ChangeAPN.getType(context, defaultId);
        }
        assert apn_data != null;
        if (type != null) {
            // Update summary
            apn_data.setSummary(type);
            // Update switch position
            if (type.equals(context.getString(R.string.apn_data_to_mms_value))) {
                apn_data.setChecked(false);
            } else {
                apn_data.setChecked(true);
            }
        } else {
            // Update summary
            apn_data.setSummary("null.");
        }
    }

    /**
     * Update the current Wi-Fi state
     *
     * @param key the key
     */

    public void updateCurrent_wifi(SwitchPreference key) {
        if (isAdded()) {
            WifiManager wifi = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
            if (wifi.isWifiEnabled()) {
                key.setChecked(true);
            } else {
                key.setChecked(false);
            }
        }
    }

    /**
     * Update the current Data state
     *
     * @param key the key
     */
    public void updateCurrent_data(SwitchPreference key) {
        if (Receiver.Is_Data_Enabled(mActivity)) {
            key.setChecked(true);
        } else {
            key.setChecked(false);
        }
    }
}
