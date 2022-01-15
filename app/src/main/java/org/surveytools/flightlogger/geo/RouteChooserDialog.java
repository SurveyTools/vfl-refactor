package org.surveytools.flightlogger.geo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.fragment.app.DialogFragment;

import org.surveytools.flightlogger.R;

import java.io.File;
import java.util.List;

import org.surveytools.flightlogger.geo.data.Route;

public class RouteChooserDialog extends DialogFragment {

	private static final String TITLE_PARAM_KEY = "title";
	private static final String STYLE_PARAM_KEY = "style";
	private static final String THEME_PARAM_KEY = "theme";

	private static final String GPX_FILE_NAME_STRING_KEY = "gpxfile";
	private static final String ROUTE_NAME_STRING_KEY = "routeName";

	public interface RouteChooserListener {
		void onRouteItemSelected(Route route);
	}

	private String mOriginalRouteName;

	public RouteChooserDialog() {
	}

	public static RouteChooserDialog newInstance(String title, int style, int theme, String gpxFilename, String routeName) {
		RouteChooserDialog dialog = new RouteChooserDialog();
		Bundle bundle = new Bundle();

		// params
		bundle.putString(TITLE_PARAM_KEY, title);
		bundle.putInt(STYLE_PARAM_KEY, style); // e.g. DialogFragment.STYLE_NORMAL
		bundle.putInt(THEME_PARAM_KEY, theme); // e.g. 0

		bundle.putString(GPX_FILE_NAME_STRING_KEY, gpxFilename);
		bundle.putString(ROUTE_NAME_STRING_KEY, routeName);

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

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		String title = getArguments().getString(TITLE_PARAM_KEY);

		builder.setCancelable(true);
		builder.setTitle(title);

		// list
		String gpxFile = getArguments().getString(GPX_FILE_NAME_STRING_KEY);
		mOriginalRouteName = getArguments().getString(ROUTE_NAME_STRING_KEY);

		File gpxFileObj = (gpxFile == null) ? null : new File(gpxFile);
		List<Route> routes = GPSUtils.parseRoute(gpxFileObj);
		final ArrayAdapter<Route> adapter = new ArrayAdapter<Route>(getActivity(), R.layout.route_list_row, R.id.route_name, routes);

		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				Route route = adapter.getItem(which);
				((RouteChooserListener) getActivity()).onRouteItemSelected(route);
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
