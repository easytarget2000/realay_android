package org.eztarget.realay.managers;

import android.content.Context;
import android.content.Intent;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.User;
import org.eztarget.realay.services.ActionSendService;
import org.eztarget.realay.ui.LoginActivity;
import org.eztarget.realay.ui.UIConstants;
import org.eztarget.realay.ui.UserDetailsActivity;

/**
 * Created by michel on 26/11/14.
 */
public class LocalUserManager {

    /**
     * Singleton instance
     */
    private static LocalUserManager instance = null;

    private static final String LOG_TAG = LocalUserManager.class.getSimpleName();

    /**
     * Retrieved from preferences and stored here for quicker access than through User.getId().
     */
    private long mUserId;

    private User mUser;


    /**
     * Exists only to defeat instantiation
     */
    protected LocalUserManager() {
    }

    public static LocalUserManager getInstance() {
        if (instance == null) instance = new LocalUserManager();
        return instance;
    }

    public long getUserId(Context context) {
        if (mUserId < 10l) {
            mUser = PreferenceHelper.from(context).buildLocalUser();
            if (mUser == null) return -969l;
            mUserId = mUser.getId();
        }
        return mUserId;
    }

    public User getUser(Context context) {
        if (mUser != null) return mUser;
        return PreferenceHelper.from(context).buildLocalUser();
    }


    /**
     * @param activityContext            Context in which to create the Intent
     * @param doShowProfileAfterCreation Start UserDetailsActivity after creating the user
     * @param prevActivityClassName      Activity that will be returned to from the UserDetailsActivity
     *                                   if the CreateUserActivity is in between,
     *                                   so that the creation process will be skipped onBackPressed.
     * @return An Intent that starts an activity: UserDetailsActivity (local) or CreateUserActivity
     */
    public Intent getProfileIntent(
            final Context activityContext,
            boolean doShowProfileAfterCreation,
            final String prevActivityClassName
    ) {
        final Intent activity;
        if (mUser == null) {
            mUser = PreferenceHelper.from(activityContext).buildLocalUser();
        }

        if (mUser == null) {
            // The CreateUserActivity needs to be displayed because there is no local user, yet.
            activity = new Intent(activityContext, LoginActivity.class);
        } else {
            // The UserDetailsActivity can be displayed because there is a local user stored.
            activity = new Intent(activityContext, UserDetailsActivity.class);
        }

        // Go to a certain Activity after creating the profile.
        activity.putExtra(UIConstants.EXTRA_DO_SHOW_MINE, doShowProfileAfterCreation);
        // Return to a certain Activity when returning from the FOLLOWING Activity.
        activity.putExtra(UIConstants.KEY_PREV_ACTIVITY, prevActivityClassName);
        return activity;
    }

    public void setUser(Context context, final User newUserData) {
        if (newUserData == null) return;
        mUser = newUserData;
        PreferenceHelper.from(context).storeLocalUser(mUser);
    }

    public void updateStorage(Context context) {
        if (context == null) return;

        UsersCache.getInstance().addUser(mUser);
        PreferenceHelper.from(context).storeLocalUser(mUser);

        // The update is handled like an Action here,
        // so that it can be sent through the ActionSendService,
        // the benefit being that it automatically queues the update for possible retries.
        final long roomId = SessionMainManager.getInstance().getRoomId(context);
        final long localUserId = getUserId(context);
        final Action updateAction;
        updateAction = Action.buildPlaceholderAction(roomId, localUserId, Action.ACTION_CODE_UPD_USER);
        if (updateAction == null) return;

        Intent actionSender = new Intent(context, ActionSendService.class);
        actionSender.putExtra(Constants.EXTRA_ACTION, updateAction);
        context.startService(actionSender);
    }
}
