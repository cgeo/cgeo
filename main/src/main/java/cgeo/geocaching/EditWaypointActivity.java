package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.databinding.EditwaypointActivityBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.DistanceParser;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.CalculatedCoordinate;
import cgeo.geocaching.models.CoordinateInputData;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.LegacyCalculatedCoordinateMigrator;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.HtmlImage;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.service.GeocacheChangedBroadcastReceiver;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.UnknownTagsHandler;
import static cgeo.geocaching.models.Waypoint.getDefaultWaypointName;

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
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;


public class EditWaypointActivity extends AbstractActionBarActivity implements CoordinatesInputDialog.CoordinateUpdate {

    public static final int SUCCESS = 0;
    public static final int UPLOAD_START = 1;
    public static final int UPLOAD_ERROR = 2;
    public static final int UPLOAD_NOT_POSSIBLE = 3;
    public static final int UPLOAD_SUCCESS = 4;
    public static final int SAVE_ERROR = 5;

    private static final String CALC_STATE_JSON = "calc_state_json";
    private static final String WP_TYPE_SELECTOR_POS = "wp_type_selector_pos";
    private static final ArrayList<WaypointType> POSSIBLE_WAYPOINT_TYPES = new ArrayList<>(WaypointType.ALL_TYPES_EXCEPT_OWN_ORIGINAL_AND_GENERATED);
    private static final ArrayList<WaypointType> POSSIBLE_WAYPOINT_TYPES_WITH_GENERATED = new ArrayList<>(WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL);

    private String geocode = null;
    private int waypointId = -1;
    private Geopoint initialCoords = null;

    private int waypointTypeSelectorPosition = -1;

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
    private EditwaypointActivityBinding binding;

    /**
     * This is the cache that the waypoint belongs to.
     */
    private Geocache cache;
    /**
     * State the Coordinate Calculator was last left in.
     */
    private String calcStateString;

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
                    activity.calcStateString = waypoint.getCalcStateConfig();

                    if (activity.initViews) {
                        activity.binding.wptVisitedCheckbox.setChecked(waypoint.isVisited());
                        activity.updateCoordinates(waypoint.getCoords());
                        final AutoCompleteTextView waypointName = activity.binding.name;
                        waypointName.setText(TextUtils.stripHtml(StringUtils.trimToEmpty(waypoint.getName())));
                        Dialogs.moveCursorToEnd(waypointName);
                        activity.binding.note.setText(HtmlCompat.fromHtml(StringUtils.trimToEmpty(waypoint.getNote()), HtmlCompat.FROM_HTML_MODE_LEGACY, new HtmlImage(activity.geocode, true, false, activity.binding.note, false), new UnknownTagsHandler()), TextView.BufferType.SPANNABLE);
                        final EditText userNote = activity.binding.userNote;
                        userNote.setText(StringUtils.trimToEmpty(waypoint.getUserNote()));
                        Dialogs.moveCursorToEnd(userNote);
                    }
                    AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.loadCache(activity.geocode, LoadFlags.LOAD_CACHE_ONLY), () -> activity.setCoordsModificationVisibility(ConnectorFactory.getConnector(activity.geocode)));
                }

                if (activity.own) {
                    activity.initializeWaypointTypeSelector(waypoint != null && waypoint.getWaypointType() != WaypointType.GENERATED);
                    if (StringUtils.isNotBlank(activity.binding.note.getText())) {
                        activity.binding.userNote.setText(activity.binding.note.getText().append("\n").append(activity.binding.userNote.getText()));
                        activity.binding.note.setText("");
                    }
                } else {
                    activity.nonEditable(activity.binding.nameLayout, activity.binding.name);
                    activity.nonEditable(activity.binding.noteLayout, activity.binding.note);
                    if (waypoint != null && !waypoint.isOriginalCoordsEmpty()) {
                        activity.binding.projection.setVisibility(View.GONE);
                    }
                }

                if (StringUtils.isBlank(activity.binding.note.getText())) {
                    activity.binding.noteLayout.setVisibility(View.GONE);
                }

            } catch (final RuntimeException e) {
                Log.e("EditWaypointActivity.loadWaypointHandler", e);
            } finally {
                Dialogs.dismiss(activity.waitDialog);
                activity.waitDialog = null;
            }
        }
    }

    private void nonEditable(final TextInputLayout textLayout, final TextView textView) {
        textLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
        textView.setKeyListener(null);
        textView.setTextIsSelectable(true);
    }

    @Override
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = EditwaypointActivityBinding.inflate(getLayoutInflater());
        setThemeAndContentView(binding);

        // get parameters
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            geocode = extras.getString(Intents.EXTRA_GEOCODE);
            waypointId = extras.getInt(Intents.EXTRA_WAYPOINT_ID);
            initialCoords = extras.getParcelable(Intents.EXTRA_COORDS);
        }

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

        binding.buttonLatLongitude.setOnClickListener(new CoordDialogListener());

        final List<String> wayPointTypes = new ArrayList<>();
        for (final WaypointType wpt : WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL) {
            if (!wayPointTypes.contains(wpt.getNameForNewWaypoint())) {
                wayPointTypes.add(wpt.getNameForNewWaypoint());
            }
        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, wayPointTypes);
        binding.name.setAdapter(adapter);

        if (savedInstanceState != null) {
            initViews = false;
            calcStateString = savedInstanceState.getString(CALC_STATE_JSON);
            waypointTypeSelectorPosition = savedInstanceState.getInt(WP_TYPE_SELECTOR_POS);
        } else {
            calcStateString = null;
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
            initializeWaypointTypeSelector(true);
            binding.noteLayout.setVisibility(View.GONE);
            updateCoordinates(initialCoords);
        }


        initializeDistanceUnitSelector();

        disableSuggestions(binding.distance);
    }

    private void setCoordsModificationVisibility(final IConnector con) {
        binding.modifyCacheCoordinatesLocalAndRemote.setVisibility(con.supportsOwnCoordinates() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        final int itemId = item.getItemId();
        if (itemId == R.id.menu_item_cancel) {
            finish();
        } else if (itemId == R.id.menu_item_save) {
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
        resumeDisposables(geoDirHandler.start(GeoDirHandler.UPDATE_GEODATA));
    }

    @Override
    public void onBackPressed() {
        finishConfirmDiscard();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ok_cancel, menu);
        return true;
    }

    private void initializeWaypointTypeSelector(final boolean excludeGenerated) {
        final ArrayList<WaypointType> allowedTypes = excludeGenerated ? POSSIBLE_WAYPOINT_TYPES : POSSIBLE_WAYPOINT_TYPES_WITH_GENERATED;
        final ArrayAdapter<WaypointType> wpAdapter = new ArrayAdapter<WaypointType>(this, android.R.layout.simple_spinner_item, allowedTypes.toArray(new WaypointType[0])) {
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
                final TextView label = view.findViewById(android.R.id.text1);
                label.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getWaypointTypeMarker(res, allowedTypes.get(position)), null, null, null);
            }
        };
        wpAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.type.setAdapter(wpAdapter);

        binding.type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View v, final int pos, final long id) {
                final String oldDefaultName = waypointTypeSelectorPosition >= 0 ? getDefaultWaypointName(cache, allowedTypes.get(waypointTypeSelectorPosition)) : StringUtils.EMPTY;
                waypointTypeSelectorPosition = pos;
                final String currentName = binding.name.getText().toString().trim();
                if (StringUtils.isBlank(currentName) || oldDefaultName.equals(currentName)) {
                    binding.name.setText(getDefaultWaypointName(cache, getSelectedWaypointType()));
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                // empty
            }
        });

        binding.type.setSelection(getDefaultWaypointTypePosition(wpAdapter));
        binding.type.setVisibility(View.VISIBLE);
    }

    private int getDefaultWaypointTypePosition(final ArrayAdapter<WaypointType> wpAdapter) {
        // potentially restore saved instance state
        if (waypointTypeSelectorPosition >= 0) {
            return waypointTypeSelectorPosition;
        }

        // when editing, use the existing type
        if (waypoint != null) {
            return wpAdapter.getPosition(waypoint.getWaypointType());
        }

        // make default for new waypoint depend on cache type
        switch (cache.getType()) {
            case MYSTERY:
                return wpAdapter.getPosition(WaypointType.FINAL);
            case MULTI:
                return wpAdapter.getPosition(WaypointType.STAGE);
            default:
                return wpAdapter.getPosition(WaypointType.WAYPOINT);
        }
    }

    private void initializeDistanceUnitSelector() {
        if (initViews) {
            binding.distanceUnit.setSelection(Settings.useImperialUnits() ?
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

                calcStateString = waypoint.getCalcStateConfig();
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
                final String[] latlonText = binding.buttonLatLongitude.getText().toString().split("\n");
                gp = new Geopoint(StringUtils.trim(latlonText[0]), StringUtils.trim(latlonText[1]));
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
            final AlertDialog.Builder builder = Dialogs.newBuilder(view.getContext());
            builder.setTitle(res.getString(R.string.waypoint_coordinates));
            builder.setItems(R.array.waypoint_coordinates_options, (dialog, item) -> {
                final String selectedOption = res.getStringArray(R.array.waypoint_coordinates_options)[item];
                if (res.getString(R.string.waypoint_copy_coordinates).equals(selectedOption) && geopoint != null) {
                    ClipboardUtils.copyToClipboard(GeopointFormatter.reformatForClipboard(geopoint.toString()));
                    showToast(res.getString(R.string.clipboard_copy_ok));
                } else if (res.getString(R.string.waypoint_duplicate).equals(selectedOption)) {
                    final Waypoint copy = cache.duplicateWaypoint(waypoint, true);
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
            final CoordinateInputData cid = new CoordinateInputData();
            cid.setGeopoint(geopoint);
            if (cache != null) {
                cid.setGeocode(cache.getGeocode());
            }
            cid.setNotes(getUserNotes().getText().toString());
            final CalculatedCoordinate cc = CalculatedCoordinate.createFromConfig(calcStateString);
            cid.setCalculatedCoordinate(cc);

            CoordinatesInputDialog.show(getSupportFragmentManager(), cid);
        }
    }

    @Override
    public void updateCoordinates(@Nullable final Geopoint gp) {
        if (gp != null) {
            binding.buttonLatLongitude.setText(String.format("%s%n%s", gp.format(GeopointFormatter.Format.LAT_DECMINUTE), gp.format(GeopointFormatter.Format.LON_DECMINUTE)));
            setProjectionEnabled(true);
        } else {
            binding.buttonLatLongitude.setText(String.format("%s%n%s", getResources().getString(R.string.waypoint_latitude_null), getResources().getString(R.string.waypoint_longitude_null)));
            setProjectionEnabled(false);
        }
        ((MaterialButton) binding.buttonLatLongitude).setIconResource(CalculatedCoordinate.isValidConfig(calcStateString) ? R.drawable.ic_menu_variable : 0);
    }

    @Override
    public void updateCoordinates(final CoordinateInputData coordinateInputData) {
        getUserNotes().setText(coordinateInputData.getNotes());

        this.calcStateString = null;
        if (coordinateInputData.getCalculatedCoordinate() != null) {
            this.calcStateString = coordinateInputData.getCalculatedCoordinate().toConfig();
        }
        updateCoordinates(coordinateInputData.getGeopoint());
    }

    @Override
    public boolean supportsNullCoordinates() {
        return true;
    }

    private void setProjectionEnabled(final boolean enabled) {
        binding.bearing.setEnabled(enabled);
        binding.distance.setEnabled(enabled);
        binding.distanceUnit.setEnabled(enabled);
        if (!enabled) {
            binding.bearing.setText("");
            binding.distance.setText("");
        }
    }

    /**
     * Save the current state of the calculator such that it can be restored after screen rotation (or similar)
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CALC_STATE_JSON, calcStateString);
        outState.putInt(WP_TYPE_SELECTOR_POS, waypointTypeSelectorPosition);

    }

    private WaypointType getSelectedWaypointType() {
        final WaypointType selectedType = (WaypointType) binding.type.getSelectedItem();
        return selectedType != null ? selectedType : waypoint.getWaypointType();
    }

    private void finishConfirmDiscard() {
        final ActivityData currentState = getActivityData();

        if (currentState != null && isWaypointChanged(currentState)) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_wp_changes).confirm((dialog, which) -> finish());
        } else {
            finish();
        }
    }

    public EditText getUserNotes() {
        return binding.userNote;
    }

    private boolean coordsTextsValid(final String latText, final String lonText) {
        return !latText.equals(getResources().getString(R.string.waypoint_latitude_null))
                && !lonText.equals(getResources().getString(R.string.waypoint_longitude_null));
    }

    @Nullable
    private ActivityData getActivityData() {
        final String[] latlonText = binding.buttonLatLongitude.getText().toString().split("\n");
        final String latText = StringUtils.trim(latlonText[0]);
        final String lonText = StringUtils.trim(latlonText[1]);
        Geopoint coords = null;
        if (coordsTextsValid(latText, lonText)) {
            try {
                coords = new Geopoint(latText, lonText);
            } catch (final Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
                return null;
            }
        }

        final String bearingText = binding.bearing.getText().toString();
        final String distanceText = binding.distance.getText().toString();
        final DistanceParser.DistanceUnit distanceUnit = DistanceParser.DistanceUnit.getById(binding.distanceUnit.getSelectedItemPosition());

        if (coords != null && StringUtils.isNotBlank(bearingText) && StringUtils.isNotBlank(distanceText)) {
            // bearing & distance
            final double bearing;
            try {
                bearing = Double.parseDouble(bearingText);
            } catch (final NumberFormatException ignored) {
                SimpleDialog.of(this).setTitle(R.string.err_point_bear_and_dist_title).setMessage(R.string.err_point_bear_and_dist).show();
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

        final String givenName = binding.name.getText().toString().trim();
        currentState.name = StringUtils.defaultIfBlank(givenName, getDefaultWaypointName(cache, getSelectedWaypointType()));
        if (own) {
            currentState.noteText = "";
        } else { // keep original note
            currentState.noteText = waypoint.getNote();
        }
        currentState.userNoteText = binding.userNote.getText().toString().trim();
        currentState.type = getSelectedWaypointType();
        currentState.visited = binding.wptVisitedCheckbox.isChecked();
        currentState.calcStateJson = calcStateString;

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
                || !StringUtils.equals(currentState.calcStateJson, waypoint.getCalcStateConfig());
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

        //if this was a calculated waypoint, then variable state may have changed -> save this
        cache.getVariables().saveState();

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
        waypoint.setCalcStateConfig(currentState.calcStateJson);

        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
        if (cache == null) {
            finishHandler.sendEmptyMessage(SAVE_ERROR);
            return;
        }
        if (cache.addOrChangeWaypoint(waypoint, true)) {
            if (waypoint.getCoords() != null && (binding.modifyCacheCoordinatesLocal.isChecked() || binding.modifyCacheCoordinatesLocalAndRemote.isChecked())) {
                if (!cache.hasUserModifiedCoords()) {
                    final Waypoint origWaypoint = new Waypoint(CgeoApplication.getInstance().getString(R.string.cache_coordinates_original), WaypointType.ORIGINAL, false);
                    origWaypoint.setCoords(cache.getCoords());
                    cache.addOrChangeWaypoint(origWaypoint, false);
                    cache.setUserModifiedCoords(true);
                }
                cache.setCoords(waypoint.getCoords());
                DataStore.saveUserModifiedCoords(cache);
            }
            if (waypoint.getCoords() != null && binding.modifyCacheCoordinatesLocalAndRemote.isChecked()) {
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

        GeocacheChangedBroadcastReceiver.sendBroadcast(this, cache.getGeocode());
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

        final Waypoint wp = cache.getWaypointById(waypointId);
        if (LegacyCalculatedCoordinateMigrator.needsMigration(wp)) {
            LegacyCalculatedCoordinateMigrator.performMigration(context, cache, wp, () -> startActivityEditWaypointInternal(context, cache, waypointId));
        } else {
            startActivityEditWaypointInternal(context, cache, waypointId);
        }
    }

    private static void startActivityEditWaypointInternal(final Context context, final Geocache cache, final int waypointId) {
        final Intent intent = new Intent(context, EditWaypointActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode())
                .putExtra(Intents.EXTRA_WAYPOINT_ID, waypointId);
        context.startActivity(intent);
    }

    public static void startActivityAddWaypoint(final Context context, final Geocache cache) {
        final Intent intent = new Intent(context, EditWaypointActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode());
        context.startActivity(intent);
    }

    public static void startActivityAddWaypoint(final Context context, final Geocache cache, final Geopoint initialCoords) {
        final Intent intent = new Intent(context, EditWaypointActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode())
                .putExtra(Intents.EXTRA_COORDS, initialCoords);
        context.startActivity(intent);
    }

    @Override
    public void finish() {
        //if this was a calculated waypoint, then reset variable state (in case of edit cancellation)
        cache.getVariables().reloadLastSavedState();

        Dialogs.dismiss(waitDialog);
        super.finish();
    }
}
