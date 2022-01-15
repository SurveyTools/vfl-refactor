package org.surveytools.flightlogger;

import java.text.NumberFormat;

import android.view.View;

import org.surveytools.flightlogger.AltitudeDatum;
import org.surveytools.flightlogger.geo.GPSUtils;
import org.surveytools.flightlogger.geo.GPSUtils.DistanceUnit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Typeface;

public class TransectILSView extends View {

	public enum GraphScaleType {
	    LINEAR, LOG
	}

	private boolean mShowDebugInfo;
	private boolean mDemoMode = false; // TODO integrate with DEMO_MODE in FlightLogger

	// altitude
	private AltitudeDatum mCurAltitude;
	private float mAltitudeTargetFeet; // e.g. 300 feet
	private float mAltitudeDialRadiusFeet; // e.g. +/- 20 feet
	private float mAltitudeDeltaInFeet;
	private float mAltitudeDeltaNormalized;

	// render snapshot to check for diffs
	private boolean mRenderedAtitudeDataIsValid;
	private boolean mRenderedAtitudeDataIsOutOfRange;
	private boolean mRenderedAtitudeDataIsOld;
	private boolean mRenderedAtitudeDataIsExpired;
	private float mRenderedAltitudeDeltaNormalized;

	// path
	private GPSDatum mCurGpsData;
	private float mTransectDialRadiusFeet; // e.g. +/- 20 feet
	private float mTransectDeltaNormalized;

	// colors
	private int mMarkerColorNormal;
	private int mMarkerColorWarning;
	private int mMarkerColorError;

	// drawing
	private Paint mPaint;
	private RectF mOvalH;
	private RectF mOvalV;
	private Path mCircleClip;
	private float mCircleClipRadius;
	private float mCircleClipX;
	private float mCircleClipY;
	
	private GraphScaleType mAltitudeGraphType = GraphScaleType.LINEAR;
	private GraphScaleType mNavigationGraphType = GraphScaleType.LINEAR;
	private DistanceUnit mDisplayUnits = DistanceUnit.FEET;
	private String mDisplayUnitsString = "";

	// for xml construction
	public TransectILSView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupVars();
	}

	// for code construction
	public TransectILSView(final Context context) {
		super(context);
		setupVars();
	}
	
	public void setDisplayUnits(DistanceUnit displayUnits) {
		mDisplayUnits = displayUnits;
		
		// EVAL_CENTRALIZE?
		switch(displayUnits) {
		case FEET:
			mDisplayUnitsString = "ft";
			break;
			
		case METERS:
			mDisplayUnitsString = "m";
			break;

		case KILOMETERS:
		case MILES:
		case NAUTICAL_MILES:
		default:
			mDisplayUnitsString = "";
			break;
		}
		
		reset();
	}

	public void updateSettings(AppSettings prefs) {
		if (prefs != null) {
			mShowDebugInfo = prefs.mPrefShowDebug;
			mAltitudeTargetFeet = prefs.mPrefAltitudeTargetFeet;
			mAltitudeDialRadiusFeet = prefs.mPrefAltitudeRadiusFeet;
			mTransectDialRadiusFeet = prefs.mPrefNavigationRadiusFeet;
			
			// force the dependent variables to update
			updateAltitude(mCurAltitude);
			updateGps(mCurGpsData);
		}
		
		invalidate();
	}

	private void setupVars() {
		
		// override with updateSettings
		mShowDebugInfo = false;
		mAltitudeTargetFeet = 300;
		mAltitudeDialRadiusFeet = 100;
		mTransectDialRadiusFeet = 200;

		// DEMO_MODE option
		// TESTING mAltitudeTargetFeet = 3; mAltitudeDialRadiusFeet = 2;
		// TODO_ILS_REVAMP - externalize

		mCircleClipRadius = 0;
		mCircleClipX = 0;
		mCircleClipY = 0;
		this.mCircleClip = new Path();

		this.mPaint = new Paint();
		this.mOvalH = new RectF(0, 0, 0, 0);
		this.mOvalV = new RectF(0, 0, 0, 0);

		mMarkerColorNormal = getResources().getColor(R.color.nav_ips_guides);
		mMarkerColorWarning = getResources().getColor(R.color.nav_ips_guides_warning);
		mMarkerColorError = getResources().getColor(R.color.nav_ips_guides_warning);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// save w & h here if need be
	}

	@Override
	protected void onDraw(Canvas canvas) {

		float w = getWidth();
		float h = getWidth();
		float centerX = (float) w / 2;
		float centerY = (float) h / 2;
		float centerSize = w / 30;
		float markerStrokeWidth = 24; // TODO_ILS_REVAMP (wider)
		float hashSize = markerStrokeWidth * 2f;
		float hashW = hashSize / 4f; // TODO_ILS_REVAMP (2 for testing)
		float hashFullW = hashW * 2f;
		float markerLen = w * 0.66f;
		float innerStrokeWidth = 3;
		float outerStrokeWidth = 6;
		float strokePad = outerStrokeWidth / 2;
		float adjW = w - strokePad;
		float adjH = h - strokePad;
		float pixelRadius = w / 2.0f;
		float contentPixelRadius = pixelRadius - outerStrokeWidth;
		float errorPixelRadius = contentPixelRadius - markerStrokeWidth; // 15 is perfect, 18 gives you a little gap which is good
		float warningPixelRadius = errorPixelRadius * .85f; // 15 is perfect, 18 gives you a little gap which is good

		//
		// ILS_BAR_DEBUGGINGTESTING
		boolean debugAlwaysShowBars = false;
		boolean debugOverrideValues = false;;
		float debugRange = 300;
		float debugNormalizedValue = -.25f;// -.2f;//0.6f;

		// DEMO_MODE
		if (mDemoMode)
			debugAlwaysShowBars = true;
		
		// TESTING Log.i("navView", "draw w = " + w + ", getWidth = " + ww +
		// ", mw = " + width + ", pl = " + pl);

		// TODO_ILS_REVAMP - efficiency
		mPaint.setColor(getResources().getColor(R.color.nav_ips_axis));
		mPaint.setStyle(Paint.Style.STROKE);

		// axison
		mPaint.setStrokeWidth(innerStrokeWidth);
		canvas.drawLine(strokePad, centerY, adjW, centerY, mPaint);
		canvas.drawLine(centerX, strokePad, centerX, adjH, mPaint);

		// circles
		mPaint.setColor(getResources().getColor(R.color.nav_ips_circle));

		// inside circle
		mPaint.setStrokeWidth(innerStrokeWidth);
		canvas.drawCircle(centerX, centerY, centerSize, mPaint);

		// outside circle
		mPaint.setStrokeWidth(outerStrokeWidth);
		canvas.drawCircle(centerX, centerY, centerX - (strokePad * 2), mPaint);

		// horizontal notches
		mPaint.setColor(getResources().getColor(R.color.nav_ips_notches));
		mPaint.setStrokeWidth(1);
		mPaint.setStyle(Paint.Style.FILL);

		float xl = centerX;
		float xr = centerX;
		float yy0 = centerY - (hashSize / 2);
		float yy1 = centerY + (hashSize / 2);
		int numHashes = 6;
		float delta = centerX / numHashes;

		mOvalH.left = 0;
		mOvalH.right = hashFullW;
		mOvalH.top = yy0;
		mOvalH.bottom = yy1;

		int numNavHashes = 5;
		float feetPerHash = mTransectDialRadiusFeet / (numNavHashes + 1); // 40
		float curFeet = feetPerHash; // start with the first hashmark

		for (int i = 0; i < numNavHashes; i++) {
			float navValueNormalized = 0;

			switch(mNavigationGraphType) {
			
			case LOG:
				// TODO
				navValueNormalized = (curFeet / mTransectDialRadiusFeet);
				break;
				
			case LINEAR:
				navValueNormalized = (curFeet / mTransectDialRadiusFeet);
				break;
			}

			float pixelXOffset = pixelRadius * navValueNormalized;
			xl = centerX + pixelXOffset;
			xr = centerX - pixelXOffset;

			// left
			mOvalH.offsetTo(xl - hashW, yy0);
			canvas.drawOval(mOvalH, mPaint);

			// right
			mOvalH.offsetTo(xr - hashW, yy0);
			canvas.drawOval(mOvalH, mPaint);
			
			//navValueNormalized += navDelta;
			curFeet += feetPerHash;
		}
		
		// vertical notches

		mPaint.setColor(getResources().getColor(R.color.nav_ips_notches));
		mPaint.setStrokeWidth(1);
		mPaint.setStyle(Paint.Style.FILL);

		float y1 = centerY;
		float y2 = centerY;
		float vHashWidth = hashSize;
		float vHashHeight = hashSize / 2;
		float vHashHalfHeight = vHashHeight / 2;
		float vHashLeft = centerX - (vHashWidth / 2);

		mOvalV.left = 0;
		mOvalV.right = vHashWidth;
		mOvalV.top = 0;
		mOvalV.bottom = vHashHeight;

		for (int i = 0; i < numHashes - 1; i++) {
			y1 -= delta;
			y2 += delta;

			// top
			mOvalV.offsetTo(vHashLeft, y1 - vHashHalfHeight);
			canvas.drawOval(mOvalV, mPaint);

			// bottom
			mOvalV.offsetTo(vHashLeft, y2 - vHashHalfHeight);
			canvas.drawOval(mOvalV, mPaint);
		}

		mPaint.setStyle(Paint.Style.STROKE);

		// markers
		mPaint.setColor(getResources().getColor(R.color.nav_ips_guides));
		mPaint.setStrokeWidth(markerStrokeWidth);

		// set up the circular clipping
		// note that this is not supported in hardware acceleraiton

		// set up the clip (if need be)
		float clipRadius = contentPixelRadius - 4;
		if ((centerX != mCircleClipX) || (centerY != mCircleClipY) || (mCircleClipRadius != clipRadius)) {
			// update the clip
			mCircleClipRadius = clipRadius;
			mCircleClipX = centerX;
			mCircleClipY = centerY;
			mCircleClip.reset();
			mCircleClip.addCircle(mCircleClipX, mCircleClipY, mCircleClipRadius, Direction.CW);
		}

		// clip
		int markerColor = mMarkerColorNormal;
		int canvasStateRef = canvas.save();
		canvas.clipPath(mCircleClip);

		float verticalMarkerX = centerX;
		float transectDeltaNormalized = 0;
		
		// vertical guide marker |
		if (debugAlwaysShowBars || ((mCurGpsData != null) && mCurGpsData.mDataIsValid && mCurGpsData.mCrossTrackDataIsValid && !mCurGpsData.mIgnore && !mCurGpsData.dataIsExpired())) {

			transectDeltaNormalized = debugOverrideValues ? debugNormalizedValue : mTransectDeltaNormalized;
			float pixelHDelta = pixelRadius * transectDeltaNormalized;

			markerColor = mMarkerColorNormal;

			// validate
			if (pixelHDelta < -errorPixelRadius) {
				// pegged
				pixelHDelta = -errorPixelRadius;
				markerColor = mMarkerColorError;
			} else if (pixelHDelta < -warningPixelRadius) {
				// warning
				markerColor = mMarkerColorWarning;
			} else if (pixelHDelta > errorPixelRadius) {
				// pegged
				pixelHDelta = errorPixelRadius;
				markerColor = mMarkerColorError;
			} else if (pixelHDelta > warningPixelRadius) {
				// warning
				markerColor = mMarkerColorWarning;
			}

			// change it to yellow if the data is old
			if ((markerColor == mMarkerColorNormal) && mCurGpsData.dataIsOld()) {
				// optional markerColor = mMarkerColorWarning;
			}

			verticalMarkerX = centerX + pixelHDelta;
			float verticalMarkerY1 = centerY - (markerLen / 2);
			float verticalMarkerY2 = centerY + (markerLen / 2);

			mPaint.setColor(markerColor);
			mPaint.setStrokeWidth(markerStrokeWidth);
			canvas.drawLine(verticalMarkerX, verticalMarkerY1, verticalMarkerX, verticalMarkerY2, mPaint);
		}

		// snapshot for alter diffs
		if (mCurAltitude != null) {
			mRenderedAtitudeDataIsValid = mCurAltitude.mDataIsValid;
			mRenderedAtitudeDataIsOutOfRange = mCurAltitude.dataIsOutOfRange();
			mRenderedAtitudeDataIsOld = mCurAltitude.dataIsOld();
			mRenderedAtitudeDataIsExpired = mCurAltitude.dataIsExpired();
			mRenderedAltitudeDeltaNormalized = mAltitudeDeltaNormalized;

		} else {
			mRenderedAtitudeDataIsValid = false;
			mRenderedAtitudeDataIsOutOfRange = false;
			mRenderedAtitudeDataIsOld = false;
			mRenderedAtitudeDataIsExpired = false;
			mRenderedAltitudeDeltaNormalized = 0;

		}

		// OUT_OF_RANGE_METRICS note: dataIsOutOfRangeAndBeenThatWayForAwhile not useful, which means this isn't getting called with the update.
		if (debugAlwaysShowBars || ((mCurAltitude != null) && mCurAltitude.mDataIsValid && !mCurAltitude.mIgnore && !mCurAltitude.dataIsExpired() && !mCurAltitude.dataIsOutOfRangeAndBeenThatWayForAwhile())) {

			float altitudeDeltaNormalized = debugOverrideValues ? debugNormalizedValue : mAltitudeDeltaNormalized;
			float pixelVDelta = pixelRadius * altitudeDeltaNormalized;
			markerColor = mMarkerColorNormal;

			// validate
			if (pixelVDelta < -errorPixelRadius) {
				// pegged
				pixelVDelta = -errorPixelRadius;
				markerColor = mMarkerColorError;
			} else if (pixelVDelta < -warningPixelRadius) {
				// warning
				markerColor = mMarkerColorWarning;
			} else if (pixelVDelta > errorPixelRadius) {
				// pegged
				pixelVDelta = errorPixelRadius;
				markerColor = mMarkerColorError;
			} else if (pixelVDelta > warningPixelRadius) {
				// warning
				markerColor = mMarkerColorWarning;
			}

			// change it to yellow if the data is old
			if ((markerColor == mMarkerColorNormal) && mCurAltitude.dataIsOld()) {
				// optional markerColor = mMarkerColorWarning;
			}

			float yPos = centerY + pixelVDelta;
			float horizMarkerX1 = centerX - (markerLen / 2);
			float horizMarkerX2 = centerX + (markerLen / 2);

			mPaint.setColor(markerColor);
			mPaint.setStrokeWidth(markerStrokeWidth);
			canvas.drawLine(horizMarkerX1, yPos, horizMarkerX2, yPos, mPaint);

			// NUMERICAL DEBUG DISPLAY (on the line)
			if (mShowDebugInfo) {
				// show the numerical date (under the line, pinned to the bottom)

				// debug/verbose color is the same as the line in most cases
				int textColor = Color.BLACK;
				float textX = centerX;
				float textY = yPos + (markerStrokeWidth / 2) - 3;

				mPaint.setColor(textColor);
				mPaint.setTextSize(24);
				mPaint.setStyle(Style.FILL);
				mPaint.setStrokeWidth(2);
				mPaint.setTextAlign(Align.CENTER);

				int feet = (int) mAltitudeDeltaInFeet;
				String navDeltaString = null;

				if (debugOverrideValues)
					feet = (int) (debugRange * altitudeDeltaNormalized);

				if (feet < 0) {
					navDeltaString = "+" + NumberFormat.getIntegerInstance().format(Math.abs(feet)) + " ft";
					canvas.drawText(navDeltaString, textX, textY, mPaint);
				} else if (feet > 0) {
					navDeltaString = "-" + NumberFormat.getIntegerInstance().format(Math.abs(feet)) + " ft";
					canvas.drawText(navDeltaString, textX, textY, mPaint);
				}
			}
		}

		// NUMERICAL DEBUG DISPLAY (to the left or right of the line)
		// this is done on the outside so that it overlaps
		if (mShowDebugInfo) {
			// show the numerical date (under the line, pinned to the bottom)

			// debug/verbose color is the same as the line in most cases
			int textColor = Color.BLACK;
			float textY = centerY;
			float textX = verticalMarkerX + (markerStrokeWidth / 2) - 5;

			mPaint.setColor(textColor);
			mPaint.setTextSize(24);
			mPaint.setStyle(Style.FILL);
			mPaint.setStrokeWidth(2);
			mPaint.setTextAlign(Align.CENTER);
			mPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

			canvas.save();
			canvas.rotate(-90, textX, textY);

			int feet = (int) mCurGpsData.getTransectDeltaDistanceUnits(DistanceUnit.FEET);
			String navDeltaString = null;

			if (debugOverrideValues)
				feet = (int) (debugRange * transectDeltaNormalized);

			int displayDistance = (int) GPSUtils.convertMetersToDistanceUnits(GPSUtils.feetToMeters((float)feet), mDisplayUnits);
			
			if (transectDeltaNormalized > 0) {
				navDeltaString = NumberFormat.getIntegerInstance().format(Math.abs(displayDistance)) + " " + mDisplayUnitsString;
				canvas.drawText(navDeltaString, textX, textY, mPaint);
			} else if (transectDeltaNormalized < 0) {
				navDeltaString = NumberFormat.getIntegerInstance().format(Math.abs(displayDistance)) + " " + mDisplayUnitsString;
				canvas.drawText(navDeltaString, textX, textY, mPaint);
			}
			canvas.restore();
		}

		/*
		 * TESTING the clip: RectF bigRect = new RectF(-10, -10, w + 20, h + 20); mPaint.setStyle(Paint.Style.FILL); canvas.drawRect(bigRect, mPaint);
		 */

		// restore
		canvas.restoreToCount(canvasStateRef);
	}

	protected boolean updateAltitude(AltitudeDatum altitudeData) {

		// snapshot
		// float oldAtitudeDeltaNormalized = mAltitudeDeltaNormalized;

		if (altitudeData != null) {

			// always accept and copy the data
			mCurAltitude = new AltitudeDatum(altitudeData);

			if (altitudeData.mDataIsValid) {

				// physical delta
				float altitudeInFeet = (float) altitudeData.getAltitudeInDistanceUnits(DistanceUnit.FEET);
				mAltitudeDeltaInFeet = altitudeInFeet - mAltitudeTargetFeet;

				// normalized delta
				mAltitudeDeltaNormalized = mAltitudeDeltaInFeet / mAltitudeDialRadiusFeet;

			} else if (altitudeData.mDemoMode) {
				// DEMO_MODE
				mAltitudeDeltaNormalized = -0.4f;
			}
		} else {
			mCurAltitude = null;
		}

		// see if we agree with what's on screen

		boolean somethingChanged = false;

		if (mCurAltitude != null) {
			somethingChanged |= mRenderedAtitudeDataIsValid != mCurAltitude.mDataIsValid;
			somethingChanged |= mRenderedAtitudeDataIsOutOfRange != mCurAltitude.dataIsOutOfRange();
			somethingChanged |= mRenderedAtitudeDataIsOld != mCurAltitude.dataIsOld();
			somethingChanged |= mRenderedAtitudeDataIsExpired != mCurAltitude.dataIsExpired();
			somethingChanged |= mRenderedAltitudeDeltaNormalized != mAltitudeDeltaNormalized;
		}

		return somethingChanged;
	}

	protected boolean updateGps(GPSDatum gpsData) {

		// keep a ref
		mCurGpsData = gpsData;

		float oldTransectDeltaNormalized = mTransectDeltaNormalized;
		// TODO - old visible

		if (gpsData != null) {

			if (gpsData.mDataIsValid && gpsData.mCrossTrackDataIsValid) {

				// physical delta
				// BUG FIX note: the minus is so we show the direction TO instead of AT
				float pathDeviationInFeet = -(float)gpsData.getTransectDeltaDistanceUnits(DistanceUnit.FEET);

				// normalized delta
				mTransectDeltaNormalized = pathDeviationInFeet / mTransectDialRadiusFeet;
			}
			else if (gpsData.mDemoMode) {
				// DEMO_MODE
				mTransectDeltaNormalized = -0.29f;
			}

		}

		return mTransectDeltaNormalized != oldTransectDeltaNormalized;
	}

	public boolean update(AltitudeDatum atitudeData, GPSDatum gpsData) {
		boolean somethingChanged = false;

		somethingChanged |= updateAltitude(atitudeData);
		somethingChanged |= updateGps(gpsData);

		if (somethingChanged)
			invalidate();

		return somethingChanged;
	}

	public void reset() {
		// todo
	}
}
