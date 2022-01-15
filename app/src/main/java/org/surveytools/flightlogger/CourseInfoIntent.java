package org.surveytools.flightlogger;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;

import org.surveytools.flightlogger.geo.data.Transect;

public class CourseInfoIntent implements Parcelable {

	public String mGpxName;
	public String mRouteName;
	public String mTransectName;
	public String mTransectDetails;
	int mAction;

	private static final String LOGGER_TAG = "CourseInfoIntent";

	public CourseInfoIntent() {
	}

	public CourseInfoIntent(String gpxName, String routeName, String transectName, String transectDetails, int action) {

		mGpxName = gpxName;
		mRouteName = routeName;
		mTransectName = transectName;
		mTransectDetails = transectDetails;
		mAction = action;
	}

	// copy constructor
	public CourseInfoIntent(CourseInfoIntent srcData) {

		if (srcData != null) {
			mGpxName = srcData.mGpxName;
			mRouteName = srcData.mRouteName;
			mTransectName = srcData.mTransectName;
			mTransectDetails = srcData.mTransectDetails;
			mAction = srcData.mAction;
		} else {
			// TODO - defaults
			mGpxName = "";
			mRouteName = "";
			mTransectName = "";
			mAction = 0;
		}
	}
	
	public void setTransectName(String transectName, String transectDetailsName) {
		mTransectName = transectName;
		mTransectDetails = transectDetailsName;
	}

	public void setTransect(Transect transect) {
		if (transect == null) {
			setTransectName(null, null);
		} else {
			setTransectName(transect.mName, transect.getDetailsName());
		}
	}

	public void clearTransectData() {
		mTransectName = null;
		mTransectDetails = null;
	}

	public void clearRouteDataAndDependencies() {
		mRouteName = null;
		clearTransectData();
	}

	public void clearFileDataAndDependencies() {
		mGpxName = null;
		clearRouteDataAndDependencies();
	}

	public void clearAll() {
		clearFileDataAndDependencies();
		mAction = 0;
	}

	public boolean hasFile() {
		return (mGpxName != null) && !mGpxName.isEmpty();
	}

	public boolean hasRoute() {
		return (mRouteName != null) && !mRouteName.isEmpty();
	}

	public boolean hasTransect() {
		return (mTransectName != null) && !mTransectName.isEmpty();
	}

	public boolean hasTransectDetails() {
		return (mTransectDetails != null) && !mTransectDetails.isEmpty();
	}

	public boolean hasFileButNotEverythingElse() {
		// YELLOW
		// something's in there, possibly all
		return hasFile() && !(hasRoute() && hasTransect() && hasTransectDetails());
	}

	public boolean isFullyReady() {
		// GREEN
		// something's in there, possibly all
		return hasFile() && hasRoute() && hasTransect() && hasTransectDetails();
	}

	public boolean isFullyDefaulted() {
		return !hasFile() && !hasRoute() && !hasTransect() && !hasTransectDetails();
	}

	public String getShortFilename() {
		return FilenameUtils.getName(mGpxName);
	}

	public String getShortRouteName() {
		return mRouteName;
	}

	public String getShortTransectName() {
		return mTransectDetails;
	}

	public String getFullTransectName() {
		return Transect.calcFullName(mTransectName, mTransectDetails);
	}

	// parcel part
	public CourseInfoIntent(Parcel in) {
		String[] data = new String[5];

		in.readStringArray(data);
		this.mGpxName = data[0];
		this.mRouteName = data[1];
		this.mTransectName = data[2];
		this.mTransectDetails = data[3];
		this.mAction = Integer.parseInt(data[4]);
	}

	public void debugDump() {
		Log.d(LOGGER_TAG, "GPX: " + mGpxName);
		Log.d(LOGGER_TAG, "Route: " + mRouteName);
		Log.d(LOGGER_TAG, "Transect: " + mTransectName);
		Log.d(LOGGER_TAG, "Transect Details: " + mTransectDetails);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeStringArray(new String[] { this.mGpxName, this.mRouteName, this.mTransectName, this.mTransectDetails, String.valueOf(this.mAction) });
	}

	public static final Parcelable.Creator<CourseInfoIntent> CREATOR = new Parcelable.Creator<CourseInfoIntent>() {

		@Override
		public CourseInfoIntent createFromParcel(Parcel source) {
			return new CourseInfoIntent(source);
		}

		@Override
		public CourseInfoIntent[] newArray(int size) {
			return new CourseInfoIntent[size];
		}
	};

}