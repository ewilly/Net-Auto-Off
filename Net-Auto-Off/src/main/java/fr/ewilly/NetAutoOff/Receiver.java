/*
 * Copyright 2014 Sylvain Arnould
 * Thanks to Thomas Hoffmann (https://github.com/j4velin/WiFi-Auto-Off)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.ewilly.NetAutoOff;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;

/**
 * Class for receiving various events and react on them.
 */
public class Receiver extends BroadcastReceiver {

    public static final String LOG_TAG = "MyLog";
    // SetData type
    public static final String ON = "on";
    public static final String OFF = "off";
    public static final String MMS = "mms";
    // Log
    static final boolean LOG = false;
    // Default timeout in minutes
    static final int TIMEOUT_SCREEN_OFF_WIFI = 5;
    static final int TIMEOUT_SCREEN_OFF_DATA_OFF = 1;
    static final int TIMEOUT_SCREEN_OFF_DATA_MMS = 0;
    // Timer number
    private static final int TIMER_SCREEN_OFF_WIFI = 1;
    private static final int TIMER_SCREEN_OFF_DATA_OFF = 2;
    private static final int TIMER_SCREEN_OFF_DATA_MMS = 3;

    /**
     * Changes the WiFi state
     *
     * @param context the context
     * @param state   on to turn WiFi on, off to turn it off
     */
    static void Change_WiFi(Context context, String state) {
        // check for airplane mode
        if (state.equals(ON)) {
            try {
                if (isAirplaneModeOn(context)) {
                    if (LOG)
                        Log.d(LOG_TAG, "not turning wifi on because device is in airplane mode");
                    return;
                } else {
                    try {
                        ((WifiManager) context.getSystemService(WIFI_SERVICE)).setWifiEnabled(true);
                    } catch (Exception e) {
                        Toast.makeText(context, "Can not change WiFi state to on: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
                    }
                }
            } catch (final SettingNotFoundException e) {
                // not airplane setting found? Handle like not in airplane mode then
                e.printStackTrace();
            }
        }
        if (state.equals(OFF)) {
            if (LOG) Log.d(LOG_TAG, "disabling wifi");
            try {
                ((WifiManager) context.getSystemService(WIFI_SERVICE)).setWifiEnabled(false);
            } catch (Exception e) {
                Toast.makeText(context, "Can not change WiFi state to off: " + e.getClass().getName(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Changes the Data state
     *
     * @param context the context
     * @param state   on to turn Data on, off to turn it off, mms to turn it to MMS only
     */
    static void Change_Data(Context context, String state) {
        if (state.equals(ON)) {
            if (LOG) Log.d(LOG_TAG, "enabling data");
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                Method setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(cm, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (state.equals(OFF)) {
            if (LOG) Log.d(LOG_TAG, "disabling data");
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                Method setMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(cm, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (state.equals(MMS)) {
            if (LOG) Log.d(LOG_TAG, "turning data to SMS only");
            try {
                Integer defaultId = ChangeAPN.getDefaultId(context);
                ChangeAPN.setData(context, defaultId, "mms");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if Data is enabled
     *
     * @param context the context
     */
    static boolean Is_Data_Enabled(Context context) {
        boolean mobileDataEnabled;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
            Method getMobileDataEnabledMethod = ConnectivityManager.class.getDeclaredMethod("getMobileDataEnabled");
            getMobileDataEnabledMethod.setAccessible(true);
            // Make the method callable
            getMobileDataEnabledMethod.invoke(cm);
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean) getMobileDataEnabledMethod.invoke(cm);
            return mobileDataEnabled;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if the phone in on airplane mode
     *
     * @param context the context
     */
    static boolean isAirplaneModeOn(final Context context) throws SettingNotFoundException {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON) == 1;
    }

    /**
     * Starts one of the timers to turn WiFi off
     *
     * @param context the context
     * @param id      TIMER_SCREEN_OFF_WIFI or TIMER_NO_NETWORK_WIFI
     * @param time    in min
     */
    private void startTimer(Context context, int id, int time) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent timer_Intent = null;
        if (id == TIMER_SCREEN_OFF_WIFI) {
            timer_Intent = new Intent(context, Receiver.class).putExtra("timer_wifi", true).setAction("SCREEN_OFF_TIMER_WIFI");
        } else if (id == TIMER_SCREEN_OFF_DATA_OFF) {
            timer_Intent = new Intent(context, Receiver.class).putExtra("timer_data_off", true).setAction("SCREEN_OFF_TIMER_DATA_OFF");
        } else if (id == TIMER_SCREEN_OFF_DATA_MMS) {
            timer_Intent = new Intent(context, Receiver.class).putExtra("timer_data_mms", true).setAction("SCREEN_OFF_TIMER_DATA_MMS");
        }
        if (LOG) Log.d(LOG_TAG, String.valueOf(timer_Intent));
        am.setWindow(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000 * time, 5000,
                PendingIntent.getBroadcast(context, id, timer_Intent, 0));
    }

    /**
     * Stops the timer
     *
     * @param context the context
     * @param id      TIMER_SCREEN_OFF_WIFI or TIMER_NO_NETWORK_WIFI
     */
    private void stopTimer(Context context, int id) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent timer_Intent = null;
        if (id == TIMER_SCREEN_OFF_WIFI) {
            timer_Intent = new Intent(context, Receiver.class).putExtra("timer_wifi", true).setAction("SCREEN_OFF_TIMER_WIFI");
        } else if (id == TIMER_SCREEN_OFF_DATA_OFF) {
            timer_Intent = new Intent(context, Receiver.class).putExtra("timer_data_off", true).setAction("SCREEN_OFF_TIMER_DATA_OFF");
        } else if (id == TIMER_SCREEN_OFF_DATA_MMS) {
            timer_Intent = new Intent(context, Receiver.class).putExtra("timer_data_mms", true).setAction("SCREEN_OFF_TIMER_DATA_MMS");
        }
        am.cancel(PendingIntent.getBroadcast(context, id, timer_Intent, 0));
    }

    /**
     * Analyse received information
     *
     * @param context the context
     * @param intent  the intent
     */
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (LOG) Log.d(LOG_TAG, "received: " + action);
        if (ScreenChangeDetector.SCREEN_OFF_ACTION.equals(action)) {
            if (LOG) Log.d(LOG_TAG, "screen is now off -> start all the timer");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            {
                if (prefs.getBoolean("screen_off_wifi", true)) {
                    if (LOG) Log.d(LOG_TAG, "screen_off_wifi checked -> start this timer");
                    // screen went off -> start TIMER_SCREEN_OFF_WIFI
                    startTimer(context, TIMER_SCREEN_OFF_WIFI,
                            PreferenceManager.getDefaultSharedPreferences(context).getInt("screen_off_timeout_wifi", TIMEOUT_SCREEN_OFF_WIFI));
                }
                if (prefs.getBoolean("screen_off_data_off", true)) {
                    if (LOG) Log.d(LOG_TAG, "screen_off_data_off checked -> start this timer");
                    // screen went off -> start TIMER_SCREEN_OFF_DATA_OFF
                    startTimer(context, TIMER_SCREEN_OFF_DATA_OFF,
                            PreferenceManager.getDefaultSharedPreferences(context).getInt("screen_off_timeout_data_off", TIMEOUT_SCREEN_OFF_DATA_OFF));
                }
                if (prefs.getBoolean("screen_off_data_mms", true)) {
                    if (LOG) Log.d(LOG_TAG, "screen_off_data_mms checked -> start this timer");
                    // screen went off -> start TIMER_SCREEN_OFF_DATA_MMS
                    startTimer(context, TIMER_SCREEN_OFF_DATA_MMS,
                            PreferenceManager.getDefaultSharedPreferences(context).getInt("screen_off_timeout_data_mms", TIMEOUT_SCREEN_OFF_DATA_MMS));
                }
            }
        } else if (Intent.ACTION_USER_PRESENT.equals(action)
                || ScreenChangeDetector.SCREEN_ON_ACTION.equals(action))
        {
            if (LOG) Log.d(LOG_TAG, "screen is now on -> stop all the timer");
            // screen went off -> stop TIMER_SCREEN_OFF_WIFI
            stopTimer(context, TIMER_SCREEN_OFF_WIFI);
            // screen went on -> stop TIMER_SCREEN_OFF_DATA_OFF
            stopTimer(context, TIMER_SCREEN_OFF_DATA_OFF);
            // screen went on -> stop TIMER_SCREEN_OFF_DATA_MMS
            stopTimer(context, TIMER_SCREEN_OFF_DATA_MMS);
        }else if (intent.hasExtra("timer_wifi")) {
            // one of the timers expired -> turn Wifi off
            if (LOG) Log.d(LOG_TAG, "timer_wifi expired");
            Change_WiFi(context, OFF);
        } else if (intent.hasExtra("timer_data_off")) {
            // one of the timers expired -> turn Data off
            if (LOG) Log.d(LOG_TAG, "timer_data_off expired");
            Change_Data(context, OFF);
        } else if (intent.hasExtra("timer_data_mms")) {
            // one of the timers expired -> turn data to MMS only
            if (LOG) Log.d(LOG_TAG, "timer_data_mms expired");
            Change_Data(context, MMS);
        }
    }
}
