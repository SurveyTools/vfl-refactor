package org.surveytools.flightlogger.util;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;

public class EditTextPreferenceShowSummary extends EditTextPreference {

	protected String mSummaryPrefix;
	protected String mSummarySuffix;

	public EditTextPreferenceShowSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public EditTextPreferenceShowSummary(Context context) {
		super(context);
		init();
	}
	
	public void setSummaryPrefix(String prefix) {
		mSummaryPrefix = prefix;
	}
	
	public void setSummarySuffix(String suffix) {
		mSummarySuffix = suffix;
	}
	
	public String getDisplayText() {
		return getText();
	}

	private String calcSummaryText() {
		String txt = getDisplayText();
		
		if (txt != null) {

			if ((mSummaryPrefix != null) && !mSummaryPrefix.equals(""))
				txt = mSummaryPrefix + " " + txt;

			if ((mSummarySuffix != null) && !mSummarySuffix.equals(""))
				txt += " " + mSummarySuffix;
		}
		
		return txt;
	}

	protected void init() {

		setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference arg0, Object arg1) {
				arg0.setSummary(calcSummaryText());
				return true;
			}
		});
	}

	@Override
	public CharSequence getSummary() {
		return calcSummaryText();
	}
	
	// TODO_EVAL onSetInitialValue
}
