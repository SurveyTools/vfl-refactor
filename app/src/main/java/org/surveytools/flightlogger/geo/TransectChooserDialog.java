package org.surveytools.flightlogger.geo;

import androidx.fragment.app.DialogFragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import org.surveytools.flightlogger.R;

import java.io.File;
import java.util.List;

import org.surveytools.flightlogger.AppSettings;
import org.surveytools.flightlogger.geo.data.Route;
import org.surveytools.flightlogger.geo.data.Transect;

public class TransectChooserDialog extends DialogFragment {
	
	private static final String TITLE_PARAM_KEY = "title";
	private static final String STYLE_PARAM_KEY = "style";
	private static final String THEME_PARAM_KEY = "theme";

	private static final String GPX_FILE_NAME_STRING_KEY = "gpxfile";
	private static final String ROUTE_NAME_STRING_KEY = "routeName";
	private static final String TRANSECT_NAME_STRING_KEY = "transectName";
	private static final String TRANSECT_DETAILS_STRING_KEY = "transectDetails";

	public interface TransectChooserListener {
		void onTransectItemSelected(Transect transect);
	}

	private String mOriginalTransectName;
	private String mOriginalTransectDetails;
	
	public TransectChooserDialog() {
	}

	public static TransectChooserDialog newInstance(String title, int style, int theme, String gpxFilename, String routeName, String transectName, String transectDetails) {
		TransectChooserDialog dialog = new TransectChooserDialog();
		Bundle bundle = new Bundle();

		// params
		bundle.putString(TITLE_PARAM_KEY, title);
		bundle.putInt(STYLE_PARAM_KEY, style); // e.g. DialogFragment.STYLE_NORMAL
		bundle.putInt(THEME_PARAM_KEY, theme); // e.g. 0

		bundle.putString(GPX_FILE_NAME_STRING_KEY, gpxFilename);
		bundle.putString(ROUTE_NAME_STRING_KEY, routeName);
		bundle.putString(TRANSECT_NAME_STRING_KEY, transectName);
		bundle.putString(TRANSECT_DETAILS_STRING_KEY, transectDetails);

		dialog.setArguments(bundle);
		return dialog;
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setCancelable(true);
		int style = getArguments().getInt(STYLE_PARAM_KEY);
		int theme = getArguments().getInt(THEME_PARAM_KEY);
		setStyle(style, theme);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// TODO_DIALOG_STYLE_UPDATE AlertDialog.THEME_HOLO_DARK
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		String title = getArguments().getString(TITLE_PARAM_KEY);

		builder.setCancelable(true);
		builder.setTitle(title);

		// list
	    String gpxFile = getArguments().getString(GPX_FILE_NAME_STRING_KEY);
	    String routeName = getArguments().getString(ROUTE_NAME_STRING_KEY);
	    mOriginalTransectName = getArguments().getString(TRANSECT_NAME_STRING_KEY);
	    mOriginalTransectDetails = getArguments().getString(TRANSECT_DETAILS_STRING_KEY);
	    
		File gpxFileObj = (gpxFile == null) ? null : new File(gpxFile);
	    List<Route> routes = GPSUtils.parseRoute(gpxFileObj);
	    
	    // find the route
	    Route route = GPSUtils.findRouteByName(routeName, routes);
		List<Transect> transects = GPSUtils.parseTransects(route, AppSettings.getPrefTransectParsingMethod(getActivity()));

		final ArrayAdapter<Transect> adapter = new ArrayAdapter<Transect>(getActivity(), R.layout.transect_list_row, R.id.transect_name, transects);

		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				Transect transect = adapter.getItem(which);
				((TransectChooserListener) getActivity()).onTransectItemSelected(transect);
			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		return builder.create();
	}
}
