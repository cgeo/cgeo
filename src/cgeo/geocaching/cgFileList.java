package cgeo.geocaching;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

public abstract class cgFileList<T extends ArrayAdapter<File>> extends ListActivity {

	private ArrayList<File> files = new ArrayList<File>();
	private cgeoapplication app = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private Activity activity = null;
	private T adapter = null;
	private ProgressDialog waitDialog = null;
	private Resources res = null;
	private loadFiles searchingThread = null;
	private boolean endSearching = false;
	private int listId = 1;
	final private Handler changeWaitDialogHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.obj != null && waitDialog != null) {
				waitDialog.setMessage(getRes().getString(R.string.file_searching_in) + " " + (String) msg.obj);
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

					getWarning().showToast(getRes().getString(R.string.file_list_no_files));

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
		res = this.getResources();
		app = (cgeoapplication) this.getApplication();
		settings = new cgSettings(this, getSharedPreferences(cgSettings.preferences, 0));
		base = new cgBase(getApp(), getSettings(), getSharedPreferences(cgSettings.preferences, 0));
		warning = new cgWarning(this);

		// set layout
		if (getSettings().skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.gpx);
		getBase().setTitle(getActivity(), getRes().getString(R.string.gpx_import_title));

		// google analytics
		getBase().sendAnal(getActivity(), "/file-import");

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
				getRes().getString(R.string.file_title_searching),
				getRes().getString(R.string.file_searching),
				true,
				true,
				new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface arg0) {
						if (searchingThread != null && searchingThread.isAlive()) {
							searchingThread.notifyEnd();
						}
						if (files.isEmpty() == true) {
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
	
	final protected cgSettings getSettings() {
		return settings;
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
	
	private class loadFiles extends Thread {
		public void notifyEnd() {
			endSearching = true;
		}

		@Override
		public void run() {
			ArrayList<File> list = new ArrayList<File>();

			try {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) == true) {
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

	/**
	 * Get the file extension to search for
	 * @return The file extension 
	 */
	protected abstract String getFileExtension();

	private void listDir(ArrayList<File> list, File directory) {
		if (directory == null || directory.isDirectory() == false || directory.canRead() == false) {
			return;
		}

		final File[] listPre = directory.listFiles();
		String fileExt = getFileExtension();

		if (listPre != null && listPre.length > 0) {
			final int listCnt = listPre.length;

			for (int i = 0; i < listCnt; i++) {
				if (endSearching == true) {
					return;
				}

				if (listPre[i].canRead() == true && listPre[i].isFile() == true) {
					final String[] nameParts = listPre[i].getName().split("\\.");
					if (nameParts.length > 1) {
						final String extension = nameParts[(nameParts.length - 1)].toLowerCase();

						if (extension.equals(fileExt) == false) {
							continue;
						}
					} else {
						continue; // file has no extension
					}

					list.add(listPre[i]); // add file to list
				} else if (listPre[i].canRead() == true && listPre[i].isDirectory() == true) {
					final Message msg = new Message();
					String name = listPre[i].getName();
					if (name.substring(0, 1).equals(".") == true) {
						continue; // skip hidden directories
					}
					if (name.length() > 16) {
						name = name.substring(0, 14) + "...";
					}
					msg.obj = name;
					changeWaitDialogHandler.sendMessage(msg);

					listDir(list, listPre[i]); // go deeper
				}
			}
		}

		return;
	}

	public void goHome(View view) {
		getBase().goHome(getActivity());
	}

	protected cgeoapplication getApp() {
		return app;
	}

	protected cgBase getBase() {
		return base;
	}

	protected cgWarning getWarning() {
		return warning;
	}

	protected Activity getActivity() {
		return activity;
	}

	protected Resources getRes() {
		return res;
	}
}
