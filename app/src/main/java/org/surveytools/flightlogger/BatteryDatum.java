package org.surveytools.flightlogger;

import android.content.Intent;
import android.os.BatteryManager;

public class BatteryDatum extends FlightDatum {

	protected float mRawBatteryLevel; // raw float value
	protected boolean mChargingOrFull;
	protected boolean mSlowCharging;
	protected boolean mFastCharging;

	static final String INVALID_BATTERY_STRING = "";
	static final String IGNORE_BATTERY_STRING = "";

	public BatteryDatum(boolean ignore, boolean demoMode) {
		super(ignore, demoMode);
	}

	protected String calcDisplayBatteryFromRaw(float rawBatteryLevel, boolean validData) {
		// convert. do units here too
		if (mIgnore) {
			// ignore data
			return IGNORE_BATTERY_STRING;
		} else if (validData) {
			// good data -- eval
			// float to int
			int intPercentValue = (int) (rawBatteryLevel * 100.0f);

			// int to string
			return Integer.toString(intPercentValue);
		} else {
			// bad data
			return INVALID_BATTERY_STRING;
		}
	}

	@Override
	public void reset() {
		super.reset();

		setRawBatteryLevel(0, false, curDataTimestamp());

		mChargingOrFull = false;
		mSlowCharging = false;
		mFastCharging = false;
	}

	@Override
	public short getStatusColor() {
		short color = FLIGHT_STATUS_UNKNOWN;

		// note: no old/expire here
		// note: DEMO_MODE ignored
		if (mIgnore)
			color = FLIGHT_STATUS_IGNORE;
		else if (mDemoMode)
			color = FLIGHT_STATUS_YELLOW;
		else if (!mDataIsValid)
			color = FLIGHT_STATUS_RED;
		else {
			color = FLIGHT_STATUS_GREEN;

			// override for low levels
			if (mRawBatteryLevel < .15f)
				color = FLIGHT_STATUS_RED;
			else if (mRawBatteryLevel < .25f)
				color = FLIGHT_STATUS_YELLOW;

			// alt -- also use this for charging status. for now this is in the Box status item. override for plugged-in
		}

		return color;
	}

	protected boolean setRawBatteryLevel(float rawBatteryValue, boolean validData, long timestamp) {
		// snapshot cur data
		final String oldBatteryDisplayValue = mValueToDisplay;
		final boolean oldBatteryDataValid = mDataIsValid;

		// TESTING rawBatteryValue = .12f;

		// update our data
		mRawBatteryLevel = rawBatteryValue;
		mDataIsValid = validData;
		mDataTimestamp = timestamp;
		mValueToDisplay = calcDisplayBatteryFromRaw(rawBatteryValue, validData);

		// see if anything changed
		boolean somethingChanged = false;
		somethingChanged |= mValueToDisplay.equals(oldBatteryDisplayValue); // value

		if (!mIgnore)
			somethingChanged |= (mDataIsValid != oldBatteryDataValid);

		// update the ui if anything change
		return somethingChanged;
	}

	public boolean updateBatteryStatus(Intent batteryStatus) {

		// TESTING return setRawBatteryLevel(.75f, true, curDataTimestamp());

		// extract info
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

		// level
		float batteryPct = level / (float) scale;
		setRawBatteryLevel(batteryPct, true, curDataTimestamp());

		// charging info
		mChargingOrFull = ((status == BatteryManager.BATTERY_STATUS_CHARGING) || (status == BatteryManager.BATTERY_STATUS_FULL));
		mSlowCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
		mFastCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

		return true;
	}
}
