package org.surveytools.flightlogger.altimeter;

import java.lang.Float;
import java.lang.NumberFormatException;
import java.lang.String;
import java.util.Arrays;
// import android.util.Log;
import org.surveytools.flightlogger.util.KMPMatch;

/*
 * This validator is used for Revision 2 SF03 devices, which are the 'old' generation devices
 * built before 6/2015.
 */

public class LightwareSF03DataValidator implements AltimeterValidator {
	
	final static int dataSampleBufferSize = 100;
	final static int dataSampleSize = 10;
	final byte[] terminatingPattern = { (byte)0x20, (byte)0x6d, (byte)0x0d, (byte)0x0a };
    //final byte[] terminatingPattern = { (byte)0x20, (byte)0x6d, (byte)0x0d };

	@Override
	public float parseDataPayload(byte[] data) {
		float meters = 0;	
		// Note: when the laser starts up, it stream a buch of 0xff characters, typically
		// 30 bytes or so at a time. The serial library allocates 4K, which seems like a 
		// crazy large size for collecting serial data, but we extract a value from the 
		// beginning of the buffer upstream in the altimeter service. Later, we should 
		// probably extract a range of values, and average them.
		
		// find the values between the patterns. The value may have up to 2 spaces (0x20) padding
		int start = KMPMatch.indexOf(data, terminatingPattern);
		// first, see if we have exactly one frame of data
        if (start > 0 && data.length <= dataSampleSize)
        {
            meters = parseAltimeterSample(data, start);
        }
		// if we have a match, extract a sample from the next value in the sequence
		else if (start > 0 && (start + dataSampleSize + terminatingPattern.length) < data.length )
		{
			int fromIndex = start + terminatingPattern.length;
			int toIndex = fromIndex + dataSampleSize + terminatingPattern.length;
			try {
				byte[] sampleSlice = Arrays.copyOfRange(data, fromIndex, toIndex);
				int matchEnd = KMPMatch.indexOf(sampleSlice, terminatingPattern);
				if (matchEnd > 0)
				{
                    meters = parseAltimeterSample(sampleSlice, matchEnd);
				}
			} 
			catch (ArrayIndexOutOfBoundsException aioobe)
			{
				//TESTING Log.d("LightwareDataValidator.parseDataPayload", ">> array out of bounds - start: " + start);
			}
			// TODO - throw an IllegalValidationError, instead of an int
			catch (IllegalArgumentException iae)
			{
				//TESTING Log.d("LightwareDataValidator.parseDataPayload", ">> illegal argument fromIndex: " + fromIndex + " toIndex: " + toIndex);
			}
		}
		else {
			meters = AltimeterService.LASER_OUT_OF_RANGE;
		}
		return meters;
	}

    private float parseAltimeterSample(byte[] sampleSlice, int matchEnd) {
        float meters;
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
        return meters;
    }

}
