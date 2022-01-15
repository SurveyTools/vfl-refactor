package org.surveytools.flightlogger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;


import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public class FileChooserDialog extends DialogFragment {

	public static final String FILE_CHOOSER_DIALOG_KEY = "gpx_chooser";

	private static final String TITLE_PARAM_KEY = "title";
	private static final String STYLE_PARAM_KEY = "style";
	private static final String THEME_PARAM_KEY = "theme";
	private static final String FILTER_EXTENSION_PARAM_KEY = "filterExtension";

	private static final String BASE_PATH = "base_path";
	private static final String PATH_LEVEL = "path_level";

	public interface FileChooserListener {
		void onFileItemSelected(String filename);
	}

	private static final String TAG = "F_PATH";

	private Item[] fileList;
	private String mBasePath;
	private String mFilterExtension = null; // lowercase
	private int mLevel;
	private File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "");

	public FileChooserDialog() {
	}

	public static FileChooserDialog newInstance(String title, int style, int theme, String basePath, String filterExtension, int level) {
		FileChooserDialog dialog = new FileChooserDialog();
		Bundle bundle = new Bundle();

		// params
		bundle.putString(TITLE_PARAM_KEY, title);
		bundle.putInt(STYLE_PARAM_KEY, style); // e.g. DialogFragment.STYLE_NORMAL
		bundle.putInt(THEME_PARAM_KEY, theme); // e.g. 0
		bundle.putString(BASE_PATH, basePath);
		bundle.putInt(PATH_LEVEL, level);
		bundle.putString(FILTER_EXTENSION_PARAM_KEY, filterExtension);

		dialog.setArguments(bundle);
		return dialog;
	}

	private void setBasePath(String basePath) {
		mBasePath = FilenameUtils.normalizeNoEndSeparator(basePath); // e.g. Environment.getExternalStorageDirectory().getAbsolutePath()
		path = new File(basePath + "");
		loadFileList();
	}

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setCancelable(true);
		int style = getArguments().getInt(STYLE_PARAM_KEY);
		int theme = getArguments().getInt(THEME_PARAM_KEY);
		String basePath = getArguments().getString(BASE_PATH);
		mLevel = getArguments().getInt(PATH_LEVEL);
		String rawExtension = getArguments().getString(FILTER_EXTENSION_PARAM_KEY);
		mFilterExtension = (rawExtension == null) ? null : rawExtension.toLowerCase();
		setStyle(style, theme);
		setBasePath(basePath);

		// IMMERSIVE_MODE note: setting the style here doesn't help
		// setStyle(STYLE_NORMAL, android.R.style.Theme_NoTitleBar_Fullscreen);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		builder.setCancelable(true);
		builder.setTitle(getArguments().getString(TITLE_PARAM_KEY));
		// IMMERSIVE_MODE note: setting style here doesn't hlep
		// IMMERSIVE_MODE note: setting window visibility here doesn't help

		// list
		final ArrayAdapter<Item> freddy = new ArrayAdapter<Item>(getActivity(), android.R.layout.select_dialog_item, android.R.id.text1, fileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// creates view
				View view = super.getView(position, convertView, parent);
				TextView textView = (TextView) view.findViewById(android.R.id.text1);

				// put the image on the text view
				textView.setCompoundDrawablesWithIntrinsicBounds(fileList[position].icon, 0, 0, 0);

				// add margin between image and text (support various screen
				// densities)
				int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
				textView.setCompoundDrawablePadding(dp5);

				return view;
			}
		};

		builder.setAdapter(freddy, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Item chosenItem = freddy.getItem(which);

				String selectedItemPath = mBasePath + "/" + chosenItem.file;

				File sel = new File(selectedItemPath);
				if (sel.isDirectory()) {
					// GO DOWN A LEVEL
					// showChooseRouteDialog()
					FragmentManager fm = getActivity().getSupportFragmentManager();
					FileChooserDialog dlog = FileChooserDialog.newInstance(getArguments().getString(TITLE_PARAM_KEY), getArguments().getInt(STYLE_PARAM_KEY), getArguments().getInt(THEME_PARAM_KEY), selectedItemPath, mFilterExtension, mLevel + 1);

					dlog.show(fm, FILE_CHOOSER_DIALOG_KEY);

				} else if (chosenItem.file.equalsIgnoreCase("up") && !sel.exists() && (mLevel > 0)) {
					// GO UP A LEVEL

					String upPath = FilenameUtils.normalizeNoEndSeparator(FilenameUtils.getPath(mBasePath));

					// showChooseRouteDialog()
					FragmentManager fm = getActivity().getSupportFragmentManager();
					FileChooserDialog dlog = FileChooserDialog.newInstance(getArguments().getString(TITLE_PARAM_KEY), getArguments().getInt(STYLE_PARAM_KEY), getArguments().getInt(THEME_PARAM_KEY), upPath, mFilterExtension, mLevel - 1);

					dlog.show(fm, FILE_CHOOSER_DIALOG_KEY);
				} else {
					// SELECT THE FILE
					((FileChooserListener) getActivity()).onFileItemSelected(selectedItemPath);
				}
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

	private void loadFileList() {
		try {
			path.mkdirs();
		} catch (SecurityException e) {
			Log.e(TAG, "unable to write on the sd card ");
		}

		if (path.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					// Filters based on whether the file, is hidden, has extension, etc.
					
					if ((filename != null) && !sel.isHidden()) {
						if (sel.isFile()) {
							// FILE
							if (mFilterExtension != null) {
								// honor the extension
								return filename.toLowerCase(Locale.US).endsWith(mFilterExtension);
							} else {
								// no extension - take it
								return true;
							}
						} else if (sel.isDirectory()){
							// all directories are ok
							return true;
						}
					}
					
					// default
					return false;
				}
			};

			String[] fList = path.list(filter);
			fileList = new Item[fList.length];
			for (int i = 0; i < fList.length; i++) {
				fileList[i] = new Item(fList[i], R.drawable.file_icon);

				// Convert into file path
				File sel = new File(path, fList[i]);

				// Set drawables
				if (sel.isDirectory()) {
					fileList[i].icon = R.drawable.directory_icon;
					Log.d("DIRECTORY", fileList[i].file);
				} else {
					Log.d("FILE", fileList[i].file);
				}
			}

			if (mLevel > 0) {
				Item temp[] = new Item[fileList.length + 1];
				for (int i = 0; i < fileList.length; i++) {
					temp[i + 1] = fileList[i];
				}
				temp[0] = new Item("Up", R.drawable.directory_up);
				fileList = temp;
			}
		} else {
			Log.e(TAG, "path does not exist");
		}
	}

	private class Item {
		public String file;
		public int icon;

		public Item(String file, Integer icon) {
			this.file = file;
			this.icon = icon;
		}

		@Override
		public String toString() {
			return file;
		}
	}
}
