package org.surveytools.flightlogger.util;

import org.surveytools.flightlogger.altimeter.AltimeterService;
import org.surveytools.flightlogger.altimeter.AltimeterUtils;
import org.surveytools.flightlogger.geo.GPSUtils;

import android.content.Context;
import android.util.Log;

public class ResourceUtils {

	private static final String TAG = "ResourceUtils";

	public static String getResourceString(Context context, int rsrcID) {
		String str = null;
		try {
			str =  context.getResources().getString(rsrcID);
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading rsrc string for \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return str;
	}

	public static int getResourceIntegerFromString(Context context, int rsrcID) {
		int intValue = -1;
		try {
			String str =  context.getResources().getString(rsrcID);
			intValue = Integer.valueOf(str);
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading rsrc int for \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return intValue;
	}
	
	public static float getResourceFloatFromString(Context context, int rsrcID) {
		float floatValue = -1;
		try {
			String str =  context.getResources().getString(rsrcID);
			floatValue = Float.valueOf(str);
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading rsrc int for \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return floatValue;
	}
	
	public static boolean getResourceBooleanFromString(Context context, int rsrcID) {
		boolean v = false;
		try {
			String str =  context.getResources().getString(rsrcID);
			v = Boolean.valueOf(str);
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading rsrc boolean for \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return v;
	}
	
	public static GPSUtils.TransectParsingMethod getResourceTransectParsingMethod(Context context, int rsrcID) {
		GPSUtils.TransectParsingMethod value = GPSUtils.TransectParsingMethod.USE_DEFAULT; // APP_SETTINGS_WIP

		try {
			value = GPSUtils.getTransectParsingMethodForKey(context.getResources().getString(rsrcID));
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading tpm rsrc \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return value;
	}
		
	public static GPSUtils.DistanceUnit getResourceDistanceUnits(Context context, int rsrcID) {
		GPSUtils.DistanceUnit value = GPSUtils.DistanceUnit.MILES; // APP_SETTINGS_WIP

		try {
			value = GPSUtils.getDistanceUnitForKey(context.getResources().getString(rsrcID));
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading distance unit rsrc \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return value;
	}
		
	public static GPSUtils.VelocityUnit getResourceVelocityUnits(Context context, int rsrcID) {
		GPSUtils.VelocityUnit value = GPSUtils.VelocityUnit.NAUTICAL_MILES_PER_HOUR; // APP_SETTINGS_WIP

		try {
			value = GPSUtils.getVelocityUnitForKey(context.getResources().getString(rsrcID));
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading speed unit rsrc \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return value;
	}

	public static GPSUtils.DataAveragingMethod getResourceDataAveragingMethod(Context context, int rsrcID) {
		GPSUtils.DataAveragingMethod value = GPSUtils.DataAveragingMethod.MEDIAN; // APP_SETTINGS_WIP

		try {
			value = GPSUtils.getDataAveragingMethodForKey(context.getResources().getString(rsrcID));
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading dam rsrc \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return value;
	}
		
	public static GPSUtils.DataAveragingWindow getResourceDataAveragingWindow(Context context, int rsrcID) {
		GPSUtils.DataAveragingWindow value = GPSUtils.DataAveragingWindow.N_3_SAMPLES; // APP_SETTINGS_WIP

		try {
			value = GPSUtils.getDataAveragingWindowForKey(context.getResources().getString(rsrcID));
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading daw  rsrc \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return value;
	}
	
	public static AltimeterService.RangefinderDriverType getResourceRangefinderDriverType(Context context, int rsrcID) {
		AltimeterService.RangefinderDriverType value = AltimeterService.RangefinderDriverType.AGLASER;

		try {
			value = AltimeterUtils.getRangefinderDriverForKey(context.getResources().getString(rsrcID));
		} catch(Exception e) {
			// failed
			Log.e(TAG, "error loading tpm rsrc \"" + rsrcID + "\" (" + e.getLocalizedMessage() + ")");
		}
		
		return value;
	}
}
