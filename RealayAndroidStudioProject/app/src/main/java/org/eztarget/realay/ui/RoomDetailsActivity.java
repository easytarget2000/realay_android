package org.eztarget.realay.ui;

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.R;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.Bouncer;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.ui.utils.FormatHelper;
import org.eztarget.realay.ui.utils.ImageLoader;

/**
 * Created by michel on 01/12/14.
 *
 */
public class RoomDetailsActivity extends PrepareSessionActivity {

    private static final String TAG = RoomDetailsActivity.class.getSimpleName();

    private boolean mDoUseImperial = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setUpBackButton(toolbar);

        // Prepare dividers for full list layout.
        findViewById(R.id.divider_time).setVisibility(View.VISIBLE);

        initElements();
        setupPasswordBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDoUseImperial = PreferenceHelper.from(this).doUseImperial();
        initElements();
    }

    private void initElements() {
        final Room room = SessionMainManager.getInstance().getRoom(this, false);
        if (room == null) {
            onBackPressed();
            return;
        }

        CollapsingToolbarLayout collapsingToolbar;
        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle(room.getTitle());

        /*
        IMAGE VIEW
         */

        final ImageView imageView = (ImageView) findViewById(R.id.image_room_full);

//        findViewById(R.id.shadow_top).setVisibility(View.VISIBLE);

        // Always download the high-res image after displaying the low-res image.
        final ImageLoader imageLoader = new ImageLoader(getApplicationContext());
        imageLoader.handle(room, true);
        imageLoader.startLoadingInto(imageView, true, null, R.drawable.ic_location_city_white_48dp);

        // Distance Card:

        final boolean didLogin = SessionMainManager.getInstance().didLogin();

        final TextView distanceView = (TextView) findViewById(R.id.text_room_distance);

        final String secondMessage;
        if (didLogin) {
            if (Bouncer.from(this).getLastReason() == Bouncer.REASON_LOCATION) {
                final String messageFormat = getString(R.string.return_location);
                final String kickTime = Bouncer.from(this).buildFormattedKickDate();
                secondMessage = String.format(messageFormat, kickTime);
            } else {
                secondMessage = null;
            }
        } else {
            secondMessage = getString(R.string.get_password_to_join);
        }

        final String distance;
        if (LocationWatcher.from(this).isInSessionRadius(true)) {
            distance = getString(R.string.currently_here);
        } else if (LocationWatcher.from(this).getLocation(false) == null){
            distance = getString(R.string.enable_network);
        } else {
            final String distanceFormat;
            distanceFormat = getString(R.string.currently_distance_away);
            final String value = FormatHelper.buildRoomDistance(this, room, mDoUseImperial);
            distance = String.format(distanceFormat, value);
        }

        if (secondMessage == null) distanceView.setText(distance);
        else distanceView.setText(distance + "\n\n" + secondMessage);

        // Details Card:

        // Description text:
        TextView descriptionView = (TextView) findViewById(R.id.text_room_description);
        descriptionView.setText(room.getDescription());

        // Address:
        final String address = room.getAddress();
        if (!TextUtils.isEmpty(address) || address == "null") {
            ((TextView) findViewById(R.id.text_room_address)).setText(address);
        } else {
            final String coordinates = String.format(
                    getString(R.string.latitude_longitude),
                    room.getLatLng().latitude,
                    room.getLatLng().longitude
            );
            ((TextView) findViewById(R.id.text_room_address)).setText(coordinates);
        }

        // Room size/radius:
        final String size;
        size = FormatHelper.buildRoomSize(this, room, mDoUseImperial);
        ((TextView) findViewById(R.id.text_room_size)).setText(size);

        // Timestamps:
        ((TextView) findViewById(R.id.text_room_time)).setText(
                FormatHelper.buildRoomStartHour(this, room)
        );
        ((TextView) findViewById(R.id.text_room_time_note)).setText(
                FormatHelper.buildRoomEndHour(this, room, true)
        );

        // Number of users:
        TextView numOfUsersView = (TextView) findViewById(R.id.text_room_user_count);

        final int numberOfUsers;
        if (SessionMainManager.getInstance().didLogin()) {
            numberOfUsers = SessionMainManager.getInstance().numberOfUsers();
        } else {
            numberOfUsers = room.getNumOfUsers();
        }
        numOfUsersView.setText(String.valueOf(numberOfUsers));
        findViewById(R.id.group_room_user_count).setVisibility(View.VISIBLE);

        FloatingActionButton actionButton;
        actionButton = (FloatingActionButton) findViewById(R.id.button_action);

        if (didLogin) {
            actionButton.setImageResource(R.drawable.ic_map_white_36dp);
            findViewById(R.id.image_share_icon).setVisibility(View.VISIBLE);
            findViewById(R.id.bar_prepare).setVisibility(View.GONE);
            findViewById(R.id.view_password_bar_shadow).setVisibility(View.GONE);
        } else {
            findViewById(R.id.image_share_icon).setVisibility(View.GONE);
            findViewById(R.id.bar_prepare).setVisibility(View.VISIBLE);
            findViewById(R.id.view_password_bar_shadow).setVisibility(View.VISIBLE);
        }
    }

    public void roomItemOnClick(View view) {
    }

    public void onActionButtonClicked(View view) {
        if (SessionMainManager.getInstance().didLogin()) startMapActivity();
        else onBackPressed(null);
    }

    public void informationOnClick(View view) {
        updateInformationView(false);
    }

    public void onBackPressed(View view) {
        onBackPressed();
    }
}
