package org.eztarget.realay.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.eztarget.realay.Constants;
import org.eztarget.realay.managers.Bouncer;
import org.eztarget.realay.managers.SessionMainManager;

/**
 * Created by michel on 05/01/15.
*/
public class PenaltyReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = PenaltyReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {
        final String actionReceived = intent.getAction();

        if (!SessionMainManager.getInstance().didLogin()) {
            Log.w(LOG_TAG, "Received " + actionReceived + " outside of Session.");
            return;
        }

        final Bouncer bouncer = Bouncer.from(context);
        if (actionReceived.equals(Constants.ACTION_WARN_HEARTBEAT)) {
            bouncer.warn(true, intent.getIntExtra(Constants.EXTRA_WARN_REASON, -1));
            return;
        }

        if (actionReceived.equals(Constants.ACTION_PENALTY_EVENT)) {
            final int event = intent.getIntExtra(Constants.EXTRA_LOCATION_EVENT, -1);
            switch (event) {
                case Constants.EVENT_LOCATION_ISSUE_STARTED:
                    bouncer.warn(true, Bouncer.REASON_LOCATION);
                    return;
                case Constants.EVENT_LOCATION_ISSUE_RESOLVED:
                    bouncer.resetSession();
                    bouncer.cancelNotifications();
                    bouncer.resetDialogs();
                    return;
                default:
                    Log.w(LOG_TAG, "Unknown code received: " + event);
            }
        }
    }

}
