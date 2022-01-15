package org.surveytools.flightlogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import org.surveytools.flightlogger.geo.TransectChooserDialog.TransectChooserListener;
import org.surveytools.flightlogger.geo.GPSUtils;
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
import android.content.Intent;
import android.view.View.OnClickListener;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class NextTransectActivity extends FragmentActivity implements OnClickListener, TransectChooserListener {

	private View mCurTransectBigButton;
	private View mNextTransectBigButton;

	private ImageView mCurTransectIcon;
	private ImageView mNextTransectIcon;

	private TextView mCurTransectView;
	private TextView mNextTransectView;
	private TextView mTransectsGraphLabel;
	private AllTransectsView mTransectGraph;
	private MiniTransectView mCurTransectMiniGraph;
	private MiniTransectView mNextTransectMiniGraph;

	private Button mCancelButton;
	private Button mStopButton;
	private Button mUseNextTransectButton;

	private CourseInfoIntent mCurTransectData;
	private CourseInfoIntent mNextTransectData;

	// objects based on mWorkingData
	private File mCurGpxFile;
	private List<Route> mCurRoutes;
	private Route mCurRoute;
	private List<Transect> mCurTransects;
	private Transect mCurTransect;
	private Transect mNextTransect;

	private ArrayList<Transect> mTransectArray;

	private static final String LOGGER_TAG = "NextTransectActivity";
	private final int NT_DIALOG_STYLE = DialogFragment.STYLE_NORMAL;
	private final int NT_DIALOG_THEME = android.R.style.Theme_NoTitleBar_Fullscreen;
	public static final String NT_CUR_TRANSECT_DATA_KEY = "NTCurTransectData";
	private static final String NT_NEXT_TRANSECT_DATA_KEY = "NTNextTransectData";

	protected void immerseMe(String caller) {

		// IMMERSIVE_MODE
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

		setContentView(R.layout.next_transect);

		// IMMERSIVE_MODE note: setting the theme here didn't help setTheme(android.R.style.Theme_NoTitleBar);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mCurTransectBigButton = findViewById(R.id.nt_cur_transect_big_button);
		mNextTransectBigButton = findViewById(R.id.nt_next_transect_big_button);
		mTransectGraph = (AllTransectsView) findViewById(R.id.nt_transect_graph);
		mCurTransectMiniGraph = (MiniTransectView) findViewById(R.id.nt_cur_transect_mini_graph);
		mNextTransectMiniGraph = (MiniTransectView) findViewById(R.id.nt_next_transect_mini_graph);

		mCurTransectMiniGraph.setup(getResources().getColor(R.color.transect_graph_active), getResources().getColor(R.color.transect_mini_graph_border), false);
		mNextTransectMiniGraph.setup(getResources().getColor(R.color.transect_graph_next), getResources().getColor(R.color.transect_mini_graph_border), false);

		mCurTransectIcon = (ImageView) findViewById(R.id.nt_cur_transect_icon);
		mNextTransectIcon = (ImageView) findViewById(R.id.nt_next_transect_icon);

		mCurTransectView = (TextView) findViewById(R.id.nt_cur_transect_value);
		mNextTransectView = (TextView) findViewById(R.id.nt_next_transect_value);

		mTransectsGraphLabel = (TextView) findViewById(R.id.nt_transect_graph_label);

		mCancelButton = (Button) findViewById(R.id.nt_cancel_button);
		mStopButton = (Button) findViewById(R.id.nt_stop_button);
		mUseNextTransectButton = (Button) findViewById(R.id.nt_next_button);

		// SAVE_RESTORE_STATE
		if (savedInstanceState != null) {
			mCurTransectData = savedInstanceState.getParcelable(NT_CUR_TRANSECT_DATA_KEY);
			mNextTransectData = savedInstanceState.getParcelable(NT_NEXT_TRANSECT_DATA_KEY);
		}

		if (mCurTransectData == null)
			mCurTransectData = getIntent().getParcelableExtra(NT_CUR_TRANSECT_DATA_KEY);

		if (mNextTransectData == null) {
			mNextTransectData = new CourseInfoIntent(mCurTransectData); // clone;
			mNextTransectData.clearTransectData(); // see NEXT_TRANSECT_DERIVATION
		}

		mNextTransectData.debugDump();

		updateCurFileFromWorkingData();

		setupButtons();
		setupColors();
		updateDataUI();

		// IMMERSIVE_MODE note: onSystemUiVisibilityChange hook didn't work
		// IMMERSIVE_MODE note: delayed change didn't help: mImmersiveRunnable = new Runnable() { @Override public void run() { immerseMe("runnable delayed"); } };
		// IMMERSIVE_MODE
		immerseMe("dlog OnCreate");
	}

	// SAVE_RESTORE_STATE
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(NT_CUR_TRANSECT_DATA_KEY, mCurTransectData);
		outState.putParcelable(NT_NEXT_TRANSECT_DATA_KEY, mNextTransectData);
	}

	protected void setupButtons() {

		mNextTransectBigButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doChooseNextTransect();
			}
		});

		mCancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finishWithCancel();
			}
		});

		mStopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finishWithOk();
			}
		});

		mUseNextTransectButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finishWithUseNextTransect();
			}
		});
	}

	@Override
	public void onClick(View v) {
		if (v == mNextTransectIcon) {
			// TRANSECT
			// TODO_NT_WIP doChooseNextTransect();
		}
	}

	protected void updateTransectListUI() {
		if (mCurTransects != null) {
			mTransectArray = new ArrayList<Transect>(mCurTransects);
		}
	}

	protected void updateTransectGraphUI() {

		if (mCurTransects != null) {
			mTransectGraph.setTransectList(mTransectArray, mCurTransect, mNextTransect);
			mCurTransectMiniGraph.setTransect(mCurTransect);
			mNextTransectMiniGraph.setTransect(mNextTransect);
		}
	}

	protected void updateDataUI() {
		mCurTransectView.setText(mCurTransectData.getFullTransectName());
		mNextTransectView.setText(mNextTransectData.getFullTransectName());

		updateTransectListUI();
		updateTransectGraphUI();

		String transBase = getResources().getString(R.string.chooser_label_transect_graph);
		if (mTransectArray == null) {
			mTransectsGraphLabel.setText(transBase + ":");
		} else {
			mTransectsGraphLabel.setText(transBase + " (" + mTransectArray.size() + "):");
		}

		mCurTransectIcon.setImageAlpha((int) (.6f * 255));

		// always false so we get the colors
		mCurTransectBigButton.setEnabled(false);
		mCurTransectView.setEnabled(false);

		// disable things as need be
		if (mCurTransect == null) {
			mCurTransectMiniGraph.setVisibility(ImageView.INVISIBLE);
		} else {
			mCurTransectMiniGraph.setVisibility(ImageView.VISIBLE);
		}

		// disable things as need be
		if (mNextTransect == null) {
			mNextTransectBigButton.setEnabled(true);
			// alt, disable at end (problematic when jumping around)  mNextTransectBigButton.setEnabled(false);
			
			mNextTransectIcon.setImageAlpha((int) (.6f * 255));
			mNextTransectView.setText("None");
			mNextTransectView.setEnabled(false);
			mNextTransectMiniGraph.setVisibility(ImageView.INVISIBLE);
			mUseNextTransectButton.setEnabled(false);
		} else {
			mNextTransectBigButton.setEnabled(true);
			mNextTransectIcon.setImageAlpha((int) (255));
			mNextTransectView.setEnabled(true);
			mNextTransectMiniGraph.setVisibility(ImageView.VISIBLE);
			mUseNextTransectButton.setEnabled(true);
		}
	}

	private void finishWithCancel() {
		this.setResult(RESULT_CANCELED, getIntent());
		finish();
	}

	private void finishWithOk() {
		// NO_TRANSDATA_MEANS_DONT_CHANGE
		// TRANSECT_SUMMARY_POI 2a
		this.setResult(RESULT_OK, getIntent());
		finish();
	}

	private void finishWithUseNextTransect() {
		Intent intent = getIntent();
		// TRANSECT_SUMMARY_POI 2b
		intent.putExtra(NT_CUR_TRANSECT_DATA_KEY, mNextTransectData);
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
			setColorforViewWithID(R.id.nt_background_wrapper, R.color.nt_background_color);
			setColorforViewWithID(R.id.nt_header, R.color.nt_header_color);
			setClearColorforViewWithID(R.id.nt_transect_stuff_wrapper);
			setClearColorforViewWithID(R.id.nt_transect_graph_blob);
			setClearColorforViewWithID(R.id.nt_transect_stuff_wrapper);
			setClearColorforViewWithID(R.id.nt_route_and_transect_wrapper);
			setColorforViewWithID(R.id.nt_footer, R.color.nt_footer_color);

			// cur transect
			setClearColorforViewWithID(R.id.nt_cur_transect_wrapper);
			setClearColorforViewWithID(R.id.nt_cur_transect_icon);
			setClearColorforViewWithID(R.id.nt_cur_transect_label);
			setClearColorforViewWithID(R.id.nt_cur_transect_value);

			// next transect
			setClearColorforViewWithID(R.id.nt_next_transect_wrapper);
			setClearColorforViewWithID(R.id.nt_next_transect_icon);
			setClearColorforViewWithID(R.id.nt_next_transect_label);
			setClearColorforViewWithID(R.id.nt_next_transect_value);
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
		mNextTransect = null;
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

			// CUR - should always be there, but just in case
			if (mCurTransectData.hasTransect()) {
				// get the specified one
				mCurTransect = GPSUtils.findTransectInList(mCurTransectData.mTransectName, mCurTransects);
			} else {
				// get the default
				mCurTransect = GPSUtils.getDefaultTransectFromList(mCurTransects);

				// update our working data
				mCurTransectData.setTransect(mCurTransect);
			}

			// NEXT
			if (mNextTransectData.hasTransect()) {
				// get the specified one
				mNextTransect = GPSUtils.findTransectInList(mNextTransectData.mTransectName, mCurTransects);
			} else {
				// find the next transect
				mNextTransect = GPSUtils.getNextTransectFromList(mCurTransects, mCurTransect);

				// backfill the data
				mNextTransectData.setTransect(mNextTransect);
			}
		}
	}

	protected void updateCurRouteFromWorkingData() {
		clearCurRouteAndDependencies();
		if (mCurRoutes != null) {
			// get the route
			if (mNextTransectData.hasRoute()) {
				// get the specified one
				mCurRoute = GPSUtils.findRouteByName(mNextTransectData.mRouteName, mCurRoutes);
			} else {
				// get the default
				mCurRoute = GPSUtils.getDefaultRouteFromList(mCurRoutes);

				// update our working data
				mNextTransectData.mRouteName = (mCurRoute == null) ? null : mCurRoute.mName;
			}

			// get the transects
			mCurTransects = GPSUtils.parseTransects(mCurRoute, AppSettings.getPrefTransectParsingMethod(this));

			// cascade
			updateCurTransectFromWorkingData();
		}
	}

	protected void updateCurFileFromWorkingData() {
		clearCurFileAndDependencies();
		if (mNextTransectData.hasFile()) {
			// get the file
			mCurGpxFile = new File(mNextTransectData.mGpxName);

			// get the routes
			mCurRoutes = GPSUtils.parseRoute(mCurGpxFile);

			// cascade
			updateCurRouteFromWorkingData();
		}
	}

	// from a chooser...
	protected void setNextTransect(String transectName, String transectDetails) {
		mNextTransectData.setTransectName(transectName, transectDetails);
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

	public void doChooseNextTransect() {
		FragmentManager fm = getSupportFragmentManager();
		TransectChooserDialog dlog = TransectChooserDialog.newInstance("Choose a Transect", NT_DIALOG_STYLE, NT_DIALOG_THEME, mNextTransectData.mGpxName, mNextTransectData.mRouteName, mNextTransectData.mTransectName, mNextTransectData.mTransectDetails);
		dlog.show(fm, "choose_transect");
	}

	// TransectChooserListener
	public void onTransectItemSelected(Transect transect) {
		// optional Toast.makeText(this, "next transect selected " + transect.mName, Toast.LENGTH_SHORT).show();
		setNextTransect(transect.mName, transect.getDetailsName());
	}
}
