package cgeo.geocaching;

import butterknife.InjectView;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.DistanceParser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class EditWaypointActivity extends AbstractActivity {
    @InjectView(R.id.buttonLatitude) protected Button buttonLat;
    @InjectView(R.id.buttonLongitude) protected Button buttonLon;
    @InjectView(R.id.add_waypoint) protected Button addWaypoint;
    @InjectView(R.id.note) protected EditText note;
    @InjectView(R.id.wpt_visited_checkbox) protected CheckBox visitedCheckBox;
    @InjectView(R.id.name) protected AutoCompleteTextView waypointName;
    @InjectView(R.id.type) protected Spinner waypointTypeSelector;
    @InjectView(R.id.distance) protected EditText distanceView;
    @InjectView(R.id.modify_cache_coordinates_group) protected RadioGroup coordinatesGroup;
    @InjectView(R.id.modify_cache_coordinates_local_and_remote) protected RadioButton modifyBoth;
    @InjectView(R.id.distanceUnit) protected Spinner distanceUnitSelector;
    @InjectView(R.id.bearing) protected EditText bearing;
    @InjectView(R.id.modify_cache_coordinates_local) protected RadioButton modifyLocal;

    private String geocode = null;
    private int id = -1;
    private ProgressDialog waitDialog = null;
    private Waypoint waypoint = null;
    private Geopoint gpTemp = null;
    private WaypointType type = WaypointType.OWN;
    private String prefix = "OWN";
    private String lookup = "---";
    private boolean own = true;
    private boolean visited = false;
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
                    visited = waypoint.isVisited();

                    if (waypoint.getCoords() != null) {
                        buttonLat.setText(waypoint.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE));
                        buttonLon.setText(waypoint.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE));
                    }
                    waypointName.setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getName())).toString());
                    if (BaseUtils.containsHtml(waypoint.getNote())) {
                        note.setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getNote())).toString());
                    }
                    else {
                        note.setText(StringUtils.trimToEmpty(waypoint.getNote()));
                    }
                    Geocache cache = cgData.loadCache(geocode, LoadFlags.LOAD_CACHE_ONLY);
                    setCoordsModificationVisibility(ConnectorFactory.getConnector(geocode), cache);
                }

                if (own) {
                    initializeWaypointTypeSelector();
                }
                visitedCheckBox.setChecked(visited);

                initializeDistanceUnitSelector();
            } catch (Exception e) {
                Log.e("EditWaypointActivity.loadWaypointHandler", e);
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
        super.onCreate(savedInstanceState, R.layout.editwaypoint_activity);

        // get parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            wpCount = extras.getInt(Intents.EXTRA_COUNT, 0);
            id = extras.getInt(Intents.EXTRA_WAYPOINT_ID);
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

        buttonLat.setOnClickListener(new CoordDialogListener());
        buttonLon.setOnClickListener(new CoordDialogListener());

        addWaypoint.setOnClickListener(new CoordsListener());

        List<String> wayPointNames = new ArrayList<String>();
        for (WaypointType wpt : WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL) {
            wayPointNames.add(wpt.getL10n());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, wayPointNames);
        waypointName.setAdapter(adapter);

        if (id > 0) {
            waypointTypeSelector.setVisibility(View.GONE);

            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
            waitDialog.setCancelable(true);

            (new LoadWaypointThread()).start();
        } else {
            initializeWaypointTypeSelector();
        }

        if (geocode != null) {
            Geocache cache = cgData.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            IConnector con = ConnectorFactory.getConnector(geocode);
            setCoordsModificationVisibility(con, cache);
        }
        visitedCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                visited = isChecked;
            }
        });

        initializeDistanceUnitSelector();

        disableSuggestions(distanceView);
    }

    private void setCoordsModificationVisibility(IConnector con, Geocache cache) {
        if (cache != null && (cache.getType() == CacheType.MYSTERY || cache.getType() == CacheType.MULTI)) {
            coordinatesGroup.setVisibility(View.VISIBLE);
            modifyBoth.setVisibility(con.supportsOwnCoordinates() ? View.VISIBLE : View.GONE);
        } else {
            coordinatesGroup.setVisibility(View.GONE);
            modifyBoth.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        geoDirHandler.startGeo();

        if (id > 0) {
            if (waitDialog == null) {
                waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
                waitDialog.setCancelable(true);

                (new LoadWaypointThread()).start();
            }
        }
    }

    @Override
    public void onPause() {
        geoDirHandler.stopGeo();
        super.onPause();
    }

    private void initializeWaypointTypeSelector() {
        wpTypes = new ArrayList<WaypointType>(WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL);
        ArrayAdapter<WaypointType> wpAdapter = new ArrayAdapter<WaypointType>(this, android.R.layout.simple_spinner_item, wpTypes.toArray(new WaypointType[wpTypes.size()]));
        wpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        waypointTypeSelector.setAdapter(wpAdapter);

        int typeIndex = wpTypes.indexOf(type);
        if (typeIndex < 0) {
            typeIndex = wpTypes.indexOf(WaypointType.WAYPOINT);
        }

        waypointTypeSelector.setSelection(typeIndex);
        waypointTypeSelector.setOnItemSelectedListener(new ChangeWaypointType(this));

        waypointTypeSelector.setVisibility(View.VISIBLE);
    }

    private void initializeDistanceUnitSelector() {
        if (StringUtils.isBlank(distanceUnit)) {
            if (Settings.isUseMetricUnits()) {
                distanceUnitSelector.setSelection(0); // m
                distanceUnit = res.getStringArray(R.array.distance_units)[0];
            } else {
                distanceUnitSelector.setSelection(2); // ft
                distanceUnit = res.getStringArray(R.array.distance_units)[2];
            }
        }

        distanceUnitSelector.setOnItemSelectedListener(new ChangeDistanceUnit(this));
    }

    final private GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoData(final IGeoData geo) {
            if (geo.getCoords() == null) {
                return;
            }

            try {
                buttonLat.setHint(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE_RAW));
                buttonLon.setHint(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE_RAW));
            } catch (final Exception e) {
                Log.e("failed to update location", e);
            }
        }
    };

    private class LoadWaypointThread extends Thread {

        @Override
        public void run() {
            try {
                waypoint = cgData.loadWaypoint(id);

                loadWaypointHandler.sendMessage(Message.obtain());
            } catch (Exception e) {
                Log.e("EditWaypointActivity.loadWaypoint.run", e);
            }
        }
    }

    private class CoordDialogListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            Geopoint gp = null;
            if (waypoint != null && waypoint.getCoords() != null) {
                gp = waypoint.getCoords();
            } else if (gpTemp != null) {
                gp = gpTemp;
            }
            Geocache cache = cgData.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
            CoordinatesInputDialog coordsDialog = new CoordinatesInputDialog(EditWaypointActivity.this, cache, gp, app.currentGeo());
            coordsDialog.setCancelable(true);
            coordsDialog.setOnCoordinateUpdate(new CoordinatesInputDialog.CoordinateUpdate() {
                @Override
                public void update(final Geopoint gp) {
                    buttonLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    buttonLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
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

    private static class ChangeWaypointType implements OnItemSelectedListener {

        private ChangeWaypointType(EditWaypointActivity wpView) {
            this.wpView = wpView;
        }

        private EditWaypointActivity wpView;

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

    private static class ChangeDistanceUnit implements OnItemSelectedListener {

        private ChangeDistanceUnit(EditWaypointActivity unitView) {
            this.unitView = unitView;
        }

        private EditWaypointActivity unitView;

        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                long arg3) {
            unitView.distanceUnit = (String) arg0.getItemAtPosition(arg2);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    public static final int SUCCESS = 0;
    public static final int UPLOAD_START = 1;
    public static final int UPLOAD_ERROR = 2;
    public static final int UPLOAD_NOT_POSSIBLE = 3;
    public static final int UPLOAD_SUCCESS = 4;
    public static final int SAVE_ERROR = 5;

    private class CoordsListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            final String bearingText = bearing.getText().toString();
            // combine distance from EditText and distanceUnit saved from Spinner
            final String distanceText = distanceView.getText().toString() + distanceUnit;
            final String latText = buttonLat.getText().toString();
            final String lonText = buttonLon.getText().toString();

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

            // if no name is given, just give the waypoint its number as name
            final String givenName = waypointName.getText().toString().trim();
            final String name = StringUtils.isNotEmpty(givenName) ? givenName : res.getString(R.string.waypoint) + " " + (wpCount + 1);
            final String noteText = note.getText().toString().trim();
            final Geopoint coordsToSave = coords;
            final ProgressDialog progress = ProgressDialog.show(EditWaypointActivity.this, getString(R.string.cache), getString(R.string.waypoint_being_saved), true);
            final Handler finishHandler = new Handler() {

                @Override
                public void handleMessage(Message msg) {
                    // TODO: The order of showToast, progress.dismiss and finish is different in these cases. Why?
                    switch (msg.what) {
                        case UPLOAD_SUCCESS:
                            showToast(getString(R.string.waypoint_coordinates_has_been_modified_on_website, coordsToSave));
                            progress.dismiss();
                            finish();
                            break;
                        case SUCCESS:
                            progress.dismiss();
                            finish();
                            break;
                        case UPLOAD_START:
                            progress.setMessage(getString(R.string.waypoint_coordinates_uploading_to_website, coordsToSave));
                            break;
                        case UPLOAD_ERROR:
                            progress.dismiss();
                            finish();
                            showToast(getString(R.string.waypoint_coordinates_upload_error));
                            break;
                        case UPLOAD_NOT_POSSIBLE:
                            progress.dismiss();
                            finish();
                            showToast(getString(R.string.waypoint_coordinates_couldnt_be_modified_on_website));
                            break;
                        case SAVE_ERROR:
                            progress.dismiss();
                            finish(); //TODO: should we close activity here ?
                            showToast(res.getString(R.string.err_waypoint_add_failed));
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            };

            class SaveWptTask extends AsyncTask<Void, Void, Void> {

                @Override
                protected Void doInBackground(Void... params) {
                    final Waypoint waypoint = new Waypoint(name, type, own);
                    waypoint.setGeocode(geocode);
                    waypoint.setPrefix(prefix);
                    waypoint.setLookup(lookup);
                    waypoint.setCoords(coordsToSave);
                    waypoint.setNote(noteText);
                    waypoint.setVisited(visited);
                    waypoint.setId(id);

                    Geocache cache = cgData.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                    if (cache == null) {
                        finishHandler.sendEmptyMessage(SAVE_ERROR);
                        return null;
                    }
                    Waypoint oldWaypoint = cache.getWaypointById(id);
                    if (cache.addOrChangeWaypoint(waypoint, true)) {
                        cgData.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
                        if (!StaticMapsProvider.hasAllStaticMapsForWaypoint(geocode, waypoint)) {
                            StaticMapsProvider.removeWpStaticMaps(oldWaypoint, geocode);
                            if (Settings.isStoreOfflineWpMaps()) {
                                StaticMapsProvider.storeWaypointStaticMap(cache, waypoint, false);
                            }
                        }
                        if (modifyLocal.isChecked() || modifyBoth.isChecked()) {
                            if (!cache.hasUserModifiedCoords()) {
                                final Waypoint origWaypoint = new Waypoint(cgeoapplication.getInstance().getString(R.string.cache_coordinates_original), WaypointType.ORIGINAL, false);
                                origWaypoint.setCoords(cache.getCoords());
                                cache.addOrChangeWaypoint(origWaypoint, false);
                                cache.setUserModifiedCoords(true);
                            }
                            cache.setCoords(waypoint.getCoords());
                            cgData.saveChangedCache(cache);
                        }
                        if (modifyBoth.isChecked() && waypoint.getCoords() != null) {
                            finishHandler.sendEmptyMessage(UPLOAD_START);

                            if (cache.supportsOwnCoordinates()) {
                                boolean result = uploadModifiedCoords(cache, waypoint.getCoords());
                                finishHandler.sendEmptyMessage(result ? SUCCESS : UPLOAD_ERROR);
                            } else {
                                showToast(getString(R.string.waypoint_coordinates_couldnt_be_modified_on_website));
                                finishHandler.sendEmptyMessage(UPLOAD_NOT_POSSIBLE);
                            }
                        } else {
                            finishHandler.sendEmptyMessage(SUCCESS);
                        }
                    } else {
                        finishHandler.sendEmptyMessage(SAVE_ERROR);
                    }
                    return null;
                }
            }
            new SaveWptTask().execute();
        }
    }

    private static boolean uploadModifiedCoords(final Geocache cache, final Geopoint waypointUploaded) {
        final IConnector con = ConnectorFactory.getConnector(cache);
        return con.supportsOwnCoordinates() && con.uploadModifiedCoordinates(cache, waypointUploaded);
    }

    public static void startActivityEditWaypoint(final Context context, final int waypointId) {
        context.startActivity(new Intent(context, EditWaypointActivity.class)
                .putExtra(Intents.EXTRA_WAYPOINT_ID, waypointId));
    }

    public static void startActivityAddWaypoint(final Context context, final Geocache cache) {
        context.startActivity(new Intent(context, EditWaypointActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode())
                .putExtra(Intents.EXTRA_COUNT, cache.getWaypoints().size()));
    }
}
