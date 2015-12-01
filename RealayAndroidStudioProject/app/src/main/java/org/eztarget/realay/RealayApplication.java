package org.eztarget.realay;

import android.app.Application;
import android.util.Log;

import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionMainManager;

/**
 * Created by michel on 05/08/15.
 *
 */
public class RealayApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Remove old warning and kick data.
        PreferenceHelper.from(getApplicationContext()).resetBouncerAttributes();

        // Start fetching the Location as early as possible.
        LocationWatcher.from(this).getLocation(true);
        LocationWatcher.from(this).adjustLocationUpdates();
    }

    @Override
    public void onTerminate() {
        Log.d(RealayApplication.class.getSimpleName(), "Terminating.");

        final SessionMainManager sessionManager = SessionMainManager.getInstance();
        if (sessionManager.didLogin()) sessionManager.dispatchLeaveAction(getApplicationContext());

        super.onTerminate();
    }

}
