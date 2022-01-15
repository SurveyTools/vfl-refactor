package org.surveytools.flightlogger.geo.data;

import java.io.File;

import org.surveytools.flightlogger.CourseInfoIntent;
import org.surveytools.flightlogger.geo.GPSUtils;
import org.surveytools.flightlogger.geo.GPSUtils.TransectParsingMethod;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Transect defines the data necessary to identify a transectpath
 * @author jayl
 *
 */
public class Transect implements Parcelable{
	public String mId;
	public String mName;
	public Location mStartWaypt;
	public Location mEndWaypt;
	public FlightStatus status; // not used yet
	
	
	public Transect(){
		// need a default constructor
  }
	
	public Transect(Parcel source){
        mId = source.readString();
        mName = source.readString();
        mStartWaypt = Location.CREATOR.createFromParcel(source);
        mEndWaypt = Location.CREATOR.createFromParcel(source);
        status = (FlightStatus)source.readSerializable();
  }
	
	public Transect(Location start, Location end, String gpxFilename, String routeName, int index) {
		mStartWaypt = start;
		mEndWaypt = end;
		mId = String.format("%s.%s.%s-%s", gpxFilename, routeName, start.getProvider(), end.getProvider());
		mName = "Transect " + index;
	}
	
	static public Transect newTransect(CourseInfoIntent data, TransectParsingMethod parsingMethod) {
		
		if ((data != null) && data.hasFile()) {
			File gpxFile = new File(data.mGpxName);
			
			if (gpxFile != null) {
				// find the route
				Route theRoute = GPSUtils.findRouteInFile(data.mRouteName, gpxFile);
				
				if (theRoute != null) {
					// find the transect
					Transect trans = GPSUtils.findTransectInRoute(data.mTransectName, theRoute, parsingMethod);
					
					return trans;
				}
			}
		}
		
		// something went wrong!
		return null;
	}
	
	public boolean isValid() {
		return (mStartWaypt != null) && (mEndWaypt != null);
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mId);
		dest.writeString(mName);
		this.mStartWaypt.writeToParcel(dest, 0);
		this.mEndWaypt.writeToParcel(dest,  0);
		dest.writeSerializable(status);	
	}
	
	public static final Parcelable.Creator<Transect> CREATOR = new Parcelable.Creator<Transect>() {

		@Override
		public Transect createFromParcel(Parcel source) {
			return new Transect(source);
		}

		@Override
		public Transect[] newArray(int size) {
			return new Transect[size];
		}    
    };

	// convenience method for UI display when using ArrayAdapters
	public String toString() {
		// e.g. "Transect 1 (T03_S ~ T03_N)"
		return getFullName();
	}
	
	public String getDetailsName() {
		// e.g. "T03_S ~ T03_N"
		return calcDetailsName(mStartWaypt.getProvider(), mEndWaypt.getProvider());
	}
	
	public String getFullName() {
		// e.g. "Transect 1 (T03_S ~ T03_N)"
		return calcFullName(mName, mStartWaypt.getProvider(), mEndWaypt.getProvider());
	}
	
	public boolean matchesByName(String targetName) {
		if ((mName != null) && (targetName != null))
			return mName.matches(targetName);
		return false;
	}

	public boolean matchesByDetailsName(String targetName) {
		String detailsName = getDetailsName();
		if ((detailsName != null) && (targetName != null))
			return detailsName.matches(targetName);
		return false;
	}

	// "T03_S ~ T03_N"
	static public String calcDetailsName(String waypointName1, String waypointName2) {
		String detailsName = null;
		
		if ((waypointName1 != null) || (waypointName2 != null)) {
			detailsName = new String();
			boolean didFirstWaypoint = false;

			if (!waypointName1.isEmpty()) {
				detailsName += waypointName1;
				didFirstWaypoint = true;
			}
			
			if (!waypointName2.isEmpty()) {
				if (didFirstWaypoint)
					detailsName += " ~ ";
					
				detailsName += waypointName2;
			}
		}
					
		return detailsName;
	}
	
	// e.g. "Transect 1 (T03_S ~ T03_N)"
	static public String calcFullName(String baseName, String transectDetails) {
		if (baseName != null) {
			String fullName = new String(baseName);

			if ((transectDetails != null) && !transectDetails.isEmpty()) {
				return fullName += " (" + transectDetails + ")";
			}
		}
					
		// no dice
		return null;
	}
	
	// e.g. "Transect 1 (T03_S ~ T03_N)"
	static public String calcFullName(String baseName, String waypointName1, String waypointName2) {
		return calcFullName(baseName, calcDetailsName(waypointName1, waypointName2));
	}
	
	public String calcBaseFilename() {
		return mId;
	}
}
