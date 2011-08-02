package cgeo.geocaching;

import gnu.android.app.appmanualclient.AppManualReaderClient;

import java.util.HashMap;
import java.util.Locale;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;

public class cgeopopup extends Activity {

	private Activity activity = null;
	private Resources res = null;
	private cgeoapplication app = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private Boolean fromDetail = false;
	private LayoutInflater inflater = null;
	private String geocode = null;
	private cgCache cache = null;
	private cgGeo geo = null;
	private cgUpdateLoc geoUpdate = new update();
	private ProgressDialog storeDialog = null;
	private ProgressDialog dropDialog = null;
	private TextView cacheDistance = null;
	private HashMap<String, Integer> gcIcons = new HashMap<String, Integer>();
	private Handler ratingHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				final Bundle data = msg.getData();

				setRating(data.getFloat("rating"), data.getInt("votes"));
			} catch (Exception e) {
				// nothing
			}
		}
	};
	private Handler storeCacheHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (storeDialog != null) {
					storeDialog.dismiss();
				}

				finish();
				return;
			} catch (Exception e) {
				warning.showToast(res.getString(R.string.err_store));

				Log.e(cgSettings.tag, "cgeopopup.storeCacheHandler: " + e.toString());
			}

			if (storeDialog != null) {
				storeDialog.dismiss();
			}
			init();
		}
	};
	private Handler dropCacheHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (dropDialog != null) {
					dropDialog.dismiss();
				}

				finish();
				return;
			} catch (Exception e) {
				warning.showToast(res.getString(R.string.err_drop));

				Log.e(cgSettings.tag, "cgeopopup.dropCacheHandler: " + e.toString());
			}

			if (dropDialog != null) {
				dropDialog.dismiss();
			}
			init();
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
		setTheme(R.style.transparent);
		setContentView(R.layout.popup);
		base.setTitle(activity, res.getString(R.string.detail));

		// get parameters
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			fromDetail = extras.getBoolean("fromdetail");
			geocode = extras.getString("geocode");
		}

		if (geocode == null || geocode.length() == 0) {
			warning.showToast(res.getString(R.string.err_detail_cache_find));

			finish();
			return;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 2, 0, res.getString(R.string.cache_menu_compass)).setIcon(android.R.drawable.ic_menu_compass); // compass

		SubMenu subMenu = menu.addSubMenu(1, 0, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_more);
		NavigationAppFactory.addMenuItems(subMenu, activity, res);

		menu.add(0, 6, 0, res.getString(R.string.cache_menu_visit)).setIcon(android.R.drawable.ic_menu_agenda); // log visit
		menu.add(0, 5, 0, res.getString(R.string.cache_menu_around)).setIcon(android.R.drawable.ic_menu_rotate); // caches around
		menu.add(0, 7, 0, res.getString(R.string.cache_menu_browser)).setIcon(android.R.drawable.ic_menu_info_details); // browser

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		try {
			if (cache != null && cache.latitude != null && cache.longitude != null) {
				menu.findItem(0).setVisible(true);
				menu.findItem(2).setVisible(true);
				menu.findItem(5).setVisible(true);
			} else {
				menu.findItem(0).setVisible(false);
				menu.findItem(2).setVisible(false);
				menu.findItem(5).setVisible(false);
			}

			if (fromDetail == false && settings.isLogin() == true) {
				menu.findItem(6).setEnabled(true);
			} else {
				menu.findItem(6).setEnabled(false);
			}
		} catch (Exception e) {
			// nothing
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int menuItem = item.getItemId();

		if (menuItem == 2) {
			navigateTo();
			return true;
		} else if (menuItem == 5) {
			cachesAround();
			return true;
		} else if (menuItem == 6) {
			if (cache.cacheid == null || cache.cacheid.length() == 0) {
				warning.showToast(res.getString(R.string.err_cannot_log_visit));
				return false;
			}

			Intent logVisitIntent = new Intent(activity, cgeovisit.class);
			logVisitIntent.putExtra("id", cache.cacheid);
			logVisitIntent.putExtra("geocode", cache.geocode.toUpperCase());
			logVisitIntent.putExtra("type", cache.type.toLowerCase());
			activity.startActivity(logVisitIntent);

			activity.finish();

			return true;
		} else if (menuItem == 7) {
			activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.geocaching.com/seek/cache_details.aspx?wp=" + cache.geocode)));
			return true;
		}

		return NavigationAppFactory.onMenuItemSelected(item, geo, activity, res, warning, cache, null, null, null);
	}

	private void init() {
		if (geo == null) {
			geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
		}

		app.setAction(geocode);

		cache = app.getCacheByGeocode(geocode);

		if (cache == null) {
			warning.showToast(res.getString(R.string.err_detail_cache_find));

			finish();
			return;
		}

		try {
			RelativeLayout itemLayout;
			TextView itemName;
			TextView itemValue;
			LinearLayout itemStars;

			if (gcIcons == null || gcIcons.isEmpty()) {
				gcIcons.put("ape", R.drawable.type_ape);
				gcIcons.put("cito", R.drawable.type_cito);
				gcIcons.put("earth", R.drawable.type_earth);
				gcIcons.put("event", R.drawable.type_event);
				gcIcons.put("letterbox", R.drawable.type_letterbox);
				gcIcons.put("locationless", R.drawable.type_locationless);
				gcIcons.put("mega", R.drawable.type_mega);
				gcIcons.put("multi", R.drawable.type_multi);
				gcIcons.put("traditional", R.drawable.type_traditional);
				gcIcons.put("virtual", R.drawable.type_virtual);
				gcIcons.put("webcam", R.drawable.type_webcam);
				gcIcons.put("wherigo", R.drawable.type_wherigo);
				gcIcons.put("mystery", R.drawable.type_mystery);
				gcIcons.put("gchq", R.drawable.type_hq);
			}

			if (cache.name != null && cache.name.length() > 0) {
				base.setTitle(activity, cache.name);
			} else {
				base.setTitle(activity, geocode.toUpperCase());
			}

			inflater = activity.getLayoutInflater();
			geocode = cache.geocode.toUpperCase();

			((ScrollView) findViewById(R.id.details_list_box)).setVisibility(View.VISIBLE);
			LinearLayout detailsList = (LinearLayout) findViewById(R.id.details_list);
			detailsList.removeAllViews();

			// actionbar icon
			if (cache.type != null && gcIcons.containsKey(cache.type) == true) { // cache icon
				((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds((Drawable) activity.getResources().getDrawable(gcIcons.get(cache.type)), null, null, null);
			} else { // unknown cache type, "mystery" icon
				((TextView) findViewById(R.id.actionbar_title)).setCompoundDrawablesWithIntrinsicBounds((Drawable) activity.getResources().getDrawable(gcIcons.get("mystery")), null, null, null);
			}

			// cache type
			itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
			itemName = (TextView) itemLayout.findViewById(R.id.name);
			itemValue = (TextView) itemLayout.findViewById(R.id.value);

			itemName.setText(res.getString(R.string.cache_type));
			if (cgBase.cacheTypesInv.containsKey(cache.type) == true) { // cache icon
				if (cache.size != null && cache.size.length() > 0) {
					itemValue.setText(cgBase.cacheTypesInv.get(cache.type) + " (" + cache.size + ")");
				} else {
					itemValue.setText(cgBase.cacheTypesInv.get(cache.type));
				}
			} else {
				if (cache.size != null && cache.size.length() > 0) {
					itemValue.setText(cgBase.cacheTypesInv.get("mystery") + " (" + cache.size + ")");
				} else {
					itemValue.setText(cgBase.cacheTypesInv.get("mystery"));
				}
			}
			detailsList.addView(itemLayout);

			// gc-code
			itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
			itemName = (TextView) itemLayout.findViewById(R.id.name);
			itemValue = (TextView) itemLayout.findViewById(R.id.value);

			itemName.setText(res.getString(R.string.cache_geocode));
			itemValue.setText(cache.geocode.toUpperCase());
			detailsList.addView(itemLayout);

			// cache state
			if (cache.archived == true || cache.disabled == true || cache.members == true || cache.found == true) {
				itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
				itemName = (TextView) itemLayout.findViewById(R.id.name);
				itemValue = (TextView) itemLayout.findViewById(R.id.value);

				itemName.setText(res.getString(R.string.cache_status));

				StringBuilder state = new StringBuilder();
				if (cache.found == true) {
					if (state.length() > 0) {
						state.append(", ");
					}
					state.append(res.getString(R.string.cache_status_found));
				}
				if (cache.archived == true) {
					if (state.length() > 0) {
						state.append(", ");
					}
					state.append(res.getString(R.string.cache_status_archived));
				}
				if (cache.disabled == true) {
					if (state.length() > 0) {
						state.append(", ");
					}
					state.append(res.getString(R.string.cache_status_disabled));
				}
				if (cache.members == true) {
					if (state.length() > 0) {
						state.append(", ");
					}
					state.append(res.getString(R.string.cache_status_premium));
				}

				itemValue.setText(state.toString());
				detailsList.addView(itemLayout);

				state = null;
			}

			// distance
			itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_item, null);
			itemName = (TextView) itemLayout.findViewById(R.id.name);
			itemValue = (TextView) itemLayout.findViewById(R.id.value);

			itemName.setText(res.getString(R.string.cache_distance));
			itemValue.setText("--");
			detailsList.addView(itemLayout);
			cacheDistance = itemValue;

			// difficulty
			if (cache.difficulty > 0f) {
				itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_layout, null);
				itemName = (TextView) itemLayout.findViewById(R.id.name);
				itemValue = (TextView) itemLayout.findViewById(R.id.value);
				itemStars = (LinearLayout) itemLayout.findViewById(R.id.stars);

				itemName.setText(res.getString(R.string.cache_difficulty));
				itemValue.setText(String.format(Locale.getDefault(), "%.1f", cache.difficulty) + " of 5");
				for (int i = 0; i <= 4; i++) {
					ImageView star = (ImageView) inflater.inflate(R.layout.star, null);
					if ((cache.difficulty - i) >= 1.0) {
						star.setImageResource(R.drawable.star_on);
					} else if ((cache.difficulty - i) > 0.0) {
						star.setImageResource(R.drawable.star_half);
					} else {
						star.setImageResource(R.drawable.star_off);
					}
					itemStars.addView(star);
				}
				detailsList.addView(itemLayout);
			}

			// terrain
			if (cache.terrain > 0f) {
				itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_layout, null);
				itemName = (TextView) itemLayout.findViewById(R.id.name);
				itemValue = (TextView) itemLayout.findViewById(R.id.value);
				itemStars = (LinearLayout) itemLayout.findViewById(R.id.stars);

				itemName.setText(res.getString(R.string.cache_terrain));
				itemValue.setText(String.format(Locale.getDefault(), "%.1f", cache.terrain) + " of 5");
				for (int i = 0; i <= 4; i++) {
					ImageView star = (ImageView) inflater.inflate(R.layout.star, null);
					if ((cache.terrain - i) >= 1.0) {
						star.setImageResource(R.drawable.star_on);
					} else if ((cache.terrain - i) > 0.0) {
						star.setImageResource(R.drawable.star_half);
					} else {
						star.setImageResource(R.drawable.star_off);
					}
					itemStars.addView(star);
				}
				detailsList.addView(itemLayout);
			}

			// rating
			if (cache.rating != null && cache.rating > 0) {
				setRating(cache.rating, cache.votes);
			} else {
				(new Thread() {

					public void run() {
						cgRating rating = base.getRating(cache.guid, geocode);

						Message msg = new Message();
						Bundle bundle = new Bundle();

						if (rating == null || rating.rating == null) {
							return;
						}

						bundle.putFloat("rating", rating.rating);
						bundle.putInt("votes", rating.votes);
						msg.setData(bundle);

						ratingHandler.sendMessage(msg);
					}
				}).start();
			}

			// more details
			if (fromDetail == false) {
				((LinearLayout) findViewById(R.id.more_details_box)).setVisibility(View.VISIBLE);

				Button buttonMore = (Button) findViewById(R.id.more_details);
				buttonMore.setOnClickListener(new OnClickListener() {

					public void onClick(View arg0) {
						Intent cachesIntent = new Intent(activity, cgeodetail.class);
						cachesIntent.putExtra("geocode", geocode.toUpperCase());
						activity.startActivity(cachesIntent);

						activity.finish();
						return;
					}
				});
			} else {
				((LinearLayout) findViewById(R.id.more_details_box)).setVisibility(View.GONE);
			}

			if (fromDetail == false) {
				((LinearLayout) findViewById(R.id.offline_box)).setVisibility(View.VISIBLE);

				// offline use
				final TextView offlineText = (TextView) findViewById(R.id.offline_text);
				final Button offlineRefresh = (Button) findViewById(R.id.offline_refresh);
				final Button offlineStore = (Button) findViewById(R.id.offline_store);

				if (cache.reason > 0) {
					Long diff = (System.currentTimeMillis() / (60 * 1000)) - (cache.detailedUpdate / (60 * 1000)); // minutes

					String ago = "";
					if (diff < 15) {
						ago = res.getString(R.string.cache_offline_time_mins_few);
					} else if (diff < 50) {
						ago = res.getString(R.string.cache_offline_time_about) + " " + diff + " " + res.getString(R.string.cache_offline_time_mins);
					} else if (diff < 90) {
						ago = res.getString(R.string.cache_offline_time_about) + " " + res.getString(R.string.cache_offline_time_hour);
					} else if (diff < (48 * 60)) {
						ago = res.getString(R.string.cache_offline_time_about) + " " + (diff / 60) + " " + res.getString(R.string.cache_offline_time_hours);
					} else {
						ago = res.getString(R.string.cache_offline_time_about) + " " + (diff / (24 * 60)) + " " + res.getString(R.string.cache_offline_time_days);
					}

					offlineText.setText(res.getString(R.string.cache_offline_stored) + "\n" + ago);

					offlineRefresh.setVisibility(View.VISIBLE);
					offlineRefresh.setEnabled(true);
					offlineRefresh.setOnClickListener(new storeCache());

					offlineStore.setText(res.getString(R.string.cache_offline_drop));
					offlineStore.setEnabled(true);
					offlineStore.setOnClickListener(new dropCache());
				} else {
					offlineText.setText(res.getString(R.string.cache_offline_not_ready));

					offlineRefresh.setVisibility(View.GONE);
					offlineRefresh.setEnabled(false);
					offlineRefresh.setOnTouchListener(null);
					offlineRefresh.setOnClickListener(null);

					offlineStore.setText(res.getString(R.string.cache_offline_store));
					offlineStore.setEnabled(true);
					offlineStore.setOnClickListener(new storeCache());
				}
			} else {
				((LinearLayout) findViewById(R.id.offline_box)).setVisibility(View.GONE);
			}
		} catch (Exception e) {
			Log.e(cgSettings.tag, "cgeopopup.init: " + e.toString());
		}

		if (geo != null) {
			geoUpdate.updateLoc(geo);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		init();
	}

	@Override
	public void onResume() {
		super.onResume();

		settings.load();
		init();
	}

	@Override
	public void onDestroy() {
		if (geo != null) {
			geo = app.removeGeo();
		}

		super.onDestroy();
	}

	@Override
	public void onStop() {
		if (geo != null) {
			geo = app.removeGeo();
		}

		super.onStop();
	}

	@Override
	public void onPause() {
		if (geo != null) {
			geo = app.removeGeo();
		}

		super.onPause();
	}

	private class update extends cgUpdateLoc {

		@Override
		public void updateLoc(cgGeo geo) {
			if (geo == null) {
				return;
			}

			try {
				if (geo.latitudeNow != null && geo.longitudeNow != null && cache != null && cache.latitude != null && cache.longitude != null) {
					cacheDistance.setText(base.getHumanDistance(cgBase.getDistance(geo.latitudeNow, geo.longitudeNow, cache.latitude, cache.longitude)));
					cacheDistance.bringToFront();
				}
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
	}

	private void navigateTo() {
		if (cache == null || cache.latitude == null || cache.longitude == null) {
			warning.showToast(res.getString(R.string.err_location_unknown));
		}

		cgeonavigate navigateActivity = new cgeonavigate();

		Intent navigateIntent = new Intent(activity, navigateActivity.getClass());
		navigateIntent.putExtra("latitude", cache.latitude);
		navigateIntent.putExtra("longitude", cache.longitude);
		navigateIntent.putExtra("geocode", "");
		navigateIntent.putExtra("name", "Some destination");

		activity.startActivity(navigateIntent);
	}

	private void cachesAround() {
		if (cache == null || cache.latitude == null || cache.longitude == null) {
			warning.showToast(res.getString(R.string.err_location_unknown));
		}

		cgeocaches cachesActivity = new cgeocaches();

		Intent cachesIntent = new Intent(activity, cachesActivity.getClass());

		cachesIntent.putExtra("type", "coordinate");
		cachesIntent.putExtra("latitude", cache.latitude);
		cachesIntent.putExtra("longitude", cache.longitude);
		cachesIntent.putExtra("cachetype", settings.cacheType);

		activity.startActivity(cachesIntent);

		finish();
	}

	private class storeCache implements View.OnClickListener {

		public void onClick(View arg0) {
			if (dropDialog != null && dropDialog.isShowing() == true) {
				warning.showToast("Still removing this cache.");
				return;
			}

			storeDialog = ProgressDialog.show(activity, res.getString(R.string.cache_dialog_offline_save_title), res.getString(R.string.cache_dialog_offline_save_message), true);
			storeDialog.setCancelable(false);
			Thread thread = new storeCacheThread(storeCacheHandler);
			thread.start();
		}
	}

	private class storeCacheThread extends Thread {

		private Handler handler = null;

		public storeCacheThread(Handler handlerIn) {
			handler = handlerIn;
		}

		@Override
		public void run() {
			base.storeCache(app, activity, cache, null, 1, handler);
		}
	}

	private class dropCache implements View.OnClickListener {

		public void onClick(View arg0) {
			if (storeDialog != null && storeDialog.isShowing() == true) {
				warning.showToast("Still saving this cache.");
				return;
			}

			dropDialog = ProgressDialog.show(activity, res.getString(R.string.cache_dialog_offline_drop_title), res.getString(R.string.cache_dialog_offline_drop_message), true);
			dropDialog.setCancelable(false);
			Thread thread = new dropCacheThread(dropCacheHandler);
			thread.start();
		}
	}

	private class dropCacheThread extends Thread {

		private Handler handler = null;

		public dropCacheThread(Handler handlerIn) {
			handler = handlerIn;
		}

		@Override
		public void run() {
			base.dropCache(app, activity, cache, handler);
		}
	}

	private void setRating(Float rating, Integer votes) {
		if (rating == null || rating <= 0) {
			return;
		}

		RelativeLayout itemLayout;
		TextView itemName;
		TextView itemValue;
		LinearLayout itemStars;
		LinearLayout detailsList = (LinearLayout) findViewById(R.id.details_list);

		itemLayout = (RelativeLayout) inflater.inflate(R.layout.cache_layout, null);
		itemName = (TextView) itemLayout.findViewById(R.id.name);
		itemValue = (TextView) itemLayout.findViewById(R.id.value);
		itemStars = (LinearLayout) itemLayout.findViewById(R.id.stars);

		itemName.setText(res.getString(R.string.cache_rating));
		itemValue.setText(String.format(Locale.getDefault(), "%.1f", rating) + " of 5");
		for (int i = 0; i <= 4; i++) {
			ImageView star = (ImageView) inflater.inflate(R.layout.star, null);
			if ((rating - i) >= 1.0) {
				star.setImageResource(R.drawable.star_on);
			} else if ((rating - i) > 0.0) {
				star.setImageResource(R.drawable.star_half);
			} else {
				star.setImageResource(R.drawable.star_off);
			}
			itemStars.addView(star, (1 + i));
		}
		if (votes != null) {
			final TextView itemAddition = (TextView) itemLayout.findViewById(R.id.addition);
			itemAddition.setText("(" + votes + ")");
			itemAddition.setVisibility(View.VISIBLE);
		}
		detailsList.addView(itemLayout);
	}

	public void goHome(View view) {
		base.goHome(activity);
	}

	public void goCompass(View view) {
		if (cache == null || cache.latitude == null || cache.longitude == null) {
			warning.showToast(res.getString(R.string.cache_coordinates_no));

			return;
		}

		cgeonavigate navigateActivity = new cgeonavigate();

		Intent navigateIntent = new Intent(activity, navigateActivity.getClass());
		navigateIntent.putExtra("latitude", cache.latitude);
		navigateIntent.putExtra("longitude", cache.longitude);
		navigateIntent.putExtra("geocode", cache.geocode.toUpperCase());
		navigateIntent.putExtra("name", cache.name);

		activity.startActivity(navigateIntent);

		finish();
	}

	public void goManual(View view) {
		try {
			AppManualReaderClient.openManual(
				"c-geo",
				"c:geo-cache-info",
				activity,
				"http://cgeo.carnero.cc/manual/"
			);
		} catch (Exception e) {
			// nothing
		}

		finish();
	}
}