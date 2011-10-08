package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.geopoint.DistanceParser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.geopoint.GeopointParser;

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

import java.util.ArrayList;
import java.util.List;

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
                    prefix = waypoint.getPrefix();
                    lookup = waypoint.lookup;

                    app.setAction(geocode);

                    if (waypoint.coords != null) {
                        ((Button) findViewById(R.id.buttonLatitude)).setText(cgBase.formatLatitude(waypoint.coords.getLatitude(), true));
                        ((Button) findViewById(R.id.buttonLongitude)).setText(cgBase.formatLongitude(waypoint.coords.getLongitude(), true));
                    }
                    ((EditText) findViewById(R.id.name)).setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.name)).toString());
                    ((EditText) findViewById(R.id.note)).setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.note)).toString());

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
                Log.e(Settings.tag, "cgeowaypointadd.loadWaypointHandler: " + e.toString());
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
            geo = app.startGeo(this, geoUpdate, base, 0, 0);
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


        if (geo == null) {
            geo = app.startGeo(this, geoUpdate, base, 0, 0);
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
            if (geo == null || geo.coordsNow == null) {
                return;
            }

            try {
                Button bLat = (Button) findViewById(R.id.buttonLatitude);
                Button bLon = (Button) findViewById(R.id.buttonLongitude);
                bLat.setHint(cgBase.formatLatitude(geo.coordsNow.getLatitude(), false));
                bLon.setHint(cgBase.formatLongitude(geo.coordsNow.getLongitude(), false));
            } catch (Exception e) {
                Log.w(Settings.tag, "Failed to update location.");
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
                Log.e(Settings.tag, "cgeowaypoint.loadWaypoint.run: " + e.toString());
            }
        }
    }

    private class coordDialogListener implements View.OnClickListener {

        public void onClick(View arg0) {
            Geopoint gp = null;
            if (waypoint != null && waypoint.coords != null)
                gp = waypoint.coords;
            cgeocoords coordsDialog = new cgeocoords(cgeowaypointadd.this, gp, geo);
            coordsDialog.setCancelable(true);
            coordsDialog.setOnCoordinateUpdate(new cgeocoords.CoordinateUpdate() {
                @Override
                public void update(final Geopoint gp) {
                    ((Button) findViewById(R.id.buttonLatitude)).setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    ((Button) findViewById(R.id.buttonLongitude)).setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                    if (waypoint != null) {
                        waypoint.coords = gp;
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

            if (StringUtils.isBlank(bearingText) && StringUtils.isBlank(distanceText)
                    && StringUtils.isBlank(latText) && StringUtils.isBlank(lonText)) {
                helpDialog(res.getString(R.string.err_point_no_position_given_title), res.getString(R.string.err_point_no_position_given));
                return;
            }

            if (StringUtils.isNotBlank(latText) && StringUtils.isNotBlank(lonText)) {
                try {
                    latitude = GeopointParser.parseLatitude(latText);
                    longitude = GeopointParser.parseLongitude(lonText);
                } catch (GeopointParser.ParseException e) {
                    showToast(res.getString(e.resource));
                    return;
                }
            } else {
                if (geo == null || geo.coordsNow == null) {
                    showToast(res.getString(R.string.err_point_curr_position_unavailable));
                    return;
                }

                latitude = geo.coordsNow.getLatitude();
                longitude = geo.coordsNow.getLongitude();
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

                double distance;
                try {
                    distance = DistanceParser.parseDistance(distanceText, Settings.isUseMetricUnits());
                } catch (NumberFormatException e) {
                    showToast(res.getString(R.string.err_parse_dist));
                    return;
                }

                Double latParsed = null;
                Double lonParsed = null;

                final Geopoint coordsDst = new Geopoint(latitude, longitude).project(bearing, distance);

                latParsed = coordsDst.getLatitude();
                lonParsed = coordsDst.getLongitude();

                if (latParsed == null || lonParsed == null) {
                    showToast(res.getString(R.string.err_point_location_error));
                    return;
                }

                coords.add(0, latParsed);
                coords.add(1, lonParsed);
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
            waypoint.setPrefix(prefix);
            waypoint.lookup = lookup;
            waypoint.name = name;
            waypoint.coords = new Geopoint(coords.get(0), coords.get(1));
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
