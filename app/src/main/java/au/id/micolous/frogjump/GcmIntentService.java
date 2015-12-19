/*
 * This file is a part of Frogjump <https://github.com/micolous/frogjump>
 * Copyright 2015 Michael Farrell <http://micolous.id.au>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package au.id.micolous.frogjump;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Handles intent messaging from GcmBroadcastReceiver and actions them.
 */
public class GcmIntentService extends GcmListenerService {
    public static final String TAG = "GcmIntentService";


    @Override
    public void onMessageReceived(String sender, Bundle extras) {
        Context app_context = getApplicationContext();
        if (extras != null && !extras.isEmpty()) {
            //Logger.getLogger("GCM_RECEIVED").log(Level.INFO, extras.toString());
            Log.i(TAG, "Message: " + extras.toString());

            String action = extras.getString("a");
            if (action == null) {
                return;
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            if (action.equalsIgnoreCase("join")) {
                // We got a login message, we need to switch our views
                String group_id_s = extras.getString("g");
                if (group_id_s == null) {
                    return;
                }
                int group_id = Integer.parseInt(group_id_s);

                Log.i(TAG, "Login: " + group_id);

                sharedPreferences.edit()
                        .putInt(ApplicationPreferences.GROUP_ID, group_id)
                        .apply();

                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } else if (action.equalsIgnoreCase("nojoin")) {
                Util.showToast(app_context, R.string.cant_join_group);
            } else if (action.equalsIgnoreCase("goto")) {
                // We got an order to change our navigation target
                String latE6_s = extras.getString("y");
                String lngE6_s = extras.getString("x");
                if (latE6_s == null || lngE6_s == null) {
                    return;
                }

                int latE6 = Integer.parseInt(latE6_s);
                int lngE6 = Integer.parseInt(lngE6_s);
                // Check for dry-run
                boolean dryRun = false;
                String dryRun_s = extras.getString("d");
                if (dryRun_s != null) {
                    dryRun = Integer.parseInt(dryRun_s) >= 1;
                }

                sharedPreferences.edit()
                        .putInt(ApplicationPreferences.LAST_X, lngE6)
                        .putInt(ApplicationPreferences.LAST_Y, latE6)
                        .apply();

                MainActivity.NavigationMode navigationMode = MainActivity.NavigationMode.getById(sharedPreferences.getInt(ApplicationPreferences.NAVIGATION_MODE, 0));
                Log.i(TAG, "Goto: " + navigationMode + " at " + latE6 + "," + lngE6 + " (dry_run = " + dryRun + ")");

                if (!dryRun) {
                    Util.navigateTo(latE6, lngE6, navigationMode, this);
                }
            }

        }
    }


}
