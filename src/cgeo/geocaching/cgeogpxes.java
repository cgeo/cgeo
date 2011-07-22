package cgeo.geocaching;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import java.io.File;
import java.util.ArrayList;

public class cgeogpxes extends cgFileList<cgGPXListAdapter> {

	private ProgressDialog parseDialog = null;
	private int listId = 1;
	private int imported = 0;

	final private Handler changeParseDialogHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.obj != null && parseDialog != null) {
				parseDialog.setMessage(getRes().getString(R.string.gpx_import_loading_stored) + " " + (Integer) msg.obj);
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

				getWarning().helpDialog(getRes().getString(R.string.gpx_import_title_caches_imported), imported + " " + getRes().getString(R.string.gpx_import_caches_imported));
				imported = 0;
			} catch (Exception e) {
				if (parseDialog != null) {
					parseDialog.dismiss();
				}
			}
		}
	};

	@Override
	protected cgGPXListAdapter getAdapter(ArrayList<File> files) {
		return new cgGPXListAdapter(this, getSettings(), files);
	}

	@Override
	protected String[] getBaseFolders() {
		String base = Environment.getExternalStorageDirectory().toString();
		return new String[]{base + "/gpx"};
	}

	@Override
	protected String getFileExtension() {
		return "gpx";
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	public void loadGPX(File file) {

		parseDialog = ProgressDialog.show(
				getActivity(),
				getRes().getString(R.string.gpx_import_title_reading_file),
				getRes().getString(R.string.gpx_import_loading),
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
			final long searchId = getBase().parseGPX(getApp(), file, listId, changeParseDialogHandler);

			imported = getApp().getCount(searchId);

			loadCachesHandler.sendMessage(new Message());
		}
	}

	public void goHome(View view) {
		getBase().goHome(getActivity());
	}

}
