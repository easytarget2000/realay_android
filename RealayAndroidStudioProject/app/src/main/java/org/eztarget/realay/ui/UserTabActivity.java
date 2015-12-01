package org.eztarget.realay.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.eztarget.realay.Constants;
import org.eztarget.realay.R;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.SessionActionsManager;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.ui.adapter.UserListPagerAdapter;
import org.eztarget.realay.ui.elements.SlidingTabLayout;
import org.eztarget.realay.ui.utils.ViewAnimator;

/**
 * Created by michel on 06/02/15.
 */
public class UserTabActivity extends BaseActivity {

    private static final String TAG = UserTabActivity.class.getSimpleName();

    private UserListPagerAdapter mPagerAdapter;

    private SlidingTabLayout mSlidingTabLayout;

    private TextView mConversationsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the title of this activity.
        final Room room = SessionMainManager.getInstance().getRoom(this, false);
        if (room == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_user_tab);

        // Set up the Toolbar.
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
        setUpBackButton(toolbar);
        showActionOverflowButton();

        // Get the ViewPager and set it's PagerAdapter so that it can display items
        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager_user_list);
        mNotificationCount = SessionActionsManager.from(this).getUnreadPrivateCount();
        mPagerAdapter = new UserListPagerAdapter(this, room, mNotificationCount);

        viewPager.setAdapter(mPagerAdapter);

        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs_user_list);
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(viewPager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!SessionMainManager.getInstance().didLogin()) {
            Log.w(TAG, "User was forced to leaveSession onResume.");
            SessionMainManager.getInstance().leaveSession(this, TAG + "OutsideSession");
            startLaunchActivity();
            return;
        }

        mPagerAdapter.restartLoader(true);
        mPagerAdapter.restartLoader(false);

        // Listen to User and conversation changes (people leaving, joining, new messages etc.).
        IntentFilter userListFilter = new IntentFilter(Constants.ACTION_USER_LIST_CHANGED);
        userListFilter.addAction(Constants.ACTION_UNREAD_COUNT_CHANGED);
        registerReceiver(mUserListReceiver, userListFilter);

        IntentFilter unreadCountFilter = new IntentFilter();
        unreadCountFilter.addAction(Constants.ACTION_UNREAD_COUNT_CHANGED);
        registerReceiver(mUnreadReceiver, unreadCountFilter);

        setNotificationCount(SessionActionsManager.from(this).getUnreadPrivateCount());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUserListReceiver);
        unregisterReceiver(mUnreadReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_user_list, menu);
        inflater.inflate(R.menu.common_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_show_blocked) {
            startActivity(new Intent(this, BlockedUsersActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void setNotificationCount(int count) {
        if (mNotificationCount == count) return;

        if (count < 1) mNotificationCount = 0;
        else if (count > 99) mNotificationCount = 99;
        else mNotificationCount = count;

        if (mSlidingTabLayout == null) return;

        if (mConversationsView == null) {
            final int tabIndex = UserListPagerAdapter.TAB_POS_CONV_LIST;
            final View tabView = mSlidingTabLayout.getTabStrip().getChildAt(tabIndex);
            if (tabView == null || !(tabView instanceof TextView)) return;
            mConversationsView = (TextView) tabView;
        }

        String chats = getResources().getString(R.string.chats);
        if (mNotificationCount > 0) chats += " (" + mNotificationCount + ")";
        mConversationsView.setText(chats);
    }

    /**
     * Registered to receive Actions of type Constants.ACTION_USER_LIST_CHANGED,
     * see onResume() and ActionQueryService;
     * If a User joined or left the Room or changed their information,
     * the User lists need to be updated.
     */
    private BroadcastReceiver mUserListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            mPagerAdapter.restartLoader(action.equals(Constants.ACTION_UNREAD_COUNT_CHANGED));
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Cancel sharing.
        ConversationActivity.setSharingPrepared(null);

        Intent returnToPublicIntent = new Intent(this, PublicConversationActivity.class);
        returnToPublicIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToPublicIntent);
    }

}
