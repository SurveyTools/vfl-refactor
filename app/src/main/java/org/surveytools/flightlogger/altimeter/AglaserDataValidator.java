package org.surveytools.flightlogger.altimeter;

import java.util.Arrays;
import slickdevlabs.apps.usb2seriallib.SlickUSB2Serial.BaudRate;

public class AglaserDataValidator implements AltimeterValidator {
	
	public float parseDataPayload(byte[] data)
	{
		float altMeters = 0.0f;
		
		boolean isValid = ((int) data[data.length - 1] == 13)
				&& (data.length == 10);
		if (isValid) {
			byte[] stripMeters = Arrays.copyOfRange(data, 0, data.length - 2);
			altMeters = Float.parseFloat(new String(stripMeters));
		}
		
		return altMeters;
	}
	
	public BaudRate getBaudRate()
	{
		return BaudRate.BAUD_9600;
	}

}
