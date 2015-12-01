package org.eztarget.realay.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eztarget.realay.R;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.ui.utils.ViewAnimator;
import org.eztarget.realay.utils.APIHelper;

/**
 * Created by michel on 28/11/14.
 */
public class LoginActivity extends BaseActivity {

    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    /**
     * User input to this Activity should be ignored if busy
     */
    private boolean mIsBusy = false;

    /**
     *
     */
    private boolean mDidFinish = false;

    /**
     * EditText View for the input requested by this Activity
     */
    private EditText mEditNameText;

    private CreateUserTask mCreateUserTask;

    /**
     * Activity Class name that will be passed on to a profile Activity after this one
     */
    private String mPrevActivityName;

    private boolean mDoShowProfileAfterCreation;

    private boolean mDoStartSessionAfterCreation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mEditNameText = (EditText) findViewById(R.id.edit_name_text_view);

        // Setting EditorActionListener for the EditText
        mEditNameText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                Log.d(TAG, "actionId: " + actionId);

                if (actionId == EditorInfo.IME_ACTION_DONE && !mIsBusy) {
                    performCreation();
                    return true;
                } else {
                    Log.d(LOG_TAG, "Unhandled actionId: " + actionId);
                    return false;
                }
            }
        });

        // Store the name of the Activity class to return to.
        // This will be passed on to the profile Activity,
        // so that it returns to the one BEFORE CreateUserActivity.
        final Bundle extras = getIntent().getExtras();
        if (extras != null && mPrevActivityName == null) {
            mPrevActivityName =
                    extras.getString(UIConstants.KEY_PREV_ACTIVITY);
            mDoShowProfileAfterCreation =
                    extras.getBoolean(UIConstants.EXTRA_DO_SHOW_MINE);
            mDoStartSessionAfterCreation =
                    extras.getBoolean(UIConstants.KEY_START_SESSION);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!mDidFinish) {
            if (mCreateUserTask != null) mCreateUserTask.cancel(true);
            Intent returnToHomeIntent = new Intent(this, RoomListActivity.class);
            returnToHomeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(returnToHomeIntent);
        }
    }

    public void onActionButtonClicked(View view) {
//        setGuiBusy(true);
        if (!mIsBusy) performCreation();
    }

    public void performCreation() {
        final String name = mEditNameText.getText().toString().trim();
        if (name.length() < 2) {
            Toast.makeText(this, R.string.name_not_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        mCreateUserTask = new CreateUserTask();
        mCreateUserTask.execute(name);
    }

    private void setGuiBusy(boolean isBusy) {
        mIsBusy = isBusy;

        mEditNameText.setEnabled(!mIsBusy);

        final View actionButtonProgressArea;
        actionButtonProgressArea = findViewById(R.id.group_action_progress);

        if (mIsBusy) {
//            mActionButton.setImageResource(R.drawable.ic_refresh_white_36dp);
//            ViewAnimator.scaleAnimateView(actionButtonProgressArea, false, 0.5f, 0.5f);
            ViewAnimator.scaleAnimateView(
                    actionButtonProgressArea,
                    false,
                    true,
                    100L,
                    0.5f,
                    0.5f,
                    new ViewAnimator.Callback() {
                        @Override
                        public void onAnimationEnd() {
                            findViewById(R.id.button_action).setVisibility(View.INVISIBLE);
                            findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                            ViewAnimator.scaleAnimateView(actionButtonProgressArea, true);
                        }
                    }
            );
        } else {
            FloatingActionButton actionButton;
            actionButton = (FloatingActionButton) findViewById(R.id.button_action);
            actionButton.setImageResource(R.drawable.ic_done_white_36dp);

            findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
        }
    }

    private void showError() {
        Toast.makeText(this, R.string.error_default, Toast.LENGTH_SHORT).show();
        onBackPressed();
    }

    public void onFacebookClick(View view) {
    }

    /**
     * Constructs a contact from the given Intent data.
     */
    private class CreateUserTask extends AsyncTask<String, Void, Long> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setGuiBusy(true);
        }

        @Override
        protected Long doInBackground(String... params) {
            if (params.length != 1 || params[0] == null) return -91L;
            final String userName = params[0].trim();
            if (TextUtils.isEmpty(userName)) return -92L;

            final User registeredUser = APIHelper.getLocalUser(getApplicationContext(), userName);
            if (registeredUser == null) return -93L;

            LocalUserManager.getInstance().setUser(getApplicationContext(), registeredUser);
            return registeredUser.getId();
        }

        @Override
        protected void onPostExecute(Long aLong) {
            super.onPostExecute(aLong);

            final long resultCode = aLong;

            setGuiBusy(false);


            final ViewAnimator.Callback finish = new ViewAnimator.Callback() {
                @Override
                public void onAnimationEnd() {
                    if (resultCode < 10L) {
                        Log.e(LOG_TAG, "Error code after execution: " + resultCode);
                        showError();
                        return;
                    }

                    mDidFinish = true;

                    if (mDoStartSessionAfterCreation) {
                        startJoinActivity();
                        return;
                    }

                    if (mDoShowProfileAfterCreation) {
                        // After creating the user, try to go to the UserDetailsActivity.
                        final Intent profileIntent;
                        profileIntent = LocalUserManager.getInstance().getProfileIntent(
                                LoginActivity.this,
                                true,
                                mPrevActivityName
                        );

                        if (profileIntent != null) startActivity(profileIntent);
                    }
                }
            };

            final View actionGroup = findViewById(R.id.group_action_progress);

            final ViewAnimator.Callback appear = new ViewAnimator.Callback() {
                @Override
                public void onAnimationEnd() {
                    findViewById(R.id.button_action).setVisibility(View.VISIBLE);
                    ViewAnimator.scaleAnimateView(
                            actionGroup,
                            true,
                            true,
                            0.5f,
                            0.5f,
                            finish
                    );
                }
            };

            // Hide, then reappear with the Button showing.
            ViewAnimator.scaleAnimateView(
                    actionGroup,
                    false,
                    true,
                    0.5f,
                    0.5f,
                    appear
            );

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            // Reset LocalUserManager.
        }

    }

    private void startJoinActivity() {
        startActivity(new Intent(this, JoinActivity.class));
    }

}
