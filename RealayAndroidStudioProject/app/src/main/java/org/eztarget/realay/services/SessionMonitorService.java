package org.eztarget.realay.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import org.eztarget.realay.managers.Bouncer;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionActionsManager;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.utils.APIHelper;

public class SessionMonitorService extends IntentService {

    protected static String TAG = SessionMonitorService.class.getSimpleName();

    public SessionMonitorService() {
        super(TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final Context context = getApplicationContext();
        APIHelper.getActions(context);

        if (SessionActionsManager.from(getApplicationContext()).didTimeout()) {
            Bouncer.from(context).kick(Bouncer.REASON_TIMEOUT, TAG);
            return;
        }

        if (SessionMainManager.getInstance().doDeepUpdates()) {
            APIHelper.getSessionUsers(context);
            LocationWatcher.from(context).didEnableProvider();
        }

        if (SessionMainManager.getInstance().didReachEndDate()) {
            Bouncer.from(context).warn(false, Bouncer.REASON_SESSION_OVER);
        }
    }
}