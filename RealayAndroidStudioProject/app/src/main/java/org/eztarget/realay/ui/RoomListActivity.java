package org.eztarget.realay.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.RoomsContentProvider;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.services.RoomListUpdateService;
import org.eztarget.realay.ui.adapter.RoomCursorAdapter;
import org.eztarget.realay.ui.utils.ViewAnimator;
import org.eztarget.realay.utils.APIHelper;

public class RoomListActivity
        extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    /**
     * Tag used by all calls to Log methods.
     */
    private static final String TAG = RoomListActivity.class.getSimpleName();

    /**
     * Time interval in which empty results cannot replace recently refreshed data in the Room list
     */
    private static final long INTERVAL_REFRESH = 30L * 1000L;

    /**
     * Contains the RecyclerView and listens for user gestures
     */
    private SwipeRefreshLayout mSwipeLayout;

    /**
     * Actual Room list
     */
    private RecyclerView mRecycler;

    /**
     * Contains the current list of Rooms and places them into Views for the Recycler
     */
    private RoomCursorAdapter mCursorAdapter;

    /**
     * If true, the Progress Bar is shown instead of the Action Button.
     */
    private boolean mIsRefreshing = false;

    /**
     * Stores the last list refresh time to avoid flicker
     */
    private long mLastResultsMillis;

    /**
     * Determines the behaviour of updateInformationView()
     */
    private boolean mDoShowInformation = true;

    /**
     * If true, the Information View is currently performing the Appear Animation.
     */
    private boolean mIsExpandingInformation = false;

    /**
     * If true, the Information View is currently performing the Disappear Animation.
     */
    private boolean mIsCollapsingInformation = false;

    /**
     * Stored to check if the unit preferences have been changed while this Activity was left
     */
    private boolean mDoUseImperial = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_room_list);

        mDoUseImperial = PreferenceHelper.from(this).doUseImperial();

        // Populate the adapter / list using a Cursor Loader.
        mCursorAdapter = new RoomCursorAdapter(this, null);

        // RecyclerView requires a LayoutManager to position items
        // and determine when it is time to recycle an item.
        LinearLayoutManager linearLayMan = new LinearLayoutManager(this);
        linearLayMan.setOrientation(LinearLayoutManager.VERTICAL);

        mRecycler = (RecyclerView) findViewById(R.id.recycler_rooms);
        mRecycler.setHasFixedSize(true);
        mRecycler.setAdapter(mCursorAdapter);
        mRecycler.setLayoutManager(linearLayMan);
        getSupportLoaderManager().initLoader(UIConstants.LOADER_ID_ROOM_LIST, null, this);

        // Setup the Toolbar.
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.near_you);
        setSupportActionBar(toolbar);
        final ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) supportActionBar.setDisplayShowTitleEnabled(false);
        showActionOverflowButton();

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_rooms);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorSchemeResources(
                R.color.primary,
                R.color.secondary_text,
                R.color.accent,
                R.color.secondary_text
        );

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Since this is the Launch Activity, check the status of the Session.
        // If in a Session, go directly to the ConversationActivity.
        // If a Session can be restored, go to the JoinActivity.
        SessionMainManager session = SessionMainManager.getInstance();
        final Room restoredRoom = session.getRoom(this, true);

        if (LocationWatcher.from(this).isInSessionRadius(false)) {
            if (session.didLogin()) {
                LocationWatcher.from(this).adjustLocationUpdates();
                Intent sessionActivity = new Intent(this, PublicConversationActivity.class);
                startActivity(sessionActivity);
                return;

            } else if (PreferenceHelper.from(this).doReLogin()) {
                LocationWatcher.from(this).adjustLocationUpdates();
                session.prepareSession(this, restoredRoom);
                Intent joinIntent = new Intent(this, JoinActivity.class);
                joinIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(joinIntent);
                return;

            } else {
                Log.d(TAG, "No Session stored for re-login.");
            }
        } else {
            Log.d(TAG, "Not looking for a stored Session.");
        }

        // Remove confusing logins caused by delayed location detection.
        SessionMainManager.getInstance().resetSession(this);
        // Resetting the Session Manager requires slowing down the Location Updates.
        LocationWatcher.from(this).adjustLocationUpdates();

        // Start the RoomListUpdateService,
        // if the distances should be updated.
        // The Service decides if a server fetch is due.
        final boolean hasOldResults;
        hasOldResults = (System.currentTimeMillis() - mLastResultsMillis) > INTERVAL_REFRESH;
        if (hasOldResults || mCursorAdapter.getItemCount() < 1) {
            startService(new Intent(this, RoomListUpdateService.class));
        }

        mCursorAdapter.updateValues();

        // Check if the units preferences have changed and rebuild the Recycler if needed.
        final boolean doUseImperialNow = PreferenceHelper.from(this).doUseImperial();
        if (mDoUseImperial != doUseImperialNow) {
            mDoUseImperial = doUseImperialNow;
            mRecycler.setAdapter(mCursorAdapter);
        }
        final IntentFilter updateReceiverFilter;
        updateReceiverFilter = new IntentFilter(Constants.ACTION_ROOM_LIST_UPDATE);
        registerReceiver(mUpdateReceiver, updateReceiverFilter);

        final View informationView = findViewById(R.id.text_information);
        if (mDoShowInformation && (informationView.getVisibility() == View.VISIBLE)) {
            ViewAnimator.slowPulse(informationView);
        }

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        updateInformationView("onResume Handler");
                    }
                },
                1800L
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!PreferenceHelper.from(this).doFollowBackground()) {
            LocationWatcher.from(this).cancelLocationUpdates();
        }

        try {
            unregisterReceiver(mUpdateReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unregister Receiver: " + e.toString());
        }

    }

    /**
     * Receiver that changes the UI to show that a Room List Update is happening right now.
     */
    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mIsRefreshing = true;
            showRefreshAnimations(false);
            final Runnable progressResetRunner = new Runnable() {
                @Override
                public void run() {
                    if (mDoShowInformation) {
                        mIsRefreshing = false;
                        updateInformationView("onRefresh Timeout Handler");
                    }
                }
            };

            new Handler().postDelayed(progressResetRunner, 10000L);
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        final int selection_radius = APIHelper.SEARCH_RADIUS * 1000;

        final String selection =
                BaseColumns.ROOM_DISTANCE + "<" + selection_radius
                        + " AND ("
                        + BaseColumns.ROOM_END_DATE + ">" + (System.currentTimeMillis() / 1000L)
                        + " OR " + BaseColumns.ROOM_END_DATE + "=0)";

        return new CursorLoader(
                this,
                RoomsContentProvider.CONTENT_URI,
                null,
                selection,
                null,
                BaseColumns.ROOM_DISTANCE + " ASC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        //        if (mCursorAdapter.getItemCount() > 0) {
//            // Results are already being displayed. Avoid flickering.
//            if ((System.currentTimeMillis() - mLastResultsMillis) < INTERVAL_REFRESH) {
//                Log.d(TAG, "Skipping Cursor Loader replacement.");
//                return;
//            }
//        }
        final int cursorCount = cursor.getCount();

        if (mCursorAdapter == null || mSwipeLayout == null) {
            mDoShowInformation = true;
            updateInformationView("NullAdapter");
            return;
        }

        mSwipeLayout.setRefreshing(false);


        if (cursorCount > 0) mLastResultsMillis = System.currentTimeMillis();

        mCursorAdapter.changeCursor(cursor);

        // Show or hide the information View depending on the Load result.
        // If the Loader returned results, the information is hidden immediately,
        // if the result is empty, a Handler calls the GUI updated delayed.
        final boolean isShowingInformation = mDoShowInformation;
        mDoShowInformation = mCursorAdapter.getItemCount() < 1;
        mIsRefreshing = false;
        if (!isShowingInformation == mDoShowInformation) {
            final Runnable delayedInformation;
            delayedInformation = new Runnable() {
                @Override
                public void run() {
                    mDoShowInformation = mCursorAdapter.getItemCount() < 1;
                    if (mDoShowInformation) {
                        updateInformationView("onLoadFinished handler");
                    }
                }
            };

            if (mDoShowInformation) {
                new Handler().postDelayed(delayedInformation, 1800L);
            } else {
                updateInformationView("onLoadFinished");
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mLastResultsMillis = 0L;
    }

    private void updateInformationView(final String tag) {
//        Log.d(TAG, "updateInformationView: " + tag + ", " + mDoShowInformation);

        final View background = findViewById(R.id.view_information_background);

        ViewAnimator.fadeView(findViewById(R.id.swipe_rooms), !mDoShowInformation);

        // Fill the screen with the background circle
        // or shrink it until it disappears,
        // depending on mDoShowInformation.

        final View content = findViewById(R.id.view_information_content);
        if (mDoShowInformation && !mIsExpandingInformation) {
            // Displaying the Information View:

            mIsExpandingInformation = true;

            // Do not change the text while a collapsing animation has been started.
            if (!mIsCollapsingInformation) {
                final TextView information = (TextView) findViewById(R.id.text_information);

                if (LocationWatcher.from(this).isEnabled()) {
                    information.setText(R.string.no_realays_found);
                } else {
                    information.setText(R.string.cannot_locate);
                }
            }

            final ViewAnimator.Callback finalCallback;
            finalCallback = new ViewAnimator.Callback() {
                @Override
                public void onAnimationEnd() {
                    mIsExpandingInformation = false;
                    final ActionBar actionBar;
                    actionBar = getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setDisplayShowTitleEnabled(!mDoShowInformation);
                    }
                }
            };

            ViewAnimator.scaleAnimateViewFillScreen(
                    findViewById(R.id.view_information_circle),
                    true,
                    background,
                    new ViewAnimator.Callback() {
                        @Override
                        public void onAnimationEnd() {
                            // Animate the appearance/disappearance of the information View.
                            ViewAnimator.scaleAnimateView(
                                    content,
                                    true,
                                    true,
                                    0.5f,
                                    0.5f,
                                    finalCallback
                            );
                        }
                    }
            );
        } else if (!mIsCollapsingInformation) {
            // Hiding the Information View:

            final ViewAnimator.Callback finalCallback;
            finalCallback = new ViewAnimator.Callback() {
                @Override
                public void onAnimationEnd() {
                    mIsCollapsingInformation = false;
                    final ActionBar actionBar;
                    actionBar = getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setDisplayShowTitleEnabled(!mDoShowInformation);
                    }
                }
            };

            mIsCollapsingInformation = true;
            ViewAnimator.scaleAnimateView(
                    content,
                    false,
                    true,
                    0.5f,
                    0.5f,
                    new ViewAnimator.Callback() {
                        @Override
                        public void onAnimationEnd() {
                            ViewAnimator.scaleAnimateViewFillScreen(
                                    findViewById(R.id.view_information_circle),
                                    false,
                                    background,
                                    finalCallback
                            );
                        }
                    }
            );

        }

        mSwipeLayout.setEnabled(!mDoShowInformation);

//        findViewById(R.id.coordinator).setEnabled(!mDoShowInformation);

        if (mDoShowInformation) {
            final FloatingActionButton actionButton;
            actionButton = (FloatingActionButton) findViewById(R.id.button_action);
            if (mIsRefreshing) {
                findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                actionButton.setVisibility(View.INVISIBLE);
            } else {
                findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
                actionButton.setVisibility(View.VISIBLE);
            }

            ViewAnimator.scaleAnimateView(actionButton, !mIsRefreshing);
            actionButton.setEnabled(!mIsRefreshing);
            actionButton.setClickable(!mIsRefreshing);

            ViewAnimator.scaleAnimateView(findViewById(R.id.group_action_progress), true);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_room_list, menu);
        getMenuInflater().inflate(R.menu.common_settings, menu);
        return true;
    }

    public void roomItemOnClick(View view) {
        final Object tag = view.getTag();
//        ViewAnimator.quickFade(
//                view,
//                new ViewAnimator.Callback() {
//                    @Override
//                    public void onAnimationEnd() {
                        if (tag == null || !(tag instanceof Room)) {
                            Log.e(TAG, "View does not contain a Room to open.");
                            return;
                        }

                        final Room selectedRoom = (Room) tag;
                        final String address = selectedRoom.getAddress();
                        if (!TextUtils.isEmpty(address)
                                && address.startsWith(Room.ADDRESS_PLAYSTORE_REDIRECT)) {
                            // If Users are forced to update the App,
                            // this will be communicated through a dummy room with a certain Address.
                            // Tapping on this Card, opens the PlayStore.
                            final Intent urlIntent = new Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("http://android.realay.net")
                            );
                            startActivity(urlIntent);
                        } else {
                            // Generally, just open the Map from here and prepare the Session.
                            final boolean didPrepareSession;
                            didPrepareSession = SessionMainManager.getInstance().prepareSession(
                                    RoomListActivity.this,
                                    selectedRoom
                            );
                            if (!didPrepareSession) return;
                            startMapActivity();
                        }
//                    }
//                }
//        );
    }

    @Override
    public void onRefresh() {
        mIsRefreshing = true;
        mLastResultsMillis = 0L;

        LocationWatcher.from(this).adjustLocationUpdates();
        if (mCursorAdapter != null) mCursorAdapter.removeCachedRooms();

        Intent updateService = new Intent(this, RoomListUpdateService.class);
        updateService.putExtra(Constants.KEY_FORCE_REFRESH, true);
        startService(updateService);

        final Runnable progressResetRunner = new Runnable() {
            @Override
            public void run() {
                if (mDoShowInformation) {
                    mIsRefreshing = false;
                    updateInformationView("onRefresh Timeout Handler");
                }
            }
        };

        new Handler().postDelayed(progressResetRunner, 5000L);
    }

    /**
     * Called by a Button or ImageView inside of the information panel at the top of the Activity
     *
     * @param view View that was clicked
     */
    @Override
    public void informationOnClick(View view) {
        onRefresh();
    }

    public void onActionButtonClicked(final View view) {
        showRefreshAnimations(true);
    }

    private void showRefreshAnimations(final boolean doRefresh) {
        mSwipeLayout.setEnabled(!mDoShowInformation);
        mSwipeLayout.setClickable(!mDoShowInformation);

        final View actionLayout = findViewById(R.id.group_action_progress);

        ViewAnimator.scaleAnimateView(
                actionLayout,
                false,
                true,
                0.5f,
                0.5f,
                new ViewAnimator.Callback() {
                    @Override
                    public void onAnimationEnd() {
                        findViewById(R.id.button_action).setVisibility(View.INVISIBLE);
                        findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
                        ViewAnimator.scaleAnimateView(
                                actionLayout,
                                true,
                                true,
                                0.5f,
                                0.5f,
                                new ViewAnimator.Callback() {
                                    @Override
                                    public void onAnimationEnd() {
                                        if (doRefresh) onRefresh();
                                    }
                                }
                        );
                    }
                }
        );
    }
}
