package org.eztarget.realay.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.R;
import org.eztarget.realay.content_providers.RoomsContract;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.ui.utils.FormatHelper;
import org.eztarget.realay.ui.utils.ImageLoader;

import java.util.HashMap;

/**
 * Created by michel on 22/11/14.
 */
public class RoomCursorAdapter extends CursorRecyclerViewAdapter<RoomCursorAdapter.RoomViewHolder> {

    private static final String TAG = RoomCursorAdapter.class.getSimpleName();

    private HashMap<Long, Room> mRoomMap = new HashMap<>();
    private boolean mDoShowDistanceBadge = false;
    private boolean mDoUseImperial = false;

    public RoomCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor);
        updateValues();
    }

    public void updateValues() {
        mDoShowDistanceBadge = LocationWatcher.from(getContext()).isEnabled();
        mDoUseImperial = PreferenceHelper.from(getContext()).doUseImperial();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        private View mCardView;
        protected ImageView mImageView;
        protected View mDistanceBadge;
        protected ImageView mHereIcon;
        protected TextView mDistanceValueView;
        protected TextView mTitleView;
        protected TextView mAddressView;
        protected TextView mSizeView;
        protected TextView mStartHoursView;
        protected TextView mEndHoursView;

        public RoomViewHolder(View v) {
            super(v);
            mCardView = v.findViewById(R.id.card_room);
            mImageView = (ImageView) v.findViewById(R.id.image_room_full);
            mDistanceBadge = v.findViewById(R.id.group_room_distance);
            mDistanceValueView = (TextView) v.findViewById(R.id.text_room_distance);
            mHereIcon = (ImageView) v.findViewById(R.id.image_here_icon);
            mTitleView = (TextView) v.findViewById(R.id.text_room_title);
            mAddressView = (TextView) v.findViewById(R.id.text_room_address);
            mSizeView = (TextView) v.findViewById(R.id.text_room_size);
            mStartHoursView = (TextView) v.findViewById(R.id.text_room_time);
            mEndHoursView = (TextView) v.findViewById(R.id.text_room_time_note);
        }
    }

    public Room getRoom(final int cursorPosition) {
        final Cursor cursor = getCursor();
        cursor.moveToPosition(cursorPosition);

        final int roomIdIndex = cursor.getColumnIndex(BaseColumns._ID);
        final long cursorRoomId = cursor.getLong(roomIdIndex);

        final Room cursorRoom = mRoomMap.get(cursorRoomId);
        if (cursorRoom == null) {
//            Log.d(LOG_TAG, "Creating Room object from cursor.");
            return RoomsContract.buildRoom(cursor);
        } else {
            return cursorRoom;
        }
    }

    @Override
    public RoomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View layout = inflater.inflate(R.layout.card_room, viewGroup, false);

//        final RoomViewHolder viewHolder = new RoomViewHolder(layout);
//        layout.setTag(getRoom(i));

        return new RoomViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(RoomViewHolder viewHolder, Cursor cursor) {
        final long cursorRoomId = cursor.getLong(0);
        Room cursorRoom = mRoomMap.get(cursorRoomId);
        if (cursorRoom == null) {
            cursorRoom = RoomsContract.buildRoom(cursor);
            if (cursorRoom == null) return;
            mRoomMap.put(cursorRoomId, cursorRoom);
        }

        viewHolder.mCardView.setTag(cursorRoom);

        viewHolder.mTitleView.setText(cursorRoom.getTitle());

        final Context context = getContext();

        if (mDoShowDistanceBadge) {
            viewHolder.mDistanceBadge.setVisibility(View.VISIBLE);

            if (cursorRoom.getDistance() > 30) {
                final String distance;
                distance = FormatHelper.buildRoomDistance(context, cursorRoom, mDoUseImperial);
                viewHolder.mHereIcon.setVisibility(View.INVISIBLE);
                viewHolder.mDistanceValueView.setText(distance);
                viewHolder.mDistanceValueView.setVisibility(View.VISIBLE);
            } else {
                viewHolder.mDistanceValueView.setVisibility(View.INVISIBLE);
                viewHolder.mHereIcon.setVisibility(View.VISIBLE);
            }
        } else if (viewHolder.mDistanceBadge.getVisibility() == View.VISIBLE) {
            viewHolder.mDistanceBadge.setVisibility(View.INVISIBLE);
        }

        final String address = cursorRoom.getAddress();
        if (!TextUtils.isEmpty(address) || address == "null") {
            viewHolder.mAddressView.setText(address);
        } else {
            final String coordinates = String.format(
                    getContext().getString(R.string.latitude_longitude),
                    cursorRoom.getLatLng().latitude,
                    cursorRoom.getLatLng().longitude
            );
            viewHolder.mAddressView.setText(coordinates);
        }

        final String size = FormatHelper.buildRoomSize(context, cursorRoom, mDoUseImperial);
        viewHolder.mSizeView.setText(size);
        viewHolder.mStartHoursView.setText(FormatHelper.buildRoomStartHour(context, cursorRoom));
        viewHolder.mEndHoursView.setText(FormatHelper.buildRoomEndHour(context, cursorRoom, true));

        final ImageLoader imageLoader = new ImageLoader(context);
        imageLoader.handle(cursorRoom, true);
        imageLoader.startLoadingInto(
                viewHolder.mImageView,
                true,
                null,
                R.drawable.ic_location_city_white_48dp
        );
    }

    public void removeCachedRooms() {
        mRoomMap = new HashMap<>();
    }
}
