package org.eztarget.realay.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.R;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.Bouncer;
import org.eztarget.realay.managers.LocalUserManager;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.ui.utils.ViewAnimator;
import org.eztarget.realay.utils.SharingGuide;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

/**
 * Created by michel on 31/12/14.
 */
public class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getSimpleName();

    protected int mNotificationCount = 0;

    protected View mNotificationIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Get the window through a reference to the activity.
            Window window = getWindow();
            if (window != null) {
                // Set the status bar colour of this window.
                int statusColor = getResources().getColor(R.color.primary_dark);
                window.setStatusBarColor(statusColor);
            }
        }
    }

    protected void showActionOverflowButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) return;

        final Field menuKeyField;
        try {
            menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
        } catch (NoSuchFieldException e) {
            Log.w(TAG, e.toString());
            return;
        }

        if (menuKeyField == null) return;

        menuKeyField.setAccessible(true);
        final ViewConfiguration config = ViewConfiguration.get(this);
        try {
            menuKeyField.setBoolean(config, false);
        } catch (IllegalAccessException e) {
            Log.w(TAG, e.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        SessionMainManager.getInstance().resumeSession();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_DISPLAY_MESSAGE);
        filter.addAction(Constants.ACTION_LOCATION_PROVIDER_CHANGED);
        registerReceiver(mReceiver, filter);

        // Store that the app is not in foreground.
        PreferenceHelper.from(this).setInBackground(false);

        // Ask the Bouncer if any warnings have to be displayed.
        // If not, check the Location status which also possibly shows Dialogs.
        if (showPendingBouncerDialogs()) PreferenceHelper.from(this).setProviderDialogLastMillis();
        else updateLocationStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        SessionMainManager.getInstance().pauseSession();
        unregisterReceiver(mReceiver);

        // Store that the app is possibly in the background.
        PreferenceHelper.from(this).setInBackground(true);

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        // If the app permanently moved in or out of the foreground,
                        // adjust the Location updates.
                        if (PreferenceHelper.from(BaseActivity.this).isInBackground()) {
                            LocationWatcher.from(BaseActivity.this).adjustLocationUpdates();
                        }
                    }
                },
                5000L
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//            case R.id.homeAsUp:
                onBackPressed();
                return true;
            case R.id.action_settings:
                startSettingsActivity();
                return true;
            case R.id.item_location_settings:
                startLocationSettingsActivity();
                return true;
            case R.id.item_directions:
                startDirections();
                return true;
            case R.id.item_share:
                SharingGuide.showLanguageDialog(this);
                return true;
            case R.id.action_room_details:
                startRoomDetailsActivity();
                return true;
            case R.id.action_profile_mine:
                startLocalProfileActivity();
                return true;
            case R.id.item_map:
                startMapActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void setUpBackButton(Toolbar toolbar) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            } else {
                Log.e(TAG, "ActionBar has not been prepared for setup.");
            }

            if (toolbar != null) {
                toolbar.setNavigationIcon(R.drawable.ic_navigate_before_white_24dp);
            }
        }
    }

    /*
    RECEIVER
     */

    /**
     * Receives messages from Managers, Services etc. that will be displayed to the user;
     * Activities register this receiver onResume
     * to listen to Constants.ACTION_DISPLAY_MESSAGE Broadcast Actions.
     * The type of message is defined in the Constants as an "event"
     * and stored in the extra Constants.EXTRA_WARN_REASON.
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String receivedAction = intent.getAction();
            if (receivedAction == null) return;

            if (receivedAction.equals(Constants.ACTION_LOCATION_PROVIDER_CHANGED)) {
                updateLocationStatus();
                return;
            }
            showPendingBouncerDialogs();
        }
    };

    private boolean showPendingBouncerDialogs() {
        return Bouncer.from(this).showPendingDialog(this);
    }

    protected BroadcastReceiver mUnreadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int newCount = intent.getIntExtra(Constants.EXTRA_UNREAD_COUNT, 0);
            setNotificationCount(newCount);
            ViewAnimator.wiggleIt(mNotificationIcon);
        }
    };

    /**
     * Dialog that shows a general "no location provider" warning
     */
    private AlertDialog mLocationDialog;

    /**
     * Interval in milliseconds in which no "no location provider" message will be displayed;
     */
    private static final long PROVIDER_DIALOG_INTERVAL_MS = 4 * 60 * 1000;

    private static final int REQUEST_PERMISSIONS = 442;

    protected boolean updateLocationStatus() {
        final LocationWatcher locationWatcher = LocationWatcher.from(this);

        if (locationWatcher.didEnableProvider()) {
            if (mLocationDialog != null && mLocationDialog.isShowing()) {
                mLocationDialog.cancel();
                Toast.makeText(this, R.string.location_enabled, Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (this instanceof RoomListActivity) return true;

        final long lastProviderDialogMillis;
        lastProviderDialogMillis = PreferenceHelper.from(this).getProviderDialogLastMillis();
        if (System.currentTimeMillis() - lastProviderDialogMillis < PROVIDER_DIALOG_INTERVAL_MS) {
            return true;
        }

        if (!locationWatcher.hasPermission()) {
            // Request Permissions if they are not granted.
            requestLocationPermissions();
        } else {
            // If the Permissions are set or this is running on API < 23,
            // remind the User to turn on a Location Provider.

            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(getResources().getString(R.string.enable_network));
            dialog.setCancelable(true);
            dialog.setPositiveButton(
                    getResources().getString(R.string.settings),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startLocationSettingsActivity();
                        }
                    }
            );

            try {
                mLocationDialog = dialog.show();
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.toString());
            }
        }

        PreferenceHelper.from(this).setProviderDialogLastMillis();
        return false;
    }

    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String[] permissions = new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
            requestPermissions(permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            // The Permissions have changed.
            // If the Permissions are granted now, but the Providers are disabled,
            // directly open the Settings.

            if (LocationWatcher.from(this).hasPermission()) {
                if (!LocationWatcher.from(this).didEnableProvider()) {
                    PreferenceHelper.from(this).setProviderDialogLastMillis();
                    startLocationSettingsActivity();
                }
            }
        }
    }

    protected void setNotificationCount(int count) {
        mNotificationCount = count;
        supportInvalidateOptionsMenu();
    }

    /**
     * Returns to the very first Activity of this app: the RoomListActivity.
     */
    public void startLaunchActivity() {
        Intent returnToHomeIntent = new Intent(this, RoomListActivity.class);
        returnToHomeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(returnToHomeIntent);
    }

    /**
     * Finds the appropriate Activity that displays the Location Service settings on this device
     * and starts it
     */
    protected void startLocationSettingsActivity() {
        if (LocationWatcher.from(this).hasPermission()) {
            final Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            List<ResolveInfo> list = getPackageManager().queryIntentActivities(settings, 0);

            if (list.size() < 1) settings.setAction(Settings.ACTION_SECURITY_SETTINGS);

            startActivity(settings);
        } else {
            requestLocationPermissions();
        }
    }

    protected void startMapActivity() {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    protected void startProfileActivity(long userId) {
        Intent profileActivity = new Intent(this, UserDetailsActivity.class);
        profileActivity.putExtra(Constants.EXTRA_USER_ID, userId);
        startActivity(profileActivity);
    }

    protected void startUserListActivity() {
        Intent userListIntent = new Intent(this, UserTabActivity.class);
        startActivity(userListIntent);
    }

    protected void hideKeyboard() {
        // Check if no view has focus:
        final View focus = getCurrentFocus();
        if (focus != null) {
            final InputMethodManager inputManager;
            inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(
                    focus.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS
            );
        }
    }

    /**
     * Start the activity showing the local profile or prompt a user registration/login if needed
     */
    protected void startLocalProfileActivity() {
        final Intent profileActivity = LocalUserManager.getInstance().getProfileIntent(
                this,
                true,
                getClass().getName()
        );
        if (profileActivity == null) return;
        startActivity(profileActivity);
    }

    protected void showLocationDialog(final String title, final String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        if (title != null) alert.setTitle(title);
        if (message != null) alert.setMessage(message + "\n");

        alert.setNeutralButton(R.string.settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startLocationSettingsActivity();
            }
        });

        alert.setPositiveButton(R.string.directions, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                startDirections();
            }
        });

        alert.show();
    }

    /*
    ACTIVITY STARTS
     */

    protected void startSettingsActivity() {
        final Intent settingsActivity = new Intent(this, SettingsActivity.class);
        startActivity(settingsActivity);
    }

    protected void startRoomDetailsActivity() {
        startActivity(new Intent(this, RoomDetailsActivity.class));
    }

    private static final String URL_FORMAT_GMAPS_DIRECTION =
            "http://maps.google.com/maps?&daddr=";

    protected void startDirections() {
        final Room room = SessionMainManager.getInstance().getRoom(this, false);
        if (room == null) return;
        final LatLng pos = room.getLatLng();
        final String address = room.getAddress();
        final String url;
        url = String.format(
                Locale.ENGLISH,
                URL_FORMAT_GMAPS_DIRECTION + "%f,%f", pos.latitude, pos.longitude
        );

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    /**
     * Exists to defeat unhandled onClick action; to be overridden
     */
    public void informationOnClick(View view) {
    }
}
