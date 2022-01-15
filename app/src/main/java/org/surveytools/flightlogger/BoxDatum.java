package org.surveytools.flightlogger;

import android.content.Intent;
import android.os.BatteryManager;

public class BoxDatum extends FlightDatum {

	protected boolean mChargingOrFull;
	protected boolean mSlowCharging;
	protected boolean mFastCharging;

	static final String INVALID_BOX_STRING = "";
	static final String IGNORE_BOX_STRING = "";

	public BoxDatum(boolean ignore, boolean demoMode) {
		super(ignore, demoMode);
	}

	protected String calcDisplayBatteryFromRaw(float rawBatteryLevel, boolean validData) {
		// convert. do units here too
		if (mIgnore) {
			// ignore data
			return IGNORE_BOX_STRING;
		} else if (validData) {
			// good data -- eval
			// float to int
			int intPercentValue = (int) (rawBatteryLevel * 100.0f);

			// int to string
			return Integer.toString(intPercentValue);
		} else {
			// bad data
			return INVALID_BOX_STRING;
		}
	}

	@Override
	public void reset() {
		super.reset();

		setChargingState(false, false, false, curDataTimestamp());
	}

	@Override
	public short getStatusColor() {
		short color = FLIGHT_STATUS_UNKNOWN;

		// note: no old/expire here

		if (mIgnore)
			color = FLIGHT_STATUS_IGNORE;
		else if (mDemoMode)
			color = FLIGHT_STATUS_IGNORE;
		else if (mChargingOrFull)
			color = FLIGHT_STATUS_GREEN;
		else if (mSlowCharging || mFastCharging)
			color = FLIGHT_STATUS_GREEN;
		else
			color = FLIGHT_STATUS_RED;

		return color;
	}

	protected boolean setChargingState(boolean slowCharging, boolean fastCharging, boolean validData, long timestamp) {

		// snapshot cur data
		final boolean oldSlowCharging = mSlowCharging;
		final boolean oldFastCharging = mFastCharging;

		// update our data
		mSlowCharging = slowCharging;
		mFastCharging = fastCharging;
		mDataIsValid = validData;
		mDataTimestamp = timestamp;
		mValueToDisplay = ""; // ignored

		// see if anything changed
		boolean somethingChanged = false;

		if (!mIgnore) {
			somethingChanged |= (mSlowCharging != oldSlowCharging);
			somethingChanged |= (mFastCharging != oldFastCharging);
		}

		// update the ui if anything change
		return somethingChanged;
	}

	public boolean updateBoxWithBatteryStatus(Intent batteryStatus) {
		// extract info
		int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

		// charging info
		boolean slowCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
		boolean fastCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

		setChargingState(slowCharging, fastCharging, true, curDataTimestamp());

		return true;
	}
}
