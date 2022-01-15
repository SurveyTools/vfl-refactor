package org.surveytools.flightlogger;

import java.util.List;

import org.surveytools.flightlogger.geo.data.Transect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;

public class MiniTransectView extends TransectGraphView {

	Transect	mTransect;
	int			mArrowColor;
	int			mBorderColor;
	boolean		mDrawBorder;
	
	protected final String TAG = this.getClass().getSimpleName();

	// for xml construction
	public MiniTransectView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// for code construction
	public MiniTransectView(final Context context) {
		super(context);
	}

	public void setup(int arrowColor, int borderColor, boolean drawBorder) {
		mArrowColor = arrowColor;
		mBorderColor = borderColor;
		mDrawBorder = drawBorder;
	}

	public void setTransect(Transect transect) {
		mTransect = transect;
		updateGpsBounds();
		invalidate();
	}

	@Override
	protected void updateGpsBounds() {
		if (mTransect != null) {
			mMinLat = Math.min(mTransect.mStartWaypt.getLatitude(), mTransect.mEndWaypt.getLatitude());
			mMaxLat = Math.max(mTransect.mStartWaypt.getLatitude(), mTransect.mEndWaypt.getLatitude());
			mMinLong = Math.min(mTransect.mStartWaypt.getLongitude(), mTransect.mEndWaypt.getLongitude());
			mMaxLong = Math.max(mTransect.mStartWaypt.getLongitude(), mTransect.mEndWaypt.getLongitude());
		}
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		float inset = 16;
		float w = getWidth()-(inset*2);
		float h = getHeight()-(inset*2);

		if (mTransect == null) {
			// circle in the center
			mPaint.setColor(mArrowColor);
			mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			mPaint.setStrokeWidth(4);
			canvas.drawCircle(getWidth()/2, getHeight()/2, 12, mPaint);
		} else {
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
				
				float fromX = calcPixelForLongitude(mTransect.mStartWaypt.getLongitude(), mMinLong, gpsToPixels, xCenteringOffset);
				float fromY = calcPixelForLatitude(mTransect.mStartWaypt.getLatitude(), mMinLat, gpsToPixels, yCenteringOffset, vPixelsUsed);
				float toX = calcPixelForLongitude(mTransect.mEndWaypt.getLongitude(), mMinLong, gpsToPixels, xCenteringOffset);
				float toY = calcPixelForLatitude(mTransect.mEndWaypt.getLatitude(), mMinLat, gpsToPixels, yCenteringOffset, vPixelsUsed);
				
					// color
				mPaint.setColor(mArrowColor);
				
				// start
				drawCircleOnLine(fromX, fromY, toX, toY, IMPORTANT_CIRCLE_SIZE, IMPORTANT_CIRCLE_STROKE_SIZE, 0.0f, canvas);
				
				// end
				drawFatArrowOnLine(fromX, fromY, toX, toY, IMPORTANT_ARROW_TAIL_SIZE, ARROW_ANGLE_DEGREES, IMPORTANT_ARROW_STROKE_SIZE, 1f, canvas);
				
				// line
				mPaint.setStrokeWidth(IMPORTANT_ARROW_BODY_STROKE_SIZE);
				canvas.drawLine(fromX, fromY, toX, toY, mPaint);
				
				// kinda fancy, but tight parallel lines are actually a problem
				// ALT arrowPosition += arrowPositionDelta;
			}
			else 
			{
				if (latRange <= 0)
					Log.d(TAG, "latRange is <= 0: " + latRange);
				if (lonRange <= 0)
					Log.d(TAG, "lonRange is <= 0: " + lonRange);
			}
		}
		
		// border box
		if (mDrawBorder) {
			mPaint.setColor(mBorderColor);
			RectF borderR = new RectF(0, 0, getWidth(), getHeight());
			mPaint.setStyle(Paint.Style.STROKE);
			mPaint.setStrokeWidth(8);
			canvas.drawRoundRect(borderR, 12, 12, mPaint);
		}
	}
}

