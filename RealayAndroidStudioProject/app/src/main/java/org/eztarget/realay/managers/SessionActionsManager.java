package org.eztarget.realay.managers;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.R;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.ChatObject;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.data.User;
import org.eztarget.realay.ui.PrivateConversationActivity;
import org.eztarget.realay.ui.PublicConversationActivity;
import org.eztarget.realay.ui.UserTabActivity;
import org.eztarget.realay.ui.utils.ImageLoader;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by michel on 10/12/14.
 *
 */
public class SessionActionsManager {

    private static SessionActionsManager instance = null;

    private Context mContext;

    private static final String TAG = SessionActionsManager.class.getSimpleName();

    private static final long PING_INTERVAL = 40L * 1000L;

    private static final long QUERY_INTERVAL_FAST = 2800L;

    private static final long QUERY_JITTER_MAX = 800L;

    private static final long INTERVAL_MS_SLOWEST = 12L * 1000L;

    private static final long INTERVAL_MS_IDLE = 45L * 1000L;

    private static final long INTERVAL_MS_SLOWER_DIF = 800L;

    private static final long TIME_SPAN_TO_IDLE_MS = 6L * 60L * 1000L;

    private PendingIntent mPendingIntent;

    private AlarmManager mAlarmMan;

    private String[] mSpamCache;

    private int mSpamCachePos = 0;

    private boolean mDidFirstQuery = false;

    private long mLastActionIdExt;

    private long mLastActionMillis;

    private long mLastQueryMillis;

    private long mLastPingMillis;

    private long mHeartbeatInterval = QUERY_INTERVAL_FAST;

    protected SessionActionsManager(final Context context) {
        mContext = context;
    }

    public static SessionActionsManager from(final Context context) {
        if (instance == null) {
            if (context == null) Log.w(TAG, "Initializer called without Context.");
            instance = new SessionActionsManager(context);
        }
        return instance;
    }

    public void prepareSession() {
        stopHeartbeat();
        mLastActionIdExt = 800l;
        mDidFirstQuery = false;
        mSpamCache = null;
        resetNotifications();
        mEditTextMemory = null;
    }

    public boolean setHeartbeatAlarm(long triggerAtMillis) {
        if (!SessionMainManager.getInstance().didLogin()) return false;

        if (mAlarmMan == null || mPendingIntent == null) initAlarmManager();

        stopHeartbeat();

        if (mAlarmMan == null) return false;
//        Log.d(TAG, "Heartbeat interval: " + mHeartbeatInterval + "ms");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mAlarmMan.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, mPendingIntent);
        } else {
            // Since 4.4, Alarms are no longer exact unless specified.
            mAlarmMan.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, mPendingIntent);
        }
        return true;
    }

    public void stopHeartbeat() {
        if (mAlarmMan == null || mPendingIntent == null) initAlarmManager();

        if (mAlarmMan != null && mPendingIntent != null) {
            mAlarmMan.cancel(mPendingIntent);
        }
    }

    private void initAlarmManager() {
        Intent alarmIntent = new Intent(Constants.ACTION_HEARTBEAT);
        mPendingIntent = PendingIntent.getService(
                mContext.getApplicationContext(),
                8881,
                alarmIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );

        if (mAlarmMan == null) {
            mAlarmMan = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        }
    }

    public long getLastReceivedActionId() {
        return mLastActionIdExt;
    }

    public void setLastReceivedAction(long id) {
        if (mLastActionIdExt < id) mLastActionIdExt = id;
        mLastActionMillis = System.currentTimeMillis();
    }

    public boolean didFirstQuery() {
        if (mDidFirstQuery) {
            return true;
        } else {
            mDidFirstQuery = true;
            return false;
        }
    }

    /**
     * @return True, if it is time to send a ping; see ActionQueryService;
     * A ping updates the last activity timestamp of a User to avoid timeout kicks.
     */
    public boolean doSendPing() {
        final long lastPingDifference = System.currentTimeMillis() - mLastPingMillis;
//        Log.d(TAG, "PingDif: " + lastPingDifference);
        return lastPingDifference > PING_INTERVAL;
    }

    /**
     * Stores the current time for timeout checks and stores
     * that at least one query has been done
     */
    public void acknowledgeQuery() {
        mDidFirstQuery = true;
        mLastQueryMillis = System.currentTimeMillis();
    }

    public void acknowledgePing() {
        final long now = System.currentTimeMillis();
        mLastPingMillis = now;
    }

    /**
     * Time interval in milliseconds in which an Action query has to be performed
     */
    private static final long TIMEOUT_INTERVAL_MILLIS = 20L * 60L * 1000L;

    /**
     * True, if the last Action query was more than TIMEOUT_INTERVAL_MILLIS ms ago
     */
    public boolean didTimeout() {
        if (!mDidFirstQuery) return false;
        if (!SessionMainManager.getInstance().didLogin()) return false;

        final long queryInterval = System.currentTimeMillis() - mLastQueryMillis;
        if (queryInterval > TIMEOUT_INTERVAL_MILLIS) {
            final String lastAck = new Date(mLastQueryMillis).toString();
            Log.w(TAG, "Timeout after: " + (queryInterval / 1000L) + "s. Last ack: " + lastAck);
            return true;
        } else {
            return false;
        }
    }

    public void increaseHeartbeatInterval() {
        // Check if enough time has passed to go into idle mode.
        final long now = System.currentTimeMillis();

        if (mHeartbeatInterval < INTERVAL_MS_IDLE) {
            if ((now - mLastActionMillis) > TIME_SPAN_TO_IDLE_MS) {
                mHeartbeatInterval = INTERVAL_MS_IDLE;
                setHeartbeatAlarm(now + mHeartbeatInterval);
                return;
            }
        }

        // Reduce the heartbeat interval by another step,
        // if it is not at the lowest, non-idle value yet.
        mHeartbeatInterval += INTERVAL_MS_SLOWER_DIF;
        if (mHeartbeatInterval > INTERVAL_MS_SLOWEST) mHeartbeatInterval = INTERVAL_MS_SLOWEST;

        // If the interval changed, restart the heartbeat.
        setHeartbeatAlarm(now + mHeartbeatInterval);
    }

    public void resetHeartbeatInterval() {
        mHeartbeatInterval = QUERY_INTERVAL_FAST + (long) (Math.random() * QUERY_JITTER_MAX);
        setHeartbeatAlarm(System.currentTimeMillis() + mHeartbeatInterval);
    }

    /*
    EditText Memory
     */

    private HashMap<Long, String> mEditTextMemory;

    public void setEditTextInput(final Long conversationId, final String text) {
        if (conversationId == null || text == null || text.length() < 0) return;

        if (mEditTextMemory == null) {
            mEditTextMemory = new HashMap<>();
        }
        mEditTextMemory.put(conversationId, text);
    }

    public String getEditTextInput(final Long conversationId) {
        if (conversationId == null || mEditTextMemory == null) {
            return "";
        } else if (mEditTextMemory.containsKey(conversationId)) {
            return mEditTextMemory.get(conversationId);
        } else {
            return "";
        }
    }

    /*
    Notifications
     */

    private static final int NOTIFICATION_DEFAULTS =
            NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE;

    private static final int NOTIFICATION_ID_PUB_MSG = 788888;

    private static final int NOTIFICATION_ID_PRV_MSG = 800000;

    private static final int NOTIFICATION_LIGHT_ON = 1000;

    private static final int NOTIFICATION_LIGHT_OFF = 2000;

    private long mForegroundConversationId = 0;

//    private boolean mDoNotifyPubMsg = false;
//
//    private boolean mDoNotifyPrvMsg = true;

    private ArrayList<Action> mPrvMsgNotificationQueue;

    private ArrayList<Action> mPubMsgNotificationQueue;

    public void setForegroundConversation(final long conversationId) {
        mForegroundConversationId = conversationId;
        cancelNotifications(conversationId);
    }

    public void addToNotificationQuery(final Action action) {
        if (!mDidFirstQuery) return;

        // Always add private messages because of internal "notification" labels
        // will never be deactivated and broadcastUnreadPrivateCount() uses this List.

        if (action.isPublic()) {
            final boolean doShowPublicNotifications;
            doShowPublicNotifications = PreferenceHelper.from(mContext).doShowPublicNotifications();
            if (action.isInfoMessage() || doShowPublicNotifications){
                if (mPubMsgNotificationQueue == null) mPubMsgNotificationQueue = new ArrayList<>();
                mPubMsgNotificationQueue.add(action);
            }
        } else {
            if (action.getSenderUserId() != mForegroundConversationId) {
                if (mPrvMsgNotificationQueue == null) mPrvMsgNotificationQueue = new ArrayList<>();
                mPrvMsgNotificationQueue.add(action);
            }
        }
    }

    public boolean hasUnreadMessages(final long userId) {
        if (!mDidFirstQuery) return false;

        if (mPrvMsgNotificationQueue == null || mPrvMsgNotificationQueue.isEmpty()) return false;
        for (final Action privateMessage : mPrvMsgNotificationQueue) {
            if (privateMessage.getSenderUserId() == userId) return true;
        }
        return false;
    }

    public void showNotifications(final boolean isCalledByPrivateMessage) {
        // Update the Notification icons inside of the app.
        broadcastUnreadPrivateCount();

        if (isCalledByPrivateMessage) {
            if (PreferenceHelper.from(mContext).doShowPrivateNotifications()) {
                showMessageNotifications(false);
            }
        } else {
            if (mForegroundConversationId != Action.PUBLIC_RECIPIENT_ID) {
                if (PreferenceHelper.from(mContext).doShowPublicNotifications()) {
                    showMessageNotifications(true);
                }
            }
        }
    }

    private static final int MAX_LAST_MSG = 4;

    private void showMessageNotifications(final boolean isPublicMessages) {
        // Get the amount of new messages and determine if a Notification is necessary.

        final ArrayList<Action> queueList;
        if (isPublicMessages) queueList = mPubMsgNotificationQueue;
        else queueList = mPrvMsgNotificationQueue;

        if (queueList == null || queueList.isEmpty()) return;

        final int number = queueList.size();

        if (number < 1) return;

        long sameUserId = -100l;

        if (!isPublicMessages) {
            // Go through the latest messages to see if they were sent by the same User.
            for (int i = number - 1; i > (number - MAX_LAST_MSG) && i >= 0; i--) {
                final long senderId = queueList.get(i).getSenderUserId();
                if (sameUserId > 10l && sameUserId != senderId) {
                    sameUserId = -100l;
                    break;
                }
                sameUserId = senderId;
            }
        }

        final Resources res = mContext.getResources();

        final String title;
        String text = "";
        final ChatObject chatObject;
        final int notificationId;
        Intent activity = new Intent();
        final Room room = SessionMainManager.getInstance().getRoom(mContext, false);
        if (isPublicMessages) {
            // Use the room name as the title and the number of new messages as the text
            // for public message notifications.
            chatObject = room;
            notificationId = NOTIFICATION_ID_PUB_MSG;
            title = room.getTitle();
            if (number == 1) {
                final Action message = queueList.get(0);
                if (message.isInfoMessage()) {
                    text = room.getTitle() + ": " + message.getMessage();
                } else {
                    final User sender = UsersCache.getInstance().fetch(message.getSenderUserId());
                    text = sender.getName() + ": " + message.getMessage();
                }
            } else {
                final String newMessagesFormat;
                newMessagesFormat = res.getString(R.string.new_public_messages);
                text = String.format(newMessagesFormat, number);
            }
            activity.setClass(mContext, PublicConversationActivity.class);
        } else if (sameUserId > 10L) {
            // The last private messages were all sent by the same User.
            notificationId = NOTIFICATION_ID_PRV_MSG;
            final User sender = UsersCache.getInstance().fetch(sameUserId);
            if (sender == null) return;
            chatObject = sender;

            // Simply display the user name in the title and the message in the notification text.
            title = sender.getName();
            if (number == 1) {
                final Action message = queueList.get(0);
                if (message.isPhotoMessage()) text = res.getString(R.string.picture);
                else text = message.getMessage();
            } else {
                // Build a generic text displaying the amount of new messages from this User.
                final String newMessagesFormat;
                newMessagesFormat = res.getString(R.string.new_private_messages);
                text = String.format(newMessagesFormat, number);
            }

            // Open the conversation with this User when clicking on the notification.
            activity.setClass(mContext, PrivateConversationActivity.class);
            activity.putExtra(Constants.EXTRA_USER_ID, sender.getId());
        } else {
            // The last private messages were sent by different people.
            notificationId = NOTIFICATION_ID_PRV_MSG;

            // Build a generic title displaying the amount of new messages from all Users.
            title = String.format(res.getString(R.string.new_private_messages), number);
            chatObject = room;

            // Go through the last messages again to fill the notification text.
            // Use the name of the people that sent the last messages.
            for (int i = number - 1; i > (number - MAX_LAST_MSG) && i >= 0; i--) {
                final Action message = queueList.get(i);
                final String name = UsersCache.getInstance().fetchName(message.getSenderUserId());
                if (!text.contains(name)) text += ", " + name;
            }

            // Open the conversation list when tapping on this notification.
            activity.setClass(mContext, UserTabActivity.class);
            activity.putExtra(Constants.EXTRA_SHOW_CONVERSATIONS, true);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(
                mContext,
                2200,
                activity,
                PendingIntent.FLAG_ONE_SHOT
        );

        // Use different priority and visibility settings for public and private messages.
        final int priority, visibility;
        if (isPublicMessages) {
            priority = NotificationCompat.PRIORITY_DEFAULT;
            visibility = NotificationCompat.VISIBILITY_PUBLIC;
        } else {
            priority = NotificationCompat.PRIORITY_HIGH;
            visibility = NotificationCompat.VISIBILITY_PRIVATE;
        }

        // Build the notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setDefaults(NOTIFICATION_DEFAULTS)
                .setColor(0x00FF00)
                .setLights(0x00FF00, NOTIFICATION_LIGHT_ON, NOTIFICATION_LIGHT_OFF)
                .setNumber(number)
                .setPriority(priority)
                .setSmallIcon(R.drawable.ic_places_bubble_24dp)
                .setVisibility(visibility);

        ImageLoader.with(mContext).startLoadingIntoAndNotify(chatObject, builder, notificationId);
    }

    protected void resetNotifications() {
        final NotificationManager notificationManager;
        notificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);


//        mDoNotifyPubMsg = PreferenceHelper.from(mContext).doShowPublicNotifications();
//        mDoNotifyPrvMsg = PreferenceHelper.from(mContext).doShowPrivateNotifications();

        mPrvMsgNotificationQueue = null;
        cancelPubMsgNotifications();
        notificationManager.cancel(NOTIFICATION_ID_PRV_MSG);
    }

    /**
     * Removes all Actions from the notification queue
     * that are related to private messages from a certain User
     * and updates the private messages Notification;
     *
     * @param senderId User ID of person whose messages have been read
     */
    public void cancelNotifications(final long senderId) {
        if (senderId < 10l) return;

//        mDoNotifyPrvMsg = PreferenceHelper.from(mContext).doShowPrivateNotifications();

        if (mPrvMsgNotificationQueue == null || mPrvMsgNotificationQueue.isEmpty()) return;

        // Go through the queue and store all read messages in a separate Collection
        // to avoid concurrent modification (removing while reading).
        final ArrayList<Action> readMessages = new ArrayList<>();
        for (final Action privateMessage : mPrvMsgNotificationQueue) {
            if (privateMessage.getSenderUserId() == senderId) {
                readMessages.add(privateMessage);
            }
        }

        // Remove all read messages from the queue and update the Notification.
        mPrvMsgNotificationQueue.removeAll(readMessages);
        broadcastUnreadPrivateCount();
        if (mPrvMsgNotificationQueue.isEmpty()) {
            final NotificationManager notificationManager;
            notificationManager = (NotificationManager)
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATION_ID_PRV_MSG);
        } else {
            showMessageNotifications(false);
        }
    }

    public void cancelPubMsgNotifications() {
//        mDoNotifyPubMsg = PreferenceHelper.from(mContext).doShowPublicNotifications();
        mPubMsgNotificationQueue = null;

        final NotificationManager notificationManager;
        notificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID_PUB_MSG);
    }

    public int getUnreadPrivateCount() {
        if (mPrvMsgNotificationQueue == null || mPrvMsgNotificationQueue.isEmpty()) return 0;
        return mPrvMsgNotificationQueue.size();
    }

    /**
     * Sends a Broadcast with Action Constants.ACTION_UNREAD_COUNT_CHANGED
     * that contains the number of unread PRIVATE messages
     * in Constants.EXTRA_UNREAD_COUNT.
     */
    private void broadcastUnreadPrivateCount() {
        if (mPrvMsgNotificationQueue == null || mPrvMsgNotificationQueue.isEmpty()) return;
        Intent broadcast = new Intent(Constants.ACTION_UNREAD_COUNT_CHANGED);
        final int unreadCount = mPrvMsgNotificationQueue.size();
        broadcast.putExtra(Constants.EXTRA_UNREAD_COUNT, unreadCount);
        mContext.sendBroadcast(broadcast);
    }

    private static final int SPAM_CACHE_SIZE = 10;

    private static final int SPAM_MAX_REPITITION = 5;

    /**
     * Checks if this message has been sent several times in rapid succession
     * @param message Content of outgoing message
     * @return True, if no spam was detected
     */
    public boolean updateSpamCache(final String message) {
        if (mSpamCache == null) {
            mSpamCache = new String[SPAM_CACHE_SIZE];
            mSpamCachePos = 0;
            mSpamCache[mSpamCachePos] = message;
            return true;
        }

        int spamCount = 0;
        for (final String storedMessage : mSpamCache) {
            if (storedMessage == null) break;
            if (storedMessage.contains(message)) spamCount++;
            else if (message.contains(storedMessage)) spamCount++;
        }

        // Place the given message at the next position in the short array,
        // going back to index 0 if the top has been reached.
        mSpamCachePos++;
        if (mSpamCachePos > SPAM_CACHE_SIZE) mSpamCachePos = 0;
        mSpamCache[mSpamCachePos] = message;

        if (spamCount >= SPAM_MAX_REPITITION) {
//            mSpamCache = null;
//            mSpamCachePos = 0;
            Bouncer.from(mContext).warn(true, Bouncer.REASON_SPAM);
            return false;
        } else {
            return true;
        }
    }

}
