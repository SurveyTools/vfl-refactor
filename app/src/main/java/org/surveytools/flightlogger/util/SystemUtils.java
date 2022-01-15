package org.surveytools.flightlogger.util;

import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class SystemUtils {

	public static String getVersionString(ContextWrapper contextWrapper) {
		
		try {
			PackageInfo pInfo = contextWrapper.getPackageManager().getPackageInfo(contextWrapper.getPackageName(), 0);
			return (pInfo == null) ? null : pInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e("SystemUtils", e.getLocalizedMessage());
		}
		
		// default
		return null;
	}

}
