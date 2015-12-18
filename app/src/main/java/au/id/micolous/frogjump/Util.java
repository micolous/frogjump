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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

/**
 * Utility functions for Frogjump
 */
public class Util {
    public static final String TAG = "Util";
    private static final Random rng = new Random();

    public static int getVersionCode() {
        //return 1;
        PackageInfo info = getPackageInfo();
        return info.versionCode;
    }

    public static String getPackageName() {
        PackageInfo info = getPackageInfo();
        return info.packageName;
    }


    public static String getVersionString() {
        PackageInfo info = getPackageInfo();
        return String.format("%s (Build %s)", info.versionName, info.versionCode);
    }

    private static PackageInfo getPackageInfo() {
        try {
            FrogjumpApplication app = FrogjumpApplication.getInstance();
            return app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void newVersionAlert(final Activity activity) {
        // We have a new version available.  Prompt.
        AlertDialog.Builder updateDialog = new AlertDialog.Builder(activity);
        updateDialog.setTitle(R.string.update_available_title);
        updateDialog.setMessage(R.string.update_available_message);
        updateDialog.setPositiveButton(R.string.update_positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                } catch (ActivityNotFoundException anfe) {
                    // Hmm, market is not installed
                    Log.w(TAG, "Google Play is not installed; cannot install update");
                }
            }
        });
        updateDialog.setNegativeButton(R.string.update_negative, null);
        updateDialog.setCancelable(true);
        updateDialog.show();

    }

    public static void updateCheck(final Activity activity) {

        (new AsyncTask<Activity, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Activity... activities) {
                try {
                    Activity activity = activities[0];
                    String my_version = Integer.toString(getVersionCode());
                    URL url = new URL("https://micolous.github.io/frogjump/version.json");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    conn.setReadTimeout(10000);
                    conn.setConnectTimeout(15000);
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == 200) {
                        InputStream is = conn.getInputStream();
                        byte[] buffer = new byte[1024];
                        is.read(buffer);
                        JSONObject root = (JSONObject) new JSONTokener(new String(buffer, "US-ASCII")).nextValue();

                        if (root.has(my_version)) {
                            if (root.getBoolean(my_version)) {
                                // Definitely needs update.
                                Log.i(TAG, "New version required, explicit flag.");
                                return true;
                            }
                        } else {
                            // unlisted version, assume it is old.
                            Log.i(TAG, "New version required, not in list.");
                            return true;
                        }

                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error getting update info", ex);
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean needsUpdate) {
                if (needsUpdate)
                    newVersionAlert(activity);
            }
        }).execute(activity);
    }

    public static void sendGcmMessage(String action) {
        sendGcmMessage(action, null);
    }

    public static void sendGcmMessage(String action, @Nullable Bundle message_p) {
        final long message_id = rng.nextLong();
        final Bundle message;
        if (message_p == null) {
            message = new Bundle();
        } else {
            message = new Bundle(message_p);
        }

        message.putString("a", action);
        message.putString("v", Integer.toString(getVersionCode()));

        FrogjumpApplication app = FrogjumpApplication.getInstance();
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(app);

        final String gcm_to = app.getString(R.string.gcm_defaultSenderId) + "@gcm.googleapis.com";
        (new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    gcm.send(gcm_to, Long.toString(message_id), message);
                    return true;
                } catch (IOException ex) {
                    Log.e(TAG, "sendGcmMessage fail", ex);
                    return false;
                }
            }
        }).execute();
    }

    public static void showToast(Context context, int resId) {
        Looper.prepare();
        String message = context.getString(resId);
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.show();
    }
}
