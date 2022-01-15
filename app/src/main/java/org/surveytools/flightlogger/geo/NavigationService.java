package org.surveytools.flightlogger.geo;

import java.util.ArrayList;
import java.util.Random;

import org.surveytools.flightlogger.geo.data.Transect;
import org.surveytools.flightlogger.geo.data.TransectStatus;
import org.surveytools.flightlogger.CourseInfoIntent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Provides navigation for transect paths. A work in progress
 * @author jayl
 */

public class NavigationService extends Service implements LocationListener {
	
	// determines whether to generate test data or live GPS fixes
	private boolean mUseMockData = false;
	
	// the minimum meter change before reporting an update, assuming the
	// MIN_TIME_BETWEEN_UPDATES threshold has been crossed
	private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0;
	
	// sample at 1 seconds
	private static final long MIN_TIME_BETWEEN_UPDATES = 1000 * 1;
	
	// how many track mock samples to create from a transect path
	private final int NUM_MOCK_TRACKS = 1000;
	
	// TODO - put these into a constants file
	public static final String USE_MOCK_DATA = "useMockData";
	public static final double METERS_NOT_AVAILABLE = -1f;
	
	private final String LOGGER_TAG = NavigationService.class.getSimpleName();

	private LocationManager mLocationManager;
	
	public boolean doNavigation = false;
	
	public Transect mCurrTransect;
	public CourseInfoIntent mCurrTransectDetails;
	private Location mCurrLoc; // last location received
	
	private final IBinder mBinder = new LocalBinder();
	private final ArrayList<TransectUpdateListener> mListeners
			= new ArrayList<TransectUpdateListener>();

	public class LocalBinder extends Binder {
        public NavigationService getService() {
            return NavigationService.this;
        }
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null)
		{
			// generate mock data if the intent calls for it
			boolean useMockData = intent.getBooleanExtra(USE_MOCK_DATA, false);
			setUseMockData(useMockData);
			if(useMockData)
			{
				mCurrTransect = buildMockTransect();
				mCurrTransectDetails = null;
				initMockGps();
			}
			else
			{
				initGps(MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES);
			}
		}
		Log.d(LOGGER_TAG, "starting navigation service");
		return START_STICKY;
	}
	
    public void registerListener(TransectUpdateListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(TransectUpdateListener listener) {
        mListeners.remove(listener);
    }

	private TransectStatus calcTransectStatus(Location currLoc) {
		// TODO validate before constructing TransectStatus
		double distance = 0;
		double crossTrackErr = 0;
		float currBearing = 0;
		float speed =  currLoc.getSpeed();
		
		if (mCurrTransect != null)
		{
			distance = currLoc.distanceTo(mCurrTransect.mEndWaypt);
			crossTrackErr = calcCrossTrackError(currLoc, mCurrTransect.mStartWaypt, mCurrTransect.mEndWaypt);
			currBearing = currLoc.bearingTo(mCurrTransect.mEndWaypt);
		}
		TransectStatus ts = new TransectStatus(mCurrTransect, distance, crossTrackErr,  currBearing, speed);
		ts.mCurrGpsLat = currLoc.getLatitude();
		ts.mCurrGpsLon = currLoc.getLongitude();
		ts.mCurrGpsAlt = currLoc.getAltitude();
		return ts;
	}
	
	private double calcCrossTrackError(Location curr, Location start, Location end)
	{
		double dist = Math.asin(Math.sin(start.distanceTo(curr)/GPSUtils.EARTH_RADIUS_METERS) * 
		         Math.sin(Math.toRadians(start.bearingTo(curr) - start.bearingTo(end)))) * GPSUtils.EARTH_RADIUS_METERS;
		
		return dist;
	}
    
    private void sendTransectUpdate(TransectStatus routeUpdate) {
        for (TransectUpdateListener listener : mListeners) {
        	listener.onRouteUpdate(routeUpdate);
        }
    }
    
    // TODO - Get a 'demo' state in place, and depending on that state, 
    // either init 'real' GPS or mock GPS
	public void startNavigation(Transect transect, CourseInfoIntent transectDetails) {
		mCurrTransect = transect;
		mCurrTransectDetails = transectDetails;
		doNavigation = true;
		initGps(MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES);
	}
	
	public void stopNavigation() {
		mCurrTransect = null;
		mCurrTransectDetails = null;
	}                  
	
	public boolean isNavigating() {
		return (mCurrTransect != null);
	}

	public double calcMetersToLocation(Location targetLoc) {
		if (isNavigating() && (mCurrLoc != null) && (targetLoc != null)) {
			return mCurrLoc.distanceTo(targetLoc);
		}
		
		// no dice
		return METERS_NOT_AVAILABLE;
	}

	public double calcMetersToStart() {
		return calcMetersToLocation(mCurrTransect.mStartWaypt);
	}

	public double calcMetersToEnd() {
		return calcMetersToLocation(mCurrTransect.mEndWaypt);
	}

	private void initGps(long millisBetweenUpdate, float minDistanceMoved) {
		doNavigation = true;
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// getting GPS statusMockTran
		boolean isGPSEnabled = mLocationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		if (isGPSEnabled) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, millisBetweenUpdate,
					minDistanceMoved, this);
			Log.d(LOGGER_TAG, "GPS service is available");
		} else {
			Log.d(LOGGER_TAG, "GPS service is not available");
		}
	}
	
	@SuppressWarnings("unused") // for now...
	private void initMockGps() {
		final long sleepTime = MIN_TIME_BETWEEN_UPDATES;
		doNavigation = true;
		
		mCurrTransect = buildMockTransect();
		mCurrTransectDetails = null;
		final double startPointLat = mCurrTransect.mStartWaypt.getLatitude();
		final double startPointLon = mCurrTransect.mStartWaypt.getLongitude();
		final double latDelta = (mCurrTransect.mEndWaypt.getLatitude() - mCurrTransect.mStartWaypt.getLatitude()) / NUM_MOCK_TRACKS;
		final double lonDelta = (mCurrTransect.mEndWaypt.getLongitude() - mCurrTransect.mStartWaypt.getLongitude()) / NUM_MOCK_TRACKS;
		final Random rand = new Random();
		
		new Thread()
		{
			int progressIndex = 0;		
			
		    public void run() {
		    	while(mUseMockData)
		    	{
			        progressIndex++;
			        Location loc = calcNewMockLocation(progressIndex);
			        try {
			        	onLocationChanged(loc);
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
		    }

			private Location calcNewMockLocation(int index) {
				float latJitter = rand.nextFloat() * (float)latDelta;
				float lonJitter = rand.nextFloat() * (float)lonDelta;
				Location newLoc = new Location("waypoint" + progressIndex);
		        newLoc.setSpeed(38 + rand.nextInt(10)); //41 meters, 80ish knots or so...
		        newLoc.setLatitude(startPointLat + latDelta * progressIndex + latJitter);
		        newLoc.setLongitude(startPointLon + lonDelta * progressIndex + lonJitter);
				return newLoc;
			}
		}.start();
		
	}

	// this is here until we put the menus back in
	private Transect buildMockTransect() {
		Location start = new Location("Front of 505");
		start.setLatitude(47.598383);
		start.setLongitude(-122.327537);
		
		Location end = new Location("NW 83rd & 14th NW");
		end.setLatitude(47.598383);
		end.setLongitude(-122.327537);
		
		Transect transect = new Transect();
		transect.mName = "My Test Transect";
		transect.mStartWaypt = start;
		transect.mEndWaypt = end;
		
		return transect;
	}

	/**
	 * Location callbacks
	 */
	@Override
	public void onLocationChanged(Location currLoc) {
		if (doNavigation)
		{
			mCurrLoc = currLoc;
			TransectStatus stat = calcTransectStatus(currLoc);
			sendTransectUpdate(stat);
		}
	}
	
	@Override
	public void onProviderDisabled(String arg0) {
		Log.d(LOGGER_TAG, "GPS Disbled");

	}

	@Override
	public void onProviderEnabled(String arg0) {
		Log.d(LOGGER_TAG, "GPS Enabled");

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		Log.d(LOGGER_TAG, "GPS stus change: " + arg0);

	}

	public void setUseMockData(boolean mUseMockData) {
		this.mUseMockData = mUseMockData;
	}

}
