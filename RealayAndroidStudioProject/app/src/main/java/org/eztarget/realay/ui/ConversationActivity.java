package org.eztarget.realay.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eztarget.realay.Constants;
import org.eztarget.realay.R;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.SessionActionsManager;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.managers.UsersCache;
import org.eztarget.realay.services.ActionSendService;
import org.eztarget.realay.ui.adapter.MessageCursorAdapter;
import org.eztarget.realay.ui.utils.MediaPickHelper;
import org.eztarget.realay.ui.utils.ViewAnimator;
import org.eztarget.realay.utils.APIHelper;
import org.eztarget.realay.utils.ImageUploaderTask;

/**
 * Created by michel on 04/12/14.
 * <p/>
 * Base class for conversations, i.e. PublicConversationActivity & PrivateConversationActivity;
 * handles common Recycler, CursorLoader and message input methods
 */
public class ConversationActivity
        extends BaseActivity
        implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener, View.OnDragListener {

    /**
     * Tag used by all calls to Log methods.
     */
    private static final String TAG = ConversationActivity.class.getSimpleName();

    private static Uri sShareUri;

    protected static final long MESSAGES_LIMIT_STEP = 10;

    protected int mLowestId = -1;

    protected int mMessageLimit = 40;

    /**
     * If true, the RecyclerView is scrolled to the top, the next time the Loader refreshes;
     * used in order to differentiate between new messages appearing at the bottom
     * and asking for old messages to be shown at the top of the conversation
     */
    protected boolean mDoMoveToTop = false;

    /**
     * Displays all the messages in this conversation
     */
    protected RecyclerView mRecycler;

    /**
     * Maps the messages in this conversation to row layouts
     */
    protected MessageCursorAdapter mCursorAdapter;

    /**
     * Message input text field
     */
    protected EditText mMessageEditText;

    /**
     * Image that acts as a button to open the media input selector rows
     */
    private ImageView mMediaButton;


    /**
     * The ID of the chat partner in PrivateConversationActivities
     * or the ID of the Room in PublicConversationActivities;
     * For building and identification of the CursorLoader
     */
    protected long mConversationId = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new ConversationRelativeLayout(this, null));

        // Populate the adapter / list using a Cursor Loader.
        mCursorAdapter = new MessageCursorAdapter(this, null);

        final SwipeRefreshLayout swipeRefresher;
        swipeRefresher = (SwipeRefreshLayout) findViewById(R.id.swipe_messages);
        swipeRefresher.setOnRefreshListener(this);
        swipeRefresher.setEnabled(true);
        swipeRefresher.setColorSchemeResources(
                R.color.primary,
                R.color.secondary_text,
                R.color.accent,
                R.color.secondary_text
        );

        swipeRefresher.setEnabled(false);

        mRecycler = (RecyclerView) findViewById(R.id.recycler_conversation);
        mRecycler.setHasFixedSize(true);
        mRecycler.setAdapter(mCursorAdapter);
        final LinearLayoutManager linearLayMan;
        linearLayMan = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecycler.setLayoutManager(linearLayMan);
        mRecycler.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        // Only handle scrolling further
                        // if there is at least one child element in the list.
                        if (recyclerView.getChildCount() == 0) {
                            swipeRefresher.setEnabled(true);
                            return;
                        }

                        // Enable the SwipeRefreshLayout only
                        // if the top of the Recycler has been reached.
                        final boolean didReachTop = recyclerView.getChildAt(0).getTop() >= 0;
                        swipeRefresher.setEnabled(didReachTop);
                    }
                }
        );

        // Set up the message EditText.
        mMessageEditText = (EditText) findViewById(R.id.edit_text_conversation_input);

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Tell the Notification Manager which Conversation is in the foreground
        // to hide the appropriate notifications.
        final SessionActionsManager actionMan = SessionActionsManager.from(this);
        actionMan.setForegroundConversation(mConversationId);

        // Restore what has been typed in this Conversation earlier.
        mMessageEditText.setText(actionMan.getEditTextInput(mConversationId));

        if (sShareUri != null) {

            if (verifyPartnerStatus()) {
                // The Intent contains an image URI and the Partner is available.
                // Crop and send the image.

                final Intent cropIntent;
                cropIntent = MediaPickHelper.buildCropIntent(this, null, sShareUri, false);
                sShareUri = null;
                try {
                    startActivityForResult(cropIntent, REQUEST_CROP);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, e.toString());
                    showUploadError();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();

        final SessionActionsManager actionMan = SessionActionsManager.from(this);
        actionMan.setForegroundConversation(0L);

        // Store whatever has been typed so far.
        actionMan.setEditTextInput(mConversationId, mMessageEditText.getText().toString().trim());
    }

    protected void setupInputViews() {
        mMessageEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_UNSPECIFIED:
                    case EditorInfo.IME_ACTION_DONE:
                        onSendButtonClicked(findViewById(R.id.image_send_message));
                        return true;
                    default:
                        return true;
                }
            }

        });

        if (mMediaButton == null) mMediaButton = (ImageView) findViewById(R.id.image_send_media);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.image_send_media:
                if (verifyPartnerStatus()) imageSelectorOnClick(view);
        }
    }


    private static final int REQUEST_PICK = 647;

    private static final int REQUEST_CROP = 847;

    /**
     * Called by onClick on the icon ImageView or one of the
     * as defined in the layout XML;
     * Animates the View, toggles the media selection rows
     * and starts the appropriate Intent, if one of the View is one of the rows
     *
     * @param view Icon ImageView or one of the image action rows
     */
    public void imageSelectorOnClick(View view) {
        final View button = view;
        ViewAnimator.quickFade(
                button,
                new ViewAnimator.Callback() {
                    @Override
                    public void onAnimationEnd() {
                        toggleMediaSelector();

                        final Intent imageIntent;
                        switch (button.getId()) {
                            case R.id.view_media_picker_gallery:
                                imageIntent = MediaPickHelper.getPhotoIntent(
                                        ConversationActivity.this,
                                        true
                                );
                                break;

                            case R.id.view_media_picker_camera:
                                imageIntent = MediaPickHelper.getPhotoIntent(
                                        ConversationActivity.this,
                                        false
                                );
                                break;

                            default:
                                imageIntent = null;
                        }

                        if (imageIntent != null) {
                            try {
                                startActivityForResult(imageIntent, REQUEST_PICK);
                            } catch (ActivityNotFoundException e) {
                                Log.e(TAG, e.toString());
                                showUploadError();
                            }
                        }
                    }
                }
        );
    }

    /**
     * Static setter;
     * Will be read by next Conversation that calls onResume()
     *
     * @param uri URI of the file that is to be shared;
     *            null if no file is to be shared anymore
     */
    public static void setSharingPrepared(final Uri uri) {
        sShareUri = uri;
    }

    private static final long ANIMATION_DURATION = 300L;

    protected boolean isShowingMediaSelector() {
        return findViewById(R.id.view_media_picker_camera).getVisibility() == View.VISIBLE;
    }

    protected void toggleMediaSelector() {
        final View galleryRow = findViewById(R.id.view_media_picker_gallery);
        final boolean isShowingMediaSelector = isShowingMediaSelector();

        // If the Visibility of the two rows does not match,
        // assume that they are in a transitional stage and do not show any additional animations.
        if (isShowingMediaSelector != (galleryRow.getVisibility() == View.VISIBLE)) return;

        if (mMediaButton == null) mMediaButton = (ImageView) findViewById(R.id.image_send_media);

        if (isShowingMediaSelector) {
            // The rows are shown.
            ViewAnimator.fadeView(findViewById(R.id.cover), false);

            // Rotate the arrow to point to the right and replace it with the media icon.
            RotateAnimation rotateAnimation = new RotateAnimation(
                    0f,
                    90f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
            );
            rotateAnimation.setDuration(ANIMATION_DURATION);
            rotateAnimation.setAnimationListener(
                    new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            mMediaButton.setImageResource(R.drawable.ic_attach_file_white_24dp);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    }
            );
            mMediaButton.startAnimation(rotateAnimation);
            // Hide the gallery row first and the photo row in the callback,
            // thus hiding them after each other from bottom to top.
            ViewAnimator.scaleAnimateView(
                    galleryRow,
                    false,
                    true,
                    ANIMATION_DURATION,
                    0f,
                    0.5f,
                    new ViewAnimator.Callback() {
                        @Override
                        public void onAnimationEnd() {
                            ViewAnimator.scaleAnimateView(
                                    findViewById(R.id.view_media_picker_camera),
                                    false,
                                    true,
                                    ANIMATION_DURATION,
                                    0f,
                                    0.5f,
                                    null
                            );
                        }
                    }
            );
        } else {
            // The rows are not shown. Only open the menu if the Partner is still available.
            if (!verifyPartnerStatus()) return;

            ViewAnimator.fadeView(findViewById(R.id.cover), true);
            hideKeyboard();
            // Replace the media icon with an arrow and rotate it to point upwards.
            RotateAnimation rotateAnimation = new RotateAnimation(
                    90f,
                    0f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f
            );
            rotateAnimation.setDuration(ANIMATION_DURATION);
            mMediaButton.setImageResource(R.drawable.ic_keyboard_arrow_up_white_24dp);
            mMediaButton.startAnimation(rotateAnimation);
            // Create a drop-down effect by scaling them after each other
            // from bottom to top.
            ViewAnimator.scaleAnimateView(
                    findViewById(R.id.view_media_picker_camera),
                    true,
                    true,
                    ANIMATION_DURATION,
                    0f,
                    0.5f,
                    new ViewAnimator.Callback() {
                        @Override
                        public void onAnimationEnd() {
                            ViewAnimator.scaleAnimateView(
                                    galleryRow,
                                    true,
                                    true,
                                    ANIMATION_DURATION,
                                    0f,
                                    0.5f,
                                    null
                            );
                            mRecycler.scrollToPosition(mCursorAdapter.getItemCount() - 1);
                        }
                    }

            );
        }
    }

    /**
     * Called when returning from the media picker, such as the photo gallery or camera
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) return;

        if (requestCode == REQUEST_PICK) {
            final Intent cropIntent = MediaPickHelper.buildCropIntent(this, data, null, false);
            try {
                startActivityForResult(cropIntent, REQUEST_CROP);
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, e.toString());
                showUploadError();
            }
        } else if (requestCode == REQUEST_CROP) {
            findViewById(R.id.view_media_picker_camera).setVisibility(View.GONE);

            final Bitmap pickedBitmap = MediaPickHelper.readTempFile(this);
            if (pickedBitmap == null) {
                Log.e(TAG, "No Bitmap retrieved from Crop request.");
                showUploadError();
                return;
            }

            final Action mediaMessage;
            mediaMessage = Action.buildImageUploadAction(this, mConversationId, pickedBitmap);

            ImageUploaderTask uploader;
            uploader = ImageUploaderTask.imageMessageUploader(this, pickedBitmap, mediaMessage);
            if (uploader != null) uploader.execute();
            else showUploadError();
        }
    }

    private void showUploadError() {
        Toast.makeText(this, R.string.error_open_file_type, Toast.LENGTH_LONG).show();
    }

    /*
    LoaderManager Base Implementation
    */

    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (mCursorAdapter == null) return;
        mCursorAdapter.changeCursor(cursor);

        if (mRecycler.getAdapter() == null) mRecycler.setAdapter(mCursorAdapter);

        if (mDoMoveToTop) mRecycler.scrollToPosition(0);
        else mRecycler.scrollToPosition(mCursorAdapter.getItemCount() - 1);

        final SwipeRefreshLayout swipeRefresher;
        swipeRefresher = (SwipeRefreshLayout) findViewById(R.id.swipe_messages);
        swipeRefresher.setRefreshing(false);
        swipeRefresher.setEnabled(cursor.getCount() > 0);

        mDoMoveToTop = false;

        ViewAnimator.fadeView(
                findViewById(R.id.text_refresh_information),
                false
        );

        if (findViewById(R.id.text_input_blocker).getVisibility() == View.VISIBLE) {
            verifyPartnerStatus();
        }
    }

    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursorAdapter.swapCursor(null);
        ((SwipeRefreshLayout) findViewById(R.id.swipe_messages)).setRefreshing(false);
        mDoMoveToTop = false;

        ViewAnimator.fadeView(findViewById(R.id.text_refresh_information), false);
    }

    /*
     User Input
     */

    public void showBlockingDialog(final User smug) {
        if (smug == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String titleFormat = getResources().getString(R.string.do_you_want_to_block);
        builder.setTitle(String.format(titleFormat, smug.getName()));
        builder.setMessage(R.string.the_person_will);

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        showReportDialog(smug);
                }
            }
        };

        builder.setNegativeButton(android.R.string.cancel, listener);
        builder.setPositiveButton(R.string.block, listener);
        builder.setCancelable(true);
        builder.show();
    }

    public void showReportDialog(final User smug) {
        if (smug == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String titleFormat = getResources().getString(R.string.do_you_want_to_block);
        builder.setTitle(String.format(titleFormat, smug.getName()));
        builder.setMessage(R.string.send_report);

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_NEUTRAL:
                        final Intent actionSender = new Intent(
                                ConversationActivity.this,
                                ActionSendService.class
                        );
                        final Action report;
                        report = Action.buildReportAction(ConversationActivity.this, smug.getId());
                        actionSender.putExtra(Constants.EXTRA_ACTION, report);

                        startService(actionSender);

                    case DialogInterface.BUTTON_POSITIVE:
                        UsersCache.getInstance().blockUser(getApplicationContext(), smug);
                        if (mConversationId == Action.PUBLIC_RECIPIENT_ID) {
                            restartLoaders();
                        } else {
                            onBackPressed();
                        }
                }
            }
        };

        builder.setNeutralButton(R.string.block_report, listener);
        builder.setPositiveButton(R.string.block, listener);
        builder.setCancelable(true);
        builder.show();
    }

    public void showKickDialog(final User smug, final boolean doBan) {
        if (smug == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final String titleFormat;
        if (doBan) {
            titleFormat = getString(R.string.do_you_want_to_ban);
        } else {
            titleFormat = getString(R.string.do_you_want_to_kick);
        }
        builder.setTitle(String.format(titleFormat, smug.getName()));

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        APIHelper.kickUser(ConversationActivity.this, smug.getId(), doBan);
                }
            }
        };

        builder.setNegativeButton(android.R.string.no, null);
        builder.setPositiveButton(android.R.string.yes, listener);
        builder.setCancelable(true);
        builder.show();
    }

    /**
     * Verifies if the conversation partner is available;
     * checks if the User is still in the Room;
     * checks if the User has been banned;
     * shows the cover overlay View and a Toast if the User is not available
     *
     * @return True, if the partner User of this conversation is available to chat
     */
    protected boolean verifyPartnerStatus() {
        if (mConversationId == Action.PUBLIC_RECIPIENT_ID) {
            doShowInputBlocker(null);
            return true;
        }

        if (mConversationId < 10L) return false;

        final User partner = UsersCache.getInstance().fetch(mConversationId);

        if (UsersCache.getInstance().didBlockUser(mConversationId)) {
            final String inputBlockerMessage = String.format(
                    getResources().getString(R.string.has_been_blocked),
                    partner == null ? "?" : partner.getName()
            );

            doShowInputBlocker(inputBlockerMessage);
            return false;
        }

        SessionMainManager manager = SessionMainManager.getInstance();
        if (manager.containsUser(mConversationId)) {
            doShowInputBlocker(null);
            return true;
        }

        // The User that this conversation is with, is not in the Room (anymore)
        final String inputBlockerMessage = String.format(
                getResources().getString(R.string.has_left),
                partner == null ? "?" : partner.getName(),
                manager.getRoom(this, false).getTitle()
        );
        doShowInputBlocker(inputBlockerMessage);
        return false;
    }

    private void doShowInputBlocker(final String message) {
        final TextView inputBlocker = (TextView) findViewById(R.id.text_input_blocker);
        final boolean doShow = (message != null);
        if (doShow) inputBlocker.setText(message);
        ViewAnimator.fadeView(inputBlocker, doShow);
        inputBlocker.setClickable(doShow);
    }


    /**
     * For implementation by child classes that know their appropriate Loaders.
     */
    public void restartLoaders() {
    }

    @Override
    public void onRefresh() {
        ViewAnimator.fadeView(
                findViewById(R.id.text_refresh_information),
                true
        );

        // Increase the message limit and reset the lowest ID value.
        mMessageLimit += MESSAGES_LIMIT_STEP;
        mLowestId = -1;
        // Restarting the loaders will now fetch a new lowest ID
        // and display the old messages at the top.
        mDoMoveToTop = true;
        restartLoaders();
    }

    public void coverOnClick(View view) {
        verifyPartnerStatus();
        if (isShowingMediaSelector()) toggleMediaSelector();
    }

    public void onSendButtonClicked(View view) {
        // If the media selection Views are currently shown, touching anywhere else,
        // toggles them to close.
        if (isShowingMediaSelector()) toggleMediaSelector();


        if (!SessionMainManager.getInstance().didLogin() || !verifyPartnerStatus()) {
            ViewAnimator.wiggleIt(view);
            hideKeyboard();
            return;
        }

        final String sendText = mMessageEditText.getText().toString().trim();
        mMessageEditText.setText("");

        if (!TextUtils.isEmpty(sendText)) {
            ViewAnimator.disappearRight(
                    view,
                    true,
                    new ViewAnimator.Callback() {
                        @Override
                        public void onAnimationEnd() {
                            hideKeyboard();
                        }
                    }
            );

            final int actionCode;
            if (mConversationId == Action.PUBLIC_RECIPIENT_ID) {
                if (!SessionActionsManager.from(this).updateSpamCache(sendText)) return;
                actionCode = Action.ACTION_CODE_MSG_PUB;
            } else if (mConversationId > 10L) {
                actionCode = Action.ACTION_CODE_MSG_PRV;
            } else {
                Log.e(TAG, "Tried sending message in Conversation " + mConversationId + ".");
                return;
            }

            final long roomId = SessionMainManager.getInstance().getRoomId(this);
            final long senderId = LocalUserManager.getInstance().getUserId(this);

            final Action message = new Action(
                    roomId,
                    senderId,
                    mConversationId,
                    System.currentTimeMillis() / 1000L,
                    actionCode,
                    sendText
            );

            final Intent actionSender;
            actionSender = new Intent(ConversationActivity.this, ActionSendService.class);
            actionSender.putExtra(Constants.EXTRA_ACTION, message);
            startService(actionSender);
        } else {
            ViewAnimator.wiggleIt(view);
        }

    }

    @Override
    public boolean onDrag(View v, DragEvent event) {
        return false;
    }


    /**
     * Loads the Layout associated with Conversation Activities
     * and monitors the measurement to handle the appearance of Softkeys;
     * To be used with setContentView() in the Activity's onCreate().
     */
    private class ConversationRelativeLayout extends RelativeLayout {

        public ConversationRelativeLayout(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.activity_conversation, this);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int proposedHeight = MeasureSpec.getSize(heightMeasureSpec);
            final int actualHeight = getHeight();

            if (actualHeight > proposedHeight) {
                if (mRecycler != null && mCursorAdapter != null) {
                    final int messagesCount = mCursorAdapter.getItemCount();
                    if (messagesCount > 1) {
                        mRecycler.smoothScrollToPosition(messagesCount - 1);
                    }
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
