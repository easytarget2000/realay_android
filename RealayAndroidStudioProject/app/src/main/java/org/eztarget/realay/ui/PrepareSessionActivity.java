package org.eztarget.realay.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.R;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.ui.utils.FormatHelper;
import org.eztarget.realay.ui.utils.ViewAnimator;
import org.eztarget.realay.utils.SharingGuide;

/**
 * Created by michel on 03/12/14.
 * <p/>
 * Super class for Activities that can lead to JoinActivity:
 * MapActivity & RoomDetailsActivity
 * <p/>
 * These Activities can also be opened during a session which changes parts of their functionality;
 */
public class PrepareSessionActivity extends BaseActivity {

    private static final String TAG = PrepareSessionActivity.class.getSimpleName();

    private static final boolean DEBUG = false;

    protected EditText mEditPasswordView;

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (PreferenceHelper.from(this).doShowPasswordInformation()) {
            new Handler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            updateInformationView(true);
                        }
                    },
                    2400L
            );
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (SessionMainManager.getInstance().didLogin()) {
            Intent returnToPublicIntent = new Intent(this, PublicConversationActivity.class);
            returnToPublicIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(returnToPublicIntent);
        }
    }

    protected void setupPasswordBar() {
        final View joinButton = findViewById(R.id.image_join_button);

        if (SessionMainManager.getInstance().didLogin()) {
            // Hide the entire password bar, if the session has already been started.
            findViewById(R.id.edit_text_password).setVisibility(View.GONE);
            joinButton.setVisibility(View.GONE);
            // Show the share button instead.
            findViewById(R.id.group_share).setVisibility(View.VISIBLE);
            return;
        } else {
            findViewById(R.id.edit_text_password).setVisibility(View.VISIBLE);
            joinButton.setVisibility(View.VISIBLE);
            findViewById(R.id.group_share).setVisibility(View.GONE);
        }

        if (mEditPasswordView != null) return;
        // If needed, set up the password listeners.
        mEditPasswordView = (EditText) findViewById(R.id.edit_text_password);
        mEditPasswordView.setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        switch (actionId) {
                            case EditorInfo.IME_ACTION_GO:
                            case EditorInfo.IME_ACTION_DONE:
                                onJoinButtonClicked(joinButton);
                                return true;
                        }
                        return false;
                    }
                }
        );
        mEditPasswordView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            new Handler().postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            updateInformationView(true);
                                        }
                                    },
                                    4000L
                            );
                            v.setOnTouchListener(null);
                            return true;
                        }
                        return false;
                    }
                }
        );
    }

    public void onJoinButtonClicked(View view) {
        if (DEBUG) {
            Log.d(TAG, "Debug mode on. Ignoring password and location.");
            startActivity(new Intent(this, JoinActivity.class));
            return;
        }

        final Room room = SessionMainManager.getInstance().getRoom(this, false);
        if (room == null) {
            onBackPressed();
            return;
        }

        // Check if the Room can be entered already.
        if (room.getStartDateSec() > (System.currentTimeMillis() / 1000L)) {
            ViewAnimator.wiggleIt(view);
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            final String format = getResources().getString(R.string.not_started_yet);
            final String message;
            message = String.format(format, FormatHelper.buildRoomStartHour(this, room));
            alert.setMessage(message);
            alert.setTitle(R.string.try_again);
            alert.show();
            return;
        }

        // Check if the Room can still be entered.
        final long endDateSec = room.getEndDateSec();
        if (endDateSec > 1000L && System.currentTimeMillis() > (endDateSec * 1000L)) {
            ViewAnimator.wiggleIt(view);
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            final String format = getResources().getString(R.string.part_of_event);
            final String message;
            message = String.format(format, FormatHelper.buildRoomEndHour(this, room, false));
            alert.setMessage(message);
            alert.setTitle(R.string.too_late);
            alert.show();
            return;
        }

        // At last, check if the location is within the radius.
        final LocationWatcher locWatcher = LocationWatcher.from(this);

        if (locWatcher.isInSessionRadius(true)) {
            final String requiredPw = room.getPassword();
            final String enteredPw = mEditPasswordView.getText().toString().trim();
            if (requiredPw == null || requiredPw.equals(enteredPw)) {
                // Everything is fine. The password is right and the location is within the radius.

                ViewAnimator.disappearRight(
                        view,
                        false,
                        new ViewAnimator.Callback() {
                            @Override
                            public void onAnimationEnd() {
                                final Intent joinActivity = new Intent(
                                        PrepareSessionActivity.this,
                                        JoinActivity.class
                                );
                                startActivity(joinActivity);
                            }
                        });
            } else {
                // A wrong password has been entered.

                ViewAnimator.wiggleIt(view);
                final AlertDialog.Builder alert = new AlertDialog.Builder(this);
                final String format = getResources().getString(R.string.wrong_password);
                final String message = String.format(format, room.getTitle());
                alert.setMessage(message);
                alert.setTitle(R.string.try_again);
                alert.show();
            }

        } else {
            // We are no longer inside of the radius.

            ViewAnimator.wiggleIt(view);
            final Resources resources = getResources();

            if (!locWatcher.didEnableProvider() || (locWatcher.getLocation(false) == null)) {
                showLocationDialog(
                        resources.getString(R.string.where_are_you),
                        resources.getString(R.string.before_can_join)
                );
            } else {
                final String distanceFormat = resources.getString(R.string.currently_distance_away);
                final boolean doUseImperial = PreferenceHelper.from(this).doUseImperial();
                final String distDialogTitle = String.format(
                        distanceFormat,
                        FormatHelper.buildRoomDistance(this, room, doUseImperial)
                );
                showLocationDialog(
                        distDialogTitle,
                        resources.getString(R.string.before_can_join)
                );
            }
        }
    }


    protected void updateInformationView(final boolean doShow) {
        if (doShow) {
            final TextView informationView;
            informationView = (TextView) findViewById(R.id.text_information);
            if (LocationWatcher.from(this).isInSessionRadius(true)) {
                informationView.setText(R.string.find_the_password);
            } else {
                informationView.setText(R.string.before_can_join);
            }
            ViewAnimator.appearUp(findViewById(R.id.card_information), null);
        } else {
            ViewAnimator.disappearDown(findViewById(R.id.card_information), null);
        }
    }

    public void onShareButtonClicked(View view) {
        ViewAnimator.quickFade(
                view,
                new ViewAnimator.Callback() {
                    @Override
                    public void onAnimationEnd() {
                        SharingGuide.showLanguageDialog(PrepareSessionActivity.this);
                    }
                }
        );
    }

    public void informationOnClick(View view) {
        updateInformationView(false);
        PreferenceHelper.from(this).ackPasswordInformation();
    }
}
