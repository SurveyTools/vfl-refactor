package org.surveytools.flightlogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.surveytools.flightlogger.FileChooserDialog.FileChooserListener;
import org.surveytools.flightlogger.geo.RouteChooserDialog.RouteChooserListener;
import org.surveytools.flightlogger.geo.TransectChooserDialog.TransectChooserListener;
import org.surveytools.flightlogger.geo.GPSUtils;
import org.surveytools.flightlogger.geo.RouteChooserDialog;
import org.surveytools.flightlogger.geo.TransectChooserDialog;
import org.surveytools.flightlogger.geo.data.Route;
import org.surveytools.flightlogger.geo.data.Transect;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;
import android.app.AlertDialog;
import android.content.Intent;
import android.view.View.OnClickListener;

public class CourseSettingsActivity extends FragmentActivity implements OnClickListener, FileChooserListener, RouteChooserListener, TransectChooserListener {

	private View mFileBigButton;
	private View mRouteBigButton;
	private View mTransectBigButton;

	private ImageView mFileIcon;
	private ImageView mRouteIcon;
	private ImageView mTransectIcon;
	
	private TextView mFile;
	private TextView mRoute;
	private TextView mTransect;
	private TextView mTransectsGraphLabel;
	private AllTransectsView mAllTransectsGraph;
	private MiniTransectView mCurTransectMiniGraph;

	private Button mCancelButton;
	private Button mOkButton;

	private CourseInfoIntent mOriginalData;
	private CourseInfoIntent mWorkingData;
	private boolean mAutoOpenShown;

	// objects based on mWorkingData
	private File mCurGpxFile;
	private List<Route> mCurRoutes;		
	private Route mCurRoute;
    private List<Transect> mCurTransects;
    private Transect mCurTransect;
    
	private ArrayList<Transect> mTransectArray;

	private static final String LOGGER_TAG = "CourseSettingsActivity";
	private final int FS_DIALOG_STYLE = DialogFragment.STYLE_NORMAL;
	private final int FS_DIALOG_THEME = android.R.style.Theme_NoTitleBar_Fullscreen;
	public static final String FS_COURSE_DATA_KEY = "FSOriginalData";
	private static final String FS_WORKING_DATA_KEY = "FSWorkingData";
	private static final String FS_AUTO_OPEN_SHOWN_KEY = "FSAutoOpenShown";
	

	protected void immerseMe(String caller) {

		// IMMERSIVE_MODE
		// TESTING Log.i(LOGGER_TAG, "setting the window to immersive and sticky (from " + caller + ")");
		// ref: source code for View
		// also ref ref https://plus.google.com/+MichaelLeahy/posts/CqSCP653UrW

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // don't bump the bottom ok/cancel buttons up even when the 3 buttons are shown
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE // keeps the content laid out correctly, but activity bar still pushes downs
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // View would like its window to be layed out as if it has requested SYSTEM_UI_FLAG_FULLSCREEN, even if it currently hasn't
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // bottom 3 buttons
				| View.SYSTEM_UI_FLAG_FULLSCREEN // top bar (system bar)
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.flight_settings);

		// IMMERSIVE_MODE note: setting the theme here didn't help setTheme(android.R.style.Theme_NoTitleBar);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mFileBigButton = findViewById(R.id.fs_file_big_button);
		mRouteBigButton = findViewById(R.id.fs_route_big_button);
		mTransectBigButton = findViewById(R.id.fs_transect_big_button);
		mAllTransectsGraph = (AllTransectsView) findViewById(R.id.fs_transect_graph);
		mCurTransectMiniGraph = (MiniTransectView) findViewById(R.id.fs_transect_mini_graph);
		mTransectsGraphLabel = (TextView) findViewById(R.id.fs_transect_graph_label);
		
		mCurTransectMiniGraph.setup(getResources().getColor(R.color.transect_graph_active), getResources().getColor(R.color.transect_mini_graph_border), false);

		mFileIcon = (ImageView) findViewById(R.id.fs_file_icon);
		mRouteIcon = (ImageView) findViewById(R.id.fs_route_icon);
		mTransectIcon = (ImageView) findViewById(R.id.fs_transect_icon);

		mFile = (TextView) findViewById(R.id.fs_file_value);
		mRoute = (TextView) findViewById(R.id.fs_route_value);
		mTransect = (TextView) findViewById(R.id.fs_transect_value);

		mCancelButton = (Button) findViewById(R.id.fs_cancel_button);
		mOkButton = (Button) findViewById(R.id.fs_ok_button);

		mAutoOpenShown = false;
		
		// SAVE_RESTORE_STATE
		if (savedInstanceState != null) {
			mOriginalData = savedInstanceState.getParcelable(FS_COURSE_DATA_KEY);
			mWorkingData = savedInstanceState.getParcelable(FS_WORKING_DATA_KEY);
			mAutoOpenShown = savedInstanceState.getBoolean(FS_AUTO_OPEN_SHOWN_KEY);
		} 
		
		if (mOriginalData == null)
			mOriginalData = getIntent().getParcelableExtra(FS_COURSE_DATA_KEY);
		
		if (mWorkingData == null)
			mWorkingData =  new CourseInfoIntent(mOriginalData); // clone;
		
		mWorkingData.debugDump();
		
		updateCurFileFromWorkingData();

		setupButtons();
		setupColors();
		updateDataUI();

		// IMMERSIVE_MODE note: onSystemUiVisibilityChange hook didn't work
		// IMMERSIVE_MODE note: delayed change didn't help: mImmersiveRunnable = new Runnable() { @Override public void run() { immerseMe("runnable delayed"); } };
		// IMMERSIVE_MODE
		immerseMe("dlog OnCreate");

		// auto-open if we have nothing
		// TESTING if (true) return;
		if (!mAutoOpenShown && !mWorkingData.hasFile()) {
			mAutoOpenShown = true;
			doChooseFile();
		}
	}

	// SAVE_RESTORE_STATE
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(FS_COURSE_DATA_KEY, mOriginalData);
		outState.putParcelable(FS_WORKING_DATA_KEY, mWorkingData);
		outState.putBoolean(FS_AUTO_OPEN_SHOWN_KEY, mAutoOpenShown);
	}

	protected void setupButtons() {

		mFileBigButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doChooseFile();
			}
		});

		mRouteBigButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doChooseRoute();
			}
		});

		mTransectBigButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doChooseTransect();
			}
		});

		mCancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finishWithCancel();
			}
		});

		mOkButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finishWithDone();
			}
		});
	}

	@Override
	public void onClick(View v) {
		if (v == mTransectIcon) {
			// TRANSECT
			// TODO_FS_WIP doChooseTransect();
		}
	}
	
	protected void updateTransectListUI() {

		if (mCurTransects != null) {
			// TODO_FS_WIP eval
			mTransectArray = new ArrayList<Transect>(mCurTransects);
		}
	}
	
	protected void updateTransectGraphUI() {

		if (mCurTransects != null) {
			
			mAllTransectsGraph.setTransectList(mTransectArray, mCurTransect, null);
			mCurTransectMiniGraph.setTransect(mCurTransect);
		}
	}
	
	protected void updateDataUI() {
		mFile.setText(mWorkingData.getShortFilename());
		mRoute.setText(mWorkingData.getShortRouteName());
		mTransect.setText(mWorkingData.getFullTransectName());
		
		
		// TODO_FS_WIP int numRoutes = (mCurRoutes == null) ? 0 : mCurRoutes.size();
		// TODO_FS_WIP int numTransects = (mCurTransects == null) ? 0 : mCurTransects.size();
		// TODO_FS_WIP mRouteIcon.setEnabled(numRoutes > 1);
		// TODO_FS_WIP mTransectIcon.setEnabled(numTransects > 1);
		
		updateTransectListUI();
		updateTransectGraphUI();

		String transBase = getResources().getString(R.string.chooser_label_transect_graph);
		if (mTransectArray == null) {
			mTransectsGraphLabel.setText(transBase + ":");
		} else {
			mTransectsGraphLabel.setText(transBase + " (" + mTransectArray.size() + "):");
		}
		
		// disable things as need be
		if (mCurTransect == null) {
			mCurTransectMiniGraph.setVisibility(ImageView.INVISIBLE);
		} else {
			mCurTransectMiniGraph.setVisibility(ImageView.VISIBLE);
		}
		
		mOkButton.setEnabled(mCurGpxFile != null);
}

	private void finishWithCancel() {
		Intent intent = getIntent();
		intent.putExtra(FS_COURSE_DATA_KEY, mOriginalData);
		this.setResult(RESULT_CANCELED, intent);
		finish();
	}

	private void finishWithDone() {
		Intent intent = getIntent();
		intent.putExtra(FS_COURSE_DATA_KEY, mWorkingData);
		this.setResult(RESULT_OK, intent);
		finish();
	}

	private void setColorforViewWithID(int viewID, int colorID) {
		View v = findViewById(viewID);
		try {
			if (v != null)
				v.setBackgroundColor(getResources().getColor(colorID));
		} catch (Exception e) {
			Log.e("FlightSettings", e.getLocalizedMessage());
		}
	}

	private void setClearColorforViewWithID(int viewID) {
		View v = findViewById(viewID);
		if (v != null)
			v.setBackgroundColor(Color.TRANSPARENT);
	}

	protected void setupColors() {

		// override debug colors
		// TESTING flag
		if (true) {

			// whole screen
			setColorforViewWithID(R.id.fs_background_wrapper, R.color.fs_background_color);
			setColorforViewWithID(R.id.fs_header, R.color.fs_header_color);
			setClearColorforViewWithID(R.id.fs_file_and_route_wrapper);
			setClearColorforViewWithID(R.id.fs_transect_stuff_wrapper);
			setClearColorforViewWithID(R.id.fs_transect_graph_blob);
			setClearColorforViewWithID(R.id.fs_transect_stuff_wrapper);
			setClearColorforViewWithID(R.id.fs_route_and_transect_wrapper);
			setColorforViewWithID(R.id.fs_footer, R.color.fs_footer_color);

			// file
			setClearColorforViewWithID(R.id.fs_file_wrapper);
			setClearColorforViewWithID(R.id.fs_file_icon);
			setClearColorforViewWithID(R.id.fs_file_label);
			setClearColorforViewWithID(R.id.fs_file_value);

			// route
			setClearColorforViewWithID(R.id.fs_route_wrapper);
			setClearColorforViewWithID(R.id.fs_route_icon);
			setClearColorforViewWithID(R.id.fs_route_label);
			setClearColorforViewWithID(R.id.fs_route_value);

			// transect
			setClearColorforViewWithID(R.id.fs_transect_wrapper);
			setClearColorforViewWithID(R.id.fs_transect_icon);
			setClearColorforViewWithID(R.id.fs_transect_label);
			setClearColorforViewWithID(R.id.fs_transect_value);
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			immerseMe("onWindowFocusChanged");
		}
	}

    protected void clearCurTransect() {
    	mCurTransect = null;
    }
    
    protected void clearCurRouteAndDependencies() {
    	mCurRoute = null;
    	mCurTransects = null;
    	clearCurTransect();
    }
    
    protected void clearCurFileAndDependencies() {
    	mCurGpxFile = null;
    	mCurRoutes = null;
    	clearCurRouteAndDependencies();
    }
    
	protected void updateCurTransectFromWorkingData() {
		clearCurTransect();
    	if (mCurTransects != null) {
			// get the cur transect
    		if (mWorkingData.hasTransect()) {
    			// get the specified one
    			mCurTransect = GPSUtils.findTransectInList(mWorkingData.mTransectName, mCurTransects);
    		} else {
    			// get the default
    			mCurTransect = GPSUtils.getDefaultTransectFromList(mCurTransects);
    			
    			// update our working data
    			mWorkingData.setTransect(mCurTransect);
    		}
		}
	}

	protected void updateCurRouteFromWorkingData() {
		clearCurRouteAndDependencies();
    	if (mCurRoutes != null) {
			// get the route
    		if (mWorkingData.hasRoute()) {
    			// get the specified one
    			mCurRoute = GPSUtils.findRouteByName(mWorkingData.mRouteName, mCurRoutes);
    		} else {
    			// get the default
    			mCurRoute = GPSUtils.getDefaultRouteFromList(mCurRoutes);
 
    			// update our working data
    			mWorkingData.mRouteName = (mCurRoute == null) ? null : mCurRoute.mName;
    		}

			// get the transects
		    mCurTransects = GPSUtils.parseTransects(mCurRoute, AppSettings.getPrefTransectParsingMethod(this));
		    
		    // cascade
		    updateCurTransectFromWorkingData();
		}
	}

	protected void updateCurFileFromWorkingData() {
		clearCurFileAndDependencies();
		if (mWorkingData.hasFile()) {
			// get the file
			mCurGpxFile = new File(mWorkingData.mGpxName);

			// get the routes
			mCurRoutes = GPSUtils.parseRoute(mCurGpxFile);			
			
			// cascade
			updateCurRouteFromWorkingData();
			
			// note: ok button updated via updateDataUI
		}
	}

	// from a chooser...
	protected void setFile(String gpxFilename) {
		mWorkingData.clearFileDataAndDependencies();
		mWorkingData.mGpxName = gpxFilename;

		updateCurFileFromWorkingData();
		updateDataUI();
	}

	// from a chooser...
	protected void setRoute(String routeName) {
		// file and route list are ok... route and transects need to be updated
		mWorkingData.clearRouteDataAndDependencies();
		mWorkingData.mRouteName = routeName;

		updateCurRouteFromWorkingData();
		updateDataUI();
	}

	// from a chooser...
	protected void setTransect(String transectName, String transectDetails) {
		mWorkingData.setTransectName(transectName, transectDetails);
		updateCurTransectFromWorkingData();
		updateDataUI();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			}
		}
	}

	protected String calcDownloadsDirectoryPath() {
		File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		return dir.toString();
	}

	public void doChooseFile() {
		FragmentManager fm = getSupportFragmentManager();
		String startingDir = calcDownloadsDirectoryPath();
		FileChooserDialog dlog = FileChooserDialog.newInstance("Choose a GPX File", FS_DIALOG_STYLE, FS_DIALOG_THEME, startingDir, ".gpx", 0);
		dlog.show(fm, FileChooserDialog.FILE_CHOOSER_DIALOG_KEY);
		// IMMERSIVE_MODE NOTE: getWindow().getDecorView().postDelayed(mImmersiveRunnable, 500);
	}

	public void doChooseRoute() {
		FragmentManager fm = getSupportFragmentManager();
		RouteChooserDialog dlog = RouteChooserDialog.newInstance("Choose a Route", FS_DIALOG_STYLE,FS_DIALOG_THEME, mWorkingData.mGpxName, mWorkingData.mRouteName);
		dlog.show(fm, "choose_route");
	}

	public void doChooseTransect() {
		FragmentManager fm = getSupportFragmentManager();
		TransectChooserDialog dlog = TransectChooserDialog.newInstance("Choose a Transect", FS_DIALOG_STYLE, FS_DIALOG_THEME, mWorkingData.mGpxName, mWorkingData.mRouteName, mWorkingData.mTransectName, mWorkingData.mTransectDetails);
		dlog.show(fm, "choose_transect");
	}

	// FileChooserListener
	public void onFileItemSelected(String filename) {
		// optional Toast.makeText(this, "file selected " + filename, Toast.LENGTH_SHORT).show();
		setFile(filename);
	}

	// RouteChooserListener
	public void onRouteItemSelected(Route route) {
		// optional Toast.makeText(this, "route selected " + route.mName, Toast.LENGTH_SHORT).show();
		setRoute(route.mName);
	}

	// TransectChooserListener
	public void onTransectItemSelected(Transect transect) {
		// optional Toast.makeText(this, "transect selected " + transect.mName, Toast.LENGTH_SHORT).show();
		setTransect(transect.mName, transect.getDetailsName());
	}
}
