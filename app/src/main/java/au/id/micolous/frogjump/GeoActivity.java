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
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.appspot.frogjump_cloud.frogjump.Frogjump;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesGroupResponse;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesPostMessageRequest;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoActivity extends AppCompatActivity {

    public static final String TAG = "GeoActivity";
    protected static final Pattern LL_PATTERN = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?),([+-]?\\d+(?:\\.\\d+)?)");
    private String gcm_token;
    private String group_key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo);

        // Start by seeing if we have a group key available.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        gcm_token = sharedPreferences.getString(ApplicationPreferences.GCM_TOKEN, null);
        group_key = sharedPreferences.getString(ApplicationPreferences.GROUP_KEY, null);

        if (gcm_token == null || group_key == null) {
            // drop out, we can't continue.
            Log.i(TAG, "Cannot find group_key, gcm_token, or otherwise not registered");
            showToast(R.string.broadcast_login_req);
            finish();
            return;
        }

        resolveIntent(getIntent());
        finish();
    }

    @Override
    public void onNewIntent(Intent intent) {
        resolveIntent(intent);
    }

    private boolean resolveIntent(Intent intent) {
        // Lets try to get the latE6 and lngE6 we need
        final Uri geoLocation = intent.getData();

        if (geoLocation.getScheme().equals("geo")) {
            // There are a few ways to do this, according to
            // https://developer.android.com/guide/components/intents-common.html#Maps

            // Case 1: geo:lat,lng
            // Case 2: geo:lat,lng?z=zoom
            // Case 3: geo:0,0?q=lat,lng(label)
            // Case 4: geo:0,0?q=street+address

            // In reality we only can handle cases 1 - 3, and case 4 will simply be dropped.  We
            // don't have a geocoder available to handle type 4.

            // We will also drop the "label" parameter, because it isn't important.
            String geoString = geoLocation.getSchemeSpecificPart();
            Log.i(TAG, "Geo = " + geoString);
            double lat = 0, lng = 0;
            boolean hasLL = false;


            // Check to see if we have q= set (type 3/4), and that it's after "?"
            int queryindex = geoString.indexOf('?');
            if (queryindex >= 0) {
                int qoff = geoString.indexOf("q=", queryindex);
                if (qoff != -1) {
                    // Type 3 / 4

                    // a.n.Uri doesn't support decoding query parameters for non-hierarchical URIs.
                    int andoff = geoString.indexOf('&', qoff);
                    String q;
                    if (andoff == -1) {
                        q = geoString.substring(qoff);
                    } else {
                        q = geoString.substring(qoff, andoff);
                    }

                    Matcher llMatcher = LL_PATTERN.matcher(q);
                    if (!llMatcher.find()) {
                        // Type 4, we can't handle
                        Log.i(TAG, "Cannot handle Type 4 (geocoder required) geo URIs");
                        showToast(R.string.broadcast_not_supported);
                        return false;
                    }

                    // Type 3
                    lat = Double.valueOf(llMatcher.group(1));
                    lng = Double.valueOf(llMatcher.group(2));
                    hasLL = true;
                }
            }

            if (!hasLL) {
                // Type 1 / 2
                String q;
                if (queryindex >= 0) {
                    // Type 2
                    q = geoString.substring(0, queryindex);
                } else {
                    // No query part, Type 1
                    q = geoString;
                }

                // Now parse
                Matcher llMatcher = LL_PATTERN.matcher(q);
                if (!llMatcher.find()) {
                    // Something weird happened...
                    Log.i(TAG, "Error handling type 1/2 location " + q);
                    showToast(R.string.broadcast_not_supported);
                    return false;
                }

                lat = Double.valueOf(llMatcher.group(1));
                lng = Double.valueOf(llMatcher.group(2));
                hasLL = true;
            }

            // We should have a lat long by now.
            assert hasLL;

            // Lets do some handling, yay!
            dispatchLatLng(lat, lng);

            return true;
        } else if (geoLocation.getHost().contains("google.")) {
            // Google Maps
            double lat = 0, lng = 0;
            boolean hasLL = false;

            if (geoLocation.getPath().startsWith("/maps/")) {
                // New style maps URL
                // www.google.com/maps/
                List<String> bits = geoLocation.getPathSegments();
                for (String bit : bits) {
                   if (bit.startsWith("@")) {
                       // Geolocation component
                       String[] loc_tokens = bit.substring(1).split(",");
                       lat = Double.valueOf(loc_tokens[0]);
                       lng = Double.valueOf(loc_tokens[1]);
                       hasLL = true;
                   }
                }
            } else {
                // Old style maps URL
                // maps.google.com/maps; maps.google.com/

                String q = geoLocation.getQueryParameter("q");
                if (q != null) {
                    Matcher llMatcher = LL_PATTERN.matcher(q);
                    if (llMatcher.find()) {
                        lat = Double.valueOf(llMatcher.group(1));
                        lng = Double.valueOf(llMatcher.group(2));
                        hasLL = true;
                    }
                }

                if (!hasLL) {
                    q = geoLocation.getQueryParameter("ll");
                    if (q != null) {
                        Matcher llMatcher = LL_PATTERN.matcher(q);
                        if (llMatcher.find()) {
                            lat = Double.valueOf(llMatcher.group(1));
                            lng = Double.valueOf(llMatcher.group(2));
                            hasLL = true;
                        }
                    }
                }

            }

            if (hasLL) {
                dispatchLatLng(lat, lng);
                return true;
            } else {
                Log.i(TAG, "Error handling gmaps URL " + geoLocation.toString());
                showToast(R.string.broadcast_not_supported);
                return false;
            }

        }


        // We don't support non-geo links yet.
        showToast(R.string.broadcast_not_supported);
        return false;
    }


    /**
     * Dispatches a request with a known decimal latitude and longitude.
     * @param lat Decimal degrees of latitude, South is negative.
     * @param lng Decimal degrees of longitude, West is negative.
     */
    private void dispatchLatLng(double lat, double lng) {
        dispatchLatLng((long)(lat * Math.pow(10, 6)), (long)(lng * Math.pow(10,6)));
    }

    /**
     * Dispatches a request with a known decimal latitude and longitude, with the values
     * multiplied by 10**6.
     * @param latE6 0.000001 degrees of latitude, South is negative.
     * @param lngE6 0.000001 degrees of longitude, West is negative.
     */
    private void dispatchLatLng(long latE6, long lngE6) {
        Log.i(TAG, "Dispatching " + latE6 + "," + lngE6);

        final Frogjump apiService = Util.getApiServiceHandle(null);

        // Lets make a callback to the web service
        FrogjumpApiMessagesPostMessageRequest req = new FrogjumpApiMessagesPostMessageRequest();
        req.setGcmToken(gcm_token);
        req.setGroupKey(group_key);
        req.setLatE6(latE6);
        req.setLngE6(lngE6);

        (new AsyncTask<FrogjumpApiMessagesPostMessageRequest, Void, FrogjumpApiMessagesGroupResponse>() {
            @Override
            protected FrogjumpApiMessagesGroupResponse doInBackground(FrogjumpApiMessagesPostMessageRequest... requests) {
                FrogjumpApiMessagesGroupResponse res = null;

                try {
                    res = apiService.group().post(requests[0]).execute();
                } catch (IOException ex) {
                    Log.d(TAG, ex.getMessage(), ex);
                }
                return res;
            }

            protected void onPostExecute(FrogjumpApiMessagesGroupResponse res) {
                if (res == null) {
                    // Error happened
                    Log.i(TAG, "Error sending message");
                    showToast(R.string.broadcast_fail);
                } else {
                    // All ok
                    Log.i(TAG, "Send message!");
                    showToast(R.string.broadcast_success);
                }
            }
        }).execute(req);
    }

    private void showToast(int resId) {
        String message = getString(resId);
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

}
