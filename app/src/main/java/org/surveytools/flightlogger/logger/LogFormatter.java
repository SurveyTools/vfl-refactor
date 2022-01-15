package org.surveytools.flightlogger.logger;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import android.location.Location;

// OK, OK, in OO this would typically by a 'Visitor' object. We may visit that
// approach if we need multiple formats, but for now, let's keep it simple-ish...

public class LogFormatter {
	
	// what do we log for log entries 
	public enum LogFields {
		TIMESTAMP, LAT, LON, ALTITUDE, GPS_ALT, SPEED 
	}

	public static final NumberFormat ELEVATION_FORMAT = NumberFormat
			.getInstance(Locale.US);
	public static final SimpleDateFormat ISO_8601_DATE_TIME_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

	static {
		ELEVATION_FORMAT.setMaximumFractionDigits(1);
		ELEVATION_FORMAT.setGroupingUsed(false);
		ISO_8601_DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public String writeTransectLogRecord(String... values) 
	{
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for (String value : values) {
			if (!isFirst) {
				builder.append(',');
			}
			isFirst = false;

			builder.append('"');
			if (value != null) 
			{
				builder.append(value.replaceAll("\"", "\"\""));
			}
			
			builder.append('"');
		}
		// XXX for now, assuming Windows (CRLF) as terminator
		builder.append("\r\n");
		return builder.toString();
	}

	public String writeGenericCSVRecord(String... values) 
	{
		StringBuilder builder = new StringBuilder();
		boolean isFirst = true;
		for (String value : values) {
			if (!isFirst) {
				builder.append(',');
			}
			isFirst = false;

			builder.append('"');
			if (value != null) 
			{
				builder.append(value.replaceAll("\"", "\"\""));
			}
			
			builder.append('"');
		}
		// XXX for now, assuming Windows (CRLF) as terminator
		builder.append("\r\n");
		return builder.toString();
	}
	
	public String writeGPXFlightlogRecord(String timestamp, LogEntry entry)
	{
		StringBuilder builder = new StringBuilder();
			builder.append("<trkpt lat=\"").append(entry.mLat).append("\" lon=\"").append(entry.mLon).append("\">");
			builder.append("<ele>").append(ELEVATION_FORMAT.format(entry.mGpsAlt)).append("</ele>");
			builder.append("<speed>").append(entry.mSpeed).append("</speed>");
			builder.append("<time>").append(timestamp).append("</time>");
			builder.append("<extension><agl>").append(ELEVATION_FORMAT.format(entry.mAlt)).append("</agl></extension>");
			builder.append("</trkpt>").append("\n");
			return builder.toString();
	}
	
	public String writeTransectColumnTitles() 
	{
		final String[] titles = {"Timestamp", "Transect", "Lat", "Lon", "Laser Alt (m)",  "GPS Alt (m)", "Speed (m/s)"};
		return writeGenericCSVRecord(titles);
	}

}
