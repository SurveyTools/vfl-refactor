/*
 * based on speedplane's solution
 *    + made it work for height as well
 *    + made it aware of ascent & descent for height
 *    
 * http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview/7875656#7875656
 *
 * <org.surveytools.flightlogger.util.FontFitTextView
 *  android:paddingTop="5dip"         
 *  android:id="@+id/childs_name"       
 *	android:layout_width="fill_parent"        
 *	android:layout_height="0dip"        
 *	android:layout_weight="1"         
 *	android:layout_gravity="center"         
 *	android:textSize="@dimen/text_size"/>
 **/

package org.surveytools.flightlogger.util;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;
import android.util.Log;

public class SquishyTextView extends TextView {

	public float mIdealTextSizeDP = 200;
	public float mMinimumTextSizeDP = 20;

	public SquishyTextView(Context context) {
		super(context);
		initialise();
	}

	public SquishyTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialise();
	}

	public void setIdealTextSizeDP(float textSizeDP) {
		mIdealTextSizeDP = textSizeDP;
	}

	public void setMinimumTextSizeDP(float textSizeDP) {
		mMinimumTextSizeDP = textSizeDP;
	}

	private void initialise() {
		mTestPaint = new Paint();
		mTestPaint.set(this.getPaint());
		// max size defaults to the initially specified text size unless it is
		// too small
	}

	private float calcPixelsFromDP(float dp) {
		// TODO - fix hack.
		// our calcs are in real pixels, not dp
		return dp * 2;
	}

	/*
	 * Re size the font so the specified text fits in the text box assuming the text box is the specified width.
	 */
	private void refitText(String text, int textWidth, int textHeight) {
		
		if (textWidth <= 0)
			return;

		if (textHeight <= 0)
			return;

		int targetWidth = textWidth - this.getPaddingLeft() - this.getPaddingRight();
		int targetHeight = textHeight - this.getPaddingTop() - this.getPaddingBottom();

		// note: mod min & max font size
		float hi = calcPixelsFromDP(mIdealTextSizeDP);
		float lo = calcPixelsFromDP(mMinimumTextSizeDP);

		final float threshold = 0.5f; // How close we have to be

		mTestPaint.set(this.getPaint());

		// TESTING
		// int iters = 0;
		// Log.d("refit starting","tw =" + textWidth + ", th = " + textHeight);
		// Log.d("  refit", "baseline: " + getBaseline());
		// Log.d("  refit", "getLineHeight: " + getLineHeight());

		while ((hi - lo) > threshold) {
			float size = (hi + lo) / 2;
			mTestPaint.setTextSize(size);
			Rect bounds = new Rect();
			mTestPaint.getTextBounds(text, 0, text.length(), bounds);

			float trueTextHeight = -mTestPaint.ascent() + mTestPaint.descent();

			// TESTING Log.d("  refit test", "test-size = " + size + " -->  text w = " + bounds.width() + ", h = " + bounds.height() + ", ascent = " + mTestPaint.ascent() + ", descent = " + mTestPaint.descent());
			// TESTING Log.d("  refit test", "test-size = " + size + " -->  text w = " + bounds.width() + ", h = " + bounds.height() + ", trueTextHeight = " + trueTextHeight);

			if ((bounds.width() >= targetWidth) || (trueTextHeight >= targetHeight))
				hi = size; // too big
			else
				lo = size; // too small

			// TESTING iters++;
		}

		// TESTING Log.d("refit done ", "(" + iters + ") hi = " + hi + ", lo = "
		// + lo);

		// Use lo so that we undershoot rather than overshoot
		this.setTextSize(TypedValue.COMPLEX_UNIT_PX, lo);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		try {
			// crashes
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
			int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
			int height = getMeasuredHeight();

			// TESTING Log.d("onMeasure", "blah");
			refitText(this.getText().toString(), parentWidth, parentHeight);
			this.setMeasuredDimension(parentWidth, height);
		} catch (Exception e) {
			Log.e("SquishyTextView.onMeasure", "ERROR");
		}
	}

	@Override
	protected void onTextChanged(final CharSequence text, final int start, final int before, final int after) {
		// TESTING Log.d("onTextChanged", "blah");
		// OUT_OF_RANGE_METRICS note:, this doesn't actually work
		refitText(text.toString(), this.getWidth(), this.getHeight());
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (w != oldw) {
			// TESTING Log.d("onSizeChanged", "blah");
			refitText(this.getText().toString(), w, h);
		}
	}

	// Attributes
	private Paint mTestPaint;
}