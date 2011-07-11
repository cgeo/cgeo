package cgeo.geocaching;

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
import java.io.File;
import java.util.ArrayList;

public class cgeogpxes extends ListActivity {

	private ArrayList<File> files = new ArrayList<File>();
	private cgeoapplication app = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private Activity activity = null;
	private cgGPXListAdapter adapter = null;
	private ProgressDialog waitDialog = null;
	private ProgressDialog parseDialog = null;
	private Resources res = null;
	private loadFiles searchingThread = null;
	private boolean endSearching = false;
	private int listId = 1;
	private int imported = 0;
	final private Handler changeWaitDialogHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.obj != null && waitDialog != null) {
				waitDialog.setMessage(res.getString(R.string.gpx_import_searching_in) + " " + (String) msg.obj);
			}
		}
	};
	final private Handler changeParseDialogHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.obj != null && parseDialog != null) {
				parseDialog.setMessage(res.getString(R.string.gpx_import_loading_stored) + " " + (Integer) msg.obj);
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

					warning.showToast(res.getString(R.string.gpx_import_no_files));

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
				Log.e(cgSettings.tag, "cgeogpxes.loadFilesHandler: " + e.toString());
			}
		}
	};
	final private Handler loadCachesHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (parseDialog != null) {
					parseDialog.dismiss();
				}

				warning.helpDialog(res.getString(R.string.gpx_import_title_caches_imported), imported + " " + res.getString(R.string.gpx_import_caches_imported));
				imported = 0;
			} catch (Exception e) {
				if (parseDialog != null) {
					parseDialog.dismiss();
				}
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
		base = new cgBase(app, settings, getSharedPreferences(cgSettings.preferences, 0));
		warning = new cgWarning(this);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.gpx);
		base.setTitle(activity, res.getString(R.string.gpx_import_title));

		// google analytics
		base.sendAnal(activity, "/gpx-import");

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
				res.getString(R.string.gpx_import_title_searching),
				res.getString(R.string.gpx_import_searching),
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
		
		settings.load();
	}

	private void setAdapter() {
		if (adapter == null) {
			adapter = new cgGPXListAdapter(this, settings, files);
			setListAdapter(adapter);
		}
	}

	private class loadFiles extends Thread {
		public void notifyEnd() {
			endSearching = true;
		}

		@Override
		public void run() {
			ArrayList<File> list = new ArrayList<File>();

			try {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) == true) {
					final File gpx = new File(Environment.getExternalStorageDirectory().toString() + "/gpx");
					
					if (gpx.exists() && gpx.isDirectory()) {
						listDir(list, gpx);
					} else {
						listDir(list, Environment.getExternalStorageDirectory());
					}
				} else {
					Log.w(cgSettings.tag, "No external media mounted.");
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeogpxes.loadFiles.run: " + e.toString());
			}

			final Message msg = new Message();
			msg.obj = "loaded directories";
			changeWaitDialogHandler.sendMessage(msg);

			files.addAll(list);
			list.clear();

			loadFilesHandler.sendMessage(new Message());
		}
	}

	private void listDir(ArrayList<File> list, File directory) {
		if (directory == null || directory.isDirectory() == false || directory.canRead() == false) {
			return;
		}

		final File[] listPre = directory.listFiles();

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

						if (extension.equals("gpx") == false) {
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

	public void loadGPX(File file) {
		if (waitDialog != null) {
			waitDialog.dismiss();
		}

		parseDialog = ProgressDialog.show(
				activity,
				res.getString(R.string.gpx_import_title_reading_file),
				res.getString(R.string.gpx_import_loading),
				true,
				false);

		new loadCaches(file).start();
	}

	private class loadCaches extends Thread {

		File file = null;

		public loadCaches(File fileIn) {
			file = fileIn;
		}

		@Override
		public void run() {
			final long searchId = base.parseGPX(app, file, listId, changeParseDialogHandler);

			imported = app.getCount(searchId);

			loadCachesHandler.sendMessage(new Message());
		}
	}

	public void goHome(View view) {
		base.goHome(activity);
	}
}
