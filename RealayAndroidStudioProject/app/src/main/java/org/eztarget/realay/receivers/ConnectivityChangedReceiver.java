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

package org.eztarget.realay.receivers;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.eztarget.realay.managers.SessionMainManager;
import org.eztarget.realay.services.ActionSendService;
import org.eztarget.realay.services.RoomListUpdateService;

/**
 * This Receiver class is designed to listen for changes in connectivity.
 * <p/>
 * When we lose connectivity the relevant Service classes will automatically
 * disable passive Location updates and queue pending checkins.
 * <p/>
 * This class will restart the  insertAction service to retry pending checkins
 * and re-enables passive location updates.
 */
public class ConnectivityChangedReceiver extends BroadcastReceiver {

//    private static final String TAG = ConnectivityChangedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager conMan;
        conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check if we are connected to an active data network.
        final NetworkInfo activeNetworkInfo = conMan.getActiveNetworkInfo();

        if (activeNetworkInfo == null) {
//            Log.w(TAG, "ConnectivityManager.getActiveNetworkInfo() return null.");
            return;
        }

        if (!activeNetworkInfo.isConnectedOrConnecting()) {
//            Log.d(TAG, "NetworkInfo.isConnectedOrConnecting() is false.");
            return;
        }

//        Log.v(TAG, "isConnected");

        final PackageManager packageManager = context.getPackageManager();

        final ComponentName connectivityReceiver =
                new ComponentName(context, ConnectivityChangedReceiver.class);
//        final ComponentName locationReceiver =
//                new ComponentName(context, LocationChangedReceiver.class);

        // The default state for this Receiver is disabled. it is only
        // enabled when a Service disables updates pending connectivity.
        packageManager.setComponentEnabledSetting(
                connectivityReceiver,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
        );

//        // The default state for the Location Receiver is enabled. it is only
//        // disabled when a Service disables updates pending connectivity.
//        packageManager.setComponentEnabledSetting(
//                locationReceiver,
//                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
//                PackageManager.DONT_KILL_APP
//        );

//        // The default state for the passive Location Receiver is enabled. it is only
//        // disabled when a Service disables updates pending connectivity.
//        packageManager.setComponentEnabledSetting(
//                passiveLocationReceiver,
//                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
//                PackageManager.DONT_KILL_APP
//        );

        if (SessionMainManager.getInstance().didLogin()) {
            context.startService(new Intent(context, ActionSendService.class));
        }
        context.startService(new Intent(context, RoomListUpdateService.class));
    }
}