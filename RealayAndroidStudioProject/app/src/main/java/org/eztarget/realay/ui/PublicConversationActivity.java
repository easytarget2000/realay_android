package org.eztarget.realay.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.eztarget.realay.Constants;
import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.Action;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.Bouncer;
import org.eztarget.realay.managers.SessionActionsManager;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.ui.utils.ViewAnimator;

/**
 * Created by michel on 06/12/14.
 * <p/>
 * Subclass of ConversationActivity;
 * Different GUI elements and CursorLoader than used in Private Conversations;
 * Navigating back (onBackPressed()) from this Activity, leaves the Session.
 */
public class PublicConversationActivity
        extends ConversationActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Tag used by all calls to Log methods.
     */
    private static final String TAG = PublicConversationActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Room room = SessionMainManager.getInstance().getRoom(this, false);
        if (room == null) {
            Log.e(TAG, "Room is null.");
            Bouncer.from(this).kick(Bouncer.REASON_DATA_OFF, TAG);
            return;
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(room.getTitle());
        toolbar.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewAnimator.quickFade(
                                v,
                                new ViewAnimator.Callback() {
                                    @Override
                                    public void onAnimationEnd() {
                                        startRoomDetailsActivity();
                                    }
                                }
                        );
                    }
                }
        );
        setSupportActionBar(toolbar);

        mConversationId = Action.PUBLIC_RECIPIENT_ID;

        setupInputViews();
        getSupportLoaderManager().initLoader((int) mConversationId, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the notification counter.
        mNotificationCount = SessionActionsManager.from(this).getUnreadPrivateCount();
        supportInvalidateOptionsMenu();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_UNREAD_COUNT_CHANGED);
        registerReceiver(mUnreadReceiver, filter);

        SessionActionsManager.from(this).cancelPubMsgNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUnreadReceiver);
    }

    @Override
    public void onBackPressed() {
        // If the media selection Views are currently shown, touching anywhere else,
        // toggles them to close.
        if (isShowingMediaSelector()) toggleMediaSelector();
        else showLeaveRoomDialog();
    }

    /*
    LoaderManager Implementation
     */

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Build a cursor that loads all public messages in this room.

        if (mLowestId < 1) {
            // Calculate the lowest ID we want to show in this conversation,
            // in order to limit the amount of messages,
            // i.e. highest ID - 50 = lowest ID --> only show last 50 messages
            Cursor lowestIdCursor = getContentResolver().query(
                    ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC,
                    ChatObjectContract.PROJECTION_MSGS_PUB_USER,
                    ChatObjectContract.SELECTION_PUBLIC_MSGS_UNBLOCKED,
                    buildSelectionArgs(),
                    BaseColumns._ID + " DESC LIMIT " + mMessageLimit
            );
            final int idColumn = lowestIdCursor.getColumnIndex(BaseColumns._ID);
            if (idColumn > -1 && lowestIdCursor.moveToLast()) {
                mLowestId = lowestIdCursor.getInt(idColumn);
            }
            lowestIdCursor.close();
        }

        if (mLowestId < 0) mLowestId = 0;

        if (i == Action.PUBLIC_RECIPIENT_ID) {
            return new CursorLoader(
                    this,
                    ChatObjectContract.CONTENT_URI_ACTIONS_PUBLIC,
                    ChatObjectContract.PROJECTION_MSGS_PUB_USER,
                    ChatObjectContract.SELECTION_PUBLIC_MSGS_UNBLOCKED,
                    buildSelectionArgs(),
                    BaseColumns.MSG_TIME + " ASC"
            );
        }
        return null;
    }

    /**
     * The Loaders are only supposed to select messages that are above a certain ID
     * and that are in this room.
     *
     * @return Array that can be used in a Cursor Builder/CursorLoader as selectionArgs
     * to fill the placeholders in the selection String,
     * namely ChatObjectContract.SELECTION_PUBLIC_MSGS_UNBLOCKED
     */
    private String[] buildSelectionArgs() {
        return new String[]{
                String.valueOf(mLowestId),
                String.valueOf(SessionMainManager.getInstance().getRoomId(this))
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursorLoader.getId() == mConversationId) {
            super.onLoadFinished(cursorLoader, cursor);
            ViewAnimator.fadeView(findViewById(R.id.card_information), cursor.getCount() < 1);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if (cursorLoader.getId() == mConversationId) super.onLoaderReset(cursorLoader);
    }

    @Override
    public void restartLoaders() {
        super.restartLoaders();
        getSupportLoaderManager().restartLoader((int) mConversationId, null, this);
    }

    /*
    ActionBar Menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_public_conversation, menu);
        MenuItem conversationsItem = menu.findItem(R.id.item_conversations);


        // Setup the "private messages" ActionView.
        mNotificationIcon = MenuItemCompat.getActionView(conversationsItem);
        if (mNotificationIcon == null) return false;

        final boolean hasUnreadMessages = mNotificationCount > 0;
        mNotificationIcon.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ViewAnimator.quickFade(view, new ViewAnimator.Callback() {
                            @Override
                            public void onAnimationEnd() {
                                Intent userListActivity = new Intent(
                                        PublicConversationActivity.this,
                                        UserTabActivity.class
                                );
                                userListActivity.putExtra(
                                        Constants.EXTRA_SHOW_CONVERSATIONS,
                                        !hasUnreadMessages
                                );

                                startActivity(userListActivity);
                            }
                        });
                    }
                }
        );

        // Show the private message count.
        TextView notificationView = (TextView) mNotificationIcon.findViewById(R.id.text_convs_count);
        if (hasUnreadMessages) {
            notificationView.setVisibility(View.VISIBLE);
            notificationView.setText(
                    mNotificationCount < 100 ? String.valueOf(mNotificationCount) : "99+"
            );
        } else {
            notificationView.setVisibility(View.INVISIBLE);
        }

        // Inflate the other menu items.
        inflater.inflate(R.menu.menu_common_room, menu);
        inflater.inflate(R.menu.common_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void showLeaveRoomDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(R.string.want_to_leave);
        alert.setMessage(R.string.only_one_at_a_time);

        alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                SessionMainManager.getInstance().leaveSession(getApplicationContext(), "Left");
                startLaunchActivity();
            }
        });

        alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

}
