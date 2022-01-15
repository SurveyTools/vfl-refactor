package org.surveytools.flightlogger.logger;

public class TransectStat {
	
	public float mAirspeed;
	public float mLaserAlt;
	public float mGpsAlt;
	
	@SuppressWarnings("unused")
	private TransectStat() { }
	
	public TransectStat(LogEntry entry)
	{
		mAirspeed = entry.mSpeed;
		mLaserAlt = entry.mAlt;
		mGpsAlt = (float)entry.mGpsAlt;
	}
}
