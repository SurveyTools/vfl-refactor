package org.surveytools.flightlogger;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;


public class AppSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // safe to call every time with false on the end
		//PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);

		// Load the preferences from an XML resource
       // addPreferencesFromResource(R.xml.preferences);
    }
    
    public void fakeInvalidate() {
    	// what a royal pain -- tried everything.  
    	setPreferenceScreen(null);
    	//addPreferencesFromResource(R.xml.preferences);
    }
}
