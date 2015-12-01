package org.eztarget.realay.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.UsersCache;

/**
 * Created by michel on 26/11/14.
 */
public class User extends ChatObject {

    private static final String LOG_TAG = User.class.getSimpleName();

    /**
     * The mime type of a directory of items
     */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.org.eztarget.realay.users";

    /**
     * The mime type of a single item.
     */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.org.eztarget.realay.users";

    /**
     * Full, readable name
     */
    private String mName;

    /**
     * Readable status message for User lists and profiles
     */
    private String mStatusMessage;

    /**
     * Mail address
     */
    private String mMailAddress;

    /**
     * Telephone number
     */
    private String mPhoneNumber;

    /**
     * Website; any readable string; not necessarily a URL
     */
    private String mWebsite;

    /**
     * Instagram account handle
     */
    private String mIgHandle;

    /**
     * Facebook account handle
     */
    private String mFbName;

    /**
     * Twitter account handle
     */
    private String mTwitterName;

    public User(final long userId, final String name, final String statusMessage) {
        mId = userId;
        mName = name;
        mStatusMessage = statusMessage;

        // Always build solid colors (max. alpha) out of the given value.
        final int red = (int) (Math.random() * 255);
        final int blue = (int) (Math.random() * 255);
//        mColor = Color.argb(255, Color.red(red), Color.green(200), Color.blue(blue));

//        Log.d("User Constructor", mId + " " + mName + " " + Integer.toHexString(mColor));
        // Avoid image updates by setting the update really, really high.
    }

    public User(
            final long userId,
            final long imageId,
            final String name,
            final String statusMessage,
            final String mailAddress,
            final String phoneNumber,
            final String website,
            final String igHandle,
            final String fbHandle,
            final String twitterHandle
    ) {
        mId = userId;
        mImageId = imageId;
        mName = name;
        mStatusMessage = statusMessage;
        mMailAddress = mailAddress;
        mPhoneNumber = phoneNumber;
        mWebsite = website;
        mIgHandle = igHandle;
        mFbName = fbHandle;
        mTwitterName = twitterHandle;
    }

    @Override
    public String toString() {
        return mId + ": " + mName;
    }

    public static User fromContentProvider(Context context, final long userId) {
        if (context == null) return null;
        context = context.getApplicationContext();
        // Load User with given ID.
        Cursor userCursor = context.getContentResolver().query(
                ChatObjectContract.CONTENT_URI_USERS,
                null,                               // SELECT *
                BaseColumns._ID + "=" + userId,     // WHERE _id = userId
                null,
                null
        );

        if (userCursor != null) {
            userCursor.moveToFirst();
            final User user = UsersCache.getInstance().fetch(userCursor, true);
            if (user != null) return user;
        }

//        Log.d(LOG_TAG, "No local result: " + BaseColumns._ID + "=" + userId);
        return null;
    }

    public String getName() {
        if (TextUtils.isEmpty(mName)) return getFakeName(mId);
        return mName;
    }

    private static String getFakeName(long userId) {
        char k = (char) (userId / 128L);
        return "Lin" + k + "_" + userId * 11L;
    }

    public String getStatusMessage() {
        if (mStatusMessage == null || TextUtils.isEmpty(mStatusMessage)) return "---";
        return mStatusMessage;
    }

    public String getPhoneNumber() {
        if (mPhoneNumber == null) return "";
        return mPhoneNumber;
    }

    public void setPhoneNumber(final String phoneNumber) {
        if (phoneNumber == null) return;
        mPhoneNumber = phoneNumber;
    }

    public String getEmailAddress() {
        if (mMailAddress == null) return "";
        return mMailAddress;
    }

    public void setStatusMessage(final String statusMessage) {
        if (!TextUtils.isEmpty(statusMessage)) mStatusMessage = statusMessage;
        else mStatusMessage = "";
    }

    public void setMailAddress(final String mailAddress) {
        if (mailAddress == null) return;
        mMailAddress = mailAddress;
    }

    public String getWebsite() {
        if (mWebsite == null) return "";
        return mWebsite;
    }

    public void setWebsite(final String website) {
        if (website == null) return;
        mWebsite = website;
    }

    public String getIgName() {
        if (mIgHandle == null) return "";
        return mIgHandle;
    }

    public void setIgHandle(final String igHandle) {
        mIgHandle = igHandle;
    }

    public String getFacebookId() {
        if (mFbName == null) return "";
        return mFbName;
    }

    public void setFacebookId(final String fbName) {
        mFbName = fbName;
    }

    public String getTwitterName() {
        if (mTwitterName == null) return "";
        return mTwitterName;
    }

    public void setTwitterName(final String twitterName) {
        mTwitterName = twitterName;
    }

    public void setName(final String name) {
        if (!TextUtils.isEmpty(name)) mName = name;
    }

    public boolean isLocalUser(final Context context) {
        return mId == LocalUserManager.getInstance().getUserId(context);
    }

}
