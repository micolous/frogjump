package au.id.micolous.frogjump;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Handles GCM registration.
 */
public class RegistrationIntentService extends IntentService {
    private static final String TAG = "RegistrationIS";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String gcm_token = instanceID.getToken(getString(R.string.gcm_defaultSenderId), GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(TAG, "GCM Registration Token: " + gcm_token);
            Log.i(TAG, "GCM App ID: " + getString(R.string.gcm_defaultSenderId));

            String token = GoogleCloudMessaging.getInstance(this).register(getString(R.string.gcm_defaultSenderId));
            Log.i(TAG, "Registered token: " + token);
            sendRegistrationToService(token);

            sharedPreferences.edit()
                    .putBoolean(ApplicationPreferences.SENT_TOKEN_TO_SERVER, true)
                    .putString(ApplicationPreferences.GCM_TOKEN, token)
                    .apply();
        } catch (Exception ex) {
            Log.d(TAG, "Failed to complete token refresh", ex);
            sharedPreferences.edit()
                    .putBoolean(ApplicationPreferences.SENT_TOKEN_TO_SERVER, false)
                    .putString(ApplicationPreferences.GCM_TOKEN, null)
                    .apply();
        }

        Intent registrationComplete = new Intent(ApplicationPreferences.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    private void sendRegistrationToService(String token) {

    }

}
