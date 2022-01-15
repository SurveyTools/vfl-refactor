package org.surveytools.flightlogger.altimeter;

/*
 * A poorly named interface that encapsulates the differences between
 * AgLaser and LightWare data payload and settings
 */

public interface AltimeterValidator {
	
	public float parseDataPayload(byte[] data);	

}
