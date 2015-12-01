package org.eztarget.realay;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;

import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.LocalUserManager;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Created by michel on 30/12/14.
 */
public class PreferenceHelper {

    private static final String TAG = PreferenceHelper.class.getSimpleName();

    /**
     * Singleton instance
     */
    private static PreferenceHelper instance = null;

    private static final String KEY_BACKGROUND_UPDATE = "pref_background_update";

    private static final String KEY_BOUNCER_REASON = "bnc_r";

    private static final String KEY_BOUNCER_NUM_OF_WARNINGS = "bnc_n";

    private static final String KEY_BOUNCER_KICK_TIME = "bnc_t";

    private static final String KEY_DEVICE_ID = "device_id";

    private static final String KEY_DO_RE_LOGIN = "do_re_login";

    private static final String KEY_NOTIFICATIONS_PRV = "pref_notifications_private";

    private static final String KEY_NOTIFICATIONS_PUB = "pref_notifications_public";

    private static final String KEY_DID_SHOW_PASSWORD_INFO = "DID_SHOW_PASSWORD_INFO";

    private static final String KEY_IN_BACKGROUND = "IN_BACKGROUND";

    private static final String KEY_LOCATION_ACCURACY = "loc_acc";

    private static final String KEY_LOCATION_TIME = "loc_time";

    private static final String KEY_LOCATION_LAT = "loc_lat";

    private static final String KEY_LOCATION_LNG = "loc_lng";

    private static final String KEY_UPDATE_LOCATION_ACCURACY = "upd_loc_acc";

    private static final String KEY_UPDATE_LOCATION_TIME = "upd_loc_time";

    private static final String KEY_UPDATE_LOCATION_LAT = "upd_loc_lat";

    private static final String KEY_UPDATE_LOCATION_LNG = "upd_loc_lng";

    private static final String KEY_LOCAL_USER_ID = "LOCAL_USER_ID";

    private static final String KEY_LOCAL_USER_IMAGE_ID = "LOCAL_USER_IMAGE_ID";

    private static final String KEY_LOCAL_USER_NAME = "LOCAL_USER_NAME";

    private static final String KEY_LOCAL_USER_STATUS = "LOCAL_USER_STATUS";

    private static final String KEY_LOCAL_USER_EMAIL = "LOCAL_USER_PHONE";

    private static final String KEY_LOCAL_USER_PHONE = "LOCAL_USER_PHONE";

    private static final String KEY_LOCAL_USER_WEBSITE = "LOCAL_USER_WEBSITE";

    private static final String KEY_LOCAL_USER_IG_HANDLE = "LOCAL_USER_IG";

    private static final String KEY_LOCAL_USER_FB_HANDLE = "LOCAL_USER_FB";

    private static final String KEY_LOCAL_USER_TWITTER_HANDLE = "LOCAL_USER_TWITTER";

    private static final String KEY_LAST_PROVIDER_DIALOG_MILLIS = "KEY_LAST_PROVIDER_DIALOG_MILLIS";

    private static final String KEY_USE_IMPERIAL = "pref_use_imperial";

    private static final String KEY_IS_POWER_SAVING = "IS_POWER_SAVING";

    private static final String KEY_RUN_ONCE = "RUN_ONCE";

    private static final String KEY_SESSION_ID = "KEY_DETAILS";


    private Context mAppContext;

//    private BackupManager mBackupManager;

    private SharedPreferences mPrefs;

    private PreferenceHelper(Context context) {
        mAppContext = context.getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
//        mBackupManager = new BackupManager(context);

        if (Locale.getDefault().getISO3Country().equalsIgnoreCase(Locale.US.getISO3Country())) {
            SharedPreferences.Editor editor = getNewEditor();
            editor.putBoolean(KEY_USE_IMPERIAL, true);
        }
    }

    public static PreferenceHelper from(Context context) {
        if (instance == null && context != null) {
            instance = new PreferenceHelper(context);
        }
        return instance;
    }


    public SharedPreferences.Editor getNewEditor() {
        return mPrefs.edit();
    }

    public void savePreferences(SharedPreferences.Editor editor, final boolean doBackup) {
        editor.apply();
//        if (mBackupManager == null || !doBackup) return;
//        mBackupManager.dataChanged();
    }

    private void edit(final String key, final String value, final boolean doBackup) {
        if (mPrefs == null) return;

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(key, value);
        editor.apply();

//        if (mBackupManager == null || !doBackup) return;
//        mBackupManager.dataChanged();
    }

    private void edit(final String key, final boolean value, final boolean doBackup) {
        if (mPrefs == null) return;

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putBoolean(key, value);
        editor.apply();

//        if (mBackupManager == null || !doBackup) return;
//        mBackupManager.dataChanged();
    }

    private void edit(final String key, final long value, final boolean doBackup) {
        if (mPrefs == null) return;

        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putLong(key, value);
        editor.apply();

//        if (mBackupManager == null || !doBackup) return;
//        mBackupManager.dataChanged();
    }

    /*
    LOCAL USER
     */

    public User buildLocalUser() {
        if (mPrefs == null) {
            Log.e(TAG, "SharedPreferences object downloadIfNeeded not been initialised.");
            return null;
        }

        // Look for the local user ID in the preferences.
        // This ID should never change unless the user deletes all app data.
        final long userId = mPrefs.getLong(KEY_LOCAL_USER_ID, -1l);
        if (userId < 10l) {
            Log.d(TAG, "Local User data not stored in Preferences.");
            return null;
        }

        // Try to get the User object from the content provider first.
        User restoredUser = User.fromContentProvider(mAppContext, userId);
        if (restoredUser != null) {
//            Log.d(TAG, "Initialised local User from Content Provider.");
            return restoredUser;
        }

        // Load all of the User data from the preferences, in case the database got purged.
        final String name = mPrefs.getString(KEY_LOCAL_USER_NAME, null);
        if (name == null) {
            Log.d(TAG, "Local User data not stored in Preferences.");
            return null;
        }

        restoredUser = new User(
                userId,
                mPrefs.getLong(KEY_LOCAL_USER_IMAGE_ID, -202L),
                name,
                mPrefs.getString(KEY_LOCAL_USER_STATUS, ""),
                mPrefs.getString(KEY_LOCAL_USER_EMAIL, ""),
                mPrefs.getString(KEY_LOCAL_USER_PHONE, ""),
                mPrefs.getString(KEY_LOCAL_USER_WEBSITE, ""),
                mPrefs.getString(KEY_LOCAL_USER_IG_HANDLE, ""),
                mPrefs.getString(KEY_LOCAL_USER_FB_HANDLE, ""),
                mPrefs.getString(KEY_LOCAL_USER_TWITTER_HANDLE, "")
        );

        // Store the new user in the content provider.
        ChatObjectContract.insertUser(mAppContext, restoredUser);
        return restoredUser;
    }

    public void storeLocalUser(final User localUser) {
        SharedPreferences.Editor prefsEditor = getNewEditor();

        prefsEditor.putLong(KEY_LOCAL_USER_ID, localUser.getId());
        prefsEditor.putLong(KEY_LOCAL_USER_IMAGE_ID, localUser.getImageId());
        prefsEditor.putString(KEY_LOCAL_USER_NAME, localUser.getName());
        prefsEditor.putString(KEY_LOCAL_USER_STATUS, localUser.getStatusMessage());
        prefsEditor.putString(KEY_LOCAL_USER_PHONE, localUser.getPhoneNumber());
        prefsEditor.putString(KEY_LOCAL_USER_EMAIL, localUser.getEmailAddress());
        prefsEditor.putString(KEY_LOCAL_USER_WEBSITE, localUser.getWebsite());
        prefsEditor.putString(KEY_LOCAL_USER_IG_HANDLE, localUser.getIgName());
        prefsEditor.putString(KEY_LOCAL_USER_FB_HANDLE, localUser.getFacebookId());
        prefsEditor.putString(KEY_LOCAL_USER_TWITTER_HANDLE, localUser.getTwitterName());

        // Store the changed preferences and backup if possible.
        savePreferences(prefsEditor, true);
        ChatObjectContract.insertUser(mAppContext, localUser);
    }

    /*
    Session Data
     */

    public boolean doReLogin() {
        return mPrefs != null && mPrefs.getBoolean(KEY_DO_RE_LOGIN, false);
    }

    public long getSessionRoomId() {
        if (mPrefs == null) return -98L;
        final long sessionId = mPrefs.getLong(KEY_SESSION_ID, -97L);
        if (sessionId < 10L) return sessionId;
        final long userId = LocalUserManager.getInstance().getUserId(mAppContext);
        if (userId < 10L) return userId;

        return sessionId / userId;
    }

    public void setSessionRoomId(final long roomId) {
        final long userId = LocalUserManager.getInstance().getUserId(mAppContext);
        if (userId < 10L) return;

        edit(KEY_SESSION_ID, roomId * userId, false);
        edit(KEY_DO_RE_LOGIN, true, false);
    }

    public void leaveSession() {
        setSessionRoomId(-77L);
        edit(KEY_DO_RE_LOGIN, false, false);
    }

    /*
    Bouncer
     */

    public void resetBouncerAttributes() {
        edit(KEY_BOUNCER_REASON, 0, false);
        edit(KEY_BOUNCER_NUM_OF_WARNINGS, 0, false);
        edit(KEY_BOUNCER_KICK_TIME, 0L, false);
    }

    public void storeBouncerAttributes(
            final int reason,
            final int numberOfWarnings,
            final long kickTimeMillis
    ) {
        edit(KEY_BOUNCER_REASON, reason, false);
        edit(KEY_BOUNCER_NUM_OF_WARNINGS, numberOfWarnings, false);
        edit(KEY_BOUNCER_KICK_TIME, kickTimeMillis, false);
    }

    /*
    LAST LOCATION
     */

    public Location getLastLocation() {
        final Location lastLocation;
        lastLocation = new Location(Constants.CONSTRUCTED_LOCATION_PROVIDER);
        lastLocation.setLatitude(mPrefs.getFloat(KEY_LOCATION_LAT, Float.MIN_VALUE));
        lastLocation.setLongitude(mPrefs.getFloat(KEY_LOCATION_LNG, Float.MIN_VALUE));
        lastLocation.setTime(mPrefs.getLong(KEY_LOCATION_TIME, 0L));
        lastLocation.setAccuracy(mPrefs.getFloat(KEY_LOCATION_ACCURACY, 50f));
        return lastLocation;
    }

    public void storeLastLocation(final Location location) {
        SharedPreferences.Editor editor = getNewEditor();
        // Save the last update time and place to the Shared Preferences.
        editor.putFloat(KEY_LOCATION_LAT, (float) location.getLatitude());
        editor.putFloat(KEY_LOCATION_LNG, (float) location.getLongitude());
        editor.putLong(KEY_LOCATION_TIME, System.currentTimeMillis());
        editor.putFloat(KEY_LOCATION_ACCURACY, location.getAccuracy());
        savePreferences(editor, false);
    }

    public Location getLastUpdateLocation() {
        final Location lastLocation;
        lastLocation = new Location(Constants.CONSTRUCTED_LOCATION_PROVIDER);
        lastLocation.setLatitude(mPrefs.getFloat(KEY_LOCATION_LAT, Float.MIN_VALUE));
        lastLocation.setLongitude(mPrefs.getFloat(KEY_LOCATION_LNG, Float.MIN_VALUE));
        lastLocation.setTime(mPrefs.getLong(KEY_LOCATION_TIME, 0L));
        lastLocation.setAccuracy(mPrefs.getFloat(KEY_LOCATION_ACCURACY, 50f));
        return lastLocation;
    }

    public void storeLastUpdateLocation(final Location location) {
        SharedPreferences.Editor editor = getNewEditor();
        // Save the last update time and place to the Shared Preferences.
        editor.putFloat(KEY_UPDATE_LOCATION_LAT, (float) location.getLatitude());
        editor.putFloat(KEY_UPDATE_LOCATION_LNG, (float) location.getLongitude());
        editor.putLong(KEY_UPDATE_LOCATION_TIME, System.currentTimeMillis());
        editor.putFloat(KEY_UPDATE_LOCATION_ACCURACY, location.getAccuracy());
        savePreferences(editor, false);
    }

    /*
    APP IN BACKGROUND
     */

    public boolean isInBackground() {
        return mPrefs != null && mPrefs.getBoolean(KEY_IN_BACKGROUND, true);
    }

    public void setInBackground(final boolean isInBackground) {
        SharedPreferences.Editor prefsEditor = getNewEditor();
        prefsEditor.putBoolean(KEY_IN_BACKGROUND, isInBackground);
        savePreferences(prefsEditor, false);
    }

    /*
    App did run once / on-boarding
     */

    /**
     * @return True, if the app has been run once before, i.e. this method has been called before.
     */
    public boolean didRunOnce() {
        if (mPrefs == null) return true;
        if (!mPrefs.getBoolean(KEY_RUN_ONCE, false)) {
            // Acknowledge that the app has been run once now but return the old value.
            edit(KEY_RUN_ONCE, true, false);
            return false;
        }
        return true;
    }

    /*
    Authorisation ID
     */

    public String getAuthId() {
        if (mPrefs == null) return null;
        final String storedId = mPrefs.getString(KEY_DEVICE_ID, null);
        if (!TextUtils.isEmpty(storedId)) {
            return storedId;
        } else {
            final String deviceId = getDeviceID();
            edit(KEY_DEVICE_ID, deviceId, false);
            return deviceId;
        }
    }

    private String getDeviceID() {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            return (String) (get.invoke(c, "ro.serialno", "unknown"));
        } catch (Exception e) {
            Log.w(TAG, "Cannot read device ID via ro.serial.no");
        }

        final TelephonyManager telMan;
        telMan = (TelephonyManager) mAppContext.getSystemService(Context.TELEPHONY_SERVICE);

        if (telMan != null) {
            String deviceId;

            try {
                deviceId = telMan.getDeviceId();
                if (deviceId != null) return deviceId;

                deviceId = telMan.getSimSerialNumber();
                if (deviceId != null) return deviceId;
            } catch (SecurityException secEx) {
                Log.w(TAG, secEx.toString());
            }
        }

        Log.e(TAG, "Could not find device ID.");
        return Display.DEFAULT_DISPLAY + "XX" + System.currentTimeMillis();
    }

    /*
    FOLLOW LOCATION IN BACKGROUND
     */

    /**
     * @return Tells Services that are optional if they should run in the background;
     * depends on user preferences, as well as the current power mode;
     * Commonly, PowerStateChangedReceiver sets the power saving flag, if the battery is low
     * and resets it when it is charged
     */
    public boolean doFollowBackground() {
        if (mPrefs == null) return true;
        if (mPrefs.getBoolean(KEY_BACKGROUND_UPDATE, true)) {
            if (!mPrefs.getBoolean(KEY_IS_POWER_SAVING, false)) return true;
        }
        return false;
    }

    /*
    NOTIFICATIONS
     */

    public boolean doShowPrivateNotifications() {
        return mPrefs != null && mPrefs.getBoolean(KEY_NOTIFICATIONS_PRV, true);
    }

    public boolean doShowPublicNotifications() {
        return mPrefs != null && mPrefs.getBoolean(KEY_NOTIFICATIONS_PUB, false);
    }

    /*
    UNITS
     */

    public boolean doUseImperial() {
        return mPrefs != null && mPrefs.getBoolean(KEY_USE_IMPERIAL, false);
    }

    /*
    Location Provider AlertDialogs
     */

    public void setProviderDialogLastMillis() {
        SharedPreferences.Editor editor = getNewEditor();
        editor.putLong(KEY_LAST_PROVIDER_DIALOG_MILLIS, System.currentTimeMillis());
        savePreferences(editor, false);
    }

    public long getProviderDialogLastMillis() {
        if (mPrefs == null) return 0L;
        return mPrefs.getLong(KEY_LAST_PROVIDER_DIALOG_MILLIS, 0l);
    }

    /*
    Password information View
     */

    public boolean doShowPasswordInformation() {
        if (mPrefs == null) return true;
        final boolean doShowPasswordInformation;
        doShowPasswordInformation = !mPrefs.getBoolean(KEY_DID_SHOW_PASSWORD_INFO, false);
        return doShowPasswordInformation;
    }

    public void ackPasswordInformation() {
        if (doShowPasswordInformation()) {
            final SharedPreferences.Editor editor = getNewEditor();
            editor.putBoolean(KEY_DID_SHOW_PASSWORD_INFO, true);
            savePreferences(editor, false);
        }
    }

    /*
    Low-battery / power saving:
     */

    public void isPowerSaving(final boolean isEnabledNow) {
        edit(KEY_IS_POWER_SAVING, isEnabledNow, false);
    }

}
