package org.eztarget.realay.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.eztarget.realay.R;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.ui.utils.ImageLoader;
import org.eztarget.realay.ui.utils.MediaPickHelper;
import org.eztarget.realay.ui.utils.ViewAnimator;
import org.eztarget.realay.utils.ImageUploaderTask;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by michel on 28/01/15.
 *
 * Activity in which the local User can edit their profile data
 */
public class UserEditorActivity extends BaseActivity {

    private static final String TAG = UserEditorActivity.class.getSimpleName();

    private User mUser;

    private EditText mNameView;

    private EditText mStatusView;

    private EditText mMailView;

    private EditText mPhoneView;

    private EditText mWebsiteView;

    private EditText mIgView;

    private EditText mTwitterView;

    private boolean mDoPopulateViews = true;

    private CallbackManager mCallbackManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setLogo(R.drawable.ic_done_white_24dp);
        final View.OnTouchListener listener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int eventId = event.getAction();
                v.setSelected(eventId == MotionEvent.ACTION_DOWN);
                if (eventId == MotionEvent.ACTION_DOWN) saveData();
                return true;
            }
        };
        toolbar.setOnTouchListener(listener);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayShowTitleEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUser = LocalUserManager.getInstance().getUser(this);
        if (mUser == null) {
            onBackPressed();
            return;
        }

        ImageLoader imageLoader = new ImageLoader(this);
        imageLoader.handle(mUser, false);
        imageLoader.doCropUserCircle();
        imageLoader.startLoadingInto(
                (ImageView) findViewById(R.id.image_user_icon),
                false,
                null,
                R.drawable.ic_photo_camera_white_24dp
        );

        if (!mDoPopulateViews) return;
        mDoPopulateViews = false;

        if (mNameView == null) mNameView = (EditText) findViewById(R.id.edit_user_name);
        // The username is the only field that cannot have empty values. Listen to its changes.
        mNameView.setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (TextUtils.isEmpty(v.getText().toString().trim())) {
                            showEmptyNameToast();
                            v.setText(mUser.getName());
                        }
                        return false;
                    }
                }
        );
        mNameView.setText(mUser.getName());

        if (mStatusView == null) mStatusView = (EditText) findViewById(R.id.edit_user_status);
        mStatusView.setText(mUser.getStatusMessage());

        if (mMailView == null) mMailView = (EditText) findViewById(R.id.edit_user_email);
        mMailView.setText(mUser.getEmailAddress());

        if (mPhoneView == null) mPhoneView = (EditText) findViewById(R.id.edit_user_phone);
        mPhoneView.setText(mUser.getPhoneNumber());

        if (mWebsiteView == null) mWebsiteView = (EditText) findViewById(R.id.edit_user_website);
        mWebsiteView.setText(mUser.getWebsite());

        updateFacebookButton(!TextUtils.isEmpty(mUser.getFacebookId()));

        if (mIgView == null) mIgView = (EditText) findViewById(R.id.edit_user_ig);
        mIgView.setText(mUser.getIgName());

        if (mTwitterView == null) mTwitterView = (EditText) findViewById(R.id.edit_user_twitter);
        mTwitterView.setText(mUser.getTwitterName());
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideKeyboard();
    }

    private CallbackManager getCallbackManager() {
        if (mCallbackManager == null) {
            mCallbackManager = CallbackManager.Factory.create();
        }
        return mCallbackManager;
    }

    private void showEmptyNameToast() {
        Toast.makeText(this, R.string.name_not_empty, Toast.LENGTH_SHORT).show();
    }

    private void saveData() {
        // The save button was pressed, check for changes.
        if (!didPerformChange()) {
            onBackPressed();
            return;
        }

        mUser.setName(mNameView.getText().toString().trim());
        mUser.setStatusMessage(mStatusView.getText().toString().trim());
        mUser.setMailAddress(mMailView.getText().toString().trim());
        mUser.setPhoneNumber(mPhoneView.getText().toString().trim());
        mUser.setWebsite(mWebsiteView.getText().toString().trim());

        // Remove preluding @ tags from Instagram and Twitter usernames.

        String instagramName = mIgView.getText().toString().trim();
        int igNameStart;
        for (igNameStart = 0; igNameStart < instagramName.length(); igNameStart++) {
            if (instagramName.charAt(igNameStart) != '@') break;
        }
        mUser.setIgHandle(instagramName.substring(igNameStart));

        String twitterName = mTwitterView.getText().toString().trim();
        int twitterNameStart;
        for (twitterNameStart = 0; twitterNameStart < twitterName.length(); twitterNameStart++) {
            if (twitterName.charAt(twitterNameStart) != '@') break;
        }
        mUser.setTwitterName(twitterName.substring(twitterNameStart));

        LocalUserManager.getInstance().updateStorage(this);

        onBackPressed();
    }

    /**
     * Goes through the Views and check for differences to the User object
     *
     * @return True if any field was changed
     */
    private boolean didPerformChange() {
        if (mUser == null) return false;

        final String newName = mNameView.getText().toString().trim();
        if (!mUser.getName().equals(newName)) return true;

        final String newStatus = mStatusView.getText().toString().trim();
        if (!mUser.getStatusMessage().equals(newStatus)) return true;

        final String newMail = mMailView.getText().toString().trim();
        if (!mUser.getEmailAddress().equals(newMail)) return true;

        final String newPhone = mPhoneView.getText().toString().trim();
        if (!mUser.getPhoneNumber().equals(newPhone)) return true;

        final String newWebsite = mWebsiteView.getText().toString().trim();
        if (!mUser.getWebsite().equals(newWebsite)) return true;

        final String newIgHandle = mIgView.getText().toString().trim();
        if (!mUser.getIgName().equals(newIgHandle)) return true;

        final String newTwitterName = mTwitterView.getText().toString().trim();
        return !mUser.getTwitterName().equals(newTwitterName);
    }

    /*
    IMAGE EDITING
     */

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
    public void imagePickerOnClick(View view) {
        ViewAnimator.quickFade(view);
        toggleMediaSelector();

        final Intent imageIntent;
        switch (view.getId()) {
            case R.id.view_media_picker_gallery:
                imageIntent = MediaPickHelper.getPhotoIntent(this, true);
                break;

            case R.id.view_media_picker_camera:
                imageIntent = MediaPickHelper.getPhotoIntent(this, false);
                break;
            default:
                imageIntent = null;
        }

        if (imageIntent != null) {
            try {
                startActivityForResult(imageIntent, REQUEST_PICK);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.error_open_file_type, Toast.LENGTH_LONG).show();
            }
        }
    }

    private static final long SCALE_ANIMATION_TIME = 300L;

    private void toggleMediaSelector() {
        final View galleryRow = findViewById(R.id.view_media_picker_gallery);
        final View photoRow = findViewById(R.id.view_media_picker_camera);
        final boolean isShowingMediaPicker = galleryRow.getVisibility() == View.VISIBLE;

        // If the Visibility of the two rows does not match,
        // assume that they are in a transitional stage and do not show any additional animations.
        if (isShowingMediaPicker != (photoRow.getVisibility() == View.VISIBLE)) return;

        final View card = findViewById(R.id.card_media_menu);
        card.setVisibility(View.VISIBLE);

        if (isShowingMediaPicker) {
            // The rows are shown. Hide the photo row first and the gallery row in the callback,
            // thus hiding them after each other from bottom to top.
            ViewAnimator.scaleAnimateView(
                    photoRow,
                    false,
                    true,
                    SCALE_ANIMATION_TIME,
                    0f,
                    0.5f,
                    new ViewAnimator.Callback() {
                        @Override
                        public void onAnimationEnd() {
                            ViewAnimator.scaleAnimateView(
                                    galleryRow,
                                    false,
                                    true,
                                    SCALE_ANIMATION_TIME,
                                    0f,
                                    0.5f,
                                    new ViewAnimator.Callback() {
                                        @Override
                                        public void onAnimationEnd() {
                                            card.setVisibility(View.GONE);
                                        }
                                    }
                            );
                        }
                    }
            );
        } else {
            // The rows are not shown. Create a drop-down effect by scaling them after each other
            // from top to bottom.
            ViewAnimator.scaleAnimateView(
                    galleryRow,
                    true,
                    true,
                    SCALE_ANIMATION_TIME,
                    0f,
                    0.5f,
                    new ViewAnimator.Callback() {
                        @Override
                        public void onAnimationEnd() {
                            ViewAnimator.scaleAnimateView(
                                    photoRow,
                                    true,
                                    true,
                                    SCALE_ANIMATION_TIME,
                                    0f,
                                    0.5f,
                                    null
                            );
                        }
                    }

            );
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK) {
            if (resultCode == Activity.RESULT_CANCELED) return;

            final Intent cropIntent = MediaPickHelper.buildCropIntent(this, data, null, true);
            try {
                startActivityForResult(cropIntent, REQUEST_CROP);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.error_open_file_type, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CROP) {
            if (resultCode == Activity.RESULT_CANCELED) return;

            final Bitmap pickedBitmap = MediaPickHelper.readTempFile(this);
            if (pickedBitmap == null) {
                Toast.makeText(this, R.string.error_open_file_type, Toast.LENGTH_SHORT).show();
                return;
            }

            // Start a task that uploads this image.
            ImageUploaderTask.userImageUploader(
                    this,
                    pickedBitmap,
                    (ImageView) findViewById(R.id.image_user_icon)
            ).execute();
        } else {
            getCallbackManager().onActivityResult(requestCode, resultCode, data);
        }

    }

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
                                        UserEditorActivity.this,
                                        true
                                );
                                break;

                            case R.id.view_media_picker_camera:
                                imageIntent = MediaPickHelper.getPhotoIntent(
                                        UserEditorActivity.this,
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
                                Toast.makeText(
                                        UserEditorActivity.this,
                                        R.string.error_open_file_type,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }
                    }
                }
        );

    }

    private static final String FB_GRAPH_KEY_ID = "id";

    public void onFacebookClick(View view) {

        final View button = view;

        ViewAnimator.scaleAnimateView(
                button,
                false,
                true,
                0.5f,
                0.5f,
                new ViewAnimator.Callback() {
                    @Override
                    public void onAnimationEnd() {
                        if (TextUtils.isEmpty(mUser.getFacebookId())) {
                            // The User does not have a Facebook ID set.
                            // Login to Facebook, get the ID, assign it to the User and sync.
                            requestFacebookId(button);
                        } else {
                            // A Facebook ID for this User has been set, remove it and sync.

                            updateFacebookButton(false);
                            setFacebookId(null);
                            ViewAnimator.scaleAnimateView(button, true);
                        }
                    }
                }
        );
    }

    private void requestFacebookId(final View button) {
        FacebookSdk.sdkInitialize(getApplicationContext());

        LoginManager.getInstance().registerCallback(
                getCallbackManager(),
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d(TAG, "Facebook Login succeeded.");
                        GraphRequest request = GraphRequest.newMeRequest(
                                loginResult.getAccessToken(),
                                new GraphRequest.GraphJSONObjectCallback() {
                                    @Override
                                    public void onCompleted(
                                            JSONObject object,
                                            GraphResponse response
                                    ) {

                                        LoginManager.getInstance().logOut();

                                        final Object idObject;
                                        try {
                                            idObject = object.get(FB_GRAPH_KEY_ID);
                                            if (idObject instanceof String) {
                                                setFacebookId((String) idObject);
                                                updateFacebookButton(true);
                                                ViewAnimator.scaleAnimateView(button, true);
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            ViewAnimator.scaleAnimateView(button, true);
                                        }

                                    }
                                }
                        );
                        final Bundle parameters = new Bundle();
                        parameters.putString("fields", FB_GRAPH_KEY_ID);
                        request.setParameters(parameters);
                        request.executeAsync();
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "Facebook Link cancelled.");
                        ViewAnimator.scaleAnimateView(button, true);
                    }

                    @Override
                    public void onError(FacebookException e) {
                        ViewAnimator.scaleAnimateView(button, true);

                        Log.e(TAG, e.toString());
                        Toast.makeText(
                                UserEditorActivity.this,
                                R.string.error_default,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        LoginManager.getInstance().logInWithReadPermissions(this, null);
    }

    private void updateFacebookButton(final boolean hasId) {
        final TextView facebookButton = (TextView) findViewById(R.id.text_facebook_button);
        facebookButton.setText(hasId ? R.string.unlink_facebook : R.string.add_link_facebook);
    }

    /**
     * Assigns the ID to the local User, stores the change and synchronises with the Server
     *
     * @param facebookId New Facebook ID grabbed from a Facebook login
     *                   or null to remove the ID from the profile
     */
    private void setFacebookId(final String facebookId) {
        mUser.setFacebookId(facebookId);
        LocalUserManager.getInstance().updateStorage(this);
    }
}
