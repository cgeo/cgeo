package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.DistanceParser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.IObserver;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class cgeowaypointadd extends AbstractActivity implements IObserver<IGeoData> {

    private String geocode = null;
    private int id = -1;
    private ProgressDialog waitDialog = null;
    private cgWaypoint waypoint = null;
    private Geopoint gpTemp = null;
    private WaypointType type = WaypointType.OWN;
    private String prefix = "OWN";
    private String lookup = "---";
    private boolean own = true;
    ArrayList<WaypointType> wpTypes = null;
    String distanceUnit = "";

    /**
     * number of waypoints that the corresponding cache has until now
     */
    private int wpCount = 0;
    private Handler loadWaypointHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (waypoint == null) {
                    id = -1;
                } else {
                    geocode = waypoint.getGeocode();
                    type = waypoint.getWaypointType();
                    prefix = waypoint.getPrefix();
                    lookup = waypoint.getLookup();
                    own = waypoint.isUserDefined();

                    app.setAction(geocode);

                    if (waypoint.getCoords() != null) {
                        ((Button) findViewById(R.id.buttonLatitude)).setText(waypoint.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE));
                        ((Button) findViewById(R.id.buttonLongitude)).setText(waypoint.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE));
                    }
                    ((EditText) findViewById(R.id.name)).setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getName())).toString());
                    if (BaseUtils.containsHtml(waypoint.getNote())) {
                        ((EditText) findViewById(R.id.note)).setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getNote())).toString());
                    }
                    else {
                        ((EditText) findViewById(R.id.note)).setText(StringUtils.trimToEmpty(waypoint.getNote()));
                    }
                }

                if (own) {
                    initializeWaypointTypeSelector();
                }

                initializeDistanceUnitSelector();
            } catch (Exception e) {
                Log.e("cgeowaypointadd.loadWaypointHandler: " + e.toString());
            } finally {
                if (waitDialog != null) {
                    waitDialog.dismiss();
                    waitDialog = null;
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();
        setContentView(R.layout.waypoint_new);
        setTitle("waypoint");

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

        List<String> wayPointNames = new ArrayList<String>();
        for (WaypointType wpt : WaypointType.ALL_TYPES_EXCEPT_OWN) {
            wayPointNames.add(wpt.getL10n());
        }
        AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.name);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, wayPointNames);
        textView.setAdapter(adapter);

        if (id > 0) {
            Spinner waypointTypeSelector = (Spinner) findViewById(R.id.type);
            waypointTypeSelector.setVisibility(View.GONE);

            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
            waitDialog.setCancelable(true);

            (new loadWaypoint()).start();
        } else {
            initializeWaypointTypeSelector();
        }

        initializeDistanceUnitSelector();

        disableSuggestions((EditText) findViewById(R.id.distance));
    }

    @Override
    public void onResume() {
        super.onResume();

        app.addGeoObserver(this);

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
        super.onDestroy();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        app.deleteGeoObserver(this);
        super.onPause();
    }

    private void initializeWaypointTypeSelector() {

        Spinner waypointTypeSelector = (Spinner) findViewById(R.id.type);

        wpTypes = new ArrayList<WaypointType>(WaypointType.ALL_TYPES_EXCEPT_OWN);
        ArrayAdapter<WaypointType> wpAdapter = new ArrayAdapter<WaypointType>(this, android.R.layout.simple_spinner_item, wpTypes.toArray(new WaypointType[wpTypes.size()]));
        wpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        waypointTypeSelector.setAdapter(wpAdapter);

        int typeIndex = wpTypes.indexOf(type);
        if (typeIndex < 0) {
            typeIndex = wpTypes.indexOf(WaypointType.WAYPOINT);
        }

        waypointTypeSelector.setSelection(typeIndex);
        waypointTypeSelector.setOnItemSelectedListener(new changeWaypointType(this));

        waypointTypeSelector.setVisibility(View.VISIBLE);
    }

    private void initializeDistanceUnitSelector() {

        Spinner distanceUnitSelector = (Spinner) findViewById(R.id.distanceUnit);

        if (StringUtils.isBlank(distanceUnit)) {
            if (Settings.isUseMetricUnits()) {
                distanceUnitSelector.setSelection(0); // m
                distanceUnit = res.getStringArray(R.array.distance_units)[0];
            } else {
                distanceUnitSelector.setSelection(2); // ft
                distanceUnit = res.getStringArray(R.array.distance_units)[2];
            }
        }

        distanceUnitSelector.setOnItemSelectedListener(new changeDistanceUnit(this));
    }

    @Override
    public void update(final IGeoData geo) {
        Log.d("cgeowaypointadd.updateLocation called");
        if (geo.getCoords() == null) {
            return;
        }

        try {
            Button bLat = (Button) findViewById(R.id.buttonLatitude);
            Button bLon = (Button) findViewById(R.id.buttonLongitude);
            bLat.setHint(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE_RAW));
            bLon.setHint(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE_RAW));
        } catch (Exception e) {
            Log.w("Failed to update location.");
        }
    }

    private class loadWaypoint extends Thread {

        @Override
        public void run() {
            try {
                waypoint = app.loadWaypoint(id);

                loadWaypointHandler.sendMessage(Message.obtain());
            } catch (Exception e) {
                Log.e("cgeowaypoint.loadWaypoint.run: " + e.toString());
            }
        }
    }

    private class coordDialogListener implements View.OnClickListener {

        public void onClick(View arg0) {
            Geopoint gp = null;
            if (waypoint != null && waypoint.getCoords() != null) {
                gp = waypoint.getCoords();
            } else if (gpTemp != null) {
                gp = gpTemp;
            }
            cgCache cache = app.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
            cgeocoords coordsDialog = new cgeocoords(cgeowaypointadd.this, cache, gp, app.currentGeo());
            coordsDialog.setCancelable(true);
            coordsDialog.setOnCoordinateUpdate(new cgeocoords.CoordinateUpdate() {
                @Override
                public void update(final Geopoint gp) {
                    ((Button) findViewById(R.id.buttonLatitude)).setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    ((Button) findViewById(R.id.buttonLongitude)).setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                    if (waypoint != null) {
                        waypoint.setCoords(gp);
                    } else {
                        gpTemp = gp;
                    }
                }
            });
            coordsDialog.show();
        }
    }

    private class changeWaypointType implements OnItemSelectedListener {

        private changeWaypointType(cgeowaypointadd wpView) {
            this.wpView = wpView;
        }

        private cgeowaypointadd wpView;

        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                long arg3) {
            if (null != wpView.wpTypes) {
                wpView.type = wpView.wpTypes.get(arg2);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            if (null != wpView.wpTypes) {
                arg0.setSelection(wpView.wpTypes.indexOf(wpView.type));
            }
        }
    }

    private class changeDistanceUnit implements OnItemSelectedListener {

        private changeDistanceUnit(cgeowaypointadd unitView) {
            this.unitView = unitView;
        }

        private cgeowaypointadd unitView;

        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                long arg3) {
            unitView.distanceUnit = (String) arg0.getItemAtPosition(arg2);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    private class coordsListener implements View.OnClickListener {

        public void onClick(View arg0) {
            final String bearingText = ((EditText) findViewById(R.id.bearing)).getText().toString();
            // combine distance from EditText and distanceUnit saved from Spinner
            final String distanceText = ((EditText) findViewById(R.id.distance)).getText().toString() + distanceUnit;
            final String latText = ((Button) findViewById(R.id.buttonLatitude)).getText().toString();
            final String lonText = ((Button) findViewById(R.id.buttonLongitude)).getText().toString();

            if (StringUtils.isBlank(bearingText) && StringUtils.isBlank(distanceText)
                    && StringUtils.isBlank(latText) && StringUtils.isBlank(lonText)) {
                helpDialog(res.getString(R.string.err_point_no_position_given_title), res.getString(R.string.err_point_no_position_given));
                return;
            }

            Geopoint coords;

            if (StringUtils.isNotBlank(latText) && StringUtils.isNotBlank(lonText)) {
                try {
                    coords = new Geopoint(latText, lonText);
                } catch (Geopoint.ParseException e) {
                    showToast(res.getString(e.resource));
                    return;
                }
            } else {
                final IGeoData geo = app.currentGeo();
                if (geo.getCoords() == null) {
                    showToast(res.getString(R.string.err_point_curr_position_unavailable));
                    return;
                }
                coords = geo.getCoords();
            }

            if (StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)) {
                // bearing & distance
                double bearing;
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

                coords = coords.project(bearing, distance);
            }

            String name = ((EditText) findViewById(R.id.name)).getText().toString().trim();
            // if no name is given, just give the waypoint its number as name
            if (name.length() == 0) {
                name = res.getString(R.string.waypoint) + " " + String.valueOf(wpCount + 1);
            }
            final String note = ((EditText) findViewById(R.id.note)).getText().toString().trim();

            final cgWaypoint waypoint = new cgWaypoint(name, type, own);
            waypoint.setGeocode(geocode);
            waypoint.setPrefix(prefix);
            waypoint.setLookup(lookup);
            waypoint.setCoords(coords);
            waypoint.setNote(note);
            waypoint.setId(id);

            cgCache cache = app.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
            if (null != cache && cache.addWaypoint(waypoint, true)) {
                app.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
                StaticMapsProvider.removeWpStaticMaps(id, geocode);
                if (Settings.isStoreOfflineWpMaps()) {
                    StaticMapsProvider.storeWaypointStaticMap(cache, cgeowaypointadd.this, waypoint, false);
                }
                finish();
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

    public static void startActivityEditWaypoint(final Context context, final int waypointId) {
        final Intent editIntent = new Intent(context, cgeowaypointadd.class);
        editIntent.putExtra("waypoint", waypointId);
        context.startActivity(editIntent);
    }

    public static void startActivityAddWaypoint(final Context context, final cgCache cache) {
        final Intent addWptIntent = new Intent(context, cgeowaypointadd.class);
        addWptIntent.putExtra("geocode", cache.getGeocode()).putExtra("count", cache.getWaypoints().size());
        context.startActivity(addWptIntent);
    }
}
