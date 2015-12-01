
package org.eztarget.realay.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionMainManager;

/**
 * This Receiver class is designed to listen for system boot and shutdown.
 * <p/>
 * If the App Preferences say that the background updates are authorized,
 * the Room List is kept up to date through this Receiver.
 * <p/>
 * Shutting down the phone while in a Session, the "Leave" Action will be dispatched.
 */
public class DeviceStateReceiver extends BroadcastReceiver {

    private static final String TAG = DeviceStateReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (action.equals("android.intent.action.BOOT_COMPLETED")) {

            // Start Location Updates.
            // The method verifies the User Preferences to only update in the background if allowed.
            // Location Updates trigger the Room List Update Service.
            LocationWatcher.from(context).adjustLocationUpdates();

        } else if (action.equals("android.intent.action.ACTION_SHUTDOWN")
                || action.equals("android.intent.action.QUICKBOOT_POWEROFF")) {

            final SessionMainManager sessionManager = SessionMainManager.getInstance();
            if (sessionManager.didLogin()) {
                sessionManager.dispatchLeaveAction(context);
            }
        }

    }

}