package cgeo.geocaching;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
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
import cgeo.geocaching.LogTemplateProvider.LogTemplate;

public class cgeovisit extends cgLogForm {
	private static final int MENU_SIGNATURE = 1;
	private static final int SUBMENU_VOTE = 2;
	
	private LayoutInflater inflater = null;
	private cgCache cache = null;
	private ArrayList<Integer> types = new ArrayList<Integer>();
	private ProgressDialog waitDialog = null;
	private String cacheid = null;
	private String geocode = null;
	private String text = null;
	private boolean alreadyFound = false;
	private String viewstate = null;
	private String viewstate1 = null;
	private Boolean gettingViewstate = true;
	private ArrayList<cgTrackableLog> trackables = null;
	private Calendar date = Calendar.getInstance();
	private int typeSelected = 1;
	private int attempts = 0;
	private boolean progressBar = false;
	private Button post = null;
	private Button save = null;
	private Button clear = null;
	private CheckBox tweetCheck = null;
	private LinearLayout tweetBox = null;
	private double rating = 0.0;
	private boolean tbChanged = false;

	// handlers
	private Handler showProgressHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (progressBar) {
				showProgress(true);
			}
		}
	};
	private Handler loadDataHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (types.contains(typeSelected) == false) {
				typeSelected = types.get(0);
				setType(typeSelected);

				showToast(res.getString(R.string.info_log_type_changed));
			}

			if ((viewstate == null || viewstate.length() == 0) && attempts < 2) {
				showToast(res.getString(R.string.err_log_load_data_again));

				loadData thread;
				thread = new loadData(cacheid);
				thread.start();

				return;
			} else if ((viewstate == null || viewstate.length() == 0) && attempts >= 2) {
				showToast(res.getString(R.string.err_log_load_data));
				showProgress(false);

				return;
			}

			gettingViewstate = false; // we're done, user can post log

			if (post == null) {
				post = (Button) findViewById(R.id.post);
			}
			post.setEnabled(true);
			post.setOnClickListener(new postListener());

			// add trackables
			if (trackables != null && trackables.isEmpty() == false) {
				if (inflater == null) {
					inflater = getLayoutInflater();
				}

				final LinearLayout inventoryView = (LinearLayout) findViewById(R.id.inventory);
				inventoryView.removeAllViews();

				for (cgTrackableLog tb : trackables) {
					LinearLayout inventoryItem = (LinearLayout) inflater.inflate(R.layout.visit_trackable, null);

					((TextView) inventoryItem.findViewById(R.id.trackcode)).setText(tb.trackCode);
					((TextView) inventoryItem.findViewById(R.id.name)).setText(tb.name);
					((TextView) inventoryItem.findViewById(R.id.action)).setText(cgBase.logTypesTrackable.get(settings.trackableAutovisit ? 1 : 0));

					inventoryItem.setId(tb.id);
					final String tbCode = tb.trackCode;
					inventoryItem.setClickable(true);
					registerForContextMenu(inventoryItem);
					inventoryItem.findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							final Intent trackablesIntent = new Intent(cgeovisit.this, cgeotrackable.class);
							trackablesIntent.putExtra("geocode", tbCode);
							startActivity(trackablesIntent);
						}
					});
					inventoryItem.findViewById(R.id.action).setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							openContextMenu(view);
						}
					});

					inventoryView.addView(inventoryItem);

					if (settings.trackableAutovisit)
                    {
					    tb.action = 1;
					    tbChanged = true;
                    }
				}

				if (inventoryView.getChildCount() > 0) {
					((LinearLayout) findViewById(R.id.inventory_box)).setVisibility(View.VISIBLE);
				}
				if (inventoryView.getChildCount() > 1) {
					final LinearLayout inventoryChangeAllView = (LinearLayout) findViewById(R.id.inventory_changeall);

					Button changeButton = (Button) inventoryChangeAllView.findViewById(R.id.changebutton);
					registerForContextMenu(changeButton);
					changeButton.setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							openContextMenu(view);
						}
					});

					((LinearLayout) findViewById(R.id.inventory_changeall)).setVisibility(View.VISIBLE);
				}
			}

			showProgress(false);
		}
	};

	private Handler postLogHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				showToast(res.getString(R.string.info_log_posted));

				if (waitDialog != null) {
					waitDialog.dismiss();
				}

				finish();
				return;
			} else if (msg.what == 2) {
				showToast(res.getString(R.string.info_log_saved));

				if (waitDialog != null) {
					waitDialog.dismiss();
				}

				finish();
				return;
			} else if (msg.what >= 1000) {
				if (msg.what == 1001) {
					showToast(res.getString(R.string.warn_log_text_fill));
				} else if (msg.what == 1002) {
					showToast(res.getString(R.string.err_log_failed_server));
				} else {
					showToast(res.getString(R.string.err_log_post_failed));
				}
			} else {
				if (cgBase.errorRetrieve.get(msg.what) != null) {
					showToast(res.getString(R.string.err_log_post_failed_because) + " " + cgBase.errorRetrieve.get(msg.what) + ".");
				} else {
					showToast(res.getString(R.string.err_log_post_failed));
				}
			}

			if (waitDialog != null) {
				waitDialog.dismiss();
			}
		}
	};

	public cgeovisit() {
		super("c:geo-log");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTheme();
		setContentView(R.layout.visit);
		setTitle(res.getString(R.string.log_new_log));

		// get parameters
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			cacheid = extras.getString("id");
			geocode = extras.getString("geocode");
			text = extras.getString("text");
			alreadyFound = extras.getBoolean("found");
		}

		if ((cacheid == null || cacheid.length() == 0) && geocode != null && geocode.length() > 0) {
			cacheid = app.getCacheid(geocode);
		}
		if ((geocode == null || geocode.length() == 0) && cacheid != null && cacheid.length() > 0) {
			geocode = app.getGeocode(cacheid);
		}

		cache = app.getCacheByGeocode(geocode);

		if (cache.name != null && cache.name.length() > 0) {
			setTitle(res.getString(R.string.log_new_log) + " " + cache.name);
		} else {
			setTitle(res.getString(R.string.log_new_log) + " " + cache.geocode.toUpperCase());
		}

		app.setAction(geocode);

		if (cache == null) {
			showToast(res.getString(R.string.err_detail_cache_forgot_visit));

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
		SubMenu menuLog = null;

		menuLog = menu.addSubMenu(0, 0, 0, res.getString(R.string.log_add)).setIcon(android.R.drawable.ic_menu_add);
		for (LogTemplate template : LogTemplateProvider.getTemplates()) {
			menuLog.add(0, template.getItemId(), 0, template.getResourceId());
		}
		menuLog.add(0, MENU_SIGNATURE, 0, res.getString(R.string.init_signature));

		SubMenu menuStars = menu.addSubMenu(0, SUBMENU_VOTE, 0, res.getString(R.string.log_rating)).setIcon(android.R.drawable.ic_menu_sort_by_size);
		menuStars.add(0, 10, 0, res.getString(R.string.log_no_rating));
		menuStars.add(0, 19, 0, res.getString(R.string.log_stars_5));
		menuStars.add(0, 18, 0, res.getString(R.string.log_stars_45));
		menuStars.add(0, 17, 0, res.getString(R.string.log_stars_4));
		menuStars.add(0, 16, 0, res.getString(R.string.log_stars_35));
		menuStars.add(0, 15, 0, res.getString(R.string.log_stars_3));
		menuStars.add(0, 14, 0, res.getString(R.string.log_stars_25));
		menuStars.add(0, 13, 0, res.getString(R.string.log_stars_2));
		menuStars.add(0, 12, 0, res.getString(R.string.log_stars_15));
		menuStars.add(0, 11, 0, res.getString(R.string.log_stars_1));

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean signatureAvailable = settings.getSignature() != null;
		menu.findItem(MENU_SIGNATURE).setVisible(signatureAvailable);

		boolean voteAvailable = settings.isGCvoteLogin() && typeSelected == cgBase.LOG_FOUND_IT && cache.guid != null && cache.guid.length() > 0;
		menu.findItem(SUBMENU_VOTE).setVisible(voteAvailable);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == MENU_SIGNATURE) {
			EditText log = (EditText) findViewById(R.id.log);
			String content = log.getText().toString();
			if (content.length() > 0) {
				insertIntoLog("\n");
			}
			insertIntoLog(LogTemplateProvider.applyTemplates(settings.getSignature(), base));
			return true;
		} else if (id >= 10 && id <= 19) {
			rating = (id - 9) / 2.0;

			if (post == null) {
				post = (Button) findViewById(R.id.post);
			}
			if (rating == 0.0) {
				post.setText(res.getString(R.string.log_post_no_rate));
			} else {
				post.setText(res.getString(R.string.log_post_rate) + " " + ratingTextValue(rating) + "*");
			}
			return true;
		}
		LogTemplate template = LogTemplateProvider.getTemplate(id);
		if (template != null) {
			String newText = template.getValue(base);
			insertIntoLog(newText);
			return true;
		}
		return false;
	}

	private void insertIntoLog(String newText) {
		EditText log = (EditText) findViewById(R.id.log);
		cgBase.insertAtPosition(log, newText, true);
	}

	private static String ratingTextValue(final double rating) {
		return String.format(Locale.getDefault(), "%.1f", rating);
	}

	public boolean setRating(String guid, double vote) {
		if (guid == null || guid.length() == 0) {
			return false;
		}
		if (vote < 0.0 || vote > 5.0) {
			return false;
		}

		final HashMap<String, String> login = settings.getGCvoteLogin();
		if (login == null) {
			return false;
		}

		final HashMap<String, String> params = new HashMap<String, String>();
		params.put("userName", login.get("username"));
		params.put("password", login.get("password"));
		params.put("cacheId", guid);
		params.put("voteUser", String.format("%.1f", rating).replace(',', '.'));
		params.put("version", "cgeo");

		final String result = base.request(false, "gcvote.com", "/setVote.php", "GET", params, false, false, false).getData();

		return result.trim().equalsIgnoreCase("ok");
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
		super.onCreateContextMenu(menu, view, info);
		final int viewId = view.getId();

		if (viewId == R.id.type) {
			for (final int typeOne : types) {
				menu.add(viewId, typeOne, 0, cgBase.logTypes2.get(typeOne));
				Log.w(cgSettings.tag, "Addig " + typeOne + " " + cgBase.logTypes2.get(typeOne));
			}
		} else if (viewId == R.id.changebutton) {
			final int textId = ((TextView) findViewById(viewId)).getId();

			menu.setHeaderTitle(res.getString(R.string.log_tb_changeall));
			for (final int logTbAction : cgBase.logTypesTrackable.keySet()) {
				menu.add(textId, logTbAction, 0, cgBase.logTypesTrackable.get(logTbAction));
			}
		} else {
			final int realViewId = ((LinearLayout) findViewById(viewId)).getId();

			for (final cgTrackableLog tb : trackables) {
				if (tb.id == realViewId) {
					menu.setHeaderTitle(tb.name);
				}
			}
			for (final int logTbAction : cgBase.logTypesTrackable.keySet()) {
				menu.add(realViewId, logTbAction, 0, cgBase.logTypesTrackable.get(logTbAction));
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int group = item.getGroupId();
		final int id = item.getItemId();

		if (group == R.id.type) {
			setType(id);

			return true;
		} else if (group == R.id.changebutton) {
			try {
				final String logTbAction = cgBase.logTypesTrackable.get(id);
				if (logTbAction != null) {
					final LinearLayout inventView = (LinearLayout) findViewById(R.id.inventory);
					for (int count = 0; count < inventView.getChildCount(); count++) {
						final LinearLayout tbView = (LinearLayout) inventView.getChildAt(count);
						if (tbView == null) {
							return false;
						}

						final TextView tbText = (TextView) tbView.findViewById(R.id.action);
						if (tbText == null) {
							return false;
						}
						tbText.setText(logTbAction);
					}
					for (cgTrackableLog tb : trackables) {
						tb.action = id;
					}
					tbChanged = true;
					return true;
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeovisit.onContextItemSelected: " + e.toString());
			}
		} else {
			try {
				final String logTbAction = cgBase.logTypesTrackable.get(id);
				if (logTbAction != null) {
					final LinearLayout tbView = (LinearLayout) findViewById(group);
					if (tbView == null) {
						return false;
					}

					final TextView tbText = (TextView) tbView.findViewById(R.id.action);
					if (tbText == null) {
						return false;
					}

					for (cgTrackableLog tb : trackables) {
						if (tb.id == group) {
							tbChanged = true;

							tb.action = id;
							tbText.setText(logTbAction);

							Log.i(cgSettings.tag, "Trackable " + tb.trackCode + " (" + tb.name + ") has new action: #" + id);
						}
					}

					return true;
				}
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeovisit.onContextItemSelected: " + e.toString());
			}
		}

		return false;
	}

	public void init() {
		if (geocode != null) {
			app.setAction(geocode);
		}

		types.clear();

		if (cache.type.equals("event") || cache.type.equals("mega") || cache.type.equals("cito") || cache.type.equals("lostfound")) {
			types.add(cgBase.LOG_WILL_ATTEND);
			types.add(cgBase.LOG_NOTE);
			types.add(cgBase.LOG_ATTENDED);
			types.add(cgBase.LOG_NEEDS_ARCHIVE);
		} else if (cache.type.equals("earth")) {
			types.add(cgBase.LOG_FOUND_IT);
			types.add(cgBase.LOG_DIDNT_FIND_IT);
			types.add(cgBase.LOG_NOTE);
			types.add(cgBase.LOG_NEEDS_MAINTENANCE);
			types.add(cgBase.LOG_NEEDS_ARCHIVE);
		} else if (cache.type.equals("webcam")) {
			types.add(cgBase.LOG_WEBCAM_PHOTO_TAKEN);
			types.add(cgBase.LOG_DIDNT_FIND_IT);
			types.add(cgBase.LOG_NOTE);
			types.add(cgBase.LOG_NEEDS_ARCHIVE);
			types.add(cgBase.LOG_NEEDS_MAINTENANCE);
		} else {
			types.add(cgBase.LOG_FOUND_IT);
			types.add(cgBase.LOG_DIDNT_FIND_IT);
			types.add(cgBase.LOG_NOTE);
			types.add(cgBase.LOG_NEEDS_ARCHIVE);
			types.add(cgBase.LOG_NEEDS_MAINTENANCE);
		}
		if (cache.owner.equalsIgnoreCase(settings.getUsername())) {
			types.add(cgBase.LOG_OWNER_MAINTENANCE);
			types.add(cgBase.LOG_TEMP_DISABLE_LISTING);
			types.add(cgBase.LOG_ENABLE_LISTING);
			types.add(cgBase.LOG_ARCHIVE);
			types.remove(Integer.valueOf(cgBase.LOG_UPDATE_COORDINATES));
			if (cache.type.equals("event") || cache.type.equals("mega") || cache.type.equals("cito") || cache.type.equals("lostfound")) {
				types.add(cgBase.LOG_ANNOUNCEMENT);
			}
		}

		final cgLog log = app.loadLogOffline(geocode);
		if (log != null) {
			typeSelected = log.type;
			date.setTime(new Date(log.date));
			text = log.log;
			if (typeSelected == cgBase.LOG_FOUND_IT && settings.isGCvoteLogin()) {
				if (post == null) {
					post = (Button) findViewById(R.id.post);
				}
				post.setText(res.getString(R.string.log_post_no_rate));
			}
		} else if (settings.getSignature() != null
		        && settings.signatureAutoinsert
		        && settings.getSignature().length() > 0
		        && 0 == ((EditText) findViewById(R.id.log)).getText().length()) {
			insertIntoLog(LogTemplateProvider.applyTemplates(settings.getSignature(), base));
		}

		if (types.contains(typeSelected) == false) {
			if (alreadyFound) {
				typeSelected = cgBase.LOG_NOTE;
			} else {
				typeSelected = types.get(0);
			}
			setType(typeSelected);
		}

		Button typeButton = (Button) findViewById(R.id.type);
		registerForContextMenu(typeButton);
		typeButton.setText(cgBase.logTypes2.get(typeSelected));
		typeButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				openContextMenu(view);
			}
		});

		Button dateButton = (Button) findViewById(R.id.date);
		dateButton.setText(base.formatShortDate(date.getTime().getTime()));
		dateButton.setOnClickListener(new cgeovisitDateListener());

		EditText logView = (EditText) findViewById(R.id.log);
		if (logView.getText().length() == 0 && text != null && text.length() > 0) {
			logView.setText(text);
		}


		if (tweetBox == null) {
			tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
		}
		if (tweetCheck == null) {
			tweetCheck = (CheckBox) findViewById(R.id.tweet);
		}
		tweetCheck.setChecked(true);

		if (post == null) {
			post = (Button) findViewById(R.id.post);
		}
		if (viewstate == null || viewstate.length() == 0) {
			post.setEnabled(false);
			post.setOnTouchListener(null);
			post.setOnClickListener(null);

			loadData thread;
			thread = new loadData(cacheid);
			thread.start();
		} else {
			post.setEnabled(true);
			post.setOnClickListener(new postListener());
		}

		if (save == null) {
			save = (Button) findViewById(R.id.save);
		}
		save.setOnClickListener(new saveListener());

		if (clear == null) {
			clear = (Button) findViewById(R.id.clear);
		}
		clear.setOnClickListener(new clearListener());
	}

	public void setDate(Calendar dateIn) {
		date = dateIn;

		final Button dateButton = (Button) findViewById(R.id.date);
		dateButton.setText(base.formatShortDate(date.getTime().getTime()));
	}

	public void setType(int type) {
		final Button typeButton = (Button) findViewById(R.id.type);

		if (cgBase.logTypes2.get(type) != null) {
			typeSelected = type;
		}
		if (cgBase.logTypes2.get(typeSelected) == null) {
			typeSelected = 1;
		}
		typeButton.setText(cgBase.logTypes2.get(typeSelected));

		if (tweetBox == null) {
			tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
		}

		if (type == 2 && tbChanged == false) {
			// TODO: change action
		} else if (type != 2 && tbChanged == false) {
			// TODO: change action
		}

		if (type == cgBase.LOG_FOUND_IT && settings.twitter == 1) {
			tweetBox.setVisibility(View.VISIBLE);
		} else {
			tweetBox.setVisibility(View.GONE);
		}

		if (post == null) {
			post = (Button) findViewById(R.id.post);
		}

		if (type == cgBase.LOG_FOUND_IT && settings.isGCvoteLogin()) {
			if (rating == 0) {
				post.setText(res.getString(R.string.log_post_no_rate));
			} else {
				post.setText(res.getString(R.string.log_post_rate) + " " + ratingTextValue(rating) + "*");
			}
		} else {
			post.setText(res.getString(R.string.log_post));
		}
	}

	private class cgeovisitDateListener implements View.OnClickListener {

		public void onClick(View arg0) {
			Dialog dateDialog = new cgeodate(cgeovisit.this, cgeovisit.this, date);
			dateDialog.setCancelable(true);
			dateDialog.show();
		}
	}

	private class postListener implements View.OnClickListener {

		public void onClick(View arg0) {
			if (gettingViewstate == false) {
				waitDialog = ProgressDialog.show(cgeovisit.this, null, res.getString(R.string.log_saving), true);
				waitDialog.setCancelable(true);

				String log = ((EditText) findViewById(R.id.log)).getText().toString();
				Thread thread = new postLog(postLogHandler, log);
				thread.start();
			} else {
				showToast(res.getString(R.string.err_log_load_data_still));
			}
		}
	}

	private class saveListener implements View.OnClickListener {

		public void onClick(View arg0) {
			String log = ((EditText) findViewById(R.id.log)).getText().toString();
			final boolean status = app.saveLogOffline(geocode, date.getTime(), typeSelected, log);
			if (save == null) {
				save = (Button) findViewById(R.id.save);
			}
			save.setOnClickListener(new saveListener());

			if (status) {
				showToast(res.getString(R.string.info_log_saved));
				app.saveVisitDate(geocode);
			} else {
				showToast(res.getString(R.string.err_log_post_failed));
			}
		}
	}

	private class clearListener implements View.OnClickListener {

		public void onClick(View arg0) {
			app.clearLogOffline(geocode);

			if (alreadyFound) {
				typeSelected = cgBase.LOG_NOTE;
			} else {
				typeSelected = types.get(0);
			}
			date.setTime(new Date());
			text = null;

			setType(typeSelected);

			Button dateButton = (Button) findViewById(R.id.date);
			dateButton.setText(base.formatShortDate(date.getTime().getTime()));
			dateButton.setOnClickListener(new cgeovisitDateListener());

			EditText logView = (EditText) findViewById(R.id.log);
			if (text != null && text.length() > 0) {
				logView.setText(text);
			} else {
				logView.setText("");
			}

			if (clear == null) {
				clear = (Button) findViewById(R.id.clear);
			}
			clear.setOnClickListener(new clearListener());

			showToast(res.getString(R.string.info_log_cleared));
		}
	}

	private class loadData extends Thread {

		private String cacheid = null;

		public loadData(String cacheidIn) {
			cacheid = cacheidIn;

			if (cacheid == null) {
				showToast(res.getString(R.string.err_detail_cache_forgot_visit));

				finish();
				return;
			}
		}

		@Override
		public void run() {
			final HashMap<String, String> params = new HashMap<String, String>();

			showProgressHandler.sendEmptyMessage(0);
			gettingViewstate = true;
			attempts++;

			try {
				if (cacheid != null && cacheid.length() > 0) {
					params.put("ID", cacheid);
				} else {
					loadDataHandler.sendEmptyMessage(0);
					return;
				}

				final String page = base.request(false, "www.geocaching.com", "/seek/log.aspx", "GET", params, false, false, false).getData();

				viewstate = cgBase.findViewstate(page, 0);
				viewstate1 = cgBase.findViewstate(page, 1);
				trackables = cgBase.parseTrackableLog(page);

				final ArrayList<Integer> typesPre = cgBase.parseTypes(page);
				if (typesPre.size() > 0) {
					types.clear();
					types.addAll(typesPre);
					types.remove(Integer.valueOf(cgBase.LOG_UPDATE_COORDINATES));
				}
				typesPre.clear();
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeovisit.loadData.run: " + e.toString());
			}

			loadDataHandler.sendEmptyMessage(0);
		}
	}

	private class postLog extends Thread {

		Handler handler = null;
		String log = null;

		public postLog(Handler handlerIn, String logIn) {
			handler = handlerIn;
			log = logIn;
		}

		@Override
		public void run() {
			int ret = -1;

			ret = postLogFn(log);

			handler.sendEmptyMessage(ret);
		}
	}

	public int postLogFn(String log) {
		int status = -1;

		try {
			if (tweetBox == null) {
				tweetBox = (LinearLayout) findViewById(R.id.tweet_box);
			}
			if (tweetCheck == null) {
				tweetCheck = (CheckBox) findViewById(R.id.tweet);
			}

			status = base.postLog(app, geocode, cacheid, viewstate, viewstate1, typeSelected, date.get(Calendar.YEAR), (date.get(Calendar.MONTH) + 1), date.get(Calendar.DATE), log, trackables);

			if (status == 1) {
				cgLog logNow = new cgLog();
				logNow.author = settings.getUsername();
				logNow.date = date.getTimeInMillis();
				logNow.type = typeSelected;
				logNow.log = log;

				if (cache != null) {
					cache.logs.add(0, logNow);
				}
				app.addLog(geocode, logNow);

				if (typeSelected == cgBase.LOG_FOUND_IT) {
					app.markFound(geocode);
					if (cache != null) {
						cache.found = true;
					}
				}

				if (cache != null) {
					app.putCacheInCache(cache);
				} else {
					app.removeCacheFromCache(geocode);
				}
			}

			if (status == 1) {
				app.clearLogOffline(geocode);
			}

			if (
					status == 1 && typeSelected == cgBase.LOG_FOUND_IT && settings.twitter == 1
					&& settings.tokenPublic != null && settings.tokenPublic.length() > 0 && settings.tokenSecret != null
					&& settings.tokenSecret.length() > 0 && tweetCheck.isChecked() && tweetBox.getVisibility() == View.VISIBLE
			) {
				cgBase.postTweetCache(app, settings, geocode);
			}

			if (status == 1 && typeSelected == cgBase.LOG_FOUND_IT && settings.isGCvoteLogin()) {
				setRating(cache.guid, rating);
			}

			return status;
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgeovisit.postLogFn: " + e.toString());
		}

		return 1000;
	}
}