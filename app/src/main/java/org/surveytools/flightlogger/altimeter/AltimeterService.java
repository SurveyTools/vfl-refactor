package org.surveytools.flightlogger.altimeter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.surveytools.flightlogger.AppSettings;
import org.surveytools.flightlogger.geo.GPSUtils;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import slickdevlabs.apps.usb2seriallib.AdapterConnectionListener;
import slickdevlabs.apps.usb2seriallib.SlickUSB2Serial;
import slickdevlabs.apps.usb2seriallib.USB2SerialAdapter;
import slickdevlabs.apps.usb2seriallib.SlickUSB2Serial.BaudRate;
import slickdevlabs.apps.usb2seriallib.SlickUSB2Serial.DataBits;
import slickdevlabs.apps.usb2seriallib.SlickUSB2Serial.ParityOption;
import slickdevlabs.apps.usb2seriallib.SlickUSB2Serial.StopBits;

public class AltimeterService extends Service implements
		AdapterConnectionListener, USB2SerialAdapter.DataListener {
	
	public enum RangefinderDriverType {
		AGLASER, LIGHTWARE, LIGHTWARE_SF30 
	}
	
	private static final long ALT_RESPONSE_TIMEOUT_MILLIS = 5 * 1000;
	private static final long NANOS_PER_MILLI = 1000000l;
	public static final int LIGHTWARE_SF03_REV2_PRODUCT_ID = 24577; // x6001 - pre 6/2015 model
	public static final int LIGHTWARE_SF03_PRODUCT_ID = 24597; // x6015 - newer model based on SF30 electronics
	public static final int PROLIFIC_PRODUCT_ID = 8200;
	public static final int DATA_SAMPLE_SIZE = 50;
	
	// used for mock data, assume a range of 300 ft +/- 20
	public final float MOCK_MAX_TOTAL_DELTA = 40/GPSUtils.FEET_PER_METER;
	public final float MOCK_DELTA_ALT = 2/GPSUtils.FEET_PER_METER;
	public final float MOCK_TARGET_ALT = 300/GPSUtils.FEET_PER_METER;
	public static final float ALTIMETER_OUT_OF_RANGE_THRESHOLD = 99999f; // AgLaser uses 99999.99.  SEE ALTIMETER_PASSES_99999_FOR_OUT_OF_RANGE_DATA
	public static final float LASER_OUT_OF_RANGE = 99999.99f; // Make sure lightware and aglaser validators report the same error value

	// how many samples for an alt avg.
	public static final String USE_MOCK_DATA = "useMockData";
	private final int ALT_SAMPLE_COUNT = 5;
	private float mCurrentAltitudeInMeters;
	private boolean mGenMockData = false;
	private boolean mIsConnected = false;
	private long mLastAltUpdateNanos = 0l;
	
	private Handler mAltResponseHandler;	

	// TODO sample altitude
	private int[] mAltSample;
	// encapsulates driver setting deltas
	private AltimeterValidator mDataValidator;

	private final String LOGGER_TAG = AltimeterService.class.getSimpleName();

	private final IBinder mBinder = new LocalBinder();
	private final ArrayList<AltitudeUpdateListener> mListeners = new ArrayList<AltitudeUpdateListener>();

	private USB2SerialAdapter mSelectedAdapter;

	public class LocalBinder extends Binder {
		public AltimeterService getService() {
			return AltimeterService.this;
		}
	}

	@Override
	// called when bound to an activity
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// called when the service is unbound (runs independent of activity)
	// TODO - this can be called multiple times (service may be wacked by OS
	// add guards for that scenario
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null)
		{
			// generate mock data if the intent calls for it
			boolean useMockData = intent.getBooleanExtra(USE_MOCK_DATA, false);
			if (useMockData) {
				mGenMockData = true;
				mCurrentAltitudeInMeters = MOCK_TARGET_ALT;
				generateMockData();
			} else {
				mAltSample = new int[ALT_SAMPLE_COUNT];
			}
		}
		//TESTING Log.d(LOGGER_TAG, ">> starting altimeter service");
		return START_STICKY;
	}

	public boolean stopService(Intent intent) {
		mGenMockData = false;
		return super.stopService(intent);
	}

	public void generateMockData() {
		final Random rand = new Random();

		new Thread() {
			public void run() {
				while (mGenMockData == true) {
					
					// flow up and down
					mCurrentAltitudeInMeters +=  ((rand.nextFloat() * MOCK_DELTA_ALT)) - (MOCK_DELTA_ALT / 2.0f);
					
					// pin it in the range
					if (mCurrentAltitudeInMeters > (MOCK_TARGET_ALT + MOCK_MAX_TOTAL_DELTA))
						mCurrentAltitudeInMeters = MOCK_TARGET_ALT + MOCK_MAX_TOTAL_DELTA;
					else if (mCurrentAltitudeInMeters < (MOCK_TARGET_ALT - MOCK_MAX_TOTAL_DELTA))
						mCurrentAltitudeInMeters = MOCK_TARGET_ALT - MOCK_MAX_TOTAL_DELTA;
								
					// TESTING mCurrentAltitudeInMeters = MOCK_TARGET_ALT + MOCK_MAX_TOTAL_DELTA / 2.0f;
					
					sendAltitudeUpdate();
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	// called once at instantiation
	public void onCreate() {
		super.onCreate();

		mAltResponseHandler = new Handler();
		Runnable runnable = new Runnable() {
			   @Override
			   public void run() {
			      mAltResponseHandler.postDelayed(this, ALT_RESPONSE_TIMEOUT_MILLIS);
			      checkAltimeterConnectionHealth();
			   }
			};
		new Thread(runnable).start();
	}
	
	private void checkAltimeterConnectionHealth() {
		if(mLastAltUpdateNanos > 0)
		{
			long currTime = System.nanoTime();
			long lastUpdateNanos = currTime - mLastAltUpdateNanos;
			//TESTING Log.d("checkAltimeterConnectionHealth", ">> currTime: " + currTime + " lastUpdateNanos: " + lastUpdateNanos);
			if(lastUpdateNanos > ALT_RESPONSE_TIMEOUT_MILLIS * NANOS_PER_MILLI)
			{
				Log.d(this.getClass().getName(), "checkAltimeterConnectionHealth >> No data recieved recently, reinitializing serial driver");
				initSerialCommunication();
			}
		}
		
	}

	// TODO Since we have generics support, we should refactor all the 
	// service listener stuff into a base class if we run out of things to do...
	public void registerListener(AltitudeUpdateListener listener) {
		mListeners.add(listener);
	}

	public void unregisterListener(AltitudeUpdateListener listener) {
		mListeners.remove(listener);
	}

	public void initSerialCommunication() {
		//TESTING Log.d("initSerialCommunications", ">> Initializing serial driver");
		
		SlickUSB2Serial.initialize(this);
		
		RangefinderDriverType driverType = AppSettings.getPrefRangefinderDriverType(this);
		if (driverType == RangefinderDriverType.LIGHTWARE || driverType == RangefinderDriverType.LIGHTWARE_SF30)
		{
			//TESTING Log.d("initSerialCommunication", ">> Initializing with Lightware device driver");
			SlickUSB2Serial.connectFTDI(AltimeterService.this);
		}
		else
		{
			//TESTING Log.d("initSerialCommunication", ">> Initializing with AgLaser device driver");
			SlickUSB2Serial.connectProlific(AltimeterService.this);
		}
		
		// we start the watchdog clock when we initialize...
		mLastAltUpdateNanos = System.nanoTime();	
	}

	@Override
	public void onDataReceived(int arg0, byte[] data) {
		int end = data.length > DATA_SAMPLE_SIZE ? DATA_SAMPLE_SIZE : data.length;
		byte[] sampleData = Arrays.copyOfRange(data, 0, end);
		
		if (validateDataPayload(sampleData)) {
			mLastAltUpdateNanos = System.nanoTime();
			//TESTING Log.d("onDataRecieved", ">> Last update: " + mCurrentAltitudeInMeters);
			sendAltitudeUpdate();
		}
	}

	public void sendAltitudeUpdate() {
		for (AltitudeUpdateListener listener : mListeners) {
			listener.onAltitudeUpdate(mCurrentAltitudeInMeters);
		}
	}
	
	public boolean isConnected()
	{
		return mIsConnected;
	}

	public static boolean valueIsOutOfRange(float v) {
		return v >= ALTIMETER_OUT_OF_RANGE_THRESHOLD;
	}
	
	private boolean validateDataPayload(byte[] data) {
		// verify that the carriage return is the terminating character
		boolean isValid = false;
		
		float meters = mDataValidator.parseDataPayload(data);
		
		if(meters > 0) 
		{
			isValid = true;
			mLastAltUpdateNanos = System.nanoTime();
			mCurrentAltitudeInMeters = meters;
		} 
		 
		return isValid;
	}

	@Override
	public void onAdapterConnected(USB2SerialAdapter adapter) {

		BaudRate adapterRate;
		RangefinderDriverType driverType = AppSettings.getPrefRangefinderDriverType(this);
		if (driverType == RangefinderDriverType.LIGHTWARE)
		{
			//TESTING 
			Log.d(LOGGER_TAG, ">> Connecting to Lightware SF03/XLR");
			mDataValidator = new LightwareSF03DataValidator();
			adapterRate = BaudRate.BAUD_460800;
		}
		else if (driverType == RangefinderDriverType.LIGHTWARE_SF30)
		{
			//TESTING 
			Log.d(LOGGER_TAG, ">> Connecting to Lightware SF30/XLR");
			mDataValidator = new LightwareSF30DataValidator();
			adapterRate = BaudRate.BAUD_115200; 
		}
		else //assume its a prolific driver
		{
			//TESTING Log.d(LOGGER_TAG, ">> Connecting AgLaser with adapter " + adapter.toString());
			mDataValidator = new AglaserDataValidator();
			adapterRate = BaudRate.BAUD_9600;
		}
		adapter.setDataListener(this);
		mIsConnected = true;
		mSelectedAdapter = adapter;
		mSelectedAdapter.setCommSettings(adapterRate,
				DataBits.DATA_8_BIT, ParityOption.PARITY_NONE,
				StopBits.STOP_1_BIT);

	}
	
	

	@Override
	public void onAdapterConnectionError(int arg0, String errMsg) {
		mIsConnected = false;
		//TESTING Log.d("AltimeterService", ">> Error connecting: " + errMsg);
		for (AltitudeUpdateListener listener : mListeners) {
			listener.onAltitudeError(errMsg);
		}
		
		SlickUSB2Serial.cleanup(this);

	}

	// TODO - record the five last samples, and assign the average to
	// the current altitude value
	public int sampleAltitude() {
		// use System.arraycopy with most recent 4 values
		return 0;
	}

}
