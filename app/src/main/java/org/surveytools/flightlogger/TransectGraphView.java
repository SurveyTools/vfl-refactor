package org.surveytools.flightlogger;

import java.util.List;

import android.view.View;

import org.surveytools.flightlogger.geo.data.Transect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;

public class TransectGraphView extends View {

	// drawing
	protected Paint mPaint;
	protected Path mArrowPath;
	
	// data
	protected double mMinLat = 0;
	protected double mMinLong = 0;
	protected double mMaxLat = 0;
	protected double mMaxLong = 0;

	protected static int NORMAL_CIRCLE_SIZE = 5;
	protected static int IMPORTANT_CIRCLE_SIZE = 9;

	protected static int NORMAL_CIRCLE_STROKE_SIZE = 4;
	protected static int IMPORTANT_CIRCLE_STROKE_SIZE = 4;

	protected static int ARROW_ANGLE_DEGREES = 20;

	protected static int NORMAL_ARROW_TAIL_SIZE = 14;
	protected static int NORMAL_ARROW_STROKE_SIZE = 4;
	protected static int NORMAL_ARROW_BODY_STROKE_SIZE = 4;

	protected static int IMPORTANT_ARROW_TAIL_SIZE = 30;
	protected static int IMPORTANT_ARROW_STROKE_SIZE = 4;
	protected static int IMPORTANT_ARROW_BODY_STROKE_SIZE = 8;
	
	// for xml construction
	public TransectGraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setupVars();
	}

	// for code construction
	public TransectGraphView(final Context context) {
		super(context);
		setupVars();
	}

	protected void setupVars() {

		this.mPaint = new Paint();
		this.mArrowPath = new Path();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// save w & h here if need be
	}
	
	protected void updateGpsBounds() {
		// pure virtual
	}
	
	protected float calcPixelForLongitude(double gpsValue, double minGpsValue, double gpsToPixels, double centeringOffset) {
		double relativeGpsValue = gpsValue - minGpsValue;
		double relativePixelValue = relativeGpsValue * gpsToPixels;
		return (float)(relativePixelValue + centeringOffset);
	}
	
	protected float calcPixelForLatitude(double gpsValue, double minGpsValue, double gpsToPixels, double centeringOffset, double pixelRange) {
		double relativeGpsValue = gpsValue - minGpsValue;
		double relativePixelValue = relativeGpsValue * gpsToPixels;
		double reflectedPixelValue = pixelRange - relativePixelValue;
		return (float)(reflectedPixelValue + centeringOffset);
	}
	
	protected double calcAngleForPoints(float x1, float y1, float x2, float y2) {
		double opp = y2 - y1;
		double adj = x2 - x1;
		
		// atan is only good for -pi/2..pi/2 (right half of circle), so we reflect it and spin it back as necessary
		
		if (adj < 0) {
			// reflect and spin back into place
			return Math.atan(-opp/-adj) + Math.PI;
		} else {
			// normal
			return  Math.atan(opp/adj);
		}
	}
	
	protected void drawThinArrowOnLine(float x1, float y1, float x2, float y2, float tailLen, float arrowAngleInDegrees, float stroke, float loc, Canvas canvas) {

		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeWidth(stroke);

		// compute the arrow
		double transAngle = calcAngleForPoints(x1, y1, x2, y2);
		double fortyFive = Math.toRadians(arrowAngleInDegrees);
		double endAngle1 = transAngle + fortyFive;
		double endAngle2 = transAngle - fortyFive;
		
		// TESTING Log.d("TransectGraphView", "arrow " + i + ", line " + Math.toDegrees(transAngle) + ", end1 " + Math.toDegrees(endAngle1) + ", end2 " + Math.toDegrees(endAngle2)); 

		// arrow point location (0 = beginning, 1 = end)
		float x = x1 + ((x2 - x1) * loc);
		float y = y1 + ((y2 - y1) * loc);
		
		float tail1y = (float)(y - tailLen * Math.sin(endAngle1));
		float tail2y = (float)(y - tailLen * Math.sin(endAngle2));
		float tail1x = (float)(x - tailLen * Math.cos(endAngle1));
		float tail2x = (float)(x - tailLen * Math.cos(endAngle2));
		
		// draw the arrow
		canvas.drawLine(tail1x, tail1y, x, y, mPaint);
		canvas.drawLine(tail2x, tail2y, x, y, mPaint);
	}

	protected void drawFatArrowOnLine(float x1, float y1, float x2, float y2, float tailLen, float arrowAngleInDegrees, float stroke, float loc, Canvas canvas) {

		mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		mPaint.setStrokeWidth(stroke);

		// compute the arrow
		double transAngle = calcAngleForPoints(x1, y1, x2, y2);
		double fortyFive = Math.toRadians(arrowAngleInDegrees);
		double endAngle1 = transAngle + fortyFive;
		double endAngle2 = transAngle - fortyFive;
		
		// TESTING Log.d("TransectGraphView", "arrow " + i + ", line " + Math.toDegrees(transAngle) + ", end1 " + Math.toDegrees(endAngle1) + ", end2 " + Math.toDegrees(endAngle2)); 
		// arrow point location (0 = beginning, 1 = end)
		// NOTE: the point of the arrow is on x,y... not really what you'd want for loc = 0
		float x = x1 + ((x2 - x1) * loc);
		float y = y1 + ((y2 - y1) * loc);

		float tail1y = (float)(y - tailLen * Math.sin(endAngle1));
		float tail2y = (float)(y - tailLen * Math.sin(endAngle2));
		float tail1x = (float)(x - tailLen * Math.cos(endAngle1));
		float tail2x = (float)(x - tailLen * Math.cos(endAngle2));
		
		// construct the arrow

		mArrowPath.reset();
		mArrowPath.moveTo(x, y);
		mArrowPath.lineTo(tail1x, tail1y);
		mArrowPath.lineTo(tail2x, tail2y);
		mArrowPath.lineTo(x, y);
		mArrowPath.close();

		// draw the arrow
		canvas.drawLine(tail1x, tail1y, x, y, mPaint);
		canvas.drawLine(tail2x, tail2y, x, y, mPaint);
		canvas.drawPath(mArrowPath, mPaint);
	}

	protected void drawCircleOnLine(float x1, float y1, float x2, float y2, float diameter, float stroke, float loc, Canvas canvas) {

		mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		mPaint.setStrokeWidth(stroke);

		// compute the loc
		float x = x1 + ((x2 - x1) * loc);
		float y = y1 + ((y2 - y1) * loc);
		
		canvas.drawCircle(x, y, diameter, mPaint);
	}

	protected void testArrowQuadrant(float fromX, float fromY, float toX1, float toY1, float toX2, float toY2, float tailLen, float arrowAngleInDegrees, float arrowLoc, int n, int color, Canvas canvas) {

		float dx = (toX2 - toX1) / n;
		float dy = (toY2 - toY1) / n;
		
		float x = toX1;
		float y = toY1;
				
		mPaint.setColor(color);

		for(int i=0;i<n;i++) {
			
			// draw the line
			mPaint.setStyle(Paint.Style.STROKE);
			canvas.drawLine(fromX, fromY, x, y, mPaint);
			
			//drawThinArrowOnLine(fromX, fromY, x, y, tailLen, arrowAngleInDegrees, 4, arrowLoc, canvas);
			drawFatArrowOnLine(fromX, fromY, x, y, tailLen, arrowAngleInDegrees, 4, arrowLoc, canvas);
			
			x  += dx;
			y += dy;
		}
	}

	protected void testArrows(Canvas canvas) {

		int n = 10;
		int inset = 8;
		float tailLen = 24;
		float arrowLoc = .8f; // 0 = start, 1 = end
		float arrowAngleInDegrees = 35;
		float w = getWidth();
		float h = getHeight();

		float centerX = w/2;
		float centerY = h/2;
		float left = inset;
		float right = w - inset;
		float top = inset;
		float bottom = h -inset;
		
		// right testArrowSet(n, centerX, centerY, w-8, h, dx, dy, canvas);
		testArrowQuadrant(centerX, centerY, right, bottom, right, top, tailLen, arrowAngleInDegrees, arrowLoc, n, getResources().getColor(R.color.debug0), canvas);
		testArrowQuadrant(centerX, centerY, right, top, left, top, tailLen, arrowAngleInDegrees, arrowLoc, n, getResources().getColor(R.color.debug1), canvas);
		testArrowQuadrant(centerX, centerY, left, top, left, bottom, tailLen, arrowAngleInDegrees, arrowLoc, n, getResources().getColor(R.color.debug2), canvas);
		testArrowQuadrant(centerX, centerY, left, bottom, right, bottom, tailLen, arrowAngleInDegrees, arrowLoc, n, getResources().getColor(R.color.debug3), canvas);
	}
}
