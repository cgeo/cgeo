package cgeo.geocaching;

import gnu.android.app.appmanualclient.AppManualReaderClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

public class cgeowaypointadd extends Activity {

	private cgeoapplication app = null;
	private Resources res = null;
	private cgSettings settings = null;
	private cgBase base = null;
	private cgWarning warning = null;
	private Activity activity = null;
	private String geocode = null;
	private int id = -1;
	private cgGeo geo = null;
	private cgUpdateLoc geoUpdate = new update();
	
	private Button bLat;
	private Button bLon;
	private EditText eLatDeg, eLatMin, eLatSec;
	private EditText eLonDeg, eLonMin, eLonSec;
	
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

					updateLatLonFields(waypoint.latitude, waypoint.longitude);
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

		// init
		activity = this;
		res = this.getResources();
		app = (cgeoapplication) this.getApplication();
		settings = new cgSettings(activity, activity.getSharedPreferences(cgSettings.preferences, 0));
		base = new cgBase(app, settings, activity.getSharedPreferences(cgSettings.preferences, 0));
		warning = new cgWarning(activity);

		// set layout
		if (settings.skin == 1) {
			setTheme(R.style.light);
		} else {
			setTheme(R.style.dark);
		}
		setContentView(R.layout.waypoint_new);
		base.setTitle(activity, "waypoint");

		if (geo == null) {
			geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
		}

		// get parameters
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			geocode = extras.getString("geocode");
			wpCount = extras.getInt("count", 0);
			id = extras.getInt("waypoint");
		}

		if ((geocode == null || geocode.length() == 0) && id <= 0) {
			warning.showToast(res.getString(R.string.err_waypoint_cache_unknown));

			finish();
			return;
		}

		if (id <= 0) {
			base.setTitle(activity, res.getString(R.string.waypoint_add_title));
		} else {
			base.setTitle(activity, res.getString(R.string.waypoint_edit_title));
		}

		if (geocode != null) {
			app.setAction(geocode);
		}

		Button buttonCurrent = (Button) findViewById(R.id.current);
		buttonCurrent.setOnClickListener(new currentListener());

		Button addWaypoint = (Button) findViewById(R.id.add_waypoint);
		addWaypoint.setOnClickListener(new coordsListener());

		ArrayList<String> wayPointNames = new ArrayList<String>(cgBase.waypointTypes.values());
		AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.name);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, wayPointNames);
		textView.setAdapter(adapter);


		if (id > 0) {
			waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
			waitDialog.setCancelable(true);

			(new loadWaypoint()).start();
		}
		
		bLat = (Button) findViewById(R.id.ButtonLat);
		eLatDeg  = (EditText) findViewById(R.id.EditTextLatDeg);
		eLatMin  = (EditText) findViewById(R.id.EditTextLatMin);
		eLatSec  = (EditText) findViewById(R.id.EditTextLatSec);

		bLon = (Button) findViewById(R.id.ButtonLon);
		eLonDeg  = (EditText) findViewById(R.id.EditTextLonDeg);
		eLonMin  = (EditText) findViewById(R.id.EditTextLonMin);
		eLonSec  = (EditText) findViewById(R.id.EditTextLonSec);
		
		bLat.setOnClickListener(new buttonClickListener());
		bLon.setOnClickListener(new buttonClickListener());
		eLatDeg.addTextChangedListener(new textChangedListener(1));
		eLatMin.addTextChangedListener(new textChangedListener(2));
		eLatSec.addTextChangedListener(new textChangedListener(3));
		eLonDeg.addTextChangedListener(new textChangedListener(4));
		eLonMin.addTextChangedListener(new textChangedListener(5));
		eLonSec.addTextChangedListener(new textChangedListener(6));
		
		((EditText) findViewById(R.id.name)).requestFocus();//TODO
		
	}
	
	private class buttonClickListener implements OnClickListener {

	  @Override
	  public void onClick(View v) {
	    Button e = (Button) v;
	    char[] c = e.getText().toString().toCharArray();
	    switch (c[0]) {
	      case 'N':
	        e.setText("S");
	        break;
	      case 'S':
	        e.setText("N");
                break;
              case 'E':
                e.setText("W");
                break;
              case 'W':
                e.setText("E");
                break;
	    }
	  }
	  
	}
	
	private class textChangedListener implements TextWatcher {

	  private int editTextId;
	  
	  public textChangedListener(int id) {
            editTextId = id;
          }
	  
	  @Override
	  public void afterTextChanged(Editable s) {
	    int maxLength = 2;
            if (editTextId == 3 || editTextId == 4 || editTextId == 6)
              maxLength = 3;
            Log.d("cgeo", "Max Length " + maxLength);
            if (s.length() == maxLength) {
              switch (editTextId) {
                case 1:
                  eLatMin.requestFocus();
                  break;
                case 2:
                  eLatSec.requestFocus();
                  break;
                case 3:
                  eLonDeg.requestFocus();
                  break;
                case 4:
                  eLonMin.requestFocus();
                  break;
                case 5:
                  eLonSec.requestFocus();
                  break;
                case 6:
                  ((EditText) findViewById(R.id.name)).requestFocus();
                  break;
              }
            }
            
	  }

	  @Override
	  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	  @Override
	  public void onTextChanged(CharSequence s, int start, int before, int count) {}

	}


	@Override
	public void onResume() {
		super.onResume();
		
		settings.load();

		if (geo == null) {
			geo = app.startGeo(activity, geoUpdate, base, settings, warning, 0, 0);
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
			if (geo == null) {
				return;
			}

			try {

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
	
	private void updateLatLonFields(Double latitude, Double longitude) {
	      // Format: N 00° 00.000
	      //         012345678901
	      char[] lat = base.formatCoordinate(latitude, "lat", true).toCharArray();
	      bLat.setText(lat, 0, 1);
	      eLatDeg.setText(lat, 2, 2);
	      eLatMin.setText(lat, 6, 2);
	      eLatSec.setText(lat, 9, 3);
	      // Format: E 008° 00.000
	      //         0123456789012
	      char[] lon = base.formatCoordinate(longitude, "lon", true).toCharArray();
	      bLon.setText(lon, 0, 1);
	      eLonDeg.setText(lon, 2, 3);
	      eLonMin.setText(lon, 7, 2);
	      eLonSec.setText(lon, 10, 3);
	}
	
	private String getLatStr() {
	  return bLat.getText()+" "+eLatDeg.getText()+"° "+eLatMin.getText()+","+eLatSec.getText();
	}
	
	private String getLonStr() {
	  return bLon.getText()+" "+eLonDeg.getText()+"° "+eLonMin.getText()+","+eLonSec.getText();
	}
		    
	private class currentListener implements View.OnClickListener {

		public void onClick(View arg0) {
			if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
				warning.showToast(res.getString(R.string.err_point_unknown_position));
				return;
			}

			updateLatLonFields(geo.latitudeNow, geo.longitudeNow);
		}
	}

	private class coordsListener implements View.OnClickListener {

		public void onClick(View arg0) {
			ArrayList<Double> coords = new ArrayList<Double>();
			Double latitude = null;
			Double longitude = null;

			final String bearingText = ((EditText) findViewById(R.id.bearing)).getText().toString();
			final String distanceText = ((EditText) findViewById(R.id.distance)).getText().toString();
			final String latText = getLatStr();
			final String lonText = getLonStr();

			if ((bearingText == null || bearingText.length() == 0) && (distanceText == null || distanceText.length() == 0)
							&& (latText == null || latText.length() == 0) && (lonText == null || lonText.length() == 0)) {
				warning.helpDialog(res.getString(R.string.err_point_no_position_given_title), res.getString(R.string.err_point_no_position_given));
				return;
			}

			if (latText != null && latText.length() > 0 && lonText != null && lonText.length() > 0) {
				// latitude & longitude
				HashMap<String, Object> latParsed = base.parseCoordinate(latText, "lat");
				HashMap<String, Object> lonParsed = base.parseCoordinate(lonText, "lat");

				if (latParsed == null || latParsed.get("coordinate") == null || latParsed.get("string") == null) {
					warning.showToast(res.getString(R.string.err_parse_lat));
					return;
				}

				if (lonParsed == null || lonParsed.get("coordinate") == null || lonParsed.get("string") == null) {
					warning.showToast(res.getString(R.string.err_parse_lon));
					return;
				}

				latitude = (Double) latParsed.get("coordinate");
				longitude = (Double) lonParsed.get("coordinate");
			} else {
				if (geo == null || geo.latitudeNow == null || geo.longitudeNow == null) {
					warning.showToast(res.getString(R.string.err_point_curr_position_unavailable));
					return;
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
							distance = (new Double(distanceText)) * 1.609344; // considering it miles
						} else {
							distance = (new Double(distanceText)) * 0.001; // considering it meters
						}
					} catch (Exception e) {
						// probably not a number
					}
				}

				if (distance == null) {
					warning.showToast(res.getString(R.string.err_parse_dist));
					return;
				}

				Double latParsed = null;
				Double lonParsed = null;

				HashMap<String, Double> coordsDst = base.getRadialDistance(latitude, longitude, bearing, distance);

				latParsed = coordsDst.get("latitude");
				lonParsed = coordsDst.get("longitude");

				if (latParsed == null || lonParsed == null) {
					warning.showToast(res.getString(R.string.err_point_location_error));
					return;
				}

				coords.add(0, (Double) latParsed);
				coords.add(1, (Double) lonParsed);
			} else if (latitude != null && longitude != null) {
				coords.add(0, latitude);
				coords.add(1, longitude);
			} else {
				warning.showToast(res.getString(R.string.err_point_location_error));
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
			waypoint.latitudeString = base.formatCoordinate(coords.get(0), "lat", true);
			waypoint.longitudeString = base.formatCoordinate(coords.get(1), "lon", true);
			waypoint.note = note;

			if (app.saveOwnWaypoint(id, geocode, waypoint) == true) {
				app.removeCacheFromCache(geocode);

				finish();
				return;
			} else {
				warning.showToast(res.getString(R.string.err_waypoint_add_failed));
			}
		}
	}

	public void goHome(View view) {
		base.goHome(activity);
	}

	public void goManual(View view) {
		try {
			if (id >= 0) {
				AppManualReaderClient.openManual(
					"c-geo",
					"c:geo-waypoint-edit",
					activity,
					"http://cgeo.carnero.cc/manual/"
				);
			} else {
				AppManualReaderClient.openManual(
					"c-geo",
					"c:geo-waypoint-new",
					activity,
					"http://cgeo.carnero.cc/manual/"
				);
			}
		} catch (Exception e) {
			// nothing
		}
	}
}