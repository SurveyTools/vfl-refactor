package org.surveytools.flightlogger;

import java.util.List;

import org.surveytools.flightlogger.geo.data.Transect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;

public class AllTransectsView extends TransectGraphView {

	// colors
	private int mNormalTransectColor;
	private int mActiveTransectColor;
	private int mNextTransectColor;

	// data
	private List<Transect> mTransectList;
	private Transect mActiveTransect;
	private Transect mNextTransect;

	protected final String TAG = this.getClass().getSimpleName();

	// for xml construction
	public AllTransectsView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// for code construction
	public AllTransectsView(final Context context) {
		super(context);
	}

	@Override
	protected void setupVars() {
		super.setupVars();
		
		mNormalTransectColor = getResources().getColor(R.color.transect_graph_normal);
		mActiveTransectColor = getResources().getColor(R.color.transect_graph_active);
		mNextTransectColor = getResources().getColor(R.color.transect_graph_next);
	}

	public void setTransectList(List<Transect> transectList, Transect activeTransect, Transect nextTransect) {
		mTransectList = transectList;
		mActiveTransect = activeTransect;
		mNextTransect = nextTransect;
		updateGpsBounds();
		invalidate();
	}

	@Override
	protected void updateGpsBounds() {

		if (mTransectList == null) {
			mMinLat = mMaxLat = mMinLong = mMaxLong = 0;
		} else {
			for(int i=0;i<mTransectList.size();i++) {
				Transect trans = mTransectList.get(i);
				
				if (i==0) {
					// first one
					mMinLat = Math.min(trans.mStartWaypt.getLatitude(), trans.mEndWaypt.getLatitude());
					mMaxLat = Math.max(trans.mStartWaypt.getLatitude(), trans.mEndWaypt.getLatitude());
					mMinLong = Math.min(trans.mStartWaypt.getLongitude(), trans.mEndWaypt.getLongitude());
					mMaxLong = Math.max(trans.mStartWaypt.getLongitude(), trans.mEndWaypt.getLongitude());
				} else {
					// check to see if the bounds are expanded
					mMinLat = Math.min(trans.mStartWaypt.getLatitude(), mMinLat);
					mMinLat = Math.min(trans.mEndWaypt.getLatitude(), mMinLat);
					mMaxLat = Math.max(trans.mStartWaypt.getLatitude(), mMaxLat);
					mMaxLat = Math.max(trans.mEndWaypt.getLatitude(), mMaxLat);

					mMinLong = Math.min(trans.mStartWaypt.getLongitude(), mMinLong);
					mMinLong = Math.min(trans.mEndWaypt.getLongitude(), mMinLong);
					mMaxLong = Math.max(trans.mStartWaypt.getLongitude(), mMaxLong);
					mMaxLong = Math.max(trans.mEndWaypt.getLongitude(), mMaxLong);
				}
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		// TESTING testArrows(canvas); return;

		float inset = 16;
		float w = getWidth()-(inset*2);
		float h = getHeight()-(inset*2);

		/*
		 	TESTING
		 	
			mPaint.setColor( getResources().getColor(R.color.debug2));
			mPaint.setStyle(Paint.Style.FILL);
			RectF rrrr = new RectF(0, 0, w, h);
			canvas.drawRect(rrrr, mPaint);

			mPaint.setColor( getResources().getColor(R.color.debug1));
			mPaint.setStyle(Paint.Style.FILL);
			RectF rrr = new RectF(1, 1, w-2, h-2);
			canvas.drawRect(rrr, mPaint);
		*/
		
		if ((mTransectList != null) && (mTransectList.size() > 1))
		{
			double latRange = Math.abs(mMaxLat - mMinLat); // south->north (0 at equator, 90 at north pole)
			double lonRange = Math.abs(mMaxLong - mMinLong); // east->west

			if ((latRange >= 0) && (lonRange >= 0)) {
				// e.g. 300px / 10 degrees = 30px/degree 
				double	hGpsToPixelScaler = (lonRange == 0) ? Double.MAX_VALUE : w / lonRange;
				double	vGpsToPixelScaler = (latRange == 0) ? Double.MAX_VALUE : h / latRange; 
				double	gpsToPixels;
				double	hPixelsUsed;
				double	vPixelsUsed;
				double xCenteringOffset; // actual
				double yCenteringOffset; // actual
				
				// see which direction needs to be squished more to fit into the window...
				if (vGpsToPixelScaler < hGpsToPixelScaler) {
					// tall
					// computer scaler
					gpsToPixels = vGpsToPixelScaler; // e.g. 300px / 10 degrees = 30px/degree 
					hPixelsUsed = lonRange * gpsToPixels;
					vPixelsUsed = h;
					
					// centering
					xCenteringOffset = inset + ((w - hPixelsUsed) / 2);
					yCenteringOffset = inset;
				} 
				else 
				{
					// wide
					// computer scaler
					gpsToPixels = hGpsToPixelScaler; // e.g. 300px / 10 degrees = 30px/degree 
					hPixelsUsed = w;
					vPixelsUsed = latRange * gpsToPixels;

					// centering
					xCenteringOffset = inset;
					yCenteringOffset = inset + ((h - vPixelsUsed) / 2);
				}
				// go from back to front so our start-marker overlays the other lines if need be
				
				// ALT (move the arrows around)
				// float range = .2f; // don't go end to end.
				// the max is so things are more tightly grouped when the number of transects is low
				// float arrowPositionDelta = range / (float) Math.max(mTransectList.size(), 8);
				// float arrowPosition = (1.0f - range) / 2.0f;
				
				boolean activeTransectFound = false;
				float activeTransectFromX = 0;
				float activeTransectFromY = 0;
				float activeTransectToX = 0;
				float activeTransectToY = 0;
		
				boolean nextTransectFound = false;
				float nextTransectFromX = 0;
				float nextTransectFromY = 0;
				float nextTransectToX = 0;
				float nextTransectToY = 0;
		
				for(int i=0;i<mTransectList.size();i++) 
				{
					Transect trans = mTransectList.get(i);
					boolean isActiveTransect = (trans == mActiveTransect);
					boolean isNextTransect = (trans == mNextTransect);
					
					float fromX = calcPixelForLongitude(trans.mStartWaypt.getLongitude(), mMinLong, gpsToPixels, xCenteringOffset);
					float fromY = calcPixelForLatitude(trans.mStartWaypt.getLatitude(), mMinLat, gpsToPixels, yCenteringOffset, vPixelsUsed);
					float toX = calcPixelForLongitude(trans.mEndWaypt.getLongitude(), mMinLong, gpsToPixels, xCenteringOffset);
					float toY = calcPixelForLatitude(trans.mEndWaypt.getLatitude(), mMinLat, gpsToPixels, yCenteringOffset, vPixelsUsed);
		
					if (isNextTransect) {
						// defer 'til the end
						nextTransectFound = true;
						nextTransectFromX = fromX;
						nextTransectFromY = fromY;
						nextTransectToX = toX;
						nextTransectToY = toY;
					} else if (isActiveTransect) {
						// defer 'til the end
						activeTransectFound = true;
						activeTransectFromX = fromX;
						activeTransectFromY = fromY;
						activeTransectToX = toX;
						activeTransectToY = toY;
					} else {
						// color
						mPaint.setColor(mNormalTransectColor);
		
						// start
						drawCircleOnLine(fromX, fromY, toX, toY, NORMAL_CIRCLE_SIZE, NORMAL_CIRCLE_STROKE_SIZE, 0.0f, canvas);
		
						// end
						drawFatArrowOnLine(fromX, fromY, toX, toY, NORMAL_ARROW_TAIL_SIZE, ARROW_ANGLE_DEGREES, NORMAL_ARROW_STROKE_SIZE, 1f, canvas);
		
						// line
						mPaint.setStrokeWidth(NORMAL_ARROW_BODY_STROKE_SIZE);
						canvas.drawLine(fromX, fromY, toX, toY, mPaint);
		
						// kinda fancy, but tight parallel lines are actually a problem
						// ALT arrowPosition += arrowPositionDelta;
					}
				}
				
				// draw the active one last (so it overlaps)
				if (activeTransectFound) {
					mPaint.setColor(mActiveTransectColor);
					drawCircleOnLine(activeTransectFromX, activeTransectFromY, activeTransectToX, activeTransectToY, IMPORTANT_CIRCLE_SIZE, IMPORTANT_CIRCLE_STROKE_SIZE, 0.0f, canvas);
					drawFatArrowOnLine(activeTransectFromX, activeTransectFromY, activeTransectToX, activeTransectToY, IMPORTANT_ARROW_TAIL_SIZE, ARROW_ANGLE_DEGREES, IMPORTANT_ARROW_STROKE_SIZE, 1f, canvas);
					mPaint.setStrokeWidth(IMPORTANT_ARROW_BODY_STROKE_SIZE);
					canvas.drawLine(activeTransectFromX, activeTransectFromY, activeTransectToX, activeTransectToY, mPaint);
				}
		
				// draw the active one last (so it overlaps)
				if (nextTransectFound) {
					mPaint.setColor(mNextTransectColor);
					drawCircleOnLine(nextTransectFromX, nextTransectFromY, nextTransectToX, nextTransectToY, IMPORTANT_CIRCLE_SIZE, IMPORTANT_CIRCLE_STROKE_SIZE, 0.0f, canvas);
					drawFatArrowOnLine(nextTransectFromX, nextTransectFromY, nextTransectToX, nextTransectToY, IMPORTANT_ARROW_TAIL_SIZE, ARROW_ANGLE_DEGREES, IMPORTANT_ARROW_STROKE_SIZE, 1f, canvas);
					mPaint.setStrokeWidth(IMPORTANT_ARROW_BODY_STROKE_SIZE);
					canvas.drawLine(nextTransectFromX, nextTransectFromY, nextTransectToX, nextTransectToY, mPaint);
				}
			}
			else 
			{
				if (latRange <= 0)
					Log.d(TAG, "latRange is <= 0: " + latRange);
				if (lonRange <= 0)
					Log.d(TAG, "lonRange is <= 0: " + lonRange);
			}
		}
	}
}
