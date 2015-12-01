package org.eztarget.realay.services;

import android.app.IntentService;
import android.content.Intent;

import org.eztarget.realay.Constants;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.UsersCache;
import org.eztarget.realay.ui.utils.ImageLoader;
import org.eztarget.realay.utils.APIHelper;

/**
 * Created by michel on 17/01/15.
 *
 */
public class UserQueryService extends IntentService {

    private static final String TAG = UserQueryService.class.getSimpleName();

    public UserQueryService() {
        super(TAG);
    }

    /**
     * {@inheritDoc}
     *
     * Requires a Long stored in the Extras Bundle using the key Constants.EXTRA_USER_ID.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        final long userId = intent.getLongExtra(Constants.EXTRA_USER_ID, -100l);

        final User user = APIHelper.getUser(getApplicationContext(), userId);
        if (user != null) {
            ImageLoader.with(getApplicationContext()).handle(user, true).startLoading();

            UsersCache.getInstance().addUser(user);
            ChatObjectContract.insertUser(getApplicationContext(), user);

            // Let the User lists know that some data was changed.
            Intent broadcast = new Intent(Constants.ACTION_USER_LIST_CHANGED);
            sendBroadcast(broadcast);
        }
    }
}
