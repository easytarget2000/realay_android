package org.eztarget.realay.managers;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.content_providers.RoomsContentProvider;
import org.eztarget.realay.content_providers.RoomsContract;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.services.ActionSendService;

import java.util.ArrayList;

/**
 * Created by michel on 24/11/14.
 *
 */
public class SessionMainManager {

    private static final String TAG = SessionMainManager.class.getSimpleName();

    // DEBUG VALUE:
    private static final long DEEP_UPDATE_MILLIS = 3L * 60L * 1000L;

    /**
     * Singleton instance
     */
    private static SessionMainManager instance = null;

    private boolean mDidStart = false;

//    protected boolean mIsPaused = false;

    private Room mRoom;

    private long mUserListUpdateMillis;

    private boolean mDidStartEndWarning = false;

    /**
     * Stores all database IDs of Users that are in this session
     * for quicker access without additional queries
     */
    private ArrayList<Long> mUserIdList;

    /**
     * Exists to defeat instantiation
     */
    protected SessionMainManager() {}

    public static SessionMainManager getInstance() {
        if (instance == null) instance = new SessionMainManager();
        return instance;
    }

    /**
     * Loads a room startLoadingInto the manager and pre-loads session data for a possible join;
     * to be called before startListening();
     *
     * @param room Room that will be prepared for a possible session
     */
    public boolean prepareSession(Context context, final Room room) {
        if (context == null || room == null) return false;

        if (mDidStart) {
            Log.w(TAG, "Attempted to prepare a new session while already in an active one.");
            resetSession(context);
        }

        if (!resetSession(context)) return false;

        mRoom = room;

        // Now that a Room has been prepared,
        // the Location Update speed and priority increase.
        LocationWatcher.from(context).adjustLocationUpdates();
        return true;
    }

    /**
     * Checks if the Manager is prepared, sets the "in session" flag
     * and starts the Heartbeats
     *
     * @param context Application Context
     * @return True, if the Session was successfully started
     */
    public boolean startSession(Context context) {
        if (getRoom(context, false) == null) return false;
//        if (mDidStart) return false;
        mDidStart = true;
        PreferenceHelper.from(context).setSessionRoomId(mRoom.getId());
        return SessionActionsManager.from(context).setHeartbeatAlarm(
                System.currentTimeMillis() + 5000L
        );
    }

    /**
     * Initialises the Session Manager and resets all values and lists;
     * to be called before startListening() to provide a clean session state
     */
    public boolean resetSession(Context context) {
        if (context == null) {
            Log.e(TAG, "resetSession() called without context.");
            return false;
        }

        /*
        RESET ATTRIBUTES
         */
        mDidStart = false;
        mRoom = null;
        resetUserList();

        UsersCache.getInstance().resetSession();
        SessionActionsManager.from(context).stopHeartbeat();
        if (!Bouncer.from(context).resetSession()) return false;

        PreferenceHelper.from(context).leaveSession();

        return true;
    }

    /**
     * Returns to the launch Activity, resets the Managers and sends a "quit" Action to the server.
     *
     * @param context Context for Content Provider & Service Intents
     * @param caller Debug Tag to monitor how this method has been called
     */
    public void leaveSession(final Context context, final String caller) {
        dispatchLeaveAction(context);

        // Keep the Session from being restored.
        mRoom = null;
        PreferenceHelper.from(context).leaveSession();

        // Decrease the speed of location updates outside of sessions.
        LocationWatcher.from(context).adjustLocationUpdates();

        // Reset the Manager and return to the launch Activity.
        resetSession(context);

        Log.d(TAG, "Session has been left. Called by " + caller + ".");
    }

    public void dispatchLeaveAction(final Context context) {
        // Build the Action and send it before resetting the Session Manager.
        // Otherwise the Room reference is already lost.
        final Action quitAction = Action.buildPlaceholderAction(
                getRoomId(context),
                LocalUserManager.getInstance().getUserId(context),
                Action.ACTION_CODE_QUIT
        );
        if (quitAction != null) {
            Intent actionSender = new Intent(context, ActionSendService.class);
            actionSender.putExtra(Constants.EXTRA_ACTION, quitAction);
            context.startService(actionSender);
        }
    }

    public Room getRoom(Context context, final boolean doForceRestoration) {
        if (mRoom == null || doForceRestoration) {
            final long roomId = PreferenceHelper.from(context).getSessionRoomId();
            if (roomId < 10L) return null;
            Cursor roomCursor = context.getContentResolver().query(
                    RoomsContentProvider.CONTENT_URI,
                    null,
                    RoomsContract.SELECTION_ROOM_BY_ID,
                    new String[]{String.valueOf(roomId)},
                    null
            );
            if (roomCursor.moveToFirst()) mRoom = RoomsContract.buildRoom(roomCursor);

            if (mRoom != null) Log.d(TAG, "Restored Room from earlier Session.");
        }
        return mRoom;
    }

    public long getRoomId(Context context) {
        getRoom(context, false);
        if (mRoom == null) return -12L;
        else return mRoom.getId();
    }

    public boolean didLogin() {
        return mDidStart;
    }

    public boolean isAdmin() {
        return false;
    }

    public boolean didReachEndDate() {
        if (!mDidStart || mRoom == null) return false;
        final long endTimeSec = mRoom.getEndDateSec();
        return endTimeSec >= 1000L && System.currentTimeMillis() > endTimeSec * 1000L;
    }

    /**
     * @param userId This user ID will be added to the ArrayList of all IDs in this session
     *               without sorting. Checks for uniqueness
     */
    public boolean addUserId(final long userId) {
        if (userId < 10L) return false;
        if (mUserIdList == null) mUserIdList = new ArrayList<>();

        if (!mUserIdList.contains(userId)) {
            mUserIdList.add(userId);
            return true;
        } else {
            return false;
        }
    }

    public void resetUserList() {
        mUserListUpdateMillis = 0L;
        mUserIdList = new ArrayList<>();
    }

    public void removeUser(final long userId) {
        if (mUserIdList == null) mUserIdList = new ArrayList<>();
        mUserIdList.remove(userId);
    }

    public boolean containsUser(final long userId) {
        if (mUserIdList == null) mUserIdList = new ArrayList<>();
        return mUserIdList.contains(userId);
    }

    public boolean doDeepUpdates() {
        if (!mDidStart) return false;

        final long now = System.currentTimeMillis();
        return now - mUserListUpdateMillis > DEEP_UPDATE_MILLIS;
    }

    public void ackUserListUpdate() {
        mUserListUpdateMillis = System.currentTimeMillis();
    }

    public int numberOfUsers() {
        if (mUserIdList == null) return 1;
        else return mUserIdList.size() + 1;
    }

    /**
     * Builds a String Array of size 1 to be used as selectionArgs argument
     * in a Cursor database query
     *
     * @return Array containing one String object that is a comma separated list
     * of all User IDs currently in this Session, e.g. "124323,4783756,942376";
     * "-37" if the User ID list is empty
     */
    public String buildUsersSelectionArgs() {
        if (numberOfUsers() < 1) return "-37";

        String idChain = "";
        for (int i = 0; i < mUserIdList.size(); i++) {
            if (i != 0) idChain += ',';
            idChain += String.valueOf(mUserIdList.get(i));
        }
        return idChain;
    }

}
