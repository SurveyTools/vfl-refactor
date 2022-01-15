package org.surveytools.flightlogger.altimeter;

public interface AltitudeUpdateListener {
	
	public void onAltitudeUpdate(float altValueInMeters);
	
	public void onAltitudeError(String error);
	
	public void onConnectionEnabled();
	
	public void onConnectionDisabled();

}

