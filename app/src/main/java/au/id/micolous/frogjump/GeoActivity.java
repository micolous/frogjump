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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
/*
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.urlshortener.Urlshortener;
import com.google.api.services.urlshortener.model.Url;
*/

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    protected static final Pattern URL_PATTERN = Pattern.compile("(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))");
    //protected String mUrlShortenerClientId;

    public static final String TAG = "GeoActivity";
    private String gcm_token;
    private int group_id;
    private String group_key;
    //private GoogleApiClient mGoogleApiClient;
    //private String google_account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo);

        /*
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            mUrlShortenerClientId = ai.metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {} // do nothing, shouldn't happen
        */

        // Start by seeing if we have a group key available.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //google_account = sharedPreferences.getString(ApplicationPreferences.GOOGLE_ACCOUNT, null);

        gcm_token = sharedPreferences.getString(ApplicationPreferences.GCM_TOKEN, null);
        group_id = sharedPreferences.getInt(ApplicationPreferences.GROUP_ID, 0);
        group_key = sharedPreferences.getString(ApplicationPreferences.GROUP_KEY, null);

        if (gcm_token == null || group_key == null) {
            // drop out, we can't continue.
            Log.i(TAG, "Cannot find group_key, gcm_token, or otherwise not registered");
            showToast(R.string.broadcast_login_req);
            finish();
            return;
        }

        /*
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addApi(Places.GEO_DATA_API)
                .build();
        */

        resolveIntent(getIntent());
        finish();
    }

    @Override
    public void onNewIntent(Intent intent) {
        resolveIntent(intent);
    }

    private boolean resolveIntent(Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            // Lets try to get the latE6 and lngE6 we need
            final Uri geoLocation = intent.getData();
            return resolveUrl(geoLocation);

        } else if (intent.getAction().equals(Intent.ACTION_SEND)) {
            // Text links
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);

            // First, try to find a URL in the message.
            Matcher m = URL_PATTERN.matcher(message);
            if (m.find()) {
                // We have something to try to work with.
                // Grab out the URL and try to parse.
                Uri uri = Uri.parse(m.group(1));

                // Throw it at the url handler
                return resolveUrl(uri);
            }
        }


        // We don't support non-geo links yet.
        showToast(R.string.broadcast_not_supported);
        return false;
    }

    private boolean resolveUrl (Uri geoLocation) {
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
            LatLng ll = null;

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

                    // Handle Type 3
                    ll = LatLng.parseFromString(q);

                    if (ll == null) {
                        // Type 4, we can't handle
                        Log.i(TAG, "Cannot handle Type 4 (geocoder required) geo URIs");
                        showToast(R.string.broadcast_not_supported);
                        return false;
                    }

                }
            }

            if (ll == null) {
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
                ll = LatLng.parseFromString(q);

                if (ll == null) {
                    // Something weird happened...
                    Log.i(TAG, "Error handling type 1/2 location " + q);
                    showToast(R.string.broadcast_not_supported);
                    return false;
                }
            }

            // Lets do some handling, yay!
            dispatchLatLng(ll);

            return true;
            /*
        } else if (geoLocation.getHost().equalsIgnoreCase("goo.gl")) {
            // goo.gl URL shortener, needed for google maps.
            if (!expandGooglAndRedispatch(geoLocation)) {
                // Something is really bad
                showToast(R.string.broadcast_not_supported);
                return false;
            }

            // We probably succeeded in setting up for a second round
            return true;
            */
        } else if (geoLocation.getHost().contains("google.")) {
            // Google Maps
            LatLng ll = null;

            if (geoLocation.getPath().startsWith("/maps/")) {
                // New style maps URL
                // www.google.com/maps/
                List<String> bits = geoLocation.getPathSegments();
                for (String bit : bits) {
                    if (bit.startsWith("@")) {
                        // Geolocation component
                        String[] loc_tokens = bit.substring(1).split(",");
                        ll = LatLng.parseFromStringArray(loc_tokens);
                    }
                }
            } else {
                // Old style maps URL
                // maps.google.com/maps; maps.google.com/

                String q = geoLocation.getQueryParameter("q");
                if (q != null) {
                    ll = LatLng.parseFromString(q);
                }

                if (ll == null) {
                    q = geoLocation.getQueryParameter("ll");
                    if (q != null) {
                        ll = LatLng.parseFromString(q);
                    }
                }

                if (ll == null) {
                    // Used for Telegram's navigation function
                    // eg: http://maps.google.com/?saddr=lat,lng&daddr=lat,lng
                    q = geoLocation.getQueryParameter("daddr");
                    if (q != null) {
                        ll = LatLng.parseFromString(q);
                    }
                }

                // Places API can't handle cid= parameters. :(
                /*
                if (ll == null) {
                    // Google Places API, used when Google Maps has a pin which matches something
                    // with a Page.
                    // eg: http://maps.google.com/?cid=123123123123
                    q = geoLocation.getQueryParameter("cid");
                    if (q != null) {
                        // Lets look this up
                        Log.i(TAG, "Need to look up " + q + " with Places API");
                        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, q);
                        placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                            @Override
                            public void onResult(PlaceBuffer places) {
                                if (!places.getStatus().isSuccess() || places.getCount() < 1) {
                                    Log.i(TAG, "Failed to get a successful position of place");
                                    showToast(R.string.api_client_fail);
                                } else {
                                    final Place place = places.get(0);
                                    Log.i(TAG, "Got place location, " + place.getLatLng().toString());
                                    dispatchLatLng(place.getLatLng());
                                }

                                places.release();
                            }

                        });

                        // This is actually a lie, but we'll catch the result of this lookup in
                        // another thread.
                        return true;
                    }
                }
                */
            }

            if (ll != null) {
                dispatchLatLng(ll);
                return true;
            } else {
                Log.i(TAG, "Error handling gmaps URL " + geoLocation.toString());
                showToast(R.string.broadcast_not_supported);
                return false;
            }
        }

        showToast(R.string.broadcast_not_supported);
        return false;
    }

    private void dispatchLatLng(LatLng latLng) {
        dispatchLatLng(latLng.getLatitude(), latLng.getLongitude());
    }

    /*
    private void dispatchLatLng(com.google.android.gms.maps.model.LatLng latLng) {
        dispatchLatLng(latLng.latitude, latLng.longitude);
    }
    */

    /**
     * Dispatches a request with a known decimal latitude and longitude.
     * @param lat Decimal degrees of latitude, South is negative.
     * @param lng Decimal degrees of longitude, West is negative.
     */
    private void dispatchLatLng(double lat, double lng) {
        dispatchLatLng((long) (lat * Math.pow(10, 6)), (long) (lng * Math.pow(10, 6)));
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

        Bundle msg = new Bundle();
        // Action
        msg.putString("a", "Send");
        // client Version
        msg.putString("v", Integer.toString(Util.getVersionCode()));
        // Group id
        msg.putString("g", Integer.toString(group_id));
        // Y (latitude)
        msg.putString("y", Long.toString(latE6));
        // X (longitude)
        msg.putString("x", Long.toString(lngE6));

        Util.sendGcmMessage(msg);

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

    /*
     * Expands goo.gl links and rethrows the GeoActivity asynchronously.
     * @param input Input goo.gl URL
     * @return false if the task has definitely failed, true if it might have succeeded.
     *
    private boolean expandGooglAndRedispatch(Uri input) {
        final GeoActivity this_activity = this;
        if (!input.getHost().equalsIgnoreCase("goo.gl")) {
            return false;
        }
        GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(
                getApplicationContext(), "server:client_id:" + mUrlShortenerClientId)
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(google_account);

        Urlshortener.Builder builder = new Urlshortener.Builder(
                AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), credential);
        builder.setApplicationName(getPackageName());
        Urlshortener urlshortener = builder.build();


        Urlshortener.Url.Get request;
        try {
            request = urlshortener.url().get(input.toString());
        } catch (IOException ex) {
            Log.d(TAG, "Caught IOException in urlshortener", ex);
            return false;
        }

        (new AsyncTask<Urlshortener.Url.Get, Void, Url>() {
            @Override
            protected Url doInBackground(Urlshortener.Url.Get... gets) {
                Url res = null;

                try {
                    res = gets[0].execute();
                } catch (IOException ex) {
                    Log.d(TAG, ex.getMessage(), ex);
                }
                return res;
            }

            protected void onPostExecute(Url res) {
                if (res == null) {
                    // Error happened
                    Log.i(TAG, "Failed to look up url on shortener");
                } else {
                    // Looks ok, lets start processing

                    if (!res.getStatus().equals("OK")) {
                        // Maybe malware? Or rate limited?  Drop out now.
                        Log.i(TAG, "urlshortener said this url was status=" + res.getStatus());
                    }

                    Uri u = Uri.parse(res.getLongUrl());

                    // Lets rethrow this, because we are async!
                    Intent i = new Intent(this_activity, GeoActivity.class);
                    i.setData(u);
                    startActivity(i);
                }
            }
        }).execute(request);

        return true;
    }
    */

    private void showToast(int resId) {
        String message = getString(resId);
        Context context = getApplicationContext();
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed, error code = " + connectionResult.getErrorCode());
        showToast(R.string.api_client_fail);
    }

}
