package org.surveytools.flightlogger;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.surveytools.flightlogger.altimeter.AltimeterService;
import org.surveytools.flightlogger.altimeter.AltitudeUpdateListener;
import org.surveytools.flightlogger.altimeter.SerialConsole;
import org.surveytools.flightlogger.geo.GPSUtils;
import org.surveytools.flightlogger.geo.GPSUtils.DistanceUnit;
import org.surveytools.flightlogger.geo.NavigationService;
import org.surveytools.flightlogger.geo.TransectUpdateListener;
import org.surveytools.flightlogger.geo.data.Transect;
import org.surveytools.flightlogger.geo.data.TransectStatus;
import org.surveytools.flightlogger.logger.LoggingService;
import org.surveytools.flightlogger.logger.LoggingStatusListener;
import org.surveytools.flightlogger.logger.TransectSummary;
import org.surveytools.flightlogger.util.SquishyTextView;
import org.surveytools.flightlogger.util.SystemUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class FlightLogger extends USBAwareActivity
		implements AltitudeUpdateListener,
		TransectUpdateListener,
		LoggingStatusListener,
		OnMenuItemClickListener {

	// used for identifying Activities that return results
	static final int LOC_REQUEST_CODE = 101001;
	static final int LOAD_FLIGHT_PATH = 10011;
	static final int LOAD_CSV_LOGFILE = 1001299;
	static final int CHOOSE_NEXT_TRANSECT = 10012;
	static final int CHANGE_APP_SETTINGS = 10013;
	static final int UI_UPDATE_TIMER_MILLIS = 500;
	static final int SHOW_OUT_OF_RANGE_AFTER_MILLIS = 12000;
	static final boolean DEMO_MODE = false; // DEMO_MODE
	public static final int UPDATE_IMAGE = 666;
	public static final String LOG_CLASSNAME = "FlightLogger";
	private static final String SAVED_FLIGHT_DATA_KEY = "FlightData";

	private static final String SAVED_FLIGHT_DATA_GPXNAME_KEY = "FlightData.gpxName";
	private static final String SAVED_FLIGHT_DATA_ROUTENAME_KEY = "FlightData.routeName";
	private static final String SAVED_FLIGHT_DATA_TRANSECTNAME_KEY = "FlightData.transectName";
	private static final String SAVED_FLIGHT_DATA_TRANSECTDETAILS_KEY = "FlightData.transectDetails";
	private static final String SAVED_FLIGHT_DATA_ACTION_KEY = "FlightData.actionX";
	private static final String SAVED_FLIGHT_DATA_TRANSECT_PARSING_METHOD_KEY = "FlightData.tpm";

	private AltimeterService mAltimeterService;
	private NavigationService mNavigationService;
	private LoggingService mLogger;
	private TransectILSView mNavigationDisplay;

	// file info
	private Button mFileIconButton;
	private TextView mFileAndRouteDisplay;
	private TextView mTransectDisplay;
	private TextView mFileMessageDisplay;

	private TextView mAltitudeValueDisplay;
	private TextView mAltitudeUnitsDisplay;
	private TextView mGroundSpeedValueDisplay;
	private TextView mGroundSpeedUnitsDisplay;

	private Button mStatusButtonGPS;
	private Button mStatusButtonALT;
	private Button mStatusButtonBAT;
	private Button mStatusButtonBOX;

	private Drawable mStatusButtonBackgroundRed;
	private Drawable mStatusButtonBackgroundYellow;
	private Drawable mStatusButtonBackgroundGreen;
	private Drawable mStatusButtonBackgroundGrey;
	private Drawable mStatusButtonBackgroundIgnore;

	private Drawable mModeButtonBorderRed;
	private Drawable mModeButtonBorderGrey;
	private Drawable mModeButtonBorderGreen;

	private int mAltitudeTextWhite;
	private int mAltitudeTextYellow;

	private int mModeButtonTextColorOnRed;
	private int mModeButtonTextColorOnGrey;
	private int mModeButtonTextColorOnGreen;

	private int mStatusTextColorRed;
	private int mStatusTextColorGreen;
	private int mStatusTextColorGrey;

	private Drawable mFileIconBackgroundWhite;
	private Drawable mFileIconBackgroundRed;
	private Drawable mFileIconBackgroundYellow;
	private Drawable mFileIconBackgroundGreen;

	private TextView mStatusDisplayLeft;
	private Button mStartStopButton;
	private TextView mStatusDisplayRight;

	private int mStatusButtonTextColorOnRed;
	private int mStatusButtonTextColorOnYellow;
	private int mStatusButtonTextColorOnGreen;
	private int mStatusButtonTextColorOnGrey;
	private int mStatusButtonTextColorOnIgnore;

	// data
	protected AppSettings mAppSettings;
	protected CourseInfoIntent mFlightData;
	protected AltitudeDatum mAltitudeData;
	protected long mLastGoodAltitudeTimestamp;
	protected float mLastGoodAltitudeDatum;
	protected GPSDatum mGPSData;
	protected BatteryDatum mBatteryData;
	protected BoxDatum mBoxData;

	// data averaging
	protected ArrayList<TransectStatus> mTransectStatusHistory;
	protected ArrayList<TransectStatus> mTransectStatusSorted;
	protected ArrayList<Float> mAltitudeHistory;
	protected ArrayList<Float> mAltitudeSorted;

	protected DistanceUnit mStatusBarDistanceUnits = DistanceUnit.MILES;
	protected String mStatusBarDistanceUnitsDisplayString = "";

	// STATE
	protected Transect mCurTransect;

	private Handler mUpdateUIHandler;
	private NumberFormat mDistanceStatusFormatterDot2;
	private NumberFormat mDistanceStatusFormatterDot1;
	private NumberFormat mDistanceStatusFormatterDot0;

	private void readObjectsFromNavService() {
		if (mNavigationService != null) {
			mCurTransect = mNavigationService.mCurrTransect;
			
			// TRANSECT_DETAILS_STORED_IN_NAV_SERVICE
			// pull flight data (will work in all cases except for mock data)
			if (mNavigationService.mCurrTransect != null)
				mFlightData = mNavigationService.mCurrTransectDetails;

			// STARTUP_SEQUENCE_EVENTS
			Log.d(LOG_CLASSNAME, "readObjectsFromNavService " + ((mCurTransect == null) ? "TRANSECT NULL" : "transect ok") + ", " + ((mFlightData == null) ? "<< FLIGHT DATA NULL >>" : "flight data ok"));
		}
	}

	/**
	 * Defines callbacks for local service binding, ie bindService() For local binds, this is where we will attach assign instance references, and add and remove listeners, since we have inprocess access to the class interface
	 */
	
	private ServiceConnection mNavigationConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(LOG_CLASSNAME, "nav service connected"); // STARTUP_SEQUENCE_EVENTS

			org.surveytools.flightlogger.geo.NavigationService.LocalBinder binder = (org.surveytools.flightlogger.geo.NavigationService.LocalBinder) service;
			mNavigationService = (NavigationService) binder.getService();
			mNavigationService.registerListener(FlightLogger.this);

			// update nav service stuff
			readObjectsFromNavService();
			updateUI();

		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mNavigationService.unregisterListener(FlightLogger.this);
		}
	};

	private ServiceConnection mAltimeterConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(LOG_CLASSNAME, "altimeter service connected"); // STARTUP_SEQUENCE_EVENTS

			org.surveytools.flightlogger.altimeter.AltimeterService.LocalBinder binder = (org.surveytools.flightlogger.altimeter.AltimeterService.LocalBinder) service;
			mAltimeterService = (AltimeterService) binder.getService();
			initUsbDriver();
			mAltimeterService.registerListener(FlightLogger.this);

			updateUI();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAltimeterService.unregisterListener(FlightLogger.this);
		}
	};

	private ServiceConnection mLoggerConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(LOG_CLASSNAME, "logger service connected"); // STARTUP_SEQUENCE_EVENTS

			org.surveytools.flightlogger.logger.LoggingService.LocalBinder binder = (org.surveytools.flightlogger.logger.LoggingService.LocalBinder) service;
			mLogger = (LoggingService) binder.getService();
			mLogger.registerListener(FlightLogger.this);
			
			// without this we get a red flash until on the footer until the logger kicks in (close to a second delay)
			updateUI();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mLogger.unregisterListener(FlightLogger.this);
		}
	};

	protected void onStart() {
		super.onStart();
		startServices();
	}

	private void bindServices() {
		Intent intent = new Intent(this, AltimeterService.class);
		this.bindService(intent, mAltimeterConnection, 0);
		Intent intent2 = new Intent(this, NavigationService.class);
		this.bindService(intent2, mNavigationConnection, 0);
		Intent intent3 = new Intent(this, LoggingService.class);
		this.bindService(intent3, mLoggerConnection, 0);
	}

	protected void resetAverages() {
		mTransectStatusHistory = new ArrayList<TransectStatus>();
		mTransectStatusSorted = new ArrayList<TransectStatus>();
		mAltitudeHistory = new ArrayList<Float>();
		mAltitudeSorted = new ArrayList<Float>();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// STARTUP_SEQUENCE_EVENTS / TRANSECT_DETAILS_STORED_IN_NAV_SERVICE POI (since the services may bind before this gets to the code below)
		int permissionCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION);
		if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
			// ask permissions here using below code
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					LOC_REQUEST_CODE);
		}

		//bindServices();
		//resetAverages();

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		ViewGroup layout = (ViewGroup) findViewById(R.id.navscreenLeft);
		TransectILSView tv = new TransectILSView(this);
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		boolean demoMode = false; // DEMO_MODE
		mAltitudeData = new AltitudeDatum(false, demoMode);
		mGPSData = new GPSDatum(false, demoMode);
		mBatteryData = new BatteryDatum(false, demoMode);
		mBoxData = new BoxDatum(false, demoMode);
		mLastGoodAltitudeTimestamp = 0;
		mLastGoodAltitudeDatum = 0;

		mFileIconButton = (Button) findViewById(R.id.nav_header_file_button);
		mFileAndRouteDisplay = (TextView) findViewById(R.id.nav_header_route_text);
		mTransectDisplay = (TextView) findViewById(R.id.nav_header_transect_text);
		mFileMessageDisplay = (TextView) findViewById(R.id.nav_header_message);

		mAltitudeValueDisplay = (TextView) findViewById(R.id.nav_altitude_value);
		mAltitudeUnitsDisplay = (TextView) findViewById(R.id.nav_altitude_units);
		mGroundSpeedValueDisplay = (TextView) findViewById(R.id.nav_speed_value);
		mGroundSpeedUnitsDisplay = (TextView) findViewById(R.id.nav_speed_units);
		mNavigationDisplay = tv;

		mStatusButtonGPS = (Button) findViewById(R.id.nav_header_status_gps);
		mStatusButtonALT = (Button) findViewById(R.id.nav_header_status_alt);
		mStatusButtonBAT = (Button) findViewById(R.id.nav_header_status_bat);
		mStatusButtonBOX = (Button) findViewById(R.id.nav_header_status_box);

		// footer
		mStatusDisplayLeft = (TextView) findViewById(R.id.nav_footer_status_left);
		mStartStopButton = (Button) findViewById(R.id.nav_footer_mode_button);
		mStatusDisplayRight = (TextView) findViewById(R.id.nav_footer_status_right);

		// backgrounds for status lights
		// COLOR_UPDATE_WIP
		mStatusButtonBackgroundRed = getResources().getDrawable(R.drawable.nav_status_red);
		mStatusButtonBackgroundYellow = getResources().getDrawable(R.drawable.nav_status_yellow);
		mStatusButtonBackgroundGreen = getResources().getDrawable(R.drawable.nav_status_green);
		mStatusButtonBackgroundGrey = getResources().getDrawable(R.drawable.nav_status_grey);
		mStatusButtonBackgroundIgnore = getResources().getDrawable(R.drawable.nav_status_ignore);

		// mode button
		mModeButtonBorderRed = getResources().getDrawable(R.drawable.nav_mode_button_red);
		mModeButtonBorderGrey = getResources().getDrawable(R.drawable.nav_mode_button_grey);
		mModeButtonBorderGreen = getResources().getDrawable(R.drawable.nav_mode_button_green);

		// file button
		mFileIconBackgroundWhite = getResources().getDrawable(R.drawable.filefolder);
		mFileIconBackgroundRed = getResources().getDrawable(R.drawable.filefolder_red);
		mFileIconBackgroundYellow = getResources().getDrawable(R.drawable.fileicon_yellow);
		mFileIconBackgroundGreen = getResources().getDrawable(R.drawable.fileicon_green);

		// formatters
		mDistanceStatusFormatterDot2 = new DecimalFormat("#0.00");
		mDistanceStatusFormatterDot1 = new DecimalFormat("#0.0");
		mDistanceStatusFormatterDot0 = new DecimalFormat("#0");

		tv.setLayoutParams(lp);
		layout.addView(tv);

		// ALTITUDE_NON_SQUISHY_TEXT_VIEW setupSquishyFontView(R.id.nav_altitude_value, 190, 20);
		setupSquishyFontView(R.id.nav_speed_value, 130, 20);

		resetData();

 		//STARTUP_SEQUENCE_EVENTS
		Log.d(LOG_CLASSNAME, "onCreate:  " + ((savedInstanceState == null) ? "INSTANCE NULL" : "instance ok") + ", " + ((mNavigationService == null) ? "<< NO NAV SERVICE >>" : "nav service ok"));

		// SAVE_RESTORE_STATE
		if (savedInstanceState != null) {
			// TODO: this shouldn't be needed anymore -- see TRANSECT_DETAILS_STORED_IN_NAV_SERVICE
			mFlightData = savedInstanceState.getParcelable(SAVED_FLIGHT_DATA_KEY);

			// TRANSECT_DETAILS_STORED_IN_NAV_SERVICE - pull these from the nav service if possible
			readObjectsFromNavService();
		} else if (DEMO_MODE) {
			mFlightData = new CourseInfoIntent("Example_survey_route.gpx", "Session 1", "Transect 3", "T03_S ~ T03_N", 0);
		} else {
			// note: you get here if you use the 'back' button on the FL activity
			readObjectsFromNavService();
		}

		// fallback defaults
		if (mFlightData == null)
			mFlightData = new CourseInfoIntent(null, null, null, null, 0);

		// TESTING debugging PreferenceUtils.resetSharedPrefsToDefaults(this);
		// update the prefs
		if (mAppSettings == null)
			mAppSettings = new AppSettings(this);

		mNavigationDisplay.updateSettings(mAppSettings);

		mUpdateUIHandler = new Handler();

		setupColors();
		updateUnitsUI();

		// TESTING showAppSettings();
	}

	private void startServices() {
		// TODO - this becomes a RouteManagerService, or
		// whatever we call it. For now, spin up the AltimeterService
		Intent altIntent = new Intent(this, AltimeterService.class);
		// altIntent.putExtra(AltimeterService.USE_MOCK_DATA, true);
		startService(altIntent);
		Intent navIntent = new Intent(this, NavigationService.class);
		// navIntent.putExtra(NavigationService.USE_MOCK_DATA, true);
		startService(navIntent);
		Intent loggerIntent = new Intent(this, LoggingService.class);
		startService(loggerIntent);
	}

	protected void updateBatteryStatus(Intent batteryStatus) {
		if (mBatteryData.updateBatteryStatus(batteryStatus))
			updateBatteryUI();

		// the box status cues off of the usb fast/slow charing
		if (mBoxData.updateBoxWithBatteryStatus(batteryStatus))
			updateBoxUI();
	}

	private void setupSquishyFontView(int groupID, int ideal, int min) {
		SquishyTextView squishyTextView = (SquishyTextView) findViewById(groupID);
		if (squishyTextView != null) {
			squishyTextView.setIdealTextSizeDP(ideal);
			squishyTextView.setMinimumTextSizeDP(min);
		}
	}

	private void setColorforViewWithID(int groupID, int colorID) {
		View v = findViewById(groupID);
		if (v != null)
			v.setBackgroundColor(getResources().getColor(colorID));
	}

	private void setBlackColorforViewWithID(int groupID) {
		setColorforViewWithID(groupID, R.color.nav_background);
	}

	private void setHeaderColorforViewWithID(int groupID) {
		setColorforViewWithID(groupID, R.color.nav_header_bg);
	}

	private void setFooterColorforViewWithID(int groupID) {
		// COLOR_UPDATE_WIP
		setColorforViewWithID(groupID, R.color.nav_footer_bg);
	}

	private void setFooterBackgroundColor(int colorID) {
		// COLOR_UPDATE_WIP
		setColorforViewWithID(R.id.nav_footer, colorID);
	}

	private void setFooterBackgroundColor2(int colorRsrc) {
		// COLOR_UPDATE_WIP
		View v = findViewById(R.id.nav_footer);
		if (v != null)
			v.setBackgroundColor(colorRsrc);
	}

	protected void setupColors() {

		// override debug colors
		// TESTING flag
		if (true) {

			// whole screen
			setBlackColorforViewWithID(R.id.main_layout);

			// left & right block
			setBlackColorforViewWithID(R.id.navscreenLeft);
			setBlackColorforViewWithID(R.id.navscreenRight);

			// altitude
			setBlackColorforViewWithID(R.id.nav_altitude_group_wrapper);
			setBlackColorforViewWithID(R.id.nav_altitude_group);
			setBlackColorforViewWithID(R.id.nav_altitude_value);
			setBlackColorforViewWithID(R.id.nav_altitude_righthalf);
			setBlackColorforViewWithID(R.id.nav_altitude_label);
			setBlackColorforViewWithID(R.id.nav_altitude_units);

			// speed
			setBlackColorforViewWithID(R.id.nav_speed_group_wrapper);
			setBlackColorforViewWithID(R.id.nav_speed_group);
			setBlackColorforViewWithID(R.id.nav_speed_value);
			setBlackColorforViewWithID(R.id.nav_speed_righthalf);
			setBlackColorforViewWithID(R.id.nav_speed_label);
			setBlackColorforViewWithID(R.id.nav_speed_units);

			// header
			setHeaderColorforViewWithID(R.id.nav_header);
			setHeaderColorforViewWithID(R.id.nav_header_right);
			setHeaderColorforViewWithID(R.id.nav_header_settings_button);

			// footer
			setFooterColorforViewWithID(R.id.nav_footer);
		}

		// status button colors
		mStatusButtonTextColorOnRed = getResources().getColor(R.color.nav_header_status_text_over_red);
		mStatusButtonTextColorOnYellow = getResources().getColor(R.color.nav_header_status_text_over_yellow);
		mStatusButtonTextColorOnGreen = getResources().getColor(R.color.nav_header_status_text_over_green);
		mStatusButtonTextColorOnGrey = getResources().getColor(R.color.nav_header_status_text_over_grey);
		mStatusButtonTextColorOnIgnore = getResources().getColor(R.color.nav_header_status_text_over_ignore);

		mStatusTextColorRed = getResources().getColor(R.color.nav_footer_status_text_red);
		mStatusTextColorGreen = getResources().getColor(R.color.nav_footer_status_text_green);
		mStatusTextColorGrey = getResources().getColor(R.color.nav_footer_status_text_grey);

		mModeButtonTextColorOnRed = getResources().getColor(R.color.nav_footer_mode_text_over_red);
		mModeButtonTextColorOnGrey = getResources().getColor(R.color.nav_footer_mode_text_over_grey);
		mModeButtonTextColorOnGreen = getResources().getColor(R.color.nav_footer_mode_text_over_green);

		// altitude button
		mAltitudeTextWhite = getResources().getColor(R.color.nav_altitude_value);
		mAltitudeTextYellow = getResources().getColor(R.color.nav_altitude_yellow);
	}

	protected void updateUnitsUI() {
		int distanceUnitsRsrcID = R.string.nav_distance_units_miles;
		int speedUnitsRsrcID = R.string.nav_speed_units_knots;
		int altitudeUnitsRsrcID = R.string.nav_altitude_units_feet;

		// EVAL_CENTRALIZE?
		switch (AppSettings.getPrefDistanceDisplayUnit(this)) {
		case KILOMETERS:
			distanceUnitsRsrcID = R.string.nav_distance_units_lowercase_kilometers;
			break;

		case MILES:
			distanceUnitsRsrcID = R.string.nav_distance_units_lowercase_miles;
			break;

		case NAUTICAL_MILES:
			distanceUnitsRsrcID = R.string.nav_distance_units_lowercase_nautical_miles;
			break;

		case FEET:
		case METERS:
		default:
			Log.e(LOGGER_TAG, "distance units not supported");
			break;
		}

		// EVAL_CENTRALIZE?
		switch (AppSettings.getPrefSpeedDisplayUnit(this)) {
		case NAUTICAL_MILES_PER_HOUR:
			speedUnitsRsrcID = R.string.nav_speed_units_knots;
			break;

		case KILOMETERS_PER_HOUR:
			speedUnitsRsrcID = R.string.nav_speed_units_kmh;
			break;

		case MILES_PER_HOUR:
			speedUnitsRsrcID = R.string.nav_speed_units_mph;
			break;

		case METERS_PER_SECOND:
		default:
			Log.e(LOGGER_TAG, "speed units not supported");
			break;
		}

		// EVAL_CENTRALIZE?
		switch (AppSettings.getPrefAltitudeDisplayUnit(this)) {
		case FEET:
			altitudeUnitsRsrcID = R.string.nav_altitude_units_feet;
			break;

		case METERS:
			altitudeUnitsRsrcID = R.string.nav_altitude_units_meters;
			break;

		case KILOMETERS:
		case MILES:
		case NAUTICAL_MILES:
		default:
			Log.e(LOGGER_TAG, "altitude units not supported");
			break;

		}

		mAltitudeUnitsDisplay.setText(getResources().getString(altitudeUnitsRsrcID));
		mGroundSpeedUnitsDisplay.setText(getResources().getString(speedUnitsRsrcID));
		mStatusBarDistanceUnitsDisplayString = getResources().getString(distanceUnitsRsrcID);

		// PREF_UNITS
		mAltitudeData.setDisplayUnits(AppSettings.getPrefAltitudeDisplayUnit(this));
		mGPSData.setDisplaySpeedUnits(AppSettings.getPrefSpeedDisplayUnit(this));
		mNavigationDisplay.setDisplayUnits(AppSettings.getPrefAltitudeDisplayUnit(this));
		mStatusBarDistanceUnits = AppSettings.getPrefDistanceDisplayUnit(this);
		updateFooterUI(); // "miles to xxx"
	}

	protected void resetData() {

		mCurTransect = null;
		// eval: mFlightData too?

		mAltitudeData.reset();
		mGPSData.reset();
		mBatteryData.reset();
		mBoxData.reset();
		mLastGoodAltitudeTimestamp = 0;
		mLastGoodAltitudeDatum = 0;
		resetAverages();
		// note: might want to update the ui (last param) depending on use
	}

	public boolean showMainMenuPopup(View v) {
		PopupMenu popup = new PopupMenu(this, v);
		popup.setOnMenuItemClickListener(this);
		MenuInflater inflater = popup.getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, popup.getMenu());
		popup.show();
		return true;
	}

	public void showAboutDialog() {
		String versionString = SystemUtils.getVersionString(this);
		if (versionString == null)
			versionString = getResources().getString(R.string.nav_about_version_missing);

		String fullVersionString = getResources().getString(R.string.nav_about_version_base) + " " + versionString;
		String title = getResources().getString(R.string.nav_about_title);

		showConfirmDialog(title, fullVersionString);
	}

	public void showConfirmDialog(String title, String msg) {

		new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT).setIcon(android.R.drawable.ic_dialog_alert).setTitle(title).setMessage(msg).setPositiveButton(R.string.modal_ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).show();
	}

	public void showAppSettings() {
		Intent intent = new Intent(this, AppSettingsActivity.class);
		this.startActivityForResult(intent, CHANGE_APP_SETTINGS);
	}

	public boolean onMenuItemClick(MenuItem item) {
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.action_show_about:
			showAboutDialog();
			break;
		case R.id.action_show_settings:
			showAppSettings();
			break;
		case R.id.action_reset_logfile:
			File rotate = mLogger.rotateLogs();
				showConfirmDialog("Flight Summary Created", "The flight summary is located in " + rotate.getAbsolutePath());
			break;
		case R.id.action_show_serial_console:
 			intent = new Intent(this, SerialConsole.class);
 			startActivity(intent);
			break;
//		case R.id.action_reset_logfile:
//			if ( mLogger.resetLogging() == true )
//				showConfirmDialog("Flight Summary", "The summary was created successfully");
//			break;
//		case R.id.action_show_gps_debug:
//			intent = new Intent(this, GPSDebugActivity.class);
//			startActivity(intent);
//			break;
//		case R.id.action_convert_cvs_logfile:
//			intent = new Intent(this, FileBrowser.class);
//			startActivityForResult(intent, LOAD_CSV_LOGFILE);
		}
		return true;
	}
	
	protected void showError(String message) {
		Log.e(LOG_CLASSNAME, "Error: " + message);
		Toast.makeText(this, "ERROR " + message, Toast.LENGTH_SHORT).show();
	}

	protected void doAdvanceTransectActivity() {
		Intent intent = new Intent(this, NextTransectActivity.class);
		intent.putExtra(NextTransectActivity.NT_CUR_TRANSECT_DATA_KEY, mFlightData);
		this.startActivityForResult(intent, CHOOSE_NEXT_TRANSECT);
	}

	protected TransectSummary setLogging(boolean on) {
		TransectSummary summary = null;

		try {
			if (mLogger != null) {
				if (on) {
					// note: this also stops the currrent log
					mLogger.startTransectLog(mCurTransect);
				} else {
					// TRANSECT_SUMMARY_POI 4, execute the stop logging here
					// TODO_TRANSECT_SUMMARY_STUFF, if mCurTransect is not null, expect a summary
					summary = mLogger.stopTransectLog();
				}
			}
		} catch (Exception e) {
			showError(e.getLocalizedMessage());
		}

		updateUI();

		return summary;
	}

	protected TransectSummary setFlightData(CourseInfoIntent data) {
		TransectSummary summary = null;
		mFlightData = data;
		mCurTransect = Transect.newTransect(data, mAppSettings.mPrefTransectParsingMethod);

		if (mCurTransect != null) {

			try {
				// TRANSECT_DETAILS_STORED_IN_NAV_SERVICE
				if (mNavigationService != null) {
					mNavigationService.startNavigation(mCurTransect, data);
				}
			} catch (Exception e) {
				showError(e.getLocalizedMessage());
			}

			// TRANSECT_SUMMARY_POI 4, key the stop
			summary = setLogging(false);

			updateRouteUI();
		}

		return summary;
	}

	// FRAGMENTS_BAD_IN_ACTIVITY_RESULTS
	// avoid committing transactions in asynchronous callback methods,
	// which happens to THIS fragment (before its state is fully set up)
	// since we are pushing another fragment.

	protected void doTransectSummaryDialog(TransectSummary summary) {

		// TRANSECT_SUMMARY_POI 6, ready to show
		if (summary != null) {

			// quick validity check to keep this from coming up when
			// you're jumping around in the ui
			TransectSummaryAlert.showSummary(this, summary);
		}
	}

	/**
	 * Callbacks from activities that return results
	 * 
	 * FRAGMENTS_BAD_IN_ACTIVITY_RESULTS
	 * 
	 * WARNING - you can't launch another fragment-based window from here (since it affects our state and we're not up and running such that we can save again).
	 * 
	 * you can: throw up alerts (non-fragment based) you can: startActivityForResult (by testing - havne't checked docs)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		TransectSummary summary = null;

		// Check which request we're responding to
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case LOAD_FLIGHT_PATH:
				CourseInfoIntent fData = data.getParcelableExtra(CourseSettingsActivity.FS_COURSE_DATA_KEY);
				if (fData != null) {
					// TRANSECT_SUMMARY_POI note, doesn't stop
					summary = setFlightData(fData);

					// TODO-EVAL, auto-stop and show summary?
					// doTransectSummaryDialog(summary);
				}
				break;
			case CHOOSE_NEXT_TRANSECT:
				// TRANSECT_SUMMARY_POI 3a
				CourseInfoIntent ntData = data.getParcelableExtra(NextTransectActivity.NT_CUR_TRANSECT_DATA_KEY);
				if (ntData != null) {
					// "NEXT" aka "USE NEXT TRANSECT"
					// TRANSECT_SUMMARY_POI 3a, setFlightData calls setLogging(false)
					summary = setFlightData(ntData);
				} else {
					// "OK" aka "BREAK"
					// NO_TRANSDATA_MEANS_DONT_CHANGE
					// TRANSECT_SUMMARY_POI 3b - still want the summary though
					// TODO_TRANSECT_SUMMARY_STUFF bug, "OK" doesn't stop logging as of 0.5.5
					summary = setLogging(false);
				}
				doTransectSummaryDialog(summary);
				break;
			case LOAD_CSV_LOGFILE:
				String filePath = data.getStringExtra(FileBrowser.FILE_NAME_STRING_KEY);
				File logFile = new File(filePath);
				mLogger.convertLogToGPXFormat(logFile);
				break;
			case CHANGE_APP_SETTINGS:
				// SETTINGS_OK_MEANS_REFRESH
				updateUnitsUI();
				mAppSettings.refresh(this);
				resetAverages();
				// TESTING mAppSettings.debugDump();
				// TODO - event this
				mNavigationDisplay.updateSettings(mAppSettings);
				break;
			}
		}
	}

	@Override
	protected void onDestroy() {
		if (mAltimeterConnection != null)
			unbindService(mAltimeterConnection);
		if (mNavigationConnection != null)
			unbindService(mNavigationConnection);
		if (this.mLoggerConnection != null) 
			unbindService(mLoggerConnection);

		super.onDestroy();
		// TODO eval for teardown?
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		if (hasFocus) {
			// note: requires Android 4.4 / api level 16 & 19
			View decorView = getWindow().getDecorView();
			decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		
		updateUI();

		mUpdateUIHandler.postDelayed(mUpdateUIRunnable, UI_UPDATE_TIMER_MILLIS);
	}

	@Override
	public void onPause() {
		super.onPause();
		mUpdateUIHandler.removeCallbacks(mUpdateUIRunnable);
	}

	private final Runnable mUpdateUIRunnable = new Runnable() {
		@Override
		public void run() {
			updateUI();
			mUpdateUIHandler.postDelayed(mUpdateUIRunnable, UI_UPDATE_TIMER_MILLIS);
		}
	};

	@Override
	protected void initUsbDevice(UsbDevice device) {
		super.initUsbDevice(device);
	}

	protected void updateStatusButton(Button button, FlightDatum dataStatus) {

		if (button != null) {
			Drawable buttonBG = mStatusButtonBackgroundGrey; // default
			int textColor = mStatusButtonTextColorOnGrey;

			// status to background color
			switch (dataStatus.getStatusColor()) {
			case FlightDatum.FLIGHT_STATUS_RED:
				buttonBG = mStatusButtonBackgroundRed;
				textColor = mStatusButtonTextColorOnRed;

				// COLOR_UPDATE_WIP
				// Force the lights to green
				// TESTING buttonBG = mStatusButtonBackgroundGreen;
				// TESTING textColor = mStatusButtonTextColorOnGreen;
				break;

			case FlightDatum.FLIGHT_STATUS_YELLOW:
				buttonBG = mStatusButtonBackgroundYellow;
				textColor = mStatusButtonTextColorOnYellow;
				break;

			case FlightDatum.FLIGHT_STATUS_GREEN:
				buttonBG = mStatusButtonBackgroundGreen;
				textColor = mStatusButtonTextColorOnGreen;
				break;

			case FlightDatum.FLIGHT_STATUS_UNKNOWN:
				buttonBG = mStatusButtonBackgroundGrey;
				textColor = mStatusButtonTextColorOnGrey;
				break;

			case FlightDatum.FLIGHT_STATUS_IGNORE:
				buttonBG = mStatusButtonBackgroundIgnore;
				textColor = mStatusButtonTextColorOnIgnore;
				break;

			}

			button.setBackground(buttonBG);
			button.setTextColor(textColor);
		}
	}

	protected void updateRouteUI() {
		if ((mFlightData == null) || !mFlightData.hasFile()) {
			// RED - just show the message
			mFileIconButton.setBackground(mFileIconBackgroundRed);

			mFileMessageDisplay.setVisibility(View.VISIBLE);
			mFileAndRouteDisplay.setVisibility(View.INVISIBLE);
			mTransectDisplay.setVisibility(View.INVISIBLE);

			mFileMessageDisplay.setText(R.string.nav_no_gpx_error_text);
			mFileAndRouteDisplay.setText(null);
			mTransectDisplay.setText(null);
		} else if (mFlightData.hasFileButNotEverythingElse()) {
			// YELLOW
			mFileIconButton.setBackground(mFileIconBackgroundYellow);

			mFileMessageDisplay.setVisibility(View.INVISIBLE);
			mFileAndRouteDisplay.setVisibility(View.VISIBLE);
			mTransectDisplay.setVisibility(View.VISIBLE);

			mFileMessageDisplay.setText(null);
			mFileAndRouteDisplay.setText(mFlightData.getShortFilename());
			mTransectDisplay.setText(mFlightData.getShortTransectName());
		} else if (mFlightData.isFullyReady()) {
			// GREEN
			mFileIconButton.setBackground(mFileIconBackgroundWhite); // optional green (kinda distracting)

			mFileMessageDisplay.setVisibility(View.INVISIBLE);
			mFileAndRouteDisplay.setVisibility(View.VISIBLE);
			mTransectDisplay.setVisibility(View.VISIBLE);

			mFileMessageDisplay.setText(null);
			mFileAndRouteDisplay.setText(mFlightData.getShortFilename());
			mTransectDisplay.setText(mFlightData.getShortTransectName());
		}
	}

	protected void updateNavigationUI() {
		mNavigationDisplay.update(mAltitudeData, mGPSData);
	}

	protected void updateAltitudeUI() {

		updateStatusButton(mStatusButtonALT, mAltitudeData);
		mAltitudeValueDisplay.setText(mAltitudeData.getAltitudeDisplayText());

		// ALTITUDE_NON_SQUISHY_TEXT_VIEW
		// SquishyTextView doesn't work fully so we'll do
		// it manually here
		if (mAltitudeData.showOutOfRangeText()) {
			mAltitudeValueDisplay.setTextSize(160);
			mAltitudeValueDisplay.setTextColor(mAltitudeTextYellow);
		} else if (mAltitudeData.showWarningTextColor()) {
			// OUT_OF_RANGE_METRICS
			mAltitudeValueDisplay.setTextSize(190);
			mAltitudeValueDisplay.setTextColor(mAltitudeTextYellow);
			// ALT mAltitudeValueDisplay.setText("OUT OF RANGE");
			// ALT mAltitudeValueDisplay.setTextSize(60);
			// ALT mAltitudeValueDisplay.setLines(2);

		} else {
			mAltitudeValueDisplay.setTextSize(190);
			mAltitudeValueDisplay.setTextColor(mAltitudeTextWhite);
			// ALT mAltitudeValueDisplay.setLines(1);
			// TESTING mAltitudeValueDisplay.setText("666");
		}
	}

	protected void updateGPSUI() {
		updateStatusButton(mStatusButtonGPS, mGPSData);
		mGroundSpeedValueDisplay.setText(mGPSData.getGroundSpeedDisplayText());
		// todo - average rate of climb
	}

	protected void updateBatteryUI() {
		updateStatusButton(mStatusButtonBAT, mBatteryData);
	}

	protected void updateBoxUI() {
		updateStatusButton(mStatusButtonBOX, mBoxData);
	}

	// STATE
	protected boolean isTransectReady() {
		return (mCurTransect == null) ? false : mCurTransect.isValid();
	}

	// STATE
	protected boolean isLogging() {
		return (mLogger == null) ? false : mLogger.isLogging();
	}

	// STATE
	protected boolean isNavigating() {
		return (mNavigationService == null) ? false : mNavigationService.isNavigating();
	}

	protected void updateStatusRight(boolean toStart) {
		// right status
		if (mFlightData.isFullyReady()) {
			// we expect to be recording the transect
			if (mGPSData.mDataIsValid && !mGPSData.mIgnore && !mGPSData.dataIsExpired()) {
				double metersToNext = toStart ? mNavigationService.calcMetersToStart() : mNavigationService.calcMetersToEnd();
				String distanceString = "??";
				String unitsString = "";

				// PREF_UNITS
				if (metersToNext != NavigationService.METERS_NOT_AVAILABLE) {

					// metersToNext = 3000000.12538491; // 1864
					// metersToNext = 30000.12538491; // 18.6
					// TESTING metersToNext = 3000.12538491; // 18.6
					// TESTING metersToNext = 1619.344; // 1.0 miles
					// TESTING metersToNext = 1699.344; // 1.1 miles
					// TESTING metersToNext = 9656.064; // 6 miles
					// TESTING metersToNext = 1519.344; // 0.94 miles
					// TESTING metersToNext = 15.344; // 0.94 miles

					double distance = GPSUtils.convertMetersToDistanceUnits(metersToNext, mStatusBarDistanceUnits);

					// figure out the decimals for any given unit
					// FEET, METERS, KILOMETERS, MILES, NAUTICAL_MILES
					if (distance >= 5) {
						// 5 and up: no decimals
						distanceString = mDistanceStatusFormatterDot0.format(distance);
					} else if (distance >= 1) {
						// 1-4: 1 decimal
						distanceString = mDistanceStatusFormatterDot1.format(distance);
					} else {
						// 0-1: 2 decimals
						distanceString = mDistanceStatusFormatterDot2.format(distance);
					}

					unitsString = mStatusBarDistanceUnitsDisplayString;
				}

				mStatusDisplayRight.setText((toStart ? "Start" : "Stop") + " in " + distanceString + " " + unitsString);
			} else {
				// right status
				if (mGPSData.mIgnore)
					mStatusDisplayRight.setText("(Ignore GPS)");
				else if (mGPSData.mDemoMode)
					mStatusDisplayRight.setText("8:16 Remaining"); // we don't actuall show this anymore but it works in all demo modes
				else
					mStatusDisplayRight.setText("GPS N/A");
			}
		} else {
			// not recording a transect
			mStatusDisplayRight.setText("No Transect");
		}

		// CROSSTRACK_TESTING_POI - show actual crosstrack
		// mStatusDisplayRight.setText("xTrackErr: " + mGPSData.mRawCrossTrackErrorMeters);
	}

	protected void updateFooterUI() {

		// START/STOP button only reflects the logging state
		
		// STARTUP_SEQUENCE_EVENTS
//		if (mLogger == null)
//			Log.d(LOG_CLASSNAME, "updateFooterUI:  LOGGER NULL");
//		else
//			Log.d(LOG_CLASSNAME, "updateFooterUI:  logger valid, " + (mLogger.isLogging() ? "logging" : "<<NOT LOGGING>>"));


		if (isLogging()) {
			// background
			// COLOR_UPDATE_WIP
			setFooterBackgroundColor2(getResources().getColor(R.color.nav_footer_bg));

			// left status
			mStatusDisplayLeft.setText(R.string.nav_msg_recording_text);
			mStatusDisplayLeft.setTextColor(mStatusTextColorGreen);

			// mode button
			mStartStopButton.setText(isTransectReady() ? R.string.nav_action_stop_transect : R.string.nav_action_stop_logging);
			// EVAL_RED_VS_BLACK mStartStopButton.setBackground(mModeButtonBorderGreen);
			// EVAL_RED_VS_BLACK mStartStopButton.setTextColor(mModeButtonTextColorOnGreen);
			mStartStopButton.setBackground(mModeButtonBorderRed);
			mStartStopButton.setEnabled(true);

			// right status
			updateStatusRight(false);
		} else {
			// background
			// COLOR_UPDATE_WIP
			setFooterBackgroundColor2(getResources().getColor(R.color.nav_footer_red));

			// left status
			mStatusDisplayLeft.setText("Waiting to Start");
			mStatusDisplayLeft.setTextColor(mStatusTextColorRed);
			mStatusDisplayLeft.setTextColor(Color.WHITE);

			// mode button
			mStartStopButton.setText(isTransectReady() ? R.string.nav_action_start_transect : R.string.nav_action_start_logging);

			mStartStopButton.setBackground(mModeButtonBorderGreen);
			mStartStopButton.setTextColor(mModeButtonTextColorOnGreen);
			mStartStopButton.setTextColor(Color.WHITE);
			// EVAL_RED_VS_BLACK mStartStopButton.setBackground(mModeButtonBorderRed);
			// EVAL_RED_VS_BLACK mStartStopButton.setTextColor(mModeButtonTextColorOnRed);
			mStartStopButton.setEnabled(true);

			// right status
			updateStatusRight(true);
			mStatusDisplayRight.setTextColor(Color.WHITE);

		}

		/*
		 * if (isLogging()) { // left status mStatusDisplayLeft.setText(R.string.nav_msg_recording_text); mStatusDisplayLeft.setTextColor(mStatusTextColorGreen);
		 * 
		 * // mode button mStartStopButton.setText(isTransectReady() ? R.string.nav_action_stop_transect : R.string.nav_action_stop_logging); // EVAL_RED_VS_BLACK mStartStopButton.setBackground(mModeButtonBorderGreen); // EVAL_RED_VS_BLACK mStartStopButton.setTextColor(mModeButtonTextColorOnGreen); mStartStopButton.setBackground(mModeButtonBorderRed); mStartStopButton.setTextColor(mModeButtonTextColorOnRed); mStartStopButton.setEnabled(true);
		 * 
		 * // right status updateStatusRight(false); } else { // left status mStatusDisplayLeft.setText("Waiting to Start"); mStatusDisplayLeft.setTextColor(mStatusTextColorRed);
		 * 
		 * // mode button mStartStopButton.setText(isTransectReady() ? R.string.nav_action_start_transect : R.string.nav_action_start_logging); mStartStopButton.setBackground(mModeButtonBorderGreen); mStartStopButton.setTextColor(mModeButtonTextColorOnGreen); // EVAL_RED_VS_BLACK mStartStopButton.setBackground(mModeButtonBorderRed); // EVAL_RED_VS_BLACK mStartStopButton.setTextColor(mModeButtonTextColorOnRed); mStartStopButton.setEnabled(true);
		 * 
		 * // right status updateStatusRight(true); }
		 */
	}

	protected void updateUI() {
		updateRouteUI();
		updateNavigationUI();
		updateAltitudeUI();
		updateGPSUI();
		updateBatteryUI();
		updateBoxUI();
		updateFooterUI();
	}

	protected long curDataTimestamp() {
		return System.currentTimeMillis();
	}

	protected float calcCurAverageAltitude(float curAltitude) {
		if (mAppSettings.mDataAveragingEnabled) {
			int maxHistoryItems = GPSUtils.convertDataAveragingWindowToInteger(mAppSettings.mDataAveragingWindow);
			// add the cur status to the list

			// trim the history if need be
			if (mAltitudeHistory.size() >= maxHistoryItems) {
				Float oldestItem = mAltitudeHistory.get(mAltitudeHistory.size() - 1);

				// remove the oldest from both arrays
				mAltitudeHistory.remove(oldestItem);
				mAltitudeSorted.remove(oldestItem);
			}
			Float curAltitudeFloatObj = Float.valueOf(curAltitude);

			// add the new item to the history (at the front)
			mAltitudeHistory.add(0, curAltitudeFloatObj);

			int n = mAltitudeHistory.size();
			int ns = mAltitudeSorted.size();

			// merge the new item into the sorted list
			boolean inserted = false;

			for (int i = 0; i < ns; i++) {
				Float obj = mAltitudeSorted.get(i);

				if (curAltitude < obj.floatValue()) {
					// winner!
					inserted = true;
					mAltitudeSorted.add(i, curAltitudeFloatObj);
					break;
				}
			}

			if (!inserted) {
				// value was larger than everything in the list... add it to the end
				mAltitudeSorted.add(curAltitudeFloatObj);
			}

			switch (mAppSettings.mDataAveragingMethod) {
			case MEDIAN:
				// take the middle one
				return mAltitudeSorted.get(n / 2).floatValue();

			case MEAN: {
				float avg = 0;

				// sum up the values
				for (int i = 0; i < n; i++) {
					avg += mAltitudeHistory.get(i).floatValue();
				}

				// calc the average
				return avg / (float) n;
			}
			}

		}

		// default
		return curAltitude;
	}

	protected TransectStatus calcCurAverageTransectStatus(TransectStatus curStatus) {
		if (mAppSettings.mDataAveragingEnabled) {
			int maxHistoryItems = GPSUtils.convertDataAveragingWindowToInteger(mAppSettings.mDataAveragingWindow);
			// add the cur status to the list

			// trim the history if need be
			if (mTransectStatusHistory.size() >= maxHistoryItems) {
				TransectStatus oldestItem = mTransectStatusHistory.get(mTransectStatusHistory.size() - 1);

				// remove the oldest from both arrays
				mTransectStatusHistory.remove(oldestItem);
				mTransectStatusSorted.remove(oldestItem);
			}

			// add the new item to the history (at the front)
			mTransectStatusHistory.add(0, curStatus);

			int n = mTransectStatusHistory.size();
			int ns = mTransectStatusSorted.size();

			// merge the new item into the sorted list
			boolean inserted = false;

			for (int i = 0; i < ns; i++) {
				TransectStatus obj = mTransectStatusSorted.get(i);

				if (curStatus.mGroundSpeed < obj.mGroundSpeed) {
					// winner!
					inserted = true;
					mTransectStatusSorted.add(i, curStatus);
					break;
				}
			}

			if (!inserted) {
				// value was larger than everything in the list... add it to the end
				mTransectStatusSorted.add(curStatus);
			}

			switch (mAppSettings.mDataAveragingMethod) {
			case MEDIAN:
				// take the middle one
				// TESTING TransectStatus winnerGS = mTransectStatusSorted.get(n/2);
				// TESTING Log.d(LOG_CLASSNAME, "median gs " + winnerGS.mGroundSpeed + "cur gs " + curStatus.mGroundSpeed + ", n " + n + ", n/2 " + (int)(n/2) + ", value " + mTransectStatusSorted.get(n/2));
				return mTransectStatusSorted.get(n / 2);

			case MEAN: {
				TransectStatus avgStatus = new TransectStatus(curStatus.mTransect, 0, 0, 0, 0);

				// sum up the values
				for (int i = 0; i < n; i++) {
					TransectStatus obj = mTransectStatusHistory.get(i);

					avgStatus.mCrossTrackError += obj.mCrossTrackError;
					avgStatus.mDistanceToEnd += obj.mDistanceToEnd;
					avgStatus.mBearing += obj.mBearing;
					avgStatus.mGroundSpeed += obj.mGroundSpeed;
					avgStatus.mCurrGpsLat += obj.mCurrGpsLat;
					avgStatus.mCurrGpsLon += obj.mCurrGpsLon;
					avgStatus.mCurrGpsAlt += obj.mCurrGpsAlt;
				}

				// calc the averages
				avgStatus.mCrossTrackError /= n;
				avgStatus.mDistanceToEnd /= n;
				avgStatus.mBearing /= n;
				avgStatus.mGroundSpeed /= n;
				avgStatus.mCurrGpsLat /= n;
				avgStatus.mCurrGpsLon /= n;
				avgStatus.mCurrGpsAlt /= n;

				// TESTING Log.d(LOG_CLASSNAME, "average gs " + avgStatus.mGroundSpeed + ", cur gs " + curStatus.mGroundSpeed + ", n " + n);

				return avgStatus;
			}
			}
		}

		// default
		return curStatus;
	}

	public void onAltitudeUpdate(float rawAltitudeInMeters) {
		// rough validation
		// TESTING Log.d("doTimerCallback", "meters" + rawAltitudeInMeters);
		final long timestamp = curDataTimestamp();
		final boolean outOfRange = AltimeterService.valueIsOutOfRange(rawAltitudeInMeters);
		final boolean showOutOfRange = outOfRange && (mLastGoodAltitudeTimestamp != 0) && ((timestamp - mLastGoodAltitudeTimestamp) > SHOW_OUT_OF_RANGE_AFTER_MILLIS);
		final float curAverageAltitude = outOfRange ? mLastGoodAltitudeDatum : calcCurAverageAltitude(rawAltitudeInMeters); // don't average invalid data
		final float currAltitudeInMeters = curAverageAltitude;

		if (!outOfRange) {
			mLastGoodAltitudeDatum = currAltitudeInMeters;
			mLastGoodAltitudeTimestamp = timestamp;
		}

		runOnUiThread(new Runnable() {
			public void run() {
				// update the altitude data (and ui if something changed)
				// note that there's another timer running, so updates
				// will happen even if we don't push them here.

				if (mAltitudeData.setRawAltitudeInMeters(currAltitudeInMeters, true, outOfRange, timestamp, mLastGoodAltitudeDatum, mLastGoodAltitudeTimestamp)) {
					updateAltitudeUI();
					updateNavigationUI();
				}
			}
		});
	}

	@Override
	public void onAltitudeError(String error) {
		// TODO Auto-generated method stub
		updateAltitudeUI();
	}

	public void doTimerCallback() {

		try {
			// TESTING Log.d("doTimerCallback", "updating the ui...");
			updateUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onConnectionEnabled() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionDisabled() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRouteUpdate(TransectStatus status) {
		TransectStatus avgStatus = calcCurAverageTransectStatus(status);

		if (avgStatus != null) {
			final float groundSpeed = avgStatus.mGroundSpeed;
			final double crossTrackErrorMeters = avgStatus.mCrossTrackError;
			final long timestamp = curDataTimestamp();
			final boolean crosstrackValid = avgStatus.isTransectValid();
			runOnUiThread(new Runnable() {
				public void run() {
					// update the altitude data (and ui if something changed)
					if (mGPSData.setRawGroundSpeed(groundSpeed, true, crossTrackErrorMeters, crosstrackValid, timestamp)) {
						updateGPSUI();
						updateNavigationUI();
					}
				}
			});
		}
	}

	public boolean browseGpxFiles(View v) {
		// load gpx
		Intent intent = new Intent(this, CourseSettingsActivity.class);
		intent.putExtra(CourseSettingsActivity.FS_COURSE_DATA_KEY, mFlightData);

		this.startActivityForResult(intent, LOAD_FLIGHT_PATH);

		return true;
	}

	public void onToggleStartStop(View v) {
		if (mLogger != null) {
			if (mLogger.isLogging()) {
				// STOP LOGGING...

				if (isTransectReady()) {
					// stopping the transect -- put up the Cancel/Next/OK dialog
					// TRANSECT_SUMMARY_POI 1
					doAdvanceTransectActivity();
				} else {
					// CONFIRM STOP
					new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.fs_confirm_stop_logging_title).setMessage(R.string.fs_confirm_stop_logging_message).setPositiveButton(R.string.fs_confirm_stop_logging_ok, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							// CONFIRMED
							TransectSummary summary = setLogging(false);

							dialog.cancel();
							updateUI();

							doTransectSummaryDialog(summary);
						}
					}).setNegativeButton(R.string.fs_confirm_stop_logging_cancel, null).show();
				}

			} else {
				// not currently logging -- START LOGGING
				setLogging(true);
			}
		}
	}

	// SAVE_RESTORE_STATE
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(SAVED_FLIGHT_DATA_KEY, mFlightData);
	}
	
	@Override
    protected void initUsbDriver()
	{
		if (mAltimeterService != null)
		{
			mAltimeterService.initSerialCommunication();
		}
	}

	@Override
	public void onLoggingStatusChangeMessage(String statusMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoggingErrorMessage(String errorMessage) {
		this.showConfirmDialog("LoggerError", errorMessage);
	}

	@Override
	public void onTransectLogSummary(TransectSummary summary) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBackPressed() {
		Log.d(LOG_CLASSNAME, "onBackPressed");

		// confirm this with the user
		AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);

		builder.setMessage(R.string.nav_confirm_back_message)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setPositiveButton(R.string.nav_confirm_yes, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                    FlightLogger.this.finish();
	               }
	           })
	           .setNegativeButton(R.string.nav_confirm_no, new DialogInterface.OnClickListener() {
	               public void onClick(DialogInterface dialog, int id) {
	                    dialog.cancel();
	               }
	           });
	    AlertDialog alert = builder.create();
	    alert.show();
	}

}
