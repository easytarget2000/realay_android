/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eztarget.realay.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.BatteryManager;
import android.util.Log;

import org.eztarget.realay.Constants;
import org.eztarget.realay.PreferenceHelper;
import org.eztarget.realay.content_providers.BaseColumns;
import org.eztarget.realay.content_providers.RoomsContentProvider;
import org.eztarget.realay.content_providers.RoomsContract;
import org.eztarget.realay.data.Room;
import org.eztarget.realay.managers.LocationWatcher;
import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.receivers.ConnectivityChangedReceiver;
import org.eztarget.realay.utils.APIHelper;
import org.eztarget.realay.utils.DeviceStatusHelper;

import java.util.Date;

/**
 * Service that requests a list of nearby rooms from the underlying web service.
 */
public class RoomListUpdateService extends IntentService {

    protected static String TAG = RoomListUpdateService.class.getSimpleName();

    /**
     * Time interval in milliseconds between updates that are considered old,
     * so that an update is triggered
     */
    private static final long UPDATE_TIME_INTERVAL_MILLIS = AlarmManager.INTERVAL_FIFTEEN_MINUTES;

    /**
     * Distance between location updates that are considered far apart,
     * so that an update is triggered;
     */
    private static final int UPDATE_DISTANCE = 500;

    /**
     * Distance between location updates that are considered far apart,
     * so that an update is triggered;
     * Maximum value for use in background monitoring
     */
    private static final int UPDATE_DISTANCE_MAX = UPDATE_DISTANCE * 2;

    public RoomListUpdateService() {
        super(TAG);
    }

    /**
     * {@inheritDoc}
     * Checks the battery and connectivity state before removing stale venues
     * and initiating a server poll for new venues around the specified
     * location within the given radius.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Remove old Rooms.
        final long oldestAllowedMillis;
        oldestAllowedMillis = System.currentTimeMillis() - 2L * 60L * 60L * 1000L;
        final String where = BaseColumns.LAST_UPDATE + "<" + oldestAllowedMillis;
        getContentResolver().delete(RoomsContentProvider.CONTENT_URI, where, null);

        final Context context = getApplicationContext();

        // Check if the Location has been established.
        LocationWatcher locationWatcher = LocationWatcher.from(context);
        final Location location = locationWatcher.getLocation(true);
        if (location == null) {
            Log.d(TAG, "Not updating because Location is null.");
            RoomsContract.notifyChange(context);
            locationWatcher.doUpdateRoomsOnLocation();
            if (locationWatcher.didEnableProvider()) {
                locationWatcher.adjustLocationUpdates();
            }
            return;
        }

        // Decide if the Rooms should be synced with the Server.

        // If we're not connected, enable the connectivity receiver.
        // There's no point trying to poll the server for updates if we're not connected,
        // and the connectivity receiver will turn the location-based updates back on
        // once we have a connection.
        if (!DeviceStatusHelper.isConnected(context)) {
            Log.d(TAG, "Not updating because device is not connected.");
            PackageManager pacMan = getPackageManager();
            pacMan.setComponentEnabledSetting(
                    new ComponentName(this, ConnectivityChangedReceiver.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
            return;
        }

        // If we are connected check to see if this is a forced update (typically triggered
        // when the location downloadIfNeeded changed).
        boolean doServerSync = intent.getBooleanExtra(Constants.KEY_FORCE_REFRESH, false);
//        Log.d(TAG, "Forced update: " + doServerSync);

        // If it's not a forced update (for example from the Activity being restarted) then
        // check to see if we've moved far enough, or there's been a long enough delay since
        // the last update and if so, enforce a new update.
        if (!doServerSync) {

            final PreferenceHelper preferences = PreferenceHelper.from(context);
            final boolean isInBackground;
            isInBackground = preferences.isInBackground();

            if (!SessionMainManager.getInstance().didLogin()) {
                // If the app is in the background and not in a Session
                // and the Preferences are set to not update in the background,
                // don't do anything.
                if (isInBackground && !preferences.doFollowBackground()) return;

                // Check if we're in a low battery situation in which forced updates are handled.
                IntentFilter batIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent battery = registerReceiver(null, batIntentFilter);
                if (getIsLowBattery(battery)) {
                    Log.d(TAG, "Not updating because battery is low.");
                    return;
                }
            }

            final Location lastLocation = preferences.getLastUpdateLocation();

            // If update time and distance bounds have been passed, do an update.
            final long maxTimeDelta =
                    isInBackground ? UPDATE_TIME_INTERVAL_MILLIS : UPDATE_TIME_INTERVAL_MILLIS / 2L;
            final boolean isOldUpdate =
                    lastLocation.getTime() < (System.currentTimeMillis() - maxTimeDelta);
            final float locationDelta =
                    lastLocation.distanceTo(location);
            final boolean isDistantUpdate =
                    locationDelta > (isInBackground ? UPDATE_DISTANCE_MAX : UPDATE_DISTANCE);

            if (isOldUpdate) {
                final String updateDate = new Date(lastLocation.getTime()).toString();
                Log.d(TAG, "Updating because last update was old. " + updateDate);
            }
            if (isDistantUpdate) {
                Log.d(TAG, "Updating because last update was " + locationDelta + "away.");
            }

            doServerSync = (isOldUpdate || isDistantUpdate);
        }


        if (doServerSync) {
            // Send a Broadcast, so that Activities may display a Progress.
            sendBroadcast(new Intent(Constants.ACTION_ROOM_LIST_UPDATE));
            APIHelper.getRooms(context);
            Log.d(TAG, "Updating from server.");
        } else {
            // Update the distances of all known (local) Rooms.
            final Cursor roomsCursor = getContentResolver().query(
                    RoomsContentProvider.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );

            boolean didChangeDistance = false;
            while (roomsCursor.moveToNext()) {
                final Room room = RoomsContract.buildRoom(roomsCursor);
                if (room != null) {
                    final int newDistance = locationWatcher.getUpdatedDistanceToRoom(room);
                    final int deltaDistance = room.getDistance() - newDistance;
                    if (deltaDistance < -20 || deltaDistance > 20) {
                        didChangeDistance = true;
                        room.setDistance(newDistance);
                        RoomsContract.insertRoom(context, room);
                    }
                }
            }

            if (didChangeDistance) RoomsContract.notifyChange(context);
        }
    }

    /**
     * Returns battery status. True if less than 10% remaining.
     *
     * @param battery Battery Intent
     * @return Battery is low
     */
    protected boolean getIsLowBattery(Intent battery) {
        final float batteryLevel =
                (float) battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 1) /
                        battery.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        return batteryLevel < 0.2f;
    }
}