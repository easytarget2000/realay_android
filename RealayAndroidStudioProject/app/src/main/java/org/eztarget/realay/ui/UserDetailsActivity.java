package org.eztarget.realay.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.eztarget.realay.Constants;
import org.eztarget.realay.R;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.UsersCache;
import org.eztarget.realay.ui.utils.ImageLoader;
import org.eztarget.realay.ui.utils.ViewAnimator;

public class UserDetailsActivity extends BaseActivity {

    private static final String TAG = UserDetailsActivity.class.getSimpleName();

    private static final int DETAIL_ROW_PHONE_NUMBER = 1000;

    private static final int DETAIL_ROW_EMAIL_ADDRESS = 1001;

    private static final int DETAIL_ROW_WEBSITE = 1002;

    private boolean mDoDisplayLocalUser = false;

    private User mUser;
    
    private Class<?> mPrevActivityClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setUpBackButton(toolbar);

        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            Log.e(TAG, "This Activity requires an Extras Bundle.");
            onBackPressed();
            return;
        }

        mDoDisplayLocalUser = extras.getBoolean(UIConstants.EXTRA_DO_SHOW_MINE, false);
        FloatingActionButton actionButton = (FloatingActionButton) findViewById(R.id.button_action);
        if (mDoDisplayLocalUser) {
            mUser = LocalUserManager.getInstance().getUser(this);
        } else {
            final long userId = extras.getLong(Constants.EXTRA_USER_ID, -100L);
            mUser = UsersCache.getInstance().fetch(userId);
            actionButton.setImageResource(R.drawable.ic_person_add_white_36dp);
        }

        final String prevClassString = extras.getString(UIConstants.KEY_PREV_ACTIVITY);
        if (!TextUtils.isEmpty(prevClassString)) {
            Log.v(TAG, "Previous Activity Class: " + prevClassString);
            try {
                mPrevActivityClass = Class.forName(prevClassString);
            } catch (ClassNotFoundException e) {
                Log.w(TAG, e.toString());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mUser == null) {
            Log.e(TAG, "User object is null.");
            return;
        }

        final ImageView headerImageView = (ImageView) findViewById(R.id.image_user_full);
        final ImageLoader imageLoader = new ImageLoader(this);
        imageLoader.handle(mUser, true);
        imageLoader.startLoadingInto(
                headerImageView,
                true,
                null,
                R.drawable.ic_mood_white_48dp
        );

        // User name:
        CollapsingToolbarLayout collapsingToolbar;
        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(mUser.getName());

        // User status:
        ((TextView) findViewById(R.id.text_user_status)).setText(mUser.getStatusMessage());

        // Details Card:
        final LinearLayout detailsList = (LinearLayout) findViewById(R.id.list_user_details);
        detailsList.removeAllViews();

        View lastDetailDivider = null;
        if (!TextUtils.isEmpty(mUser.getPhoneNumber())) {
            final View phoneRow = getDetailRow(DETAIL_ROW_PHONE_NUMBER);
            if (phoneRow != null) {
                detailsList.addView(phoneRow);
                lastDetailDivider = phoneRow.findViewById(R.id.view_detail_divider);
            }
        }
        if (!TextUtils.isEmpty(mUser.getEmailAddress())) {
            final View emailRow = getDetailRow(DETAIL_ROW_EMAIL_ADDRESS);
            if (emailRow != null) {
                detailsList.addView(emailRow);
                lastDetailDivider = emailRow.findViewById(R.id.view_detail_divider);
            }
        }
        if (!TextUtils.isEmpty(mUser.getWebsite())) {
            final View websiteRow = getDetailRow(DETAIL_ROW_WEBSITE);
            if (websiteRow != null) {
                detailsList.addView(websiteRow);
                lastDetailDivider = websiteRow.findViewById(R.id.view_detail_divider);
            }
        }

        if (lastDetailDivider != null) {
            findViewById(R.id.card_user_details).setVisibility(View.VISIBLE);
            lastDetailDivider.setVisibility(View.GONE);
        }

        // Facebook Card:
        final String facebookId = mUser.getFacebookId();
        if (!TextUtils.isEmpty(facebookId)) {
            findViewById(R.id.card_facebook).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.card_facebook).setVisibility(View.GONE);
        }

        // Instagram Card:
        String instagramId = mUser.getIgName();
        if (!TextUtils.isEmpty(instagramId)) {
            if (instagramId.charAt(0) != '@') instagramId = '@' + instagramId;

            final TextView instagramIdView = (TextView) findViewById(R.id.text_instagram);
            instagramIdView.setText(instagramId);
            findViewById(R.id.card_instagram).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.card_instagram).setVisibility(View.GONE);
        }

        // Twitter Card:
        String twitterId = mUser.getTwitterName();
        if (!TextUtils.isEmpty(mUser.getTwitterName())) {
            if (twitterId.charAt(0) != '@') twitterId = '@' + twitterId;

            final TextView twitterIdView = (TextView) findViewById(R.id.text_twitter);
            twitterIdView.setText(twitterId);
            findViewById(R.id.card_twitter).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.card_twitter).setVisibility(View.GONE);
        }

    }

    private View getDetailRow(final int id) {
        final LinearLayout detailsList = (LinearLayout) findViewById(R.id.list_user_details);
        final View row = getLayoutInflater().inflate(R.layout.item_detail_row, detailsList, false);
        
        final ImageView icon = (ImageView) row.findViewById(R.id.image_detail_icon);
        final TextView singleText = (TextView) row.findViewById(R.id.text_detail_single);

        switch (id) {
            case DETAIL_ROW_PHONE_NUMBER:
                icon.setImageResource(R.drawable.ic_phone_black_24dp);
                final String phoneNumber = mUser.getPhoneNumber();
                singleText.setText(phoneNumber);
                singleText.setAutoLinkMask(Linkify.PHONE_NUMBERS);

                return row;

            case DETAIL_ROW_EMAIL_ADDRESS:
                icon.setImageResource(R.drawable.ic_mail_black_24dp);
                final String emailAddress = mUser.getEmailAddress();
                singleText.setText(emailAddress);
                singleText.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);

                return row;

            case DETAIL_ROW_WEBSITE:
                icon.setImageResource(R.drawable.ic_web_black_24dp);
                singleText.setText(mUser.getWebsite());
                singleText.setAutoLinkMask(Linkify.WEB_URLS);
                return row;

            default:
                return null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mPrevActivityClass != null) {
            // Clear out the activity history stack till now.
            Intent goToReturnActivity = new Intent(this, mPrevActivityClass);
            goToReturnActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(goToReturnActivity);
        }
    }

        /*
    ActionBar Menu
     */

    public void fbOnClick(View view) {
        ViewAnimator.quickFade(
                view,
                new ViewAnimator.Callback() {
                    @Override
                    public void onAnimationEnd() {
                        if (mUser == null) return;
                        final String fbId = mUser.getFacebookId();
                        if (TextUtils.isEmpty(fbId)) return;

                        final Uri appUri = Uri.parse("fb://profile?app_scoped_user_id=" + fbId);
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, appUri));
                        } catch (ActivityNotFoundException e) {
                            final Uri webUri = Uri.parse("https://facebook.com/" + fbId);
                            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                        }
                    }
                }
        );
    }

    public void igOnClick(View view) {
        ViewAnimator.quickFade(
                view,
                new ViewAnimator.Callback() {
                    @Override
                    public void onAnimationEnd() {
                        if (mUser == null) return;
                        final String igHandle = mUser.getIgName();
                        if (TextUtils.isEmpty(igHandle)) return;

                        Uri uri = Uri.parse("http://instagram.com/_u/" + igHandle);
                        Intent igIntent = new Intent(Intent.ACTION_VIEW, uri);
                        igIntent.setPackage("com.instagram.android");

                        try {
                            startActivity(igIntent);
                        } catch (ActivityNotFoundException e) {
                            final Uri webUri = Uri.parse("http://instagram.com/" + igHandle);
                            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                        }
                    }
                }
        );
    }

    public void twitterOnClick(View view) {
        ViewAnimator.quickFade(
                view,
                new ViewAnimator.Callback() {
                    @Override
                    public void onAnimationEnd() {
                        if (mUser == null) return;
                        final String twitterName = mUser.getTwitterName();
                        if (TextUtils.isEmpty(twitterName)) return;

                        final Uri appUri = Uri.parse("twitter://user?screen_name=" + twitterName);
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, appUri));
                        } catch (Exception e) {
                            final Uri webUri = Uri.parse("https://twitter.com/#!/" + twitterName);
                            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                        }
                    }
                }
        );
    }

    public void onActionButtonClicked(View view) {
        if (mDoDisplayLocalUser) {
            startActivity(new Intent(this, UserEditorActivity.class));
        } else {
            if (mUser == null) return;
            // Create a new Intent to insert a contact
            // and sets the MIME type to match the Contacts Provider.
            Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION);
            contactIntent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

            contactIntent.putExtra(ContactsContract.Intents.Insert.NAME, mUser.getName());

            // Insert available data.
            final String email = mUser.getEmailAddress();
            if (!TextUtils.isEmpty(email)) {
                contactIntent.putExtra(ContactsContract.Intents.Insert.EMAIL, email);
                contactIntent.putExtra(
                        ContactsContract.Intents.Insert.EMAIL_TYPE,
                        ContactsContract.CommonDataKinds.Email.TYPE_HOME
                );
            }
            final String phone = mUser.getPhoneNumber();
            if (!TextUtils.isEmpty(phone)) {
                contactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, phone);
                contactIntent.putExtra(
                        ContactsContract.Intents.Insert.PHONE_TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MAIN
                );
            }
            startActivity(contactIntent);
        }
    }

}
