package org.eztarget.realay.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by michel on 2015-02-23.
 *
 */
public class DeviceStatusHelper {

    private static final String TAG = DeviceStatusHelper.class.getSimpleName();

    public static boolean isConnected(Context context) {
        if (context == null) return false;

        final ConnectivityManager conMan;
        conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMan == null) return false;

        final NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

//    public static boolean isOnMobileData(Context context) {
//        if (context == null) return true;
//
//        final ConnectivityManager conMan;
//        conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (conMan == null) return true;
//
//        final NetworkInfo activeNetwork = conMan.getActiveNetworkInfo();
//        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
//    }

}
