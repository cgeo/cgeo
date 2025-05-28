package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.databinding.EditwaypointActivityBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.ProjectionType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.DistanceUnit;
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
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.FormulaEditText;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.VariableListView;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.NewCoordinateInputDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.formulas.Formula;
import cgeo.geocaching.utils.formulas.VariableList;
import cgeo.geocaching.utils.html.UnknownTagsHandler;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;


public class EditWaypointActivity extends AbstractActionBarActivity implements NewCoordinateInputDialog.CoordinateUpdate {

    public static final int SUCCESS = 0;
    public static final int UPLOAD_START = 1;
    public static final int UPLOAD_ERROR = 2;
    public static final int UPLOAD_NOT_POSSIBLE = 3;
    public static final int UPLOAD_SUCCESS = 4;
    public static final int SAVE_ERROR = 5;


    private static final String STATE_CURRENT_COORD = "current_coord";
    private static final String STATE_PREPROJECTED_COORD = "preprojected_coord";

    private static final String STATE_CALC_STATE_JSON = "calc_state_json";
    private static final String STATE_LAST_SELECTED_WP_TYPE = "wp_type_selector_pos";
    private static final ArrayList<WaypointType> POSSIBLE_WAYPOINT_TYPES = new ArrayList<>(WaypointType.ALL_TYPES_EXCEPT_OWN_ORIGINAL_AND_GENERATED);
    private static final ArrayList<WaypointType> POSSIBLE_WAYPOINT_TYPES_WITH_GENERATED = new ArrayList<>(WaypointType.ALL_TYPES_EXCEPT_OWN_AND_ORIGINAL);

    private String geocode = null;
    private int waypointId = -1;

    private WaypointType lastSelectedWaypointType = null;

    private ProgressDialog waitDialog = null;
    private Waypoint waypoint = null;
    private String prefix = "";
    private String lookup = "---";
    private boolean own = true;
    private boolean originalCoordsEmpty = false;

    private Geopoint preprojectedCoords = null;
    private Geopoint currentCoords = null;

    /**
     * {@code true} if the activity is newly created, {@code false} if it is restored from an instance state
     */
    private boolean initViews = true;
    private EditwaypointActivityBinding binding;
    private VariableListView.VariablesListAdapter varListAdapter;

    /**
     * This is the cache that the waypoint belongs to.
     */
    private Geocache cache;
    /**
     * State the Coordinate Calculator was last left in.
     */
    private String calcStateString;

    private final Handler loadWaypointHandler = new LoadWaypointHandler(this);

    private final TextSpinner<WaypointType> waypointType = new TextSpinner<>();
    private final TextSpinner<ProjectionType> projectionType = new TextSpinner<>();
    private final TextSpinner<DistanceUnit> projectionBearingUnit = new TextSpinner<>();

    private static final class LoadWaypointHandler extends WeakReferenceHandler<EditWaypointActivity> {
        LoadWaypointHandler(final EditWaypointActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
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

                    //projection
                    activity.projectionType.set(waypoint.getProjectionType());
                    final ImmutableTriple<FormulaEditText, FormulaEditText, TextSpinner<DistanceUnit>> projectionFields =
                        getFieldsForProjectionType(activity);
                    if (projectionFields.left != null) {
                        projectionFields.left.setFormulaText(waypoint.getProjectionFormula1());
                    }
                    if (projectionFields.middle != null) {
                        projectionFields.middle.setFormulaText(waypoint.getProjectionFormula2());
                    }
                    if (projectionFields.right != null) {
                        projectionFields.right.set(waypoint.getProjectionDistanceUnit());
                    }

                    if (activity.initViews) {
                        activity.binding.wptVisitedCheckbox.setChecked(waypoint.isVisited());
                        activity.updateCoordinates(waypoint.getPreprojectedCoords());
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
                }

                if (StringUtils.isBlank(activity.binding.note.getText())) {
                    activity.binding.noteLayout.setVisibility(View.GONE);
                }
                activity.recalculateProjectionView();

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
            preprojectedCoords = extras.getParcelable(Intents.EXTRA_COORDS);
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
            calcStateString = savedInstanceState.getString(STATE_CALC_STATE_JSON);
            lastSelectedWaypointType = CommonUtils.intToEnum(WaypointType.class, savedInstanceState.getInt(STATE_LAST_SELECTED_WP_TYPE));
            currentCoords = savedInstanceState.getParcelable(STATE_CURRENT_COORD);
            preprojectedCoords = savedInstanceState.getParcelable(STATE_PREPROJECTED_COORD);
        } else {
            calcStateString = null;
        }

        if (geocode != null) {
            cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            setCoordsModificationVisibility(ConnectorFactory.getConnector(geocode));
        }

        initializeProjectionView(cache);
        recalculateProjectionView();

        if (waypointId > 0) { // existing waypoint
            waitDialog = ProgressDialog.show(this, null, res.getString(R.string.waypoint_loading), true);
            waitDialog.setCancelable(true);
            (new LoadWaypointThread()).start();
        } else { // new waypoint
            initializeWaypointTypeSelector(true);
            binding.noteLayout.setVisibility(View.GONE);
            updateCoordinates(preprojectedCoords);
        }

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
        waypointType
            .setValues(allowedTypes)
            .setSpinner(binding.type)
            .setDisplayMapper(wpt -> TextParam.text(wpt.getL10n())
                .setImage(ImageParam.drawable(MapMarkerUtils.getWaypointTypeMarker(getResources(), wpt)), 30));

        waypointType.setChangeListener(wpt -> {
            final String oldDefaultName = lastSelectedWaypointType == null ? StringUtils.EMPTY : getDefaultWaypointName(cache, (lastSelectedWaypointType));
            lastSelectedWaypointType = wpt;
            final String currentName = binding.name.getText().toString().trim();
            if (StringUtils.isBlank(currentName) || oldDefaultName.equals(currentName)) {
                binding.name.setText(getDefaultWaypointName(cache, getSelectedWaypointType()));
            }
        });
        waypointType.set(getDefaultWaypointType());

        binding.type.setVisibility(View.VISIBLE);
    }

    private WaypointType getDefaultWaypointType() {
        // potentially restore saved instance state
        if (lastSelectedWaypointType != null) {
            return lastSelectedWaypointType;
        }

        // when editing, use the existing type
        if (waypoint != null && waypoint.getWaypointType() != null) {
            return waypoint.getWaypointType();
        }

        // make default for new waypoint depend on cache type
        switch (cache.getType()) {
            case MYSTERY:
                return WaypointType.FINAL;
            case MULTI:
                return WaypointType.STAGE;
            default:
                return WaypointType.WAYPOINT;
        }
    }

    private Geopoint calculateProjection(final Geopoint coords) {
        final ProjectionType pt = projectionType.get();
        final ImmutableTriple<FormulaEditText, FormulaEditText, TextSpinner<DistanceUnit>> fields = getFieldsForProjectionType();

        return pt.project(coords,
            fields.left == null ? null : fields.left.getValueAsDouble(),
            fields.middle == null ? null : fields.middle.getValueAsDouble(),
            fields.right == null ? null : fields.right.get());
    }

    private ImmutableTriple<FormulaEditText, FormulaEditText, TextSpinner<DistanceUnit>> getFieldsForProjectionType() {
        return getFieldsForProjectionType(this);
    }

    public static ImmutableTriple<FormulaEditText, FormulaEditText, TextSpinner<DistanceUnit>> getFieldsForProjectionType(final EditWaypointActivity activity) {
        final ProjectionType pt = activity.projectionType.get();
        final EditwaypointActivityBinding binding = activity.binding;
        switch (pt) {
            case BEARING:
                return ImmutableTriple.of(binding.projectionBearingDistance, binding.projectionBearingAngle, activity.projectionBearingUnit);
            case OFFSET:
                return ImmutableTriple.of(binding.projectionOffsetLatitude, binding.projectionOffsetLongitude, null);
            case NO_PROJECTION:
            default:
                return ImmutableTriple.of(null, null, null);
        }
    }

    private Set<String> getNeededVariablesForProjection() {
        final Set<String> projectionVars = new HashSet<>();

        final ImmutableTriple<FormulaEditText, FormulaEditText, TextSpinner<DistanceUnit>> fields = getFieldsForProjectionType();
        if (fields.left != null) {
            fields.left.addNeededVariables(projectionVars);
        }
        if (fields.middle != null) {
            fields.middle.addNeededVariables(projectionVars);
        }

        return projectionVars;
    }

    private void initializeProjectionView(final Geocache cache) {

        //connect formula-editfields with cache's Variable list
        final VariableList varList = cache.getVariables();
        final BiConsumer<String, Formula> listener = (s, f) -> {
            varListAdapter.checkAddVisibleVariables(getNeededVariablesForProjection());
            recalculateProjectedCoordinates();
        };
        binding.projectionBearingAngle.setVariableList(varList);
        binding.projectionBearingAngle.setFormulaChangeListener(listener);
        binding.projectionBearingDistance.setVariableList(varList);
        binding.projectionBearingDistance.setFormulaChangeListener(listener);
        binding.projectionOffsetLatitude.setVariableList(varList);
        binding.projectionOffsetLatitude.setFormulaChangeListener(listener);
        binding.projectionOffsetLongitude.setVariableList(varList);
        binding.projectionOffsetLongitude.setFormulaChangeListener(listener);

        //initialize spinners
        projectionType.setValues(Arrays.asList(ProjectionType.values()))
            .setDisplayMapper(pt -> TextParam.text(pt.getL10n()).setImage(ImageParam.id(pt.markerId)))
            .setSpinner(binding.projectionType)
            .setChangeListener(pt -> recalculateProjectionView());
        projectionBearingUnit.setValues(Arrays.asList(DistanceUnit.values()))
            .setDisplayMapper(pt -> TextParam.text(pt.getId()))
            .setSpinner(binding.projectionBearingUnit)
            .setChangeListener(pt -> recalculateProjectionView());

        varListAdapter = binding.variableList.getAdapter();
        varListAdapter.setDisplay(VariableListView.DisplayType.MINIMALISTIC, 2);
        varListAdapter.setVarChangeCallback((v, s) -> {
            varListAdapter.checkAddVisibleVariables(Collections.singletonList(v));
            recalculateProjectedCoordinates();
        });

        varListAdapter.setVariableList(varList);
        varListAdapter.setVisibleVariablesAndDependent(getNeededVariablesForProjection());

        binding.variablesTidyup.setOnClickListener(v -> varListAdapter.tidyUp(getNeededVariablesForProjection()));
    }

    /**
     * this method assumes a correctly filled "preprojectedCoord"-var.
     * It recalculates the "currentCoord" value and updates view according to projection settings
     */
    @SuppressWarnings("PMD.NPathComplexity") // readability won't be imporved upon split
    private void recalculateProjectionView() {

        final ProjectionType pType = this.projectionType.get();
        final boolean projectionEnabled = waypoint == null || (waypoint.isUserDefined() || waypoint.isOriginalCoordsEmpty());

        //update view visibilities
        binding.projectionType.setVisibility(projectionEnabled ? View.VISIBLE : View.GONE);
        binding.projectedLatLongitude.setVisibility(projectionEnabled && pType != ProjectionType.NO_PROJECTION ? View.VISIBLE : View.GONE);
        binding.projectionBearingBox.setVisibility(projectionEnabled && pType == ProjectionType.BEARING ? View.VISIBLE : View.GONE);
        binding.projectionOffsetBox.setVisibility(projectionEnabled && pType == ProjectionType.OFFSET ? View.VISIBLE : View.GONE);

        binding.variableList.setVisibility(projectionEnabled && pType != ProjectionType.NO_PROJECTION ? View.VISIBLE : View.GONE);
        binding.variablesTidyup.setVisibility(projectionEnabled && pType != ProjectionType.NO_PROJECTION ? View.VISIBLE : View.GONE);

        //update currentCoords and coordinate Views
        recalculateProjectedCoordinates();
    }

    private void recalculateProjectedCoordinates() {
        //update currentCoords and coordinate Views
        final Geopoint base = this.preprojectedCoords;

        final ProjectionType pType = this.projectionType.get();
        final boolean projectionEnabled = waypoint == null || (waypoint.isUserDefined() || waypoint.isOriginalCoordsEmpty());

        if (!projectionEnabled || pType == ProjectionType.NO_PROJECTION || base == null) {
            this.currentCoords = base;
        } else {
            this.currentCoords = calculateProjection(base);
        }
        ViewUtils.setCoordinates(base, binding.buttonLatLongitude);
        ViewUtils.setCoordinates(this.currentCoords, binding.projectedLatLongitude);
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

                final boolean resetFromOriginal = (WaypointType.ORIGINAL == waypoint.getWaypointType());
                binding.modifyCacheCoordinatesLocal.setText(resetFromOriginal ? R.string.waypoint_localy_reset_cache_coords : R.string.waypoint_set_as_cache_coords);
                binding.modifyCacheCoordinatesLocalAndRemote.setText(resetFromOriginal ? R.string.waypoint_reset_local_and_remote_cache_coords : R.string.waypoint_save_and_modify_on_website);

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
            final Geopoint geopoint = preprojectedCoords;
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
                        CacheDetailActivity.saveAndNotify(EditWaypointActivity.this, cache);
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

            NewCoordinateInputDialog.show(EditWaypointActivity.this, this::onCoordinatesUpdated, cid);
        }
        private void onCoordinatesUpdated(@Nullable final Geopoint gp) {
            // Arrives here from the new coordinate dialog either for a standard waypoint
            // or a calculated one that has been converted to plain coordinates, in which case we need to clear the state
            calcStateString = null;
            updateCoordinates(gp);
        }
    }
    @Override
    public void updateCoordinates(@Nullable final Geopoint gp) {
        //this method is supposed to update the "base" / "preprojected" coordinate
        if (!Objects.equals(preprojectedCoords, gp)) {
            preprojectedCoords = gp;
            recalculateProjectionView();
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

    /**
     * Save the current state of the calculator such that it can be restored after screen rotation (or similar)
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CALC_STATE_JSON, calcStateString);
        outState.putInt(STATE_LAST_SELECTED_WP_TYPE, CommonUtils.enumToInt(lastSelectedWaypointType));
        outState.putParcelable(STATE_CURRENT_COORD, currentCoords);
        outState.putParcelable(STATE_PREPROJECTED_COORD, preprojectedCoords);

    }

    private WaypointType getSelectedWaypointType() {
        final WaypointType selectedType = waypointType.get();
        return selectedType != null ? selectedType : waypoint.getWaypointType();
    }

    private void finishConfirmDiscard() {
        final ActivityData currentState = getActivityData();

        if (currentState != null && isWaypointChanged(currentState)) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_wp_changes).confirm(this::finish);
        } else {
            finish();
        }
    }

    public EditText getUserNotes() {
        return binding.userNote;
    }

    @Nullable
    private ActivityData getActivityData() {

        final ActivityData currentState = new ActivityData();

        //projection
        currentState.preprojectedCoords = this.preprojectedCoords;
        currentState.coords = this.currentCoords;
        currentState.projectionType = this.projectionType.get();

        //projection settings
        final ImmutableTriple<FormulaEditText, FormulaEditText, TextSpinner<DistanceUnit>> fields = getFieldsForProjectionType();
        currentState.projectionFormula1 = fields.left == null ? null : fields.left.getFormulaText();
        currentState.projectionFormula2 = fields.middle == null ? null : fields.middle.getFormulaText();
        currentState.projectionUnits = fields.right == null ? DistanceUnit.getDefaultUnit(false) : fields.right.get();

        final String givenName = binding.name.getText().toString().trim();
        currentState.name = StringUtils.defaultIfBlank(givenName, getDefaultWaypointName(cache, getSelectedWaypointType()));
        if (own) {
            currentState.noteText = "";
        } else { // keep original note
            currentState.noteText = waypoint.getNote();
        }
        currentState.userNoteText = ViewUtils.getEditableText(binding.userNote.getText()).trim();
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
            || !StringUtils.equals(currentState.calcStateJson, waypoint.getCalcStateConfig())
            || !Geopoint.equalsFormatted(currentState.preprojectedCoords, waypoint.getPreprojectedCoords(), GeopointFormatter.Format.LAT_LON_DECMINUTE)
            || currentState.projectionType != waypoint.getProjectionType()
            || currentState.projectionUnits != waypoint.getProjectionDistanceUnit()
            || !StringUtils.equals(currentState.projectionFormula1, waypoint.getProjectionFormula1())
            || !StringUtils.equals(currentState.projectionFormula2, waypoint.getProjectionFormula2());
}

    private static class FinishWaypointSaveHandler extends WeakReferenceHandler<EditWaypointActivity> {

        private final Geopoint coords;

        FinishWaypointSaveHandler(final EditWaypointActivity activity, final Geopoint coords) {
            super(activity);
            this.coords = coords != null ? new Geopoint(coords.getLatitude(), coords.getLongitude()) : null;
        }

        @Override
        public void handleMessage(@NonNull final Message msg) {
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
        waypoint.setCoordsPure(currentState.coords);
        waypoint.setPreprojectedCoords(currentState.preprojectedCoords);
        waypoint.setNote(currentState.noteText);
        waypoint.setUserNote(currentState.userNoteText);
        waypoint.setVisited(currentState.visited);
        waypoint.setId(waypointId);
        waypoint.setOriginalCoordsEmpty(originalCoordsEmpty);
        waypoint.setCalcStateConfig(currentState.calcStateJson);
        waypoint.setProjection(currentState.projectionType, currentState.projectionUnits,
                currentState.projectionFormula1, currentState.projectionFormula2);

        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_WAYPOINTS);
        if (cache == null) {
            finishHandler.sendEmptyMessage(SAVE_ERROR);
            return;
        }
        final String oldAllUserNotes = cache.getAllUserNotes();
        if (cache.addOrChangeWaypoint(waypoint, true)) {
            cache.addCacheArtefactsFromNotes(oldAllUserNotes);
            boolean deleteModifiedOnline = false;
            if (waypoint.getCoords() != null && (binding.modifyCacheCoordinatesLocal.isChecked() || binding.modifyCacheCoordinatesLocalAndRemote.isChecked())) {
                if (waypoint.getWaypointType() == WaypointType.ORIGINAL) {
                    deleteModifiedOnline = true;
                    cache.resetUserModifiedCoords(waypoint);
                } else {
                    if (!cache.hasUserModifiedCoords()) {
                        cache.createOriginalWaypoint(cache.getCoords());
                    }
                    cache.setCoords(waypoint.getCoords());
                    DataStore.saveUserModifiedCoords(cache);
                }
            }
            if (waypoint.getCoords() != null && binding.modifyCacheCoordinatesLocalAndRemote.isChecked()) {
                finishHandler.sendEmptyMessage(UPLOAD_START);

                if (cache.supportsOwnCoordinates()) {
                    final boolean result = deleteModifiedOnline ? deleteModifiedCoords(cache) : uploadModifiedCoords(cache, waypoint.getCoords());
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
        public Geopoint preprojectedCoords;
        public String noteText;
        public String userNoteText;
        public boolean visited;
        public String calcStateJson;
        public ProjectionType projectionType;
        public DistanceUnit projectionUnits;
        public String projectionFormula1;
        public String projectionFormula2;
    }

    private static boolean uploadModifiedCoords(final Geocache cache, final Geopoint waypointUploaded) {
        final IConnector con = ConnectorFactory.getConnector(cache);
        return con.supportsOwnCoordinates() && con.uploadModifiedCoordinates(cache, waypointUploaded);
    }

    private static boolean deleteModifiedCoords(final Geocache cache) {
        final IConnector con = ConnectorFactory.getConnector(cache);
        return con.supportsOwnCoordinates() && con.deleteModifiedCoordinates(cache);
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
                .putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode())
                .putExtra(Intents.EXTRA_WAYPOINT_ID, Waypoint.NEW_ID);
        context.startActivity(intent);
    }

    public static void startActivityAddWaypoint(final Context context, final Geocache cache, final Geopoint initialCoords) {
        final Intent intent = new Intent(context, EditWaypointActivity.class)
                .putExtra(Intents.EXTRA_GEOCODE, cache.getGeocode())
                .putExtra(Intents.EXTRA_WAYPOINT_ID, Waypoint.NEW_ID)
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
