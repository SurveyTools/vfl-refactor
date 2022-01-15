package org.surveytools.flightlogger.logger;

public class LogEntry {
	double mLat;
	double mLon; 
	float mAlt;
	float mSpeed;
	double mGpsAlt;
	
	public LogEntry() 
	{
		this.mLat = 0;
		this.mLon = 0;
		this.mAlt = 0; // need to know the difference between true 0 and no response.
		this.mSpeed = 0;
		this.mGpsAlt = 0; 
	}

	public LogEntry(double currLat, double currLon, float currAlt, float currSpeed, float gpsAlt)
	{
		this.mLat = currLat;
		this.mLon = currLon;
		this.mAlt = currAlt;
		this.mSpeed = currSpeed;
		this.mGpsAlt = gpsAlt;
	}
	
	public void clearEntry()
	{
		this.mLat = 0;
		this.mLon = 0;
		this.mAlt = 0;
		this.mSpeed = 0;
		this.mGpsAlt = 0;		
	}
	
	// copy constructor to keep sampled data atomic
	public LogEntry(LogEntry cloned) 
	{
		this.mLat = cloned.mLat;
		this.mLon = cloned.mLon;
		this.mAlt = cloned.mAlt;
		this.mSpeed = cloned.mSpeed;
		this.mGpsAlt = cloned.mGpsAlt;
	}
	
	public boolean isValidEntry()
	{
		// don't want to log anything where the lat and lon are 0. 
		// this may be a problem if you are really 300 miles offshore in
		// the Gulf of Guinea in the Atlantic Ocean, where lat/lon is 0
		return !((this.mLat == 0.0f) && (this.mLon == 0.0f));
	}

}
