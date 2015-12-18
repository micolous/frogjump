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
import android.preference.PreferenceManager;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "LoginActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private TextView lblStatus;
    private EditText txtGroupId;
    private boolean auto_join = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);



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

        setupUI();

        // Lets see if there was a Web Intent fired to start us up
        resolveIntent(getIntent());

        lblStatus.setText("Connecting to Frogjump API...");

        Util.updateCheck(this);
        lblStatus.setText("Connecting to Cloud Messaging...  If this takes more than a few seconds, rotate your device to try again.");

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences.getBoolean(ApplicationPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    lblStatus.setText("Acquired token from Cloud Messaging.");
                    // Leave any group we are a member of.
                    Util.sendGcmMessage("part");

                    sensitize(true);

                    // Check if there is an auto-join for us
                    if (auto_join) {
                        auto_join = false;
                        onBtnJoinGroupClick(txtGroupId);
                    }
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



    @Override
    public void onNewIntent(Intent intent) {
        resolveIntent(intent);
    }

    private boolean resolveIntent(Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_VIEW)) {
            final Uri intentUri = intent.getData();

            if (intentUri.getHost().endsWith("frjmp.xyz")) {
                // Known host, figure out our action!
                List<String> pathSegments = intentUri.getPathSegments();
                if (pathSegments.get(0).equalsIgnoreCase("g")) {
                    // Join Group (G)
                    // Parameter 1 is the group number, prefill the box with it.

                    // Make sure it is a number
                    int group_id;
                    try {
                        group_id = Integer.valueOf(pathSegments.get(1));
                    } catch (NumberFormatException ex) {
                        return false;
                    }

                    // Now set the text field
                    txtGroupId.setText(String.format("%1$09d", group_id));

                    // Make sure futures are set to auto-join
                    auto_join = true;
                }
            }

        }

        return false;
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

    /**
     * Enables or disables all UI elements.
     * @param status Should the controls be enabled?
     */
    private void sensitize(boolean status) {
        findViewById(R.id.txtGroupId).setEnabled(status);
        findViewById(R.id.btnCreateGroup).setEnabled(status);
        findViewById(R.id.btnJoinGroup).setEnabled(status);
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
        // Validate the group ID first
        int group_id;
        try {
            group_id = Integer.parseInt(txtGroupId.getText().toString());
        } catch (NumberFormatException ex) {
            Log.i(TAG, "group_id is not a number");
            return;
        }

        if (group_id > 999999999 || group_id <= 0) {
            // fail
            Log.i(TAG, "group_id is invalid");
            return;
        }

        // 0-pad group ID
        String group_id_s = String.format("%1$09d", group_id);

        // Set the value of the input back to the value we actually used
        txtGroupId.setText(group_id_s);
        lblStatus.setText("Attempting to join group " + group_id_s);

        Bundle message = new Bundle();
        message.putString("g", group_id_s);
        Util.sendGcmMessage("knock", message);
    }

    public void onBtnAboutClick(View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void onBtnWebsiteClick(View view) {
        Uri u = Uri.parse("https://micolous.github.io/frogjump/");
        startActivity(new Intent(Intent.ACTION_VIEW, u));
    }

    public void onBtnCreateGroupClick(View view) {
        lblStatus.setText("Attempting to create new group...");
        Util.sendGcmMessage("create");
    }

}
