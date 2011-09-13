package cgeo.geocaching;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import cgeo.geocaching.geopoint.DistanceParser;
import cgeo.geocaching.geopoint.Geopoint;

public class cgeopoint extends AbstractActivity {
	
	public static final Pattern patternA = Pattern.compile("^([0-9\\.\\,]+)[ ]*m$", Pattern.CASE_INSENSITIVE); // m
	public static final Pattern patternB = Pattern.compile("^([0-9\\.\\,]+)[ ]*km$", Pattern.CASE_INSENSITIVE); // km
	public static final Pattern patternC = Pattern.compile("^([0-9\\.\\,]+)[ ]*ft$", Pattern.CASE_INSENSITIVE); // ft - 0.3048m
	public static final Pattern patternD = Pattern.compile("^([0-9\\.\\,]+)[ ]*yd$", Pattern.CASE_INSENSITIVE); // yd - 0.9144m
	public static final Pattern patternE = Pattern.compile("^([0-9\\.\\,]+)[ ]*mi$", Pattern.CASE_INSENSITIVE); // mi - 1609.344m

	private static class DestinationHistoryAdapter extends ArrayAdapter<cgDestination> {
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

			String lonString = cgBase.formatLongitude(loc.getCoords().getLongitude(), true);
			String latString = cgBase.formatLatitude(loc.getCoords().getLatitude(), true);

			longitude.setText(lonString);
			latitude.setText(latString);
			date.setText(cgBase.formatShortDateTime(getContext(), loc.getDate()));

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
	private Button latButton = null;
	private Button lonButton = null;
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
					navigateTo(((cgDestination) selection).getCoords());
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

		latButton = (Button) findViewById(R.id.buttonLatitude);
		lonButton = (Button) findViewById(R.id.buttonLongitude);

		latButton.setOnClickListener(new coordDialogListener());
		lonButton.setOnClickListener(new coordDialogListener());

		if (prefs.contains("anylatitude") && prefs.contains("anylongitude")) {
			latButton.setText(cgBase.formatLatitude(Double.valueOf(prefs.getFloat("anylatitude", 0f)), true));
			lonButton.setText(cgBase.formatLongitude(Double.valueOf(prefs.getFloat("anylongitude", 0f)), true));
		}

		Button buttonCurrent = (Button) findViewById(R.id.current);
		buttonCurrent.setOnClickListener(new currentListener());

		getDestionationHistoryAdapter().notifyDataSetChanged();
	}
	
	private class coordDialogListener implements View.OnClickListener {

		public void onClick(View arg0) {
			Geopoint gp = null;
			if (latButton.getText().length() > 0 && lonButton.getText().length() > 0) {
				gp = new Geopoint(latButton.getText().toString() + " " + lonButton.getText().toString());
			}
			cgeocoords coordsDialog = new cgeocoords(cgeopoint.this, settings, gp, geo);
			coordsDialog.setCancelable(true);
			coordsDialog.setOnCoordinateUpdate(new cgeocoords.CoordinateUpdate() {
				@Override
				public void update(Geopoint gp) {
					latButton.setText(cgBase.formatLatitude(gp.getLatitude(), true));
					lonButton.setText(cgBase.formatLongitude(gp.getLongitude(), true));
					changed = true;
				}
			});
			coordsDialog.show();
		}
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
			final Geopoint coords = getDestination();

			if (coords != null) {
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

		final Geopoint coords = getDestination();

		if(coords != null)
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

	private void addToHistory(final Geopoint coords) {
		// Add locations to history
		cgDestination loc = new cgDestination();
		loc.setCoords(coords);

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

	private void navigateTo(Geopoint geopoint) {
		if (geopoint == null) {
			showToast(res.getString(R.string.err_location_unknown));
			return;
		}

		cgeonavigate navigateActivity = new cgeonavigate();

		Intent navigateIntent = new Intent(this, navigateActivity.getClass());
		navigateIntent.putExtra("latitude", geopoint.getLatitude());
		navigateIntent.putExtra("longitude", geopoint.getLongitude());
		navigateIntent.putExtra("geocode", "");
		navigateIntent.putExtra("name", "Some destination");

		startActivity(navigateIntent);
	}

	private void cachesAround() {
		final Geopoint coords = getDestination();

		if (coords == null) {
			showToast(res.getString(R.string.err_location_unknown));
			return;
		}

		cgeocaches cachesActivity = new cgeocaches();

		Intent cachesIntent = new Intent(this, cachesActivity.getClass());

		cachesIntent.putExtra("type", "coordinate");
		cachesIntent.putExtra("latitude", coords.getLatitude());
		cachesIntent.putExtra("longitude", coords.getLongitude());
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
				latButton.setHint(cgBase.formatLatitude(geo.coordsNow.getLatitude(), false));
				lonButton.setHint(cgBase.formatLongitude(geo.coordsNow.getLongitude(), false));
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
	}

	private class currentListener implements View.OnClickListener {

		public void onClick(View arg0) {
			if (geo == null || geo.coordsNow == null) {
				showToast(res.getString(R.string.err_point_unknown_position));
				return;
			}

			latButton.setText(cgBase.formatLatitude(geo.coordsNow.getLatitude(), true));
			lonButton.setText(cgBase.formatLongitude(geo.coordsNow.getLongitude(), true));

			changed = false;
		}
	}

	private Geopoint getDestination() {
		Geopoint result = null;
		Geopoint coords = null;

		String bearingText = ((EditText) findViewById(R.id.bearing)).getText().toString();
		String distanceText = ((EditText) findViewById(R.id.distance)).getText().toString();
		String latText = latButton.getText().toString();
		String lonText = lonButton.getText().toString();

		if (StringUtils.isBlank(bearingText) && StringUtils.isBlank(distanceText)
				&& StringUtils.isBlank(latText) && StringUtils.isBlank(lonText)) {
			showToast(res.getString(R.string.err_point_no_position_given));
			return null;
		}

		if (StringUtils.isNotBlank(latText) && StringUtils.isNotBlank(lonText)) {
			// latitude & longitude
			Map<String, Object> latParsed = cgBase.parseCoordinate(latText, "lat");
			Map<String, Object> lonParsed = cgBase.parseCoordinate(lonText, "lon");

			if (latParsed == null || latParsed.get("coordinate") == null || latParsed.get("string") == null) {
				showToast(res.getString(R.string.err_parse_lat));
				return null;
			}

			if (lonParsed == null || lonParsed.get("coordinate") == null || lonParsed.get("string") == null) {
				showToast(res.getString(R.string.err_parse_lon));
				return null;
			}

			coords = new Geopoint((Double) latParsed.get("coordinate"), (Double) lonParsed.get("coordinate"));
		} else {
			if (geo == null || geo.coordsNow == null) {
				showToast(res.getString(R.string.err_point_curr_position_unavailable));
				return null;
			}

			coords = geo.coordsNow;
		}

		if (StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)) {
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

<<<<<<< HEAD
			Double distance = null; // km

			Matcher matcherA = patternA.matcher(distanceText);
			Matcher matcherB = patternB.matcher(distanceText);
			Matcher matcherC = patternC.matcher(distanceText);
			Matcher matcherD = patternD.matcher(distanceText);
			Matcher matcherE = patternE.matcher(distanceText);

			if (matcherA.find() && matcherA.groupCount() > 0) {
				distance = (new Double(matcherA.group(1))) * 0.001;
			} else if (matcherB.find() && matcherB.groupCount() > 0) {
				distance = new Double(matcherB.group(1));
			} else if (matcherC.find() && matcherC.groupCount() > 0) {
				distance = (new Double(matcherC.group(1))) * 0.0003048;
			} else if (matcherD.find() && matcherD.groupCount() > 0) {
				distance = (new Double(matcherD.group(1))) * 0.0009144;
			} else if (matcherE.find() && matcherE.groupCount() > 0) {
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
=======
			double distance;
			try {
			    distance = DistanceParser.parseDistance(distanceText, settings.units);
			} catch (NumberFormatException e) {
>>>>>>> refs/remotes/upstream/master
				showToast(res.getString(R.string.err_parse_dist));
				return null;
			}

			final Geopoint coordsDst = coords.project(bearing, distance);

			if (coordsDst == null) {
				showToast(res.getString(R.string.err_point_location_error));
				return null;
			}

			result = coordsDst;
		} else if (coords != null) {
			result = coords;
		} else {
			return null;
		}

		saveCoords(result);

		return result;
	}

	private void saveCoords(final Geopoint coords) {
		if (changed && coords != null) {
			SharedPreferences.Editor edit = prefs.edit();

			edit.putFloat("anylatitude", (float) coords.getLatitude());
			edit.putFloat("anylongitude", (float) coords.getLongitude());

			edit.commit();
		} else {
			SharedPreferences.Editor edit = prefs.edit();

			edit.remove("anylatitude");
			edit.remove("anylongitude");

			edit.commit();
		}
	}
}
