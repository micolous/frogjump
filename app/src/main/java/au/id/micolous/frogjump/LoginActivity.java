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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.appspot.frogjump_cloud.frogjump.Frogjump;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesCreateGroupRequest;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesGroupResponse;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesJoinGroupRequest;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesPartGroupRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;

public class LoginActivity extends AppCompatActivity {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "LoginActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private TextView lblStatus;
    private Frogjump apiService;
    private EditText txtGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        lblStatus = (TextView) findViewById(R.id.lblStatus);
        txtGroupId = (EditText) findViewById(R.id.txtGroupId);

        sensitize(false);

        txtGroupId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_GO) {
                    onBtnJoinGroupClick(textView);
                }
                return false;
            }
        });

        apiService = getApiServiceHandle(null);

        setupUI();

        lblStatus.setText("Connecting to Frogjump API...");

        String gcm_token = sharedPreferences.getString(ApplicationPreferences.GCM_TOKEN, null);
        if (gcm_token != null) {
            FrogjumpApiMessagesPartGroupRequest partGroupRequest = new FrogjumpApiMessagesPartGroupRequest();
            partGroupRequest.setGcmToken(gcm_token);

            (new AsyncTask<FrogjumpApiMessagesPartGroupRequest, Void, Void>() {

                @Override
                protected Void doInBackground(FrogjumpApiMessagesPartGroupRequest... reqs) {
                    try {
                        apiService.group().part(reqs[0]).execute();
                    } catch (IOException ex) {
                        Log.d(TAG, ex.getMessage(), ex);
                    }
                    return null;
                }
            }).execute(partGroupRequest);
        }

        lblStatus.setText("Connecting to Cloud Messaging...  If this takes more than a few seconds, rotate your device to try again.");

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences.getBoolean(ApplicationPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    lblStatus.setText("Acquired token from Cloud Messaging.");
                    sensitize(true);
                } else {
                    lblStatus.setText("Could not acquire token from Cloud Messaging. Check network connection.");
                    sensitize(false);
                }
            }
        };

        if (isPlayServicesAvailable()) {
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

    }

    private void setupUI() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        int last_group = sharedPreferences.getInt(ApplicationPreferences.GROUP_ID, 0);
        if (last_group != 0) {
            txtGroupId.setText(String.format("%1$09d", last_group));
        }

        lblStatus.setText("");

    }

    @Override
    public void onRestart() {
        setupUI();
        super.onRestart();
    }


    public static Frogjump getApiServiceHandle(@Nullable GoogleAccountCredential credential) {
      // Use a builder to help formulate the API request.
      Frogjump.Builder frogjump = new Frogjump.Builder(AndroidHttp.newCompatibleTransport(),
          new AndroidJsonFactory(), credential);

      // DEBUG
      //frogjump.setRootUrl("http://172.20.0.238:8080/_ah/api");

      return frogjump.build();
    }


    /**
     * Enables or disables all UI elements.
     * @param status Should the controls be enabled?
     */
    private void sensitize(boolean status) {
        findViewById(R.id.txtGroupId).setEnabled(status);
        findViewById(R.id.btnCreateGroup).setEnabled(status);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver, new IntentFilter(ApplicationPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    private boolean isPlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                lblStatus.setText("Google Play Services has a problem. :(");
            } else {
                lblStatus.setText("Google Play Services is not available on this device.");
            }
            return false;
        }

        return true;
    }

    public void onBtnJoinGroupClick(View view) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Validate the group ID first
        int group_id = Integer.parseInt(txtGroupId.getText().toString());
        if (group_id > 999999999 || group_id < 0) {
            // fail
            Log.i(TAG, "group_id is invalid");
            return;
        }

        // 0-pad group ID
        String group_id_s = String.format("%1$09d", group_id);

        // Set the value of the input back to the value we actually used
        txtGroupId.setText(group_id_s);
        lblStatus.setText("Attempting to join group " + group_id_s);

        // We need to join a group ID.
        String token = sharedPreferences.getString(ApplicationPreferences.GCM_TOKEN, null);
        if (token == null) {
            // bail
            lblStatus.setText("No GCM token available");
            return;
        }

        // Lets make a callback to the web service
        FrogjumpApiMessagesJoinGroupRequest req = new FrogjumpApiMessagesJoinGroupRequest();
        req.setGcmToken(token);
        req.setGroupId(group_id_s);

        (new AsyncTask<FrogjumpApiMessagesJoinGroupRequest, Void, FrogjumpApiMessagesGroupResponse>() {
            @Override
            protected FrogjumpApiMessagesGroupResponse doInBackground(FrogjumpApiMessagesJoinGroupRequest... requests) {
                FrogjumpApiMessagesGroupResponse res = null;

                try {
                    res = apiService.group().join(requests[0]).execute();
                } catch (IOException ex) {
                    Log.d(TAG, ex.getMessage(), ex);
                }
                return res;
            }

            protected void onPostExecute(FrogjumpApiMessagesGroupResponse res) {
                if (res == null) {
                    // Error happened
                    lblStatus.setText("Error getting token from Frogjump API");
                    return;
                }

                // Use the response
                if (res.getSuccess()) {
                    lblStatus.setText("Group join request sent. Waiting to be added to group...");
                } else {
                    lblStatus.setText("Group join failed. Maybe the Group ID is wrong or expired?");
                }
            }
        }).execute(req);
    }

    public void onBtnAboutClick(View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void onBtnWebsiteClick(View view) {
        Uri u = Uri.parse("https://micolous.github.io/frogjump/");
        startActivity(new Intent(Intent.ACTION_VIEW, u));
    }

    public void onBtnCreateGroupClick(View view) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // We need to request a new group ID from the server.
        String token = sharedPreferences.getString(ApplicationPreferences.GCM_TOKEN, null);
        if (token == null) {
            // bail
            lblStatus.setText("No GCM token available");
            return;
        }

        // Lets make a callback to the web service
        FrogjumpApiMessagesCreateGroupRequest req = new FrogjumpApiMessagesCreateGroupRequest();
        req.setGcmToken(token);

        lblStatus.setText("Attempting to create new group...");

        (new AsyncTask<FrogjumpApiMessagesCreateGroupRequest, Void, FrogjumpApiMessagesGroupResponse>() {
            @Override
            protected FrogjumpApiMessagesGroupResponse doInBackground(FrogjumpApiMessagesCreateGroupRequest... requests) {
                FrogjumpApiMessagesGroupResponse res = null;

                try {
                    res = apiService.group().create(requests[0]).execute();
                } catch (IOException ex) {
                    Log.d(TAG, ex.getMessage(), ex);
                }
                return res;
            }

            protected void onPostExecute(FrogjumpApiMessagesGroupResponse res) {
                if (res == null) {
                    // Error happened
                    lblStatus.setText("Error getting token from Frogjump API");
                    return;
                }

                // Use the response
                if (res.getSuccess()) {
                    lblStatus.setText("Group created. Waiting to be added to group...");
                } else {
                    lblStatus.setText("Group creation failed.");
                }
            }
        }).execute(req);
    }

}
