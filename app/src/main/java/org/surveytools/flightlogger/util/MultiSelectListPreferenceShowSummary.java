package org.surveytools.flightlogger.util;

import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;

public class MultiSelectListPreferenceShowSummary extends MultiSelectListPreference {

	private final static String TAG = MultiSelectListPreferenceShowSummary.class.getName();

	public MultiSelectListPreferenceShowSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MultiSelectListPreferenceShowSummary(Context context) {
		super(context);
		init();
	}

	private void init() {

		setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				arg0.setSummary(calcSelectedItemsCommaString());
				return true;
			}
		});
	}

    private boolean[] sneakyGetSelectedItems() {
        final CharSequence[] entryValues = getEntryValues();
        final int entryCount = entryValues.length;
        final Set<String> values = getValues();
        boolean[] result = new boolean[entryCount];
        
        for (int i = 0; i < entryCount; i++) {
            result[i] = values.contains(entryValues[i].toString());
        }
        
        return result;
    }

	private String calcSelectedItemsCommaString() {
		
		CharSequence[] entries = getEntries();
		CharSequence[] values = getEntryValues();
		Set<String> values2 = getValues();
		boolean[] selected = sneakyGetSelectedItems();
		
		// TESTING Log.i(TAG, values2.toString());
		String str = null;

		for(int i=0;i<values.length;i++) {
			// TESTING Log.i(TAG, entries[i].toString() + ": " + values[i].toString());
			if (selected[i]) {
				// append
				if (str == null)
					str = entries[i].toString();
				else
					str += ", " + entries[i].toString();
			}
		}
		
		return (str == null) ? "" : str; // a String is a CharSequence
	}

    @Override
	public CharSequence getSummary() {
		return calcSelectedItemsCommaString();
	}
}
