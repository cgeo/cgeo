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
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.formulas.VariableList;
import static cgeo.geocaching.models.CalculatedCoordinateType.PLAIN;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

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

    private CoordinateInputData createFromDialog() {
        final CoordinateInputData cid = new CoordinateInputData();
        cid.setGeocode(geocode);
        cid.setNotes(binding.notesText.getText().toString());
        cid.setCalculatedCoordinate(calcCoord);
        cid.setGeopoint(calcCoord.calculateGeopoint(varList::getValue));
        return cid;
    }

    private void saveAndFinishDialog () {
        ActivityMixin.showToast(this.getActivity(), R.string.warn_calculator_state_save);

        final Activity activity = requireActivity();
        if (activity instanceof CoordinatesInputDialog.CoordinateUpdate) {
            ((CoordinatesInputDialog.CoordinateUpdate) activity).updateCoordinates(createFromDialog());
        }

        //save changes to the var list
        if (varList instanceof CacheVariableList) {
            ((CacheVariableList) varList).saveState();
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
        ccd.show(mgr, "wpcalcglobaldialog");
    }

    public CoordinatesCalculateGlobalDialog() {
        setCancelable(true);
    }

    private void initFromBundle(final Bundle data) {
        if (data == null || !data.containsKey(ARG_INPUT_DATA)) {
            return;
        }
        final CoordinateInputData inputData = data.getParcelable(ARG_INPUT_DATA);
        geopoint = inputData.getGeopoint();
        calcCoord = inputData.getCalculatedCoordinate();
        notes = inputData.getNotes();

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
        binding.ccSwitchGuided.setChecked(false);
        binding.ccGuidedFormat.setVisibility(View.GONE);

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
            toolbar.setTitle("Calculate Coordinate");
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

        binding.convertToPlain.setOnClickListener(v -> {
            final CoordinateInputData cid = createFromDialog();
            cid.setCalculatedCoordinate(null);
            CoordinatesInputDialog.show(this.requireActivity().getSupportFragmentManager(), cid);
            dismiss();
        });

        displayType.setSpinner(binding.ccGuidedFormat)
            .setValues(CollectionStream.of(CalculatedCoordinateType.values()).filter(t -> PLAIN != t).toList())
            .setDisplayMapper(CalculatedCoordinateType::toUserDisplayableString)
            .set(calcCoord.getType())
            .setChangeListener(t -> refreshType(t, false));

        varListAdapter = binding.variableList.getAdapter();
        varListAdapter.setDisplay(VariableListView.DisplayType.MINIMALISTIC, 2);
        varListAdapter.setVarChangeCallback((v, s) -> {
            checkAddVariables(Collections.singletonList(v));
            updateView();
        });
        varListAdapter.setVariableList(varList);
        varListAdapter.setVisibleVariablesAndDependent(calcCoord.getNeededVars());

        binding.variablesTidyup.setOnClickListener(v -> varListAdapter.tidyUp(calcCoord.getNeededVars()));

        binding.notesText.setText(notes);

        binding.PlainLat.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            calcCoord.setLatitudePattern(s.toString());
            checkAddVariables(calcCoord.getNeededVars());
            updateView();
        }));
        binding.PlainLat.setText(calcCoord.getLatitudePattern());

        binding.PlainLon.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            calcCoord.setLongitudePattern(s.toString());
            checkAddVariables(calcCoord.getNeededVars());
            updateView();
        }));
        binding.PlainLon.setText(calcCoord.getLongitudePattern());

        binding.NonPlainFormat.setChangeListener(p -> {
            calcCoord.setLatitudePattern(p.first);
            calcCoord.setLongitudePattern(p.second);
            checkAddVariables(calcCoord.getNeededVars());
            updateView();
        });

        binding.ccSwitchGuided.setOnCheckedChangeListener((v, c) -> {
            if (!c) {
                refreshType(PLAIN, false);
            } else {
                final CalculatedCoordinateType guessType = binding.NonPlainFormat.guessType(calcCoord.getLatitudePattern(), calcCoord.getLongitudePattern());
                refreshType(guessType == null ? displayType.get() : guessType, false);
            }
        });

        binding.ccRemoveSpaces.setOnClickListener(v -> {
            binding.PlainLon.setText(binding.PlainLon.getText().toString().replaceAll(" ", ""));
            binding.PlainLat.setText(binding.PlainLat.getText().toString().replaceAll(" ", ""));
        });

        refreshType(calcCoord.getType(), true);

        return binding.getRoot();
    }

    private void checkAddVariables(final Collection<String> vars) {
        final Set<String> neededVars = varListAdapter.getVariables().getDependentVariables(vars);
        varListAdapter.ensureVariables(neededVars);
        varListAdapter.addVisibleVariables(neededVars);
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) // splitting up that method would not help improve readability
    private void refreshType(final CalculatedCoordinateType newType, final boolean initialLoad) {
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
        binding.PlainFormat.setVisibility(newType == PLAIN ? View.VISIBLE : View.GONE);
        binding.NonPlainFormat.setVisibility(newType != PLAIN ? View.VISIBLE : View.GONE);
        if (newType == PLAIN) {
            binding.NonPlainFormat.unmarkButtons();
        }
        binding.ccGuidedFormat.setVisibility(newType == PLAIN ? View.GONE : View.VISIBLE);
        binding.ccSwitchGuided.setChecked(newType != PLAIN);
        if (newType != PLAIN) {
            displayType.set(newType);
        }
        binding.ccRemoveSpaces.setVisibility(newType != PLAIN ? View.GONE : View.VISIBLE);

        if (newType == PLAIN) {
            if (initialLoad) {
                binding.PlainLat.setText(calcCoord.getLatitudePattern() == null ? "" : calcCoord.getLatitudePattern());
                binding.PlainLon.setText(calcCoord.getLongitudePattern() == null ? "" : calcCoord.getLongitudePattern());
            } else {
                final Pair<String, String> coords = binding.NonPlainFormat.getPlain();
                binding.PlainLat.setText(coords.first);
                binding.PlainLon.setText(coords.second);
            }
        } else {
            binding.NonPlainFormat.setData(newType, calcCoord.getLatitudePattern(),  calcCoord.getLongitudePattern(), currentGp);
        }
    }

    private void updateView() {
        //update the displayed coordinate texts
        final Pair<Double, Double> gpData = calcCoord.calculateGeopointData(varList::getValue);
        binding.latRes.setText(TextUtils.concat(calcCoord.getLatitudeString(varList::getValue), " ", getStatusText(gpData.first != null)));
        binding.lonRes.setText(TextUtils.concat(calcCoord.getLongitudeString(varList::getValue), " ", getStatusText(gpData.second != null)));
    }

    private CharSequence getStatusText(final boolean positive) {
        if (positive) {
            return TextUtils.setSpan("✓", new ForegroundColorSpan(Color.GREEN));
        }
        return TextUtils.setSpan("✖", new ForegroundColorSpan(Color.RED));
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

}
