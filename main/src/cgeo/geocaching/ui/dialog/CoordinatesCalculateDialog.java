package cgeo.geocaching.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.BuildConfig;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.GridLayout;
import android.text.Editable;
import android.text.TextWatcher;
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

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.CalcState;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.CalculateButton;
import cgeo.geocaching.ui.EditButton;
import cgeo.geocaching.ui.CalculatorVariable;

import static cgeo.geocaching.R.id.PlainFormat;
import static cgeo.geocaching.R.id.coordTable;
import static cgeo.geocaching.models.CalcState.ERROR_CHAR;

public class CoordinatesCalculateDialog extends DialogFragment {

    private static final String PLACE_HOLDER = "~"; // Character used to represent a "blanked-out' CoordinateButton.

    static final String SYMBOL_DEG = "°";
    static final String SYMBOL_MIN = "'";
    static final String SYMBOL_SEC = "\"";
    static final String SYMBOL_POINT = ".";

    private Geopoint gp;
    private CalcState calcState;

    // Throught this implementation:
    //
    // 'Equations' are used to represent 'Variables' that appear in the description of the cache coordinated themselves.
    //             As in "N 42° 127.ABC".  In this example 'A', 'B' and 'C' are all 'equations'.
    //             All 'equations' must have a CAPITAL-LETTER name.
    //
    // 'FreeVariables' are used to represent 'Variables' that appear in the 'expression' of an equation
    //                 As in "X = a^2 + b^2".  In this example 'a' and 'b' are both 'freeVariables'.
    //                 All 'freeVariables' must have a lower-case name.

    List<CalculatorVariable> equations;       // List of equations to be displayed in the calculator.
    List<CalculatorVariable> freeVariables;   // List of freeVariables to be displayed in the calculator.

    Spinner spinner;

    private String inputLatHem;
    private Button bLatHem;  // Latitude hemisphere (North or South)
    private CalculateButton         bLatDeg_100,   bLatDeg_010,   bLatDeg_001,
                                                    bLatMin_10,    bLatMin_01,
                                                    bLatSec_10,    bLatSec_01,
    bLatPnt_10000, bLatPnt_01000, bLatPnt_00100, bLatPnt_00010, bLatPnt_00001;

    private String inputLonHem;
    private Button bLonHem;  // Longitude hemisphere (East or West)
    private CalculateButton         bLonDeg_100,   bLonDeg_010,   bLonDeg_001,
                                                    bLonMin_10,    bLonMin_01,
                                                    bLonSec_10,    bLonSec_01,
    bLonPnt_10000, bLonPnt_01000, bLonPnt_00100, bLonPnt_00010, bLonPnt_00001;

    private TextView tLatDegChar, tLatMinChar, tLatLastUnits,
                     tLonDegChar, tLonMinChar, tLonLastUnits;

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
    private List<View> degButtons;
    private List<View> minButtons;
    private List<View> secButtons;
    private List<CalculateButton> pointLowButtons;
    private List<TextView> lastUnits;


    private static final String GEOPOINT_ARG = "GEOPOINT";

    private class PlainWarcher implements TextWatcher {
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
            for (final CalculatorVariable equ : equations) {
                equ.setCacheDirty();
            }

            updateResult();
        }
    }

    private class InputDoneListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {

            // Save calculator state regardless of weather the coordinates are valid or not.
            ((CoordinatesInputDialog.CalculateState) getActivity()).saveCalculatorState(saveState());
            displayToast(R.string.info_calculator_state_saved, true);


            if (!areCurrentCoordinatesValid(true)) {
                return;
            }

            if (gp != null) {
                ((CoordinatesInputDialog.CoordinateUpdate) getActivity()).updateCoordinates(gp);
            }

            close();
        }

    }
    private class InputCancelListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            close();
        }

    }
    private class CoordinateFormatListener implements AdapterView.OnItemSelectedListener {

        // One Shot marker
        private boolean shot = false;

        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int pos, final long id) {

            // Ignore first call, which comes from onCreate()
            if (shot) {
                resetCalculator();
            } else {
                shot = true;
            }

            currentFormat = Settings.CoordInputFormatEnum.fromInt(pos);
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
     *
     * @param gp: Geopoint representing the coordinated from the CoordinateInputDialog
     * @param calculationState: State to set the calculator to when created
     */
    public static CoordinatesCalculateDialog getInstance(final Geopoint gp, final CalcState calculationState) {

        final Bundle args = new Bundle();

        if (gp != null) {
            args.putParcelable(GEOPOINT_ARG, gp);
        }

        final CoordinatesCalculateDialog cid = new CoordinatesCalculateDialog();
        cid.setArguments(args);
        cid.setCalcState(calculationState);
        return cid;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        equations = new ArrayList<>();
        freeVariables = new ArrayList<>();
        gp = getArguments().getParcelable(GEOPOINT_ARG);
        if (gp == null) {
            gp = Sensors.getInstance().currentGeo().getCoords();
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.getParcelable(GEOPOINT_ARG) != null) {
                gp = savedInstanceState.getParcelable(GEOPOINT_ARG);
            }

            final byte[] bytes = savedInstanceState.getByteArray("calc_state");
            if (bytes != null) {
                calcState = SerializationUtils.deserialize(bytes);
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final Dialog dialog = getDialog();
        final boolean noTitle = dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final View v = inflater.inflate(R.layout.coordinatescalculate_dialog, container, false);
        final InputDoneListener inputdone = new InputDoneListener();

        if (!noTitle) {
            dialog.setTitle(R.string.cache_coordinates);
        } else {
            final TextView title = ButterKnife.findById(v, R.id.dialog_title_title);
            if (title != null) {
                title.setText(R.string.cache_coordinates);
                title.setVisibility(View.VISIBLE);
            }
            final ImageButton cancel = ButterKnife.findById(v, R.id.dialog_title_cancel);
            if (cancel != null) {
                cancel.setOnClickListener(new InputCancelListener());
                cancel.setVisibility(View.VISIBLE);
            }
            final ImageButton done = ButterKnife.findById(v, R.id.dialog_title_done);
            if (done != null) {
                done.setOnClickListener(inputdone);
                done.setVisibility(View.VISIBLE);
            }
        }

        spinner = ButterKnife.findById(v, R.id.spinnerCoordinateFormats);
        final ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(getActivity(),
                        R.array.waypoint_coordinate_formats,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new CoordinateFormatListener());

        ePlainLat = ButterKnife.findById(v, R.id.PlainLat);
        ePlainLon = ButterKnife.findById(v, R.id.PlainLon);

        bLatHem = ButterKnife.findById(v, R.id.ButtonLatHem);
        bLatDeg_100 = ButterKnife.findById(v, R.id.ButtonLatDeg_100);
        bLatDeg_010 = ButterKnife.findById(v, R.id.ButtonLatDeg_010);
        bLatDeg_001 = ButterKnife.findById(v, R.id.ButtonLatDeg_001);
        tLatDegChar = ButterKnife.findById(v, R.id.LatDegChar);
        bLatMin_10 = ButterKnife.findById(v, R.id.ButtonLatMin_10);
        bLatMin_01 = ButterKnife.findById(v, R.id.ButtonLatMin_01);
        tLatMinChar = ButterKnife.findById(v, R.id.LatMinChar);
        bLatSec_10 = ButterKnife.findById(v, R.id.ButtonLatSec_10);
        bLatSec_01 = ButterKnife.findById(v, R.id.ButtonLatSec_01);
        bLatPnt_10000 = ButterKnife.findById(v, R.id.ButtonLatPnt_10000);
        bLatPnt_01000 = ButterKnife.findById(v, R.id.ButtonLatPnt_01000);
        bLatPnt_00100 = ButterKnife.findById(v, R.id.ButtonLatPnt_00100);
        bLatPnt_00010 = ButterKnife.findById(v, R.id.ButtonLatPnt_00010);
        bLatPnt_00001 = ButterKnife.findById(v, R.id.ButtonLatPnt_00001);
        tLatLastUnits = ButterKnife.findById(v, R.id.LatLastUnitsChar);

        bLonHem = ButterKnife.findById(v, R.id.ButtonLonHem);
        bLonDeg_100 = ButterKnife.findById(v, R.id.ButtonLonDeg_100);
        bLonDeg_010 = ButterKnife.findById(v, R.id.ButtonLonDeg_010);
        bLonDeg_001 = ButterKnife.findById(v, R.id.ButtonLonDeg_001);
        tLonDegChar = ButterKnife.findById(v, R.id.LonDegChar);
        bLonMin_10 = ButterKnife.findById(v, R.id.ButtonLonMin_10);
        bLonMin_01 = ButterKnife.findById(v, R.id.ButtonLonMin_01);
        tLonMinChar = ButterKnife.findById(v, R.id.LonMinChar);
        bLonSec_10 = ButterKnife.findById(v, R.id.ButtonLonSec_10);
        bLonSec_01 = ButterKnife.findById(v, R.id.ButtonLonSec_01);
        bLonPnt_10000 = ButterKnife.findById(v, R.id.ButtonLonPnt_10000);
        bLonPnt_01000 = ButterKnife.findById(v, R.id.ButtonLonPnt_01000);
        bLonPnt_00100 = ButterKnife.findById(v, R.id.ButtonLonPnt_00100);
        bLonPnt_00010 = ButterKnife.findById(v, R.id.ButtonLonPnt_00010);
        bLonPnt_00001 = ButterKnife.findById(v, R.id.ButtonLonPnt_00001);
        tLonLastUnits = ButterKnife.findById(v, R.id.LonLastUnitsChar);

        variablesPanel = ButterKnife.findById(v, R.id.VariablesPanel);
        variablesScrollableContent = ButterKnife.findById(v, R.id.VariablesScrollpane);
        variableDivider = ButterKnife.findById(v, R.id.VariableDivider);
        equationGrid = ButterKnife.findById(v, R.id.EquationTable);
        variableGrid = ButterKnife.findById(v, R.id.FreeVariableTable);

        tLatResult = ButterKnife.findById(v, R.id.latRes);
        tLonResult = ButterKnife.findById(v, R.id.lonRes);

        notes = ButterKnife.findById(v, R.id.notes_text);

        inputLatHem = gp.getLatitude() > 0 ? "N" : "S";
        bLatHem.setText(inputLatHem);
        setCoordValue(gp.getLatDeg(), bLatDeg_100, bLatDeg_010, bLatDeg_001);
        setCoordValue(gp.getLatMin(), bLatMin_10, bLatMin_01);
        setCoordValue(gp.getLatSec(), bLatSec_10, bLatSec_01);

        inputLonHem = gp.getLatitude() > 0 ? "E" : "W";
        bLatHem.setText(inputLonHem);
        setCoordValue(gp.getLonDeg(), bLonDeg_100, bLonDeg_010, bLonDeg_001);
        setCoordValue(gp.getLonMin(), bLonMin_10, bLonMin_01);
        setCoordValue(gp.getLonSec(), bLonSec_10, bLonSec_01);

        latButtons = Arrays.asList(bLatDeg_100, bLatDeg_010, bLatDeg_001,
                                                 bLatMin_10,  bLatMin_01,
                                                 bLatSec_10,  bLatSec_01,
        bLatPnt_10000, bLatPnt_01000, bLatPnt_00100, bLatPnt_00010, bLatPnt_00001);

        lonButtons = Arrays.asList(bLonDeg_100, bLonDeg_010, bLonDeg_001,
                                                 bLonMin_10,  bLonMin_01,
                                                 bLonSec_10,  bLonSec_01,
        bLonPnt_10000, bLonPnt_01000, bLonPnt_00100, bLonPnt_00010, bLonPnt_00001);

        coordButtons = new ArrayList<>(latButtons.size() + lonButtons.size());
        coordButtons.addAll(latButtons);
        coordButtons.addAll(lonButtons);

        minButtons = Arrays.asList(tLatDegChar, bLatMin_10, bLatMin_01,
                                   tLonDegChar, bLonMin_10, bLonMin_01);
        secButtons = Arrays.asList(tLatMinChar, bLatSec_10, bLatSec_01,
                                   tLonMinChar, bLonSec_10, bLonSec_01);
        pointLowButtons = Arrays.asList(bLatPnt_00010, bLatPnt_00001,
                                        bLonPnt_00010, bLonPnt_00001);
        lastUnits = Arrays.asList(tLatLastUnits, tLonLastUnits);

        bLatDeg_100.setNextButton(bLatDeg_010);
        bLatDeg_010.setNextButton(bLatDeg_001);
        bLatDeg_001.setNextButton(bLatMin_10);
        bLatMin_10.setNextButton(bLatMin_01);
        bLatMin_01.setNextButton(bLatSec_10);
        bLatSec_10.setNextButton(bLatSec_01);
        bLatSec_01.setNextButton(bLatPnt_10000);
        bLatPnt_10000.setNextButton(bLatPnt_01000);
        bLatPnt_01000.setNextButton(bLatPnt_00100);
        bLatPnt_00100.setNextButton(bLatPnt_00010);
        bLatPnt_00010.setNextButton(bLatPnt_00001);
        bLatPnt_00001.setNextButton(bLonDeg_100);

        bLonDeg_100.setNextButton(bLonDeg_010);
        bLonDeg_010.setNextButton(bLonDeg_001);
        bLonDeg_001.setNextButton(bLonMin_10);
        bLonMin_10.setNextButton(bLonMin_01);
        bLonMin_01.setNextButton(bLonSec_10);
        bLonSec_10.setNextButton(bLonSec_01);
        bLonSec_01.setNextButton(bLonPnt_10000);
        bLonPnt_10000.setNextButton(bLonPnt_01000);
        bLonPnt_01000.setNextButton(bLonPnt_00100);
        bLonPnt_00100.setNextButton(bLonPnt_00010);
        bLonPnt_00010.setNextButton(bLonPnt_00001);

        final Button buttonDone = ButterKnife.findById(v, R.id.done);
        if (noTitle) {
            buttonDone.setVisibility(View.GONE);
        } else {
            buttonDone.setOnClickListener(inputdone);
        }

        loadCalcState();
        resortEquations();
        updateResult();

        /**
         * Add Watchers
         */

        ePlainLat.addTextChangedListener(new PlainWarcher());
        ePlainLon.addTextChangedListener(new PlainWarcher());

        bLatHem.setOnClickListener(new HemisphereClickListener());
        bLonHem.setOnClickListener(new HemisphereClickListener());

        for (final EditButton button : coordButtons) {
            button.setTextChangedListener(new TextWatcher() {
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
                    resortEquations();
                    updateResult();
                }
            });
        }

        return v;
    }

    // Make this dialog completely fill the screen
    @Override
    public void onStart() {
        super.onStart();
        final Dialog d = getDialog();
        if (d != null) {
            d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    // Save the current state of the calculator such that it can be restored after screen rotation (or similar).
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray("calc_state", SerializationUtils.serialize(saveState()));
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

    // This method has largely been lifted from the CoordinateInputDialog and is used to validate
    // coordinate specifications prior to assigning them to the Geopoint.
    private boolean areCurrentCoordinatesValid(final boolean signalError) {
        // then convert text to geopoint
        final Geopoint current;
        final String lat = getLatResult();
        final String lon = getLonResult();

        final String[] latTokens = lat.split("[ °'\"\\.]+");
        final String[] lonTokens = lon.split("[ °'\"\\.]+");

        try {
            switch (currentFormat) {
                case Deg:
                    current = new Geopoint(latTokens[0], latTokens[1], latTokens[2], lonTokens[0], lonTokens[1], lonTokens[2]);
                    break;
                case Min:
                    current = new Geopoint(latTokens[0], latTokens[1], latTokens[2], latTokens[3], lonTokens[0], lonTokens[1], lonTokens[2], lonTokens[3]);
                    break;
                case Sec:
                    current = new Geopoint(latTokens[0], latTokens[1], latTokens[2], latTokens[3], latTokens[4], lonTokens[0], lonTokens[1], lonTokens[2], lonTokens[3], lonTokens[4]);
                    break;
                case Plain:
                    current = new Geopoint(lat, lon);
                    break;
                default:
                    throw new IllegalStateException("can never happen, keep tools happy");
            }
            if (current.isValid()) {
                gp = current;
                return true;
            }
        } catch (final Geopoint.ParseException ignored) {
            // Signaled and returned below
        }

        if (signalError) {
            displayToast(R.string.err_parse_lat_lon);
        }
        return false;
    }

    private void setCalcState(final CalcState calcState) {
        this.calcState = calcState;
    }

    private void loadCalcState() {
        if (calcState != null) {

            setCoordFormat(calcState.format);
            final List<CalculateButton.ButtonData> buttons = calcState.buttons;

            bLatHem.setText(String.valueOf(calcState.latHemisphere));
            bLonHem.setText(String.valueOf(calcState.lonHemisphere));

            int i = 0;
            CalculateButton b = bLatDeg_100;
            while (b != null && i < buttons.size()) {
                b.setData(buttons.get(i++));
                b = b.getNextButton();
            }

            if (BuildConfig.DEBUG && b == null && i == buttons.size()) {
                throw new AssertionError("Number of ButtonData objects differ from the number of Buttons");
            }

            for (final CalculatorVariable.VariableData equ : calcState.equations) {
                equations.add(new CalculatorVariable(getContext(),
                                                     equ,
                                                     getString(R.string.equation_hint),
                                                     new EquationWatcher()));
            }

            for (final CalculatorVariable.VariableData var : calcState.freeVariables) {
                freeVariables.add(new CalculatorVariable(getContext(),
                                                         var,
                                                         getString(R.string.free_variable_hint),
                                                         new VariableWatcher()));
            }

            // Text must be set after Equations have been loaded as the TextWatcher will be triggered when the text is set
            ePlainLat.setText(calcState.plainLat);
            ePlainLon.setText(calcState.plainLon);

            notes.setText(calcState.notes);
        } else {
            resetCalculator();
            setCoordFormat(Settings.getCoordInputFormat());
        }
    }

    private CalcState saveState() {
        final List<CalculateButton.ButtonData> butData = new ArrayList<>();
        final List<CalculatorVariable.VariableData> equData = new ArrayList<>();
        final List<CalculatorVariable.VariableData> freeVarData = new ArrayList<>();

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

        CalculateButton b = bLatDeg_100;
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
                             notes.getText().toString());
    }

    private static void setCoordValue(final int val, final CalculateButton tenThousands, final CalculateButton thousands, final CalculateButton hundreds, final CalculateButton tens, final CalculateButton units) {
        int valCopy = val;
        tenThousands.setInputVal(String.valueOf(valCopy / 10000));

        valCopy %= 10000;
        thousands.setInputVal(String.valueOf(valCopy / 1000));

        setCoordValue(valCopy % 1000, hundreds, tens, units);
    }

    private static void setCoordValue(final int val, final CalculateButton hundreds, final CalculateButton tens, final CalculateButton units) {
        hundreds.setInputVal(String.valueOf(val / 100));
        setCoordValue(val, tens, units);
    }

    private static void setCoordValue(final int val, final CalculateButton tens, final CalculateButton units) {
        int valCopy = val % 100;
        tens.setInputVal(String.valueOf(valCopy / 10));

        valCopy = valCopy % 10;
        units.setInputVal(String.valueOf(valCopy));
    }

    private void setVisible(@IdRes final int viewId, final int visibility) {
        final View view = getView();
        assert view != null;
        ButterKnife.findById(view, viewId).setVisibility(visibility);
    }

    private void setCoordFormat(final Settings.CoordInputFormatEnum currentFormat) {
        this.currentFormat = currentFormat;
        spinner.setSelection(currentFormat.ordinal(), false);
    }

    /**
     * Format the given string into the appropriate 'pretty' representation:  42ABCDE -> 42° AB.CDE'
     * @param values: The string of values to be formatted.
     */
    private String format(final String values) {
        // Add degrees
        final String rv;

        switch (currentFormat) {
            case Plain:
                rv = values;
                break;

            case Sec:
                rv = " " + values.substring(0, 3) + SYMBOL_DEG
                   + " " + values.substring(3, 5) + SYMBOL_MIN
                   + " " + values.substring(5, 7) + SYMBOL_POINT
                         + values.substring(7)    + SYMBOL_SEC;
                break;

            case Min:
                rv = " " + values.substring(0, 3) + SYMBOL_DEG
                   + " " + values.substring(3, 5) + SYMBOL_POINT
                         + values.substring(5)    + SYMBOL_MIN;
                break;

            case Deg:
                rv = " " + values.substring(0, 3) + SYMBOL_POINT
                         + values.substring(3)    + SYMBOL_DEG;
                break;

            default:
                rv = "***";
        }

        return rv;
    }

    /**
     * Replace 'equation' Variables with there computer values: 42° AB.CDE' -> 42° 12.345'
     * @param values: The string to perform the substitutions on.
     * @return String with the substitutions performed.
     */
    private String substitute(final String values) {
        String rv = "";

        if (values.length() > 0) {
            final char first = values.charAt(0);
            String substitutionString;

            // Trim of the leading hemisphere character is it exists.
            if (first == 'N' || first == 'S' || first == 'E' || first == 'W') {
                rv = rv.concat(String.valueOf(first));
                substitutionString = values.substring(1);
            } else {
                substitutionString = values;
            }

            // Perform the substitutions on the remainder of teh string.
            for (final CalculatorVariable equ : equations) {
                substitutionString = substitutionString.replace(String.valueOf(equ.getName()), equ.evaluateString(freeVariables));
            }

            // Recombine the hemisphere and substituted string.
            rv = rv.concat(substitutionString);
        }

        // Remove placeholder characters.
        rv = rv.replaceAll(PLACE_HOLDER, "");

        // Break up connecting underscores
        while (rv.contains("__")) {
            rv = rv.replace("__", "_ _");
        }

        return rv;
    }


    /**
     * Retrieve all the values from the calculation buttons.
     * @param buttons: List of button from which to extract the values.
     * @return: Button values as a string.
     **
     * Note that a special 'place-holder' character is used to represent "blanked-out" buttons.
     * This is needed to preserve formatting when multi digit calculations are used.
     **/
    private String getValues(final List<CalculateButton> buttons) {
        String rv = "";
        for (final EditButton button : buttons) {
            // Remove inactive and blank digits from result
            if (button.getVisibility() == View.VISIBLE) {
                if (button.getLabel() == ' ') {
                    rv = rv.concat(PLACE_HOLDER);
                } else {
                    rv = rv.concat(String.valueOf(button.getLabel()));
                }
            }
        }

        // Formatting intentionally done first in case the substitution changes number of characters.
        return format(rv);
    }

    /**
     * Retireves the current representation of the computed longitudinal result.
     * @return: The result as a string.
     */
    private String getLatResult() {
        final String rv;

        if (currentFormat == Settings.CoordInputFormatEnum.Plain) {
            rv = ePlainLat.getText().toString();
        } else {
            rv = bLatHem.getText() + getValues(latButtons);
        }

        return substitute(rv);
    }

    /**
     * Retireves the current representation of the computed latitudinal result.
     * @return: The result as a string.
     */
    private String getLonResult() {
        final String rv;

        if (currentFormat == Settings.CoordInputFormatEnum.Plain) {
            rv = ePlainLon.getText().toString();
        } else {
            rv =  bLonHem.getText() + getValues(lonButtons);
        }

        return substitute(rv);
    }

    /**
     * Updates the supplies with the list of variables supplies.
     * @param variables: Variables to be included in the table.
     * @param grid: The table the variables are to be placed in.
     * @param startId: ID to be used for the first variable in the list.
     **
     * The ID's are used to assign an ordering such that the 'next' button on the keyboard (">")
     * will take you to the appropriate variable.
     */
    private void updateGrid(final List<CalculatorVariable> variables, final GridLayout grid, final int startId) {
        /**
         *   A = 2+2    F = 2+2
         *   B = 2+2    G = 2+2
         *   C = 2+2    H = 2+2
         *   D = 2+2    I = 2+2
         *   E = 2+2
         **/

        int id = startId;

        // If 'freeVariables' are to be displayed include a border around the tables
        // such that it becomes apparent that their is a second table which may be off teh screen.
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
        tLatResult.setText(getLatResult());
        tLonResult.setText(getLonResult());
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

                setCoordValue(gp.getLatDegFrac(), bLatPnt_10000, bLatPnt_01000, bLatPnt_00100, bLatPnt_00010, bLatPnt_00001);
                setCoordValue(gp.getLonDegFrac(), bLonPnt_10000, bLonPnt_01000, bLonPnt_00100, bLonPnt_00010, bLonPnt_00001);
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

                setCoordValue(gp.getLatMinFrac(), bLatPnt_10000, bLatPnt_01000, bLatPnt_00100);
                setCoordValue(gp.getLonMinFrac(), bLonPnt_10000, bLonPnt_01000, bLonPnt_00100);
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

                setCoordValue(gp.getLatSecFrac(), bLatPnt_10000, bLatPnt_01000, bLatPnt_00100);
                setCoordValue(gp.getLonSecFrac(), bLonPnt_10000, bLonPnt_01000, bLonPnt_00100);
                break;
        }
    }

    /**
     * Find if a button exists with the given name.
     * @param name: The name to search for.
     * @return: The first occurrence of the button if it can found, 'null' otherwise.
     */
    private EditButton findButton(final char name) {
        for (final EditButton button : coordButtons) {
            if (button.getLabel() == name) {
                return button;
            }
        }

        return null;
    }

    /**
     * Find if a variable exists in the supplied list with the given name.
     * @param name: The name to search for.
     * @return: The first occurrence of the variable if it can found, 'null' otherwise.
     */
    private CalculatorVariable findVariable(final char name, final List<CalculatorVariable> list) {
        for (final CalculatorVariable equ : list) {
            if (equ.getName() == name) {
                return equ;
            }
        }

        return null;
    }

    // Resort the equations into the order in which they first appear in the 'buttons' or 'plain-text' fields as appropriate.
    // This method is also responsible for adding and removing equations from the associated list as appropriate.
    private void resortEquations() {
        final List<CalculatorVariable> resortedEquations = new ArrayList<>();

        String chars = "";

        if (currentFormat == Settings.CoordInputFormatEnum.Plain) {
            final String lat = ePlainLat.getText().toString();
            final String lon = ePlainLon.getText().toString();
            char first;

            if (lat.length() > 0) {
                first = lat.charAt(0);
                if (first == 'N' || first == 'S') {
                    chars = chars.concat(lat.substring(1));
                } else {
                    chars = chars.concat(lat);
                }
            }

            if (lon.length() > 0) {
                first = lon.charAt(0);
                if (first == 'E' || first == 'W') {
                    chars = chars.concat(lon.substring(1));
                } else {
                    chars = chars.concat(lon);
                }
            }
        } else {
            for (final EditButton b : coordButtons) {
                chars = chars.concat(String.valueOf(b.getLabel()));
            }
        }

        for (int i = 0; i<chars.length(); i++) {
            final char ch = chars.charAt(i);

            if ('A' <= ch && ch <= 'Z') {
                if (findVariable(ch, resortedEquations) != null) {
                    continue;
                }

                CalculatorVariable thisEquation = findVariable(ch, equations);
                if (thisEquation == null) {
                    thisEquation = new CalculatorVariable(getContext(),
                            new CalculatorVariable.VariableData(ch),
                            getString(R.string.equation_hint),
                            new EquationWatcher());
                }

                resortedEquations.add(thisEquation);
            }
        }

        equations = resortedEquations;
        updateGrid(equations, equationGrid, 0);

        resortFreeVariables();
    }

    // Resort the free-variables into the order in which they first appear in the 'equations'.
    // This method is also responsible for adding and removing free-variables from the associated list as appropriate.
    private void resortFreeVariables() {
        final List<CalculatorVariable> resortedVariables = new ArrayList<>();

        String chars = "";

        for (final CalculatorVariable equ : equations) {
            chars = chars.concat(equ.getExpression());
        }

        for (int i = 0; i<chars.length(); i++) {
            final char ch = chars.charAt(i);

            if ('a' <= ch && ch <= 'z') {
                if (findVariable(ch, resortedVariables) != null) {
                    continue;
                }

                CalculatorVariable thisVariable = findVariable(ch, freeVariables);
                if (thisVariable == null) {
                    thisVariable = new CalculatorVariable(getContext(),
                            new CalculatorVariable.VariableData(ch),
                            getString(R.string.free_variable_hint),
                            new VariableWatcher());
                }

                resortedVariables.add(thisVariable);
            }
        }

        freeVariables = resortedVariables;
        updateGrid(freeVariables, variableGrid, equations.size());
    }

    private void resetCalculator() {
        // Reset the text in the Plain format EditTexts
        ePlainLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
        ePlainLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));

        bLatHem.setText(inputLatHem);
        bLonHem.setText(inputLonHem);

        // Resetting the 'first' button causes all subsequent buttons to be reset as well
        bLatDeg_100.resetButton();
    }

    // This method displays a reminder about saving the calculator state then closes both this dialog as well as the 'CoordinateInputDialog'.
    // Note that clicking the back arrow on the device does not run this method so in that case the user will be returned to the 'CoordinateInputDialog'.
    private void close() {
        if (calcState != null) {
            displayToast(R.string.warn_calculator_state_save);
        }

        // Close the 'CoordinateInputDialog'.
        getTargetFragment().onActivityResult(0, 0, null);
        dismiss();
    }
}

