package cgeo.geocaching.ui.dialog;

import static cgeo.geocaching.R.id.PlainFormat;
import static cgeo.geocaching.R.id.coordTable;
import static cgeo.geocaching.models.CalcState.ERROR_CHAR;
import static cgeo.geocaching.ui.dialog.CoordinatesInputDialog.GEOPOINT_ARG;

import cgeo.geocaching.BuildConfig;
import cgeo.geocaching.EditWaypointActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.CalcState;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.CalculateButton;
import cgeo.geocaching.ui.CalculatorVariable;
import cgeo.geocaching.ui.EditButton;
import cgeo.geocaching.utils.CalculationUtils;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.gridlayout.widget.GridLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Through out this implementation:
 *
 * 'Equations' are used to represent 'Variables' that appear in the description of the cache coordinated themselves.
 *             As in "N 42° 127.ABC".  In this example 'A', 'B' and 'C' are all 'equations'.
 *             All 'equations' must have a CAPITAL-LETTER name.
 *
 * 'FreeVariables' are used to represent 'Variables' that appear in the 'expression' of an equation
 *                 As in "X = a^2 + b^2".  In this example 'a' and 'b' are both 'freeVariables'.
 *                 All 'freeVariables' must have a lower-case name.
 */
public class CoordinatesCalculateDialog extends DialogFragment implements ClickCompleteCallback, LongClickCompleteCallback {

    /** Character used to represent a "blanked-out" CoordinateButton */
    private static final String PLACE_HOLDER = "~";
    private static final Pattern PRE_DECIMAL_PATTERN = Pattern.compile("(.*)[.,]");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("[.,](\\d*)");
    private static final Pattern TRAILING_PATTERN = Pattern.compile("[.,]\\d*(.*)");
    private static final String SYMBOL_DEG = "°";
    private static final String SYMBOL_MIN = "'";
    private static final String SYMBOL_SEC = "\"";
    private static final String SYMBOL_POINT = ".";
    private static final char[] BRACKET_OPENINGS = {'(', '[', '{'};
    private static final char[] BRACKET_CLOSINGS = {')', ']', '}'};

    public static final String CALC_STATE = "calc_state";

    private ImageButton doneButton;
    private boolean stateSaved = false;

    private Geopoint gp;
    private int latLeadingZerosAdded;
    private int lonLeadingZerosAdded;
    private CalcState savedState;

    /** List of equations to be displayed in the calculator */
    private List<CalculatorVariable> equations;
    /** List of freeVariables to be displayed in the calculator */
    private List<CalculatorVariable> freeVariables;
    /** List of previously assigned variables that have since been removed */
    private List<CalculatorVariable.VariableData> variableBank;

    private Spinner spinner;

    private String inputLatHem;
    /** Latitude hemisphere (North or South) */
    private Button bLatHem;
    private final CalculateButton[] bLatDeg = new CalculateButton[2],
                                    bLatMin = new CalculateButton[2],
                                    bLatSec = new CalculateButton[2],
                                    bLatPnt = new CalculateButton[5];

    private String inputLonHem;
    /** Longitude hemisphere (East or West) */
    private Button bLonHem;
    private final CalculateButton[] bLonDeg = new CalculateButton[3],
                                    bLonMin = new CalculateButton[2],
                                    bLonSec = new CalculateButton[2],
                                    bLonPnt = new CalculateButton[5];

    private EditText ePlainLat, ePlainLon;

    private HorizontalScrollView variablesPanel;
    private View variablesScrollableContent, variableDivider;
    private GridLayout equationGrid, variableGrid;

    private TextView tLatResult, tLonResult;

    private EditText notes;

    private Settings.CoordInputFormatEnum currentFormat = null;
    private List<CalculateButton> latButtons;
    private List<CalculateButton> lonButtons;
    private List<CalculateButton> coordButtons;
    private List<View> minButtons;
    private List<View> secButtons;
    private List<CalculateButton> pointLowButtons;
    private List<TextView> lastUnits;

    /**
     * Class used for checking that a value is with in a given range.
     * This is used to check for upper-case an lower-case letters.
     */
    private static class CaseCheck {
        final boolean useUpper;

        CaseCheck(final boolean upper) {
            useUpper = upper;
        }

        boolean check(final char ch) {

            boolean returnValue = Character.isLetterOrDigit(ch);
            if (useUpper) {
                returnValue &= Character.isUpperCase(ch);
            } else {
                returnValue &= Character.isLowerCase(ch);
            }

            return returnValue;
        }
    }

    @Override
    public void onClickCompleteCallback() {
        stateSaved = false;
        resortEquations();
        updateResult();
    }

    @Override
    public void onLongClickCompleteCallback() {
        stateSaved = false;
        resortEquations();
        updateResult();
    }

    private class InputDoneListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            // Save calculator state regardless of weather the coordinates are valid or not.
            final CalcState currentState = getCurrentState();
            setSavedState(currentState);
            ((CoordinatesInputDialog.CalculateState) getActivity()).saveCalculatorState(currentState);
            ((EditWaypointActivity) getActivity()).getUserNotes().setText(notes.getText());


            if (areCurrentCoordinatesValid()) {
                ((CoordinatesInputDialog.CoordinateUpdate) getActivity()).updateCoordinates(gp);
            } else {
                ((CoordinatesInputDialog.CoordinateUpdate) getActivity()).updateCoordinates(null);
            }
            close();
        }
    }

    private class CalculateCancelListener implements View.OnClickListener {
        @Override
        public void onClick(final View v) {
            close();
        }
    }

    private class PlainWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            // Intentionally left empty
        }
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            // Intentionally left empty
        }
        @Override
        public void afterTextChanged(final Editable s) {
            stateSaved = false;
            resortEquations();
            updateResult();
        }
    }

    private class EquationWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            // Intentionally left empty
        }
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            // Intentionally left empty
        }
        @Override
        public void afterTextChanged(final Editable s) {
            stateSaved = false;
            resortFreeVariables();
            updateResult();
        }
    }

    private class VariableWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            // Intentionally left empty
        }
        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            // Intentionally left empty
        }
        @Override
        public void afterTextChanged(final Editable s) {
            stateSaved = false;

            for (final CalculatorVariable equ : equations) {
                equ.setCacheDirty();
            }

            updateResult();
        }
    }

    private class CoordinateFormatListener implements AdapterView.OnItemSelectedListener {
        /** One shot marker */
        private boolean shot = false;

        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int pos, final long id) {
            // Ignore first call, which comes from onCreate()
            if (shot) {
                resetCalculator();
            } else {
                shot = true;
            }

            currentFormat = Settings.CoordInputFormatEnum.fromInt(((SpinnerItem) parent.getItemAtPosition(pos)).id);
            Settings.setCoordInputFormat(currentFormat);
            setFormat();

            resortEquations();
            updateResult();
        }
        @Override
        public void onNothingSelected(final AdapterView<?> arg0) {
            // do nothing
        }
    }

    private class HemisphereClickListener implements View.OnClickListener {
        @Override
        public void onClick(final View view) {
            stateSaved = false;

            final Button button = (Button) view;
            final CharSequence text = button.getText();
            if (StringUtils.isBlank(text)) {
                button.setText("*");
                return;
            }
            switch (text.charAt(0)) {
                case 'N':
                    button.setText("S");
                    break;
                case 'S':
                    button.setText("N");
                    break;
                case 'E':
                    button.setText("W");
                    break;
                case 'W':
                    button.setText("E");
                    break;
                default:
                    if (button.equals(bLatHem)) {
                        button.setText("N");
                    } else if (button.equals(bLonHem)) {
                        button.setText("E");
                    } else {
                        button.setText(ERROR_CHAR);
                    }
                    break;
            }
            updateResult();
        }
    }

    /**
     * @param gp               Geopoint representing the coordinates from the CoordinateInputDialog
     * @param calculationState State to set the calculator to when created
     */
    public static CoordinatesCalculateDialog getInstance(final Geopoint gp, final CalcState calculationState) {
        final Bundle args = new Bundle();

        if (gp != null) {
            args.putParcelable(GEOPOINT_ARG, gp);
        }

        final CoordinatesCalculateDialog ccd = new CoordinatesCalculateDialog();
        ccd.setArguments(args);
        ccd.setSavedState(calculationState);
        return ccd;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        equations = new ArrayList<>();
        freeVariables = new ArrayList<>();
        variableBank = new ArrayList<>();
        gp = getArguments().getParcelable(GEOPOINT_ARG);
        if (gp == null) {
            gp = Sensors.getInstance().currentGeo().getCoords();
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.getParcelable(GEOPOINT_ARG) != null) {
                gp = savedInstanceState.getParcelable(GEOPOINT_ARG);
            }

            final byte[] bytes = savedInstanceState.getByteArray(CALC_STATE);
            setSavedState(bytes != null ? (CalcState) SerializationUtils.deserialize(bytes) : null);
        }
    }

    // minimum helper class for workaround until Plain_SingleLine gets supported by coordinates calculator
    private class SpinnerItem {
        public int id;
        public String text;

        SpinnerItem (final int id, final String text) {
            this.id = id;
            this.text = text;
        }

        @NonNull
        @Override
        public String toString() {
            return text;
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final Dialog dialog = getDialog();
        final boolean noTitle = dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final View v = inflater.inflate(R.layout.coordinatescalculate_dialog, container, false);
        final InputDoneListener inputDone = new InputDoneListener();

        if (!noTitle) {
            dialog.setTitle(R.string.cache_coordinates);
        } else {
            final TextView title = v.findViewById(R.id.dialog_title_title);
            if (title != null) {
                title.setText(R.string.cache_calculator);
                title.setVisibility(View.VISIBLE);
            }
            final ImageButton cancel = v.findViewById(R.id.dialog_title_cancel);
            if (cancel != null) {
                cancel.setOnClickListener(new CalculateCancelListener());
                cancel.setVisibility(View.VISIBLE);
            }
            doneButton = v.findViewById(R.id.dialog_title_done);
            if (doneButton != null) {
                doneButton.setOnClickListener(inputDone);
                doneButton.setVisibility(View.VISIBLE);
            }
        }

        spinner = v.findViewById(R.id.spinnerCoordinateFormats);
        /*
        final ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(getActivity(),
                        R.array.waypoint_coordinate_formats,
                        android.R.layout.simple_spinner_item);
        */
        // workaround until Plain_SingleLine format is supported in CoordinatesCalculator:
        final String[] formats = getResources().getStringArray(R.array.waypoint_coordinate_formats);
        final String leaveOut = getString(R.string.waypoint_coordinate_formats_plain_singleline);
        final ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<SpinnerItem>(getActivity(), android.R.layout.simple_spinner_item);
        for (int i = 0; i < formats.length; i++) {
            if (!formats[i].equals(leaveOut)) {
                adapter.add(new SpinnerItem(i, formats[i]));
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new CoordinateFormatListener());

        ePlainLat = v.findViewById(R.id.PlainLat);
        ePlainLon = v.findViewById(R.id.PlainLon);

        bLatHem = v.findViewById(R.id.ButtonLatHem);
        bLatDeg[1] = v.findViewById(R.id.ButtonLatDeg_010);
        bLatDeg[0] = v.findViewById(R.id.ButtonLatDeg_001);
        final TextView tLatDegChar = v.findViewById(R.id.LatDegChar);
        bLatMin[1] = v.findViewById(R.id.ButtonLatMin_10);
        bLatMin[0] = v.findViewById(R.id.ButtonLatMin_01);
        final TextView tLatMinChar = v.findViewById(R.id.LatMinChar);
        bLatSec[1] = v.findViewById(R.id.ButtonLatSec_10);
        bLatSec[0] = v.findViewById(R.id.ButtonLatSec_01);
        bLatPnt[4] = v.findViewById(R.id.ButtonLatPnt_10000);
        bLatPnt[3] = v.findViewById(R.id.ButtonLatPnt_01000);
        bLatPnt[2] = v.findViewById(R.id.ButtonLatPnt_00100);
        bLatPnt[1] = v.findViewById(R.id.ButtonLatPnt_00010);
        bLatPnt[0] = v.findViewById(R.id.ButtonLatPnt_00001);
        final TextView tLatLastUnits = v.findViewById(R.id.LatLastUnitsChar);

        bLonHem = v.findViewById(R.id.ButtonLonHem);
        bLonDeg[2] = v.findViewById(R.id.ButtonLonDeg_100);
        bLonDeg[1] = v.findViewById(R.id.ButtonLonDeg_010);
        bLonDeg[0] = v.findViewById(R.id.ButtonLonDeg_001);
        final TextView tLonDegChar = v.findViewById(R.id.LonDegChar);
        bLonMin[1] = v.findViewById(R.id.ButtonLonMin_10);
        bLonMin[0] = v.findViewById(R.id.ButtonLonMin_01);
        final TextView tLonMinChar = v.findViewById(R.id.LonMinChar);
        bLonSec[1] = v.findViewById(R.id.ButtonLonSec_10);
        bLonSec[0] = v.findViewById(R.id.ButtonLonSec_01);
        bLonPnt[4] = v.findViewById(R.id.ButtonLonPnt_10000);
        bLonPnt[3] = v.findViewById(R.id.ButtonLonPnt_01000);
        bLonPnt[2] = v.findViewById(R.id.ButtonLonPnt_00100);
        bLonPnt[1] = v.findViewById(R.id.ButtonLonPnt_00010);
        bLonPnt[0] = v.findViewById(R.id.ButtonLonPnt_00001);
        final TextView tLonLastUnits = v.findViewById(R.id.LonLastUnitsChar);

        variablesPanel = v.findViewById(R.id.VariablesPanel);
        variablesScrollableContent = v.findViewById(R.id.VariablesScrollpane);
        variableDivider = v.findViewById(R.id.VariableDivider);
        equationGrid = v.findViewById(R.id.EquationTable);
        variableGrid = v.findViewById(R.id.FreeVariableTable);

        tLatResult = v.findViewById(R.id.latRes);
        tLonResult = v.findViewById(R.id.lonRes);

        notes = v.findViewById(R.id.notes_text);
        notes.setText(((EditWaypointActivity) getActivity()).getUserNotes().getText());

        latButtons = Arrays.asList(bLatDeg[1], bLatDeg[0],
                                               bLatMin[1], bLatMin[0],
                                               bLatSec[1], bLatSec[0],
           bLatPnt[4], bLatPnt[3], bLatPnt[2], bLatPnt[1], bLatPnt[0]);

        lonButtons = Arrays.asList(bLonDeg[2], bLonDeg[1], bLonDeg[0],
                                               bLonMin[1], bLonMin[0],
                                               bLonSec[1], bLonSec[0],
           bLonPnt[4], bLonPnt[3], bLonPnt[2], bLonPnt[1], bLonPnt[0]);

        coordButtons = new ArrayList<>(latButtons.size() + lonButtons.size());
        coordButtons.addAll(latButtons);
        coordButtons.addAll(lonButtons);

        minButtons = Arrays.asList(tLatDegChar, bLatMin[1], bLatMin[0],
                                   tLonDegChar, bLonMin[1], bLonMin[0]);
        secButtons = Arrays.asList(tLatMinChar, bLatSec[1], bLatSec[0],
                                   tLonMinChar, bLonSec[1], bLonSec[0]);
        pointLowButtons = Arrays.asList(bLatPnt[4], bLatPnt[3],
                                        bLonPnt[4], bLonPnt[3]);
        lastUnits = Arrays.asList(tLatLastUnits, tLonLastUnits);

        bLatDeg[1].setNextButton(bLatDeg[0])
                  .setNextButton(bLatMin[1])
                  .setNextButton(bLatMin[0])
                  .setNextButton(bLatSec[1])
                  .setNextButton(bLatSec[0])
                  .setNextButton(bLatPnt[4])
                  .setNextButton(bLatPnt[3])
                  .setNextButton(bLatPnt[2])
                  .setNextButton(bLatPnt[1])
                  .setNextButton(bLatPnt[0])

                  .setNextButton(bLonDeg[2])
                  .setNextButton(bLonDeg[1])
                  .setNextButton(bLonDeg[0])
                  .setNextButton(bLonMin[1])
                  .setNextButton(bLonMin[0])
                  .setNextButton(bLonSec[1])
                  .setNextButton(bLonSec[0])
                  .setNextButton(bLonPnt[4])
                  .setNextButton(bLonPnt[3])
                  .setNextButton(bLonPnt[2])
                  .setNextButton(bLonPnt[1])
                  .setNextButton(bLonPnt[0]);

        final Button buttonDone = v.findViewById(R.id.done);
        if (noTitle) {
            buttonDone.setVisibility(View.GONE);
        } else {
            buttonDone.setOnClickListener(inputDone);
        }

        ePlainLat.addTextChangedListener(new PlainWatcher());
        ePlainLon.addTextChangedListener(new PlainWatcher());

        bLatHem.setOnClickListener(new HemisphereClickListener());
        bLonHem.setOnClickListener(new HemisphereClickListener());

        for (final CalculateButton button : coordButtons) {
            button.addClickCompleteCallback(this);
            button.addLongClickCompleteCallback(this);
        }

        return v;
    }

    /**
     * Make this dialog completely fill the screen
     */
    @Override
    public void onStart() {
        super.onStart();
        final Dialog d = getDialog();
        if (d != null) {
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (savedState != null) {
            loadCalcState();
        } else if (currentFormat == null) {
            setCoordFormat(Settings.getCoordInputFormat());
            setButtonInputValuesFromGP();
            resetCalculator();
        }


        resortEquations();
        updateResult();
    }

    /**
     * Save the current state of the calculator such that it can be restored after screen rotation (or similar)
     */
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(CALC_STATE, SerializationUtils.serialize(getCurrentState()));
    }

    private void displayToast(final int message) {
        displayToast(message, false);
    }

    private void displayToast(final int message, final boolean shortToast) {
        final AbstractActivity activity = (AbstractActivity) getActivity();
        if (shortToast) {
            activity.showShortToast(activity.getString(message));
        } else {
            activity.showToast(activity.getString(message));
        }
    }

    private void setButtonInputValuesFromGP() {
        inputLatHem = String.valueOf(gp.getLatDir());
        bLatHem.setText(inputLatHem);
        setCoordValue(gp.getLatDeg(), bLatDeg[0], bLatDeg[1]);
        setCoordValue(gp.getLatMin(), bLatMin[0], bLatMin[1]);
        setCoordValue(gp.getLatSec(), bLatSec[0], bLatSec[1]);

        inputLonHem = String.valueOf(gp.getLonDir());
        bLatHem.setText(inputLonHem);
        setCoordValue(gp.getLonDeg(), bLonDeg[0], bLonDeg[1], bLonDeg[2]);
        setCoordValue(gp.getLonMin(), bLonMin[0], bLonMin[1]);
        setCoordValue(gp.getLonSec(), bLonSec[0], bLonSec[1]);

        switch (currentFormat) {
            case Deg:
                setCoordValue(gp.getLatDegFrac(), bLatPnt[0], bLatPnt[1], bLatPnt[2], bLatPnt[3], bLatPnt[4]);
                setCoordValue(gp.getLonDegFrac(), bLonPnt[0], bLonPnt[1], bLonPnt[2], bLonPnt[3], bLonPnt[4]);
                break;

            case Min:
                setCoordValue(gp.getLatMinFrac(), bLatPnt[0], bLatPnt[1], bLatPnt[2]);
                setCoordValue(gp.getLonMinFrac(), bLonPnt[0], bLonPnt[1], bLonPnt[2]);
                break;

            case Sec:
                setCoordValue(gp.getLatSecFrac(), bLatPnt[0], bLatPnt[1], bLatPnt[2]);
                setCoordValue(gp.getLonSecFrac(), bLonPnt[0], bLonPnt[1], bLonPnt[2]);
                break;
        }
    }

    private String addLeadingZerosToDecimal(final String coordinate, final boolean lat) {
        final Matcher wholeMatcher = PRE_DECIMAL_PATTERN.matcher(coordinate);
        final Matcher decimalMatcher = DECIMAL_PATTERN.matcher(coordinate);

        if (lat) {
            latLeadingZerosAdded = 0;
        } else {
            lonLeadingZerosAdded = 0;
        }

        if (!wholeMatcher.find() || !decimalMatcher.find()) {
            return coordinate;
        }

        final String leadingString = wholeMatcher.group(1);
        String decimalsString = decimalMatcher.group(1);
        final Matcher trailingMatcher = TRAILING_PATTERN.matcher(coordinate);
        final String trailingString = trailingMatcher.find() ? trailingMatcher.group(1) : "";

        final int decimalPoints;
        switch (currentFormat) {
            case Deg:
                decimalPoints = 5;
                break;

            case Min:
            case Sec:
            case Plain:
            default:
                decimalPoints = 3;
        }

        while (decimalsString.length() < decimalPoints) {
            decimalsString = "0".concat(decimalsString);
            if (lat) {
                latLeadingZerosAdded++;
            } else {
                lonLeadingZerosAdded++;
            }
        }

        return leadingString + "." + decimalsString + trailingString;
    }

    /**
     * This method has largely been lifted from the CoordinateInputDialog and is used to validate
     * coordinate specifications prior to assigning them to the Geopoint.
     */
    private boolean areCurrentCoordinatesValid() {
        // convert text to geopoint
        final Geopoint current;
        String lat = getLatResult();
        String lon = getLonResult();

        if (currentFormat == null || lat.contains("_") || lat.contains("*") || lon.contains("_") || lon.contains("*")) {
            return false;
        }

        // Pad decimal field with leading zeros
        lat = addLeadingZerosToDecimal(lat, true);
        lon = addLeadingZerosToDecimal(lon, false);

        try {
            current = new Geopoint(lat, lon);

            if (current.isValid()) {
                gp = current;
                return true;
            }
        } catch (final Geopoint.ParseException ignored) {
            // Signaled and returned below
        }

        return false;
    }

    // FIXME: Why do we have this method? The field savedState is never evaluated anywhere...
    private void setSavedState(final CalcState savedState) {
        this.savedState = savedState;
        stateSaved = true;
    }

    private void loadCalcState() {
        setCoordFormat(savedState.format);
        final List<CalculateButton.ButtonData> buttons = savedState.buttons;

        bLatHem.setText(String.valueOf(savedState.latHemisphere));
        bLonHem.setText(String.valueOf(savedState.lonHemisphere));

        int i = 0;
        CalculateButton b = bLatDeg[1];
        while (b != null && i < buttons.size()) {
            b.setData(buttons.get(i++));
            b = b.getNextButton();
        }

        if (BuildConfig.DEBUG && b == null && i != buttons.size()) {
            throw new AssertionError("Number of ButtonData objects differ from the number of Buttons");
        }

        for (final CalculatorVariable.VariableData equ : savedState.equations) {
            equations.add(new CalculatorVariable(getContext(),
                    equ,
                    getString(R.string.equation_hint),
                    new EquationWatcher()));
        }

        for (final CalculatorVariable.VariableData var : savedState.freeVariables) {
            freeVariables.add(new CalculatorVariable(getContext(),
                    var,
                    getString(R.string.free_variable_hint),
                    new VariableWatcher()));
        }

        variableBank.addAll(savedState.variableBank);

        // Text must be set after Equations have been loaded as the TextWatcher will be triggered when the text is set
        ePlainLat.setText(savedState.plainLat);
        ePlainLon.setText(savedState.plainLon);
    }

    private CalcState getCurrentState() {
        final List<CalculateButton.ButtonData> butData = new ArrayList<>(coordButtons.size());
        final List<CalculatorVariable.VariableData> equData = new ArrayList<>(equations.size());
        final List<CalculatorVariable.VariableData> freeVarData = new ArrayList<>(freeVariables.size());
        final List<CalculatorVariable.VariableData> varBankData = new ArrayList<>(variableBank);

        final char latHem;
        final char lonHem;

        if (bLatHem.getText().toString().length() > 0) {
            latHem = bLatHem.getText().toString().charAt(0);
        } else {
            latHem = 'N';
        }

        if (bLonHem.getText().toString().length() > 0) {
            lonHem = bLonHem.getText().toString().charAt(0);
        } else {
            lonHem = 'W';
        }

        CalculateButton b = bLatDeg[1];
        while (b != null) {
            butData.add(b.getData());
            b = b.getNextButton();
        }

        for (final CalculatorVariable equ : equations) {
            equData.add(equ.getData());
        }

        for (final CalculatorVariable var : freeVariables) {
            freeVarData.add(var.getData());
        }

        return new CalcState(currentFormat,
                             ePlainLat.getText().toString(),
                             ePlainLon.getText().toString(),
                             latHem,
                             lonHem,
                             butData,
                             equData,
                             freeVarData,
                             varBankData);
    }

    private static void setCoordValue(final int val,
                                      final CalculateButton units,
                                      final CalculateButton tens,
                                      final CalculateButton hundreds,
                                      final CalculateButton thousands,
                                      final CalculateButton tenThousands) {
        int valCopy = val;
        tenThousands.setInputVal(String.valueOf(valCopy / 10000));

        valCopy %= 10000;
        thousands.setInputVal(String.valueOf(valCopy / 1000));

        setCoordValue(valCopy % 1000, units, tens, hundreds);
    }

    private static void setCoordValue(final int val,
                                      final CalculateButton units,
                                      final CalculateButton tens,
                                      final CalculateButton hundreds) {
        hundreds.setInputVal(String.valueOf(val / 100));
        setCoordValue(val, units, tens);
    }

    private static void setCoordValue(final int val,
                                      final CalculateButton units,
                                      final CalculateButton tens) {
        int valCopy = val % 100;
        tens.setInputVal(String.valueOf(valCopy / 10));

        valCopy = valCopy % 10;
        units.setInputVal(String.valueOf(valCopy));
    }

    private void setVisible(@IdRes final int viewId, final int visibility) {
        final View view = getView();
        assert view != null;
        view.findViewById(viewId).setVisibility(visibility);
    }

    private void setCoordFormat(final Settings.CoordInputFormatEnum currentFormat) {
        this.currentFormat = currentFormat;
        spinner.setSelection(currentFormat.ordinal(), false);
        setFormat();
    }

    /**
     * Format the given string into the appropriate 'pretty' representation:  42ABCDE -> 42° AB.CDE'
     *
     * @param values The string of values to be formatted.
     */
    private String format(final String values, final int degDigits) {
        if (currentFormat == null) {
            return "";
        }
        final String returnValue;

        switch (currentFormat) {
            case Plain:
                returnValue = values;
                break;

            case Sec:
                returnValue = " " + values.substring(0, degDigits) + SYMBOL_DEG
                            + " " + values.substring(degDigits, degDigits + 2) + SYMBOL_MIN
                            + " " + values.substring(degDigits + 2, degDigits + 4) + SYMBOL_POINT
                                  + values.substring(degDigits + 4) + SYMBOL_SEC;
                break;

            case Min:
                returnValue = " " + values.substring(0, degDigits) + SYMBOL_DEG
                            + " " + values.substring(degDigits, degDigits + 2) + SYMBOL_POINT
                                  + values.substring(degDigits + 2) + SYMBOL_MIN;
                break;

            case Deg:
                returnValue = " " + values.substring(0, degDigits) + SYMBOL_POINT
                                  + values.substring(degDigits) + SYMBOL_DEG;
                break;

            default:
                returnValue = "***";
        }

        return returnValue;
    }

    String evaluateBrackets(final String original) {
        String returnValue = original;
        int openIndex;
        int closeIndex;

        try {
            for (int bracketIndex = 0; bracketIndex < BRACKET_OPENINGS.length; bracketIndex++) {
                for (int returnValueIndex = 0; returnValueIndex < returnValue.length(); returnValueIndex++) {
                    char ch = returnValue.charAt(returnValueIndex);

                    if (ch == BRACKET_OPENINGS[bracketIndex]) {
                        int nestedBrackerCount = 1;
                        openIndex = returnValueIndex;
                        closeIndex = returnValueIndex;

                        while (nestedBrackerCount > 0 && closeIndex < returnValue.length() - 1) {
                            closeIndex++;
                            ch = returnValue.charAt(closeIndex);

                            if (ch == BRACKET_OPENINGS[bracketIndex]) {
                                nestedBrackerCount++;
                            } else if (ch == BRACKET_CLOSINGS[bracketIndex]) {
                                nestedBrackerCount--;
                            }
                        }

                        if (nestedBrackerCount == 0) {
                            String result = "";

                            if (closeIndex > openIndex + 1) {
                                final int resInt = (int) (new CalculationUtils(returnValue.substring(openIndex + 1, closeIndex)).eval());
                                result = String.valueOf(resInt);
                            }

                            returnValue = returnValue.substring(0, openIndex) + result + returnValue.substring(closeIndex + 1, returnValue.length());
                        } else {
                            // Reached end without finding enough closing brackets
                            throw new IllegalArgumentException("Unmatched opening bracket '" + returnValue.charAt(openIndex) + "' at index " + openIndex + " of \"" + returnValue + "\"/");
                        }
                    } else if (ch == BRACKET_CLOSINGS[bracketIndex]) {
                        // Negative nested bracket count.
                        throw new IllegalArgumentException("Unmatched closing bracket '" + ch + "' at index " + returnValueIndex + " of \"" + returnValue + "\"/");
                    }
                }
            }
        } catch (final Exception e) {
            // section can't be evaluated
            returnValue = original;
        }

        return returnValue;
    }

    /**
     * Replace 'equation' variables with their computed values: 42° AB.CDE' -> 42° 12.345'
     *
     * @param values The string to perform the substitutions on
     * @return String with the substitutions performed
     */
    private String substituteVariables(final String values) {
        String returnValue = "";

        if (values.length() > 0) {
            final char first = values.charAt(0);
            String substitutionString;

            // Trim of the leading hemisphere character if it exists.
            if (first == 'N' || first == 'S' || first == 'E' || first == 'W') {
                returnValue = returnValue.concat(String.valueOf(first));
                substitutionString = values.substring(1);
            } else {
                substitutionString = values;
            }

            // Perform the substitutions on the remainder of the string.
            for (final CalculatorVariable equ : equations) {
                substitutionString = substitutionString.replace(String.valueOf(equ.getName()), equ.evaluateString(freeVariables));
            }

            // If the string contains matching brackets evaluate the enclosed expression (for use in PLANE format)
            substitutionString = evaluateBrackets(substitutionString);

            // Recombine the hemisphere and substituted string.
            returnValue = returnValue.concat(substitutionString);
        }

        // Remove placeholder characters.
        returnValue = returnValue.replaceAll(PLACE_HOLDER, "");

        // Break up connecting underscores
        while (returnValue.contains("__")) {
            returnValue = returnValue.replace("__", "_ _");
        }

        return returnValue;
    }

    /**
     * Retrieve all the values from the calculation buttons
     *
     * Note that a special 'place-holder' character is used to represent "blanked-out" buttons.
     * This is needed to preserve formatting when multi digit calculations are used.
     *
     * @param buttons List of button from which to extract the values
     * @return Button values as a string
     */
    private String getValues(final List<CalculateButton> buttons, final int degDigits) {
        String returnValue = "";
        for (final EditButton button : buttons) {
            // Remove inactive and blank digits from result
            if (button.getVisibility() == View.VISIBLE) {
                if (button.getLabel() == CalculateButton.ButtonData.BLANK) {
                    returnValue = returnValue.concat(PLACE_HOLDER);
                } else {
                    returnValue = returnValue.concat(String.valueOf(button.getLabel()));
                }
            }
        }

        // Formatting intentionally done first in case the substitution changes number of characters.
        return format(returnValue, degDigits);
    }

    /**
     * Retrieves the current representation of the computed latitudinal result
     *
     * @return The result as a string
     */
    private String getLatResult() {
        final String returnValue;

        if (currentFormat == Settings.CoordInputFormatEnum.Plain) {
            returnValue = ePlainLat.getText().toString();
        } else {
            returnValue = bLatHem.getText() + getValues(latButtons, 2);
        }

        return substituteVariables(returnValue);
    }

    /**
     * Retrieves the current representation of the computed longitudinal result
     *
     * @return The result as a string
     */
    private String getLonResult() {
        final String returnValue;

        if (currentFormat == Settings.CoordInputFormatEnum.Plain) {
            returnValue = ePlainLon.getText().toString();
        } else {
            returnValue =  bLonHem.getText() + getValues(lonButtons, 3);
        }

        return substituteVariables(returnValue);
    }

    /**
     * Updates the supplies with the list of variables supplies
     *
     * The ID's are used to assign an ordering such that the 'next' button on the keyboard (">")
     * will take you to the appropriate variable.
     *
     * @param variables Variables to be included in the table
     * @param grid The table the variables are to be placed in
     * @param startId ID to be used for the first variable in the list
     */
    private void updateGrid(final List<CalculatorVariable> variables, final GridLayout grid, final int startId) {
        int id = startId;

        // If 'freeVariables' are to be displayed include a border around the tables
        // such that it becomes apparent that their is a second table which may be off the screen.
        if (freeVariables.isEmpty()) {
            variablesScrollableContent.setBackgroundResource(0);
            variableDivider.setVisibility(View.GONE);
            variablesPanel.setFillViewport(true);
        } else {
            variablesScrollableContent.setBackgroundResource(R.drawable.border);
            variableDivider.setVisibility(View.VISIBLE);
            variablesPanel.setFillViewport(false);
        }

        grid.removeAllViews();
        grid.setRowCount((variables.size() + 1) / 2);

        CalculatorVariable prev = null;
        for (final CalculatorVariable v : variables) {
            v.setExpressionId(id++);
            if (prev != null) {
                prev.setNextFocusDownId(v.getExpressionId());
            }

            v.setLayoutParams(new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)));
            grid.addView(v);

            prev = v;
        }
    }

    private void updateResult() {
        final Spannable latFormatted;
        final Spannable lonFormatted;

        final boolean lightSkin = Settings.isLightSkin();
        final int validColour = ContextCompat.getColor(getContext(), lightSkin ? R.color.text_light : R.color.text_dark);
        final int invalidColour = ContextCompat.getColor(getContext(), lightSkin ? R.color.text_hint_light : R.color.text_hint_dark);
        final int resultColour;

        if (areCurrentCoordinatesValid()) {
            resultColour = validColour;

            final String lat;
            final String lon;

            switch (currentFormat) {

                case Deg:
                    lat = gp.format(GeopointFormatter.Format.LAT_DECDEGREE);
                    lon = gp.format(GeopointFormatter.Format.LON_DECDEGREE);
                    break;

                case Min:
                case Plain:
                    lat = gp.format(GeopointFormatter.Format.LAT_DECMINUTE);
                    lon = gp.format(GeopointFormatter.Format.LON_DECMINUTE);
                    break;

                case Sec:
                    lat = gp.format(GeopointFormatter.Format.LAT_DECMINSEC);
                    lon = gp.format(GeopointFormatter.Format.LON_DECMINSEC);
                    break;

                default:
                    lat = getLatResult();
                    lon = getLonResult();
                    break;
            }

            latFormatted = formatCoordinateString(lat, latLeadingZerosAdded, invalidColour);
            lonFormatted = formatCoordinateString(lon, lonLeadingZerosAdded, invalidColour);
        } else {
            resultColour = invalidColour;

            latFormatted = new SpannableString(getLatResult());
            lonFormatted = new SpannableString(getLonResult());
        }

        tLatResult.setText(latFormatted);
        tLonResult.setText(lonFormatted);
        tLatResult.setTextColor(resultColour);
        tLonResult.setTextColor(resultColour);
    }

    private Spannable formatCoordinateString(final String coordinateString, final int leadingZeros, final int paddingColour) {
        final Spannable returnValue = new SpannableString(coordinateString);

        if (leadingZeros > 0) {
            final Pattern pat = Pattern.compile("[\\.,]");
            final Matcher match = pat.matcher(coordinateString);
            if (match.find()) {
                final int point = match.start() + 1;
                returnValue.setSpan(new ForegroundColorSpan(paddingColour), point, point + leadingZeros, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }
        }

        return returnValue;
    }

    private void setFormat() {
        switch (currentFormat) {
            case Plain:
                setVisible(coordTable, View.GONE);
                setVisible(PlainFormat, View.VISIBLE);
                break;

            case Deg: // DDD.DDDDD°
                setVisible(coordTable, View.VISIBLE);
                setVisible(PlainFormat, View.GONE);

                for (final View view : minButtons) {
                    view.setVisibility(View.GONE);
                }
                for (final View view : secButtons) {
                    view.setVisibility(View.GONE);
                }
                for (final EditButton button : pointLowButtons) {
                    button.setVisibility(View.VISIBLE);
                }
                for (final TextView view : lastUnits) {
                    view.setText(SYMBOL_DEG);
                }

                setCoordValue(gp.getLatDegFrac(), bLatPnt[0], bLatPnt[1], bLatPnt[2], bLatPnt[3], bLatPnt[4]);
                setCoordValue(gp.getLonDegFrac(), bLonPnt[0], bLonPnt[1], bLonPnt[2], bLonPnt[3], bLonPnt[4]);
                break;

            case Min: // DDD° MM.MMM'
                setVisible(coordTable, View.VISIBLE);
                setVisible(PlainFormat, View.GONE);

                for (final View view : minButtons) {
                    view.setVisibility(View.VISIBLE);
                }
                for (final View view : secButtons) {
                    view.setVisibility(View.GONE);
                }
                for (final EditButton button : pointLowButtons) {
                    button.setVisibility(View.GONE);
                }
                for (final TextView view : lastUnits) {
                    view.setText(SYMBOL_MIN);
                }

                setCoordValue(gp.getLatMinFrac(), bLatPnt[0], bLatPnt[1], bLatPnt[2]);
                setCoordValue(gp.getLonMinFrac(), bLonPnt[0], bLonPnt[1], bLonPnt[2]);
                break;

            case Sec: // DDD° MM' SS.SSS"
                setVisible(coordTable, View.VISIBLE);
                setVisible(PlainFormat, View.GONE);

                for (final View view : minButtons) {
                    view.setVisibility(View.VISIBLE);
                }
                for (final View view : secButtons) {
                    view.setVisibility(View.VISIBLE);
                }
                for (final EditButton button : pointLowButtons) {
                    button.setVisibility(View.GONE);
                }
                for (final TextView view : lastUnits) {
                    view.setText(SYMBOL_SEC);
                }

                setCoordValue(gp.getLatSecFrac(), bLatPnt[0], bLatPnt[1], bLatPnt[2]);
                setCoordValue(gp.getLonSecFrac(), bLonPnt[0], bLonPnt[1], bLonPnt[2]);
                break;
        }
    }

    /**
     * Find if a variable exists in the supplied list with the given name
     *
     * @param name name to search for
     * @param list list of variables
     * @return first occurrence of the variable if it can found, 'null' otherwise
     */
    private CalculatorVariable getVariable(final char name, final List<CalculatorVariable> list, final boolean remove) {
        for (final CalculatorVariable equ : list) {
            if (equ.getName() == name) {
                if (remove) {
                    list.remove(equ);
                }
                return equ;
            }
        }

        return null;
    }

    /**
     * Find if variable data exists in the supplied list with the given name
     *
     * @param name name to search for
     * @return first occurrence of the data if it can found, 'null' otherwise
     */
    private CalculatorVariable.VariableData findAndRemoveData(final char name, final List<CalculatorVariable.VariableData> list) {
        for (final CalculatorVariable.VariableData var : list) {
            if (var.getName() == name) {
                list.remove(var);
                return var;
            }
        }

        return null;
    }

    /**
     * Create a list of variables in the order provided by the 'variableNames' string
     *
     * The purpose of this method is to create a list of all the variables needed to satisfy all the characters the supplied 'names' and no more.
     * The list returned will return the variables in the order in which they first appear in the 'names' string.
     * This method is responsible for using and maintaining the 'variableBank'.
     *
     * @param variables List of variables as they currently are.
     * @param variableNames String containing all the names for which variables are required
     * @param theCase case for which variables are to be created (ie. Uppercase or Lowercase)
     * @param hintText text to be used as a hint when new variables are created
     * @return full list of variables in the appropriate order
     */
    private List<CalculatorVariable> sortVariables(final List<CalculatorVariable> variables,
                                                   final String variableNames,
                                                   final CaseCheck theCase,
                                                   final String hintText,
                                                   final TextWatcher textWatcher) {
        final List<CalculatorVariable> returnList = new ArrayList<>();

        final char[] sortedVariables = variableNames.toCharArray();
        Arrays.sort(sortedVariables);

        for (final char ch : sortedVariables) {
            if (theCase.check(ch)) {
                if (getVariable(ch, returnList, false) != null) {
                    continue;
                }

                CalculatorVariable thisEquation = getVariable(ch, variables, true);
                if (thisEquation == null) {
                    CalculatorVariable.VariableData data = findAndRemoveData(ch, variableBank);

                    if (data == null) {
                        data = new CalculatorVariable.VariableData(ch);
                    }

                    thisEquation = new CalculatorVariable(getContext(),
                            data,
                            hintText,
                            textWatcher);
                }

                returnList.add(thisEquation);
            }
        }

        // Add all the left over equations to the variable bank.
        for (final CalculatorVariable var : variables) {
            variableBank.add(var.getData());
        }

        return returnList;
    }

    /**
     * Re-sort the equations into the order in which they first appear in the 'buttons' or 'plain-text' fields as appropriate
     */
    private void resortEquations() {
        String coordinateChars = ""; // All the characters that appear in the coordinate representation.

        if (currentFormat == Settings.CoordInputFormatEnum.Plain) {
            final String lat = ePlainLat.getText().toString();
            final String lon = ePlainLon.getText().toString();

            if (lat.length() > 0) {
                final char first = lat.charAt(0);
                if (first == 'N' || first == 'S') {
                    coordinateChars = coordinateChars.concat(lat.substring(1));
                } else {
                    coordinateChars = coordinateChars.concat(lat);
                }
            }

            if (lon.length() > 0) {
                final char first = lon.charAt(0);
                if (first == 'E' || first == 'W') {
                    coordinateChars = coordinateChars.concat(lon.substring(1));
                } else {
                    coordinateChars = coordinateChars.concat(lon);
                }
            }
        } else {
            for (final EditButton b : coordButtons) {
                coordinateChars = coordinateChars.concat(String.valueOf(b.getLabel()));
            }
        }

        // replace the old equation list with a newly created ones
        equations = sortVariables(equations, coordinateChars, new CaseCheck(true), getString(R.string.equation_hint), new EquationWatcher());
        updateGrid(equations, equationGrid, 0);
        resortFreeVariables();
    }

    /**
     * Resort the free-variables into the order in which they first appear in the 'equations'
     */
    private void resortFreeVariables() {
        String equationStrings = "";

        for (final CalculatorVariable equ : equations) {
            equationStrings = equationStrings.concat(equ.getExpression());
        }

        // replace the old free variables list with a newly created ones.
        freeVariables = sortVariables(freeVariables, equationStrings, new CaseCheck(false), getString(R.string.free_variable_hint), new VariableWatcher());
        updateGrid(freeVariables, variableGrid, equations.size());
    }

    private void resetCalculator() {
        // Reset the text in the Plain format EditTexts
        ePlainLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
        ePlainLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));

        bLatHem.setText(inputLatHem);
        bLonHem.setText(inputLonHem);

        // Resetting the 'first' button causes all subsequent buttons to be reset as well
        bLatDeg[1].resetButton();
    }

    /**
     * This method displays a reminder about saving the calculator state then closes both this dialog as well as the 'CoordinateInputDialog'
     *
     * Note that clicking the back arrow on the device does not run this method so in that case the user will be returned to the 'CoordinateInputDialog'.
     */
    private void close() {
        if (savedState != null) {
            displayToast(R.string.warn_calculator_state_save);
        }

        dismiss();
    }
}
