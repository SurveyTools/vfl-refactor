package org.surveytools.flightlogger.util;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.util.AttributeSet;

public class ListPreferenceShowSummary extends ListPreference {

	public ListPreferenceShowSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ListPreferenceShowSummary(Context context) {
		super(context);
		init();
	}

	private void init() {

		setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				arg0.setSummary(getEntry());
				return true;
			}
		});
	}

	@Override
	public CharSequence getSummary() {
		return super.getEntry();
	}
}
