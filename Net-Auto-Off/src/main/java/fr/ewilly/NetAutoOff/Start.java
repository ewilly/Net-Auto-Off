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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Utility class to set all necessary timers / start the background service
 */
public class Start {
    /**
     * Sets all necessary timers / starts the background service depending on the user settings
     *
     * @param context the context
     */
    static void start(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("screen_off_wifi", true) || prefs.getBoolean("screen_off_data_off", true) || prefs.getBoolean("screen_off_data_sms", true)) {
            if (Receiver.LOG) Log.d(Receiver.LOG_TAG, "start service ScreenChangeDetector");
            context.startService(new Intent(context, ScreenChangeDetector.class));
        } else {
            if (Receiver.LOG) Log.d(Receiver.LOG_TAG, "stop service ScreenChangeDetector");
            context.stopService(new Intent(context, ScreenChangeDetector.class));
        }
    }
}
