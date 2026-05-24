package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.CoordinatescalculateglobalDialogBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.RDPoint;
import cgeo.geocaching.location.UTMPoint;
import cgeo.geocaching.models.CacheVariableList;
import cgeo.geocaching.models.CalculatedCoordinate;
import cgeo.geocaching.models.CalculatedCoordinateType;
import cgeo.geocaching.models.CoordinateInputData;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.UtmPatternUtils;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.CalculatedCoordinateInputGuideView;
import cgeo.geocaching.ui.SimpleItemListModel;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.VariableListView;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.unifiedmap.DefaultMap;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.formulas.DegreeFormula;
import cgeo.geocaching.utils.formulas.FormulaUtils;
import cgeo.geocaching.utils.formulas.VariableList;
import cgeo.geocaching.utils.formulas.VariableMap;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;

/**
 * Dialog to manage calculation of a coordinate
 */
public class CoordinatesCalculateGlobalDialog extends DialogFragment {

    private static final String ARG_INPUT_DATA = "arg_input_data";

    private String geocode;
    private CalculatedCoordinate calcCoord = new CalculatedCoordinate();
    private Geopoint geopoint;
    private String notes;
    private VariableList varList;
    private VariableListView.VariablesListAdapter varListAdapter;

    private static DialogCallback callback;
    private CoordinatescalculateglobalDialogBinding binding;

    private final TextSpinner<CalculatedCoordinateType> displayType = new TextSpinner<>();
    private boolean suppressUtmFieldSync = false;
    private boolean suppressTypeRefresh = false;

    private CoordinateInputData createFromDialog() {
        final CoordinateInputData cid = new CoordinateInputData();
        cid.setGeocode(geocode);
        cid.setNotes(ViewUtils.getEditableText(binding.notesText.getText()));
        cid.setCalculatedCoordinate(calcCoord);
        cid.setGeopoint(calcCoord.calculateGeopoint(varList::getValue));
        return cid;
    }

    private void saveAndFinishDialog() {
        final CoordinateInputData cid = createFromDialog();
        ActivityMixin.showToast(this.getActivity(), R.string.warn_calculator_state_save);

        final Activity activity = requireActivity();
        if (activity instanceof CoordinateInputDialog.CoordinateUpdate) {
            ((CoordinateInputDialog.CoordinateUpdate) activity).updateCoordinates(cid);
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
    public static void show(final FragmentManager mgr, final DialogCallback callbackMethod, final CoordinateInputData initialState) {
        callback = callbackMethod;
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
            if (!StringUtils.isBlank(geocode)) {
                final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    this.varList = cache.getVariables();
                }
            }
        }
        if (varList == null) {
            varList = new VariableList();
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initFromBundle(savedInstanceState);
        initFromBundle(getArguments());
        if (geopoint == null) {
            geopoint = LocationDataProvider.getInstance().currentGeo().getCoords();
        }
    }

    /**
     * Save the current state of the calculator such that it can be restored after screen rotation (or similar)
     */
    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_INPUT_DATA, createFromDialog());
    }


    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        binding = CoordinatescalculateglobalDialogBinding.inflate(inflater, container, false);
        binding.NonPlainFormat.setVisibility(View.GONE);
        binding.NonPlainFormat.setVariableList(varList);
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
            toolbar.setTitle(R.string.waypoint_calculated_coordinates_global);
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
            // When the callback is hit it will clear the calculator state associated with the waypoint
            final CoordinateInputData cid = createFromDialog();
            final Geopoint plainGp = cid.getGeopoint();
            if (plainGp == null) {
                SimpleDialog.of(this.getActivity()).setTitle(R.string.calccoord_convert_to_plain)
                        .setMessage(TextParam.id(R.string.calccoord_convert_to_plain_error))
                        .setPositiveButton(TextParam.id(R.string.button_continue))
                        .setNegativeButton(TextParam.id(R.string.cancel))
                        .confirm(() -> {
                            callback.onDialogClosed(null);
                            dismiss();
                        });
                return;
            }
            callback.onDialogClosed(plainGp);
            dismiss();
        });

        binding.generateRangeCoordinates.setOnClickListener(v -> generateRangeCoordinates());

        displayType.setSpinner(binding.ccGuidedFormat)
                .setValues(CollectionStream.of(CalculatedCoordinateType.values()).filter(t -> PLAIN != t).toList())
                .setDisplayMapperPure(CalculatedCoordinateType::toUserDisplayableString)
                .set(calcCoord.getType())
                .setChangeListener(t -> refreshType(t, false));

        varListAdapter = binding.variableList.getAdapter();
        varListAdapter.setDisplay(VariableListView.DisplayType.MINIMALISTIC, 2);
        varListAdapter.setVarChangeCallback((v, s) -> {
            varListAdapter.checkAddVisibleVariables(Collections.singletonList(v));
            updateView();
        });
        varListAdapter.setVariableList(varList);
        varListAdapter.setVisibleVariablesAndDependent(calcCoord.getNeededVars());

        binding.variablesTidyup.setOnClickListener(v -> varListAdapter.tidyUp(calcCoord.getNeededVars()));

        binding.notesText.setText(notes);

        binding.PlainLat.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            if (calcCoord.getType() == CalculatedCoordinateType.UTM) {
                syncUtmPatternsFromPlainFields();
            } else if (calcCoord.getType() == CalculatedCoordinateType.RD) {
                calcCoord.setLatitudePattern(normalizeRdPattern('X', s.toString()));
            } else {
                calcCoord.setLatitudePattern(s.toString());
            }
            varListAdapter.checkAddVisibleVariables(calcCoord.getNeededVars());
            updateView();
        }));

        binding.PlainLon.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            if (calcCoord.getType() == CalculatedCoordinateType.UTM) {
                syncUtmPatternsFromPlainFields();
            } else if (calcCoord.getType() == CalculatedCoordinateType.RD) {
                calcCoord.setLongitudePattern(normalizeRdPattern('Y', s.toString()));
            } else {
                calcCoord.setLongitudePattern(s.toString());
            }
            varListAdapter.checkAddVisibleVariables(calcCoord.getNeededVars());
            updateView();
        }));

        binding.PlainThird.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
            if (calcCoord.getType() == CalculatedCoordinateType.UTM && !suppressUtmFieldSync) {
                syncUtmPatternsFromPlainFields();
                varListAdapter.checkAddVisibleVariables(calcCoord.getNeededVars());
                updateView();
            }
        }));

        suppressUtmFieldSync = true;
        binding.PlainLat.setText(calcCoord.getLatitudePattern());
        binding.PlainLon.setText(calcCoord.getLongitudePattern());
        binding.PlainThird.setText("");
        suppressUtmFieldSync = false;

        binding.NonPlainFormat.setChangeListener(p -> {
            calcCoord.setLatitudePattern(p.first);
            calcCoord.setLongitudePattern(p.second);
            varListAdapter.checkAddVisibleVariables(calcCoord.getNeededVars());
            updateView();
        });

        binding.ccSwitchGuided.setOnCheckedChangeListener((v, c) -> {
            Settings.putBoolean(R.string.pref_preferGuidedCoordFormulaInput, c);
            final CalculatedCoordinateType currentType = calcCoord.getType();
            final CalculatedCoordinateType selectedType = displayType.get() == null ? currentType : displayType.get();
            final CalculatedCoordinateType targetType = currentType == PLAIN ? (selectedType == null ? PLAIN : selectedType) : currentType;
            refreshType(targetType, false);
        });

        binding.ccPaste.setOnClickListener(v -> {
            final String clip = ClipboardUtils.getText();
            final List<Pair<String, String>> patterns = FormulaUtils.scanForCoordinates(Collections.singleton(clip), null);
            if (patterns.isEmpty()) {
                ActivityMixin.showShortToast(this.getActivity(), R.string.variables_scanlisting_nopatternfound);
            } else {
                binding.PlainLat.setText(patterns.get(0).first);
                binding.PlainLon.setText(patterns.get(0).second);
            }
        });

        binding.ccPlainTools.setOnClickListener(v -> {
            final List<Integer> options = List.of(R.string.calccoord_remove_spaces, R.string.calccoord_replace_x_with_multiplication_symbol);
            final SimpleDialog.ItemSelectModel<Integer> model = new SimpleDialog.ItemSelectModel<>();
            model
                .setItems(options)
                .setDisplayMapper(TextParam::id)
                .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_PLAIN);

            SimpleDialog.of(this.getActivity()).setTitle(R.string.calccoord_plain_tools_title)
                    .selectSingle(model, (o) -> {
                        if (o == R.string.calccoord_remove_spaces) {
                            binding.PlainLat.setText(DegreeFormula.removeSpaces(ViewUtils.getEditableText(binding.PlainLat.getText())));
                            binding.PlainLon.setText(DegreeFormula.removeSpaces(ViewUtils.getEditableText(binding.PlainLon.getText())));
                        } else if (o == R.string.calccoord_replace_x_with_multiplication_symbol) {
                            binding.PlainLat.setText(DegreeFormula.replaceXWithMultiplicationSign(ViewUtils.getEditableText(binding.PlainLat.getText())));
                            binding.PlainLon.setText(DegreeFormula.replaceXWithMultiplicationSign(ViewUtils.getEditableText(binding.PlainLon.getText())));
                        }
                    });
        });

        //check if type from config is applicable
        if (calcCoord.getType() != PLAIN) {
            final CalculatedCoordinateType type = CalculatedCoordinateInputGuideView.guessType(calcCoord.getLatitudePattern(), calcCoord.getLongitudePattern());
            if (type != null) {
                calcCoord.setType(type);
            }
        }

        refreshType(calcCoord.getType(), true);

        return binding.getRoot();
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private void refreshType(final CalculatedCoordinateType newType, final boolean initialLoad) {
        if (suppressTypeRefresh) {
            return;
        }
        suppressTypeRefresh = true;
        try {
        final Geopoint sourceGp = calcCoord == null ? null : calcCoord.calculateGeopoint(varList::getValue);
        calcCoord.setType(newType);
        if (!initialLoad && sourceGp != null) {
            applyGeopointAsPatternForType(newType, sourceGp);
        } else if (newType == CalculatedCoordinateType.UTM) {
            normalizeUtmPatterns();
        }
        final boolean isGuidedMode = newType != PLAIN && Settings.getBoolean(R.string.pref_preferGuidedCoordFormulaInput, true);

        final Geopoint currentGp = sourceGp == null ? geopoint : sourceGp;
        binding.PlainFormat.setVisibility(!isGuidedMode ? View.VISIBLE : View.GONE);
        binding.NonPlainFormat.setVisibility(isGuidedMode ? View.VISIBLE : View.GONE);
        if (!isGuidedMode) {
            binding.NonPlainFormat.unmarkButtons();
        }
        binding.ccGuidedFormat.setVisibility(!isGuidedMode ? View.GONE : View.VISIBLE);
        binding.ccSwitchGuided.setChecked(isGuidedMode);
        if (isGuidedMode) {
            displayType.set(newType);
        }
        binding.ccSwitchGuided.setVisibility(View.VISIBLE);
        binding.ccPlainTools.setVisibility(isGuidedMode ? View.GONE : View.VISIBLE);

        binding.ccPaste.setVisibility(isGuidedMode ? View.GONE : View.VISIBLE);
        binding.ccPaste.setEnabled(!FormulaUtils.scanForCoordinates(Collections.singleton(ClipboardUtils.getText()), null).isEmpty());

        if (!isGuidedMode) {
            configurePlainInputsForType(newType);
            if (initialLoad) {
                if (newType == CalculatedCoordinateType.UTM) {
                    applyUtmPatternsToPlainFields();
                } else if (newType == CalculatedCoordinateType.RD) {
                    binding.PlainLat.setText(stripAxisPrefix(calcCoord.getLatitudePattern(), 'X'));
                    binding.PlainLon.setText(stripAxisPrefix(calcCoord.getLongitudePattern(), 'Y'));
                    binding.PlainThird.setText("");
                } else {
                    binding.PlainLat.setText(calcCoord.getLatitudePattern() == null ? "" : calcCoord.getLatitudePattern());
                    binding.PlainLon.setText(calcCoord.getLongitudePattern() == null ? "" : calcCoord.getLongitudePattern());
                    binding.PlainThird.setText("");
                }
            } else {
                if (newType == CalculatedCoordinateType.UTM) {
                    applyUtmPatternsToPlainFields();
                } else {
                    final Pair<String, String> coords = binding.NonPlainFormat.getPlain();
                    if (newType == CalculatedCoordinateType.RD) {
                        binding.PlainLat.setText(stripAxisPrefix(coords.first, 'X'));
                        binding.PlainLon.setText(stripAxisPrefix(coords.second, 'Y'));
                    } else {
                        binding.PlainLat.setText(coords.first);
                        binding.PlainLon.setText(coords.second);
                    }
                    binding.PlainThird.setText("");
                }
            }
        } else {
            configurePlainInputsForType(newType);
            binding.NonPlainFormat.setData(newType, calcCoord.getLatitudePattern(), calcCoord.getLongitudePattern(), currentGp);
        }
        } finally {
            suppressTypeRefresh = false;
        }
    }

    private void applyGeopointAsPatternForType(final CalculatedCoordinateType type, final Geopoint gp) {
        final Locale locale = Locale.US;
        switch (type) {
            case DEGREE:
                calcCoord.setLatitudePattern(String.format(locale, "%c %08.5f°", gp.getLatDir(), Math.abs(gp.getLatitude())));
                calcCoord.setLongitudePattern(String.format(locale, "%c%09.5f°", gp.getLonDir(), Math.abs(gp.getLongitude())));
                break;
            case DEGREE_MINUTE:
                calcCoord.setLatitudePattern(String.format(locale, "%c %02d°%02d.%03d'", gp.getLatDir(), gp.getDecMinuteLatDeg(), gp.getDecMinuteLatMin(), gp.getDecMinuteLatMinFrac()));
                calcCoord.setLongitudePattern(String.format(locale, "%c%03d°%02d.%03d'", gp.getLonDir(), gp.getDecMinuteLonDeg(), gp.getDecMinuteLonMin(), gp.getDecMinuteLonMinFrac()));
                break;
            case DEGREE_MINUTE_SEC:
                calcCoord.setLatitudePattern(String.format(locale, "%c %02d°%02d'%02d.%03d\"", gp.getLatDir(), gp.getDMSLatDeg(), gp.getDMSLatMin(), gp.getDMSLatSec(), gp.getDMSLatSecFrac()));
                calcCoord.setLongitudePattern(String.format(locale, "%c%03d°%02d'%02d.%03d\"", gp.getLonDir(), gp.getDMSLonDeg(), gp.getDMSLonMin(), gp.getDMSLonSec(), gp.getDMSLonSecFrac()));
                break;
            case UTM:
                final UTMPoint utm = UTMPoint.latLong2UTM(gp);
                final String zone = String.format(locale, "%02d%c", utm.getZoneNumber(), utm.getZoneLetter());
                final String easting = Long.toString(Math.round(utm.getEasting()));
                final String northing = Long.toString(Math.round(utm.getNorthing()));
                final UtmPatternUtils.UtmPatterns utmPatterns = UtmPatternUtils.createPatternsFromPlainFields(zone, easting, northing, true);
                calcCoord.setLatitudePattern(utmPatterns.latitudePattern);
                calcCoord.setLongitudePattern(utmPatterns.longitudePattern);
                break;
            case RD:
                final RDPoint rd = RDPoint.latLong2RD(gp);
                calcCoord.setLatitudePattern("X " + Math.round(rd.getX()));
                calcCoord.setLongitudePattern("Y " + Math.round(rd.getY()));
                break;
            case PLAIN:
            default:
                break;
        }
    }

    private void configurePlainInputsForType(final CalculatedCoordinateType type) {
        final boolean isUtm = type == CalculatedCoordinateType.UTM;
        final boolean isRd = type == CalculatedCoordinateType.RD;
        binding.PlainThirdLayout.setVisibility(isUtm ? View.VISIBLE : View.GONE);
        final String latHint = isUtm ? LocalizationUtils.getString(R.string.coord_input_zone)
                : isRd ? "X" : LocalizationUtils.getString(R.string.latitude);
        final String lonHint = isUtm ? LocalizationUtils.getString(R.string.coord_input_easting)
                : isRd ? "Y" : LocalizationUtils.getString(R.string.longitude);
        final String thirdHint = LocalizationUtils.getString(R.string.coord_input_northing);
        binding.PlainLatLayout.setHint(latHint);
        binding.PlainLonLayout.setHint(lonHint);
        binding.PlainThirdLayout.setHint(thirdHint);
        binding.PlainLat.setHint(latHint);
        binding.PlainLon.setHint(lonHint);
        binding.PlainThird.setHint(thirdHint);
    }

    private void applyUtmPatternsToPlainFields() {
        final UtmPatternUtils.UtmFields utm = UtmPatternUtils.extractFieldsFromPatterns(calcCoord.getLatitudePattern(), calcCoord.getLongitudePattern());

        suppressUtmFieldSync = true;
        binding.PlainLat.setText(utm.zone);
        binding.PlainLon.setText(utm.easting);
        binding.PlainThird.setText(utm.northing);
        suppressUtmFieldSync = false;
        updateView();
    }

    private void normalizeUtmPatterns() {
        final UtmPatternUtils.UtmFields utm = UtmPatternUtils.extractFieldsFromPatterns(calcCoord.getLatitudePattern(), calcCoord.getLongitudePattern());
        final UtmPatternUtils.UtmPatterns patterns = UtmPatternUtils.createPatternsFromPlainFields(utm.zone, utm.easting, utm.northing, true);
        calcCoord.setLatitudePattern(patterns.latitudePattern);
        calcCoord.setLongitudePattern(patterns.longitudePattern);
    }

    private void syncUtmPatternsFromPlainFields() {
        if (suppressUtmFieldSync) {
            return;
        }

        final UtmPatternUtils.UtmPatterns patterns = UtmPatternUtils.createPatternsFromPlainFields(
                ViewUtils.getEditableText(binding.PlainLat.getText()),
                ViewUtils.getEditableText(binding.PlainLon.getText()),
                ViewUtils.getEditableText(binding.PlainThird.getText()),
                true);
        calcCoord.setLatitudePattern(patterns.latitudePattern);
        calcCoord.setLongitudePattern(patterns.longitudePattern);
    }

    private void updateView() {
        //update the displayed coordinate texts
        final ImmutableTriple<Double, CharSequence, Boolean> latData = calcCoord.calculateLatitudeData(varList::getValue);
        final ImmutableTriple<Double, CharSequence, Boolean> lonData = calcCoord.calculateLongitudeData(varList::getValue);
        final boolean projectedType = calcCoord.getType() == CalculatedCoordinateType.UTM || calcCoord.getType() == CalculatedCoordinateType.RD;
        final boolean geopointInvalid = projectedType && calcCoord.calculateGeopoint(varList::getValue) == null;
        if (calcCoord.getType() == CalculatedCoordinateType.UTM) {
            final boolean isGuidedMode = binding.ccSwitchGuided.isChecked();
            final String[] utmLatTokens = Objects.toString(latData.middle, "").trim().split("\\s+");
            final String[] utmLonTokens = Objects.toString(lonData.middle, "").trim().split("\\s+");
            final String[] rawLatTokens = StringUtils.defaultString(calcCoord.getLatitudePattern()).trim().split("\\s+");
            final String[] rawLonTokens = StringUtils.defaultString(calcCoord.getLongitudePattern()).trim().split("\\s+");
            String zoneText = StringUtils.EMPTY;
            String eastingText = StringUtils.EMPTY;
            String northingText = StringUtils.EMPTY;

            if (!isGuidedMode) {
                zoneText = ViewUtils.getEditableText(binding.PlainLat.getText()).trim().toUpperCase(Locale.US);
                eastingText = ViewUtils.getEditableText(binding.PlainLon.getText()).trim();
                northingText = ViewUtils.getEditableText(binding.PlainThird.getText()).trim();
            } else {
                if (utmLatTokens.length >= 2) {
                    final String tokenA = utmLatTokens[0].toUpperCase(Locale.US);
                    final String tokenB = utmLatTokens[1].toUpperCase(Locale.US);
                    if (tokenA.matches("^[0-9]{1,2}[C-HJ-NP-X]$")) {
                        zoneText = tokenA;
                        eastingText = utmLatTokens[1];
                    } else if (tokenB.matches("^[0-9]{1,2}[C-HJ-NP-X]$")) {
                        zoneText = tokenB;
                        eastingText = utmLatTokens[0];
                    }
                }

                if (StringUtils.isBlank(zoneText) && rawLatTokens.length >= 3) {
                    zoneText = rawLatTokens[1] + rawLatTokens[0].toUpperCase(Locale.US);
                    eastingText = rawLatTokens[2];
                } else if (StringUtils.isBlank(zoneText) && rawLatTokens.length == 2) {
                    if (rawLatTokens[0].matches("^[0-9]{1,2}$")) {
                        zoneText = rawLatTokens[0];
                        eastingText = rawLatTokens[1];
                    } else {
                        zoneText = rawLatTokens[1] + rawLatTokens[0].toUpperCase(Locale.US);
                    }
                } else if (StringUtils.isBlank(zoneText) && rawLatTokens.length == 1) {
                    zoneText = rawLatTokens[0];
                }
                if (StringUtils.isBlank(eastingText) && rawLatTokens.length >= 3) {
                    eastingText = rawLatTokens[2];
                }
                if (utmLonTokens.length >= 3 && "N".equalsIgnoreCase(utmLonTokens[0])) {
                    northingText = utmLonTokens[1] + utmLonTokens[2];
                } else if (utmLonTokens.length >= 2 && "N".equalsIgnoreCase(utmLonTokens[0])) {
                    northingText = utmLonTokens[1];
                } else if (utmLonTokens.length >= 1) {
                    northingText = utmLonTokens[utmLonTokens.length - 1];
                }
                if (StringUtils.isBlank(northingText) && rawLonTokens.length >= 3 && "N".equalsIgnoreCase(rawLonTokens[0])) {
                    northingText = rawLonTokens[1] + rawLonTokens[2];
                }
            }

            final boolean zoneError = !zoneText.toUpperCase(Locale.US).matches("^[0-9]{1,2}[C-HJ-NP-X]$");
            final boolean eastingError = !eastingText.matches("^[0-9]{5,7}$");
            final boolean northingError = !northingText.matches("^[0-9]{6,8}$");
            binding.zoneRes.setVisibility(View.VISIBLE);
            binding.zoneRes.setText(TextUtils.concat("Z ", zoneText, " ", getStatusText(zoneError, latData.right)));
            binding.latRes.setText(TextUtils.concat("E ", eastingText, " ", getStatusText(eastingError, latData.right)));
            binding.lonRes.setText(TextUtils.concat("N ", northingText, " ", getStatusText(northingError, lonData.right)));
        } else if (calcCoord.getType() == CalculatedCoordinateType.RD) {
            binding.zoneRes.setVisibility(View.GONE);
            // Official EPSG:28992 projected bounds (Area of use Netherlands onshore incl. coastal zone).
            // X: 482.06 .. 284182.97, Y: 306602.42 .. 637049.52
            final Long xValue = parseLongValue(latData.middle);
            final Long yValue = parseLongValue(lonData.middle);
            final boolean xRangeInvalid = xValue == null || xValue < 482 || xValue > 284183;
            final boolean yRangeInvalid = yValue == null || yValue < 306602 || yValue > 637050;
            final CharSequence xDisplay = TextUtils.concat("X ", latData.middle);
            final CharSequence yDisplay = TextUtils.concat("Y ", lonData.middle);
            binding.latRes.setText(TextUtils.concat(xDisplay, " ", getStatusText(latData.left == null || xRangeInvalid, latData.right)));
            binding.lonRes.setText(TextUtils.concat(yDisplay, " ", getStatusText(lonData.left == null || yRangeInvalid, lonData.right)));
        } else {
            binding.zoneRes.setVisibility(View.GONE);
            binding.latRes.setText(TextUtils.concat(latData.middle, " ", getStatusText(latData.left == null || geopointInvalid, latData.right)));
            binding.lonRes.setText(TextUtils.concat(lonData.middle, " ", getStatusText(lonData.left == null || geopointInvalid, lonData.right)));
        }
    }

    private static String normalizeRdPattern(final char axis, final String value) {
        final String stripped = stripAxisPrefix(value, axis);
        if (StringUtils.isBlank(stripped)) {
            return Character.toUpperCase(axis) + " ";
        }
        return Character.toUpperCase(axis) + " " + stripped;
    }

    private static String stripAxisPrefix(final String value, final char axis) {
        final String text = StringUtils.defaultString(value).trim();
        if (text.length() > 0 && Character.toUpperCase(text.charAt(0)) == Character.toUpperCase(axis)) {
            return text.substring(1).trim();
        }
        return text;
    }

    private static Long parseLongValue(final CharSequence text) {
        try {
            return Long.parseLong(StringUtils.defaultString(Objects.toString(text, "")).trim());
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private CharSequence getStatusText(final boolean error, final boolean warning) {
        if (error) {
            return TextUtils.setSpan("✖", new ForegroundColorSpan(Color.RED));
        }
        if (warning) {
            return TextUtils.setSpan("⚠", new ForegroundColorSpan(Color.YELLOW));
        }
        return TextUtils.setSpan("✓", new ForegroundColorSpan(Color.GREEN));
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

    private void generateRangeCoordinates() {

        final List<String> varsToConsider = new ArrayList<>(varList.getDependentVariables(calcCoord.getNeededVars()));
        final Iterator<String> it = varsToConsider.iterator();
        while (it.hasNext()) {
            final VariableMap.VariableState state = varList.getState(it.next());
            if (state == null || state.getFormula() == null || state.getFormula().getRangeIndexSize() == 1) {
                it.remove();
            }
        }
        TextUtils.sortListLocaleAware(varsToConsider);
        if (varsToConsider.isEmpty()) {
            ActivityMixin.showShortToast(this.getActivity(), R.string.calccoord_generate_error_novarwithrange);
            return;
        }

        final List<Pair<String, Geopoint>> gps = new ArrayList<>();
        generateRangeCoordinatesRecursive(varsToConsider, 0, null, gps);

        if (gps.isEmpty()) {
            ActivityMixin.showShortToast(this.getActivity(), R.string.calccoord_generate_error_novalidgeopoints);
            return;
        }

        final SimpleDialog.ItemSelectModel<Pair<String, Geopoint>> model = new SimpleDialog.ItemSelectModel<>();
        model
            .setItems(gps)
            .setDisplayMapper((p) -> TextParam.text(p.first + ":\n" + p.second));

        SimpleDialog.of(this.getActivity()).setTitle(TextParam.id(R.string.calccoord_generate_title))
                .setNeutralButton(TextParam.id(R.string.calccoord_generate_showonmap))
                .setNeutralAction(() -> {
                    final Set<Pair<String, Geopoint>> s = model.getSelectedItems();
                    if (s.isEmpty()) {
                        ActivityMixin.showShortToast(this.getActivity(), R.string.calccoord_generate_error_nogeopointselected);
                        return;
                    }
                    //generate a new fake cache in-memory (without a coordinate) to enable showing the waypoints on a map
                    final String dummyGeocode = "TEMP-SHOWWPS-" + System.currentTimeMillis();
                    final Geocache dummyCache = new Geocache();
                    dummyCache.setGeocode(dummyGeocode);
                    DataStore.saveCache(dummyCache, EnumSet.of(LoadFlags.SaveFlag.CACHE));
                    generateWaypoints(dummyCache, false, s);

                    DefaultMap.startActivityGeoCode(this.getActivity(), dummyGeocode);
                })
                .selectMultiple(model, s -> {
                    if (s.isEmpty()) {
                        ActivityMixin.showShortToast(this.getActivity(), R.string.calccoord_generate_error_nogeopointselected);
                        return;
                    }
                    final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                    generateWaypoints(cache, true, s);
                });
    }

    private void generateWaypoints(final Geocache cache, final boolean updateDatabase, final Collection<Pair<String, Geopoint>> gps) {
        boolean changed = false;
        for (Pair<String, Geopoint> p : gps) {
            final Waypoint wp = new Waypoint(LocalizationUtils.getString(R.string.calccoord_generate_waypointnameprefix) + ": " + p.first, WaypointType.GENERATED, true);
            wp.setCoords(p.second);
            wp.setGeocode(cache.getGeocode());
            changed = changed | cache.addOrChangeWaypoint(wp, updateDatabase);
        }

        if (changed) {
            ActivityMixin.showShortToast(this.getActivity(), LocalizationUtils.getString(R.string.waypoint_added));
        }
    }

    private void generateRangeCoordinatesRecursive(final List<String> vars, final int idx, final String praefix, final List<Pair<String, Geopoint>> result) {
        if (idx < vars.size()) {
            final String var = vars.get(idx);
            final VariableMap.VariableState state = varList.getState(var);
            for (int i = 0; i < Objects.requireNonNull(Objects.requireNonNull(state).getFormula()).getRangeIndexSize(); i++) {
                varList.setRangeIndex(var, i);
                generateRangeCoordinatesRecursive(vars, idx + 1, (praefix == null ? "" : praefix + ", ") + var + "=" + varList.getValue(var), result);
            }
            varList.setRangeIndex(var, 0);
        } else {
            final Geopoint gp = calcCoord.calculateGeopoint(varList::getValue);
            if (gp != null) {
                result.add(new Pair<>(praefix, gp));
            }
        }
    }
}
