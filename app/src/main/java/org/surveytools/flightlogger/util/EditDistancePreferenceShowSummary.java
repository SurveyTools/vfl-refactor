package org.surveytools.flightlogger.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.surveytools.flightlogger.altimeter.AltimeterService;
import org.surveytools.flightlogger.geo.GPSUtils;
import org.surveytools.flightlogger.geo.GPSUtils.DistanceUnit;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.AttributeSet;
import android.util.Log;

public class EditDistancePreferenceShowSummary extends EditTextPreferenceShowSummary {

	private DistanceUnit mPrimaryUnits;
	private DistanceUnit mSuffixUnits;
	private NumberFormat mSuffixNumberFormatter;
	
	private static final String TAG = EditDistancePreferenceShowSummary.class.getSimpleName();

	public EditDistancePreferenceShowSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditDistancePreferenceShowSummary(Context context) {
		super(context);
	}
	
	@Override
	protected void init() {
		super.init();
		// round to the nearest 1/10th
		mSuffixNumberFormatter = new DecimalFormat("#0.0");
	}

	public void setSummarySuffix(String suffix) {
		// just to be clear
		mSummarySuffix = null;
	}

	public void setUnits(DistanceUnit primaryUnits, DistanceUnit suffixUnits) {
		mPrimaryUnits = primaryUnits;
		mSuffixUnits = suffixUnits;
	}
	
	protected String getUnitsLabel(DistanceUnit units) {
		
		switch (units) {
			case FEET: return "feet";
			case METERS:return "meters";
			case KILOMETERS:return "kilometers";
			case MILES:return "miles";
			case NAUTICAL_MILES:return "nautical miles";
		}
		
		Log.e(TAG, "unrecognized unit (" + units + ")");
		return "";
	}

	@Override
	public String getDisplayText() {
		String rawValue = getText();
		String prefixAndValueText = super.getDisplayText() + " " + getUnitsLabel(mPrimaryUnits);
		String suffixText = "";
		
		try {
			double primaryValue = Integer.valueOf(rawValue);
			double suffixValue = GPSUtils.convertDistanceUnits(primaryValue, mPrimaryUnits, mSuffixUnits);
			suffixText = " (" + mSuffixNumberFormatter.format(suffixValue) + " " + getUnitsLabel(mSuffixUnits) + ")";
		} catch(Exception e)
		{
			Log.e(TAG, "bad integer (" + e.getLocalizedMessage() + ")");
			suffixText = " (ERROR)";
		}
		
		return prefixAndValueText + " " + suffixText;
	}
}
