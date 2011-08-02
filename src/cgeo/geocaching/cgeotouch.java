package cgeo.geocaching;

import gnu.android.app.appmanualclient.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class cgeotouch extends cgLogForm {
	private cgeoapplication app = null;
	private Activity activity = null;
	private Resources res = null;
	private LayoutInflater inflater = null;
	private cgBase base = null;
	private cgSettings settings = null;
	private cgWarning warning = null;
	private cgTrackable trackable = null;
	private ArrayList<Integer> types = new ArrayList<Integer>();
	private ProgressDialog waitDialog = null;
	private String guid = null;
	private String geocode = null;
	private String text = null;
	private String viewstate = null;
	private String viewstate1 = null;
	private Boolean gettingViewstate = true;
	private Calendar date = Calendar.getInstance();
	private int typeSelected = -1;
	private int attempts = 0;
	private CheckBox tweetCheck = null;
	private LinearLayout tweetBox = null;

	private Handler showProgressHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			base.showProgress(activity, true);
		}
	};

	private Handler loadDataHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if ((viewstate == null || viewstate.length() == 0) && attempts < 2) {
				warning.showToast(res.getString(R.string.err_log_load_data_again));

				loadData thread;
				thread = new loadData(guid);
				thread.start();

				return;
			} else if ((viewstate == null || viewstate.length() == 0) && attempts >= 2) {
				warning.showToast(res.getString(R.string.err_log_load_data));
				base.showProgress(activity, false);

				return;
			}

			gettingViewstate = false; // we're done, user can post log

			Button buttonPost = (Button)findViewById(R.id.post);
			buttonPost.setEnabled(true);
			buttonPost.setOnClickListener(new postListener());

			base.showProgress(activity, false);
		}
	};

	private Handler postLogHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				warning.showToast(res.getString(R.string.info_log_posted));

				if (waitDialog != null) {
					waitDialog.dismiss();
				}
				finish();
				return;
			} else if (msg.what >= 1000) {
				if (msg.what == 1001) {
					warning.showToast(res.getString(R.string.warn_log_text_fill));
				} else if(msg.what == 1002) {
					warning.showToast(res.getString(R.string.err_log_failed_server));
				} else {
					warning.showToast(res.getString(R.string.err_log_post_failed));
				}
			} else {
				if (cgBase.errorRetrieve.get(msg.what) != null) {
					warning.showToast(res.getString(R.string.err_log_post_failed_because) + cgBase.errorRetrieve.get(msg.what) + ".");
				} else {
					warning.showToast(res.getString(R.string.err_log_post_failed));
				}
			}

			if (waitDialog != null) {
				waitDialog.dismiss();
			}
		}
	};

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
		res = this.getResources();
		app = (cgeoapplication)this.getApplication();
		settings = new cgSettings(this, getSharedPreferences(cgSettings.preferences, 0));
		base = new cgBase(app, settings, getSharedPreferences(cgSettings.preferences, 0));
		warning = new cgWarning(this);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.touch);
		base.setTitle(activity, res.getString(R.string.trackable_touch));

		// get parameters
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			geocode = extras.getString("geocode");
			guid = extras.getString("guid");
			text = extras.getString("text");
		}

		trackable = app.getTrackableByGeocode("logging trackable");

		if (trackable.name != null && trackable.name.length() > 0) {
			base.setTitle(activity, res.getString(R.string.trackable_touch) + trackable.name);
		} else {
			base.setTitle(activity, res.getString(R.string.trackable_touch) + trackable.geocode.toUpperCase());
		}

		app.setAction("logging trackable");

		if (trackable == null || guid == null) {
			warning.showToast(res.getString(R.string.err_tb_forgot_saw));

			finish();
			return;
		}

		init();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		settings.load();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SubMenu subMenu = menu.addSubMenu(0, 0, 0, res.getString(R.string.log_add)).setIcon(android.R.drawable.ic_menu_add);

		subMenu.add(0, 0x6, 0, res.getString(R.string.log_date_time));
		subMenu.add(0, 0x4, 0, res.getString(R.string.log_date));
		subMenu.add(0, 0x2, 0, res.getString(R.string.log_time));
		subMenu.add(0, 0x1, 0, res.getString(R.string.init_signature));
		subMenu.add(0, 0x7, 0, res.getString(R.string.log_date_time) + " & " + res.getString(R.string.init_signature));

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (settings.getSignature() == null) {
			menu.findItem(0x1).setVisible(false);
			menu.findItem(0x7).setVisible(false);
		} else {
			menu.findItem(0x1).setVisible(true);
			menu.findItem(0x7).setVisible(true);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		EditText text = null;
		String textContent = null;
		String dateString = null;
		String timeString = null;
		String addText = "";

		if ((id >= 0x1 && id <= 0x7)) {
			text = (EditText) findViewById(R.id.log);
			textContent = text.getText().toString();
			dateString = cgBase.dateOut.format(new Date());
			timeString = cgBase.timeOut.format(new Date());
		
			if ((id & 0x4) == 0x4) {
				addText += dateString;
				if ((id & 0x2) == 0x2) {
					addText += " | ";
				}
			}
			if ((id & 0x2) == 0x2) {
				addText += timeString;
			}
			if ((id & 0x1) == 0x1 && settings.getSignature() != null) {
				if (addText.length() > 0) {
					addText += "\n";
				}
				addText += settings.getSignature()
				.replaceAll("\\[DATE\\]", dateString)
				.replaceAll("\\[TIME\\]", timeString)
				.replaceAll("\\[USER\\]", settings.getUsername())
				.replaceAll("\\[NUMBER\\]", "");
			}
			if (textContent.length() > 0 && addText.length() > 0 ) {
				addText = "\n" + addText;
			}
			text.setText(textContent + addText, TextView.BufferType.NORMAL);
			text.setSelection(text.getText().toString().length());
			return true;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
		super.onCreateContextMenu(menu, view, info);
		final int viewId = view.getId();

		if (viewId == R.id.type) {
			for (final int typeOne : types) menu.add(viewId, typeOne, 0, cgBase.logTypes2.get(typeOne));
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int group = item.getGroupId();
		final int id = item.getItemId();

		if (group == R.id.type) {
			setType(id);

			return true;
		}

		return false;
	}

	public void init() {
		if (geocode != null) app.setAction("logging trackable");

		types.clear();
		types.add(cgBase.LOG_RETRIEVED_IT);
		types.add(cgBase.LOG_GRABBED_IT);
		types.add(cgBase.LOG_NOTE);
		types.add(cgBase.LOG_DISCOVERED_IT);

		if (typeSelected < 0 && cgBase.logTypes2.get(typeSelected) == null) typeSelected = types.get(2);
		setType(typeSelected);

		Button typeButton = (Button)findViewById(R.id.type);
		registerForContextMenu(typeButton);
		typeButton.setText(cgBase.logTypes2.get(typeSelected));
		typeButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				openContextMenu(view);
			}
		});

		Button dateButton = (Button)findViewById(R.id.date);
		dateButton.setText(cgBase.dateOutShort.format(date.getTime()));
		dateButton.setOnClickListener(new cgeotouchDateListener());

        if (tweetBox == null) tweetBox = (LinearLayout)findViewById(R.id.tweet_box);
        if (tweetCheck == null) tweetCheck = (CheckBox)findViewById(R.id.tweet);
        tweetCheck.setChecked(true);

		Button buttonPost = (Button)findViewById(R.id.post);
		if (viewstate == null || viewstate.length() == 0) {
			buttonPost.setEnabled(false);
			buttonPost.setOnTouchListener(null);
			buttonPost.setOnClickListener(null);

			loadData thread;
			thread = new loadData(guid);
			thread.start();
		} else {
			buttonPost.setEnabled(true);
			buttonPost.setOnClickListener(new postListener());
		}
	}

	public void setDate(Calendar dateIn) {
		date = dateIn;

		final Button dateButton = (Button)findViewById(R.id.date);
		dateButton.setText(cgBase.dateOutShort.format(date.getTime()));
	}

	public void setType(int type) {
		final Button typeButton = (Button)findViewById(R.id.type);

		if (cgBase.logTypes2.get(type) != null) typeSelected = type;
		if (cgBase.logTypes2.get(typeSelected) == null) typeSelected = 0;
		typeButton.setText(cgBase.logTypes2.get(typeSelected));

        if (tweetBox == null) tweetBox = (LinearLayout)findViewById(R.id.tweet_box);
        if (settings.twitter == 1) tweetBox.setVisibility(View.VISIBLE);
        else tweetBox.setVisibility(View.GONE);
	}

	private class cgeotouchDateListener implements View.OnClickListener {
		public void onClick(View arg0) {
			Dialog dateDialog = new cgeodate(activity, (cgeotouch)activity, date);
			dateDialog.setCancelable(true);
			dateDialog.show();
		}
	}

	private class postListener implements View.OnClickListener {
		public void onClick(View arg0) {
			if (gettingViewstate == false) {
				waitDialog = ProgressDialog.show(activity, null, res.getString(R.string.log_saving), true);
				waitDialog.setCancelable(true);

				String tracking = ((EditText)findViewById(R.id.tracking)).getText().toString();
				String log = ((EditText)findViewById(R.id.log)).getText().toString();
				Thread thread = new postLog(postLogHandler, tracking, log);
				thread.start();
			} else {
				warning.showToast(res.getString(R.string.err_log_load_data_still));
			}
		}
	}

	private class loadData extends Thread {
		private String guid = null;

		public loadData(String guidIn) {
			guid = guidIn;

			if (guid == null) {
				warning.showToast(res.getString(R.string.err_tb_forgot_saw));

				finish();
				return;
			}
		}

		@Override
		public void run() {
			final HashMap<String, String> params = new HashMap<String, String>();

			showProgressHandler.sendEmptyMessage(0);
			gettingViewstate = true;
			attempts ++;

			try {
				if (guid != null && guid.length() > 0) {
					params.put("wid", guid);
				} else {
					loadDataHandler.sendEmptyMessage(0);
					return;
				}

				final String page = base.request(false, "www.geocaching.com", "/track/log.aspx", "GET", params, false, false, false).getData();

				viewstate = base.findViewstate(page, 0);
				viewstate1 = base.findViewstate(page, 1);

				final ArrayList<Integer> typesPre = base.parseTypes(page);
				if (typesPre.size() > 0) {
					types.clear();
					types.addAll(typesPre);
				}
				typesPre.clear();

				if (types.contains(typeSelected) == false) {
					typeSelected = types.get(0);
					setType(typeSelected);
					warning.showToast(res.getString(R.string.info_log_type_changed));
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeotouch.loadData.run: " + e.toString());
			}

			loadDataHandler.sendEmptyMessage(0);
		}
	}

	private class postLog extends Thread {
		Handler handler = null;
		String tracking = null;
		String log = null;

		public postLog(Handler handlerIn, String trackingIn, String logIn) {
			handler = handlerIn;
			tracking = trackingIn;
			log = logIn;
		}

		@Override
		public void run() {
			int ret = -1;

			ret = postLogFn(tracking, log);

			handler.sendEmptyMessage(ret);
		}
	}

	public int postLogFn(String tracking, String log) {
		int status = -1;

		try {
			if (tweetBox == null) tweetBox = (LinearLayout)findViewById(R.id.tweet_box);
			if (tweetCheck == null) tweetCheck = (CheckBox)findViewById(R.id.tweet);

			status = base.postLogTrackable(guid, tracking, viewstate, viewstate1, typeSelected, date.get(Calendar.YEAR), (date.get(Calendar.MONTH ) + 1), date.get(Calendar.DATE), log);

			if (
				status == 1 && settings.twitter == 1 &&
				settings.tokenPublic != null && settings.tokenPublic.length() > 0 && settings.tokenSecret != null && settings.tokenSecret.length() > 0 &&
				tweetCheck.isChecked() == true && tweetBox.getVisibility() == View.VISIBLE
			) {
				base.postTweetTrackable(app, settings, geocode);
			}

			return status;
		} catch (Exception e) {
            Log.e(cgSettings.tag, "cgeotouch.postLogFn: " + e.toString());
		}

		return 1000;
	}

	public void goHome(View view) {
		base.goHome(activity);
	}
	
	public void goManual(View view) {
		try {
			AppManualReaderClient.openManual(
				"c-geo",
				"c:geo-log-trackable",
				activity,
				"http://cgeo.carnero.cc/manual/"
			);
		} catch (Exception e) {
			// nothing
		}
	}
}