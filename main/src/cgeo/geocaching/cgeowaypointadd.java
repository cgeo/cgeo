package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.WaypointType;
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
    private UpdateLocationCallback geoUpdate = new update();
    private ProgressDialog waitDialog = null;
    private cgWaypoint waypoint = null;
    private WaypointType type = WaypointType.OWN;
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
                    geocode = waypoint.getGeocode();
                    type = waypoint.getWaypointType();
                    prefix = waypoint.getPrefix();
                    lookup = waypoint.getLookup();

                    app.setAction(geocode);

                    if (waypoint.getCoords() != null) {
                        ((Button) findViewById(R.id.buttonLatitude)).setText(waypoint.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE));
                        ((Button) findViewById(R.id.buttonLongitude)).setText(waypoint.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE));
                    }
                    ((EditText) findViewById(R.id.name)).setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getName())).toString());
                    ((EditText) findViewById(R.id.note)).setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getNote())).toString());

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
            geo = app.startGeo(geoUpdate);
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

        List<String> wayPointNames = new ArrayList<String>(WaypointType.ALL_TYPES_EXCEPT_OWN.values());
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, wayPointNames);
        textView.setAdapter(adapter);

        if (id > 0) {
            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
            waitDialog.setCancelable(true);

            (new loadWaypoint()).start();
        }

        disableSuggestions((EditText) findViewById(R.id.distance));
    }

    @Override
    public void onResume() {
        super.onResume();


        if (geo == null) {
            geo = app.startGeo(geoUpdate);
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

    private class update implements UpdateLocationCallback {

        @Override
        public void updateLocation(cgGeo geo) {
            Log.d(Settings.tag, "cgeowaypointadd.updateLocation called");
            if (geo == null || geo.coordsNow == null) {
                return;
            }

            try {
                Button bLat = (Button) findViewById(R.id.buttonLatitude);
                Button bLon = (Button) findViewById(R.id.buttonLongitude);
                bLat.setHint(geo.coordsNow.format(GeopointFormatter.Format.LAT_DECMINUTE_RAW));
                bLon.setHint(geo.coordsNow.format(GeopointFormatter.Format.LON_DECMINUTE_RAW));
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
            if (waypoint != null && waypoint.getCoords() != null) {
                gp = waypoint.getCoords();
            }
            cgCache cache = app.getCacheByGeocode(geocode);
            cgeocoords coordsDialog = new cgeocoords(cgeowaypointadd.this, cache, gp, geo);
            coordsDialog.setCancelable(true);
            coordsDialog.setOnCoordinateUpdate(new cgeocoords.CoordinateUpdate() {
                @Override
                public void update(final Geopoint gp) {
                    ((Button) findViewById(R.id.buttonLatitude)).setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    ((Button) findViewById(R.id.buttonLongitude)).setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                    if (waypoint != null) {
                        waypoint.setCoords(gp);
                    }
                }
            });
            coordsDialog.show();
        }
    }

    private class coordsListener implements View.OnClickListener {

        public void onClick(View arg0) {
            final String bearingText = ((EditText) findViewById(R.id.bearing)).getText().toString();
            final String distanceText = ((EditText) findViewById(R.id.distance)).getText().toString();
            final String latText = ((Button) findViewById(R.id.buttonLatitude)).getText().toString();
            final String lonText = ((Button) findViewById(R.id.buttonLongitude)).getText().toString();

            if (StringUtils.isBlank(bearingText) && StringUtils.isBlank(distanceText)
                    && StringUtils.isBlank(latText) && StringUtils.isBlank(lonText)) {
                helpDialog(res.getString(R.string.err_point_no_position_given_title), res.getString(R.string.err_point_no_position_given));
                return;
            }

            double latitude;
            double longitude;

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

            Geopoint coords = null;
            if (StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)) {
                // bearing & distance
                double bearing = 0;
                try {
                    bearing = Double.parseDouble(bearingText);
                } catch (NumberFormatException e) {
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

                coords = new Geopoint(latitude, longitude).project(bearing, distance);
            } else {
                coords = new Geopoint(latitude, longitude);
            }

            String name = ((EditText) findViewById(R.id.name)).getText().toString().trim();
            // if no name is given, just give the waypoint its number as name
            if (name.length() == 0) {
                name = res.getString(R.string.waypoint) + " " + String.valueOf(wpCount + 1);
            }
            // If own waypoint, check the name for type
            if (type == WaypointType.OWN) {
                if (StringUtils.startsWith(name, WaypointType.OWN_FINAL.getL10n())) {
                    type = WaypointType.OWN_FINAL;
                } else if (StringUtils.startsWith(name, WaypointType.OWN_STAGE.getL10n())) {
                    type = WaypointType.OWN_STAGE;
                } else if (StringUtils.startsWith(name, WaypointType.OWN_PARKING.getL10n())) {
                    type = WaypointType.OWN_PARKING;
                }
            }
            final String note = ((EditText) findViewById(R.id.note)).getText().toString().trim();

            final cgWaypoint waypoint = new cgWaypoint(name, type);
            waypoint.setGeocode(geocode);
            waypoint.setPrefix(prefix);
            waypoint.setLookup(lookup);
            waypoint.setCoords(coords);
            waypoint.setNote(note);
            waypoint.setId(id);

            if (app.saveOwnWaypoint(id, geocode, waypoint)) {
                StaticMapsProvider.removeWpStaticMaps(id, geocode);
                if (Settings.isStoreOfflineWpMaps()) {
                    StaticMapsProvider.storeWaypointStaticMap(app.getCacheByGeocode(geocode), cgeowaypointadd.this, waypoint);
                }
                finish();
                return;
            } else {
                showToast(res.getString(R.string.err_waypoint_add_failed));
            }
        }
    }

    @Override
    public void goManual(View view) {
        if (id >= 0) {
            ActivityMixin.goManual(this, "c:geo-waypoint-edit");
        } else {
            ActivityMixin.goManual(this, "c:geo-waypoint-new");
        }
    }
}
