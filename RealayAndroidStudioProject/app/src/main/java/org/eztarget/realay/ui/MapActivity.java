package org.eztarget.realay.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.eztarget.realay.R;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.ui.utils.ViewAnimator;

public class MapActivity extends PrepareSessionActivity implements OnMapReadyCallback {

    private static final String TAG = MapActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSION = 443;

    private GoogleMap mMap;

    private boolean mDidRequestPermission = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Room room = SessionMainManager.getInstance().getRoom(this, false);
        if (room == null) {
            onBackPressed();
            return;
        }

        setContentView(R.layout.activity_map);

        setupPasswordBar();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(room.getTitle());
        setSupportActionBar(toolbar);
        setUpBackButton(toolbar);
        showActionOverflowButton();
    }

    @Override
    protected void onResume() {
        super.onResume();

        final int isPlayServicesAvailable;
        isPlayServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (isPlayServicesAvailable == ConnectionResult.SUCCESS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final int storagePermission;
                storagePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (storagePermission != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Not permitted to write on storage.");
                    if (!mDidRequestPermission) {
                        final TextView errorTextView = (TextView) findViewById(R.id.text_map_error);
                        errorTextView.setText(R.string.allow_storage);
                        findViewById(R.id.button_request).setVisibility(View.VISIBLE);
                        ViewAnimator.fadeView(findViewById(R.id.card_error), true);

                        // TODO: Popup View above placeholder ImageView.
                        onRequestButtonClicked(null);
                    }
                    return;
                }
            }
        } else {
            mDidRequestPermission = false;
            GooglePlayServicesUtil.getErrorDialog(isPlayServicesAvailable, this, 1122).show();
            final TextView errorTextView = (TextView) findViewById(R.id.text_map_error);
            errorTextView.setText(R.string.update_play);
            findViewById(R.id.button_request).setVisibility(View.GONE);
            ViewAnimator.fadeView(findViewById(R.id.card_error), true);
            return;
        }

        // PlayServices is installed, up to date and the Permissions are looking good.
        setUpMap();
        ViewAnimator.fadeView(findViewById(R.id.card_error), false);
    }

    private void setUpMap() {
        final SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.frame_map, mapFragment);
        transaction.commitAllowingStateLoss();

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!SessionMainManager.getInstance().didLogin()) {
            SessionMainManager.getInstance().resetSession(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        if (!SessionMainManager.getInstance().didLogin()) {
            menuInflater.inflate(R.menu.common_room_details, menu);
        }
        menuInflater.inflate(R.menu.activity_map, menu);
        menuInflater.inflate(R.menu.common_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_map_type:
                toggleMapLayer();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && permissions.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0])) {
                        // The Permission that has been granted is the Storage Write Access.
                        setUpMap();
                    }
                }
            }
        }
    }

    /**
     * Alpha value of the zone circle displaying the radius of a Room
     */
    private static final int CIRCLE_ALPHA = 60;

    private static final float DEFAULT_ZOOM = 15f;

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        final Room sessionRoom = SessionMainManager.getInstance().getRoom(this, false);
        if (sessionRoom == null) {
            Log.e(TAG, "No room set.");
            onBackPressed();
            return;
        }

        mMap.setMyLocationEnabled(true);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(sessionRoom.getLatLng());
        markerOptions.title(sessionRoom.getTitle());
        mMap.setOnMarkerClickListener(
                new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        startRoomDetailsActivity();
                        return true;
                    }
                }
        );
        mMap.addMarker(markerOptions).showInfoWindow();

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(sessionRoom.getLatLng());
        circleOptions.radius(sessionRoom.getRadius());
        final int accentColor = getResources().getColor(R.color.primary);
        circleOptions.strokeColor(accentColor);
        final int fillColor = Color.argb(
                CIRCLE_ALPHA,
                Color.red(accentColor),
                Color.green(accentColor),
                Color.blue(accentColor)
        );
        circleOptions.fillColor(fillColor);
        circleOptions.strokeWidth(2f);

        mMap.addCircle(circleOptions);

        final LatLng roomLatLng = sessionRoom.getLatLng();
        final LatLng deviceLatLng = LocationWatcher.from(this).getLatLng();
        final int distance = sessionRoom.getDistance();

        final GoogleMap.CancelableCallback callback;
        callback = new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                finishMapSetup();
            }

            @Override
            public void onCancel() {
                finishMapSetup();
            }
        };

        if (deviceLatLng != null && distance > 10 && distance < 30000) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(roomLatLng);
            builder.include(deviceLatLng);
            final LatLngBounds bounds = builder.build();

            final View mapFrame = findViewById(R.id.frame_map);

            final int viewWidth = mapFrame.getWidth();
            final int viewHeight = mapFrame.getHeight();

            final int padding;
            if (viewHeight > 100 && viewHeight > 100) {
                padding = (int) (Math.max(viewWidth, viewHeight) * 0.1f);
            } else {
                padding = 10;
            }

            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding),
                    callback
            );
        } else {
            mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(roomLatLng, DEFAULT_ZOOM),
                    callback
            );
        }

    }

    private void finishMapSetup() {
        ViewAnimator.fadeView(findViewById(R.id.image_map_placeholder), false);
        final View errorCard = findViewById(R.id.card_error);
        if (errorCard.getVisibility() == View.VISIBLE) {
            ViewAnimator.disappearRight(errorCard, false, null);
        }
    }

    private void toggleMapLayer() {
        if (mMap != null) {
            if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            } else {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        }
    }

    public void onRequestButtonClicked(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String[] permission;
            permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(permission, REQUEST_PERMISSION);
            mDidRequestPermission = true;
        }
    }
}
