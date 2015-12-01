package org.eztarget.realay.ui.adapter;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.ui.elements.DividerItemDecoration;
import org.eztarget.realay.ui.utils.ViewAnimator;

/**
 * The {@link android.support.v4.view.PagerAdapter} used to display pages in this sample.
 * The individual pages are simple and just display two lines of text. The important section of
 * this class is the {@link #getPageTitle(int)} method which controls what is displayed in the
 * {@link org.eztarget.realay.ui.elements.SlidingTabLayout}.
 */
public class UserListPagerAdapter
        extends PagerAdapter
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = UserListPagerAdapter.class.getSimpleName();

    private static final int TAB_COUNT = 2;

    public static final int TAB_POS_USER_LIST = 0;

    public static final int TAB_POS_CONV_LIST = 1;

    private static final int LOADER_ID_CONV_LIST = 7472;

    private static final int LOADER_ID_USER_LIST = 6182;

    private AppCompatActivity mActivity;

    private int mNotificationCount;

    private Room mRoom;

    private UserCursorAdapter mCursorAdapters[] = new UserCursorAdapter[TAB_COUNT];

    private RecyclerView mRecyclers[] = new RecyclerView[TAB_COUNT];

    private TextView mInformationView[] = new TextView[TAB_COUNT];

//    private Uri mImageShareUri;


    public UserListPagerAdapter(AppCompatActivity activity, Room room, int notificationCount) {
        super();
        mRoom = room;
        mActivity = activity;
        mNotificationCount = notificationCount;
    }

//    public void setImageShareUri(Uri imageShareUri) {
//        mImageShareUri = imageShareUri;
//    }

    /**
     * @return the number of pages to display
     */
    @Override
    public int getCount() {
        return TAB_COUNT;
    }

    /**
     * @return true if the value returned from {@link #instantiateItem(android.view.ViewGroup, int)} is the
     * same object as the {@link android.view.View} added to the {@link android.support.v4.view.ViewPager}.
     */
    @Override
    public boolean isViewFromObject(View view, Object o) {
        return o == view;
    }

    /**
     * Return the title of the item at {@code position}. This is important as what this method
     * returns is what is displayed in the {@link org.eztarget.realay.ui.elements.SlidingTabLayout}.
     * <p/>
     * Here we construct one using the position value, but for real application the title should
     * refer to the item's contents.
     */
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case TAB_POS_USER_LIST:
                return mActivity.getResources().getString(R.string.users);
            case TAB_POS_CONV_LIST:
                final String chats = mActivity.getResources().getString(R.string.chats);
                if (mNotificationCount < 1) {
                    return chats;
                } else {
                    return chats + " (" + mNotificationCount + ")";
                }
            default:
                return "";
        }
    }

    /**
     * Instantiate the {@link View} which should be displayed at {@code position}.
     * Here we inflate a mLayout from the apps resources
     * and then change the text view to signify the position.
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // Inflate a new mLayout from the resources.
        View view;
        view = mActivity.getLayoutInflater().inflate(R.layout.element_user_list, container, false);

        // Add the newly created View to the ViewPager
        container.addView(view);


        // Use a different title and Loader for User and Conversation list
        // and prepare the Information Views that appear when the lists are empty.
        final LoaderManager loaderMan = mActivity.getSupportLoaderManager();
        final String informationText;
        if (position == TAB_POS_CONV_LIST) {
            loaderMan.initLoader(LOADER_ID_CONV_LIST, null, this);
            informationText = view.getResources().getString(R.string.no_private_conversations);
        } else {
            loaderMan.initLoader(LOADER_ID_USER_LIST, null, this);
            informationText = view.getResources().getString(R.string.no_users);
        }
        mInformationView[position] = (TextView) view.findViewById(R.id.card_information);
        mInformationView[position].setText(informationText);

        mRecyclers[position] = (RecyclerView) view.findViewById(R.id.user_list_recycler);
        mRecyclers[position].setHasFixedSize(true);

        // Populate the adapter / list using a Cursor Loader.
        final boolean hasLongClick = position != TAB_POS_USER_LIST;
        mCursorAdapters[position] = new UserCursorAdapter(mActivity, null, hasLongClick);
//        mCursorAdapters[position].setImageShareUri(mImageShareUri);
        mRecyclers[position].setAdapter(mCursorAdapters[position]);

        // RecyclerView requires a LayoutManager to position items
        // and determine when it is time to recycle an item.
        LinearLayoutManager usersLayMan = new LinearLayoutManager(mActivity);
        usersLayMan.setOrientation(LinearLayoutManager.VERTICAL);

        mRecyclers[position].setLayoutManager(usersLayMan);

        mRecyclers[position].addItemDecoration(
                new DividerItemDecoration(mActivity, DividerItemDecoration.VERTICAL_LIST)
        );

        return view;
    }

    public void restartLoader(final boolean doRestartConversationLoader) {
        final LoaderManager loaderMan = mActivity.getSupportLoaderManager();
        if (doRestartConversationLoader) {
            loaderMan.restartLoader(LOADER_ID_CONV_LIST, null, this);
        } else {
            loaderMan.restartLoader(LOADER_ID_USER_LIST, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_CONV_LIST:
                // This Activity displays all private conversations
                // that the local User has in this Room.
                return new CursorLoader(
                        mActivity,
                        ChatObjectContract.CONTENT_URI_CONVS,
                        ChatObjectContract.PROJECTION_CONVERSATIONS,
                        ChatObjectContract.SELECTION_CONVS_IN_ROOM,
                        new String[]{String.valueOf(mRoom.getId())},
                        BaseColumns.MSG_TIME + " DESC"
                );

            case LOADER_ID_USER_LIST:
                return new CursorLoader(
                        mActivity,
                        ChatObjectContract.CONTENT_URI_USERS,
                        ChatObjectContract.PROJECTION_USERS,
                        ChatObjectContract.buildSessionUserListSelection(),
                        null,
                        BaseColumns.USER_NAME + " ASC"
                );

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final int position;
        position = (loader.getId() == LOADER_ID_CONV_LIST) ? TAB_POS_CONV_LIST : TAB_POS_USER_LIST;

        // Fade in or out the appropriate Information View,
        // depending on the amount of Users or Conversations currently displayed,
        // i.e. fade in if 0 or fade out if more than 0.
        ViewAnimator.fadeView(mInformationView[position], data.getCount() < 1);

        final CursorRecyclerViewAdapter adapter = mCursorAdapters[position];
        if (adapter == null) return;
        adapter.changeCursor(data);

//        RecyclerView recycler = mRecyclers[position];
//        if (recycler == null) return;
//        recycler.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        final int position;
        position = (loader.getId() == LOADER_ID_CONV_LIST) ? TAB_POS_CONV_LIST : TAB_POS_USER_LIST;
        mCursorAdapters[position].swapCursor(null);
    }
}
