package org.surveytools.flightlogger;

import java.util.ArrayList;

import org.surveytools.flightlogger.geo.data.Transect;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.surveytools.flightlogger.R;

public class TransectAdapter extends ArrayAdapter<Transect> {
	public TransectAdapter(Context context, ArrayList<Transect> transectList) {
		super(context, R.layout.fs_transect_list_row, transectList);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		 // data
		Transect transect = getItem(position);
		boolean checked = ((ListView)parent).isItemChecked(position);

		// resuse
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.fs_transect_list_row, parent, false);
		}

		// get the ui items
		ImageView arrow = (ImageView) convertView.findViewById(R.id.fs_transect_row_arrow);
		ImageView icon = (ImageView) convertView.findViewById(R.id.fs_transect_row_icon);
		TextView name = (TextView) convertView.findViewById(R.id.fs_transect_row_name);
		TextView state = (TextView) convertView.findViewById(R.id.fs_transect_row_state);

		// name
		name.setText(transect.getFullName());

		// state
		state.setText(null);

		// checked (kind of a hack)
		// TODO_FS_WIP, optional checkBox.setChecked(checked);

		if (checked) {
			// TODO_FS_WIP, optional state.setText("* CURRENT *");
			arrow.setVisibility(View.VISIBLE);
		}
		else {
			state.setText(null);
			arrow.setVisibility(View.INVISIBLE);
		}
		
		/* TODO_FS_WIP honor status 
		if (transect.status == null) {
			// no flight status - base it on the position
		} else {
			// honor the flight status
			switch (statusToUse) {
			case NOT_ACTIVATED:
				state.setText(null);
				break;

			case NAVIGATE_TO:
				state.setText("Navigate To");
				break;

			case IN_FLIGHT:
				state.setText("In Flight");
				break;

			case COMPLETED:
				state.setText("DONE");
				break;
			}
		}
			*/

		return convertView;
	}
}
