package org.eztarget.realay.ui.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import java.util.List;

/**
 * Created by michel on 14/02/15.
 */
public class IntentFactory {

    /**
     * Finds the appropriate Activity that displays the Location Service settings on this device
     * and starts it
     */
    public static void startLocationSettingsActivity(Context activityContext) {
        if (activityContext == null) return;
        final Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        List<ResolveInfo> list = activityContext.getPackageManager().queryIntentActivities(settings, 0);

        if (list.size() < 1) settings.setAction(Settings.ACTION_SECURITY_SETTINGS);

        activityContext.startActivity(settings);
    }
}
