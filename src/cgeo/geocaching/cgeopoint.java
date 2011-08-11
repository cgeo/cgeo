package cgeo.geocaching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;

public class cgeopoint extends AbstractActivity {

	private class DestinationHistoryAdapter extends ArrayAdapter<cgDestination> {
		private LayoutInflater inflater = null;

		public DestinationHistoryAdapter(Context context,
				List<cgDestination> objects) {
			super(context, 0, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			cgDestination loc = getItem(position);

			if (convertView == null) {
				convertView = getInflater().inflate(R.layout.simple_way_point,
						null);
			}
			TextView longitude = (TextView) convertView
					.findViewById(R.id.simple_way_point_longitude);
			TextView latitude = (TextView) convertView
					.findViewById(R.id.simple_way_point_latitude);
			TextView date = (TextView) convertView.findViewById(R.id.date);

			String lonString = cgBase.formatCoordinate(loc.getLongitude(), "lon",
					true);
			String latString = cgBase.formatCoordinate(loc.getLatitude(), "lat",
					true);

			longitude.setText(lonString);
			latitude.setText(latString);
			CharSequence dateString = DateFormat.format("dd/MM/yy kk:mm",
					loc.getDate());
			date.setText(dateString);

			return convertView;
		}

		private LayoutInflater getInflater() {
			if (inflater == null) {
				inflater = ((Activity) getContext()).getLayoutInflater();
			}

			return inflater;
		}
	}

	private cgGeo geo = null;
	private cgUpdateLoc geoUpdate = new update();
	private EditText latEdit = null;
	private EditText lonEdit = null;
	private boolean changed = false;
	private List<cgDestination> historyOfSearchedLocations;
	private DestinationHistoryAdapter destionationHistoryAdapter;
	private ListView historyListView;
	private TextView historyFooter;

	private static final int CONTEXT_MENU_DELETE_WAYPOINT = Menu.FIRST;

	public cgeopoint() {
		super("c:geo-navigate-any");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTheme();
		setContentView(R.layout.point);
		setTitle(res.getString(R.string.search_destination));

		createHistoryView();

		init();
	}

	private void createHistoryView() {
		historyListView = (ListView) findViewById(R.id.historyList);

		View pointControls = getLayoutInflater().inflate(
				R.layout.point_controls, null);
		historyListView.addHeaderView(pointControls);

		if (getHistoryOfSearchedLocations().isEmpty()) {
			historyListView.addFooterView(getEmptyHistoryFooter(), null, false);
		}

		historyListView.setAdapter(getDestionationHistoryAdapter());
		historyListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Object selection = arg0.getItemAtPosition(arg2);
				if (selection instanceof cgDestination) {
					List<Double> coords = new ArrayList<Double>(2);
					coords.add(((cgDestination) selection).getLatitude());
					coords.add(((cgDestination) selection).getLongitude());

					navigateTo(coords);
				}
			}
		});
		historyListView
				.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

					@Override
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						menu.add(Menu.NONE, CONTEXT_MENU_DELETE_WAYPOINT,
								Menu.NONE, R.string.waypoint_delete);
					}
				});
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case CONTEXT_MENU_DELETE_WAYPOINT:
			AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();
			Object destination = historyListView
					.getItemAtPosition(menuInfo.position);
			if (destination instanceof cgDestination) {
				removeFromHistory((cgDestination) destination);
			}
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	private TextView getEmptyHistoryFooter() {
		if (historyFooter == null) {
			historyFooter = (TextView) getLayoutInflater().inflate(
					R.layout.caches_footer, null);
			historyFooter.setText(R.string.search_history_empty);
		}
		return historyFooter;
	}

	private DestinationHistoryAdapter getDestionationHistoryAdapter() {
		if (destionationHistoryAdapter == null) {
			destionationHistoryAdapter = new DestinationHistoryAdapter(this,
					getHistoryOfSearchedLocations());
		}
		return destionationHistoryAdapter;
	}

	private List<cgDestination> getHistoryOfSearchedLocations() {
		if (historyOfSearchedLocations == null) {
			// Load from database
			historyOfSearchedLocations = app.getHistoryOfSearchedLocations();
		}

		return historyOfSearchedLocations;
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

	private void init() {
		if (geo == null) {
			geo = app.startGeo(this, geoUpdate, base, settings, 0, 0);
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
			latitudeEdit.setText(cgBase.formatCoordinate(Double.valueOf(prefs.getFloat("anylatitude", 0f)), "lat", true));
			longitudeEdit.setText(cgBase.formatCoordinate(Double.valueOf(prefs.getFloat("anylongitude", 0f)), "lon", true));
		}

		Button buttonCurrent = (Button) findViewById(R.id.current);
		buttonCurrent.setOnClickListener(new currentListener());

		getDestionationHistoryAdapter().notifyDataSetChanged();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 2, 0, res.getString(R.string.cache_menu_compass)).setIcon(android.R.drawable.ic_menu_compass); // compass

		SubMenu subMenu = menu.addSubMenu(1, 0, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_more);
		NavigationAppFactory.addMenuItems(subMenu, this, res);

		menu.add(0, 5, 0, res.getString(R.string.cache_menu_around)).setIcon(android.R.drawable.ic_menu_rotate); // caches around

		// clear history
		MenuItem clearHistoryItem = menu.add(0, 6, 0, res.getString(R.string.search_clear_history));
		clearHistoryItem.setIcon(android.R.drawable.ic_menu_delete);

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

			menu.findItem(6).setEnabled(!getHistoryOfSearchedLocations().isEmpty());
		} catch (Exception e) {
			// nothing
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int menuItem = item.getItemId();

		ArrayList<Double> coords = getDestination();

		if(coords != null && !coords.isEmpty())
		{
			addToHistory(coords);
		}

		if (menuItem == 2) {
			navigateTo();
			return true;
		} else if (menuItem == 5) {
			cachesAround();
			return true;
		}
		else if (menuItem == 6) {
			clearHistory();
			return true;
		}

		return NavigationAppFactory.onMenuItemSelected(item, geo, this, res, null, null, null, coords);
	}

	private void addToHistory(ArrayList<Double> coords) {
		// Add locations to history
		cgDestination loc = new cgDestination();
		loc.setLatitude(coords.get(0));
		loc.setLongitude(coords.get(1));

		if(!getHistoryOfSearchedLocations().contains(loc))
		{
			loc.setDate(System.currentTimeMillis());
			getHistoryOfSearchedLocations().add(0,loc);

			// Save location
			app.saveSearchedDestination(loc);

			// Ensure to remove the footer
			historyListView.removeFooterView(getEmptyHistoryFooter());
		}
	}

	private void removeFromHistory(cgDestination destination) {
		if (getHistoryOfSearchedLocations().contains(destination)) {
			getHistoryOfSearchedLocations().remove(destination);

			// Save
			app.removeSearchedDestinations(destination);

			if (getHistoryOfSearchedLocations().isEmpty()) {
				if (historyListView.getFooterViewsCount() == 0) {
					historyListView.addFooterView(getEmptyHistoryFooter());
				}
			}

			getDestionationHistoryAdapter().notifyDataSetChanged();

			showToast(res.getString(R.string.search_remove_destination));
		}
	}

	private void clearHistory() {
		if (!getHistoryOfSearchedLocations().isEmpty()) {
			getHistoryOfSearchedLocations().clear();

			// Save
			app.clearSearchedDestinations();

			if (historyListView.getFooterViewsCount() == 0) {
				historyListView.addFooterView(getEmptyHistoryFooter());
			}

			getDestionationHistoryAdapter().notifyDataSetChanged();

			showToast(res.getString(R.string.search_history_cleared));
		}
	}

	private void navigateTo() {
		navigateTo(getDestination());
	}

	private void navigateTo(List<Double> coords) {
		if (coords == null || coords.get(0) == null || coords.get(1) == null) {
			showToast(res.getString(R.string.err_location_unknown));
		}

		cgeonavigate navigateActivity = new cgeonavigate();

		Intent navigateIntent = new Intent(this, navigateActivity.getClass());
		navigateIntent.putExtra("latitude", coords.get(0));
		navigateIntent.putExtra("longitude", coords.get(1));
		navigateIntent.putExtra("geocode", "");
		navigateIntent.putExtra("name", "Some destination");

		startActivity(navigateIntent);
	}

	private void cachesAround() {
		ArrayList<Double> coords = getDestination();

		if (coords == null || coords.get(0) == null || coords.get(1) == null) {
			showToast(res.getString(R.string.err_location_unknown));
		}

		cgeocaches cachesActivity = new cgeocaches();

		Intent cachesIntent = new Intent(this, cachesActivity.getClass());

		cachesIntent.putExtra("type", "coordinate");
		cachesIntent.putExtra("latitude", coords.get(0));
		cachesIntent.putExtra("longitude", coords.get(1));
		cachesIntent.putExtra("cachetype", settings.cacheType);

		startActivity(cachesIntent);

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

				latEdit.setHint(cgBase.formatCoordinate(geo.latitudeNow, "lat", false));
				lonEdit.setHint(cgBase.formatCoordinate(geo.longitudeNow, "lon", false));
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
	}

	private class currentListener implements View.OnClickListener {

		public void onClick(View arg0) {
			if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
				showToast(res.getString(R.string.err_point_unknown_position));
				return;
			}

			((EditText) findViewById(R.id.latitude)).setText(cgBase.formatCoordinate(geo.latitudeNow, "lat", true));
			((EditText) findViewById(R.id.longitude)).setText(cgBase.formatCoordinate(geo.longitudeNow, "lon", true));

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
			showToast(res.getString(R.string.err_point_no_position_given));
			return null;
		}

		if (latText != null && latText.length() > 0 && lonText != null && lonText.length() > 0) {
			// latitude & longitude
			HashMap<String, Object> latParsed = cgBase.parseCoordinate(latText, "lat");
			HashMap<String, Object> lonParsed = cgBase.parseCoordinate(lonText, "lon");

			if (latParsed == null || latParsed.get("coordinate") == null || latParsed.get("string") == null) {
				showToast(res.getString(R.string.err_parse_lat));
				return null;
			}

			if (lonParsed == null || lonParsed.get("coordinate") == null || lonParsed.get("string") == null) {
				showToast(res.getString(R.string.err_parse_lon));
				return null;
			}

			latitude = (Double) latParsed.get("coordinate");
			longitude = (Double) lonParsed.get("coordinate");
		} else {
			if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
				showToast(res.getString(R.string.err_point_curr_position_unavailable));
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
				helpDialog(res.getString(R.string.err_point_bear_and_dist_title), res.getString(R.string.err_point_bear_and_dist));
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
				showToast(res.getString(R.string.err_parse_dist));
				return null;
			}

			Double latParsed = null;
			Double lonParsed = null;

			HashMap<String, Double> coordsDst = cgBase.getRadialDistance(latitude, longitude, bearing, distance);

			latParsed = coordsDst.get("latitude");
			lonParsed = coordsDst.get("longitude");

			if (latParsed == null || lonParsed == null) {
				showToast(res.getString(R.string.err_point_location_error));
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

			edit.putFloat("anylatitude", latitude.floatValue());
			edit.putFloat("anylongitude", longitude.floatValue());

			edit.commit();
		} else {
			SharedPreferences.Editor edit = prefs.edit();

			edit.remove("anylatitude");
			edit.remove("anylongitude");

			edit.commit();
		}
	}
}
