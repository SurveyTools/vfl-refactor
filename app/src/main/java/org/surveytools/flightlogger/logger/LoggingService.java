package org.surveytools.flightlogger.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import org.surveytools.flightlogger.altimeter.AltimeterService;
import org.surveytools.flightlogger.altimeter.AltitudeUpdateListener;
import org.surveytools.flightlogger.geo.NavigationService;
import org.surveytools.flightlogger.geo.TransectUpdateListener;
import org.surveytools.flightlogger.geo.data.Transect;
import org.surveytools.flightlogger.geo.data.TransectStatus;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class LoggingService extends Service implements AltitudeUpdateListener,
		TransectUpdateListener, SensorEventListener {
	private static final long LOGGING_FREQUENCY_SECS = 1;
	private final String mLoggingBaseDirName = "flightlog";
	private final String mGlobalLogname = "flightlog.gpx";
	private final String mTransectLogname = "transectlog.csv";
	private LogFormatter mLogFormatter;
	private File mLogDir = null;
	protected final String TAG = this.getClass().getSimpleName();
	private File mTransectLogfile;
	private File mGlobalFlightLog;
	private LogEntry mCurrLogEntry;
	private boolean mLogTransectData;
	private boolean mLogFlightData;
	private String mCurrTransectName;
	private TransectStats mCurrStats;

	//sensor data
	private SensorManager mSensorManager;
	Sensor mAccelerometer;
	Sensor mMagnetometer;
	private float[] mGravityData;
	private float[] mGeomagneticData;

	protected NavigationService mNavigationService;
	protected AltimeterService mAltimeterService;
	
	private final ArrayList<LoggingStatusListener> mListeners = new ArrayList<LoggingStatusListener>();

	public static final SimpleDateFormat ISO_8601_DATE_TIME_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

	// references to the services consumed
	private ServiceConnection mNavigationConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			NavigationService.LocalBinder binder = (NavigationService.LocalBinder) service;
			mNavigationService = (NavigationService) binder.getService();
			mNavigationService.registerListener(LoggingService.this);
			
			// Register the sensor listeners here, as we always need GPS service
		    // mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		    // mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		    // mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		    
		   // mSensorManager.registerListener(LoggingService.this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
		   // mSensorManager.registerListener(LoggingService.this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
		 
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mNavigationService.unregisterListener(LoggingService.this);
			mSensorManager.unregisterListener(LoggingService.this, mAccelerometer);
			mSensorManager.unregisterListener(LoggingService.this, mMagnetometer);
		}
	};

	private ServiceConnection mAltimeterConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			AltimeterService.LocalBinder binder = (AltimeterService.LocalBinder) service;
			mAltimeterService = (AltimeterService) binder.getService();
			mAltimeterService.registerListener(LoggingService.this);
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mAltimeterService.unregisterListener(LoggingService.this);
		}
	};

	private void bindServices() {
		if (mAltimeterService == null) {
			Intent intent = new Intent(this, AltimeterService.class);
			this.bindService(intent, mAltimeterConnection, 0);
		}
		if (mNavigationService == null) {
			Intent intent2 = new Intent(this, NavigationService.class);
			this.bindService(intent2, mNavigationConnection, 0);
		}
	}
	
	private final IBinder mBinder = new LocalBinder();
	
	@Override
    public void onCreate() {
		Log.d(this.getClass().getName(), "logging service started");
        // code to execute when the service is first created
    }

	@Override
	// called when bound to an activity
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public void onRouteUpdate(TransectStatus status) {

	}

	public class LocalBinder extends Binder {
		public LoggingService getService() {
			return LoggingService.this;
		}
	}

	public void startTransectLog(Transect transect) {
		String transectName = (transect == null) ? "no-transect-specified" : transect.calcBaseFilename();
		mLogTransectData = true;
		mCurrTransectName = transectName;
		// DONT_USE_TRANSECT_LOG_BEFORE_ITS_CREATED
		// if this thread gets delayed and writeLogEntries comes in with data... we crash
		mCurrStats = new TransectStats(mCurrTransectName, transect);
		Log.d(TAG, "startTransectLog!!!");
	}
	
	public void stopLogging()
	{
		Log.d(TAG, "stop logging");
		
		stopTransectLog(false);
		stopFlightLog();	
		makeFilesVisible(new String[] {mGlobalFlightLog.getAbsolutePath(), mTransectLogfile.getAbsolutePath()});
	}
	
	public boolean resetLogging()
	{
		Log.d(TAG, "reset logging");
		writeLogEntry(mGlobalFlightLog, GPXLogConverter.GPX_FOOTER);
		makeFilesVisible(new String[] {mGlobalFlightLog.getAbsolutePath(), this.mTransectLogfile.getAbsolutePath()});
		setupLogs();
		return (mGlobalFlightLog != null);
	}
	
	public File rotateLogs()
	{
		Log.d(TAG, "rotate logs");
		writeLogEntry(mGlobalFlightLog, GPXLogConverter.GPX_FOOTER);
		File rotated = rotateFlightlogs();
		File[] logFiles = rotated.listFiles();
		if (logFiles.length == 2)
			makeFilesVisible(new String[] {logFiles[0].getAbsolutePath(), logFiles[1].getAbsolutePath()});
		setupLogs();
		return rotated;
	}
	
	private void setupLogs() 
	{
		Log.d(TAG, "creating new flightlogs");
		
		createFlightLogDirectory();
		createFlightLog();
		createTransectLog();
	}
	
	public void stopFlightLog() 
	{
		Log.d(TAG, "stop flightlog");
		if (mGlobalFlightLog != null)
		{
			Log.d(TAG, "closing flightlog");
			writeLogEntry(mGlobalFlightLog, GPXLogConverter.GPX_FOOTER);
		}
		mLogFlightData = false;
		mLogDir = null;	
	}
	
	public TransectSummary stopTransectLog() 
	{
		return stopTransectLog(true);
	}
	
	public TransectSummary stopTransectLog(boolean doSummary) 
	{
		Log.d(TAG, "stop transect log");
		TransectSummary summary = null;
				
		if (mTransectLogfile != null)
		{
			mLogTransectData = false;
			this.mCurrTransectName = "";
			if (doSummary && mCurrStats != null)
			{
				summary = mCurrStats.getTransectSummary();
			}
		}
		return summary;
	}
	
    public void registerListener(LoggingStatusListener listener) {
        mListeners.add(listener);
    }

    public void unregisterListener(LoggingStatusListener listener) {
        mListeners.remove(listener);
    }
	
	// convert contents referenced by an immutable File 
	public void convertLogToGPXFormat(final File currLog) {
		if(currLog.canRead())
		{
			new Thread() {
				public void run() {
					String gpxLog = FilenameUtils.removeExtension(currLog.getName()) + ".gpx";
					File gpxFile = createLogFile(gpxLog, "");
					try {
						final FileInputStream fis = new FileInputStream(currLog);
						final FileOutputStream fos = new FileOutputStream(gpxFile);
						final GPXLogConverter gpxLogger = new GPXLogConverter();
						gpxLogger.writeGPXFile(fis, fos);					
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	public boolean isLogStarted()
	{
		return mLogFlightData;
	}

	public boolean isLogging() {
		return mLogTransectData && (mTransectLogfile != null);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent!= null)
		{
			if (mLogFormatter == null)
				mLogFormatter = new LogFormatter();
			Log.d(TAG, "starting logging service");
			bindServices();
			if (isLogStarted() == false)
			{
				setupLogs();
				logFlightData(LOGGING_FREQUENCY_SECS);
			}
		}
		return START_STICKY;
	}

	public boolean stopService(Intent intent) {
		Log.d(TAG, "stopping logging service");
		return super.stopService(intent);
	}
	
	// TODO - If needed, consider write into a buffer, and flush it every 20 entries or so.
	private void logFlightData(long logFrequencySecs) {
		final long logFrequencyMillis = logFrequencySecs * 1000;

		mLogFlightData = true;			
		mCurrLogEntry = new LogEntry();
		
		new Thread() {
			public void run() {
				while (mLogFlightData) {
					try {
						LogEntry entry;
						synchronized (mCurrLogEntry) {
							// synchronized copy constructor to keep it atomic
							entry = new LogEntry(mCurrLogEntry);
							mCurrLogEntry.clearEntry();
						}
						String entryTime = ISO_8601_DATE_TIME_FORMAT.format(new Date());
						writeLogEntries(entry, entryTime);
						Thread.sleep(logFrequencyMillis);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						// mLogFightData = false;
					}
				}
			}
		}.start();
	}

	private void writeLogEntries(LogEntry entry, String timestamp) {
		if (entry.isValidEntry())
		{
			String flightlogEntry = mLogFormatter.writeGPXFlightlogRecord(timestamp, entry);
			writeLogEntry(this.mGlobalFlightLog, flightlogEntry);
			if (isLogging() == true)
			{
				// DONT_USE_TRANSECT_LOG_BEFORE_ITS_CREATED (fixes crash bug)
				if (mCurrStats == null) {
					Log.d(TAG, "mCurrStats null - not averaging this entry");
				} else {
					mCurrStats.addTransectStat(entry);
				}
				
				String transectEntry = mLogFormatter.writeGenericCSVRecord(
						timestamp,
						mCurrTransectName,
						Double.toString(entry.mLat),
						Double.toString(entry.mLon), 
						Float.toString(entry.mAlt),					
						Double.toString(entry.mGpsAlt),
						Float.toString(entry.mSpeed));
				writeLogEntry(this.mTransectLogfile, transectEntry);
			}
		}
	}
	
	private void writeLogEntry(File logName, String entry) 
	{
		try {
		        FileOutputStream fos = new FileOutputStream(logName, true);
		        PrintStream writer = new PrintStream(fos);
		        writer.append(entry);
		        writer.flush();
		        writer.close();
		    }  catch (IOException e) {
		    	Log.e(TAG, "Log write failed: " + e.toString());
		    }
	}
	
	private File rotateFlightlogs()
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-k.m.s", Locale.US);
		String logName = String.format("%s-%s", mLoggingBaseDirName, sdf.format(cal.getTime()));
		
		String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + logName;
		File rotateDir = new File(dirPath);
		rotateDir.mkdirs();
		File[] logs = mLogDir.listFiles();
		if (logs != null)
		{
			for (File log:logs)
			{
				log.renameTo(new File(rotateDir, log.getName()));
			}
		}
		return rotateDir;	
	}
	
	private void createTransectLog() 
	{
		String csvHeader = mLogFormatter.writeTransectColumnTitles();
		mTransectLogfile = createLogFile(mTransectLogname, csvHeader);	
	}
	
	private void createFlightLog() 
	{
		mGlobalFlightLog = createLogFile(mGlobalLogname, GPXLogConverter.GPX_HEADER);
	}

	private File createLogFile(String logName, String header) 
	{
		File logFile = null;
		logName = FilenameUtils.normalize(logName);

		if (mLogDir.isDirectory() && mLogDir.canWrite()) {
			logFile = new File(mLogDir, logName);
			if (!logFile.exists()) {
				try {
					logFile.createNewFile();
					writeLogEntry(logFile, header);
				} catch (IOException e) {
					Log.e(TAG, e.getLocalizedMessage());
				}
			}
			makeFilesVisible(new String[] {logFile.toString()});
		}
		return logFile;
	}

	private boolean createFlightLogDirectory() {
//		Calendar cal = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-k.m.s", Locale.US);
//		String logName = String.format("%s-%s", mLoggingBaseDirName, sdf.format(cal.getTime()));
		
		String dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + File.separator + mLoggingBaseDirName;
		File flightLogDir = new File(dirPath);
		flightLogDir.mkdirs();
		mLogDir = flightLogDir;
		return flightLogDir.exists();
	}

	@Override
	public void onAltitudeUpdate(float altValueInMeters) {
		// note: we get altitude updates when we're not logging
		if (mCurrLogEntry != null) {
			if (!AltimeterService.valueIsOutOfRange(altValueInMeters))
				this.mCurrLogEntry.mAlt = altValueInMeters;
		}
	}
	
	@Override
	public void onAltitudeError(String error) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionEnabled() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionDisabled() {
		// TODO Auto-generated method stub

	}
	
	public void onAccuracyChanged(Sensor sensor, int accuracy) {  }
	
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravityData = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagneticData = event.values;
		if (mGravityData != null && mGeomagneticData != null) 
		{
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravityData, mGeomagneticData);
			if (success) 
			{
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				float azimuth = orientation[0]; // orientation contains: azimut, pitch and roll
				float pitch = orientation[1];
				float roll = orientation[2];
			}
		}
		// work on data
	}
	
	private void makeFilesVisible(String[] filePaths)
	{
		MediaScannerConnection.scanFile(this, filePaths, null, null);
	}

}
