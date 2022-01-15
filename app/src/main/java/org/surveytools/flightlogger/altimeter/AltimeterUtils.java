package org.surveytools.flightlogger.altimeter;

import android.content.res.Resources.NotFoundException;

import org.surveytools.flightlogger.altimeter.AltimeterService.RangefinderDriverType;

public class AltimeterUtils {
	
	public static RangefinderDriverType getRangefinderDriverForKey(String key) 
	throws NotFoundException
	{
		if (key != null) {
			if (key.equalsIgnoreCase("rangefinder_aglaser"))
				return RangefinderDriverType.AGLASER;
			else if (key.equalsIgnoreCase("rangefinder_sf03xlr"))
				return RangefinderDriverType.LIGHTWARE;
			else if (key.equalsIgnoreCase("rangefinder_sf30xlr"))
				return RangefinderDriverType.LIGHTWARE_SF30;
		}
		
		throw new NotFoundException("driver key not found");
		
	}

}
