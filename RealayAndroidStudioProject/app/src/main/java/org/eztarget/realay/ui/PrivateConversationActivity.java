package org.eztarget.realay.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.eztarget.realay.Constants;
import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.User;
import org.eztarget.realay.managers.UsersCache;

/**
 * Created by michel on 17/12/14.
 */
public class PrivateConversationActivity
        extends ConversationActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Tag used by all calls to Log methods.
     */
    private static final String LOG_TAG = PrivateConversationActivity.class.getSimpleName();

    private User mPartner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int privateBackgroundColor = getResources().getColor(R.color.background);
        findViewById(R.id.recycler_conversation).setBackgroundColor(privateBackgroundColor);

        mCursorAdapter.isPublicConversation(false);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Extract the partner User ID from the Extras.
            mConversationId = extras.getLong(Constants.EXTRA_USER_ID);

            // Use the partner's name as the Activity title.
            mPartner = UsersCache.getInstance().fetch(mConversationId);
            if (mPartner == null) {
                Log.e(LOG_TAG, "Could not fetch User object.");
                onBackPressed();
                return;
            }

            setTitle(mPartner.getName());
            verifyPartnerStatus();
        } else {
            Log.e(LOG_TAG, "Requires extras Bundle.");
            onBackPressed();
            return;
        }

        setupInputViews();
        getSupportLoaderManager().initLoader((int) mConversationId, null, this);

        // Toolbar setup:
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(mPartner.getName());
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startProfileActivity(mConversationId);
            }
        });

        setSupportActionBar(toolbar);
        setUpBackButton(toolbar);
        showActionOverflowButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyPartnerStatus();
    }

    @Override
    public void onBackPressed() {
        // If the media selection Views are currently shown, touching anywhere else,
        // toggles them to close.
        if (isShowingMediaSelector()) toggleMediaSelector();
        else super.onBackPressed();
    }

    /*
    OPTIONS MENU
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_private_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_partner_profile:
                startProfileActivity(mConversationId);
                return true;
            case R.id.item_block_user:
                showBlockingDialog(mPartner);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    CURSOR LOADER
     */

    @Override
    public void restartLoaders() {
        super.restartLoaders();
        getSupportLoaderManager().restartLoader((int) mConversationId, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle args) {
        if (i != (int) mConversationId) return null;

        if (mLowestId < 1) {
            // Calculate the lowest ID we want to show in this conversation,
            // in order to limit the amount of messages,
            // i.e. highest ID - 50 = lowest ID --> only show last 50 messages
            Cursor lowestIdCursor = getContentResolver().query(
                    ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE,
                    ChatObjectContract.PROJECTION_MSGS_PRV_USER,
                    ChatObjectContract.SELECTION_PRIVATE_MSGS,
                    buildSelectionArgs(),
                    BaseColumns._ID + " DESC LIMIT " + mMessageLimit
            );
//            final int idColumn = lowestIdCursor.getColumnIndex(BaseColumns._ID);
            if (lowestIdCursor.moveToLast()) {
                mLowestId = lowestIdCursor.getInt(0);
            }
            lowestIdCursor.close();
        }

        if (mLowestId < 0) mLowestId = 0;

        return new CursorLoader(
                this,
                ChatObjectContract.CONTENT_URI_ACTIONS_PRIVATE,
                ChatObjectContract.PROJECTION_MSGS_PRV_USER,
//                ChatObjectContract.buildPrivateMessagesSelection(mLowestId, mConversationId),
//                null,
                ChatObjectContract.SELECTION_PRIVATE_MSGS,
                buildSelectionArgs(),
                BaseColumns.MSG_TIME + " ASC"
        );
    }

    private String[] buildSelectionArgs() {
        final String partnerId = String.valueOf(mConversationId);
        return new String[]{
                String.valueOf(mLowestId),
                partnerId,
                partnerId
        };
    }

}
