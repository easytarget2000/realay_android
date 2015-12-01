package org.eztarget.realay.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.SessionMainManager;

import java.util.Date;

/**
 * Created by michel on 11/12/14.
 * <p/>
 * Data model object: Room Action
 * Represents an action performed by a User in a certain Room;
 * <p/>
 * These actions are usually sent to all users in a Room,
 * such as a message in the public conversation
 * or are directed at one specific other User,
 * such as a private message.
 * <p/>
 * Besides messages, common Actions can also be a User joining the Room
 * or a User updating his profile.
 * Other Users receive this action and so their apps are notified to update the User data.
 */
public class Action extends ChatObject implements Parcelable {

    private static final String TAG = Action.class.getSimpleName();

    private int mId;

    /**
     * Placeholder ID for Action objects that were created locally, mostly outgoing messages
     */
    public static final int ID_UNKNOWN = -444;

    /**
     * The mime type of a directory of items
     */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.org.eztarget.realay.actions";

    /**
     * The mime type of a single item.
     */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.org.eztarget.realay.actions";

    /**
     * Database ID of the Room in which this Action occurred
     */
    private long mRoomId;

    /**
     * Database ID of the User that performed this Action
     */
    private long mSenderId;

    /**
     * Database ID of the User that is supposed to receive this Action;
     * Ignored used for public chat messages or server wide announcements, usually -1
     */
    private long mRecipientId;

    /**
     * UNIX timestamp (in SECONDS) at which this action occurred
     */
    private long mTimestampSec;

    /**
     * Action specific code that determines what happened
     */
    private int mCode;

    /**
     * Readable message
     */
    private String mMessage;

    /**
     * Readable user name, only used for Notification purposes
     */
    private String mDisplayName;

    /**
     * Action code "Public conversation message"
     */
    public static final int ACTION_CODE_MSG_PUB = 10;

    /**
     * Action code "Private conversation message"
     */
    public static final int ACTION_CODE_MSG_PRV = 11;

    /**
     * Action code "User kick"
     */
    public static final int ACTION_CODE_KICK = 16;

    /**
     * Action code "User ban"
     */
    public static final int ACTION_CODE_BAN = 19;

    /**
     * Action code "User joined"
     */
    public static final int ACTION_CODE_JOIN = 21;

    /**
     * Action code "User updated profile"
     */
    public static final int ACTION_CODE_UPD_USER = 22;

    /**
     * Action code "Server-wide message"
     */
    public static final int ACTION_CODE_SERVER_MSG = 28;

    /**
     * Action code "public photo message"
     */
    public static final int ACTION_CODE_PHOTO_PUB = 40;

    /**
     * Action code "private photo message"
     */
    public static final int ACTION_CODE_PHOTO_PRV = 41;

    /**
     * Action code "User has left the Room"
     */
    public static final int ACTION_CODE_QUIT = 66;

    /**
     * Action code "Report User (Recipient) behaviour"
     */
    public static final int ACTION_CODE_REPORT = 71;

    /**
     * Default status code for messages
     */
    public static final int ACTION_STATUS_DEFAULT = 0;

    /**
     * Status code for messages that have not been sent to the Server yet
     */
    public static final int ACTION_STATUS_UNSENT = 100;

    /**
     * Recipient User ID for public Actions;
     * Public Actions do not have one defined recipient,
     * so this is used for consistency purposes only
     */
    public static final long PUBLIC_RECIPIENT_ID = -10l;

    public Action(
            final int id,
            final long roomId,
            final long senderId,
            final long recipientId,
            final long timestampSec,
            final int code,
            final String message
    ) {
        mId = id;
        mRoomId = roomId;
        mSenderId = senderId;
        mRecipientId = recipientId;
        mTimestampSec = timestampSec;
        mCode = code;
        if (message != null) {
            mMessage = message;
            if (mCode == ACTION_CODE_PHOTO_PRV || mCode == ACTION_CODE_PHOTO_PUB) {
                try {
                    mImageId = Long.parseLong(mMessage);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "NFE: " + message);
                }
            }
        }
    }

    public Action(final long timestampSec, final String message, final int code) {
        mId = ID_UNKNOWN;
        mRoomId = -100l;
        mSenderId = -100l;
        mRecipientId = -100l;
        mTimestampSec = timestampSec;
        mCode = code;
        mMessage = message;
    }

    public static Action buildImageUploadAction(
            Context context,
            final long recipientId,
            final Bitmap icon
    ) {
        final long roomId = SessionMainManager.getInstance().getRoomId(context);
        final long localUserId = LocalUserManager.getInstance().getUserId(context);
        final int code;
        code = (recipientId == PUBLIC_RECIPIENT_ID) ? ACTION_CODE_PHOTO_PUB : ACTION_CODE_PHOTO_PRV;

        return new Action(
                ID_UNKNOWN,
                roomId,
                localUserId,
                recipientId,
                System.currentTimeMillis() / 1000L,
                code,
                null
        );
    }

    public static Action buildReportAction(final Context context, final long userId) {
        if (context == null || userId < 100L) return null;

        final long roomId = SessionMainManager.getInstance().getRoomId(context);
        final long localUserId = LocalUserManager.getInstance().getUserId(context);

        return new Action(
                ID_UNKNOWN,
                roomId,
                localUserId,
                userId,
                System.currentTimeMillis() / 1000L,
                ACTION_CODE_REPORT,
                null
        );
    }

    public Action(
            final long roomId,
            final long senderId,
            final long recipientId,
            final long timeStampSec,
            final int code,
            final long imageId
    ) {
        this(ID_UNKNOWN, roomId, senderId, recipientId, timeStampSec, code, null);
        mImageId = imageId;
    }


    public Action(
            final long roomId,
            final long senderId,
            final long recipientId,
            final long timeStampSec,
            final int code,
            final String message
    ) {
        this(ID_UNKNOWN, roomId, senderId, recipientId, timeStampSec, code, message);
    }

    public static Action buildAction(Cursor cursor) {
//        final int idIndex =
//                cursor.getColumnIndex(BaseColumns._ID);
//        if (idIndex != 0) return null;
        final int roomIdIndex =
                cursor.getColumnIndex(BaseColumns.ROOM_ID);
        final int senderIdIndex =
                cursor.getColumnIndex(BaseColumns.MSG_SENDER);
        final int recipientIdIndex =
                cursor.getColumnIndex(BaseColumns.MSG_RECIPIENT);
        final int timeIndex =
                cursor.getColumnIndex(BaseColumns.MSG_TIME);
        final int codeIndex =
                cursor.getColumnIndex(BaseColumns.MSG_CODE);
        final int contentIndex =
                cursor.getColumnIndex(BaseColumns.MSG_CONTENT);

        // If this cursor points at the PUBLIC messages table, no recipient ID is stored.
        final long recipientId;
        if (recipientIdIndex < 0) recipientId = Action.PUBLIC_RECIPIENT_ID;
        else recipientId = cursor.getLong(recipientIdIndex);

        try {
            return (
                    new Action(
                            cursor.getInt(0),
                            cursor.getLong(roomIdIndex),
                            cursor.getLong(senderIdIndex),
                            recipientId,
                            cursor.getLong(timeIndex),
                            cursor.getInt(codeIndex),
                            cursor.getString(contentIndex)
                    )
            );

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Action(Parcel source) {
        if (source == null) return;
        mId = source.readInt();
        mImageId = source.readLong();
        mRoomId = source.readLong();
        mSenderId = source.readLong();
        mRecipientId = source.readLong();
        mTimestampSec = source.readLong();
        mCode = source.readInt();
        mMessage = source.readString();
    }

    public static Action buildPlaceholderAction(
            final long sessionId,
            final long senderUserId,
            final int code
    ) {
        if (sessionId < 10L || senderUserId < 10L) {
            Log.e(TAG, "Creating placeholder Action without Session or Sender ID.");
        }

        return new Action(
                ID_UNKNOWN,
                sessionId,
                senderUserId,
                PUBLIC_RECIPIENT_ID,
                System.currentTimeMillis() / 1000l,
                code,
                null
        );
    }

    /*
    GETTER / SETTER
     */

    public long getRoomId() {
        return mRoomId;
    }

    public long getSenderUserId() {
        return mSenderId;
    }

    public long getRecipientUserId() {
        return mRecipientId;
    }

    /**
     * @return The User ID that is not the local User,
     * i.e. the sender ID if the recipient is the local User and vice versa
     */
    public long getPartnerId(final long localUserId) {
        if (mSenderId == localUserId) return mRecipientId;
        else return mSenderId;
    }

    public boolean isPublic() {
        return !(mCode == ACTION_CODE_MSG_PRV || mCode == ACTION_CODE_PHOTO_PRV);
    }

    public long getTimeStampSec() {
        return mTimestampSec;
    }

    public int getCode() {
        return mCode;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(final String message) {
        mMessage = message;
    }

    public boolean isMessage() {
        if (mCode == ACTION_CODE_MSG_PUB || mCode == ACTION_CODE_PHOTO_PUB) return true;
        else if (mCode == ACTION_CODE_MSG_PRV || mCode == ACTION_CODE_PHOTO_PRV) return true;
        else return isInfoMessage();
    }

    public boolean isInfoMessage() {
        if (mSenderId == PUBLIC_RECIPIENT_ID) {
            if (mRecipientId == PUBLIC_RECIPIENT_ID) {
                if (mCode == ACTION_CODE_MSG_PUB) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isPhotoMessage() {
        return mCode == ACTION_CODE_PHOTO_PUB || mCode == ACTION_CODE_PHOTO_PRV;
    }

    public void setId(final int id) {
        mId = id;
    }

    public void setImageId(final long imageId) {
        super.setImageId(imageId);
        if (isPhotoMessage()) mMessage = String.valueOf(imageId);
    }

    public int getActionId() {
        return mId;
    }

    @Override
    public String toString() {
        return mId
                + ": From: " + mSenderId
                + " To: " + mRecipientId
                + " At: " + new Date(mTimestampSec * 1000l).toString()
                + " Code: " + mCode
                + " : " + mMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeLong(mImageId);
        dest.writeLong(mRoomId);
        dest.writeLong(mSenderId);
        dest.writeLong(mRecipientId);
        dest.writeLong(mTimestampSec);
        dest.writeInt(mCode);
        final String messageParceled = mMessage == null ? "" : mMessage;
        dest.writeString(messageParceled);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            return new Action(source);
        }

        @Override
        public Object[] newArray(int size) {
            return new Action[size];
        }
    };
}
