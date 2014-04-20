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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

/**
 * Background service which detects SCREEN_OFF events.
 * <p/>
 * Necessary for the 'turn off if screen is off' option
 */
public class ScreenChangeDetector extends Service {

    final static String SCREEN_OFF_ACTION = "SCREEN_OFF";
    final static String SCREEN_ON_ACTION = "SCREEN_ON";

    private static BroadcastReceiver br;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (br == null) {
            if (Receiver.LOG) Log.d(Receiver.LOG_TAG, "creating screen on/off receiver");
            br = new ScreenOffReceiver();
            IntentFilter intf = new IntentFilter();
            intf.addAction(Intent.ACTION_SCREEN_ON);
            intf.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(br, intf);
        }
        // Workaround as on Android 4.4.2 START_STICKY has currently no
        // effect
        // -> restart service every hour
        if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.KITKAT)
            ((AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System
                    .currentTimeMillis() + 1000 * 60 * 60, PendingIntent.getService(getApplicationContext(), 1, new Intent(this,
                    ScreenChangeDetector.class), PendingIntent.FLAG_UPDATE_CURRENT));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Receiver.LOG) Log.d(Receiver.LOG_TAG, "destroying screen on/off receiver");
        if (br != null) {
            try {
                unregisterReceiver(br);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        br = null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Workaround for "Android 4.4.2 ignoring START_STICKY bug"
        // Restart service in 500 ms
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC, System.currentTimeMillis() + 500,
                PendingIntent.getService(this, 0, new Intent(this, ScreenChangeDetector.class), 0));
    }

    private class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                sendBroadcast(new Intent(context, Receiver.class).setAction(SCREEN_OFF_ACTION));
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())
                    && !((KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode()) {
                // SCREEN_ON is only send if there is no lockscreen active! Otherwise the Receiver will get USER_PRESENT
                sendBroadcast(new Intent(context, Receiver.class).setAction(SCREEN_ON_ACTION));
            }
        }
    }
}
