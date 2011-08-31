package cgeo.geocaching.files;

import java.io.File;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ArrayAdapter;
import cgeo.geocaching.R;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.activity.AbstractListActivity;

public abstract class FileList<T extends ArrayAdapter<File>> extends AbstractListActivity {

	private ArrayList<File> files = new ArrayList<File>();
	private T adapter = null;
	private ProgressDialog waitDialog = null;
	private loadFiles searchingThread = null;
	private boolean endSearching = false;
	private int listId = 1;
	final private Handler changeWaitDialogHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.obj != null && waitDialog != null) {
				waitDialog.setMessage(res.getString(R.string.file_searching_in) + " " + (String) msg.obj);
			}
		}
	};
	final private Handler loadFilesHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (files == null || files.isEmpty()) {
					if (waitDialog != null) {
						waitDialog.dismiss();
					}

					showToast(res.getString(R.string.file_list_no_files));

					finish();
					return;
				} else {
					if (adapter != null) {
						adapter.notifyDataSetChanged();
					}
				}

				if (waitDialog != null) {
					waitDialog.dismiss();
				}
			} catch (Exception e) {
				if (waitDialog != null) {
					waitDialog.dismiss();
				}
				Log.e(cgSettings.tag, "cgFileList.loadFilesHandler: " + e.toString());
			}
		}
	};
	private String[] extensions;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTheme();
		setContentView(R.layout.gpx);
		setTitle();

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			listId = extras.getInt("list");
		}
		if (listId <= 0) {
			listId = 1;
		}

		setAdapter();

		waitDialog = ProgressDialog.show(
				this,
				res.getString(R.string.file_title_searching),
				res.getString(R.string.file_searching),
				true,
				true,
				new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface arg0) {
						if (searchingThread != null && searchingThread.isAlive()) {
							searchingThread.notifyEnd();
						}
						if (files.isEmpty()) {
							finish();
						}
					}
				}
			);

		endSearching = false;
		searchingThread = new loadFiles();
		searchingThread.start();
	}

	@Override
	public void onResume() {
		super.onResume();

		getSettings().load();
	}

	protected abstract T getAdapter(ArrayList<File> files);

	private void setAdapter() {
		if (adapter == null) {
			adapter = getAdapter(files);
			setListAdapter(adapter);
		}
	}

	/**
	 * Gets the base folder for file searches
	 * @return The folder to start the recursive search in
	 */
	protected abstract String[] getBaseFolders();

	/**
	 * Triggers the deriving class to set the title
	 */
	protected abstract void setTitle();

	private class loadFiles extends Thread {
		public void notifyEnd() {
			endSearching = true;
		}

		@Override
		public void run() {
			ArrayList<File> list = new ArrayList<File>();

			try {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					boolean loaded = false;
					for(String baseFolder : getBaseFolders())
					{
						final File dir = new File(baseFolder);

						if (dir.exists() && dir.isDirectory()) {
							listDir(list, dir);
							if (list.size() > 0) {
								loaded = true;
								break;
							}
						}
					}
					if (!loaded) {
						listDir(list, Environment.getExternalStorageDirectory());
					}
				} else {
					Log.w(cgSettings.tag, "No external media mounted.");
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgFileList.loadFiles.run: " + e.toString());
			}

			final Message msg = new Message();
			msg.obj = "loaded directories";
			changeWaitDialogHandler.sendMessage(msg);

			files.addAll(list);
			list.clear();

			loadFilesHandler.sendMessage(new Message());
		}
	}

	private void listDir(ArrayList<File> result, File directory) {
		if (directory == null || !directory.isDirectory() || !directory.canRead()) {
			return;
		}

		final File[] files = directory.listFiles();

		if (files != null && files.length > 0) {
			for (File file : files) {
				if (endSearching) {
					return;
				}
				if (!file.canRead()) {
					continue;
				}
				String name = file.getName();
				if (file.isFile()) {
					for (String ext : extensions) {
						int extLength = ext.length();
						if (name.length() > extLength && name.substring(name.length() - extLength, name.length()).equalsIgnoreCase(ext)) {
							result.add(file); // add file to list
							break;
						}
					}

				} else if (file.isDirectory()) {
					if (name.charAt(0) == '.') {
						continue; // skip hidden directories
					}
					if (name.length() > 16) {
						name = name.substring(0, 14) + "...";
					}
					final Message msg = new Message();
					msg.obj = name;
					changeWaitDialogHandler.sendMessage(msg);

					listDir(result, file); // go deeper
				}
			}
		}

		return;
	}

	protected FileList(final String extension) {
		setExtensions(new String[] {extension});
	}

	protected FileList(final String[] extensions) {
		setExtensions(extensions);
	}

	private void setExtensions(String[] extensionsIn) {
		for (String extension : extensionsIn) {
			if (extension.length() == 0 || extension.charAt(0) != '.') {
				extension = "." + extension;
			}
		}
		extensions = extensionsIn;
	}
}
