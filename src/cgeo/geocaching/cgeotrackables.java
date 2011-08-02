package cgeo.geocaching;

import gnu.android.app.appmanualclient.*;

import java.util.ArrayList;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class cgeotrackables extends Activity {
	private ArrayList<cgTrackable> trackables = new ArrayList<cgTrackable>();
	private String geocode = null;
	private cgeoapplication app = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private Activity activity = null;
	private LayoutInflater inflater = null;
	private LinearLayout addList = null;
	private ProgressDialog waitDialog = null;
	private Handler loadInventoryHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (inflater == null) {
					inflater = activity.getLayoutInflater();
				}

				if (addList == null) {
					addList = (LinearLayout) findViewById(R.id.trackable_list);
				}

				if (trackables.isEmpty()) {
					if (waitDialog != null) {
						waitDialog.dismiss();
					}

					warning.showToast("Sorry, c:geo failed to load cache inventory.");

					finish();
					return;
				} else {
					LinearLayout oneTbPre = null;
					for (cgTrackable trackable : trackables) {
						oneTbPre = (LinearLayout) inflater.inflate(R.layout.trackable_button, null);

						Button oneTb = (Button) oneTbPre.findViewById(R.id.button);

						if (trackable.name != null) {
							oneTb.setText(Html.fromHtml(trackable.name).toString());
						} else {
							oneTb.setText("some trackable");
						}
						oneTb.setClickable(true);
						oneTb.setOnClickListener(new buttonListener(trackable.guid, trackable.geocode, trackable.name));
						addList.addView(oneTbPre);
					}
				}

				if (waitDialog != null) {
					waitDialog.dismiss();
				}
			} catch (Exception e) {
				if (waitDialog != null) {
					waitDialog.dismiss();
				}
				Log.e(cgSettings.tag, "cgeotrackables.loadInventoryHandler: " + e.toString());
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
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
		setContentView(R.layout.trackables);
		base.setTitle(activity, "Trackables");

		// get parameters
		Bundle extras = getIntent().getExtras();

		// try to get data from extras
		if (extras != null) {
			geocode = extras.getString("geocode");
		}

		if (geocode == null) {
			warning.showToast("Sorry, c:geo forgot for what cache you want to load trackables.");
			finish();
			return;
		}

		waitDialog = ProgressDialog.show(this, null, "loading cache inventory...", true);
		waitDialog.setCancelable(true);

		(new loadInventory()).start();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		settings.load();
	}

	private class loadInventory extends Thread {

		@Override
		public void run() {
			try {
				trackables = app.loadInventory(geocode);

				loadInventoryHandler.sendMessage(new Message());
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeotrackables.loadInventory.run: " + e.toString());
			}
		}
	}

	private class buttonListener implements View.OnClickListener {

		private String guid = null;
		private String geocode = null;
		private String name = null;

		public buttonListener(String guidIn, String geocodeIn, String nameIn) {
			guid = guidIn;
			geocode = geocodeIn;
			name = nameIn;
		}

		public void onClick(View arg0) {
			Intent trackableIntent = new Intent(activity, cgeotrackable.class);
			trackableIntent.putExtra("guid", guid);
			trackableIntent.putExtra("geocode", geocode);
			trackableIntent.putExtra("name", name);
			activity.startActivity(trackableIntent);

			finish();
			return;
		}
	}

	public void goHome(View view) {
		base.goHome(activity);
	}
	
	public void goManual(View view) {
		try {
			AppManualReaderClient.openManual(
				"c-geo",
				"c:geo-trackable-list",
				activity,
				"http://cgeo.carnero.cc/manual/"
			);
		} catch (Exception e) {
			// nothing
		}
	}
}