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
    public static final String GOOGLE_MAPS = "com.google.android.apps.maps";
    public static final String GPS_STATUS = "com.eclipsim.gpsstatus2";


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
                MainActivity.NavigationMode navigationMode = MainActivity.NavigationMode.getById(sharedPreferences.getInt(ApplicationPreferences.NAVIGATION_MODE, 0));

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
                    dryRun = Integer.parseInt(dryRun_s) >= 1 ;
                }

                Log.i(TAG, "Goto: " + navigationMode + " at " + latE6 + "," + lngE6 + " (dry_run = " + dryRun + ")");

                // Lets make that a decimal again
                String geoloc = String.format("%1$d.%2$06d,%3$d.%4$06d",
                        latE6 / 1000000, Math.abs(latE6 % 1000000),
                        lngE6 / 1000000, Math.abs(lngE6 % 1000000));

                if (!dryRun) {
                    if (navigationMode == MainActivity.NavigationMode.CROW_FLIES) {
                        geoloc = "geo:" + geoloc;
                        Uri geouri = Uri.parse(geoloc);
                        Intent intent = new Intent(Intent.ACTION_VIEW, geouri);
                        intent.setPackage(GPS_STATUS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                    } else if (navigationMode != MainActivity.NavigationMode.OFF) {
                        geoloc = "google.navigation:q=" + geoloc;
                        switch (navigationMode) {
                            // https://developers.google.com/maps/documentation/directions/intro#Restrictions
                            // The intent service supports this parameter too, but it is not documented.
                            case DRIVING_AVOID_TOLLS:
                                geoloc += "&avoid=tolls";
                            case DRIVING:
                                geoloc += "&mode=d";
                                break;
                            case CYCLING:
                                geoloc += "&mode=b";
                                break;
                            case WALKING:
                                geoloc += "&mode=w";
                                break;
                        }

                        // Launch Google Maps
                        Uri geouri = Uri.parse(geoloc);
                        Intent intent = new Intent(Intent.ACTION_VIEW, geouri);
                        intent.setPackage(GOOGLE_MAPS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            }

        }
    }
}
