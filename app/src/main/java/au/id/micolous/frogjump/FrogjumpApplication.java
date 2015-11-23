package au.id.micolous.frogjump;

import android.app.Application;

/**
 * Shares global state between application components.
 */
public class FrogjumpApplication extends Application {
    private static FrogjumpApplication app;

    public FrogjumpApplication() {
        super();
        app = this;
    }

    public static FrogjumpApplication getInstance() {
        return app;
    }

}
