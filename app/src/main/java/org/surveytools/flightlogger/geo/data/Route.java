package org.surveytools.flightlogger.geo.data;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;

/**
 * A Route contains all the transect paths we find in a GPX file
 * 
 * @author jayl
 */

public class Route {
	
	public enum ParseType {
	    PARSE_ROUTE_POINT_PAIRS, PARSE_ROUTE_POINT_NAMES, PARSE_WAYPOINT_PAIRS, PARSE_WAYPOINT_NAMES
	}
	
	private static final String[] mParseDesc = {
		"Parsed route waypoint pairs", "Parsed named route waypoints", "Parsed waypoint pairs", "Parsed named waypoints"
	};
	
	public String gpxFile;
	public String mName;
	private String mParseErrorMsg;
	private ParseType mParseType;
	private boolean mHasParseError;
	public List<Location> mWayPoints;

	public Route() {
		mWayPoints = new ArrayList<Location>();
		mHasParseError = false;
	}

	public void addWayPoint(Location location) {
		mWayPoints.add(location);
	}
	
	public void setParseMethod(ParseType type)
	{
		mParseType = type;
	}
	
	public String getParseMethod()
	{
		int ord = mParseType.ordinal();
		return  mParseDesc[ord];
	}
	
	public boolean hasParseError()
	{
		return mHasParseError;
	}
	
	public void setParseErrorMsg(String errMsg)
	{
		mHasParseError = true;
		mParseErrorMsg = errMsg;
	}
	
	public String getParseErrorMsg()
	{
		return mParseErrorMsg;
	}

	// convenience method for UI display when using ArrayAdapters
	public String toString() {
		return mName;
	}
	
	public boolean matchesByName(String targetName) {
		if ((mName != null) && (targetName != null))
			return mName.matches(targetName);
		return false;
	}

}
