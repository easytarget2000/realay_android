package org.eztarget.realay.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionActionsManager;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.ui.utils.ViewAnimator;
import org.eztarget.realay.utils.APIHelper;

/**
 * Created by michel on 31/01/15.
 *
 * Activity that controls and displays the status of the Task
 * which performs the necessary API calls to join a Room
 */
public class JoinActivity extends Activity {

    private static final String TAG = JoinActivity.class.getSimpleName();

    private static final String TASK_TAG = SessionStartTask.class.getSimpleName();

    private static final boolean DEBUG = false;

    private static final String KEY_IS_JOINING = "KEY_IS_JOINING";

    private static final String KEY_CURRENT_MESSAGE_ID = "KEY_CURRENT_MESSAGE_ID";

    private static final int COLOR_TRANSITION_TIME = 6 * 1000;

    private static final long TIME_OUT_DELAY = 20L * 1000L;

    private boolean mIsJoining = false;

    private boolean mIsCanceled = false;

    private boolean mDoShowMessages = true;

    private int mCurrentMessageId;

    private SessionStartTask mStartTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_join);

        if (SessionMainManager.getInstance().didLogin()) {
            startFinishAnimation();
            return;
        }

        if (savedInstanceState != null) {
            mIsJoining = savedInstanceState.getBoolean(KEY_IS_JOINING);
            mCurrentMessageId = savedInstanceState.getInt(KEY_CURRENT_MESSAGE_ID, R.string.join);
            if (mCurrentMessageId == 0) return;
            final TextView statusView = (TextView) findViewById(R.id.text_join_status);
            statusView.setText(mCurrentMessageId);
        }

        final Runnable timeOutRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mIsCanceled) {
                    if (mDoShowMessages) {
                        onBackPressed();
                    } else if (SessionMainManager.getInstance().didLogin()) {
                        startFinishAnimation();
                    }
                }
            }
        };
        final Runnable slowDownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mDoShowMessages || mIsCanceled) return;

                if (SessionMainManager.getInstance().didLogin()) {
                    startFinishAnimation();
                    return;
                }

                mCurrentMessageId = R.string.takes_longer;
                final TextView statusView = (TextView) findViewById(R.id.text_join_status);
                ViewAnimator.quickFade(statusView);
                statusView.setText(mCurrentMessageId);
                new Handler().postDelayed(timeOutRunnable, TIME_OUT_DELAY * 2L);
            }
        };

        new Handler().postDelayed(slowDownRunnable, TIME_OUT_DELAY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Immediately cancel, if the area has been left.
        final boolean isInRadius = LocationWatcher.from(this).isInSessionRadius(false);
        if (!isInRadius) {
            if (mStartTask != null && !mStartTask.isCancelled()) mStartTask.cancel(true);
        }

        // Check again, if the local User has been loaded now.
        if (LocalUserManager.getInstance().getUser(this) == null) {
            // The local user has not been created or logged in, prompt for details
            // to create a new User and then start the Session.
            if (mStartTask != null) mStartTask.cancel(true);

            Intent createUserActivity = new Intent(this, LoginActivity.class);
            // CreateUserActivity calls startListening() after handling the local user login.
            createUserActivity.putExtra(UIConstants.KEY_START_SESSION, true);
            startActivity(createUserActivity);
            finish();
            return;
        }

        // If the login has been finished, skip everything else.
        if (SessionMainManager.getInstance().didLogin()) {
            startFinishAnimation();
            return;
        }

        if (mIsJoining && mCurrentMessageId > 0) {
            final TextView statusView = (TextView) findViewById(R.id.text_join_status);
            statusView.setText(mCurrentMessageId);
            return;
        }

        TransitionDrawable transition;
        transition = (TransitionDrawable) findViewById(R.id.layout_join).getBackground();
        transition.startTransition(COLOR_TRANSITION_TIME);

        // Only start a new session if the old one has been terminated correctly.
        // On resetting a session, the tasks have to be purged.
        if (mStartTask == null) {
            mStartTask = new SessionStartTask();
            mStartTask.execute();
            mIsJoining = true;
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_JOINING, mIsJoining);
        outState.putInt(KEY_CURRENT_MESSAGE_ID, mCurrentMessageId);
    }

    @Override
    public void onBackPressed() {
        if (mStartTask != null && !mStartTask.isCancelled()) mStartTask.cancel(true);

        if (!SessionMainManager.getInstance().didLogin()) {
            mIsCanceled = true;
            SessionMainManager.getInstance().leaveSession(this, "CanceledLogin");

            Intent roomListIntent = new Intent(this, RoomListActivity.class);
            roomListIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(roomListIntent);
        }
    }

    private void startFinishAnimation() {

        final ViewAnimator.Callback finishCallback;

        final View icon = findViewById(R.id.image_check_mark);

        final Intent publicConversation = new Intent(this, PublicConversationActivity.class);

        finishCallback = new ViewAnimator.Callback() {
            @Override
            public void onAnimationEnd() {
                ViewAnimator.scaleAnimateView(
                        icon,
                        true,
                        false,
                        1000L,
                        0.5f,
                        0.5f,
                        new ViewAnimator.Callback() {
                            @Override
                            public void onAnimationEnd() {
                                if (mIsCanceled) {
                                    onBackPressed();
                                } else {
                                    mStartTask = null;
                                    finish();
                                    startActivity(publicConversation);
                                }
                            }
                        }
                );
            }
        };

        ViewAnimator.scaleAnimateView(
                findViewById(R.id.text_join_status),
                false,
                true,
                0.5f,
                0.5f,
                finishCallback
        );

        ViewAnimator.scaleAnimateView(findViewById(R.id.progress_join), false);
    }

    private class SessionStartTask extends AsyncTask<Void, Integer, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            // Try signing in.
            if (!APIHelper.doJoinRoom(getApplicationContext())) return 100;

            publishProgress(R.string.loading_user);
            // Get the fresh list of Users and all their details in this Room
            final boolean didUpdateUsers = APIHelper.getSessionUsers(getApplicationContext());
            if (!didUpdateUsers) return 102;
            // Remove all old, unneeded Actions.
            SessionActionsManager.from(JoinActivity.this).prepareSession();
            cleanActionsTables();

            // Query all recent public messages.
            publishProgress(R.string.loading_messages);
            final int queryActionsResult;
            queryActionsResult = APIHelper.getActions(getApplicationContext());
            if (queryActionsResult < 0) {
                Log.e(TASK_TAG, "Query Actions returned " + queryActionsResult + ".");
                return 404 - queryActionsResult;
            }

            // Start the Action query heartbeat.
            if (SessionMainManager.getInstance().startSession(getApplicationContext())) {
                return 0;
            } else {
                return 106;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values.length != 1) return;
            final String status = getResources().getString(values[0]);
            if (TextUtils.isEmpty(status)) return;

            mCurrentMessageId = values[0];

            final TextView statusView = (TextView) findViewById(R.id.text_join_status);
            statusView.setText(status);
        }

        @Override
        protected void onPostExecute(Integer anInteger) {
            super.onPostExecute(anInteger);
            mDoShowMessages = false;

            if (!mIsCanceled) {
                if (anInteger != 0) {
                    Log.e(TASK_TAG, "An error occurred during log-in Task.");

                    final String message;
                    final String errorMessage = getResources().getString(R.string.error_default);
                    if (DEBUG) message = errorMessage + " (" + anInteger + ")";
                    else message = errorMessage;

                    Toast.makeText(JoinActivity.this, message, Toast.LENGTH_SHORT).show();
                    onBackPressed();
                } else {
                    startFinishAnimation();
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.d(TASK_TAG, "Login cancelled.");
            onBackPressed();
        }

        /**
         * Deletes all entries in the Public Actions table and all queued in the Private table
         */
        private void cleanActionsTables() {
            getContentResolver().delete(
                    ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC,
                    null,
                    null
            );
            getContentResolver().delete(
                    ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE,
                    BaseColumns.MSG_IN_QUEUE + "=1",
                    null
            );
        }
    }

}
