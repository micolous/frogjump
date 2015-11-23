package au.id.micolous.frogjump;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;

import com.appspot.frogjump_cloud.frogjump.Frogjump;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

/**
 * Created by michael on 24/11/15.
 */
public class Util {
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

        // DEBUG
        //frogjump.setRootUrl("http://172.20.0.238:8080/_ah/api");

        return frogjump.build();
    }
}
