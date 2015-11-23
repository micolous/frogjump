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

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.appspot.frogjump_cloud.frogjump.Frogjump;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesPartGroupRequest;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public static final String TAG = "MainActivity";

    private Spinner spnNavigationMode;
    private NavigationMode navigationMode;
    private int group_id;
    private String group_key;
    private TextView lblGroupId;
    private Frogjump apiService;

    public enum NavigationMode {
        OFF (0),
        DRIVING (1),
        DRIVING_AVOID_TOLLS (2),
        CYCLING (3),
        WALKING (4),
        CROW_FLIES (5);

        private final int id;
        NavigationMode(int id) {
            this.id = id;
        }

        public static NavigationMode getById(int id) {
            for (NavigationMode n : NavigationMode.values()) {
                if (n.id == id) {
                    return n;
                }
            }

            return NavigationMode.OFF;
        }

        public int getId() {
            return this.id;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        apiService = Util.getApiServiceHandle(null);

        group_id = sharedPreferences.getInt(ApplicationPreferences.GROUP_ID, 0);
        group_key = sharedPreferences.getString(ApplicationPreferences.GROUP_KEY, null);

        if (group_id == 0 || group_key == null) {
            Log.i(TAG, "Group ID and/or key could not be found in preferences store.");
            finish();
            return;
        }

        // Setup the navigation mode Spinner
        spnNavigationMode = (Spinner) findViewById(R.id.spnNavigationMode);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.navigation_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnNavigationMode.setAdapter(adapter);
        spnNavigationMode.setOnItemSelectedListener(this);
        navigationMode = NavigationMode.getById(sharedPreferences.getInt(ApplicationPreferences.NAVIGATION_MODE, 0));
        spnNavigationMode.setSelection(navigationMode.getId());

        // Show the group ID in the UI, this allows it to be shared
        lblGroupId = (TextView)findViewById(R.id.lblGroupId);
        lblGroupId.setText(formatGroupId(this.group_id));

    }

    private static String formatGroupId(int group_id) {
        String group_id_s = String.format("%1$09d", group_id);
        group_id_s = group_id_s.substring(0, 3) + " " + group_id_s.substring(3, 6) + " " + group_id_s.substring(6);
        return group_id_s;
    }

    /**
     * Prompts to install a package if it is not installed.
     * @param packageName Package name to check for.
     * @return True if the package was already installed, False otherwise.
     */
    private boolean promptForInstall(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
            return false;
        }
        return true;
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        navigationMode = NavigationMode.getById(pos);
        // Check that the package is installed
        if (navigationMode != NavigationMode.OFF) {
            if (navigationMode == NavigationMode.CROW_FLIES) {
                // Check for GPS Status and Toolbox
                if (!promptForInstall(GcmIntentService.GPS_STATUS)) {
                    navigationMode = NavigationMode.OFF;
                }
            } else {
                // Check for Google Maps
                if (!promptForInstall(GcmIntentService.GOOGLE_MAPS)) {
                    navigationMode = NavigationMode.OFF;
                }
            }

            if (navigationMode == NavigationMode.OFF) {
                // It has been turned off due to no package installed
                spnNavigationMode.setSelection(navigationMode.getId());
            }
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit()
                .putInt(ApplicationPreferences.NAVIGATION_MODE, navigationMode.getId())
                .apply();

        Log.i(TAG, "NavigationMode = " + navigationMode);
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void partGroup() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.part:
            case android.R.id.home:
                partGroup();
                finish();
                return true;
        }
        return false;
    }

}
