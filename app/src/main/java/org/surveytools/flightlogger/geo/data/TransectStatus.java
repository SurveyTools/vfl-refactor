package org.surveytools.flightlogger.geo.data;


public class TransectStatus {
	
	public TransectStatus( 
			Transect path, // TODO - this should be an id/guid, if possible
			double distance,
			double crossTrackErr, 
			float bearing, 
			float speed)
	{
		this.mTransect = path;
		this.mCrossTrackError = crossTrackErr;
		this.mDistanceToEnd = distance;
		this.mBearing = bearing;
		this.mGroundSpeed = speed;
	}
	
	public Transect mTransect;
	public double mCrossTrackError;
	public double mDistanceToEnd;
	public float mBearing;
	public float mGroundSpeed;
	public double mCurrGpsLat;
	public double mCurrGpsLon;
	public double mCurrGpsAlt;
	
	public boolean isTransectValid() {
		return mTransect != null;
	}

}
