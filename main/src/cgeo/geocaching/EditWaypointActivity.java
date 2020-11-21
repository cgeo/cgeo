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
import cgeo.geocaching.models.CalcState;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.SmileyImage;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;
import static cgeo.geocaching.models.Waypoint.getDefaultWaypointName;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import io.reactivex.rxjava3.schedulers.Schedulers;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.apache.commons.lang3.StringUtils;

@EActivity
public class EditWaypointActivity extends AbstractActionBarActivity implements CoordinatesInputDialog.CoordinateUpdate, CoordinatesInputDialog.CalculateState {

    public static final int SUCCESS = 0;
    public static final int UPLOAD_START = 1;
    public static final int UPLOAD_ERROR = 2;
    public static final int UPLOAD_NOT_POSSIBLE = 3;
    public static final int UPLOAD_SUCCESS = 4;
    public static final int SAVE_ERROR = 5;

    private static final String CALC_STATE_JSON = "calc_state_json";
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

    /**
     * {@code true} if the activity is newly created, {@code false} if it is restored from an instance state
     */
    private boolean initViews = true;
    /**
     * This is the cache that the waypoint belongs to.
     */
    private Geocache cache;
    /**
     * State the Coordinate Calculator was last left in.
     */
    private String calcStateJson;

    private final Handler loadWaypointHandler = new LoadWaypointHandler(this);

    private static final class LoadWaypointHandler extends WeakReferenceHandler<EditWaypointActivity> {
        LoadWaypointHandler(final EditWaypointActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            final EditWaypointActivity activity = getReference();
            if (activity == null) {
                return;
            }

            try {
                final Waypoint waypoint = activity.waypoint;
                if (waypoint == null) {
                    Log.d("No waypoint loaded to edit. id= " + activity.waypointId);
                    activity.waypointId = -1;
                } else {
                    activity.geocode = waypoint.getGeocode();
                    activity.prefix = waypoint.getPrefix();
                    activity.lookup = waypoint.getLookup();
                    activity.own = waypoint.isUserDefined();
                    activity.originalCoordsEmpty = waypoint.isOriginalCoordsEmpty();
                    activity.calcStateJson = waypoint.getCalcStateJson();

                    if (activity.initViews) {
                        activity.visitedCheckBox.setChecked(waypoint.isVisited());
                        activity.updateCoordinates(waypoint.getCoords());
                        final AutoCompleteTextView waypointName = activity.waypointName;
                        waypointName.setText(TextUtils.stripHtml(StringUtils.trimToEmpty(waypoint.getName())));
                        Dialogs.moveCursorToEnd(waypointName);
                        activity.note.setText(HtmlCompat.fromHtml(StringUtils.trimToEmpty(waypoint.getNote()), HtmlCompat.FROM_HTML_MODE_LEGACY, new SmileyImage(activity.geocode, activity.note), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
                        final EditText userNote = activity.userNote;
                        userNote.setText(StringUtils.trimToEmpty(waypoint.getUserNote()));
                        Dialogs.moveCursorToEnd(userNote);
                    }
                    AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.loadCache(activity.geocode, LoadFlags.LOAD_CACHE_ONLY), () -> activity.setCoordsModificationVisibility(ConnectorFactory.getConnector(activity.geocode)));
                }

                if (activity.own) {
                    activity.initializeWaypointTypeSelector();
                    if (StringUtils.isNotBlank(activity.note.getText())) {
                        activity.userNote.setText(activity.note.getText().append("\n").append(activity.userNote.getText()));
                        activity.note.setText("");
                    }
                } else {
                    activity.nonEditable(activity.waypointName);
                    activity.nonEditable(activity.note);
                    if (waypoint != null && !waypoint.isOriginalCoordsEmpty()) {
                        activity.projection.setVisibility(View.GONE);
                    }
                }

                if (StringUtils.isBlank(activity.note.getText())) {
                    activity.note.setVisibility(View.GONE);
                }

            } catch (final RuntimeException e) {
                Log.e("EditWaypointActivity.loadWaypointHandler", e);
            } finally {
                Dialogs.dismiss(activity.waitDialog);
                activity.waitDialog = null;
            }
        }
    }

    private void nonEditable(final TextView textView) {
        textView.setKeyListener(null);
        textView.setTextIsSelectable(true);
    }

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
            calcStateJson = savedInstanceState.getString(CALC_STATE_JSON);
        } else {
            calcStateJson = null;
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
            note.setVisibility(View.GONE);
            updateCoordinates(initialCoords);
        }


        initializeDistanceUnitSelector();

        disableSuggestions(distanceView);
    }

    private void setCoordsModificationVisibility(final IConnector con) {
        modifyBoth.setVisibility(con.supportsOwnCoordinates() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        final int itemId = item.getItemId();
        if (itemId == R.id.menu_edit_waypoint_cancel) {
            finish();
        } else if (itemId == R.id.menu_edit_waypoint_save) {
            saveWaypoint(getActivityData());
            finish();
        } else if (itemId == android.R.id.home) {
            finishConfirmDiscard();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        // resume location access
        PermissionHandler.executeIfLocationPermissionGranted(this,
                new RestartLocationPermissionGrantedCallback(PermissionRequestContext.EditWaypointActivity) {

                    @Override
                    public void executeAfter() {
                        resumeDisposables(geoDirHandler.start(GeoDirHandler.UPDATE_GEODATA));
                    }
                });
    }

    @Override
    public void onBackPressed() {
        finishConfirmDiscard();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.edit_waypoint_options, menu);
        return true;
    }

    private void initializeWaypointTypeSelector() {
        final ArrayAdapter<WaypointType> wpAdapter = new ArrayAdapter<WaypointType>(this, android.R.layout.simple_spinner_item, POSSIBLE_WAYPOINT_TYPES.toArray(new WaypointType[POSSIBLE_WAYPOINT_TYPES.size()])) {
            @Override
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                addWaypointIcon(position, view);
                return view;
            }

            @Override
            public View getDropDownView(final int position, final View convertView, @NonNull final ViewGroup parent) {
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
                final String oldDefaultName = waypointTypeSelectorPosition >= 0 ? getDefaultWaypointName(cache, POSSIBLE_WAYPOINT_TYPES.get(waypointTypeSelectorPosition)) : StringUtils.EMPTY;
                waypointTypeSelectorPosition = pos;
                final String currentName = waypointName.getText().toString().trim();
                if (StringUtils.isBlank(currentName) || oldDefaultName.equals(currentName)) {
                    waypointName.setText(getDefaultWaypointName(cache, getSelectedWaypointType()));
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // empty
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
        if (initViews) {
            distanceUnitSelector.setSelection(Settings.useImperialUnits() ?
                    DistanceParser.DistanceUnit.FT.getValue() : DistanceParser.DistanceUnit.M.getValue());
        }
    }

    private class LoadWaypointThread extends Thread {

        @Override
        public void run() {
            try {
                waypoint = DataStore.loadWaypoint(waypointId);
                if (waypoint == null) {
                    return;
                }

                calcStateJson = waypoint.getCalcStateJson();
                loadWaypointHandler.sendMessage(Message.obtain());
            } catch (final Exception e) {
                Log.e("EditWaypointActivity.loadWaypoint.run", e);
            }
        }
    }

    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoData(final GeoData geo) {
            try {
                // keep updates coming while activity is visible, to have better coords when needed
                Log.i("update geo data: " + geo);
            } catch (final Exception e) {
                Log.e("failed to update location", e);
            }
        }
    };

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
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS), geocache -> {
                if (waypoint == null || waypoint.isUserDefined() || waypoint.isOriginalCoordsEmpty()) {
                    showCoordinatesInputDialog(geopoint, cache);
                } else {
                    showCoordinateOptionsDialog(view, geopoint, cache);
                }
            });

        }

        private void showCoordinateOptionsDialog(final View view, final Geopoint geopoint, final Geocache cache) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(res.getString(R.string.waypoint_coordinates));
            builder.setItems(R.array.waypoint_coordinates_options, (dialog, item) -> {
                final String selectedOption = res.getStringArray(R.array.waypoint_coordinates_options)[item];
                if (res.getString(R.string.waypoint_copy_coordinates).equals(selectedOption) && geopoint != null) {
                    ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(geopoint.toString()));
                    showToast(res.getString(R.string.clipboard_copy_ok));
                } else if (res.getString(R.string.waypoint_duplicate).equals(selectedOption)) {
                    final Waypoint copy = cache.duplicateWaypoint(waypoint);
                    if (copy != null) {
                        DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                        EditWaypointActivity.startActivityEditWaypoint(EditWaypointActivity.this, cache, copy.getId());
                    }
                }
            });
            final AlertDialog alert = builder.create();
            alert.show();
        }

        private void showCoordinatesInputDialog(final Geopoint geopoint, final Geocache cache) {
            final CoordinatesInputDialog coordsDialog = CoordinatesInputDialog.getInstance(cache, geopoint);
            coordsDialog.setCancelable(true);
            coordsDialog.show(getSupportFragmentManager(), "wpeditdialog");
        }

    }

    @Override
    public void updateCoordinates(final Geopoint gp) {
        if (gp != null) {
            buttonLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
            buttonLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
            setProjectionEnabled(true);
        } else {
            buttonLat.setText(R.string.waypoint_latitude_null);
            buttonLon.setText(R.string.waypoint_longitude_null);
            setProjectionEnabled(false);
        }
    }

    @Override
    public boolean supportsNullCoordinates() {
        return true;
    }

    private void setProjectionEnabled(final boolean enabled) {
        bearing.setEnabled(enabled);
        distanceView.setEnabled(enabled);
        distanceUnitSelector.setEnabled(enabled);
        if (!enabled) {
            bearing.setText("");
            distanceView.setText("");
        }
    }

    @Override
    public void saveCalculatorState(final CalcState calcState) {
        this.calcStateJson = calcState != null ? calcState.toJSON().toString() : null;
    }

    @Override
    public CalcState fetchCalculatorState() {
        return CalcState.fromJSON(calcStateJson);
    }

    /**
     * Save the current state of the calculator such that it can be restored after screen rotation (or similar)
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CALC_STATE_JSON, calcStateJson);
    }

    private WaypointType getSelectedWaypointType() {
        final int selectedTypeIndex = waypointTypeSelector.getSelectedItemPosition();
        return selectedTypeIndex >= 0 ? POSSIBLE_WAYPOINT_TYPES.get(selectedTypeIndex) : waypoint.getWaypointType();
    }

    private void finishConfirmDiscard() {
        final ActivityData currentState = getActivityData();

        if (currentState != null && isWaypointChanged(currentState)) {
            Dialogs.confirm(this, R.string.confirm_unsaved_changes_title, R.string.confirm_discard_wp_changes, (dialog, which) -> finish());
        } else {
            finish();
        }
    }

    public EditText getUserNotes() {
        return userNote;
    }

    private boolean coordsTextsValid(final String latText, final String lonText) {
        return !latText.equals(getResources().getString(R.string.waypoint_latitude_null))
                && !lonText.equals(getResources().getString(R.string.waypoint_longitude_null));
    }

    private ActivityData getActivityData() {
        final String latText = buttonLat.getText().toString();
        final String lonText = buttonLon.getText().toString();
        Geopoint coords = null;
        if (coordsTextsValid(latText, lonText)) {
            try {
                coords = new Geopoint(latText, lonText);
            } catch (final Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
                return null;
            }
        }

        final String bearingText = bearing.getText().toString();
        final String distanceText = distanceView.getText().toString();
        final DistanceParser.DistanceUnit distanceUnit = DistanceParser.DistanceUnit.getById(distanceUnitSelector.getSelectedItemPosition());

        if (coords != null && StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)) {
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
                distance = DistanceParser.parseDistance(distanceText, distanceUnit);
            } catch (final NumberFormatException ignored) {
                showToast(res.getString(R.string.err_parse_dist));
                return null;
            }

            coords = coords.project(bearing, distance);
        }

        final ActivityData currentState = new ActivityData();

        currentState.coords = coords;

        final String givenName = waypointName.getText().toString().trim();
        currentState.name = StringUtils.defaultIfBlank(givenName, getDefaultWaypointName(cache, getSelectedWaypointType()));
        if (own) {
            currentState.noteText = "";
        } else { // keep original note
            currentState.noteText = waypoint.getNote();
        }
        currentState.userNoteText = userNote.getText().toString().trim();
        currentState.type = getSelectedWaypointType();
        currentState.visited = visitedCheckBox.isChecked();
        currentState.calcStateJson = calcStateJson;

        return currentState;
    }

    private boolean isWaypointChanged(@NonNull final ActivityData currentState) {
        return waypoint == null
                || !Geopoint.equalsFormatted(currentState.coords, waypoint.getCoords(), GeopointFormatter.Format.LAT_LON_DECMINUTE)
                || !StringUtils.equals(currentState.name, waypoint.getName())
                || !StringUtils.equals(currentState.noteText, waypoint.getNote())
                || !StringUtils.equals(currentState.userNoteText, waypoint.getUserNote())
                || currentState.visited != waypoint.isVisited()
                || currentState.type != waypoint.getWaypointType()
                || !StringUtils.equals(currentState.calcStateJson, waypoint.getCalcStateJson());
    }

    private static class FinishWaypointSaveHandler extends WeakReferenceHandler<EditWaypointActivity> {

        private final Geopoint coords;

        FinishWaypointSaveHandler(final EditWaypointActivity activity, final Geopoint coords) {
            super(activity);
            this.coords = coords != null ? new Geopoint(coords.getLatitude(), coords.getLongitude()) : null;
        }

        @Override
        public void handleMessage(final Message msg) {
            final EditWaypointActivity activity = getReference();
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

        AndroidRxUtils.computationScheduler.scheduleDirect(() -> saveWaypointInBackground(currentState, finishHandler));
    }

    protected void saveWaypointInBackground(final ActivityData currentState, final Handler finishHandler) {
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
        waypoint.setCalcStateJson(currentState.calcStateJson);

        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
        if (cache == null) {
            finishHandler.sendEmptyMessage(SAVE_ERROR);
            return;
        }
        if (cache.addOrChangeWaypoint(waypoint, true)) {
            if (waypoint.getCoords() != null && (modifyLocal.isChecked() || modifyBoth.isChecked())) {
                if (!cache.hasUserModifiedCoords()) {
                    final Waypoint origWaypoint = new Waypoint(CgeoApplication.getInstance().getString(R.string.cache_coordinates_original), WaypointType.ORIGINAL, false);
                    origWaypoint.setCoords(cache.getCoords());
                    cache.addOrChangeWaypoint(origWaypoint, false);
                    cache.setUserModifiedCoords(true);
                }
                cache.setCoords(waypoint.getCoords());
                DataStore.saveUserModifiedCoords(cache);
            }
            if (waypoint.getCoords() != null && modifyBoth.isChecked()) {
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
    }

    private static class ActivityData {
        public String name;
        public WaypointType type;
        public Geopoint coords;
        public String noteText;
        public String userNoteText;
        public boolean visited;
        public String calcStateJson;
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
