package org.surveytools.flightlogger.altimeter;

import java.util.Arrays;
import android.util.Log;
import org.surveytools.flightlogger.util.KMPMatch;

/*
 * This validator is used for the newer SF30XLR devices
 */

public class LightwareSF30DataValidator implements AltimeterValidator {
	
	final static int dataSampleBufferSize = 100;
	final static int dataSampleSize = 10;
	final byte[] terminatingPattern = { (byte)0x20, (byte)0x6d }; 
	final byte[] eolPattern = { (byte)0x0d, (byte)0x0a }; 

	@Override
	public float parseDataPayload(byte[] data) {
		float meters = 0;	
		
		// valid data starts immediately after CR/LF. Matchfind the values between the patterns. The value may have up to 2 spaces (0x20) padding
		int start = KMPMatch.indexOf(data, eolPattern);
		// if we have a match, extract a sample from the next value in the sequence
		if (start > 0)
		//0 && (start + dataSampleSize + terminatingPattern.length) < data.length )
		{
			int fromIndex = start + terminatingPattern.length;
			int toIndex = fromIndex + dataSampleSize + terminatingPattern.length;
			try {
				byte[] sampleSlice = Arrays.copyOfRange(data, fromIndex, toIndex);
				int matchEnd = KMPMatch.indexOf(sampleSlice, terminatingPattern);
				if (matchEnd > 0)
				{
					byte[] sample = Arrays.copyOfRange(sampleSlice, 0, matchEnd);
					//TESTING Log.d(this.getClass().getName(), "Meters as unparsed string: " + new String(sample));
					try {
						meters = Float.parseFloat(new String(sample));
					} catch (NumberFormatException nfe) { // for lightware lasers that report ---.--
						meters = AltimeterService.LASER_OUT_OF_RANGE;
					}
					//TESTING Log.d(this.getClass().getName(), "Meters as float: " + meters);
					if (meters < 0) // for lightware lasers that report -1.00 m
						meters = AltimeterService.LASER_OUT_OF_RANGE; 
				}	
			} 
			catch (ArrayIndexOutOfBoundsException aioobe)
			{
				//TESTING Log.d("LightwareDataValidator.parseDataPayload", ">> array out of bounds - start: " + start);
			}
			// TODO - throw an IllegalValidationError, instead of returning an int
			catch (IllegalArgumentException iae)
			{
				//TESTING Log.d("LightwareDataValidator.parseDataPayload", ">> illegal argument fromIndex: " + fromIndex + " toIndex: " + toIndex);
			}
		}
		else {
			//TESTING Log.d(this.getClass().getName(), "No match");
			meters = AltimeterService.LASER_OUT_OF_RANGE;
		}
		return meters;
	}

}
