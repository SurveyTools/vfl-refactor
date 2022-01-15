package org.surveytools.flightlogger;

import org.surveytools.flightlogger.geo.GPSUtils;
import org.surveytools.flightlogger.geo.GPSUtils.DistanceUnit;

public class AltitudeDatum extends FlightDatum {

	protected float mRawAltitudeInMeters; // raw float value
	protected DistanceUnit	mDisplayUnits;
	protected boolean mDataOutOfRange;
	protected long mLastGoodAltitudeTimestamp;
	protected float mLastGoodAltitudeDatum;

	static final String INVALID_ALTITUDE_STRING = "--";
	static final String IGNORE_ALTITUDE_STRING = "";
	static final String OUT_OF_RANGE_ALTITUDE_STRING = "RNG"; // OUT_OF_RANGE_METRICS
	static final String DEMO_ALTITUDE_STRING = "299"; // DEMO_MODE

	static final long SHOW_OUT_OF_RANGE_AFTER_MILLIS = DATA_IS_OLD_THRESHOLD_MILLIS;

	public AltitudeDatum(boolean ignore, boolean demoMode) {
		super(ignore, demoMode);
	}

	// copy constructor
	public AltitudeDatum(AltitudeDatum srcDatum) {
		super(srcDatum);
		mRawAltitudeInMeters = srcDatum.mRawAltitudeInMeters;
		mDisplayUnits = srcDatum.mDisplayUnits;
	}

	// calculate the text based on all available data
	protected String calcDisplayAltitudeFromRaw(float rawAltitudeInMeters, boolean validData, boolean outOfRange) {
		// convert. do units here too
		if (mIgnore) {
			// ignore data
			return IGNORE_ALTITUDE_STRING;
		} else if (validData && !dataIsOld()) {
			// good data
			int altitudeInDisplayUnits = (int) GPSUtils.convertMetersToDistanceUnits(rawAltitudeInMeters, mDisplayUnits);
			return Integer.toString(altitudeInDisplayUnits);
		} else if (dataIsOutOfRange() && beenOutOfRangeForAwhile()) {
			return OUT_OF_RANGE_ALTITUDE_STRING; 
		} else {
			// bad data
			return INVALID_ALTITUDE_STRING;
		}
	}

	@Override
	public void reset() {
		super.reset();
		setRawAltitudeInMeters(0, false, false, curDataTimestamp(), 0, 0);
	}

	public void setDisplayUnits(DistanceUnit displayUnits) {
		mDisplayUnits = displayUnits;
		reset();
	}
	
	public boolean dataIsOutOfRange() {
		return mDataOutOfRange;
	}
	
	protected boolean beenOutOfRangeForAwhile() {
		if ((mLastGoodAltitudeTimestamp != 0) && (mLastGoodAltitudeDatum != 0) && ((mDataTimestamp - mLastGoodAltitudeTimestamp) > SHOW_OUT_OF_RANGE_AFTER_MILLIS))
			return true;
			
		// default
		return false;
	}

	@Override
	public short getStatusColor() {
		if (mDemoMode)
			return FLIGHT_STATUS_RED; // DEMO_MODE
		else if (mIgnore)
			return FLIGHT_STATUS_IGNORE;
		else if (!mDataIsValid)
			return FLIGHT_STATUS_RED;
		else if (dataIsExpired())
			return FLIGHT_STATUS_RED;// ALT_RED_ONLY_WHEN_DISCONNECTED (expired)
		else if (dataIsOld())
			return FLIGHT_STATUS_YELLOW;
		else
			return FLIGHT_STATUS_GREEN;
	}
	
	public boolean showWarningTextColor() {
		if (dataIsOutOfRange() && !dataIsExpired())
			return true;
		else
			return false;
	}
	
	public boolean dataIsOutOfRangeAndBeenThatWayForAwhile() {
		return dataIsOutOfRange() && beenOutOfRangeForAwhile();
	}
	
	public boolean showOutOfRangeText() {
		if (!mDataIsValid || dataIsExpired())
			return false; // has to come first
		else if (dataIsOutOfRangeAndBeenThatWayForAwhile())
			return true; 
		else
			return false;
	}
	
	// use the calculated text, unless we've changed to ignore mode or expired
	public String getAltitudeDisplayText() {
		if (mIgnore)
			return IGNORE_ALTITUDE_STRING;
		else if (mDemoMode)
			return DEMO_ALTITUDE_STRING; // DEMO_MODE
		else if (!mDataIsValid || dataIsExpired())
			return INVALID_ALTITUDE_STRING;
		else if (dataIsOutOfRangeAndBeenThatWayForAwhile())
			return OUT_OF_RANGE_ALTITUDE_STRING; 
		else
			return mValueToDisplay;
	}

	public double getAltitudeInDistanceUnits(DistanceUnit units) {
		// TESTING ILS_BAR_DEBUGGING if (true) return 350f;
		return GPSUtils.convertMetersToDistanceUnits(mRawAltitudeInMeters, units);
	}

	public boolean setRawAltitudeInMeters(float rawAltitudeInMeters, boolean validData, boolean outOfRange, long timestamp, float lastGoodAltitudeDatum, long lastGoodAltitudeTimestamp) {

		// snapshot cur data
		final String oldAltitudeDisplayValue = (mValueToDisplay == null) ? new String() : new String(mValueToDisplay);
		final boolean oldAltitudeDataValid = mDataIsValid;
		final boolean oldDataOld = dataIsOld();
		final boolean oldDataExpired = dataIsExpired();
		final boolean oldDataOutOfRange = mDataOutOfRange;
		final int oldStatusColor = getStatusColor();

		// update our data
		mRawAltitudeInMeters = rawAltitudeInMeters;
		mDataOutOfRange = outOfRange; // note: out of range data is considered valid
		mDataTimestamp = timestamp; // this is how 'expired' is driven
		mDataIsValid = validData && !dataIsExpired();// invalidate the data if we're expired
		mValueToDisplay = calcDisplayAltitudeFromRaw(mRawAltitudeInMeters, mDataIsValid, mDataOutOfRange);
		mLastGoodAltitudeDatum = lastGoodAltitudeDatum;
		mLastGoodAltitudeTimestamp = lastGoodAltitudeTimestamp;

		// see if anything changed - always check the value (since it might
		// change from a number to an ignore value)
		boolean somethingChanged = mValueToDisplay.equals(oldAltitudeDisplayValue); // value

		// MEC_TODO
		if (!mIgnore) {
			somethingChanged |= dataIsOld() != oldDataOld;
			somethingChanged |= dataIsExpired() != oldDataExpired;
			somethingChanged |= getStatusColor() != oldStatusColor;
			somethingChanged |= mDataOutOfRange != oldDataOutOfRange;
			somethingChanged |= (mDataIsValid != oldAltitudeDataValid);
		}

		// update the ui if anything change
		return somethingChanged;
	}
}
