package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.geopoint.DistanceParser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.IGeoData;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@EActivity
public class EditWaypointActivity extends AbstractActionBarActivity implements CoordinatesInputDialog.CoordinateUpdate {
    private static final ArrayList<WaypointType> POSSIBLE_WAYPOINT_TYPES = new ArrayList<WaypointType>(WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL);

    @ViewById(R.id.buttonLatitude) protected Button buttonLat;
    @ViewById(R.id.buttonLongitude) protected Button buttonLon;
    @ViewById(R.id.add_waypoint) protected Button addWaypoint;
    @ViewById(R.id.note) protected EditText note;
    @ViewById(R.id.wpt_visited_checkbox) protected CheckBox visitedCheckBox;
    @ViewById(R.id.name) protected AutoCompleteTextView waypointName;
    @ViewById(R.id.type) protected Spinner waypointTypeSelector;
    @ViewById(R.id.distance) protected EditText distanceView;
    @ViewById(R.id.modify_cache_coordinates_group) protected RadioGroup coordinatesGroup;
    @ViewById(R.id.modify_cache_coordinates_local_and_remote) protected RadioButton modifyBoth;
    @ViewById(R.id.distanceUnit) protected Spinner distanceUnitSelector;
    @ViewById(R.id.bearing) protected EditText bearing;
    @ViewById(R.id.modify_cache_coordinates_local) protected RadioButton modifyLocal;

    @Extra(Intents.EXTRA_GEOCODE) protected String geocode = null;
    @Extra(Intents.EXTRA_WAYPOINT_ID) protected int id = -1;
    /**
     * number of waypoints that the corresponding cache has until now
     */
    @Extra(Intents.EXTRA_COUNT) protected int wpCount = 0;

    @InstanceState protected int waypointTypeSelectorPosition = -1;

    private ProgressDialog waitDialog = null;
    private Waypoint waypoint = null;
    private String prefix = "OWN";
    private String lookup = "---";
    private boolean own = true;
    ArrayList<String> distanceUnits = null;
    /**
     * {@code true} if the activity is newly created, {@code false} if it is restored from an instance state
     */
    private boolean initViews = true;
    /**
     * This is the cache that the waypoint belongs to.
     */
    private Geocache cache;

    private Handler loadWaypointHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            try {
                if (waypoint == null) {
                    Log.d("No waypoint loaded to edit. id= " + id);
                    id = -1;
                } else {
                    geocode = waypoint.getGeocode();
                    prefix = waypoint.getPrefix();
                    lookup = waypoint.getLookup();
                    own = waypoint.isUserDefined();

                    if (initViews) {
                        visitedCheckBox.setChecked(waypoint.isVisited());
                        if (waypoint.getCoords() != null) {
                            buttonLat.setText(waypoint.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE));
                            buttonLon.setText(waypoint.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE));
                        }
                        waypointName.setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getName())).toString());
                        Dialogs.moveCursorToEnd(waypointName);
                        if (TextUtils.containsHtml(waypoint.getNote())) {
                            note.setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getNote())).toString());
                        }
                        else {
                            note.setText(StringUtils.trimToEmpty(waypoint.getNote()));
                        }
                        Dialogs.moveCursorToEnd(note);
                    }
                    final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_ONLY);
                    setCoordsModificationVisibility(ConnectorFactory.getConnector(geocode), cache);
                }

                if (own) {
                    initializeWaypointTypeSelector();
                }
            } catch (RuntimeException e) {
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

        addWaypoint.setOnClickListener(new SaveWaypointListener());

        List<String> wayPointNames = new ArrayList<String>();
        for (WaypointType wpt : WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL) {
            wayPointNames.add(wpt.getL10n());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, wayPointNames);
        waypointName.setAdapter(adapter);

        if (savedInstanceState != null) {
            initViews = false;
        }

        if (geocode != null) {
            cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            setCoordsModificationVisibility(ConnectorFactory.getConnector(geocode), cache);
        }
        if (id > 0) { // existing waypoint
            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
            waitDialog.setCancelable(true);

            (new LoadWaypointThread()).start();

        } else { // new waypoint
            initializeWaypointTypeSelector();
        }

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
        super.onResume(geoDirHandler.start(GeoDirHandler.UPDATE_GEODATA));
    }

    private void initializeWaypointTypeSelector() {
        ArrayAdapter<WaypointType> wpAdapter = new ArrayAdapter<WaypointType>(this, android.R.layout.simple_spinner_item, POSSIBLE_WAYPOINT_TYPES.toArray(new WaypointType[POSSIBLE_WAYPOINT_TYPES.size()]));
        wpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        waypointTypeSelector.setAdapter(wpAdapter);

        waypointTypeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                waypointTypeSelectorPosition = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        waypointTypeSelector.setSelection(getDefaultWaypointType());
        waypointTypeSelector.setVisibility(View.VISIBLE);
    }

    private int getDefaultWaypointType() {
        // potentially restore saved instance state
        if (waypointTypeSelectorPosition >= 0) {
            return waypointTypeSelectorPosition;
        }

        // when editing, use the existing type
        if (waypoint != null) {
            return POSSIBLE_WAYPOINT_TYPES.indexOf(waypoint.getWaypointType());
        }

        // make default for new waypoint depend on cache type
        switch (cache.getType()) {
            case MYSTERY:
                return POSSIBLE_WAYPOINT_TYPES.indexOf(WaypointType.FINAL);
            case MULTI:
                return POSSIBLE_WAYPOINT_TYPES.indexOf(WaypointType.STAGE);
            default:
                return POSSIBLE_WAYPOINT_TYPES.indexOf(WaypointType.WAYPOINT);
        }
    }

    private void initializeDistanceUnitSelector() {
        distanceUnits = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.distance_units)));
        if (initViews) {
            distanceUnitSelector.setSelection(Settings.isUseImperialUnits() ? 2 : 0); //0:m, 2:ft
        }
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
                waypoint = DataStore.loadWaypoint(id);

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
            try {
                gp = new Geopoint(buttonLat.getText().toString(), buttonLon.getText().toString());
            } catch (Geopoint.ParseException e) {
                // button text is blank when creating new waypoint
            }
            Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
            CoordinatesInputDialog coordsDialog = CoordinatesInputDialog.getInstance(cache, gp, app.currentGeo());
            coordsDialog.setCancelable(true);
            coordsDialog.show(getSupportFragmentManager(),"wpeditdialog");
        }


    }

    @Override
    public void updateCoordinates(Geopoint gp) {
        buttonLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
        buttonLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
    }

    public static final int SUCCESS = 0;
    public static final int UPLOAD_START = 1;
    public static final int UPLOAD_ERROR = 2;
    public static final int UPLOAD_NOT_POSSIBLE = 3;
    public static final int UPLOAD_SUCCESS = 4;
    public static final int SAVE_ERROR = 5;

    private class SaveWaypointListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            final String bearingText = bearing.getText().toString();
            // combine distance from EditText and distanceUnit saved from Spinner
            final String distanceText = distanceView.getText().toString() + distanceUnits.get(distanceUnitSelector.getSelectedItemPosition());
            final String latText = buttonLat.getText().toString();
            final String lonText = buttonLon.getText().toString();

            if (StringUtils.isBlank(bearingText) && StringUtils.isBlank(distanceText)
                    && StringUtils.isBlank(latText) && StringUtils.isBlank(lonText)) {
                Dialogs.message(EditWaypointActivity.this, R.string.err_point_no_position_given_title, R.string.err_point_no_position_given);
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
                    Dialogs.message(EditWaypointActivity.this, R.string.err_point_bear_and_dist_title, R.string.err_point_bear_and_dist);
                    return;
                }

                double distance;
                try {
                    distance = DistanceParser.parseDistance(distanceText,
                            !Settings.isUseImperialUnits());
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
            final int selectedTypeIndex = waypointTypeSelector.getSelectedItemPosition();
            final WaypointType type = selectedTypeIndex >= 0 ? POSSIBLE_WAYPOINT_TYPES.get(selectedTypeIndex) : waypoint.getWaypointType();
            final boolean visited = visitedCheckBox.isChecked();
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

                    Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                    if (cache == null) {
                        finishHandler.sendEmptyMessage(SAVE_ERROR);
                        return null;
                    }
                    Waypoint oldWaypoint = cache.getWaypointById(id);
                    if (cache.addOrChangeWaypoint(waypoint, true)) {
                        DataStore.saveCache(cache, EnumSet.of(SaveFlag.SAVE_DB));
                        if (!StaticMapsProvider.hasAllStaticMapsForWaypoint(geocode, waypoint)) {
                            StaticMapsProvider.removeWpStaticMaps(oldWaypoint, geocode);
                            if (Settings.isStoreOfflineWpMaps()) {
                                StaticMapsProvider.storeWaypointStaticMap(cache, waypoint).subscribe();
                            }
                        }
                        if (modifyLocal.isChecked() || modifyBoth.isChecked()) {
                            if (!cache.hasUserModifiedCoords()) {
                                final Waypoint origWaypoint = new Waypoint(CgeoApplication.getInstance().getString(R.string.cache_coordinates_original), WaypointType.ORIGINAL, false);
                                origWaypoint.setCoords(cache.getCoords());
                                cache.addOrChangeWaypoint(origWaypoint, false);
                                cache.setUserModifiedCoords(true);
                            }
                            cache.setCoords(waypoint.getCoords());
                            DataStore.saveChangedCache(cache);
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

    public static void startActivityEditWaypoint(final Context context, final Geocache cache, final int waypointId) {
        EditWaypointActivity_.intent(context).geocode(cache.getGeocode()).id(waypointId).start();
    }

    public static void startActivityAddWaypoint(final Context context, final Geocache cache) {
        EditWaypointActivity_.intent(context).geocode(cache.getGeocode()).wpCount(cache.getWaypoints().size()).start();
    }
}
