package cgeo.geocaching;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.Geopoint;

public class cgeowaypointadd extends AbstractActivity {

	private String geocode = null;
	private int id = -1;
	private cgGeo geo = null;
	private cgUpdateLoc geoUpdate = new update();
	private ProgressDialog waitDialog = null;
	private cgWaypoint waypoint = null;
	private String type = "own";
	private String prefix = "OWN";
	private String lookup = "---";
	/**
	 * number of waypoints that the corresponding cache has until now
	 */
	private int wpCount = 0;
	private Handler loadWaypointHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			try {
				if (waypoint == null) {
					if (waitDialog != null) {
						waitDialog.dismiss();
						waitDialog = null;
					}

					id = -1;
				} else {
					geocode = waypoint.geocode;
					type = waypoint.type;
					prefix = waypoint.prefix;
					lookup = waypoint.lookup;

					app.setAction(geocode);

					((Button) findViewById(R.id.buttonLatitude)).setText(cgBase.formatLatitude(waypoint.latitude, true));
					((Button) findViewById(R.id.buttonLongitude)).setText(cgBase.formatLongitude(waypoint.longitude, true));
					((EditText) findViewById(R.id.name)).setText(Html.fromHtml(waypoint.name.trim()).toString());
					((EditText) findViewById(R.id.note)).setText(Html.fromHtml(waypoint.note.trim()).toString());

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
				Log.e(cgSettings.tag, "cgeowaypointadd.loadWaypointHandler: " + e.toString());
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTheme();
		setContentView(R.layout.waypoint_new);
		setTitle("waypoint");

		if (geo == null) {
			geo = app.startGeo(this, geoUpdate, base, settings, 0, 0);
		}

		// get parameters
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			geocode = extras.getString("geocode");
			wpCount = extras.getInt("count", 0);
			id = extras.getInt("waypoint");
		}

		if (StringUtils.isBlank(geocode) && id <= 0) {
			showToast(res.getString(R.string.err_waypoint_cache_unknown));

			finish();
			return;
		}

		if (id <= 0) {
			setTitle(res.getString(R.string.waypoint_add_title));
		} else {
			setTitle(res.getString(R.string.waypoint_edit_title));
		}

		if (geocode != null) {
			app.setAction(geocode);
		}

		Button buttonLat = (Button) findViewById(R.id.buttonLatitude);
		buttonLat.setOnClickListener(new coordDialogListener());
		Button buttonLon = (Button) findViewById(R.id.buttonLongitude);
		buttonLon.setOnClickListener(new coordDialogListener());

		Button addWaypoint = (Button) findViewById(R.id.add_waypoint);
		addWaypoint.setOnClickListener(new coordsListener());

		List<String> wayPointNames = new ArrayList<String>(cgBase.waypointTypes.values());
		AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.name);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, wayPointNames);
		textView.setAdapter(adapter);


		if (id > 0) {
			waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
			waitDialog.setCancelable(true);

			(new loadWaypoint()).start();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		settings.load();

		if (geo == null) {
			geo = app.startGeo(this, geoUpdate, base, settings, 0, 0);
		}

		if (id > 0) {
			if (waitDialog == null) {
				waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
				waitDialog.setCancelable(true);

				(new loadWaypoint()).start();
			}
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

	private class update extends cgUpdateLoc {

		@Override
		public void updateLoc(cgGeo geo) {
			if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
				return;
			}

			try {
				Button bLat = (Button) findViewById(R.id.buttonLatitude);
				Button bLon = (Button) findViewById(R.id.buttonLongitude);
				bLat.setHint(cgBase.formatLatitude(geo.latitudeNow, false));
				bLon.setHint(cgBase.formatLongitude(geo.longitudeNow, false));
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
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

	private class coordDialogListener implements View.OnClickListener {

		public void onClick(View arg0) {
			Geopoint gp = null;
			if (waypoint != null && waypoint.latitude != null && waypoint.longitude != null)
				gp = new Geopoint(waypoint.latitude, waypoint.longitude);
			cgeocoords coordsDialog = new cgeocoords(cgeowaypointadd.this, settings, gp, geo);
			coordsDialog.setCancelable(true);
			coordsDialog.setOnCoordinateUpdate(new cgeocoords.CoordinateUpdate() {
				@Override
				public void update(Geopoint gp) {
					((Button) findViewById(R.id.buttonLatitude)).setText(cgBase.formatLatitude(gp.getLatitude(), true));
					((Button) findViewById(R.id.buttonLongitude)).setText(cgBase.formatLongitude(gp.getLongitude(), true));
					if (waypoint != null) {
						waypoint.latitude = gp.getLatitude();
						waypoint.longitude = gp.getLongitude();
					}
				}
			});
			coordsDialog.show();
		}
	}

	private class coordsListener implements View.OnClickListener {

		public void onClick(View arg0) {
			List<Double> coords = new ArrayList<Double>();
			Double latitude = null;
			Double longitude = null;

			final String bearingText = ((EditText) findViewById(R.id.bearing)).getText().toString();
			final String distanceText = ((EditText) findViewById(R.id.distance)).getText().toString();
			final String latText = ((Button) findViewById(R.id.buttonLatitude)).getText().toString();
			final String lonText = ((Button) findViewById(R.id.buttonLongitude)).getText().toString();

			if (StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)
							&& StringUtils.isNotBlank(latText) && StringUtils.isNotBlank(lonText)) {
				helpDialog(res.getString(R.string.err_point_no_position_given_title), res.getString(R.string.err_point_no_position_given));
				return;
			}

			if (StringUtils.isNotBlank(latText) && StringUtils.isNotBlank(lonText)) {
				// latitude & longitude
				Map<String, Object> latParsed = cgBase.parseCoordinate(latText, "lat");
				Map<String, Object> lonParsed = cgBase.parseCoordinate(lonText, "lon");

				if (latParsed == null || latParsed.get("coordinate") == null || latParsed.get("string") == null) {
					showToast(res.getString(R.string.err_parse_lat));
					return;
				}

				if (lonParsed == null || lonParsed.get("coordinate") == null || lonParsed.get("string") == null) {
					showToast(res.getString(R.string.err_parse_lon));
					return;
				}

				latitude = (Double) latParsed.get("coordinate");
				longitude = (Double) lonParsed.get("coordinate");
			} else {
				if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
					showToast(res.getString(R.string.err_point_curr_position_unavailable));
					return;
				}

				latitude = geo.latitudeNow;
				longitude = geo.longitudeNow;
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
					return;
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
							distance = (new Double(distanceText)) * 1.609344; // considering it miles
						} else {
							distance = (new Double(distanceText)) * 0.001; // considering it meters
						}
					} catch (Exception e) {
						// probably not a number
					}
				}

				if (distance == null) {
					showToast(res.getString(R.string.err_parse_dist));
					return;
				}

				Double latParsed = null;
				Double lonParsed = null;

				Map<String, Double> coordsDst = cgBase.getRadialDistance(latitude, longitude, bearing, distance);

				latParsed = coordsDst.get("latitude");
				lonParsed = coordsDst.get("longitude");

				if (latParsed == null || lonParsed == null) {
					showToast(res.getString(R.string.err_point_location_error));
					return;
				}

				coords.add(0, (Double) latParsed);
				coords.add(1, (Double) lonParsed);
			} else if (latitude != null && longitude != null) {
				coords.add(0, latitude);
				coords.add(1, longitude);
			} else {
				showToast(res.getString(R.string.err_point_location_error));
				return;
			}

			String name = ((EditText) findViewById(R.id.name)).getText().toString().trim();
			// if no name is given, just give the waypoint its number as name
			if (name.length() == 0) {
				name = res.getString(R.string.waypoint) + " " + String.valueOf(wpCount + 1);
			}
			final String note = ((EditText) findViewById(R.id.note)).getText().toString().trim();

			final cgWaypoint waypoint = new cgWaypoint();
			waypoint.type = type;
			waypoint.geocode = geocode;
			waypoint.prefix = prefix;
			waypoint.lookup = lookup;
			waypoint.name = name;
			waypoint.latitude = coords.get(0);
			waypoint.longitude = coords.get(1);
			waypoint.latitudeString = cgBase.formatLatitude(coords.get(0), true);
			waypoint.longitudeString = cgBase.formatLongitude(coords.get(1), true);
			waypoint.note = note;

			if (app.saveOwnWaypoint(id, geocode, waypoint)) {
				app.removeCacheFromCache(geocode);

				finish();
				return;
			} else {
				showToast(res.getString(R.string.err_waypoint_add_failed));
			}
		}
	}

	public void goManual(View view) {
		if (id >= 0) {
			ActivityMixin.goManual(this, "c:geo-waypoint-edit");
		} else {
			ActivityMixin.goManual(this, "c:geo-waypoint-new");
		}
	}
}
