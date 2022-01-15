package org.surveytools.flightlogger;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class USBAwareActivity extends Activity {

	private final String USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
	protected final String LOGGER_TAG = this.getClass().getSimpleName();
	protected boolean mHasInitialized = false;

//	// listens for attachment events
//	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
//
//		public void onReceive(Context context, Intent intent) {
//			if (intent != null)
//			{
//				String action = intent.getAction();
//				Log.d(LOGGER_TAG, "USBAwareActivity recieved " + action);
//				if (USB_DEVICE_ATTACHED.equals(action)) {
//					synchronized (this) {
//						UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//						if (device != null) {
//							initUsbDevice(device);
//						} else {
//							Log.d(LOGGER_TAG, "permission denied for device " + device);
//						}
//					}
//				}
//			}
//		}
//	};

	protected void updateBatteryStatus(Intent batteryStatus) {
		// semi pure virtual
		// TODO - look at a broadcast model instead with the Intent as an object
	}

	// listens for battery events
	private final BroadcastReceiver mBatteryStatusReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
				updateBatteryStatus(intent);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		IntentFilter filter = new IntentFilter(USB_DEVICE_ATTACHED);
//		registerReceiver(mUsbReceiver, filter);

		IntentFilter batteryfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(mBatteryStatusReceiver, batteryfilter);
	}

	/**
	 * Intended to be a template method - the expectation is that in the derived
	 * class we'll do something of interest with the device passed in to this
	 * method
	 * 
	 * @param device
	 */
	protected void initUsbDevice(UsbDevice device) {
		// Log.d(LOGGER_TAG, "init USB device: " + device);
		String deviceInfo = String.format(Locale.US, "Connecting device: %s vendor id: %d prod id: %d", device.getDeviceName(), device.getVendorId(), device.getProductId());
		// Log.d("USBAwareActivity", deviceInfo);
		showToast("Connecting usb device");
		initUsbDriver();
	}

	public void showToast(String message) {
		Context context = getApplicationContext();
		CharSequence text = message;
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, text, duration);
		toast.show();
	}
	
	// overide in subclasses
	protected void initUsbDriver()
    {
		
    }
	
	@Override
    protected void onDestroy()
    {
      //  unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mBatteryStatusReceiver);
        super.onDestroy();
    }

}
