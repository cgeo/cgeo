package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.DistanceParser;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.staticmaps.StaticMapsProvider;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.apache.commons.lang3.StringUtils;

@EActivity
public class EditWaypointActivity extends AbstractActionBarActivity implements CoordinatesInputDialog.CoordinateUpdate {

    public static final int SUCCESS = 0;
    public static final int UPLOAD_START = 1;
    public static final int UPLOAD_ERROR = 2;
    public static final int UPLOAD_NOT_POSSIBLE = 3;
    public static final int UPLOAD_SUCCESS = 4;
    public static final int SAVE_ERROR = 5;

    private static final ArrayList<WaypointType> POSSIBLE_WAYPOINT_TYPES = new ArrayList<>(WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL);

    @ViewById(R.id.buttonLatitude) protected Button buttonLat;
    @ViewById(R.id.buttonLongitude) protected Button buttonLon;
    @ViewById(R.id.note) protected EditText note;
    @ViewById(R.id.user_note) protected EditText userNote;
    @ViewById(R.id.wpt_visited_checkbox) protected CheckBox visitedCheckBox;
    @ViewById(R.id.name) protected AutoCompleteTextView waypointName;
    @ViewById(R.id.type) protected Spinner waypointTypeSelector;
    @ViewById(R.id.distance) protected EditText distanceView;
    @ViewById(R.id.modify_cache_coordinates_group) protected RadioGroup coordinatesGroup;
    @ViewById(R.id.modify_cache_coordinates_local_and_remote) protected RadioButton modifyBoth;
    @ViewById(R.id.distanceUnit) protected Spinner distanceUnitSelector;
    @ViewById(R.id.bearing) protected EditText bearing;
    @ViewById(R.id.modify_cache_coordinates_local) protected RadioButton modifyLocal;
    @ViewById(R.id.projection) protected LinearLayout projection;

    @Extra(Intents.EXTRA_GEOCODE) protected String geocode = null;
    @Extra(Intents.EXTRA_WAYPOINT_ID) protected int waypointId = -1;
    @Extra(Intents.EXTRA_COORDS) protected Geopoint initialCoords = null;

    @InstanceState protected int waypointTypeSelectorPosition = -1;

    private ProgressDialog waitDialog = null;
    private Waypoint waypoint = null;
    private String prefix = "";
    private String lookup = "---";
    private boolean own = true;
    private boolean originalCoordsEmpty = false;
    List<String> distanceUnits = null;
    /**
     * {@code true} if the activity is newly created, {@code false} if it is restored from an instance state
     */
    private boolean initViews = true;
    /**
     * This is the cache that the waypoint belongs to.
     */
    private Geocache cache;

    private final Handler loadWaypointHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            try {
                if (waypoint == null) {
                    Log.d("No waypoint loaded to edit. id= " + waypointId);
                    waypointId = -1;
                } else {
                    geocode = waypoint.getGeocode();
                    prefix = waypoint.getPrefix();
                    lookup = waypoint.getLookup();
                    own = waypoint.isUserDefined();
                    originalCoordsEmpty = waypoint.isOriginalCoordsEmpty();

                    if (initViews) {
                        visitedCheckBox.setChecked(waypoint.isVisited());
                        final Geopoint coordinates = waypoint.getCoords();
                        if (coordinates != null) {
                            buttonLat.setText(coordinates.format(GeopointFormatter.Format.LAT_DECMINUTE));
                            buttonLon.setText(coordinates.format(GeopointFormatter.Format.LON_DECMINUTE));
                        }
                        waypointName.setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getName())).toString());
                        Dialogs.moveCursorToEnd(waypointName);
                        if (TextUtils.containsHtml(waypoint.getNote())) {
                            note.setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getNote())).toString());
                        } else {
                            note.setText(StringUtils.trimToEmpty(waypoint.getNote()));
                        }
                        if (TextUtils.containsHtml(waypoint.getUserNote())) {
                            userNote.setText(Html.fromHtml(StringUtils.trimToEmpty(waypoint.getUserNote())).toString());
                        } else {
                            userNote.setText(StringUtils.trimToEmpty(waypoint.getUserNote()));
                        }
                        Dialogs.moveCursorToEnd(userNote);
                    }
                    new AsyncTask<Void, Void, Geocache>() {
                        @Override
                        protected Geocache doInBackground(final Void... params) {
                            return DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_ONLY);
                        }

                        @Override
                        protected void onPostExecute(final Geocache cache) {
                            setCoordsModificationVisibility(ConnectorFactory.getConnector(geocode));
                        }
                    }.execute();
                }

                if (own) {
                    initializeWaypointTypeSelector();
                } else {
                    waypointName.setEnabled(false);
                    note.setEnabled(false);
                    if (!waypoint.isOriginalCoordsEmpty()) {
                        projection.setVisibility(View.GONE);
                    }
                }
            } catch (final RuntimeException e) {
                Log.e("EditWaypointActivity.loadWaypointHandler", e);
            } finally {
                Dialogs.dismiss(waitDialog);
                waitDialog = null;
            }
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.editwaypoint_activity);

        if (StringUtils.isBlank(geocode) && waypointId <= 0) {
            showToast(res.getString(R.string.err_waypoint_cache_unknown));

            finish();
            return;
        }

        if (waypointId <= 0) {
            setTitle(res.getString(R.string.waypoint_add_title));
        } else {
            setTitle(res.getString(R.string.waypoint_edit_title));
        }

        buttonLat.setOnClickListener(new CoordDialogListener());
        buttonLon.setOnClickListener(new CoordDialogListener());

        final List<String> wayPointTypes = new ArrayList<>();
        for (final WaypointType wpt : WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL) {
            wayPointTypes.add(wpt.getL10n());
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, wayPointTypes);
        waypointName.setAdapter(adapter);

        if (savedInstanceState != null) {
            initViews = false;
        }

        if (geocode != null) {
            cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            setCoordsModificationVisibility(ConnectorFactory.getConnector(geocode));
        }
        if (waypointId > 0) { // existing waypoint
            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
            waitDialog.setCancelable(true);

            (new LoadWaypointThread()).start();

        } else { // new waypoint
            initializeWaypointTypeSelector();

            if (initialCoords != null) {
                updateCoordinates(initialCoords);
            }
        }

        initializeDistanceUnitSelector();

        disableSuggestions(distanceView);
    }

    private void setCoordsModificationVisibility(final IConnector con) {
        modifyBoth.setVisibility(con.supportsOwnCoordinates() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume(geoDirHandler.start(GeoDirHandler.UPDATE_GEODATA));
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_edit_waypoint_cancel:
                finish();
                return true;
            case R.id.menu_edit_waypoint_save:
                saveWaypoint(getActivityData());
                finish();
                return true;
            case android.R.id.home:
                toastOnChanged();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        toastOnChanged();
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.edit_waypoint_options, menu);
        return true;
    }

    private void initializeWaypointTypeSelector() {
        final ArrayAdapter<WaypointType> wpAdapter = new ArrayAdapter<WaypointType>(this, android.R.layout.simple_spinner_item, POSSIBLE_WAYPOINT_TYPES.toArray(new WaypointType[POSSIBLE_WAYPOINT_TYPES.size()])) {
            @Override
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                addWaypointIcon(position, view);
                return view;
            }

            @Override
            public View getDropDownView(final int position, final View convertView, final ViewGroup parent) {
                final View view = super.getDropDownView(position, convertView, parent);
                addWaypointIcon(position, view);
                return view;
            }

            private void addWaypointIcon(final int position, final View view) {
                final TextView label = (TextView) view.findViewById(android.R.id.text1);
                label.setCompoundDrawablesWithIntrinsicBounds(POSSIBLE_WAYPOINT_TYPES.get(position).markerId, 0, 0, 0);
            }
        };
        wpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        waypointTypeSelector.setAdapter(wpAdapter);

        waypointTypeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View v, final int pos, final long id) {
                final String oldDefaultName = waypointTypeSelectorPosition >= 0 ? getDefaultWaypointName(POSSIBLE_WAYPOINT_TYPES.get(waypointTypeSelectorPosition)) : StringUtils.EMPTY;
                waypointTypeSelectorPosition = pos;
                final String currentName = waypointName.getText().toString().trim();
                if (StringUtils.isBlank(currentName) || oldDefaultName.equals(currentName)) {
                    waypointName.setText(getDefaultWaypointName(getSelectedWaypointType()));
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
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
        distanceUnits = new ArrayList<>(Arrays.asList(res.getStringArray(R.array.distance_units)));
        if (initViews) {
            distanceUnitSelector.setSelection(Settings.useImperialUnits() ? 2 : 0); //0:m, 2:ft
        }
    }

    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoData(final GeoData geo) {
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
                waypoint = DataStore.loadWaypoint(waypointId);

                loadWaypointHandler.sendMessage(Message.obtain());
            } catch (final Exception e) {
                Log.e("EditWaypointActivity.loadWaypoint.run", e);
            }
        }
    }

    private class CoordDialogListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            Geopoint gp = null;
            try {
                gp = new Geopoint(buttonLat.getText().toString(), buttonLon.getText().toString());
            } catch (final Geopoint.ParseException ignored) {
                // button text is blank when creating new waypoint
            }
            final Geopoint geopoint = gp;
            new AsyncTask<Void, Void, Geocache>() {
                @Override
                protected Geocache doInBackground(final Void... params) {
                    return DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                }

                @Override
                protected void onPostExecute(final Geocache cache) {
                    if (waypoint == null || waypoint.isUserDefined() || waypoint.isOriginalCoordsEmpty()) {
                        showCoordinatesInputDialog(cache);
                    } else {
                        showCoordinateOptionsDialog(cache);
                    }
                }

                private void showCoordinateOptionsDialog(final Geocache cache) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setTitle(res.getString(R.string.waypoint_coordinates));
                    builder.setItems(R.array.waypoint_coordinates_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int item) {
                            final String selectedOption = res.getStringArray(R.array.waypoint_coordinates_options)[item];
                            if (res.getString(R.string.waypoint_copy_coordinates).equals(selectedOption) && geopoint != null) {
                                ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(geopoint.toString()));
                                showToast(res.getString(R.string.clipboard_copy_ok));
                            } else if (res.getString(R.string.waypoint_duplicate).equals(selectedOption) && cache.duplicateWaypoint(waypoint)) {
                                DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                                showToast(res.getString(R.string.waypoint_duplicated));
                            }
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                }

                private void showCoordinatesInputDialog(final Geocache cache) {
                    final CoordinatesInputDialog coordsDialog = CoordinatesInputDialog.getInstance(cache, geopoint);
                    coordsDialog.setCancelable(true);
                    coordsDialog.show(getSupportFragmentManager(), "wpeditdialog");
                }
            }.execute();
        }


    }

    @Override
    public void updateCoordinates(final Geopoint gp) {
        buttonLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
        buttonLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
    }

    /**
     * Suffix the waypoint type with a running number to get a default name.
     *
     * @param type
     *            type to create a new default name for
     *
     */
    private String getDefaultWaypointName(final WaypointType type) {
        final ArrayList<String> wpNames = new ArrayList<>();
        for (final Waypoint waypoint : cache.getWaypoints()) {
            wpNames.add(waypoint.getName());
        }
        // try final and trailhead without index
        if ((type == WaypointType.FINAL || type == WaypointType.TRAILHEAD) && !wpNames.contains(type.getL10n())) {
            return type.getL10n();
        }
        // for other types add an index by default, which is highest found index + 1
        int max = 0;
        for (int i = 0; i < 30; i++) {
            if (wpNames.contains(type.getL10n() + " " + i)) {
                max = i;
            }
        }
        return type.getL10n() + " " + (max + 1);
    }

    private WaypointType getSelectedWaypointType() {
        final int selectedTypeIndex = waypointTypeSelector.getSelectedItemPosition();
        return selectedTypeIndex >= 0 ? POSSIBLE_WAYPOINT_TYPES.get(selectedTypeIndex) : waypoint.getWaypointType();
    }

    private void toastOnChanged() {
        final ActivityData currentState = getActivityData();

        if (currentState != null && isWaypointChanged(currentState)) {
            ActivityMixin.showToast(this, R.string.warn_discard_changes);
        }
    }

    private ActivityData getActivityData() {

        final String bearingText = bearing.getText().toString();
        // combine distance from EditText and distanceUnit saved from Spinner
        final String distanceText = distanceView.getText().toString() + distanceUnits.get(distanceUnitSelector.getSelectedItemPosition());
        final String latText = buttonLat.getText().toString();
        final String lonText = buttonLon.getText().toString();

        if (StringUtils.isBlank(bearingText) && StringUtils.isBlank(distanceText)
                && StringUtils.isBlank(latText) && StringUtils.isBlank(lonText)) {
            Dialogs.message(this, R.string.err_point_no_position_given_title, R.string.err_point_no_position_given);
            return null;
        }

        Geopoint coords;

        if (StringUtils.isNotBlank(latText) && StringUtils.isNotBlank(lonText)) {
            try {
                coords = new Geopoint(latText, lonText);
            } catch (final Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
                return null;
            }
        } else {
            coords = Sensors.getInstance().currentGeo().getCoords();
        }

        if (StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)) {
            // bearing & distance
            final double bearing;
            try {
                bearing = Double.parseDouble(bearingText);
            } catch (final NumberFormatException ignored) {
                Dialogs.message(this, R.string.err_point_bear_and_dist_title, R.string.err_point_bear_and_dist);
                return null;
            }

            final double distance;
            try {
                distance = DistanceParser.parseDistance(distanceText,
                        !Settings.useImperialUnits());
            } catch (final NumberFormatException ignored) {
                showToast(res.getString(R.string.err_parse_dist));
                return null;
            }

            coords = coords.project(bearing, distance);
        }

        final ActivityData currentState = new ActivityData();

        currentState.coords = coords;

        final String givenName = waypointName.getText().toString().trim();
        currentState.name = StringUtils.defaultIfBlank(givenName, getDefaultWaypointName(getSelectedWaypointType()));
        currentState.noteText = note.getText().toString().trim();
        currentState.userNoteText = userNote.getText().toString().trim();
        currentState.type = getSelectedWaypointType();
        currentState.visited = visitedCheckBox.isChecked();

        return currentState;
    }

    private boolean isWaypointChanged(@NonNull final ActivityData currentState) {
        return waypoint == null
                || !Geopoint.equals(currentState.coords, waypoint.getCoords())
                || !StringUtils.equals(currentState.name, waypoint.getName())
                || !StringUtils.equals(currentState.noteText, waypoint.getNote())
                || !StringUtils.equals(currentState.userNoteText, waypoint.getUserNote())
                || currentState.visited != waypoint.isVisited()
                || currentState.type != waypoint.getWaypointType();

    }

    private static class FinishWaypointSaveHandler extends WeakReferenceHandler<EditWaypointActivity> {

        private final Geopoint coords;

        FinishWaypointSaveHandler(final EditWaypointActivity activity, final Geopoint coords) {
            super(activity);
            this.coords = new Geopoint(coords.getLatitude(), coords.getLongitude());
        }

        @Override
        public void handleMessage(final Message msg) {
            final EditWaypointActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case UPLOAD_SUCCESS:
                    ActivityMixin.showApplicationToast(activity.getString(R.string.waypoint_coordinates_has_been_modified_on_website, coords));
                    break;
                case SUCCESS:
                    break;
                case UPLOAD_START:
                    break;
                case UPLOAD_ERROR:
                    ActivityMixin.showApplicationToast(activity.getString(R.string.waypoint_coordinates_upload_error));
                    break;
                case UPLOAD_NOT_POSSIBLE:
                    ActivityMixin.showApplicationToast(activity.getString(R.string.waypoint_coordinates_couldnt_be_modified_on_website));
                    break;
                case SAVE_ERROR:
                    ActivityMixin.showApplicationToast(activity.getString(R.string.err_waypoint_add_failed));
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private void saveWaypoint(final ActivityData currentState) {
        // currentState might be null here if there is a problem with the waypoints data
        if (currentState == null) {
            return;
        }

        final Handler finishHandler = new FinishWaypointSaveHandler(this, currentState.coords);

        class SaveWptTask extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(final Void... params) {
                final Waypoint waypoint = new Waypoint(currentState.name, currentState.type, own);
                waypoint.setGeocode(geocode);
                waypoint.setPrefix(prefix);
                waypoint.setLookup(lookup);
                waypoint.setCoords(currentState.coords);
                waypoint.setNote(currentState.noteText);
                waypoint.setUserNote(currentState.userNoteText);
                waypoint.setVisited(currentState.visited);
                waypoint.setId(waypointId);
                waypoint.setOriginalCoordsEmpty(originalCoordsEmpty);

                final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
                if (cache == null) {
                    finishHandler.sendEmptyMessage(SAVE_ERROR);
                    return null;
                }
                final Waypoint oldWaypoint = cache.getWaypointById(waypointId);
                if (cache.addOrChangeWaypoint(waypoint, true)) {
                    DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));

                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(Intents.INTENT_CACHE_CHANGED));

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
                            final boolean result = uploadModifiedCoords(cache, waypoint.getCoords());
                            finishHandler.sendEmptyMessage(result ? UPLOAD_SUCCESS : UPLOAD_ERROR);
                        } else {
                            ActivityMixin.showApplicationToast(getString(R.string.waypoint_coordinates_couldnt_be_modified_on_website));
                            finishHandler.sendEmptyMessage(UPLOAD_NOT_POSSIBLE);
                        }
                    } else {
                        finishHandler.sendEmptyMessage(SUCCESS);
                    }
                } else {
                    finishHandler.sendEmptyMessage(SAVE_ERROR);
                }

                LocalBroadcastManager.getInstance(EditWaypointActivity.this).sendBroadcast(new Intent(Intents.INTENT_CACHE_CHANGED));

                return null;
            }
        }
        new SaveWptTask().execute();
    }

    private static class ActivityData {
        public String name;
        public WaypointType type;
        public Geopoint coords;
        public String noteText;
        public String userNoteText;
        public boolean visited;
    }

    private static boolean uploadModifiedCoords(final Geocache cache, final Geopoint waypointUploaded) {
        final IConnector con = ConnectorFactory.getConnector(cache);
        return con.supportsOwnCoordinates() && con.uploadModifiedCoordinates(cache, waypointUploaded);
    }

    public static void startActivityEditWaypoint(final Context context, final Geocache cache, final int waypointId) {
        EditWaypointActivity_.intent(context).geocode(cache.getGeocode()).waypointId(waypointId).start();
    }

    public static void startActivityAddWaypoint(final Context context, final Geocache cache) {
        EditWaypointActivity_.intent(context).geocode(cache.getGeocode()).start();
    }

    public static void startActivityAddWaypoint(final Context context, final Geocache cache, final Geopoint initialCoords) {
        EditWaypointActivity_.intent(context).geocode(cache.getGeocode()).initialCoords(initialCoords).start();
    }

    @Override
    public void finish() {
        Dialogs.dismiss(waitDialog);
        super.finish();
    }
}
