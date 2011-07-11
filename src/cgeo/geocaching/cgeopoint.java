package cgeo.geocaching;

import gnu.android.app.appmanualclient.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cgeopoint extends Activity {

	private Resources res = null;
	private cgeoapplication app = null;
	private cgSettings settings = null;
	private SharedPreferences prefs = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private Activity activity = null;
	private GoogleAnalyticsTracker tracker = null;
	private cgGeo geo = null;
	private cgUpdateLoc geoUpdate = new update();
	private EditText latEdit = null;
	private EditText lonEdit = null;
	private boolean changed = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// init
		activity = this;
		app = (cgeoapplication) this.getApplication();
		res = this.getResources();
		settings = new cgSettings(activity, activity.getSharedPreferences(cgSettings.preferences, 0));
		prefs = getSharedPreferences(cgSettings.preferences, 0);
		base = new cgBase(app, settings, activity.getSharedPreferences(cgSettings.preferences, 0));
		warning = new cgWarning(activity);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.point);
		base.setTitle(activity, res.getString(R.string.search_destination));

		// google analytics
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start(cgSettings.analytics, this);
		tracker.dispatch();
		base.sendAnal(activity, tracker, "/point");

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
		if (geo != null) {
			geo = app.removeGeo();
		}
		if (tracker != null) {
			tracker.stop();
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

	private void init() {
		if (geo == null) {
			geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
		}

		EditText latitudeEdit = (EditText) findViewById(R.id.latitude);
		latitudeEdit.setOnKeyListener(new View.OnKeyListener() {

			public boolean onKey(View v, int i, KeyEvent k) {
				changed = true;

				return false;
			}
		});

		EditText longitudeEdit = (EditText) findViewById(R.id.longitude);
		longitudeEdit.setOnKeyListener(new View.OnKeyListener() {

			public boolean onKey(View v, int i, KeyEvent k) {
				changed = true;

				return false;
			}
		});

		if (prefs.contains("anylatitude") == true && prefs.contains("anylongitude") == true) {
			latitudeEdit.setText(base.formatCoordinate(new Double(prefs.getFloat("anylatitude", 0f)), "lat", true));
			longitudeEdit.setText(base.formatCoordinate(new Double(prefs.getFloat("anylongitude", 0f)), "lon", true));
		}

		Button buttonCurrent = (Button) findViewById(R.id.current);
		buttonCurrent.setOnClickListener(new currentListener());
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 2, 0, res.getString(R.string.cache_menu_compass)).setIcon(android.R.drawable.ic_menu_compass); // compass

		SubMenu subMenu = menu.addSubMenu(1, 0, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_more);
		subMenu.add(0, 3, 0, res.getString(R.string.cache_menu_radar)); // radar
		subMenu.add(0, 1, 0, res.getString(R.string.cache_menu_map)); // c:geo map
		if (base.isLocus(activity)) {
			subMenu.add(0, 20, 0, res.getString(R.string.cache_menu_locus)); // ext.: locus
		}
		if (base.isRmaps(activity)) {
			subMenu.add(0, 21, 0, res.getString(R.string.cache_menu_rmaps)); // ext.: rmaps
		}
		subMenu.add(0, 23, 0, res.getString(R.string.cache_menu_map_ext)); // ext.: other
		subMenu.add(0, 4, 0, res.getString(R.string.cache_menu_tbt)); // turn-by-turn
		
		menu.add(0, 5, 0, res.getString(R.string.cache_menu_around)).setIcon(android.R.drawable.ic_menu_rotate); // caches around

		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		try {
			ArrayList<Double> coords = getDestination();
			
			if (coords != null && coords.get(0) != null && coords.get(1) != null) {
				menu.findItem(0).setVisible(true);
				menu.findItem(2).setVisible(true);
				menu.findItem(5).setVisible(true);
			} else {
				menu.findItem(0).setVisible(false);
				menu.findItem(2).setVisible(false);
				menu.findItem(5).setVisible(false);
			}
		} catch (Exception e) {
			// nothing
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int menuItem = item.getItemId();
		
		ArrayList<Double> coords = getDestination();

		if (menuItem == 1) {
			showOnMap();
			return true;
		} else if (menuItem == 2) {
			navigateTo();
			return true;
		} else if (menuItem == 3) {
			radarTo();
			return true;
		} else if (menuItem == 4) {
			if (geo != null) {
				base.runNavigation(activity, res, settings, warning, tracker, coords.get(0), coords.get(1), geo.latitudeNow, geo.longitudeNow);
			} else {
				base.runNavigation(activity, res, settings, warning, tracker, coords.get(0), coords.get(1));
			}

			return true;
		} else if (menuItem == 5) {
			cachesAround();
			return true;
		} else if (menuItem == 20) {
			base.runExternalMap(cgBase.mapAppLocus, activity, res, warning, tracker, coords.get(0), coords.get(1)); // locus
			return true;
		} else if (menuItem == 21) {
			base.runExternalMap(cgBase.mapAppRmaps, activity, res, warning, tracker, coords.get(0), coords.get(1)); // rmaps
			return true;
		} else if (menuItem == 23) {
			base.runExternalMap(cgBase.mapAppAny, activity, res, warning, tracker, coords.get(0), coords.get(1)); // rmaps
			return true;
		}

		return false;
	}
	
	private void showOnMap() {
		ArrayList<Double> coords = getDestination();
		
		if (coords == null || coords.get(0) == null || coords.get(1) == null) {
			warning.showToast(res.getString(R.string.err_location_unknown));
		}
		
		Intent mapIntent = new Intent(activity, settings.getMapFactory().getMapClass());
		
		mapIntent.putExtra("latitude", coords.get(0));
		mapIntent.putExtra("longitude", coords.get(1));

		activity.startActivity(mapIntent);
	}

	private void navigateTo() {
		ArrayList<Double> coords = getDestination();
		
		if (coords == null || coords.get(0) == null || coords.get(1) == null) {
			warning.showToast(res.getString(R.string.err_location_unknown));
		}

		cgeonavigate navigateActivity = new cgeonavigate();

		Intent navigateIntent = new Intent(activity, navigateActivity.getClass());
		navigateIntent.putExtra("latitude", coords.get(0));
		navigateIntent.putExtra("longitude", coords.get(1));
		navigateIntent.putExtra("geocode", "");
		navigateIntent.putExtra("name", "Some destination");

		activity.startActivity(navigateIntent);
	}

	private void radarTo() {
		ArrayList<Double> coords = getDestination();
		
		if (coords == null || coords.get(0) == null || coords.get(1) == null) {
			warning.showToast(res.getString(R.string.err_location_unknown));
		}
		
		try {
			if (cgBase.isIntentAvailable(activity, "com.google.android.radar.SHOW_RADAR") == true) {
				Intent radarIntent = new Intent("com.google.android.radar.SHOW_RADAR");
				radarIntent.putExtra("latitude", new Float(coords.get(0)));
				radarIntent.putExtra("longitude", new Float(coords.get(1)));
				activity.startActivity(radarIntent);
			} else {
				AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
				dialog.setTitle(res.getString(R.string.err_radar_title));
				dialog.setMessage(res.getString(R.string.err_radar_message));
				dialog.setCancelable(true);
				dialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int id) {
						try {
							activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:com.eclipsim.gpsstatus2")));
							dialog.cancel();
						} catch (Exception e) {
							warning.showToast(res.getString(R.string.err_radar_market));
							Log.e(cgSettings.tag, "cgeopoint.radarTo.onClick: " + e.toString());
						}
					}
				});
				dialog.setNegativeButton("no", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});

				AlertDialog alert = dialog.create();
				alert.show();
			}
		} catch (Exception e) {
			warning.showToast(res.getString(R.string.err_radar_generic));
			Log.e(cgSettings.tag, "cgeopoint.radarTo: " + e.toString());
		}
	}
	
	private void cachesAround() {
		ArrayList<Double> coords = getDestination();
		
		if (coords == null || coords.get(0) == null || coords.get(1) == null) {
			warning.showToast(res.getString(R.string.err_location_unknown));
		}
		
		cgeocaches cachesActivity = new cgeocaches();

		Intent cachesIntent = new Intent(activity, cachesActivity.getClass());
		
		cachesIntent.putExtra("type", "coordinate");
		cachesIntent.putExtra("latitude", coords.get(0));
		cachesIntent.putExtra("longitude", coords.get(1));
		cachesIntent.putExtra("cachetype", settings.cacheType);

		activity.startActivity(cachesIntent);

		finish();
	}
	
	private class update extends cgUpdateLoc {

		@Override
		public void updateLoc(cgGeo geo) {
			if (geo == null) {
				return;
			}

			try {
				if (latEdit == null) {
					latEdit = (EditText) findViewById(R.id.latitude);
				}
				if (lonEdit == null) {
					lonEdit = (EditText) findViewById(R.id.longitude);
				}

				latEdit.setHint(base.formatCoordinate(geo.latitudeNow, "lat", false));
				lonEdit.setHint(base.formatCoordinate(geo.longitudeNow, "lon", false));
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
	}

	private class currentListener implements View.OnClickListener {

		public void onClick(View arg0) {
			if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
				warning.showToast(res.getString(R.string.err_point_unknown_position));
				return;
			}

			((EditText) findViewById(R.id.latitude)).setText(base.formatCoordinate(geo.latitudeNow, "lat", true));
			((EditText) findViewById(R.id.longitude)).setText(base.formatCoordinate(geo.longitudeNow, "lon", true));

			changed = false;
		}
	}

	private ArrayList<Double> getDestination() {
		ArrayList<Double> coords = new ArrayList<Double>();
		Double latitude = null;
		Double longitude = null;

		String bearingText = ((EditText) findViewById(R.id.bearing)).getText().toString();
		String distanceText = ((EditText) findViewById(R.id.distance)).getText().toString();
		String latText = ((EditText) findViewById(R.id.latitude)).getText().toString();
		String lonText = ((EditText) findViewById(R.id.longitude)).getText().toString();

		if ((bearingText == null || bearingText.length() == 0) && (distanceText == null || distanceText.length() == 0)
				&& (latText == null || latText.length() == 0) && (lonText == null || lonText.length() == 0)) {
			warning.helpDialog(res.getString(R.string.err_point_no_position_given_title), res.getString(R.string.err_point_no_position_given));
			return null;
		}

		if (latText != null && latText.length() > 0 && lonText != null && lonText.length() > 0) {
			// latitude & longitude
			HashMap<String, Object> latParsed = base.parseCoordinate(latText, "lat");
			HashMap<String, Object> lonParsed = base.parseCoordinate(lonText, "lat");

			if (latParsed == null || latParsed.get("coordinate") == null || latParsed.get("string") == null) {
				warning.showToast(res.getString(R.string.err_parse_lat));
				return null;
			}

			if (lonParsed == null || lonParsed.get("coordinate") == null || lonParsed.get("string") == null) {
				warning.showToast(res.getString(R.string.err_parse_lon));
				return null;
			}

			latitude = (Double) latParsed.get("coordinate");
			longitude = (Double) lonParsed.get("coordinate");
		} else {
			if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
				warning.showToast(res.getString(R.string.err_point_curr_position_unavailable));
				return null;
			}

			latitude = geo.latitudeNow;
			longitude = geo.longitudeNow;
		}

		if (bearingText != null && bearingText.length() > 0 && distanceText != null && distanceText.length() > 0) {
			// bearing & distance
			Double bearing = null;
			try {
				bearing = new Double(bearingText);
			} catch (Exception e) {
				// probably not a number
			}
			if (bearing == null) {
				warning.helpDialog(res.getString(R.string.err_point_bear_and_dist_title), res.getString(R.string.err_point_bear_and_dist));
				return null;
			}

			Double distance = null; // km

			final Pattern patternA = Pattern.compile("^([0-9\\.\\,]+)[ ]*m$", Pattern.CASE_INSENSITIVE); // m
			final Pattern patternB = Pattern.compile("^([0-9\\.\\,]+)[ ]*km$", Pattern.CASE_INSENSITIVE); // km
			final Pattern patternC = Pattern.compile("^([0-9\\.\\,]+)[ ]*ft$", Pattern.CASE_INSENSITIVE); // ft - 0.3048m
			final Pattern patternD = Pattern.compile("^([0-9\\.\\,]+)[ ]*yd$", Pattern.CASE_INSENSITIVE); // yd - 0.9144m
			final Pattern patternE = Pattern.compile("^([0-9\\.\\,]+)[ ]*mi$", Pattern.CASE_INSENSITIVE); // mi - 1609.344m

			Matcher matcherA = patternA.matcher(distanceText);
			Matcher matcherB = patternB.matcher(distanceText);
			Matcher matcherC = patternC.matcher(distanceText);
			Matcher matcherD = patternD.matcher(distanceText);
			Matcher matcherE = patternE.matcher(distanceText);

			if (matcherA.find() == true && matcherA.groupCount() > 0) {
				distance = (new Double(matcherA.group(1))) * 0.001;
			} else if (matcherB.find() == true && matcherB.groupCount() > 0) {
				distance = new Double(matcherB.group(1));
			} else if (matcherC.find() == true && matcherC.groupCount() > 0) {
				distance = (new Double(matcherC.group(1))) * 0.0003048;
			} else if (matcherD.find() == true && matcherD.groupCount() > 0) {
				distance = (new Double(matcherD.group(1))) * 0.0009144;
			} else if (matcherE.find() == true && matcherE.groupCount() > 0) {
				distance = (new Double(matcherE.group(1))) * 1.609344;
			} else {
				try {
					if (settings.units == cgSettings.unitsImperial) {
						distance = (new Double(distanceText)) * 0.0003048; // considering it feet
					} else {
						distance = (new Double(distanceText)) * 0.001; // considering it meters
					}
				} catch (Exception e) {
					// probably not a number
				}
			}

			if (distance == null) {
				warning.showToast(res.getString(R.string.err_parse_dist));
				return null;
			}

			Double latParsed = null;
			Double lonParsed = null;

			HashMap<String, Double> coordsDst = base.getRadialDistance(latitude, longitude, bearing, distance);

			latParsed = coordsDst.get("latitude");
			lonParsed = coordsDst.get("longitude");

			if (latParsed == null || lonParsed == null) {
				warning.showToast(res.getString(R.string.err_point_location_error));
				return null;
			}

			coords.add(0, (Double) latParsed);
			coords.add(1, (Double) lonParsed);
		} else if (latitude != null && longitude != null) {
			coords.add(0, latitude);
			coords.add(1, longitude);
		} else {
			return null;
		}

		saveCoords(coords.get(0), coords.get(1));

		return coords;
	}

	private void saveCoords(Double latitude, Double longitude) {
		if (changed == true && latitude == null || longitude == null) {
			SharedPreferences.Editor edit = prefs.edit();

			edit.putFloat("anylatitude", new Float(latitude));
			edit.putFloat("anylongitude", new Float(longitude));

			edit.commit();
		} else {
			SharedPreferences.Editor edit = prefs.edit();

			edit.remove("anylatitude");
			edit.remove("anylongitude");

			edit.commit();
		}
	}

	public void goHome(View view) {
		base.goHome(activity);
	}

	public void goManual(View view) {
		try {
			AppManualReaderClient.openManual(
					"c-geo",
					"c:geo-navigate-any",
					activity,
					"http://cgeo.carnero.cc/manual/");
		} catch (Exception e) {
			// nothing
		}
	}
}
