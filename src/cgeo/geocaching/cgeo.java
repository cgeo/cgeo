package cgeo.geocaching;

import gnu.android.app.appmanualclient.*;

import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

public class cgeo extends Activity {

	private Resources res = null;
	private cgeoapplication app = null;
	private Context context = null;
	private cgSettings settings = null;
	private SharedPreferences prefs = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private Integer version = null;
	private cgGeo geo = null;
	private cgUpdateLoc geoUpdate = new update();
	private TextView navType = null;
	private TextView navAccuracy = null;
	private TextView navSatellites = null;
	private TextView navLocation = null;
	private TextView filterTitle = null;
	private TextView countBubble = null;
	private boolean cleanupRunning = false;
	private int countBubbleCnt = 0;
	private Double addLat = null;
	private Double addLon = null;
	private List<Address> addresses = null;
	private boolean addressObtaining = false;
	private boolean initialized = false;
	private Handler countBubbleHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (countBubble == null) {
					countBubble = (TextView) findViewById(R.id.offline_count);
				}

				if (countBubbleCnt == 0) {
					countBubble.setVisibility(View.GONE);
				} else {
					countBubble.setText(Integer.toString(countBubbleCnt));
					countBubble.bringToFront();
					countBubble.setVisibility(View.VISIBLE);
				}
			} catch (Exception e) {
				Log.w(cgSettings.tag, "cgeo.countBubbleHander: " + e.toString());
			}
		}
	};
	private Handler obtainAddressHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (addresses != null && addresses.isEmpty() == false) {
					final Address address = addresses.get(0);
					final StringBuilder addText = new StringBuilder();

					if (address.getCountryName() != null) {
						addText.append(address.getCountryName());
					}
					if (address.getLocality() != null) {
						if (addText.length() > 0) {
							addText.append(", ");
						}
						addText.append(address.getLocality());
					} else if (address.getAdminArea() != null) {
						if (addText.length() > 0) {
							addText.append(", ");
						}
						addText.append(address.getAdminArea());
					}

					addLat = geo.latitudeNow;
					addLon = geo.longitudeNow;

					if (navLocation == null) {
						navLocation = (TextView) findViewById(R.id.nav_location);
					}

					navLocation.setText(addText.toString());
				}
			} catch (Exception e) {
				// nothing
			}

			addresses = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		context = this;
		res = this.getResources();
		app = (cgeoapplication) this.getApplication();
		app.setAction(null);
		settings = new cgSettings(this, getSharedPreferences(cgSettings.preferences, 0));
		prefs = getSharedPreferences(cgSettings.preferences, 0);
		base = new cgBase(app, settings, getSharedPreferences(cgSettings.preferences, 0));
		warning = new cgWarning(this);

		app.cleanGeo();
		app.cleanDir();

		setContentView(R.layout.main);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL); // type to search

		try {
			PackageManager manager = this.getPackageManager();
			PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);

			version = info.versionCode;

			base.sendAnal(context, "/?ver=" + info.versionCode);
			Log.i(cgSettings.tag, "Starting " + info.packageName + " " + info.versionCode + " a.k.a " + info.versionName + "...");

			info = null;
			manager = null;
		} catch (Exception e) {
			base.sendAnal(context, "/");
			Log.i(cgSettings.tag, "No info.");
		}

		try {
			if (settings.helper == 0) {
				RelativeLayout helper = (RelativeLayout) findViewById(R.id.helper);
				if (helper != null) {
					helper.setVisibility(View.VISIBLE);
					helper.setClickable(true);
					helper.setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							try {
								AppManualReaderClient.openManual(
										"c-geo",
										"c:geo-intro",
										context,
										"http://cgeo.carnero.cc/manual/");
							} catch (Exception e) {
								// nothing
							}

							view.setVisibility(View.GONE);
						}
					});

					final SharedPreferences.Editor edit = getSharedPreferences(cgSettings.preferences, 0).edit();
					edit.putInt("helper", 1);
					edit.commit();
				}
			}
		} catch (Exception e) {
			// nothing
		}

		init();
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
		initialized = false;

		if (geo != null) {
			geo = app.removeGeo();
		}

		super.onDestroy();
	}

	@Override
	public void onStop() {
		initialized = false;

		if (geo != null) {
			geo = app.removeGeo();
		}

		super.onStop();
	}

	@Override
	public void onPause() {
		initialized = false;

		if (geo != null) {
			geo = app.removeGeo();
		}

		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, res.getString(R.string.menu_about)).setIcon(android.R.drawable.ic_menu_help);
		menu.add(0, 1, 0, res.getString(R.string.menu_helpers)).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, 2, 0, res.getString(R.string.menu_settings)).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, 3, 0, res.getString(R.string.menu_history)).setIcon(android.R.drawable.ic_menu_recent_history);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int id = item.getItemId();
		if (id == 0) {
			showAbout(null);

			return true;
		} else if (id == 1) {
			context.startActivity(new Intent(context, cgeohelpers.class));

			return true;
		} else if (id == 2) {
			context.startActivity(new Intent(context, cgeoinit.class));

			return true;
		} else if (id == 3) {
			final Intent cachesIntent = new Intent(context, cgeocaches.class);
			cachesIntent.putExtra("type", "history");
			context.startActivity(cachesIntent);

			return true;
		}

		return false;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.setHeaderTitle(res.getString(R.string.menu_filter));

		//first add the most used types
		menu.add(1, 0, 0, res.getString(R.string.all_types));
		menu.add(1, 1, 0, res.getString(R.string.traditional));
		menu.add(1, 2, 0, res.getString(R.string.multi));
		menu.add(1, 3, 0, res.getString(R.string.mystery));

		// then add all other cache types sorted alphabetically
		HashMap<String, String> allTypes = (HashMap<String, String>) base.cacheTypesInv.clone();
		allTypes.remove("traditional");
		allTypes.remove("multi");
		allTypes.remove("mystery");
		ArrayList<String> sorted = new ArrayList<String>(allTypes.values());
		Collections.sort(sorted);
		for (String choice : sorted) {
			menu.add(1, menu.size(), 0, choice);
		}

		// mark current filter as checked
		menu.setGroupCheckable(1, true, true);
		boolean foundItem = false;
		int itemCount = menu.size();
		if (settings.cacheType != null) {
			String typeTitle = cgBase.cacheTypesInv.get(settings.cacheType);
			if (typeTitle != null) {
				for (int i = 0; i < itemCount; i++) {
					if (menu.getItem(i).getTitle().equals(typeTitle)) {
						menu.getItem(i).setChecked(true);
						foundItem = true;
						break;
					}
				}
			}
		}
		if (!foundItem) {
			menu.getItem(0).setChecked(true);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int id = item.getItemId();

		if (id == 0) {
			settings.setCacheType(null);
			setFilterTitle();

			return true;
		} else if (id > 0) {
			String itemTitle = item.getTitle().toString();
			String choice = null;
			for (Entry<String, String> entry : cgBase.cacheTypesInv.entrySet()) {
				if (entry.getValue().equalsIgnoreCase(itemTitle)) {
					choice = entry.getKey();
					break;
				}
			}
			if (choice == null) {
				settings.setCacheType(null);
			} else {
				settings.setCacheType(choice);
			}
			setFilterTitle();

			return true;
		}

		return false;
	}

	private void setFilterTitle() {
		if (filterTitle == null) {
			filterTitle = (TextView) findViewById(R.id.filter_button_title);
		}
		if (settings.cacheType != null) {
			filterTitle.setText(cgBase.cacheTypesInv.get(settings.cacheType));
		} else {
			filterTitle.setText(res.getString(R.string.all));
		}
	}

	private void init() {
		if (initialized == true) {
			return;
		}

		initialized = true;

		settings.getLogin();
		settings.reloadCacheType();

		if (app.firstRun == true) {
			new Thread() {

				@Override
				public void run() {
					int status = base.login();

					if (status == 1) {
						app.firstRun = false;
					}
				}
			}.start();
		}

		(new countBubbleUpdate()).start();
		(new cleanDatabase()).start();

		if (settings.cacheType != null && cgBase.cacheTypesInv.containsKey(settings.cacheType) == false) {
			settings.setCacheType(null);
		}

		if (geo == null) {
			geo = app.startGeo(context, geoUpdate, base, settings, warning, 0, 0);
		}

		navType = (TextView) findViewById(R.id.nav_type);
		navAccuracy = (TextView) findViewById(R.id.nav_accuracy);
		navLocation = (TextView) findViewById(R.id.nav_location);

		final LinearLayout findOnMap = (LinearLayout) findViewById(R.id.map);
		findOnMap.setClickable(true);
		findOnMap.setOnClickListener(new cgeoFindOnMapListener());

		final RelativeLayout findByOffline = (RelativeLayout) findViewById(R.id.search_offline);
		findByOffline.setClickable(true);
		findByOffline.setOnClickListener(new cgeoFindByOfflineListener());

		(new countBubbleUpdate()).start();

		final LinearLayout advanced = (LinearLayout) findViewById(R.id.advanced_button);
		advanced.setClickable(true);
		advanced.setOnClickListener(new cgeoSearchListener());

		final LinearLayout any = (LinearLayout) findViewById(R.id.any_button);
		any.setClickable(true);
		any.setOnClickListener(new cgeoPointListener());

		final LinearLayout filter = (LinearLayout) findViewById(R.id.filter_button);
		registerForContextMenu(filter);
		filter.setOnClickListener(new View.OnClickListener() {

			public void onClick(View view) {
				openContextMenu(view);
			}
		});
		filter.setClickable(true);

		setFilterTitle();
	}

	private class update extends cgUpdateLoc {

		@Override
		public void updateLoc(cgGeo geo) {
			if (geo == null) {
				return;
			}

			try {
				if (navType == null || navLocation == null || navAccuracy == null) {
					navType = (TextView) findViewById(R.id.nav_type);
					navAccuracy = (TextView) findViewById(R.id.nav_accuracy);
					navSatellites = (TextView) findViewById(R.id.nav_satellites);
					navLocation = (TextView) findViewById(R.id.nav_location);
				}

				if (geo.latitudeNow != null && geo.longitudeNow != null) {
					LinearLayout findNearest = (LinearLayout) findViewById(R.id.nearest);
					findNearest.setClickable(true);
					findNearest.setOnClickListener(new cgeoFindNearestListener());

					String satellites = null;
					if (geo.satellitesVisible != null && geo.satellitesFixed != null && geo.satellitesFixed > 0) {
						satellites = res.getString(R.string.loc_sat) + ": " + geo.satellitesFixed + "/" + geo.satellitesVisible;
					} else if (geo.satellitesVisible != null) {
						satellites = res.getString(R.string.loc_sat) + ": 0/" + geo.satellitesVisible;
					} else {
						satellites = "";
					}
					navSatellites.setText(satellites);

					if (geo.gps == -1) {
						navType.setText(res.getString(R.string.loc_last));
					} else if (geo.gps == 0) {
						navType.setText(res.getString(R.string.loc_net));
					} else {
						navType.setText(res.getString(R.string.loc_gps));
					}

					if (geo.accuracyNow != null) {
						if (settings.units == cgSettings.unitsImperial) {
							navAccuracy.setText("±" + String.format(Locale.getDefault(), "%.0f", (geo.accuracyNow * 3.2808399)) + " ft");
						} else {
							navAccuracy.setText("±" + String.format(Locale.getDefault(), "%.0f", geo.accuracyNow) + " m");
						}
					} else {
						navAccuracy.setText(null);
					}

					if (settings.showAddress == 1) {
						if (addLat == null || addLon == null) {
							navLocation.setText(res.getString(R.string.loc_no_addr));
						}
						if (addLat == null || addLon == null || (cgBase.getDistance(geo.latitudeNow, geo.longitudeNow, addLat, addLon) > 0.5 && addressObtaining == false)) {
							(new obtainAddress()).start();
						}
					} else {
						if (geo.altitudeNow != null) {
							String humanAlt;
							if (settings.units == cgSettings.unitsImperial) {
								humanAlt = String.format("%.0f", (geo.altitudeNow * 3.2808399)) + " ft";
							} else {
								humanAlt = String.format("%.0f", geo.altitudeNow) + " m";
							}
							navLocation.setText(base.formatCoordinate(geo.latitudeNow, "lat", true) + " | " + base.formatCoordinate(geo.longitudeNow, "lon", true) + " | " + humanAlt);
						} else {
							navLocation.setText(base.formatCoordinate(geo.latitudeNow, "lat", true) + " | " + base.formatCoordinate(geo.longitudeNow, "lon", true));
						}
					}
				} else {
					Button findNearest = (Button) findViewById(R.id.nearest);
					findNearest.setClickable(false);
					findNearest.setOnClickListener(null);

					navType.setText(null);
					navAccuracy.setText(null);
					navLocation.setText(res.getString(R.string.loc_trying));
				}
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
	}

	private class cgeoFindNearestListener implements View.OnClickListener {

		public void onClick(View arg0) {
			if (geo == null) {
				return;
			}

			final Intent cachesIntent = new Intent(context, cgeocaches.class);
			cachesIntent.putExtra("type", "nearest");
			cachesIntent.putExtra("latitude", geo.latitudeNow);
			cachesIntent.putExtra("longitude", geo.longitudeNow);
			cachesIntent.putExtra("cachetype", settings.cacheType);
			context.startActivity(cachesIntent);
		}
	}

	private class cgeoFindOnMapListener implements View.OnClickListener {

		public void onClick(View arg0) {
			context.startActivity(new Intent(context, settings.getMapFactory().getMapClass()));
		}
	}

	private class cgeoFindByOfflineListener implements View.OnClickListener {

		public void onClick(View arg0) {
			final Intent cachesIntent = new Intent(context, cgeocaches.class);
			cachesIntent.putExtra("type", "offline");
			context.startActivity(cachesIntent);
		}
	}

	private class cgeoSearchListener implements View.OnClickListener {

		public void onClick(View arg0) {
			context.startActivity(new Intent(context, cgeoadvsearch.class));
		}
	}

	private class cgeoPointListener implements View.OnClickListener {

		public void onClick(View arg0) {
			context.startActivity(new Intent(context, cgeopoint.class));
		}
	}

	private class countBubbleUpdate extends Thread {

		@Override
		public void run() {
			if (app == null) {
				return;
			}

			int checks = 0;
			while (app.storageStatus() == false) {
				try {
					wait(500);
					checks++;
				} catch (Exception e) {
					// nothing;
				}

				if (checks > 10) {
					return;
				}
			}


			countBubbleCnt = app.getAllStoredCachesCount(true, null, null);

			countBubbleHandler.sendEmptyMessage(0);
		}
	}

	private class cleanDatabase extends Thread {

		@Override
		public void run() {
			if (app == null) {
				return;
			}
			if (cleanupRunning == true) {
				return;
			}

			boolean more = false;
			if (version != settings.version) {
				Log.i(cgSettings.tag, "Initializing hard cleanup - version changed from " + settings.version + " to " + version + ".");

				more = true;
			}

			cleanupRunning = true;
			app.cleanDatabase(more);
			cleanupRunning = false;

			if (version != null && version > 0) {
				SharedPreferences.Editor edit = prefs.edit();
				edit.putInt("version", version);
				edit.commit();
			}
		}
	}

	private class obtainAddress extends Thread {

		public obtainAddress() {
			setPriority(Thread.MIN_PRIORITY);
		}

		@Override
		public void run() {
			if (geo == null) {
				return;
			}
			if (addressObtaining == true) {
				return;
			}
			addressObtaining = true;

			try {
				Geocoder geocoder = new Geocoder(context, Locale.getDefault());

				addresses = geocoder.getFromLocation(geo.latitudeNow, geo.longitudeNow, 1);
			} catch (Exception e) {
				Log.i(cgSettings.tag, "Failed to obtain address");
			}

			obtainAddressHandler.sendEmptyMessage(0);

			addressObtaining = false;
		}
	}

	public void showAbout(View view) {
		context.startActivity(new Intent(context, cgeoabout.class));
	}

	public void goSearch(View view) {
		onSearchRequested();
	}

	public void goManual(View view) {
		try {
			AppManualReaderClient.openManual(
					"c-geo",
					"c:geo-main-screen",
					context,
					"http://cgeo.carnero.cc/manual/");
		} catch (Exception e) {
			// nothing
		}
	}
}
