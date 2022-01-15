package org.surveytools.flightlogger;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.app.AlertDialog;

import org.surveytools.flightlogger.R;

import java.util.List;
import java.util.ArrayList;

import org.surveytools.flightlogger.SummaryRowItem;
import org.surveytools.flightlogger.SummaryArrayAdapter;
import org.surveytools.flightlogger.geo.GPSUtils;
import org.surveytools.flightlogger.geo.GPSUtils.DistanceUnit;
import org.surveytools.flightlogger.geo.GPSUtils.VelocityUnit;
import org.surveytools.flightlogger.logger.TransectSummary;
import org.surveytools.flightlogger.util.PreferenceUtils;
import org.surveytools.flightlogger.util.ResourceUtils;
import org.surveytools.flightlogger.AppSettings;

// FRAGMENTS_BAD_IN_ACTIVITY_RESULTS
// Note: this was not done as a Fragment due to the state
// problems involved with putting up a fragment-based dialog
// from onActivityResult

// CUSTOM_LIST_IN_DIALOG
// here's where it comes together

public class TransectSummaryAlert {
	
	protected static String getDistanceUnitsLabel(DistanceUnit units) {
		
		switch (units) {
			case FEET: return "FEET";
			case METERS:return "METERS";
			case KILOMETERS:return "KM";
			case MILES:return "MILES";
			case NAUTICAL_MILES:return "NM";
		}
		
		return "";
	}

	protected static String getSpeedUnitsLabel(Context context, VelocityUnit units) {
		
		switch (units) {
		case NAUTICAL_MILES_PER_HOUR:
			return context.getString(R.string.nav_speed_units_knots);
			
		case KILOMETERS_PER_HOUR:
			return context.getString(R.string.nav_speed_units_kmh);
			
		case MILES_PER_HOUR:
			return context.getString(R.string.nav_speed_units_mph);
			
		case METERS_PER_SECOND:
		default:
			// not supporeted
				break;
		}

		return "";
	}

	public static void showSummary(Context context, TransectSummary summary) {

		AlertDialog.Builder builder = new AlertDialog.Builder(context, AlertDialog.THEME_HOLO_LIGHT);

		builder.setCancelable(true);
		builder.setTitle((summary.mTransect == null) ? R.string.transectsummary_notransct_title : R.string.transectsummary_title);

		// raw list
		List<SummaryRowItem> summaryList = new ArrayList<SummaryRowItem>();
		
		// units
		DistanceUnit altUnits = AppSettings.getPrefAltitudeDisplayUnit(context);
		VelocityUnit speedUnits = AppSettings.getPrefSpeedDisplayUnit(context);
	
		// conversions
		double avgLaserAlt = GPSUtils.convertMetersToDistanceUnits(summary.mAvgLaserAlt, AppSettings.getPrefAltitudeDisplayUnit(context));
		double avgSpeed = GPSUtils.convertMetersPerSecondToVelocityUnits(summary.mAvgSpeed, AppSettings.getPrefSpeedDisplayUnit(context));
		
		// labels
		String avgAltLabel = getDistanceUnitsLabel(altUnits);
		String avgSpeedLabel = getSpeedUnitsLabel(context, speedUnits);
		 
		// full strings
		String avgAltStr = " " + String.format("%.2f", avgLaserAlt) + " " + avgAltLabel;
		String speedStr = " " + String.format("%.2f", avgSpeed) + " " + avgSpeedLabel;
		String transName = (summary.mTransect == null) ? "<No Transect>" : summary.mTransect.getFullName();

		// TODO_TRANSECT_SUMMARY_STUFF - real data
		summaryList.add(new SummaryRowItem(context.getString(R.string.transectsummary_name_label), transName, true));
		summaryList.add(new SummaryRowItem(context.getString(R.string.transectsummary_average_speed_label), speedStr, false));
		summaryList.add(new SummaryRowItem(context.getString(R.string.transectsummary_average_altitude_label), avgAltStr, false));

		final SummaryArrayAdapter adapter = new SummaryArrayAdapter(context, R.layout.transect_summary_row, summaryList.toArray(new SummaryRowItem[summaryList.size()]));

		//builder.s
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// ignore
			}
		});

		builder.setNegativeButton(R.string.modal_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		builder.show();
	
		}
	
}
