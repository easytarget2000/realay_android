/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eztarget.realay.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import org.eztarget.realay.Constants;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.receivers.ConnectivityChangedReceiver;
import org.eztarget.realay.utils.DeviceStatusHelper;
import org.eztarget.realay.utils.APIHelper;

public class ActionSendService extends IntentService {

    private static String LOG_TAG = ActionSendService.class.getSimpleName();

    private static final long QUEUE_RETRY_INTERVAL = 60l * 1000l;

    protected AlarmManager mAlarmManager;

    protected PendingIntent mRetryIntent;

    public ActionSendService() {
        super(LOG_TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        final Intent retryIntent = new Intent(Constants.ACTION_RETRY_SENDING);
        mRetryIntent =
                PendingIntent.getBroadcast(this, 0, retryIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * {@inheritDoc}
     *
     * Sends a locally performed Action, e.g. sending a message, to the server;
     * If this fails, usually because there was no reception, adds it to the queue
     * and set an alarm to retry;
     *
     * Queries the Actions queue to see if there are pending Actions to be retried
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final Action action = intent.getParcelableExtra(Constants.EXTRA_ACTION);

        // If we're not connected then disable the retry Alarm,
        // enable the Connectivity Changed Receiver and add the new Action directly to the queue.
        // The Connectivity Changed Receiver will listen for when we connect to a network
        // and startWarningAlarm this service to retry the Actions.
        if (!DeviceStatusHelper.isConnected(getApplicationContext())) {
            // No connection, so do not trigger an alarm to retry until reconnected.
            mAlarmManager.cancel(mRetryIntent);
            PackageManager pacMan = getPackageManager();
            pacMan.setComponentEnabledSetting(
                    new ComponentName(this, ConnectivityChangedReceiver.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );

            // Add this Action to the queue and do not try sending anything.
            addToQueue(action);
            return;
        }

        // If this Intent came with an Action, add it locally and send it.
        if (action != null) {
            // Add it to the local DB first, so that it appears in the conversation immediately.
            // Assume the messages was sent. addToQueue() changes the queue-flag later, if needed.
            if (!ChatObjectContract.insertAction(getApplicationContext(), action, true)) {
                Log.e(LOG_TAG, "Could not insert Action: " + action.toString());
                return;
            }

            // Notify the change in the local DB to update the conversations.
            final Uri notifyUri = action.isPublic() ?
                    ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC :
                    ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE;
            getContentResolver().notifyChange(notifyUri, null);

            // If we could not send this Action, do not try to send the ones in the queue.
            // Assume the connection is not stable.
            if (!APIHelper.putAction(getApplicationContext(), action)) {
                addToQueue(action);
                return;
            }
        }

        // Try resending all Actions that are in the queue.
        if (sendQueuedActions(true) && sendQueuedActions(false)) {
            mAlarmManager.cancel(mRetryIntent);
            return;
        }

        // If there are still queued Actions, then set a non-waking alarm to retry them.
        final long triggerAtTime = System.currentTimeMillis() + QUEUE_RETRY_INTERVAL;
        mAlarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerAtTime, mRetryIntent);
    }

    private boolean addToQueue(final Action action) {
        if (action == null) return false;

        if (ChatObjectContract.insertAction(getApplicationContext(), action, false, true)) {
//            Log.d(LOG_TAG, "Set unsent flag for Action: " + action.toString());
            // Notify the change in the local DB to update the conversations.
            final Uri notifyUri = action.isPublic() ?
                    ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC :
                    ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE;
            getContentResolver().notifyChange(notifyUri, null);
            return true;
        }

        return false;
    }

    private boolean sendQueuedActions(final boolean doSendPublicActions) {
        // Select the corresponding Actions table
        // and query for entries that have the queue flag set.
        final Uri uri = doSendPublicActions ? ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC
                : ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE;
        final String selection = BaseColumns.MSG_IN_QUEUE + "=1";
        final String sortOrder = BaseColumns.MSG_TIME + " ASC";

        Cursor queueCursor = getContentResolver().query(uri, null, selection, null, sortOrder);

        if (queueCursor == null) {
            Log.e(LOG_TAG, "ERROR: queueCursor is null.");
            return false;
        }

        while (queueCursor.moveToNext()) {
            final Action queueAction = Action.buildAction(queueCursor);
            // If the message was sent successfully, update its entry to reset the flag.
//            Log.d(LOG_TAG, "Retrying: " + queueAction.toString());
            if (APIHelper.putAction(getApplicationContext(), queueAction)) {
                ChatObjectContract.insertAction(getApplicationContext(), queueAction, false, false);
//                Log.d(LOG_TAG, "Did send on retry: " + queueAction.toString());
            }
        }
        queueCursor.close();

        // Query the database again to see if all messages have been sent.
        Cursor checkCursor = getContentResolver().query(uri, null, selection, null, sortOrder);
        final boolean didSendAll = checkCursor.getCount() == 0;
        checkCursor.close();
//        Log.d(LOG_TAG, "sendQueuedActions(" + doSendPublicActions + ") returning " + didSendAll);
        return didSendAll;
    }


}