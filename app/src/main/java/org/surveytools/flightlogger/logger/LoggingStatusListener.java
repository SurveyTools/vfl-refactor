package org.surveytools.flightlogger.logger;

public interface LoggingStatusListener {
	
	public void onLoggingStatusChangeMessage(String statusMessage);
	
	// TODO - put in error code
	public void onLoggingErrorMessage(String errorMessage);
	
	public void onTransectLogSummary(TransectSummary summary);

}
