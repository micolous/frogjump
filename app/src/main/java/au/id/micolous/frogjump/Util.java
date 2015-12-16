package au.id.micolous.frogjump;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.appspot.frogjump_cloud.frogjump.Frogjump;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesProductVersionRequest;
import com.appspot.frogjump_cloud.frogjump.model.FrogjumpApiMessagesProductVersionResponse;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;

/**
 * Utility functions for Frogjump
 */
public class Util {
    public static final String TAG = "Util";

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

    public static Frogjump getApiServiceHandle(@Nullable GoogleAccountCredential credential) {
        // Use a builder to help formulate the API request.
        Frogjump.Builder frogjump = new Frogjump.Builder(AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), credential);

        frogjump.setApplicationName(getPackageInfo().packageName);

        // DEBUG
        //frogjump.setRootUrl("http://172.20.0.238:8080/_ah/api");

        return frogjump.build();
    }

    public static void updateCheck(final Frogjump apiService, final Activity activity) {
        FrogjumpApiMessagesProductVersionRequest productVersionRequest = new FrogjumpApiMessagesProductVersionRequest();
        productVersionRequest.setVersionCode((long) Util.getVersionCode());
        (new AsyncTask<FrogjumpApiMessagesProductVersionRequest, Void, FrogjumpApiMessagesProductVersionResponse>() {
            @Override
            protected FrogjumpApiMessagesProductVersionResponse doInBackground(FrogjumpApiMessagesProductVersionRequest... reqs) {
                FrogjumpApiMessagesProductVersionResponse res = null;
                try {
                    res = apiService.version(reqs[0]).execute();
                } catch (IOException ex) {
                    Log.d(TAG, ex.getMessage(), ex);
                }
                return res;
            }

            @Override
            protected void onPostExecute(FrogjumpApiMessagesProductVersionResponse productVersionResponse) {
                if (productVersionResponse != null) {
                    // We have a response, lets see if we need to prompt an update
                    if (productVersionResponse.getNewVersion()) {
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
                }
            }
        }).execute(productVersionRequest);

    }
}
