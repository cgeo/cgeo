package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Geopoint.ParseException;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.CalcState;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.Settings.CoordInputFormatEnum;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CoordinatesInputDialog extends DialogFragment {

    private Geopoint gp;
    private Geopoint cacheCoords;

    private EditText eLat, eLon;
    private Button bLat, bLon;
    private EditText eLatDeg, eLatMin, eLatSec, eLatSub;
    private EditText eLonDeg, eLonMin, eLonSec, eLonSub;
    private TextView tLatSep1, tLatSep2, tLatSep3;
    private TextView tLonSep1, tLonSep2, tLonSep3;

    private CoordInputFormatEnum currentFormat = null;
    private List<EditText> orderedInputs;

    public static final String GEOPOINT_ARG = "GEOPOINT";
    private static final String CACHECOORDS_ARG = "CACHECOORDS";

    private FragmentActivity myContext;

    @NonNull
    private static Geopoint currentCoords() {
        return Sensors.getInstance().currentGeo().getCoords();
    }

    public static CoordinatesInputDialog getInstance(@Nullable final Geocache cache, @Nullable final Geopoint gp) {

        final Bundle args = new Bundle();

        if (gp != null) {
            args.putParcelable(GEOPOINT_ARG, gp);
        }

        if (cache != null) {
            args.putParcelable(CACHECOORDS_ARG, cache.getCoords());
        }

        final CoordinatesInputDialog cid = new CoordinatesInputDialog();
        cid.setArguments(args);
        return cid;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gp = getArguments().getParcelable(GEOPOINT_ARG);
        if (gp == null && !supportsNullCoordinates()) {
            gp = currentCoords();
        }
        cacheCoords = getArguments().getParcelable(CACHECOORDS_ARG);

        if (savedInstanceState != null && savedInstanceState.getParcelable(GEOPOINT_ARG) != null) {
            gp = savedInstanceState.getParcelable(GEOPOINT_ARG);
        }
    }

    private boolean supportsNullCoordinates() {
        return ((CoordinateUpdate) getActivity()).supportsNullCoordinates();
    }

    @Override
    public void onPause() {
        super.onPause();
        new Keyboard(getActivity()).hide();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        // TODO: if current input is not committed in gp, read the current input into gp
        outState.putParcelable(GEOPOINT_ARG, gp);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final Dialog dialog = getDialog();
        final boolean noTitle = dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final View v = inflater.inflate(R.layout.coordinatesinput_dialog, container, false);
        final InputDoneListener inputdone = new InputDoneListener();
        if (!noTitle) {
            dialog.setTitle(R.string.cache_coordinates);
        } else {
            final TextView title = v.findViewById(R.id.dialog_title_title);
            if (title != null) {
                title.setText(R.string.cache_coordinates);
                title.setVisibility(View.VISIBLE);
            }
            final ImageButton cancel = v.findViewById(R.id.dialog_title_cancel);
            if (cancel != null) {
                cancel.setOnClickListener(new InputCancelListener());
                cancel.setVisibility(View.VISIBLE);
            }
            final ImageButton done = v.findViewById(R.id.dialog_title_done);
            if (done != null) {
                done.setOnClickListener(inputdone);
                done.setVisibility(View.VISIBLE);
            }
        }

        final Spinner spinner = v.findViewById(R.id.spinnerCoordinateFormats);
        final ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(getActivity(),
                        R.array.waypoint_coordinate_formats,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Settings.getCoordInputFormat().ordinal());
        spinner.setOnItemSelectedListener(new CoordinateFormatListener());

        bLat = v.findViewById(R.id.ButtonLat);
        eLat = v.findViewById(R.id.latitude);
        eLatDeg = v.findViewById(R.id.EditTextLatDeg);
        eLatMin = v.findViewById(R.id.EditTextLatMin);
        eLatSec = v.findViewById(R.id.EditTextLatSec);
        eLatSub = v.findViewById(R.id.EditTextLatSecFrac);
        tLatSep1 = v.findViewById(R.id.LatSeparator1);
        tLatSep2 = v.findViewById(R.id.LatSeparator2);
        tLatSep3 = v.findViewById(R.id.LatSeparator3);

        bLon = v.findViewById(R.id.ButtonLon);
        eLon = v.findViewById(R.id.longitude);
        eLonDeg = v.findViewById(R.id.EditTextLonDeg);
        eLonMin = v.findViewById(R.id.EditTextLonMin);
        eLonSec = v.findViewById(R.id.EditTextLonSec);
        eLonSub = v.findViewById(R.id.EditTextLonSecFrac);
        tLonSep1 = v.findViewById(R.id.LonSeparator1);
        tLonSep2 = v.findViewById(R.id.LonSeparator2);
        tLonSep3 = v.findViewById(R.id.LonSeparator3);

        orderedInputs = Arrays.asList(eLatDeg, eLatMin, eLatSec, eLatSub, eLonDeg, eLonMin, eLonSec, eLonSub);

        for (final EditText editText : orderedInputs) {
            editText.addTextChangedListener(new SwitchToNextFieldWatcher(editText));
            editText.setOnFocusChangeListener(new PadZerosOnFocusLostListener());
            EditUtils.disableSuggestions(editText);
        }

        bLat.setOnClickListener(new ButtonClickListener());
        bLon.setOnClickListener(new ButtonClickListener());

        final Button buttonCurrent = v.findViewById(R.id.current);
        buttonCurrent.setOnClickListener(new CurrentListener());
        final Button buttonCache = v.findViewById(R.id.cache);

        if (cacheCoords != null) {
            buttonCache.setOnClickListener(new CacheListener());
        } else {
            buttonCache.setVisibility(View.GONE);
        }
        final Button buttonCalculate = v.findViewById(R.id.calculate);
        if (getActivity() instanceof CalculateState) {
            buttonCalculate.setOnClickListener(new CalculateListener());
            buttonCalculate.setVisibility(View.VISIBLE);
        }

        final Button buttonClear = v.findViewById(R.id.clear);
        if (supportsNullCoordinates()) {
            buttonClear.setOnClickListener(new ClearCoordinatesListener());
            buttonClear.setVisibility(View.VISIBLE);
        }

        if (hasClipboardCoordinates()) {
            final Button buttonClipboard = v.findViewById(R.id.clipboard);
            buttonClipboard.setOnClickListener(new ClipboardListener());
            buttonClipboard.setVisibility(View.VISIBLE);
        }

        final Button buttonDone = v.findViewById(R.id.done);
        if (noTitle) {
            buttonDone.setVisibility(View.GONE);
        } else {
            buttonDone.setOnClickListener(inputdone);
        }

        return v;
    }

    @Override
    public void onAttach(final Activity activity) {
        myContext = (FragmentActivity) activity;
        super.onAttach(activity);
    }

    @SuppressWarnings("unused")
    private static boolean hasClipboardCoordinates() {
        try {
            new Geopoint(StringUtils.defaultString(ClipboardUtils.getText()));
        } catch (final ParseException ignored) {
            return false;
        }
        return true;
    }

    private void updateGUI() {
        if (gp != null) {
            bLat.setText(String.valueOf(gp.getLatDir()));
            bLon.setText(String.valueOf(gp.getLonDir()));
        } else {
            bLat.setText(String.valueOf(currentCoords().getLatDir()));
            bLon.setText(String.valueOf(currentCoords().getLonDir()));
        }

        switch (currentFormat) {
            case Plain:
                setVisible(R.id.coordTable, View.GONE);
                eLat.setVisibility(View.VISIBLE);
                eLon.setVisibility(View.VISIBLE);
                if (gp != null) {
                    eLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    eLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                }
                break;
            case Deg: // DDD.DDDDD°
                setVisible(R.id.coordTable, View.VISIBLE);
                eLat.setVisibility(View.GONE);
                eLon.setVisibility(View.GONE);
                eLatSec.setVisibility(View.GONE);
                eLonSec.setVisibility(View.GONE);
                tLatSep3.setVisibility(View.GONE);
                tLonSep3.setVisibility(View.GONE);
                eLatSub.setVisibility(View.GONE);
                eLonSub.setVisibility(View.GONE);

                eLatMin.setHint(R.string.cc_hint_fraction);
                eLonMin.setHint(R.string.cc_hint_fraction);

                tLatSep1.setText(".");
                tLonSep1.setText(".");
                tLatSep2.setText("°");
                tLonSep2.setText("°");

                eLatMin.setGravity(Gravity.NO_GRAVITY);
                eLonMin.setGravity(Gravity.NO_GRAVITY);

                if (gp != null) {
                    eLatDeg.setText(addZeros(gp.getLatDeg(), 2));
                    eLatMin.setText(addZeros(gp.getLatDegFrac(), 5));
                    eLonDeg.setText(addZeros(gp.getLonDeg(), 3));
                    eLonMin.setText(addZeros(gp.getLonDegFrac(), 5));
                }
                break;
            case Min: // DDD° MM.MMM
                setVisible(R.id.coordTable, View.VISIBLE);
                eLat.setVisibility(View.GONE);
                eLon.setVisibility(View.GONE);
                eLatSec.setVisibility(View.VISIBLE);
                eLonSec.setVisibility(View.VISIBLE);
                tLatSep3.setVisibility(View.VISIBLE);
                tLonSep3.setVisibility(View.VISIBLE);
                eLatSub.setVisibility(View.GONE);
                eLonSub.setVisibility(View.GONE);

                eLatMin.setHint(R.string.cc_hint_minutes);
                eLatSec.setHint(R.string.cc_hint_fraction);
                eLonMin.setHint(R.string.cc_hint_minutes);
                eLonSec.setHint(R.string.cc_hint_fraction);

                tLatSep1.setText("°");
                tLonSep1.setText("°");
                tLatSep2.setText(".");
                tLonSep2.setText(".");
                tLatSep3.setText("'");
                tLonSep3.setText("'");

                eLatMin.setGravity(Gravity.RIGHT);
                eLonMin.setGravity(Gravity.RIGHT);

                if (gp != null) {
                    eLatDeg.setText(addZeros(gp.getLatDeg(), 2));
                    eLatMin.setText(addZeros(gp.getLatMin(), 2));
                    eLatSec.setText(addZeros(gp.getLatMinFrac(), 3));
                    eLonDeg.setText(addZeros(gp.getLonDeg(), 3));
                    eLonMin.setText(addZeros(gp.getLonMin(), 2));
                    eLonSec.setText(addZeros(gp.getLonMinFrac(), 3));
                }
                break;
            case Sec: // DDD° MM SS.SSS
                setVisible(R.id.coordTable, View.VISIBLE);
                eLat.setVisibility(View.GONE);
                eLon.setVisibility(View.GONE);
                eLatSec.setVisibility(View.VISIBLE);
                eLonSec.setVisibility(View.VISIBLE);
                tLatSep3.setVisibility(View.VISIBLE);
                tLonSep3.setVisibility(View.VISIBLE);
                eLatSub.setVisibility(View.VISIBLE);
                eLonSub.setVisibility(View.VISIBLE);

                eLatMin.setHint(R.string.cc_hint_minutes);
                eLatSec.setHint(R.string.cc_hint_seconds);
                eLatSub.setHint(R.string.cc_hint_fraction);
                eLonMin.setHint(R.string.cc_hint_minutes);
                eLonSec.setHint(R.string.cc_hint_seconds);
                eLonSub.setHint(R.string.cc_hint_fraction);

                tLatSep1.setText("°");
                tLonSep1.setText("°");
                tLatSep2.setText("'");
                tLonSep2.setText("'");
                tLatSep3.setText(".");
                tLonSep3.setText(".");

                eLatMin.setGravity(Gravity.RIGHT);
                eLonMin.setGravity(Gravity.RIGHT);

                if (gp != null) {
                    eLatDeg.setText(addZeros(gp.getLatDeg(), 2));
                    eLatMin.setText(addZeros(gp.getLatMin(), 2));
                    eLatSec.setText(addZeros(gp.getLatSec(), 2));
                    eLatSub.setText(addZeros(gp.getLatSecFrac(), 3));
                    eLonDeg.setText(addZeros(gp.getLonDeg(), 3));
                    eLonMin.setText(addZeros(gp.getLonMin(), 2));
                    eLonSec.setText(addZeros(gp.getLonSec(), 2));
                    eLonSub.setText(addZeros(gp.getLonSecFrac(), 3));
                }
                break;
        }

        for (final EditText editText : orderedInputs) {
            setSize(editText);
        }
    }

    private void setVisible(@IdRes final int viewId, final int visibility) {
        final View view = getView();
        assert view != null;
        view.findViewById(viewId).setVisibility(visibility);
    }

    private void setSize(final EditText someEditText) {
        if (someEditText.getVisibility() == View.GONE) {
            return;
        }
        someEditText.setMinEms(getMaxLengthFromCurrentField(someEditText));
    }

    private static String addZeros(final int value, final int len) {
        return StringUtils.leftPad(Integer.toString(value), len, '0');
    }

    private class ButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            final Button button = (Button) view;
            final CharSequence text = button.getText();
            if (StringUtils.isBlank(text)) {
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
                    break;
            }
        }
    }

    private class SwitchToNextFieldWatcher implements TextWatcher {

        /**
         * weak reference, such that garbage collector can do its work
         */
        private final WeakReference<EditText> editTextRef;

        SwitchToNextFieldWatcher(final EditText editText) {
            this.editTextRef = new WeakReference<>(editText);
        }

        @Override
        public void afterTextChanged(final Editable s) {
            if (currentFormat == CoordInputFormatEnum.Plain) {
                return;
            }

            final EditText editText = editTextRef.get();
            if (editText == null) {
                return;
            }
            if (!editText.hasFocus()) {
                return;
            }

            if (s.length() == getMaxLengthFromCurrentField(editText)) {
                focusNextVisibleInput(editText);
            }
        }

        private void focusNextVisibleInput(final EditText editText) {
            int index = orderedInputs.indexOf(editText);
            do {
                index = (index + 1) % orderedInputs.size();
            } while (orderedInputs.get(index).getVisibility() == View.GONE);

            orderedInputs.get(index).requestFocus();
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            // nothing to do
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            // nothing to do
        }

    }

    private boolean areCurrentCoordinatesValid(final boolean signalError) {
        try {
            // first normalize all the text fields
            final String latDir = bLat.getText().toString();
            final String lonDir = bLon.getText().toString();
            final String latDeg = eLatDeg.getText().toString();
            final String lonDeg = eLonDeg.getText().toString();
            // right-pad decimal fraction
            final String latDegFrac = padZeros(eLatMin);
            final String lonDegFrac = padZeros(eLonMin);
            final String latMin = eLatMin.getText().toString();
            final String lonMin = eLonMin.getText().toString();
            final String latMinFrac = padZeros(eLatSec);
            final String lonMinFrac = padZeros(eLonSec);
            final String latSec = padZeros(eLatSec);
            final String lonSec = padZeros(eLonSec);
            // right-pad seconds fraction
            final String latSecFrac = padZeros(eLatSub);
            final String lonSecFrac = padZeros(eLonSub);

            // then convert text to geopoint
            final Geopoint current;
            switch (currentFormat) {
                case Deg:
                    current = new Geopoint(latDir, latDeg, latDegFrac, lonDir, lonDeg, lonDegFrac);
                    break;
                case Min:
                    current = new Geopoint(latDir, latDeg, padZeros(eLatMin), latMinFrac, lonDir, lonDeg, padZeros(eLonMin), lonMinFrac);
                    break;
                case Sec:
                    current = new Geopoint(latDir, latDeg, latMin, latSec, latSecFrac, lonDir, lonDeg, lonMin, lonSec, lonSecFrac);
                    break;
                case Plain:
                    current = new Geopoint(eLat.getText().toString(), eLon.getText().toString());
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
            final AbstractActivity activity = (AbstractActivity) getActivity();
            activity.showToast(activity.getString(R.string.err_parse_lat_lon));
        }
        return false;
    }

    private String padZeros(final EditText editText) {
        final int maxLength = getMaxLengthFromCurrentField(editText);
        if (editText.length() < maxLength) {
            if ((editText.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
                return StringUtils.leftPad(editText.getText().toString(), maxLength, '0');
            }
            return StringUtils.rightPad(editText.getText().toString(), maxLength, '0');
        }
        return editText.getText().toString();
    }

    /**
     * Max lengths, depending on currentFormat
     *
     * formatPlain = disabled
     * DEG MIN SEC SUB
     * formatDeg 2/3 5 - -
     * formatMin 2/3 2 3 -
     * formatSec 2/3 2 2 3
     */

    public int getMaxLengthFromCurrentField(final EditText editText) {
        if (editText == eLonDeg || editText == eLatSub || editText == eLonSub) {
            return 3;
        }
        if ((editText == eLatMin || editText == eLonMin) && currentFormat == CoordInputFormatEnum.Deg) {
            return 5;
        }
        if ((editText == eLatSec || editText == eLonSec) && currentFormat == CoordInputFormatEnum.Min) {
            return 3;
        }
        return 2;
    }

    private class CoordinateFormatListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(final AdapterView<?> parent, final View view, final int pos, final long id) {
            // Ignore first call, which comes from onCreate()
            if (currentFormat != null) {

                // Start new format with an acceptable value: either the current one
                // entered by the user, or our current position.
                if (!areCurrentCoordinatesValid(false) && !supportsNullCoordinates()) {
                    gp = currentCoords();
                }
            }

            currentFormat = CoordInputFormatEnum.fromInt(pos);
            Settings.setCoordInputFormat(currentFormat);
            updateGUI();

            // select first field
            orderedInputs.get(0).requestFocus();
        }

        @Override
        public void onNothingSelected(final AdapterView<?> arg0) {
            // do nothing
        }

    }

    private class CurrentListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            gp = currentCoords();
            updateGUI();
        }
    }

    private class CacheListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            if (cacheCoords == null) {
                final AbstractActivity activity = (AbstractActivity) getActivity();
                activity.showToast(activity.getString(R.string.err_location_unknown));
                return;
            }

            gp = cacheCoords;
            updateGUI();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        dismiss();
    }

    private class CalculateListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            if (getActivity() instanceof  CalculateState) {
                final CalculateState calculateState = (CalculateState) getActivity();
                final CalcState theState = calculateState.fetchCalculatorState();
                final CoordinatesCalculateDialog calculateDialog = CoordinatesCalculateDialog.getInstance(gp, theState);
                calculateDialog.setCancelable(true);
                calculateDialog.show(myContext.getSupportFragmentManager(), "wpcalcdialog");
                dismiss();
            }
        }
    }

    private class ClipboardListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            try {
                gp = new Geopoint(StringUtils.defaultString(ClipboardUtils.getText()));
                updateGUI();
            } catch (final ParseException ignored) {
            }
        }
    }

    private class InputDoneListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            if (!areCurrentCoordinatesValid(true)) {
                return;
            }
            ((CoordinateUpdate) getActivity()).updateCoordinates(gp);
            dismiss();
        }
    }

    private class ClearCoordinatesListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            ((CoordinateUpdate) getActivity()).updateCoordinates(null);
            dismiss();
        }
    }

    private class InputCancelListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            dismiss();
        }
    }

    public interface CoordinateUpdate {
        void updateCoordinates(Geopoint gp);
        boolean supportsNullCoordinates();
    }

    // Interface used by the coordinate calculator dialog too preserve its state in the waypoint itself.
    public interface CalculateState {
        void saveCalculatorState(CalcState calc);
        CalcState fetchCalculatorState();
    }

    private class PadZerosOnFocusLostListener implements OnFocusChangeListener {

        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            if (!hasFocus) {
                final EditText editText = (EditText) v;
                if (editText.length() > 0) {
                    editText.setText(padZeros(editText));
                }
            }
        }
    }

}
