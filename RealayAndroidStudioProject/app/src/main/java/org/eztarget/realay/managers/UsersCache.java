package org.eztarget.realay.managers;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.util.Log;

import org.eztarget.realay.Constants;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.User;
import org.eztarget.realay.services.UserQueryService;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by michel on 19/12/14.
 */
public class UsersCache {

    /** Singleton instance */
    private static UsersCache instance = null;

    private static final String TAG = UsersCache.class.getSimpleName();

    private static final long DOWNLOAD_RETRY_INTERVAL_MILLIS = 20L * 1000L;

    public static final long ERROR_CURSOR_COLUMN = -7l;

    public static final long ERROR_CURSOR_NULL = -8l;

    public static final long ERROR_ID_NOT_IN_DB = -9l;

    private HashMap<Long, User> mUserHashMap = new HashMap<>();

    private ArrayList<Long> mDownloadQueueIds;

    private long mLastDownloadMillis;

    private ArrayList<Long> mBlockedUsers;


    /** Exists only to defeat instantiation */
    protected UsersCache() {}

    public static UsersCache getInstance() {
        if (instance == null) instance = new UsersCache();
        return instance;
    }

    protected void resetSession() {
        mUserHashMap = new HashMap<>();
    }

    public void addUser(final User user) {
        final long userId = user.getId();

        didFinishDownloading(userId);

        if (mUserHashMap == null) mUserHashMap = new HashMap<>();
        mUserHashMap.put(userId, user);
    }

    public void downloadIfNeeded(Context context, final long userId) {
        if (context == null) return;
        if (userId < 10L) return;

        if (mUserHashMap == null) mUserHashMap = new HashMap<>();
        if (mUserHashMap.containsKey(userId)) return;

        final Cursor userCursor = context.getContentResolver().query(
                ChatObjectContract.CONTENT_URI_USERS,
                ChatObjectContract.PROJECTION_USERS,
                ChatObjectContract.SELECTION_USER,
                new String[] {String.valueOf(userId)},
                null
        );

        if (userCursor.moveToFirst()) {
            final User user = fetch(userCursor, true);
            userCursor.close();
            if (user != null) return;
        }

        // The user was not found in the local database.
        startDownloadService(context, userId);
    }

    /**
     * Starts the service that downloads the User data;
     * The service updates the local database and this cache when it finishes.
     * @param context Context in which the UserUpdateService will be started
     * @param userId User ID that will be selected
     */
    public void startDownloadService(Context context, final long userId) {
        if (context == null) return;
        if (isDownloading(userId)) return;

        context = context.getApplicationContext();
        // Start the UpdateService.
        Intent updateServiceIntent = new Intent(context, UserQueryService.class);
        updateServiceIntent.putExtra(Constants.EXTRA_USER_ID, userId);
        context.startService(updateServiceIntent);
    }

    /**
     * Checks if a download of User data has been recently started;
     * Assuming that this check is used in order to start downloads,
     * the ID is added to the list of recent downloads, if it is not in there already
     *
     * @param userId ID of the User whose data is supposed to be updated
     * @return True, if the given User's data are already being downloaded
     */
    private boolean isDownloading(final long userId) {
        if (System.currentTimeMillis() - mLastDownloadMillis > DOWNLOAD_RETRY_INTERVAL_MILLIS) {
            mDownloadQueueIds = new ArrayList<>();
            mLastDownloadMillis = System.currentTimeMillis();
            mDownloadQueueIds.add(userId);
            return false;
        }

        if (mDownloadQueueIds == null) {
            mDownloadQueueIds = new ArrayList<>();
            mDownloadQueueIds.add(userId);
            mLastDownloadMillis = System.currentTimeMillis();
            return false;
        } else {
            if (mDownloadQueueIds.contains(userId)) {
//                Log.d(TAG, "Not querying for " + userId + ". Last download: " + new Date(mLastDownloadMillis));
                return true;
            }
            mDownloadQueueIds.add(userId);
            mLastDownloadMillis = System.currentTimeMillis();
            return false;
        }
    }

    /**
     * Acknowledges that a User data download has been finished;
     * see isDownloading()
     *
     * @param userId ID of the User whose data download has been finished or cancelled
     */
    public void didFinishDownloading(final long userId) {
        if (mDownloadQueueIds == null) return;
        mDownloadQueueIds.remove(userId);
    }

    public User fetch(final long userId) {
        return mUserHashMap.get(userId);
    }

    // CONTINUE HERE: Wieso war "Redet Ihr über mich" unknown und Sender auf iPod unknown?

    public String fetchName(final long userId) {
        final User fetchedUser = fetch(userId);
        if (fetchedUser == null) return String.valueOf(userId * 11L);
        return fetchedUser.getName();
    }

    public User fetch(Cursor cursor, final boolean doCloseCursor) {
        if (cursor == null) return null;

        if (cursor.getCount() < 1 || cursor.getColumnCount() < 1) {
            return null;
        }

        final long cursorUserId = idFromCursor(cursor);

        final int nameIndex = cursor.getColumnIndex(BaseColumns.USER_NAME);
        final String name;
        try {
            name = cursor.getString(nameIndex);
        } catch (CursorIndexOutOfBoundsException e) {
            return null;
        }

        if (cursorUserId < 10l) {
            Log.e(TAG, "Cursor is invalid: " + cursorUserId + ", " + name);
            return null;
        }

        User cursorUser = fetch(cursorUserId);
        if (cursorUser != null) return cursorUser;

        final int imageIdIndex =
                cursor.getColumnIndex(BaseColumns.IMAGE_ID);
        final int statusIndex =
                cursor.getColumnIndex(BaseColumns.USER_STATUS);
        final int mailAddressIndex =
                cursor.getColumnIndex(BaseColumns.USER_MAIL);
        final int phoneNumberIndex =
                cursor.getColumnIndex(BaseColumns.USER_PHONE);
        final int websiteIndex =
                cursor.getColumnIndex(BaseColumns.USER_WEBSITE);
        final int igHandleIndex =
                cursor.getColumnIndex(BaseColumns.USER_IG);
        final int fbHandleIndex =
                cursor.getColumnIndex(BaseColumns.USER_FB);
        final int twitterHandleIndex =
                cursor.getColumnIndex(BaseColumns.USER_TWITTER);
//        final int colorIndex =
//                cursor.getColumnIndex(BaseColumns.COLOR);

        final User newUser = new User(
                cursorUserId,
                cursor.getLong(imageIdIndex),
                name,
                cursor.getString(statusIndex),
                cursor.getString(mailAddressIndex),
                cursor.getString(phoneNumberIndex),
                cursor.getString(websiteIndex),
                cursor.getString(igHandleIndex),
                cursor.getString(fbHandleIndex),
                cursor.getString(twitterHandleIndex)
        );
        if (doCloseCursor) cursor.close();

        mUserHashMap.put(cursorUserId, newUser);
        return newUser;
    }

    public long idFromCursor(Cursor cursor) {
        if (cursor == null) return ERROR_CURSOR_NULL;

        final int idIndex = cursor.getColumnIndex(BaseColumns._ID);
        if (idIndex < 0 || cursor.getColumnCount() <= 0) {
            Log.e(TAG, "Column index of " + BaseColumns._ID + ": " + idIndex);
            return ERROR_CURSOR_COLUMN;
        } else {
            try {
                return cursor.getLong(idIndex);
            } catch (CursorIndexOutOfBoundsException e) {
                return ERROR_ID_NOT_IN_DB;
            }
        }
    }

    public void blockUser(Context context, final User smug) {
        ChatObjectContract.blockUser(context, smug);
        getBlockedUsers(context);
        mBlockedUsers.add(smug.getId());
    }

    public void unblockUser(Context context, final long userId) {
        unblockUser(context, fetch(userId));
    }

    public void unblockUser(Context context, final User user) {
        ChatObjectContract.unblockUser(context, user);
        getBlockedUsers(context);
        mBlockedUsers.remove(user.getId());
    }

    public Long[] getBlockedUsers(Context context) {
        if (mBlockedUsers == null) {
            mBlockedUsers = new ArrayList<>();
            Cursor blockedCursor = context.getContentResolver().query(
                    ChatObjectContract.CONTENT_URI_USERS,
                    ChatObjectContract.PROJECTION_BLOCKED_USER_IDS,
                    ChatObjectContract.SELECTION_BLOCKED_USERS,
                    null,
                    ChatObjectContract.SORT_ASCENDING_IDS
                    );
            final int userIdColumn = blockedCursor.getColumnIndex(BaseColumns._ID);
            while (blockedCursor.moveToNext() && userIdColumn > -1) {
                mBlockedUsers.add(blockedCursor.getLong(userIdColumn));
            }
        }
        Long[] smugs = new Long[mBlockedUsers.size()];
        mBlockedUsers.toArray(smugs);
        return smugs;
    }

    public boolean didBlockUser(final long userId) {
        return mBlockedUsers.contains(userId);
    }

}
