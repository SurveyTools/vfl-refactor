package org.surveytools.flightlogger;

import org.surveytools.flightlogger.altimeter.AltimeterService.RangefinderDriverType;
import org.surveytools.flightlogger.geo.GPSUtils.*;
import org.surveytools.flightlogger.util.PreferenceUtils;
import org.surveytools.flightlogger.util.ResourceUtils;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.surveytools.flightlogger.R;

public class AppSettings {

	private ContextWrapper mContextWrapper;
	
	public boolean mPrefShowDebug;
	public float mPrefAltitudeTargetFeet; // e.g. 300, shown in mPrefAltitudeDisplayUnits, stored in PREF_ALT_NAV_STORAGE_UNITS (FEET)
	public float mPrefAltitudeRadiusFeet; // e.g. +/- 100', shown in mPrefAltitudeDisplayUnits, stored in PREF_ALT_NAV_STORAGE_UNITS (FEET)
	public float mPrefNavigationRadiusFeet; // e.g. +/- 200', shown in mPrefAltitudeDisplayUnits, stored in PREF_ALT_NAV_STORAGE_UNITS (FEET)
	public int mPrefUpdateFrequency;
	public boolean mUseCustomParsingMethod;
	public TransectParsingMethod mPrefTransectParsingMethod; // e.g. TransectParsingMethod.USE_DEFAULT
	public DistanceUnit mPrefDistanceDisplayUnits;
	public VelocityUnit mPrefSpeedDisplayUnits;
	public DistanceUnit mPrefAltitudeDisplayUnits; // display
	public RangefinderDriverType mRangefinderType; // laser type

	public boolean mDataAveragingEnabled;
	public DataAveragingMethod mDataAveragingMethod;
	public DataAveragingWindow mDataAveragingWindow;
	
	public static final DistanceUnit ALT_NAV_STORAGE_UNITS = DistanceUnit.FEET;

	private static final String LOGGER_TAG = "AppSettings";

	public static final String PREF_SHOW_DEBUG_KEY = "PREF_SHOW_DEBUG_KEY";
	public static final String PREF_ALTITUDE_TARGET_KEY = "PREF_ALTITUDE_TARGET_KEY";
	public static final String PREF_ALTITUDE_RADIUS_KEY = "PREF_ALTITUDE_RADIUS_KEY";
	public static final String PREF_NAVIGATION_RADIUS_KEY = "PREF_NAVIGATION_RADIUS_KEY";
	public static final String PREF_USE_CUSTOM_PARSING_METHOD_KEY = "PREF_USE_CUSTOM_PARSING_METHOD_KEY";
	public static final String PREF_TRANSECT_PARSING_METHOD_KEY = "PREF_TRANSECT_PARSING_METHOD_KEY";
	
	public static final String PREF_DATA_AVERAGING_ENABLED_KEY = "PREF_DATA_AVERAGING_ENABLED_KEY";
	public static final String PREF_DATA_AVERAGING_METHOD_KEY = "PREF_DATA_AVERAGING_METHOD_KEY";
	public static final String PREF_DATA_AVERAGING_WINDOW_KEY = "PREF_DATA_AVERAGING_WINDOW_KEY";
	
	// PREF_UNITS
	public static final String PREF_DISPLAY_UNITS_DISTANCE_KEY = "PREF_DISPLAY_UNITS_DISTANCE_KEY";
	public static final String PREF_DISPLAY_UNITS_SPEED_KEY = "PREF_DISPLAY_UNITS_SPEED_KEY";
	public static final String PREF_DISPLAY_UNITS_ALTITUDE_KEY = "PREF_DISPLAY_UNITS_ALTITUDE_KEY";
	public static final String PREF_RANGEFINDER_TYPE_KEY = "PREF_RANGEFINDER_TYPE_KEY";
	public static final String PREF_ALT_NAV_UNITS_STORAGE_KEY = "PREF_ALT_NAV_UNITS_STORAGE_KEY";
	public static final String PREF_LOGGING_FREQ_KEY = "PREF_LOGGING_FREQ_KEY";

	public AppSettings(ContextWrapper contextWrapper) {
		mContextWrapper = contextWrapper;
		refresh(contextWrapper);
	}

	// copy constructor
	public AppSettings(AppSettings srcData) {

		if (srcData != null) {
			mPrefShowDebug = srcData.mPrefShowDebug;
			mPrefAltitudeTargetFeet = srcData.mPrefAltitudeTargetFeet;
			mPrefNavigationRadiusFeet = srcData.mPrefNavigationRadiusFeet;
			mUseCustomParsingMethod = srcData.mUseCustomParsingMethod;
			mPrefTransectParsingMethod = srcData.mPrefTransectParsingMethod;
			mPrefDistanceDisplayUnits = srcData.mPrefDistanceDisplayUnits;
			mPrefSpeedDisplayUnits = srcData.mPrefSpeedDisplayUnits;
			mPrefAltitudeDisplayUnits = srcData.mPrefAltitudeDisplayUnits;
			mDataAveragingEnabled = srcData.mDataAveragingEnabled;
			mDataAveragingMethod = srcData.mDataAveragingMethod;
			mDataAveragingWindow = srcData.mDataAveragingWindow;
			mRangefinderType = srcData.mRangefinderType;

			mContextWrapper = srcData.mContextWrapper;
		} else {
			reset();
		}
	}

	public void debugDump() {
		Log.d(LOGGER_TAG, "mPrefShowDebug: " + mPrefShowDebug);
		Log.d(LOGGER_TAG, "mPrefAltitudeTargetFeet: " + mPrefAltitudeTargetFeet);
		Log.d(LOGGER_TAG, "mPrefAltitudeRadiusFeet: " + mPrefAltitudeRadiusFeet);
		Log.d(LOGGER_TAG, "mPrefNavigationRadiusFeet: " + mPrefNavigationRadiusFeet);
		Log.d(LOGGER_TAG, "mUseCustomParsingMethod: " + mUseCustomParsingMethod);
		Log.d(LOGGER_TAG, "mPrefTransectParsingMethod: " + mPrefTransectParsingMethod);
		Log.d(LOGGER_TAG, "mPrefDistanceDisplayUnits: " + mPrefDistanceDisplayUnits);
		Log.d(LOGGER_TAG, "mPrefSpeedDisplayUnits: " + mPrefSpeedDisplayUnits);
		Log.d(LOGGER_TAG, "mPrefAltitudeDisplayUnits: " + mPrefAltitudeDisplayUnits);
		Log.d(LOGGER_TAG, "mDataAveragingEnabled: " + mDataAveragingEnabled);
		Log.d(LOGGER_TAG, "mDataAveragingMethod: " + mDataAveragingMethod);
		Log.d(LOGGER_TAG, "mDataAveragingWindow: " + mDataAveragingWindow);
		Log.d(LOGGER_TAG, "mRangefinderType: " + mRangefinderType);
	}

	public void refresh(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		// TODO eval context.getSharedPreferences("userdetails", Context.MODE_PRIVATE)

		mPrefShowDebug = sharedPref.getBoolean(PREF_SHOW_DEBUG_KEY, ResourceUtils.getResourceBooleanFromString(context, R.string.pref_show_debug_default_value));

		mPrefAltitudeTargetFeet = PreferenceUtils.getSharedPrefStringAsInteger(sharedPref, PREF_ALTITUDE_TARGET_KEY, ResourceUtils.getResourceIntegerFromString(context, R.string.pref_altitude_target_default_value));
		mPrefAltitudeRadiusFeet = PreferenceUtils.getSharedPrefStringAsInteger(sharedPref, PREF_ALTITUDE_RADIUS_KEY, ResourceUtils.getResourceIntegerFromString(context, R.string.pref_altitude_radius_default_value));
		mPrefNavigationRadiusFeet = PreferenceUtils.getSharedPrefStringAsInteger(sharedPref, PREF_NAVIGATION_RADIUS_KEY, ResourceUtils.getResourceIntegerFromString(context, R.string.pref_navigation_radius_default_value));
		
		mUseCustomParsingMethod = sharedPref.getBoolean(PREF_USE_CUSTOM_PARSING_METHOD_KEY, ResourceUtils.getResourceBooleanFromString(context, R.string.pref_use_custom_transect_parsing_method_default_value));
		mPrefTransectParsingMethod = PreferenceUtils.getSharedPrefTransectParsingMethod(sharedPref, PREF_TRANSECT_PARSING_METHOD_KEY, ResourceUtils.getResourceTransectParsingMethod(context, R.string.pref_transect_parsing_method_default_value));
		
		mPrefDistanceDisplayUnits = PreferenceUtils.getSharedPrefDistanceUnits(sharedPref, PREF_DISPLAY_UNITS_DISTANCE_KEY, ResourceUtils.getResourceDistanceUnits(context, R.string.pref_distance_units_default_value));
		mPrefSpeedDisplayUnits = PreferenceUtils.getSharedPrefVelocityUnits(sharedPref, PREF_DISPLAY_UNITS_SPEED_KEY, ResourceUtils.getResourceVelocityUnits(context, R.string.pref_speed_units_default_value));
		mPrefAltitudeDisplayUnits = PreferenceUtils.getSharedPrefDistanceUnits(sharedPref, PREF_DISPLAY_UNITS_ALTITUDE_KEY, ResourceUtils.getResourceDistanceUnits(context, R.string.pref_altitude_units_default_value));
		mPrefAltitudeDisplayUnits = PreferenceUtils.getSharedPrefDistanceUnits(sharedPref, PREF_DISPLAY_UNITS_ALTITUDE_KEY, ResourceUtils.getResourceDistanceUnits(context, R.string.pref_altitude_units_default_value));

		mDataAveragingEnabled = sharedPref.getBoolean(PREF_DATA_AVERAGING_ENABLED_KEY, ResourceUtils.getResourceBooleanFromString(context, R.string.pref_use_custom_transect_parsing_method_default_value));
		mDataAveragingMethod = PreferenceUtils.getSharedPrefDataAveragingMethod(sharedPref, PREF_DATA_AVERAGING_METHOD_KEY, ResourceUtils.getResourceDataAveragingMethod(context, R.string.pref_data_averaging_method_default_value));
		mDataAveragingWindow = PreferenceUtils.getSharedPrefDataAveragingWindow(sharedPref, PREF_DATA_AVERAGING_WINDOW_KEY, ResourceUtils.getResourceDataAveragingWindow(context, R.string.pref_data_averaging_window_default_value));
		
		mRangefinderType = PreferenceUtils.getSharedPrefRangefinderType(sharedPref, PREF_RANGEFINDER_TYPE_KEY, ResourceUtils.getResourceRangefinderDriverType(context, R.string.pref_rangefinder_default_value));

		// TESTING 
		debugDump();
	}

	public void reset() {
		// TODO reset doesn't really work -- it's just for the memory copy (not the real prefs)
		mPrefShowDebug = ResourceUtils.getResourceBooleanFromString(mContextWrapper, R.string.pref_show_debug_default_value);

		mPrefAltitudeTargetFeet = ResourceUtils.getResourceIntegerFromString(mContextWrapper, R.string.pref_altitude_target_default_value);
		mPrefAltitudeRadiusFeet = ResourceUtils.getResourceIntegerFromString(mContextWrapper, R.string.pref_altitude_radius_default_value);
		mPrefNavigationRadiusFeet = ResourceUtils.getResourceIntegerFromString(mContextWrapper, R.string.pref_navigation_radius_default_value);
		
		mUseCustomParsingMethod = ResourceUtils.getResourceBooleanFromString(mContextWrapper, R.string.pref_use_custom_transect_parsing_method_default_value);
		mPrefTransectParsingMethod = ResourceUtils.getResourceTransectParsingMethod(mContextWrapper, R.string.pref_transect_parsing_method_default_value);

		mPrefDistanceDisplayUnits = ResourceUtils.getResourceDistanceUnits(mContextWrapper, R.string.pref_distance_units_default_value);
		mPrefSpeedDisplayUnits = ResourceUtils.getResourceVelocityUnits(mContextWrapper, R.string.pref_speed_units_default_value);
		mPrefAltitudeDisplayUnits = ResourceUtils.getResourceDistanceUnits(mContextWrapper, R.string.pref_altitude_units_default_value);

		mDataAveragingMethod = ResourceUtils.getResourceDataAveragingMethod(mContextWrapper, R.string.pref_data_averaging_method_default_value);
		mDataAveragingWindow = ResourceUtils.getResourceDataAveragingWindow(mContextWrapper, R.string.pref_data_averaging_window_default_value);
		
		mRangefinderType = ResourceUtils.getResourceRangefinderDriverType(mContextWrapper, R.string.pref_rangefinder_default_value);
	}
	
	public static TransectParsingMethod getPrefTransectParsingMethod(Context context) {
		// note: could honor mUseCustomParsingMethod
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return PreferenceUtils.getSharedPrefTransectParsingMethod(sharedPref, PREF_TRANSECT_PARSING_METHOD_KEY, ResourceUtils.getResourceTransectParsingMethod(context, R.string.pref_transect_parsing_method_default_value));
	}
	
	public static DistanceUnit getPrefDistanceDisplayUnit(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return PreferenceUtils.getSharedPrefDistanceUnits(sharedPref, PREF_DISPLAY_UNITS_DISTANCE_KEY, ResourceUtils.getResourceDistanceUnits(context, R.string.pref_distance_units_default_value));
	}
	
	public static VelocityUnit getPrefSpeedDisplayUnit(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return PreferenceUtils.getSharedPrefVelocityUnits(sharedPref, PREF_DISPLAY_UNITS_SPEED_KEY, ResourceUtils.getResourceVelocityUnits(context, R.string.pref_speed_units_default_value));
	}
	
	public static DistanceUnit getPrefAltitudeDisplayUnit(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return PreferenceUtils.getSharedPrefDistanceUnits(sharedPref, PREF_DISPLAY_UNITS_ALTITUDE_KEY, ResourceUtils.getResourceDistanceUnits(context, R.string.pref_altitude_units_default_value));
	}
	
	public static RangefinderDriverType getPrefRangefinderDriverType(Context context) {
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
		return PreferenceUtils.getSharedPrefRangefinderType(sharedPref, PREF_RANGEFINDER_TYPE_KEY, ResourceUtils.getResourceRangefinderDriverType(context, R.string.pref_rangefinder_default_value));
	}
	
	public static boolean getPrefDataAveragingEnabled(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREF_DATA_AVERAGING_ENABLED_KEY, ResourceUtils.getResourceBooleanFromString(context, R.string.pref_data_averaging_enabled_default_value));
	}

	public static boolean isPrefUseCustomTransectParsingKey(String key) {
		return PREF_USE_CUSTOM_PARSING_METHOD_KEY.equalsIgnoreCase(key);
	}
	
	public static boolean isPrefDisplayUnitsDistanceParsingKey(String key) {
		return PREF_DISPLAY_UNITS_DISTANCE_KEY.equalsIgnoreCase(key);
	}
	
	public static boolean isPrefDisplayUnitsSpeedParsingKey(String key) {
		return PREF_DISPLAY_UNITS_SPEED_KEY.equalsIgnoreCase(key);
	}
	
	public static boolean isPrefDisplayUnitsAltitudeParsingKey(String key) {
		return PREF_DISPLAY_UNITS_ALTITUDE_KEY.equalsIgnoreCase(key);
	}
	
	public static boolean isPrefDataAveragingEnabledKey(String key) {
		return PREF_DATA_AVERAGING_ENABLED_KEY.equalsIgnoreCase(key);
	}
	
	public static boolean getPrefUseCustomTransectParsing(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		return prefs.getBoolean(PREF_USE_CUSTOM_PARSING_METHOD_KEY, ResourceUtils.getResourceBooleanFromString(context, R.string.pref_show_debug_default_value));
	}

	public static boolean resetCustomTransectParsingMethodToDefault(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		TransectParsingMethod oldMethod = PreferenceUtils.getSharedPrefTransectParsingMethod(prefs, PREF_TRANSECT_PARSING_METHOD_KEY, ResourceUtils.getResourceTransectParsingMethod(context, R.string.pref_transect_parsing_method_default_value));
	
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PREF_TRANSECT_PARSING_METHOD_KEY, context.getResources().getString(R.string.pref_transect_parsing_method_default_value));
		editor.commit();

		TransectParsingMethod newMethod = PreferenceUtils.getSharedPrefTransectParsingMethod(prefs, PREF_TRANSECT_PARSING_METHOD_KEY, ResourceUtils.getResourceTransectParsingMethod(context, R.string.pref_transect_parsing_method_default_value));
		
		// return true if something changed
		return oldMethod != newMethod;
	}
	
	public static boolean resetDataAveragingDependencies(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		DataAveragingMethod oldDataAveragingMethod = PreferenceUtils.getSharedPrefDataAveragingMethod(prefs, PREF_DATA_AVERAGING_METHOD_KEY, ResourceUtils.getResourceDataAveragingMethod(context, R.string.pref_data_averaging_method_default_value));
		DataAveragingWindow oldDataAveragingWindow = PreferenceUtils.getSharedPrefDataAveragingWindow(prefs, PREF_DATA_AVERAGING_WINDOW_KEY, ResourceUtils.getResourceDataAveragingWindow(context, R.string.pref_data_averaging_window_default_value));
	
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PREF_DATA_AVERAGING_METHOD_KEY, context.getResources().getString(R.string.pref_data_averaging_method_default_value));
		editor.putString(PREF_DATA_AVERAGING_WINDOW_KEY, context.getResources().getString(R.string.pref_data_averaging_window_default_value));
		editor.commit();

		DataAveragingMethod newDataAveragingMethod = PreferenceUtils.getSharedPrefDataAveragingMethod(prefs, PREF_DATA_AVERAGING_METHOD_KEY, ResourceUtils.getResourceDataAveragingMethod(context, R.string.pref_data_averaging_method_default_value));
		DataAveragingWindow newDataAveragingWindow = PreferenceUtils.getSharedPrefDataAveragingWindow(prefs, PREF_DATA_AVERAGING_WINDOW_KEY, ResourceUtils.getResourceDataAveragingWindow(context, R.string.pref_data_averaging_window_default_value));
		
		// return true if something changed
		return (oldDataAveragingMethod != newDataAveragingMethod) || (oldDataAveragingWindow != newDataAveragingWindow);
	}
}
