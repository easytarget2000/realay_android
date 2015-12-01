package org.eztarget.realay.managers;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.R;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.ui.BaseActivity;
import org.eztarget.realay.ui.MapActivity;
import org.eztarget.realay.ui.RoomListActivity;
import org.eztarget.realay.ui.utils.ImageLoader;
import org.eztarget.realay.ui.utils.IntentFactory;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by michel on 06/01/15.
 *
 */
public class Bouncer {

    private static Bouncer instance = null;

    private static final String TAG = Bouncer.class.getSimpleName();

    private static final int NOTIFICATION_ID = 448982;

    public static final int REASON_TIMEOUT = 5577;

    public static final int REASON_DATA_OFF = 3213;

    public static final int REASON_LOCATION = 4441;

    public static final int REASON_SPAM = 77777;

    public static final int REASON_RECEIVED_KICK = 8181;
    
    public static final int REASON_SESSION_OVER = 8918;

    private static final long INTERVAL_DEBUG = 60L * 1000L;

    private static final long INTERVAL_FIVE_MINUTES = 5L * 60L * 1000L;

    private static final long INTERVAL_TEN_MINUTES = 2L * INTERVAL_FIVE_MINUTES;

    private static final long[] WARNING_INTERVALS =
            new long[]{
            INTERVAL_TEN_MINUTES,
            INTERVAL_TEN_MINUTES,
            INTERVAL_FIVE_MINUTES,
            INTERVAL_FIVE_MINUTES
    };
//            new long[]{INTERVAL_DEBUG, INTERVAL_DEBUG, INTERVAL_DEBUG, INTERVAL_DEBUG};

    private static final int MAX_WARNINGS = WARNING_INTERVALS.length;

    private Context mContext;

    private int mLastReason;

    private boolean mHasPendingKick = false;

    private long mKickTimeMillis;

    private PendingIntent mWarningIntent;

    private AlarmManager mAlarmMan;

    private int mNumOfWarnings;
    
    private Bouncer(Context context) {
        mContext = context;
    }

    public static Bouncer from(Context context) {
        if (instance == null || instance.mContext == null) {
            instance = new Bouncer(context);

        }
        return instance;
    }

    public int getLastReason() {
        return mLastReason;
    }

    public boolean resetSession() {
        PreferenceHelper.from(mContext).resetBouncerAttributes();
        mNumOfWarnings = 0;
        mKickTimeMillis = 0L;
        stopAllAlarms();
        return true;
    }

    public void resetDialogs() {
        mLastReason = 0;
        mHasPendingKick = false;
    }

    public void cancelNotifications() {
        if (mContext == null) return;
        NotificationManager nm;
        nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    public void kick(final int reason, final String caller) {
        if (mContext == null) {
            Log.e(TAG, "kick() called with null context.");
            return;
        }
        Log.d(TAG, "Kicking. Reason: " + reason + ", Caller: " + caller);
        mHasPendingKick = true;
        mLastReason = reason;

        // Leave the Room but prepare a new Session in the same one.
        final Room oldRoom = SessionMainManager.getInstance().getRoom(mContext, false);
        SessionMainManager.getInstance().leaveSession(mContext, TAG);
        SessionMainManager.getInstance().prepareSession(mContext, oldRoom);

        // Show a notification if the app is in the background.
        if (PreferenceHelper.from(mContext).isInBackground()) {
            showNotification(reason, true);
        } else {
            Intent messageAction = new Intent(Constants.ACTION_DISPLAY_MESSAGE);
            mContext.sendBroadcast(messageAction);
        }
    }

    public void warn(final boolean doAllowDuplicates, final int reason) {
        if (!doAllowDuplicates && reason == mLastReason) {
            return;
        }

        Log.d(TAG, "Warning.");

        if (mKickTimeMillis < 1000L) {
            // Receiving the first warning, calculate the kick time by summing up the warning times.
            long warningIntervalSum = 0L;
            for (int i = 0; i < MAX_WARNINGS; i++) warningIntervalSum += WARNING_INTERVALS[i];
            mKickTimeMillis = System.currentTimeMillis() + warningIntervalSum;
        }

        if (mKickTimeMillis > System.currentTimeMillis() && mNumOfWarnings < MAX_WARNINGS) {
            mHasPendingKick = false;
//            Log.d(TAG, mNumOfWarnings + "/" + MAX_WARNINGS + ": " + reason);

            mLastReason = reason;

            if (PreferenceHelper.from(mContext).isInBackground()) {
                showNotification(reason, false);
            } else if (mContext != null) {
                Intent showWarning = new Intent(Constants.ACTION_DISPLAY_MESSAGE);
                mContext.sendBroadcast(showWarning);
            }

            if (reason == REASON_LOCATION || reason == REASON_SESSION_OVER) {
                startWarningAlarm(reason);
            }

            PreferenceHelper.from(mContext).storeBouncerAttributes(
                    mLastReason,
                    mNumOfWarnings,
                    mKickTimeMillis
            );
        } else {
            kick(reason, TAG + " warning limit");
        }
        mNumOfWarnings++;
    }

    private String buildFormattedEndDate() {
        final Room sessionRoom = SessionMainManager.getInstance().getRoom(mContext, false);
        if (sessionRoom == null) return "--:--";
        final long sessionEndSecs = sessionRoom.getEndDateSec();
        if (sessionEndSecs < 10000L) return "--:--";
        final DateFormat hoursMinutesFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        final Date date = new Date(sessionEndSecs * 1000L);
        return hoursMinutesFormat.format(date);
    }

    /**
     * Uses the warning intervals to calculate when the final warning will be displayed
     * and stores the value as a readable HH:MM string
     */
    public String buildFormattedKickDate() {
        if (mKickTimeMillis < 1000L) return "--:--";
        final DateFormat hoursMinutesFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        final Date date = new Date(mKickTimeMillis);
        return hoursMinutesFormat.format(date);
    }

    public boolean showPendingDialog(final BaseActivity activity) {
        if (activity == null || mLastReason == 0) return false;

        final Resources res = activity.getResources();

        final int titleId;
        final String message;
        switch (mLastReason) {
            case Bouncer.REASON_LOCATION:
                if (mHasPendingKick) {
                    titleId = R.string.session_terminated;
                    message = res.getString(R.string.stay_in_boundaries);
                } else {
                    titleId = R.string.advise;
                    final String messageFormat = res.getString(R.string.return_location);
                    message = String.format(messageFormat, buildFormattedKickDate());
                }
                break;
            case Bouncer.REASON_SESSION_OVER:
                if (mHasPendingKick) {
                    titleId = R.string.session_terminated;
                    message = res.getString(R.string.event_ended);
                } else {
                    titleId = R.string.advise;
                    final String messageFormat = res.getString(R.string.part_of_event_stay);
                    message = String.format(
                            messageFormat,
                            buildFormattedEndDate(),
                            buildFormattedKickDate()
                    );
                }
                break;

            case Bouncer.REASON_TIMEOUT:
                titleId = R.string.session_terminated;
                message = res.getString(R.string.timeout_occurred);
                break;

            case Bouncer.REASON_DATA_OFF:
                titleId = R.string.app_restarted;
                message = res.getString(R.string.feel_free_to_join);
                break;

            case Bouncer.REASON_RECEIVED_KICK:
                titleId = R.string.session_terminated;
                message = res.getString(R.string.requested_to_leave);
                break;

            default:
                return false;
        }

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        alert.setTitle(res.getString(titleId));
        alert.setMessage(message);

        if (mHasPendingKick) {
            // Make sure that any user input from now on, closes this activity.
            alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    resetDialogs();
                }
            });

            // Always start the Launch Activity before showing the kick dialog.
            if (!(activity instanceof RoomListActivity)) {
                activity.startLaunchActivity();
                return true;
            }
        } else if (mLastReason == Bouncer.REASON_LOCATION) {
            alert.setNeutralButton(R.string.settings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    IntentFactory.startLocationSettingsActivity(activity);
                }
            });
        }

        try {
            alert.show();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.toString());
        }
        resetDialogs();
        cancelNotifications();
        return true;
    }

    private static final int NOTIFICATION_LIGHT_ON = 500;

    private static final int NOTIFICAITON_LIGHT_OFF = 500;

    private void showNotification(final int reason, final boolean isKick) {
        if (mContext == null) return;
        final Resources res = mContext.getResources();
        final Room room = SessionMainManager.getInstance().getRoom(mContext, false);

        final String title, text;
        final int argb = mContext.getResources().getColor(R.color.accent);
        if (isKick) {
            if (room == null) title = res.getString(R.string.advise);
            else title = room.getTitle();
            text = res.getString(R.string.session_terminated);
        } else {
            title = res.getString(R.string.advise);
            switch (reason) {
                case REASON_LOCATION: {
                    final String textFormat = res.getString(R.string.return_until);
                    text = String.format(textFormat, buildFormattedKickDate());
                    break;
                }
                case REASON_SESSION_OVER: {
                    final String textFormat = res.getString(R.string.logged_in_until);
                    text = String.format(
                            textFormat,
                            buildFormattedKickDate()
                    );
                    break;
                }
                default:
                    return;
            }
        }

        final Intent activity = new Intent(mContext, MapActivity.class);
        final PendingIntent contentIntent = PendingIntent.getActivity(
                mContext,
                NOTIFICATION_ID,
                activity,
                PendingIntent.FLAG_ONE_SHOT
        );

        // Build the notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setColor(argb)
                .setLights(argb, NOTIFICATION_LIGHT_ON, NOTIFICAITON_LIGHT_OFF)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_places_bubble_24dp)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (room != null) {
            ImageLoader.with(mContext).startLoadingIntoAndNotify(room, builder, NOTIFICATION_ID);
            return;
        }

        final Notification notification = builder.build();
        if (notification != null) {
            try {
                NotificationManager nm;
                nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(NOTIFICATION_ID, notification);
            } catch (SecurityException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void startWarningAlarm(final int reason) {
        initAlarm(reason);

        if (mAlarmMan != null && mWarningIntent != null) {
            final long triggerAtMillis;
            triggerAtMillis = System.currentTimeMillis() + WARNING_INTERVALS[mNumOfWarnings];
            mAlarmMan.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    mWarningIntent
            );
        }
    }

    private void stopAllAlarms() {
        if (mAlarmMan == null || mWarningIntent == null) initAlarm(-1);

        if (mAlarmMan != null && mWarningIntent != null) {
            mAlarmMan.cancel(mWarningIntent);
//            Log.i(TAG, "Stopped Actions AlarmManager.");
        }
    }

    private void initAlarm(final int reason) {
        if (mContext == null) return;
        mContext = mContext.getApplicationContext();

        Intent alarmIntent = new Intent();
        alarmIntent.setAction(Constants.ACTION_WARN_HEARTBEAT);
        alarmIntent.putExtra(Constants.EXTRA_WARN_REASON, reason);
        mWarningIntent = PendingIntent.getBroadcast(
                mContext,
                4041,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (mAlarmMan == null) {
            mAlarmMan = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        }
    }

}
