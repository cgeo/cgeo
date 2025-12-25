// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui.dialog

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.databinding.CoordinatescalculateglobalDialogBinding
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.maps.DefaultMap
import cgeo.geocaching.models.CacheVariableList
import cgeo.geocaching.models.CalculatedCoordinate
import cgeo.geocaching.models.CalculatedCoordinateType
import cgeo.geocaching.models.CoordinateInputData
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.sensors.LocationDataProvider
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.CalculatedCoordinateInputGuideView
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.TextSpinner
import cgeo.geocaching.ui.VariableListView
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.ClipboardUtils
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.formulas.DegreeFormula
import cgeo.geocaching.utils.formulas.FormulaUtils
import cgeo.geocaching.utils.formulas.VariableList
import cgeo.geocaching.utils.formulas.VariableMap
import cgeo.geocaching.models.CalculatedCoordinateType.PLAIN

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window

import androidx.annotation.NonNull
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.EnumSet
import java.util.Iterator
import java.util.List
import java.util.Objects
import java.util.Set

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutableTriple

/**
 * Dialog to manage calculation of a coordinate
 */
class CoordinatesCalculateGlobalDialog : DialogFragment() {

    private static val ARG_INPUT_DATA: String = "arg_input_data"

    private String geocode
    private var calcCoord: CalculatedCoordinate = CalculatedCoordinate()
    private Geopoint geopoint
    private String notes
    private VariableList varList
    private VariableListView.VariablesListAdapter varListAdapter

    private static DialogCallback callback
    private CoordinatescalculateglobalDialogBinding binding

    private val displayType: TextSpinner<CalculatedCoordinateType> = TextSpinner<>()

    private CoordinateInputData createFromDialog() {
        val cid: CoordinateInputData = CoordinateInputData()
        cid.setGeocode(geocode)
        cid.setNotes(ViewUtils.getEditableText(binding.notesText.getText()))
        cid.setCalculatedCoordinate(calcCoord)
        cid.setGeopoint(calcCoord.calculateGeopoint(varList::getValue))
        return cid
    }

    private Unit saveAndFinishDialog() {
        ActivityMixin.showToast(this.getActivity(), R.string.warn_calculator_state_save)

        val activity: Activity = requireActivity()
        if (activity is CoordinateInputDialog.CoordinateUpdate) {
            ((CoordinateInputDialog.CoordinateUpdate) activity).updateCoordinates(createFromDialog())
        }

        //save changes to the var list
        if (varList is CacheVariableList) {
            ((CacheVariableList) varList).saveState()
        }
        dismiss()
    }

    /**
     * Displays an instance of the calculator dialog
     */
    public static Unit show(final FragmentManager mgr, final DialogCallback callbackMethod, final CoordinateInputData initialState) {
        callback = callbackMethod
        val ccd: CoordinatesCalculateGlobalDialog = CoordinatesCalculateGlobalDialog()
        val args: Bundle = Bundle()
        args.putParcelable(ARG_INPUT_DATA, initialState)
        ccd.setArguments(args)
        ccd.show(mgr, "wpcalcglobaldialog")
    }

    public CoordinatesCalculateGlobalDialog() {
        setCancelable(true)
    }

    private Unit initFromBundle(final Bundle data) {
        if (data == null || !data.containsKey(ARG_INPUT_DATA)) {
            return
        }
        val inputData: CoordinateInputData = data.getParcelable(ARG_INPUT_DATA)
        geopoint = inputData.getGeopoint()
        calcCoord = inputData.getCalculatedCoordinate()
        notes = inputData.getNotes()

        //(re)load variable list as necessary
        val oldGeocode: String = geocode
        geocode = inputData.getGeocode()
        if (!Objects == (oldGeocode, geocode)) {
            if (!StringUtils.isBlank(geocode)) {
                val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
                if (cache != null) {
                    this.varList = cache.getVariables()
                }
            }
        }
        if (varList == null) {
            varList = VariableList()
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        initFromBundle(savedInstanceState)
        initFromBundle(getArguments())
        if (geopoint == null) {
            geopoint = LocationDataProvider.getInstance().currentGeo().getCoords()
        }
    }

    /**
     * Save the current state of the calculator such that it can be restored after screen rotation (or similar)
     */
    override     public Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_INPUT_DATA, createFromDialog())
    }


    override     public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        binding = CoordinatescalculateglobalDialogBinding.inflate(inflater, container, false)
        binding.NonPlainFormat.setVisibility(View.GONE)
        binding.ccSwitchGuided.setChecked(false)
        binding.ccGuidedFormat.setVisibility(View.GONE)

        //handle dialog title
        val dialog: Dialog = getDialog()
        val noTitle: Boolean = dialog != null && dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        if (!noTitle) {
            if (dialog != null) {
                dialog.setTitle(R.string.cache_coordinates)
            }
            binding.done.setOnClickListener(view -> saveAndFinishDialog())
        } else {
            val toolbar: Toolbar = binding.toolbarWrapper.toolbar
            toolbar.setTitle(R.string.waypoint_calculated_coordinates_global)
            toolbar.inflateMenu(R.menu.menu_ok_cancel)
            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_item_save) {
                    saveAndFinishDialog()
                } else {
                    dismiss()
                }
                return true
            })
            binding.done.setVisibility(View.GONE)
        }

        binding.convertToPlain.setOnClickListener(v -> {
            // When the callback is hit it will clear the calculator state associated with the waypoint
            CoordinateInputDialog.showSimple(this.requireActivity(), callback, createFromDialog())
            dismiss()
        })

        binding.generateRangeCoordinates.setOnClickListener(v -> generateRangeCoordinates())

        displayType.setSpinner(binding.ccGuidedFormat)
                .setValues(CollectionStream.of(CalculatedCoordinateType.values()).filter(t -> PLAIN != t).toList())
                .setDisplayMapperPure(CalculatedCoordinateType::toUserDisplayableString)
                .set(calcCoord.getType())
                .setChangeListener(t -> refreshType(t, false))

        varListAdapter = binding.variableList.getAdapter()
        varListAdapter.setDisplay(VariableListView.DisplayType.MINIMALISTIC, 2)
        varListAdapter.setVarChangeCallback((v, s) -> {
            varListAdapter.checkAddVisibleVariables(Collections.singletonList(v))
            updateView()
        })
        varListAdapter.setVariableList(varList)
        varListAdapter.setVisibleVariablesAndDependent(calcCoord.getNeededVars())

        binding.variablesTidyup.setOnClickListener(v -> varListAdapter.tidyUp(calcCoord.getNeededVars()))

        binding.notesText.setText(notes)

        binding.PlainLat.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            calcCoord.setLatitudePattern(s.toString())
            varListAdapter.checkAddVisibleVariables(calcCoord.getNeededVars())
            updateView()
        }))
        binding.PlainLat.setText(calcCoord.getLatitudePattern())

        binding.PlainLon.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            calcCoord.setLongitudePattern(s.toString())
            varListAdapter.checkAddVisibleVariables(calcCoord.getNeededVars())
            updateView()
        }))
        binding.PlainLon.setText(calcCoord.getLongitudePattern())

        binding.NonPlainFormat.setChangeListener(p -> {
            calcCoord.setLatitudePattern(p.first)
            calcCoord.setLongitudePattern(p.second)
            varListAdapter.checkAddVisibleVariables(calcCoord.getNeededVars())
            updateView()
        })

        binding.ccSwitchGuided.setOnCheckedChangeListener((v, c) -> {
            Settings.putBoolean(R.string.pref_preferGuidedCoordFormulaInput, c)
            if (!c) {
                refreshType(PLAIN, false)
            } else {
                val guessType: CalculatedCoordinateType = CalculatedCoordinateInputGuideView.guessType(calcCoord.getLatitudePattern(), calcCoord.getLongitudePattern())
                refreshType(guessType == null ? displayType.get() : guessType, false)
            }
        })

        binding.ccPaste.setOnClickListener(v -> {
            val clip: String = ClipboardUtils.getText()
            final List<Pair<String, String>> patterns = FormulaUtils.scanForCoordinates(Collections.singleton(clip), null)
            if (patterns.isEmpty()) {
                ActivityMixin.showShortToast(this.getActivity(), R.string.variables_scanlisting_nopatternfound)
            } else {
                binding.PlainLat.setText(patterns.get(0).first)
                binding.PlainLon.setText(patterns.get(0).second)
            }
        })

        binding.ccPlainTools.setOnClickListener(v -> {
            val options: List<Integer> = List.of(R.string.calccoord_remove_spaces, R.string.calccoord_replace_x_with_multiplication_symbol)
            final SimpleDialog.ItemSelectModel<Integer> model = SimpleDialog.ItemSelectModel<>()
            model
                .setItems(options)
                .setDisplayMapper(TextParam::id)
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN)

            SimpleDialog.of(this.getActivity()).setTitle(R.string.calccoord_plain_tools_title)
                    .selectSingle(model, (o) -> {
                        if (o == R.string.calccoord_remove_spaces) {
                            binding.PlainLat.setText(DegreeFormula.removeSpaces(ViewUtils.getEditableText(binding.PlainLat.getText())))
                            binding.PlainLon.setText(DegreeFormula.removeSpaces(ViewUtils.getEditableText(binding.PlainLon.getText())))
                        } else if (o == R.string.calccoord_replace_x_with_multiplication_symbol) {
                            binding.PlainLat.setText(DegreeFormula.replaceXWithMultiplicationSign(ViewUtils.getEditableText(binding.PlainLat.getText())))
                            binding.PlainLon.setText(DegreeFormula.replaceXWithMultiplicationSign(ViewUtils.getEditableText(binding.PlainLon.getText())))
                        }
                    })
        })

        //check if type from config is applicable
        if (calcCoord.getType() != PLAIN) {
            val type: CalculatedCoordinateType = CalculatedCoordinateInputGuideView.guessType(calcCoord.getLatitudePattern(), calcCoord.getLongitudePattern())
            calcCoord.setType(type == null ? PLAIN : type)
        }

        refreshType(calcCoord.getType(), true)

        return binding.getRoot()
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private Unit refreshType(final CalculatedCoordinateType newType, final Boolean initialLoad) {
        calcCoord.setType(newType)
        val isGuidedMode: Boolean = newType != PLAIN && Settings.getBoolean(R.string.pref_preferGuidedCoordFormulaInput, true)

        Geopoint currentGp = null
        if (calcCoord != null) {
            currentGp = calcCoord.calculateGeopoint(varList::getValue)
        }
        if (currentGp == null) {
            currentGp = geopoint
        }
        binding.PlainFormat.setVisibility(!isGuidedMode ? View.VISIBLE : View.GONE)
        binding.NonPlainFormat.setVisibility(isGuidedMode ? View.VISIBLE : View.GONE)
        if (!isGuidedMode) {
            binding.NonPlainFormat.unmarkButtons()
        }
        binding.ccGuidedFormat.setVisibility(!isGuidedMode ? View.GONE : View.VISIBLE)
        binding.ccSwitchGuided.setChecked(isGuidedMode)
        if (isGuidedMode) {
            displayType.set(newType)
        }
        binding.ccPlainTools.setVisibility(isGuidedMode ? View.GONE : View.VISIBLE)

        binding.ccPaste.setVisibility(isGuidedMode ? View.GONE : View.VISIBLE)
        binding.ccPaste.setEnabled(!FormulaUtils.scanForCoordinates(Collections.singleton(ClipboardUtils.getText()), null).isEmpty())

        if (!isGuidedMode) {
            if (initialLoad) {
                binding.PlainLat.setText(calcCoord.getLatitudePattern() == null ? "" : calcCoord.getLatitudePattern())
                binding.PlainLon.setText(calcCoord.getLongitudePattern() == null ? "" : calcCoord.getLongitudePattern())
            } else {
                val coords: Pair<String, String> = binding.NonPlainFormat.getPlain()
                binding.PlainLat.setText(coords.first)
                binding.PlainLon.setText(coords.second)
            }
        } else {
            binding.NonPlainFormat.setData(newType, calcCoord.getLatitudePattern(), calcCoord.getLongitudePattern(), currentGp)
        }
    }

    private Unit updateView() {
        //update the displayed coordinate texts
        val latData: ImmutableTriple<Double, CharSequence, Boolean> = calcCoord.calculateLatitudeData(varList::getValue)
        val lonData: ImmutableTriple<Double, CharSequence, Boolean> = calcCoord.calculateLongitudeData(varList::getValue)
        binding.latRes.setText(TextUtils.concat(latData.middle, " ", getStatusText(latData.left == null, latData.right)))
        binding.lonRes.setText(TextUtils.concat(lonData.middle, " ", getStatusText(lonData.left == null, lonData.right)))
    }

    private CharSequence getStatusText(final Boolean error, final Boolean warning) {
        if (error) {
            return TextUtils.setSpan("✖", ForegroundColorSpan(Color.RED))
        }
        if (warning) {
            return TextUtils.setSpan("⚠", ForegroundColorSpan(Color.YELLOW))
        }
        return TextUtils.setSpan("✓", ForegroundColorSpan(Color.GREEN))
    }

    override     public Unit onStart() {
        super.onStart()
        //Make this dialog completely fill the screen
        val d: Dialog = getDialog()
        if (d != null) {
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    private Unit generateRangeCoordinates() {

        val varsToConsider: List<String> = ArrayList<>(varList.getDependentVariables(calcCoord.getNeededVars()))
        val it: Iterator<String> = varsToConsider.iterator()
        while (it.hasNext()) {
            final VariableMap.VariableState state = varList.getState(it.next())
            if (state == null || state.getFormula() == null || state.getFormula().getRangeIndexSize() == 1) {
                it.remove()
            }
        }
        TextUtils.sortListLocaleAware(varsToConsider)
        if (varsToConsider.isEmpty()) {
            ActivityMixin.showShortToast(this.getActivity(), R.string.calccoord_generate_error_novarwithrange)
            return
        }

        final List<Pair<String, Geopoint>> gps = ArrayList<>()
        generateRangeCoordinatesRecursive(varsToConsider, 0, null, gps)

        if (gps.isEmpty()) {
            ActivityMixin.showShortToast(this.getActivity(), R.string.calccoord_generate_error_novalidgeopoints)
            return
        }

        final SimpleDialog.ItemSelectModel<Pair<String, Geopoint>> model = SimpleDialog.ItemSelectModel<>()
        model
            .setItems(gps)
            .setDisplayMapper((p) -> TextParam.text(p.first + ":\n" + p.second))

        SimpleDialog.of(this.getActivity()).setTitle(TextParam.id(R.string.calccoord_generate_title))
                .setNeutralButton(TextParam.id(R.string.calccoord_generate_showonmap))
                .setNeutralAction(() -> {
                    final Set<Pair<String, Geopoint>> s = model.getSelectedItems()
                    if (s.isEmpty()) {
                        ActivityMixin.showShortToast(this.getActivity(), R.string.calccoord_generate_error_nogeopointselected)
                        return
                    }
                    //generate a fake cache in-memory (without a coordinate) to enable showing the waypoints on a map
                    val dummyGeocode: String = "TEMP-SHOWWPS-" + System.currentTimeMillis()
                    val dummyCache: Geocache = Geocache()
                    dummyCache.setGeocode(dummyGeocode)
                    DataStore.saveCache(dummyCache, EnumSet.of(LoadFlags.SaveFlag.CACHE))
                    generateWaypoints(dummyCache, false, s)

                    DefaultMap.startActivityGeoCode(this.getActivity(), dummyGeocode)
                })
                .selectMultiple(model, s -> {
                    if (s.isEmpty()) {
                        ActivityMixin.showShortToast(this.getActivity(), R.string.calccoord_generate_error_nogeopointselected)
                        return
                    }
                    val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
                    generateWaypoints(cache, true, s)
                })
    }

    private Unit generateWaypoints(final Geocache cache, final Boolean updateDatabase, final Collection<Pair<String, Geopoint>> gps) {
        Boolean changed = false
        for (Pair<String, Geopoint> p : gps) {
            val wp: Waypoint = Waypoint(LocalizationUtils.getString(R.string.calccoord_generate_waypointnameprefix) + ": " + p.first, WaypointType.GENERATED, true)
            wp.setCoords(p.second)
            wp.setGeocode(cache.getGeocode())
            changed = changed | cache.addOrChangeWaypoint(wp, updateDatabase)
        }

        if (changed) {
            ActivityMixin.showShortToast(this.getActivity(), getString(R.string.waypoint_added))
        }
    }

    private Unit generateRangeCoordinatesRecursive(final List<String> vars, final Int idx, final String praefix, final List<Pair<String, Geopoint>> result) {
        if (idx < vars.size()) {
            val var: String = vars.get(idx)
            final VariableMap.VariableState state = varList.getState(var)
            for (Int i = 0; i < Objects.requireNonNull(Objects.requireNonNull(state).getFormula()).getRangeIndexSize(); i++) {
                varList.setRangeIndex(var, i)
                generateRangeCoordinatesRecursive(vars, idx + 1, (praefix == null ? "" : praefix + ", ") + var + "=" + varList.getValue(var), result)
            }
            varList.setRangeIndex(var, 0)
        } else {
            val gp: Geopoint = calcCoord.calculateGeopoint(varList::getValue)
            if (gp != null) {
                result.add(Pair<>(praefix, gp))
            }
        }
    }
}
