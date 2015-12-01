package org.eztarget.realay.managers;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.services.RoomListUpdateService;

import java.util.Date;

/**
 * Created by michel on 15/01/15.
 *
 */
public class LocationWatcher
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    /**
     * Singleton object instance
     */
    private static LocationWatcher instance = null;

    private static final String TAG = LocationWatcher.class.getSimpleName();

    private static final String FALLBACK_TAG = TAG + "-Fallback";

    private static final long INTERVAL_FAST = 2L * 60L * 1000L;

    private static final long INTERVAL_MEDIUM = 4L * 60L * 1000L;

    private static final long INTERVAL_SLOW = 10L * 60L * 1000L;

//    private static final long INTERVAL_BACKGROUND = 14L * 60L * 1000L;

    private static final float SMALLEST_DISPLACEMENT_BACKGROUND = 160f;

    private static final float SMALLEST_DISPLACEMENT_FOLLOWING = 80f;

    /**
     * True, if the Location is within the boundaries of the Session Room
     */
    private boolean mIsInSessionRadius = false;

    /**
     * True, if at least one useful Location provider is enabled, i.e. gps or network;
     * Default is true because it assumes the providers are on until they have been checked
     * to avoid unnecessary state changes and calls to RoomListUpdateService
     */
    private boolean mDidEnableProvider = true;

    /**
     * A combination of the Location Update Priority and the Interval;
     * stored to avoid Update restarts to existing settings
     */
    private int mSettings = -1;

    /**
     * Context in which this Singleton was instantiated
     */
    private Context mContext;

    /**
     * Google Service Client that requests and returns fused LocationUpdates
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Current device location
     */
    private Location mLocation;

    /**
     * Fallback Location Listener that does not use the Google API Client / Fused Location Provider
     */
    private android.location.LocationListener mFallbackListener;

    /**
     * The first time the App gets started, and in turn this watcher, force a Room List Update,
     * if Background Following is enabled.
     */
    private boolean mDidForceFirstUpdate = false;

    /**
     * To be set by RoomListUpdateService through doUpdateRoomsOnLocation();
     * If true, the Update Service will be forced to fetch
     * as soon as this Watcher receives a new Location.
     */
    private boolean mDoUpdateRoomsOnLocation = false;

    /**
     * Used for alternative Location requests,
     * if the device does not have a valid version of Google's Play Services installed
     */
    private boolean mHasPlayServices = true;

    /*
    Singleton construction:
     */

    public static LocationWatcher from(Context context) {
        if (instance == null || instance.mContext == null) instance = new LocationWatcher(context);
        return instance;
    }

    private LocationWatcher(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null.");
            return;
        }
        mContext = context;

        final int hasPlayServices;
        hasPlayServices = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
        mHasPlayServices = (hasPlayServices == ConnectionResult.SUCCESS);

        if (mHasPlayServices && mGoogleApiClient == null) {
            startFallbackLocationListener(true);
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    /*
    Location updates:
     */

    public void adjustLocationUpdates() {

//        final int priority;
        final long interval;
        final long fastestInterval;
        final float smallestDisplacement;
        if (SessionMainManager.getInstance().getRoom(mContext, false) != null) {
            // If a Room has been stored, a session is prepared.
            // Already start fast updates, even if a Session has not been started, yet.
//            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;

            // Follow more accurately, if the Location is not inside of the Room Radius.
            // Knowing when the Radius has been entered is more important
            // than knowing when it has been left.
            if (isInSessionRadius(false)) {
                smallestDisplacement = SMALLEST_DISPLACEMENT_FOLLOWING;
                interval = INTERVAL_SLOW;
                fastestInterval = INTERVAL_FAST;
            } else {
                smallestDisplacement = SMALLEST_DISPLACEMENT_BACKGROUND;
                interval = INTERVAL_MEDIUM;
                fastestInterval = INTERVAL_MEDIUM;
            }

        } else {
            // Outside of Sessions, update slowly when in foreground
            // and with the lowest priority if the app is in the background
            // and background updates are allowed.
            final PreferenceHelper preferenceHelper = PreferenceHelper.from(mContext);
            if (preferenceHelper.isInBackground()) {
                if (preferenceHelper.doFollowBackground()) {
//                    priority = LocationRequest.PRIORITY_LOW_POWER;
                    interval = INTERVAL_SLOW;
                    smallestDisplacement = SMALLEST_DISPLACEMENT_BACKGROUND;
                    fastestInterval = INTERVAL_MEDIUM;
                } else {
                    Log.d(TAG, "Not following in background.");
                    cancelLocationUpdates();
                    removeFallbackListener();
                    return;
                }
            } else {
//                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                interval = INTERVAL_MEDIUM;
                smallestDisplacement = SMALLEST_DISPLACEMENT_FOLLOWING;
                fastestInterval = INTERVAL_FAST;
            }
        }

        final int settings = (int) interval + (int) smallestDisplacement + (int) fastestInterval;
        if (mSettings == settings && mLocation != null) return;

        if (mHasPlayServices) {
            if (mGoogleApiClient == null) {
                Log.e(TAG, "Cannot start location updates. Google API Client is null.");
                mDidEnableProvider = false;
                return;
            }

            if (mGoogleApiClient.isConnected()) {
                if (updateProviderStatus()) {
                    final LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    locationRequest.setInterval(interval);
                    locationRequest.setFastestInterval(fastestInterval);
                    locationRequest.setSmallestDisplacement(smallestDisplacement);

                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleApiClient,
                            locationRequest,
                            this
                    );

                    mSettings = settings;
                    Log.d(TAG, "New: " + mSettings);
                } else {
                    mSettings = -2;
                }
            } else {
                mSettings = -3;
                mGoogleApiClient.connect();
            }
        } else if (mFallbackListener == null) {
            mSettings = -4;
            startFallbackLocationListener(false);
        }
    }

    private void startFallbackLocationListener(final boolean isOneShot) {
        if (!hasPermission()) return;

        if (!isOneShot && mFallbackListener != null) return;

        Log.d(FALLBACK_TAG, "One Shot: " + isOneShot);

        mFallbackListener = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                setLocation(location, isOneShot);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                Log.d(FALLBACK_TAG, s + " " + i);
                didEnableProvider();
            }

            @Override
            public void onProviderEnabled(String s) {
//                startFallbackLocationListener(isOneShot);
                didEnableProvider();
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };

        if (mContext == null) {
            Log.e(FALLBACK_TAG, "No context.");
            return;
        }

        final LocationManager locationManager;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager == null) {
            Log.e(TAG, "Could not find Location Service.");
            return;
        }

        // Look for the last Location stored in the fallback Manager.
        final Location networkLocation;
        networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        final Location gpsLocation;
        gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        // Compare the Locations, if they exist, and apply the most recent.
        if (gpsLocation != null) {
            if (networkLocation == null || networkLocation.getTime() < gpsLocation.getTime()) {
                setLocation(gpsLocation, false);
            } else {
                setLocation(networkLocation, false);
            }
        } else if (networkLocation != null) {
            setLocation(networkLocation, false);
        } else {
            setLocation(
                    locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER),
                    false
            );
        }

        if (isOneShot) {
            locationManager.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER,
                    mFallbackListener,
                    null
            );
            locationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    mFallbackListener,
                    null
            );
        } else {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    INTERVAL_SLOW,
                    SMALLEST_DISPLACEMENT_BACKGROUND,
                    mFallbackListener
            );
        }
    }

    public void cancelLocationUpdates() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) return;
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mSettings = -1;
    }

    private void removeFallbackListener() {
        if (mFallbackListener != null) {
            final LocationManager locationManager;
            locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

            locationManager.removeUpdates(mFallbackListener);
            mFallbackListener = null;
        }
    }

    /*
    Connection interface implementations:
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Connected.");
        updateProviderStatus();
        mSettings = -1;
        adjustLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection Suspended: " + i);
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, connectionResult.toString());
        mDidEnableProvider = false;
        startFallbackLocationListener(false);
    }

    /*
    LOCATION LISTENER INTERFACE IMPLEMENTATION
     */

    @Override
    public void onLocationChanged(Location location) {
        setLocation(location, true);
    }

    /*
    LOCATION GETTER/SETTER
     */

    public Location getLocation(final boolean doRestore) {
        if (mLocation == null) {
            startFallbackLocationListener(true);

            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            }

            if (mLocation == null && doRestore) {
                // Get the last Location that has been stored.
                mLocation = PreferenceHelper.from(mContext).getLastLocation();

                if (mLocation != null) {
                    // If a Location has been restored,
                    // use it only if the Provider has been enabled
                    // or if it is not too old.
                    if (updateProviderStatus()) {
                        final long locationTime = mLocation.getTime();
                        Log.d(TAG, "Restored Location from " + new Date(locationTime).toString());

                        final long now = System.currentTimeMillis();
                        if (now - locationTime > INTERVAL_SLOW) {
                            mLocation = null;
                        }
                    } else {
                        mLocation = null;
                    }
                }
            }


        }

        return mLocation;
    }

    public LatLng getLatLng() {
        if (getLocation(true) == null) return null;
        return new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
    }

    public void setLocation(final Location location, final boolean doRemoveFallbackListener) {
        if (location == null) {
            Log.e(TAG, "setLocation(null)");
            return;
        }

        mLocation = location;

        Log.d(TAG, mLocation.toString());

        final boolean wasDisabled = !mDidEnableProvider;
        mDidEnableProvider = true;
        // Send a broadcast, if the provider status changes.
        if (wasDisabled) {
            mContext.sendBroadcast(new Intent(Constants.ACTION_LOCATION_PROVIDER_CHANGED));
        }

        broadcastUpdate();

        if (doRemoveFallbackListener) removeFallbackListener();

        PreferenceHelper.from(mContext).storeLastLocation(mLocation);

        if (mDoUpdateRoomsOnLocation) {
            startRoomsUpdateService(true);
            mDoUpdateRoomsOnLocation = false;
        } else {
            startRoomsUpdateService(false);
        }
    }

    /**
     * Checks the provider status and updates the status flag accordingly;
     * Sends broadcasts if the flag was changed;
     *
     * @return Flag value
     */
    public boolean didEnableProvider() {
        final boolean wasEnabled = mDidEnableProvider;
        mDidEnableProvider = updateProviderStatus();
        if (mDidEnableProvider != wasEnabled) {
//            Log.d(TAG, "Provider status changed. Now: " + mDidEnableProvider);
            broadcastUpdate();
            if (mDidEnableProvider) {
                startRoomsUpdateService(false);
                adjustLocationUpdates();
            } else {
                cancelLocationUpdates();
            }
        }

        return mDidEnableProvider;
    }

    public boolean isEnabled() {
        final boolean hasLocation = getLocation(false) != null;
        final boolean hasProvider = didEnableProvider();
        return hasLocation && hasProvider;
    }

    public boolean isInSessionRadius(final boolean doCalculate) {
        if (getLocation(true) == null) {
            Log.d(TAG, "Not in Region because Location is unknown.");
            mIsInSessionRadius = false;
            return false;
        }

        final Room room = SessionMainManager.getInstance().getRoom(mContext, false);
        if (room == null) {
            return false;
        }

        didEnableProvider();
//        mIsInSessionRadius = ( < 20) && mDidEnableProvider;
        final int distance;
        if (doCalculate) {
            distance = getUpdatedDistanceToRoom(room);
            room.setDistance(distance);
        } else {
            distance = room.getDistance();
        }

        mIsInSessionRadius = distance < 20 && didEnableProvider();
        return mIsInSessionRadius;
    }

    public void doUpdateRoomsOnLocation() {
        mDoUpdateRoomsOnLocation = true;
    }

    private void startRoomsUpdateService(final boolean doForce) {
        // Start the RoomListUpdateService.
        Intent updateServiceIntent = new Intent(mContext, RoomListUpdateService.class);
        if (doForce) {
            updateServiceIntent.putExtra(Constants.KEY_FORCE_REFRESH, true);
        } else if (PreferenceHelper.from(mContext).doFollowBackground()) {
            // If Background Following is enabled,
            // force an update the very first time this instance starts.
            updateServiceIntent.putExtra(Constants.KEY_FORCE_REFRESH, !mDidForceFirstUpdate);
        }
        mDidForceFirstUpdate = true;
        mContext.startService(updateServiceIntent);
    }

    private void broadcastUpdate() {
        // Within Sessions, broadcast if the user has left or entered the room radius.
        if (!SessionMainManager.getInstance().didLogin()) return;

        final boolean wasInRoomRadius = mIsInSessionRadius;
        mIsInSessionRadius = isInSessionRadius(true);
        Log.d(TAG, "Broadcast. In radius? " + mIsInSessionRadius + ", old: " + wasInRoomRadius);

        // If we are in a Session and moved startLoadingInto our out of the Room radius,
        // send a broadcast that is received by the PenaltyReceiver,
        // so that it evaluates the situation with the Bouncer.
        if (wasInRoomRadius != mIsInSessionRadius) {
            Intent locEventBroadcast = new Intent(Constants.ACTION_PENALTY_EVENT);

            locEventBroadcast.putExtra(
                    Constants.EXTRA_LOCATION_EVENT,
                    mIsInSessionRadius ?
                            Constants.EVENT_LOCATION_ISSUE_RESOLVED :
                            Constants.EVENT_LOCATION_ISSUE_STARTED
            );

            mContext.sendBroadcast(locEventBroadcast);
        }
    }

    public int getUpdatedDistanceToRoom(final Room room) {
        if (getLocation(false) == null) return 117733;

        final LatLng roomLatLng = room.getLatLng();

        final Location roomLocation = new Location("");
        roomLocation.setLatitude(roomLatLng.latitude);
        roomLocation.setLongitude(roomLatLng.longitude);

        final float accuracy = mLocation.getAccuracy() * 1.4f;
        final float distanceAbsolute = mLocation.distanceTo(roomLocation);
        final int distanceToCentre = (int) (distanceAbsolute - accuracy);
        if (distanceToCentre < room.getRadius()) return 0;

        final int distance = distanceToCentre - room.getRadius();

        if (distance < 6) return 0;
        else return distance;
    }

    public boolean hasPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        final int coarsePermission;
        coarsePermission = mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (coarsePermission == PackageManager.PERMISSION_GRANTED) {
            final int finePermission;
            finePermission = mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (finePermission == PackageManager.PERMISSION_GRANTED) return true;
            Log.w(TAG, "No permission to access Fine Location.");
        }

        Log.w(TAG, "No permission to access Coarse Location.");
        return false;
    }

    private boolean updateProviderStatus() {
        if (!hasPermission()) return false;


        if (mHasPlayServices) {
            final LocationAvailability available;
            available = LocationServices.FusedLocationApi.getLocationAvailability(mGoogleApiClient);
            if (available != null && available.isLocationAvailable()) {
                return true;
            }
        }

        final ContentResolver contentResolver = mContext.getContentResolver();

        if (mLocation != null) {
            final long locationAge = System.currentTimeMillis() - mLocation.getTime();
            if (locationAge < INTERVAL_MEDIUM) return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final int locationMode;
            try {
                locationMode = Settings.Secure.getInt(
                        contentResolver,
                        Settings.Secure.LOCATION_MODE
                );
            } catch (Settings.SettingNotFoundException e) {
                Log.e(TAG, e.toString());
                return false;
            }

            if (locationMode == Settings.Secure.LOCATION_MODE_OFF) return false;
            if (locationMode == Settings.Secure.LOCATION_MODE_SENSORS_ONLY) return false;
        } else {
            final String locationProviders = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED
            );

            Log.d(TAG, "Pre-KitKat Provider check: " + locationProviders);
            if (TextUtils.isEmpty(locationProviders)) return false;
            if (locationProviders.contains(LocationManager.NETWORK_PROVIDER)) return true;
            if (locationProviders.equals("0")) return false;
        }

        return true;
    }

}
