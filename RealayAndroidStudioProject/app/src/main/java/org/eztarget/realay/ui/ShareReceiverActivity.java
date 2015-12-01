package org.eztarget.realay.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.eztarget.realay.R;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.ui.utils.MediaPickHelper;
import org.eztarget.realay.ui.utils.ViewAnimator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by michel on 29/08/15.
 *
 */
public class ShareReceiverActivity extends BaseActivity {

    private static final String TAG = ShareReceiverActivity.class.getSimpleName();

    private boolean mDoAnimate;

    private boolean mIsInSession;

    private Uri mShareUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_receiver);

        final SessionMainManager manager = SessionMainManager.getInstance();
        mIsInSession = manager.didLogin() && manager.getRoom(this, false) != null;
        if (!mIsInSession) {
            // Outside of Sessions, show a message
            // and a button that leads to the Room List. Don't handle the Share Intent.
            findViewById(R.id.button_secondary).setVisibility(View.GONE);

            ((TextView) findViewById(R.id.text_information)).setText(R.string.join_to_share);

            final TextView buttonText = (TextView) findViewById(R.id.text_primary_button);
            buttonText.setText(R.string.near_you);
        }

        final File tempFile = MediaPickHelper.getTempFile(
                this,
                MediaPickHelper.IMAGE_STATE_ORIGINAL
        );

        // Clear the temp file just to make sure that no old sharing attempts are in there.
        tempFile.delete();

        final Intent intent = getIntent();
        final String type = intent.getType();

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if (type != null && type.startsWith("image/")) {
                mShareUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                InputStream inputStream = null;
                try {
                    inputStream = getContentResolver().openInputStream(mShareUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (inputStream != null) {
                    MediaPickHelper.streamIntoFile(inputStream, tempFile);
                } else {
                    final String filePath;
                    filePath = MediaPickHelper.getPathFromUri(this, mShareUri);
                    if (filePath != null) {
                        MediaPickHelper.copyFile(new File(filePath), tempFile);
                    } else {
                        Log.w(TAG, "Did not find a file associated with Sharing URI.");
                        tempFile.delete();
                    }
                }

                if (tempFile.exists()) {
                    Bitmap preview;
                    try {
                        preview = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
                        ((ImageView) findViewById(R.id.image_background)).setImageBitmap(preview);
                    } catch (OutOfMemoryError e) {
                        Log.e(TAG, e.toString());
                    }
                }

                if (mIsInSession) {
                    // If a Session has been started, the sharing can begin.
                    // Move the file to be the temporary image expected by the Media Helper
                    // and let the Conversations know to start a Crop Intent next.

                    ConversationActivity.setSharingPrepared(Uri.parse("tempFile"));

                    // Everything went fine.
                    // Prepare the Views for the pop-up animations.

                    findViewById(R.id.card_main).setVisibility(View.INVISIBLE);
                    mDoAnimate = true;
                }
                return;
            } else {
                Log.e(TAG, "Unexpected Intent Type:" + intent.getType());
            }
        } else {
            Log.e(TAG, "Unexpected Intent: " + intent.toString());
        }

        tempFile.delete();
        ConversationActivity.setSharingPrepared(null);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDoAnimate) {

            new Handler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            final View primaryButton = findViewById(R.id.button_primary);
                            primaryButton.setVisibility(View.INVISIBLE);

                            final View secondaryButton = findViewById(R.id.button_secondary);
                            if (mIsInSession) secondaryButton.setVisibility(View.INVISIBLE);
                            else secondaryButton.setVisibility(View.GONE);

                            // Pop up the Card, then the primary button, then the secondary button.

                            final ViewAnimator.Callback primaryButtonCallback;
                            primaryButtonCallback = new ViewAnimator.Callback() {
                                @Override
                                public void onAnimationEnd() {
                                    mDoAnimate = false;
                                    if (mIsInSession) {
                                        ViewAnimator.appearUp(
                                                secondaryButton,
                                                null
                                        );
                                    }
                                }
                            };

                            final ViewAnimator.Callback cardCallback;
                            cardCallback = new ViewAnimator.Callback() {
                                @Override
                                public void onAnimationEnd() {
                                    ViewAnimator.appearUp(
                                            primaryButton,
                                            primaryButtonCallback
                                    );
                                }
                            };

                            ViewAnimator.appearUp(findViewById(R.id.card_main), cardCallback);
                        }
                    },
                    1400L
            );
        }
    }

    public void onPrimaryButtonClick(View view) {
        final Intent intent;
        if (mIsInSession) {
            ConversationActivity.setSharingPrepared(mShareUri);
            intent = new Intent(this, PublicConversationActivity.class);
        } else {
            intent = new Intent(this, RoomListActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public void onSecondaryButtonClick(View view) {
        if (mIsInSession) {
            ConversationActivity.setSharingPrepared(mShareUri);

            final Intent userList;
            userList = new Intent(this, UserTabActivity.class);
            userList.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(userList);
            finish();
        }
    }

}
