package org.surveytools.flightlogger;

// CUSTOM_LIST_IN_DIALOG
// row item wrapper to hold the details

public class SummaryRowItem {
	public String mLabelText;
	public String mDetailsText;
	public boolean mIsHeader;

	public SummaryRowItem(String label, String details, boolean isHeader) {
		super();
		mLabelText = label;
		mDetailsText = details;
		mIsHeader = isHeader;
	}

	// copy constructor
	public SummaryRowItem(SummaryRowItem srcData) {
		super();
		if (srcData != null) {
			mLabelText = srcData.mLabelText;
			mDetailsText = srcData.mDetailsText;
			mIsHeader = srcData.mIsHeader;
		}
	}

	// convenience method for UI display when using ArrayAdapters
	public String toString() {
		return mLabelText;
	}
}
