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

package org.eztarget.realay.content_providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.SessionMainManager;

/**
 * Contains utility methods and constants that are used with ChatObjectContentProvider
 */
public class ChatObjectContract {

    private static final String TAG = ChatObjectContract.class.getSimpleName();

    /**
     * Top-level provider authority
     */
    public static final String AUTHORITY = "org.eztarget.realay.chatobjects";

    /**
     * The content URI for the top-level Realay authority.
     */
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * URI path to Conversation content
     */
    public static Uri CONTENT_URI_CONVS =
            Uri.withAppendedPath(ChatObjectContract.CONTENT_URI, "conversations");

    /**
     * URI path to User content
     */
    public static Uri CONTENT_URI_USERS =
            Uri.withAppendedPath(ChatObjectContract.CONTENT_URI, "users");

    /**
     * URI path to Public Actions
     */
    public static Uri CONTENT_URI_ACTIONS_PUBLIC =
            Uri.withAppendedPath(ChatObjectContract.CONTENT_URI, "actions_public");

    /**
     * URI path to Private Actions
     */
    public static Uri CONTENT_URI_ACTIONS_PRIVATE =
            Uri.withAppendedPath(ChatObjectContract.CONTENT_URI, "actions_private");

    /*
    PRE-DEFINED QUERY PROJECTION ARRAYS
     */

    /**
     * Cursor projection to be used as SELECT clause
     * that returns * from the Conversation table
     * and the ID and name from the Users table
     * and the ID, time and content from the private messages table
     */
    public static final String[] PROJECTION_CONVERSATIONS = new String[]{
            ChatObjectContentProvider.TABLE_CONVS + ".*",
            ChatObjectContentProvider.TABLE_USERS + ".*",
            ChatObjectContentProvider.TABLE_MSGS_PRV + "." + android.provider.BaseColumns._ID
                    + " AS " + BaseColumns.MSG_ID,
            ChatObjectContentProvider.TABLE_MSGS_PRV + "." + BaseColumns.MSG_TIME,
            ChatObjectContentProvider.TABLE_MSGS_PRV + "." + BaseColumns.MSG_CODE,
            ChatObjectContentProvider.TABLE_MSGS_PRV + "." + BaseColumns.MSG_CONTENT
    };

    /**
     * Cursor projection to be used as SELECT clause
     * that returns * from the Public Messages table
     * and the ID and name from the Users table
     */
    public static final String[] PROJECTION_MSGS_PUB_USER = new String[]{
            ChatObjectContentProvider.TABLE_MSGS_PUB + ".*",
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns._ID
                    + " AS " + BaseColumns.USER_ID,
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_NAME,
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.IMAGE_ID,
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED
    };

    /**
     * Cursor projection to be used as SELECT clause
     * that returns * from the Private Messages table
     * and the ID and name from the Users table
     */
    public static final String[] PROJECTION_MSGS_PRV_USER = new String[]{
            ChatObjectContentProvider.TABLE_MSGS_PRV + ".*",
            ChatObjectContentProvider.TABLE_USERS + "." + android.provider.BaseColumns._ID
                    + " AS " + BaseColumns.USER_ID,
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_NAME,
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.IMAGE_ID
    };

    /**
     * For WHERE clause:
     * "users.*"
     */
    public static final String[] PROJECTION_USERS = new String[]{
            ChatObjectContentProvider.TABLE_USERS + ".*"
    };

    public static final String[] PROJECTION_BLOCKED_USER_IDS = new String[]{
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns._ID,
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED
    };

    public static final String SORT_ASCENDING_IDS = BaseColumns._ID + " ASC";

    public static final int BLOCK_STATUS_UNCHANGED = -1;

    public static final int BLOCK_STATUS_UNBLOCKED = 0;

    public static final int BLOCK_STATUS_BLOCKED = 9;

    /**
     * For Cursor selection/SQL-WHERE argument;
     * For querying all conversations belonging inside a Room
     * and that do not involve a blocked user;
     * "conversations.room_id=?"
     * To be used with selectionArgs of size 1: Room ID
     */
    public static final String SELECTION_CONVS_IN_ROOM =
            ChatObjectContentProvider.TABLE_CONVS + "." + BaseColumns.ROOM_ID + "=? AND ("
                    + ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED
                    + "!=" + BLOCK_STATUS_BLOCKED + " OR "
                    + ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED
                    + " IS NULL)";

    /**
     * For Cursor selection/SQL-WHERE argument;
     * For querying all users that have been blocked;
     * "users.is_blocked = blocked";
     * Not to be used with selectionArgs
     */
    public static final String SELECTION_BLOCKED_USERS =
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED + "="
                    + BLOCK_STATUS_BLOCKED;

//    /**
//     * For Cursor selection/SQL-WHERE argument;
//     * For querying all users of a comma separated list that have not been blocked;
//     * "users._ID IN (149444, 38014, 83284) AND (users.is_blocked != blocked OR users.is_blocked IS NULL)";
//     * To be used with selectionArgs of size 1: list of user IDs;
//     * e.g. "455,9832,9938"
//     */
//    public static final String SELECTION_UNBLOCKED_SESSION_USERS =
//            BaseColumns._ID + " IN ( ? ) AND ("
//                    + BaseColumns.USER_IS_BLOCKED
//                    + "!=" + BLOCK_STATUS_BLOCKED + " OR "
//                    + BaseColumns.USER_IS_BLOCKED
//                    + " IS NULL)";

    public static String buildSessionUserListSelection() {
        final String idChain = SessionMainManager.getInstance().buildUsersSelectionArgs();
        return ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns._ID
                + " IN (" + idChain + ") AND ("
                + ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED
                + "!=" + BLOCK_STATUS_BLOCKED + " OR "
                + ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED
                + " IS NULL)";
    }

//    public static String buildPrivateMessagesSelection(final int lowestId, final long partnerId) {
//        return ChatObjectContentProvider.TABLE_MSGS_PRV + "." + BaseColumns._ID + ">=" + lowestId
//                + " AND ("
//                + ChatObjectContentProvider.TABLE_MSGS_PRV + "."
//                + BaseColumns.MSG_SENDER + "=" + partnerId + " OR "
//                + ChatObjectContentProvider.TABLE_MSGS_PRV + "."
//                + BaseColumns.MSG_RECIPIENT + "=" + partnerId + ")";
//    }

    /**
     * "messages_public.code IS IN (10, 40)
     * AND messages_public.room_id =? AND users.is_blocked != blocked"
     */
    public static final String SELECTION_PUBLIC_MSGS_UNBLOCKED =
            ChatObjectContentProvider.TABLE_MSGS_PUB + "." + BaseColumns._ID + " > ? AND "
                    + ChatObjectContentProvider.TABLE_MSGS_PUB + "." + BaseColumns.MSG_CODE
                    + " IN (" + Action.ACTION_CODE_MSG_PUB + "," + Action.ACTION_CODE_PHOTO_PUB
                    + ") AND "
                    + ChatObjectContentProvider.TABLE_MSGS_PUB + "." + BaseColumns.ROOM_ID
                    + "=? AND ("
                    + ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED
                    + "!=" + BLOCK_STATUS_BLOCKED + " OR "
                    + ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns.USER_IS_BLOCKED
                    + " IS NULL)";

    public static final String SELECTION_PRIVATE_MSGS =
            ChatObjectContentProvider.TABLE_MSGS_PRV + "." + BaseColumns._ID + ">=? AND ("
                    + ChatObjectContentProvider.TABLE_MSGS_PRV + "."
                    + BaseColumns.MSG_SENDER + "=? OR "
                    + ChatObjectContentProvider.TABLE_MSGS_PRV + "."
                    + BaseColumns.MSG_RECIPIENT + "=? )";

    /**
     * For WHERE clause:
     * "users._id=?"
     */
    public static final String SELECTION_USER =
            ChatObjectContentProvider.TABLE_USERS + "." + BaseColumns._ID + "=?";

    /*
    Action Inserts/Updates, Queueing & Conversations:
     */

    /**
     * Adds this Action to the ChatObjectContentProvider
     *
     * @param context Context in which to open the ContentResolver
     * @return Local database ID of this Action, negative value if failed
     */
    public static boolean insertAction(
            Context context,
            final Action action,
            final boolean doAutoIncrementId
    ) {
        return insertAction(context, action, doAutoIncrementId, false);
    }

    /**
     * Adds this Action to the ChatObjectContentProvider
     *
     * @param context Context in which to open the ContentResolver
     * @param inQueue Sets the "In Queue" flag to 1 for Actions that have to be resent
     * @return True, if successful
     */
    public static boolean insertAction(
            Context context,
            final Action action,
            final boolean doAutoIncrementId,
            final boolean inQueue
    ) {
        if (context == null) return false;

        // Construct the Content Values.
        ContentValues values = new ContentValues();
        final long id = action.getActionId();
        if (!doAutoIncrementId && id > 0L) values.put(BaseColumns._ID, id);
        values.put(BaseColumns.ROOM_ID, action.getRoomId());
        values.put(BaseColumns.MSG_SENDER, action.getSenderUserId());
        values.put(BaseColumns.MSG_TIME, action.getTimeStampSec());
        values.put(BaseColumns.MSG_CODE, action.getCode());
        values.put(BaseColumns.MSG_CONTENT, action.getMessage());
        values.put(BaseColumns.MSG_IN_QUEUE, inQueue ? 1 : 0);

        // Set the URI for private or public message table and add a recipient,
        // if this is a private message.
        final Uri uri;
        final boolean isPublic = action.isPublic();
        if (isPublic) {
            uri = ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC;
        } else {
            values.put(BaseColumns.MSG_RECIPIENT, action.getRecipientUserId());
            uri = ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE;
        }

        // Update or insert.
        ContentResolver resolver = context.getContentResolver();

        final String where = BaseColumns._ID + "=" + id;
        final int updateResult = resolver.update(uri, values, where, null);
        if (updateResult > 0) return true;

        final Uri msgInsertUri;
        msgInsertUri = resolver.insert(uri, values);
        if (msgInsertUri == null) return false;

        if (doAutoIncrementId || action.getActionId() < 1) {
            // Get the new ID of this message.
            try {
                action.setId(Integer.parseInt(msgInsertUri.getLastPathSegment()));
            } catch (NumberFormatException ex) {
                Log.e(TAG, "NFE: " + msgInsertUri.getPathSegments() + ", " + action.toString());
                return false;
            }
        }

        // Private messages also update the Conversation table.
        return isPublic || insertConversation(context, action, true);
    }

    public static void deleteAction(Context context, final Action action) {
        if (context == null || action == null) return;

        final Uri uri = action.isPublic() ?
                ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC :
                ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE;

        context.getContentResolver().delete(
                uri,
                BaseColumns._ID + "=" + action.getActionId(),
                null
        );
    }

    private static boolean insertConversation(
            Context context,
            final Action message,
            final boolean isRead
    ) {
        if (context == null || message == null) return false;

        ContentValues values = new ContentValues();
        values.put(BaseColumns.ROOM_ID, message.getRoomId());
        final long myId = LocalUserManager.getInstance().getUserId(context);
        final long partnerId;
        if (message.getRecipientUserId() == myId) partnerId = message.getSenderUserId();
        else partnerId = message.getRecipientUserId();
        values.put(BaseColumns.PARTNER_ID, partnerId);
        values.put(BaseColumns.LAST_MSG_ID, message.getActionId());

        ContentResolver resolver = context.getContentResolver();

        final String where = BaseColumns.ROOM_ID + "=" + message.getRoomId()
                + " AND " + BaseColumns.PARTNER_ID + "=" + partnerId;
        final int updateResult =
                resolver.update(ChatObjectContract.CONTENT_URI_CONVS, values, where, null);

        if (updateResult > 0) return true;

        final Uri insertUri = resolver.insert(ChatObjectContract.CONTENT_URI_CONVS, values);
        return insertUri != null;

    }

    /**
     * Deletes all Conversation and private messages from a certain User
     *
     * @param context Context from which to get the Content Resolver.
     * @param partnerId User ID of the person whose Conversation will be deleted
     */
    public static void deleteConversation(Context context, final long partnerId) {
        if (context == null) return;

        context.getContentResolver().delete(
                ChatObjectContract.CONTENT_URI_CONVS,
                BaseColumns.PARTNER_ID + "=" + partnerId,
                null
        );

        final String messagesWhere = BaseColumns.MSG_SENDER + "=" + partnerId
                + " OR " + BaseColumns.MSG_RECIPIENT + "=" + partnerId;

        context.getContentResolver().delete(
                ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE,
                messagesWhere,
                null
        );
    }

    /*
    User Inserts/Updates & blocking:
     */

    /**
     * @param context Context in which to perform the ContentProvider actions
     * @param user    Inserts or updates this User in the appropriate ContentProvider
     *                without setting a blocked flag.
     * @return True, if database update or insert was successful
     */
    public static boolean insertUser(Context context, final User user) {
        return insertUser(context, user, BLOCK_STATUS_UNCHANGED);
    }

    /**
     * Commonly used for updates
     *
     * @param context Context in which to perform the ContentProvider actions
     * @param user    Inserts or updates this User in the appropriate ContentProvider
     *                and sets their blocked flag to "blocked".
     * @return True, if database update or insert was successful
     */
    public static boolean blockUser(Context context, final User user) {
        return insertUser(context, user, BLOCK_STATUS_BLOCKED);
    }

    /**
     * Commonly used for updates
     *
     * @param context Context in which to perform the ContentProvider actions
     * @param user    Inserts or updates this User in the appropriate ContentProvider
     *                and sets their blocked flag to "unblocked".
     * @return True, if database update or insert was successful
     */
    public static boolean unblockUser(Context context, final User user) {
        return insertUser(context, user, BLOCK_STATUS_UNBLOCKED);
    }

    /**
     * Inserts or updates this User in the appropriate Content Provider
     * and sets a certain block status if it is supposed to be changed.
     */
    private static boolean insertUser(Context context, final User user, final int blockStatus) {
        if (context == null) return false;

        // Construct the ContentValues.
        ContentValues values = new ContentValues();
        final long userId = user.getId();
        values.put(android.provider.BaseColumns._ID, userId);
        values.put(BaseColumns.IMAGE_ID, user.getImageId());
        values.put(BaseColumns.USER_NAME, user.getName());
        values.put(BaseColumns.USER_STATUS, user.getStatusMessage());
        values.put(BaseColumns.USER_PHONE, user.getPhoneNumber());
        values.put(BaseColumns.USER_MAIL, user.getEmailAddress());
        values.put(BaseColumns.USER_WEBSITE, user.getWebsite());
        values.put(BaseColumns.USER_IG, user.getIgName());
        values.put(BaseColumns.USER_FB, user.getFacebookId());
        values.put(BaseColumns.USER_TWITTER, user.getTwitterName());
//        values.put(BaseColumns.COLOR, user.getColor());
        if (blockStatus != BLOCK_STATUS_UNCHANGED) {
            values.put(BaseColumns.USER_IS_BLOCKED, blockStatus);
        }

        // Update or add the new user to the UsersContentProvider.
        final boolean didSucceed;
        try {
            ContentResolver resolver = context.getApplicationContext().getContentResolver();
            // If the update returned 0, try an insert.
            final String where = android.provider.BaseColumns._ID + "=" + userId;
            final int updateResult =
                    resolver.update(ChatObjectContract.CONTENT_URI_USERS, values, where, null);

            if (updateResult > 0) {
                didSucceed = true;
            } else {
                didSucceed = resolver.insert(ChatObjectContract.CONTENT_URI_USERS, values) != null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        if (!didSucceed) {
            Log.e(TAG, "Could not insert User: " + user.toString());
        }

        return didSucceed;
    }

}