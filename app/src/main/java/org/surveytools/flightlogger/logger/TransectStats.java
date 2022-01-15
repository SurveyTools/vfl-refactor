package org.surveytools.flightlogger.logger;

import java.util.ArrayList;
import org.surveytools.flightlogger.geo.data.Transect;

public class TransectStats { 
	
	private ArrayList<TransectStat> mTransectStats;
	
	// what do we log for log entries 
	public enum LogFields {
		TIMESTAMP, LAT, LON, LASER_ALT, GPS_ALT, SPEED
	}

	public String mTransectName;
	public Transect mTransect;

	@SuppressWarnings("unused")
	private TransectStats()
	{
		// force the object to have a name
	}
	
	public TransectStats(String name, Transect transect)
	{
		mTransectName = name;
		mTransect = transect;
		mTransectStats = new ArrayList<TransectStat>();
	}
	
	public void addTransectStat(LogEntry entry)
	{
		TransectStat stat = new TransectStat(entry);
		mTransectStats.add(stat);
	}
	
	public TransectSummary getTransectSummary()
	{
		double laserAlt = 0.0f;
		double gpsAlt = 0.0f;
		double speed = 0.0f;
		
		for (TransectStat stat : mTransectStats)
		{
			laserAlt += stat.mLaserAlt;
			gpsAlt += stat.mGpsAlt;
			speed += stat.mAirspeed;
		}
		
		if(mTransectStats.size() > 0)
		{
			int size = mTransectStats.size();
			laserAlt = laserAlt/size;
			gpsAlt = gpsAlt/size;
			speed = speed/size;
		}
		
		TransectSummary summary = new TransectSummary(mTransect, mTransectName, (float)speed, (float)gpsAlt, (float)laserAlt);

		return summary;
	}

}
