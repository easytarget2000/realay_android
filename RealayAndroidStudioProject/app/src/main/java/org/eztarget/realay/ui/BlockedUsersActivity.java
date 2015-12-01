package org.eztarget.realay.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.ChatObjectContract;
import org.eztarget.realay.managers.UsersCache;
import org.eztarget.realay.ui.adapter.UserCursorAdapter;
import org.eztarget.realay.ui.utils.ViewAnimator;

/**
 * Created by michel on 09/02/15.
 */
public class BlockedUsersActivity
        extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = BlockedUsersActivity.class.getSimpleName();

    private static final int LOADER_ID = 9;

    private RecyclerView mRecycler;

    private UserCursorAdapter mCursorAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocked_users);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.blocked_users);
        setSupportActionBar(toolbar);
        setUpBackButton(toolbar);

        // RecyclerView requires a LayoutManager to position items
        // and determine when it is time to recycle an item.
        LinearLayoutManager linearLayMan = new LinearLayoutManager(this);
        linearLayMan.setOrientation(LinearLayoutManager.VERTICAL);
        mRecycler = (RecyclerView) findViewById(R.id.recycler_blocked_users);
        mRecycler.setHasFixedSize(true);
        mRecycler.setLayoutManager(linearLayMan);

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                this,
                ChatObjectContract.CONTENT_URI_USERS,
                null,
                ChatObjectContract.SELECTION_BLOCKED_USERS,
                null,
                BaseColumns.USER_NAME + " ASC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mRecycler == null) return;

        if (mCursorAdapter == null) {
            mCursorAdapter = new UserCursorAdapter(this, data);
        }
        mCursorAdapter.changeCursor(data);
        mRecycler.setAdapter(mCursorAdapter);

        final boolean doShowInformation;
        doShowInformation = mCursorAdapter == null || mCursorAdapter.getItemCount() < 1;
        ViewAnimator.fadeView(findViewById(R.id.card_information), doShowInformation);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

    public void unblockUser(final long userId) {
//        final UserCursorAdapter.UserViewHolder viewHolder;
//        viewHolder = (UserCursorAdapter.UserViewHolder) view.getTag();
//
//        if (viewHolder == null) {
//            Log.e(TAG, "unblockUser: ViewHolder is null.");
//            return;
//        }
//        final Object viewTag = view.getTag();
//        if (!(viewTag instanceof Long)) return;
//        final long unblockId = (Long) viewTag;

        UsersCache.getInstance().unblockUser(this, userId);
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }
}
