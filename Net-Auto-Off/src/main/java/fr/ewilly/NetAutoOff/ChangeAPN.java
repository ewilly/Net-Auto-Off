/*
 * Copyright 2014 Sylvain Arnould
 * Thanks to Bence Béky (https://github.com/bencebeky/MMSKeeper)
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Carriers;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

/**
 * Class for APN configuration.
 */
public class ChangeAPN extends ContextThemeWrapper {
    public static final Uri APN_TABLE_URI = Uri.parse("content://telephony/carriers");
    public static final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");


    /**
     * Read preferred table to find _id of default APN
     *
     * @param context the context
     */
    public static Integer getDefaultId(Context context) {
        Cursor c;
        String[] projections = new String[]{Carriers._ID};
        Integer defaultId = -1;
        try {
            // Query database.
            c = context.getContentResolver().query(PREFERRED_APN_URI, projections, null, null, Carriers.DEFAULT_SORT_ORDER);
            if (c != null) {
                if (c.moveToFirst()) {
                    defaultId = c.getInt(c.getColumnIndex(Carriers._ID));
                }
                c.close();
            }
        } catch (SecurityException e) {
            // No permission.
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                Toast toast = Toast.makeText(context, context.getString(R.string.permissionError), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        return defaultId;
    }

    /**
     * Read current type from database
     *
     * @param context the context
     */
    public static String getType(Context context, Integer defaultId) {
        if (defaultId == -1)
            return null;
        Cursor c;
        String[] projections = new String[]{Carriers._ID, Carriers.TYPE};
        String where = "_id = ?";
        String wargs[] = new String[]{Integer.toString(defaultId)};
        String type = null;
        try {
            // Query database.
            c = context.getContentResolver().query(APN_TABLE_URI, projections, where, wargs, Carriers.DEFAULT_SORT_ORDER);
            if (c != null) {
                if (c.moveToFirst()) {
                    type = c.getString(c.getColumnIndex(Carriers.TYPE));
                }
                c.close();
            }
        } catch (SecurityException e) {
            // No permission: do nothing. Toaster is displayed by getDefaultId anyway.
        }
        return type;
    }

    /**
     * Turn data on or off or to mms
     *
     * @param context   the context
     * @param defaultId the preferred table to find _id of default APN
     * @param newType   the new APN type
     */
    public static void setData(Context context, Integer defaultId, String newType) {
        if (defaultId == -1) {
            return;
        }
        // Load data strings from preferences.
        // SharedPreferences settings = context.getSharedPreferences("global", MODE_PRIVATE);
        // Assemble query.
        String where = "_id = ?";
        String wargs[] = new String[]{Integer.toString(defaultId)};
        ContentValues updateValues = new ContentValues();
        updateValues.put(Carriers.TYPE, newType);
        try {
            // Update database.
            context.getContentResolver().update(APN_TABLE_URI, updateValues, where, wargs);
        } catch (SecurityException e) {
            // No permission: do nothing. Toaster is displayed by getDefaultId anyway.
        }
    }
}
