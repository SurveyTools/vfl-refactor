package org.surveytools.flightlogger.util;

import org.surveytools.flightlogger.altimeter.LightwareSF03DataValidator;

public class ParseTester {

	/**
	 * @param args
	 */
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		final byte[] testInput = "  3.00 m\n".getBytes();
		
		LightwareSF03DataValidator dataValidator = new LightwareSF03DataValidator();
		float alt = dataValidator.parseDataPayload(testInput);
		System.out.println("The reading is" + alt);
	}

}
