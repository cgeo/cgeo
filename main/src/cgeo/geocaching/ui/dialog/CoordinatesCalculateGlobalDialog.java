package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.CoordinatescalculateglobalDialogBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.CacheVariableList;
import cgeo.geocaching.models.CalculatedCoordinate;
import cgeo.geocaching.models.CalculatedCoordinateType;
import cgeo.geocaching.models.CoordinateInputData;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.VariableListView;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.formulas.VariableList;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.Arrays;
import java.util.Objects;

/**
 * Dialog to manage calculation of a coordinate
 */
public class CoordinatesCalculateGlobalDialog extends DialogFragment { // implements ClickCompleteCallback, LongClickCompleteCallback {

    private static final String ARG_INPUT_DATA = "arg_input_data";

    private String geocode;
    private CalculatedCoordinate calcCoord = new CalculatedCoordinate();
    private Geopoint geopoint;
    private String notes;
    private VariableList varList;
    private VariableListView.VariablesListAdapter varListAdapter;

    private CoordinatescalculateglobalDialogBinding binding;

    private final TextSpinner<CalculatedCoordinateType> displayType = new TextSpinner<>();

    // Interface used by the coordinate calculator to callback activity with dialog result.
    public interface CalculatedCoordinateUpdate {
        void updateCalculatedCoordinates(CoordinateInputData coordinateInputData);
    }

    private CoordinateInputData createFromDialog() {
        final CoordinateInputData cid = new CoordinateInputData();
        cid.setGeocode(geocode);
        cid.setNotes(binding.notesText.getText().toString());
        cid.setCalculatedCoordinate(calcCoord);
        cid.setGeopoint(calcCoord.calculateGeopoint(varList::getValue));
        return cid;
    }

    private void saveAndFinishDialog () {

        final Activity activity = requireActivity();
        if (activity instanceof CalculatedCoordinateUpdate) {
            ((CalculatedCoordinateUpdate) activity).updateCalculatedCoordinates(createFromDialog());
        }
        dismiss();
    }

    /**
     * Displays an instance of the calculator dialog
     */
    public static void show(final FragmentManager mgr, final CoordinateInputData initialState) {
        final CoordinatesCalculateGlobalDialog ccd = new CoordinatesCalculateGlobalDialog();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_INPUT_DATA, initialState);
        ccd.setArguments(args);
        ccd.setCancelable(true);
        ccd.show(mgr, "wpcalcglobaldialog");
    }

    private CoordinatesCalculateGlobalDialog() {
        //empty on purpose
    }

    private void initFromBundle(final Bundle data) {
        if (data == null || !data.containsKey(ARG_INPUT_DATA)) {
            return;
        }
        final CoordinateInputData inputData = data.getParcelable(ARG_INPUT_DATA);
        geopoint = inputData.getGeopoint();
        calcCoord = inputData.getCalculatedCoordinate();
        notes = inputData.getNotes();

        if (!calcCoord.isFilled()) {
            calcCoord.setFrom(geopoint);
        }

        //(re)load variable list as necessary
        final String oldGeocode = geocode;
        geocode = inputData.getGeocode();
        if (!Objects.equals(oldGeocode, geocode)) {
            if (geocode != null) {
                final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    this.varList = cache.getVariables();
                }
            }
            if (varList == null) {
                varList = new VariableList();
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initFromBundle(savedInstanceState);
        initFromBundle(getArguments());
        if (geopoint == null) {
            geopoint = Sensors.getInstance().currentGeo().getCoords();
        }
    }

    /**
     * Save the current state of the calculator such that it can be restored after screen rotation (or similar)
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_INPUT_DATA, createFromDialog());
        //outState.putStringArray(ARG_PREFILLED_VARS, varsAlwaysToKeep.toArray(new String[0]));
    }


    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        binding = CoordinatescalculateglobalDialogBinding.inflate(inflater, container, false);
        binding.NonPlainFormat.setVisibility(View.GONE);

        //handle dialog title
        final Dialog dialog = getDialog();
        final boolean noTitle = dialog != null && dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (!noTitle) {
            if (dialog != null) {
                dialog.setTitle(R.string.cache_coordinates);
            }
            binding.done.setOnClickListener(view -> saveAndFinishDialog());
        } else {
            final Toolbar toolbar = binding.toolbarWrapper.toolbar;
            toolbar.setTitle(R.string.cache_coordinates);
            toolbar.inflateMenu(R.menu.menu_ok_cancel);
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_item_save) {
                    saveAndFinishDialog();
                } else {
                    dismiss();
                }
                return true;
            });
            binding.done.setVisibility(View.GONE);
        }

        displayType.setSpinner(binding.spinnerCoordinateFormats)
            .setValues(Arrays.asList(CalculatedCoordinateType.values()))
            .setDisplayMapper(t -> t.toUserDisplayableString())
            .set(calcCoord.getType())
            .setChangeListener(t -> refreshType(false));
        refreshType(true);

        varListAdapter = binding.variableList.getAdapter();
        varListAdapter.setDisplay(VariableListView.DisplayType.MINIMALISTIC, 2);
        varListAdapter.setVarChangeCallback((v, s) -> {
            //varsAlwaysToKeep.add(v);
            updateView();
        });
        varListAdapter.setVariableList(varList);

        binding.variablesAddmissing.setOnClickListener(v -> {
            varListAdapter.addAllMissing();
            for (String var : calcCoord.getNeededVars()) {
                if (!varListAdapter.containsVariable(var)) {
                    varListAdapter.addVariable(var, "");
                }
            }
        });
        binding.variablesSort.setOnClickListener(v -> varListAdapter.sortVariables(TextUtils.COLLATOR::compare));

        binding.notesText.setText(notes);

        binding.PlainLat.setFilters(new InputFilter[] { new InputFilter.AllCaps() });
        binding.PlainLat.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            calcCoord.setLatitudePattern(s.toString());
            updateView();
        }));
        binding.PlainLat.setText(calcCoord.getLatitudePattern());

        binding.PlainLon.setFilters(new InputFilter[] { new InputFilter.AllCaps() });
        binding.PlainLon.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            calcCoord.setLongitudePattern(s.toString());
            updateView();
        }));
        binding.PlainLon.setText(calcCoord.getLongitudePattern());

        binding.NonPlainFormat.setChangeListener(p -> {
            calcCoord.setLatitudePattern(p.first);
            calcCoord.setLongitudePattern(p.second);
            updateView();
        });

        return binding.getRoot();
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) // splitting up that method would not help improve readability
    private void refreshType(final boolean initialLoad) {
        final CalculatedCoordinateType newType = displayType.get();
        if (!initialLoad && calcCoord.getType() == newType) {
            return;
        }
        calcCoord.setType(newType);

        Geopoint currentGp = null;
        if (calcCoord != null) {
            currentGp = calcCoord.calculateGeopoint(varList::getValue);
        }
        if (currentGp == null) {
            currentGp = geopoint;
        }
        binding.PlainFormat.setVisibility(newType == CalculatedCoordinateType.PLAIN ? View.VISIBLE : View.GONE);
        binding.NonPlainFormat.setVisibility(newType != CalculatedCoordinateType.PLAIN ? View.VISIBLE : View.GONE);

        if (newType == CalculatedCoordinateType.PLAIN) {
            final Pair<String, String> coords = binding.NonPlainFormat.getPlain();
            binding.PlainLat.setText(coords.first);
            binding.PlainLon.setText(coords.second);
        } else {
            binding.NonPlainFormat.setData(newType,
                initialLoad ? calcCoord.getLatitudePattern() : null, initialLoad ? calcCoord.getLongitudePattern() : null, currentGp);
        }

    }

    private void updateView() {
        //update the displayed coordinate texts
        binding.latRes.setText(calcCoord.getLatitudeString(varList::getValue));
        binding.lonRes.setText(calcCoord.getLongitudeString(varList::getValue));
    }

    @Override
    public void onStart() {
        super.onStart();
        //Make this dialog completely fill the screen
        final Dialog d = getDialog();
        if (d != null) {
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * This method displays a reminder about saving the calculator state then closes both this dialog as well as the 'CoordinateInputDialog'
     *
     * Note that clicking the back arrow on the device does not run this method so in that case the user will be returned to the 'CoordinateInputDialog'.
     */
    @Override
    public void dismiss() {
        ActivityMixin.showToast(this.getActivity(), R.string.warn_calculator_state_save);
        if (varList instanceof CacheVariableList) {
            ((CacheVariableList) varList).saveState();
        }
        super.dismiss();
    }
}
