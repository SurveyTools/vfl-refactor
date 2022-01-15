package org.surveytools.flightlogger.geo;

import org.surveytools.flightlogger.geo.data.TransectStatus;

public interface TransectUpdateListener {
	
	public void onRouteUpdate(TransectStatus status);

}
