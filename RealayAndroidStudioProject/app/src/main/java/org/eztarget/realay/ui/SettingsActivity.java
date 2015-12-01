package org.eztarget.realay.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import org.eztarget.realay.R;

/**
 * Created by michel on 05/01/15.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
