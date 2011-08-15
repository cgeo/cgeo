package cgeo.geocaching;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;

public class cgeowaypoint extends AbstractActivity {

	private static final int MENU_ID_NAVIGATION = 0;
	private static final int MENU_ID_CACHES_AROUND = 5;
	private static final int MENU_ID_COMPASS = 2;
	private cgWaypoint waypoint = null;
	private String geocode = null;
	private int id = -1;
	private ProgressDialog waitDialog = null;
	private cgGeo geo = null;
	private cgUpdateLoc geoUpdate = new update();
	private Handler loadWaypointHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (waypoint == null) {
					if (waitDialog != null) {
						waitDialog.dismiss();
						waitDialog = null;
					}

					showToast(res.getString(R.string.err_waypoint_load_failed));

					finish();
					return;
				} else {
					final TextView identification = (TextView) findViewById(R.id.identification);
					final TextView coords = (TextView) findViewById(R.id.coordinates);
					final ImageView compass = (ImageView) findViewById(R.id.compass);
					final View separator = (View) findViewById(R.id.separator);

					final View headline = (View) findViewById(R.id.headline);
					registerNavigationMenu(headline);

					if (waypoint.name != null && waypoint.name.length() > 0) {
						setTitle(Html.fromHtml(waypoint.name.trim()).toString());
					} else {
						setTitle(res.getString(R.string.waypoint_title));
					}

					if (waypoint.prefix.equalsIgnoreCase("OWN") == false) {
						identification.setText(waypoint.prefix.trim() + "/" + waypoint.lookup.trim());
					} else {
						identification.setText(res.getString(R.string.waypoint_custom));
					}
					registerNavigationMenu(identification);
					waypoint.setIcon(res, base, identification);

					if (waypoint.latitude != null && waypoint.longitude != null) {
						coords.setText(Html.fromHtml(cgBase.formatCoordinate(waypoint.latitude, "lat", true) + " | " + cgBase.formatCoordinate(waypoint.longitude, "lon", true)), TextView.BufferType.SPANNABLE);
						compass.setVisibility(View.VISIBLE);
						separator.setVisibility(View.VISIBLE);
					} else {
						coords.setText(res.getString(R.string.waypoint_unknown_coordinates));
						compass.setVisibility(View.GONE);
						separator.setVisibility(View.GONE);
					}
					registerNavigationMenu(coords);

					if (waypoint.note != null && waypoint.note.length() > 0) {
						final TextView note = (TextView) findViewById(R.id.note);
						note.setText(Html.fromHtml(waypoint.note.trim()), TextView.BufferType.SPANNABLE);
						registerNavigationMenu(note);
					}

					Button buttonEdit = (Button) findViewById(R.id.edit);
					buttonEdit.setOnClickListener(new editWaypointListener(waypoint.id));

					Button buttonDelete = (Button) findViewById(R.id.delete);
					if (waypoint.type != null && waypoint.type.equalsIgnoreCase("own")) {
						buttonDelete.setOnClickListener(new deleteWaypointListener(waypoint.id));
						buttonDelete.setVisibility(View.VISIBLE);
					}

					if (waitDialog != null) {
						waitDialog.dismiss();
						waitDialog = null;
					}
				}
			} catch (Exception e) {
				if (waitDialog != null) {
					waitDialog.dismiss();
					waitDialog = null;
				}
				Log.e(cgSettings.tag, "cgeowaypoint.loadWaypointHandler: " + e.toString());
			}
		}

		private void registerNavigationMenu(View view) {
			view.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					registerForContextMenu(v);
					openContextMenu(v);
				}
			});
		}
	};

	public cgeowaypoint() {
		super("c:geo-waypoint-details");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTheme();
		setContentView(R.layout.waypoint);
		setTitle("waypoint");

		// get parameters
		Bundle extras = getIntent().getExtras();

		// try to get data from extras
		if (extras != null) {
			id = extras.getInt("waypoint");
			geocode = extras.getString("geocode");
		}

		if (id <= 0) {
			showToast(res.getString(R.string.err_waypoint_unknown));
			finish();
			return;
		}

		if (geo == null) {
			geo = app.startGeo(this, geoUpdate, base, settings, 0, 0);
		}

		waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
		waitDialog.setCancelable(true);

		(new loadWaypoint()).start();
	}

	@Override
	public void onResume() {
		super.onResume();

		settings.load();

		if (geo == null) {
			geo = app.startGeo(this, geoUpdate, base, settings, 0, 0);
		}

		if (waitDialog == null) {
			waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
			waitDialog.setCancelable(true);

			(new loadWaypoint()).start();
		}
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ID_COMPASS, 0, res.getString(R.string.cache_menu_compass)).setIcon(android.R.drawable.ic_menu_compass); // compass

		SubMenu subMenu = menu.addSubMenu(1, MENU_ID_NAVIGATION, 0, res.getString(R.string.cache_menu_navigate)).setIcon(android.R.drawable.ic_menu_more);
		addNavigationMenuItems(subMenu);

		menu.add(0, MENU_ID_CACHES_AROUND, 0, res.getString(R.string.cache_menu_around)).setIcon(android.R.drawable.ic_menu_rotate); // caches around

		return true;
	}

	private void addNavigationMenuItems(Menu menu) {
		NavigationAppFactory.addMenuItems(menu, this, res);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		try {
			boolean visible = waypoint != null && waypoint.latitude != null && waypoint.longitude != null;
			menu.findItem(MENU_ID_NAVIGATION).setVisible(visible);
			menu.findItem(MENU_ID_COMPASS).setVisible(visible);
			menu.findItem(MENU_ID_CACHES_AROUND).setVisible(visible);
		} catch (Exception e) {
			// nothing
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int menuItem = item.getItemId();
		if (menuItem == MENU_ID_COMPASS) {
			goCompass(null);
			return true;
		} else if (menuItem == MENU_ID_CACHES_AROUND) {
			cachesAround();
			return true;
		}

		return NavigationAppFactory.onMenuItemSelected(item, geo, this, res, null, null, waypoint, null);
	}

	private void cachesAround() {
		if (waypoint == null || waypoint.latitude == null || waypoint.longitude == null) {
			showToast(res.getString(R.string.err_location_unknown));
		}

		cgeocaches cachesActivity = new cgeocaches();

		Intent cachesIntent = new Intent(this, cachesActivity.getClass());
		cachesIntent.putExtra("type", "coordinate");
		cachesIntent.putExtra("latitude", waypoint.latitude);
		cachesIntent.putExtra("longitude", waypoint.longitude);
		cachesIntent.putExtra("cachetype", settings.cacheType);

		startActivity(cachesIntent);

		finish();
	}

	private class loadWaypoint extends Thread {

		@Override
		public void run() {
			try {
				waypoint = app.loadWaypoint(id);

				loadWaypointHandler.sendMessage(new Message());
			} catch (Exception e) {
				Log.e(cgSettings.tag, "cgeowaypoint.loadWaypoint.run: " + e.toString());
			}
		}
	}

	private class update extends cgUpdateLoc {

		@Override
		public void updateLoc(cgGeo geo) {
			// nothing
		}
	}

	private class editWaypointListener implements View.OnClickListener {

		private int id = -1;

		public editWaypointListener(int idIn) {
			id = idIn;
		}

		public void onClick(View arg0) {
			Intent editIntent = new Intent(cgeowaypoint.this, cgeowaypointadd.class);
			editIntent.putExtra("waypoint", id);
			startActivity(editIntent);
		}
	}

	private class deleteWaypointListener implements View.OnClickListener {

		private Integer id = null;

		public deleteWaypointListener(int idIn) {
			id = idIn;
		}

		public void onClick(View arg0) {
			if (app.deleteWaypoint(id) == false) {
				showToast(res.getString(R.string.err_waypoint_delete_failed));
			} else {
				app.removeCacheFromCache(geocode);

				finish();
				return;
			}
		}
	}

	public void goCompass(View view) {
		if (waypoint == null || waypoint.latitude == null || waypoint.longitude == null) {
			showToast(res.getString(R.string.err_location_unknown));
		}

		Intent navigateIntent = new Intent(this, cgeonavigate.class);
		navigateIntent.putExtra("latitude", waypoint.latitude);
		navigateIntent.putExtra("longitude", waypoint.longitude);
		navigateIntent.putExtra("geocode", waypoint.prefix.trim() + "/" + waypoint.lookup.trim());
		navigateIntent.putExtra("name", waypoint.name);

		if (cgeonavigate.coordinates != null) {
			cgeonavigate.coordinates.clear();
		}
		cgeonavigate.coordinates = new ArrayList<cgCoord>();
		cgeonavigate.coordinates.add(new cgCoord(waypoint));
		startActivity(navigateIntent);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle(res.getString(R.string.cache_menu_navigate));
		addNavigationMenuItems(menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return onOptionsItemSelected(item);
	}
}