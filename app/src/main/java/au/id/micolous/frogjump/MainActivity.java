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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import com.google.zxing.integration.android.IntentIntegrator;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    public static final String TAG = "MainActivity";

    private Spinner spnNavigationMode;
    private NavigationMode navigationMode;
    private int group_id;
    private TextView lblGroupId;
    private boolean hasLastDestination = false;
    private int lastLatE6 = 0;
    private int lastLngE6 = 0;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;


    public enum NavigationMode {
        OFF (0),
        DRIVING (1),
        DRIVING_AVOID_TOLLS (2),
        CYCLING (3),
        WALKING (4),
        CROW_FLIES (5),

        // This option is not shown in the UI, it is used for "recall" function only.
        SHOW_MAP (10000);

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
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        group_id = sharedPreferences.getInt(ApplicationPreferences.GROUP_ID, 0);

        if (group_id == 0) {
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

        updateLastDestination();

        // refresh last location data
        Bundle bundle = new Bundle();
        bundle.putString("g", Integer.toString(group_id));
        Util.sendGcmMessage("peek", bundle);

        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (s.equals(ApplicationPreferences.LAST_X) || s.equals(ApplicationPreferences.LAST_Y)) {
                    // This will fire twice, but not a big issue.
                    updateLastDestination();
                }
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        preferenceChangeListener = null;
        super.onDestroy();

    }

    private void updateLastDestination() {
        if (sharedPreferences.contains(ApplicationPreferences.LAST_X) && sharedPreferences.contains(ApplicationPreferences.LAST_Y)) {
            hasLastDestination = true;
            lastLatE6 = sharedPreferences.getInt(ApplicationPreferences.LAST_Y, 0);
            lastLngE6 = sharedPreferences.getInt(ApplicationPreferences.LAST_X, 0);
        }

        findViewById(R.id.btnRecall).setEnabled(hasLastDestination);
    }

    private static String formatGroupId(int group_id) {
        String group_id_s = String.format("%1$09d", group_id);
        group_id_s = group_id_s.substring(0, 3) + " " + group_id_s.substring(3, 6) + " " + group_id_s.substring(6);
        return group_id_s;
    }

    public void onBtnRecallClick(View view) {
        if (hasLastDestination) {
            updateLastDestination();

            final Context context = this;

            // Show a prompt about what to do
            AlertDialog.Builder updateDialog = new AlertDialog.Builder(this);
            updateDialog.setTitle(R.string.recall_title);
            updateDialog.setMessage(Util.formatLatLngE6(lastLatE6, lastLngE6));
            updateDialog.setPositiveButton(R.string.recall_positive, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show a map of the location
                    Util.navigateTo(lastLatE6, lastLngE6, NavigationMode.SHOW_MAP, context);
                }
            });

            if (navigationMode == NavigationMode.OFF) {
                updateDialog.setNegativeButton(R.string.recall_negative_unavailable, null);
            } else {
                updateDialog.setNegativeButton(R.string.recall_negative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Navigate to location
                    Util.navigateTo(lastLatE6, lastLngE6, navigationMode, context);

                }
            });
            }
            updateDialog.setCancelable(true);
            updateDialog.show();
        }
    }



    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        navigationMode = NavigationMode.getById(pos);
        // Check that the package is installed
        if (navigationMode != NavigationMode.OFF) {
            if (navigationMode == NavigationMode.CROW_FLIES) {
                // Check for GPS Status and Toolbox
                if (!Util.promptForInstall(Util.GPS_STATUS, this)) {
                    navigationMode = NavigationMode.OFF;
                }
            } else {
                // Check for Google Maps
                if (!Util.promptForInstall(Util.GOOGLE_MAPS, this)) {
                    navigationMode = NavigationMode.OFF;
                }
            }

            if (navigationMode == NavigationMode.OFF) {
                // It has been turned off due to no package installed
                spnNavigationMode.setSelection(navigationMode.getId());
            }
        }

        sharedPreferences.edit()
                .putInt(ApplicationPreferences.NAVIGATION_MODE, navigationMode.getId())
                .apply();

        Log.i(TAG, "NavigationMode = " + navigationMode);
    }

    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void partGroup() {
        Util.sendGcmMessage("part");

        // Clear the cache of the last destination.
        sharedPreferences.edit()
                .remove(ApplicationPreferences.LAST_X)
                .remove(ApplicationPreferences.LAST_Y)
                .apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String group_url = "http://frjmp.xyz/g/" + group_id;

        switch (item.getItemId()) {
            case R.id.part:
            case android.R.id.home:
                partGroup();
                finish();
                return true;

            case R.id.qr:
                IntentIntegrator qrIntent = new IntentIntegrator(this);
                qrIntent.shareText(group_url);
                return true;

            case R.id.share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, group_url);
                startActivity(Intent.createChooser(intent, "Share URL"));
                break;

        }
        return false;
    }

    public void onShareMarkerClick(View view) {
        // We don't handle this ourselves, direct to our website to handle this bit.
        Uri u = Uri.parse("https://micolous.github.io/frogjump/map");
        startActivity(new Intent(Intent.ACTION_VIEW, u));
    }

}
