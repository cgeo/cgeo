package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Geopoint.ParseException;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.Settings.CoordInputFormatEnum;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;

import android.app.Dialog;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import butterknife.ButterKnife;
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

    private static final String GEOPOINT_ARG = "GEOPOINT";
    private static final String CACHECOORDS_ARG = "CACHECOORDS";

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
        if (gp == null) {
            gp = currentCoords();
        }
        cacheCoords = getArguments().getParcelable(CACHECOORDS_ARG);

        if (savedInstanceState != null && savedInstanceState.getParcelable(GEOPOINT_ARG) != null) {
            gp = savedInstanceState.getParcelable(GEOPOINT_ARG);
        }

        if (VERSION.SDK_INT < VERSION_CODES.HONEYCOMB && Settings.isLightSkin()) {
            setStyle(STYLE_NORMAL, R.style.DialogFixGingerbread);
        }
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

        final Spinner spinner = ButterKnife.findById(v, R.id.spinnerCoordinateFormats);
        final ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(getActivity(),
                        R.array.waypoint_coordinate_formats,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Settings.getCoordInputFormat().ordinal());
        spinner.setOnItemSelectedListener(new CoordinateFormatListener());

        bLat = ButterKnife.findById(v, R.id.ButtonLat);
        eLat = ButterKnife.findById(v, R.id.latitude);
        eLatDeg = ButterKnife.findById(v, R.id.EditTextLatDeg);
        eLatMin = ButterKnife.findById(v, R.id.EditTextLatMin);
        eLatSec = ButterKnife.findById(v, R.id.EditTextLatSec);
        eLatSub = ButterKnife.findById(v, R.id.EditTextLatSecFrac);
        tLatSep1 = ButterKnife.findById(v, R.id.LatSeparator1);
        tLatSep2 = ButterKnife.findById(v, R.id.LatSeparator2);
        tLatSep3 = ButterKnife.findById(v, R.id.LatSeparator3);

        bLon = ButterKnife.findById(v, R.id.ButtonLon);
        eLon = ButterKnife.findById(v, R.id.longitude);
        eLonDeg = ButterKnife.findById(v, R.id.EditTextLonDeg);
        eLonMin = ButterKnife.findById(v, R.id.EditTextLonMin);
        eLonSec = ButterKnife.findById(v, R.id.EditTextLonSec);
        eLonSub = ButterKnife.findById(v, R.id.EditTextLonSecFrac);
        tLonSep1 = ButterKnife.findById(v, R.id.LonSeparator1);
        tLonSep2 = ButterKnife.findById(v, R.id.LonSeparator2);
        tLonSep3 = ButterKnife.findById(v, R.id.LonSeparator3);

        orderedInputs = Arrays.asList(eLatDeg, eLatMin, eLatSec, eLatSub, eLonDeg, eLonMin, eLonSec, eLonSub);

        for (final EditText editText : orderedInputs) {
            editText.addTextChangedListener(new SwitchToNextFieldWatcher(editText));
            editText.setOnFocusChangeListener(new PadZerosOnFocusLostListener());
            EditUtils.disableSuggestions(editText);
        }

        bLat.setOnClickListener(new ButtonClickListener());
        bLon.setOnClickListener(new ButtonClickListener());

        final Button buttonCurrent = ButterKnife.findById(v, R.id.current);
        buttonCurrent.setOnClickListener(new CurrentListener());
        final Button buttonCache = ButterKnife.findById(v, R.id.cache);

        if (cacheCoords != null) {
            buttonCache.setOnClickListener(new CacheListener());
        } else {
            buttonCache.setVisibility(View.GONE);
        }

        if (hasClipboardCoordinates()) {
            final Button buttonClipboard = ButterKnife.findById(v, R.id.clipboard);
            buttonClipboard.setOnClickListener(new ClipboardListener());
            buttonClipboard.setVisibility(View.VISIBLE);
        }

        final Button buttonDone = ButterKnife.findById(v, R.id.done);
        if (noTitle) {
            buttonDone.setVisibility(View.GONE);
        } else {
            buttonDone.setOnClickListener(inputdone);
        }
        return v;
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
        if (gp == null) {
            return;
        }

        bLat.setText(String.valueOf(gp.getLatDir()));
        bLon.setText(String.valueOf(gp.getLonDir()));

        switch (currentFormat) {
            case Plain:
                setVisible(R.id.coordTable, View.GONE);
                eLat.setVisibility(View.VISIBLE);
                eLon.setVisibility(View.VISIBLE);
                eLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                eLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
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

                tLatSep1.setText(".");
                tLonSep1.setText(".");
                tLatSep2.setText("°");
                tLonSep2.setText("°");

                eLatDeg.setText(addZeros(gp.getLatDeg(), 2));
                eLatMin.setText(addZeros(gp.getLatDegFrac(), 5));
                eLatMin.setGravity(Gravity.NO_GRAVITY);
                eLonDeg.setText(addZeros(gp.getLonDeg(), 3));
                eLonMin.setText(addZeros(gp.getLonDegFrac(), 5));
                eLonMin.setGravity(Gravity.NO_GRAVITY);
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

                tLatSep1.setText("°");
                tLonSep1.setText("°");
                tLatSep2.setText(".");
                tLonSep2.setText(".");
                tLatSep3.setText("'");
                tLonSep3.setText("'");

                eLatDeg.setText(addZeros(gp.getLatDeg(), 2));
                eLatMin.setText(addZeros(gp.getLatMin(), 2));
                eLatMin.setGravity(Gravity.RIGHT);
                eLatSec.setText(addZeros(gp.getLatMinFrac(), 3));
                eLonDeg.setText(addZeros(gp.getLonDeg(), 3));
                eLonMin.setText(addZeros(gp.getLonMin(), 2));
                eLonMin.setGravity(Gravity.RIGHT);
                eLonSec.setText(addZeros(gp.getLonMinFrac(), 3));
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

                tLatSep1.setText("°");
                tLonSep1.setText("°");
                tLatSep2.setText("'");
                tLonSep2.setText("'");
                tLatSep3.setText(".");
                tLonSep3.setText(".");

                eLatDeg.setText(addZeros(gp.getLatDeg(), 2));
                eLatMin.setText(addZeros(gp.getLatMin(), 2));
                eLatMin.setGravity(Gravity.RIGHT);
                eLatSec.setText(addZeros(gp.getLatSec(), 2));
                eLatSub.setText(addZeros(gp.getLatSecFrac(), 3));
                eLonDeg.setText(addZeros(gp.getLonDeg(), 3));
                eLonMin.setText(addZeros(gp.getLonMin(), 2));
                eLonMin.setGravity(Gravity.RIGHT);
                eLonSec.setText(addZeros(gp.getLonSec(), 2));
                eLonSub.setText(addZeros(gp.getLonSecFrac(), 3));
                break;
        }

        for (final EditText editText : orderedInputs) {
            setSize(editText);
        }
    }

    private void setVisible(@IdRes final int viewId, final int visibility) {
        final View view = getView();
        assert view != null;
        ButterKnife.findById(view, viewId).setVisibility(visibility);
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

            // This will serve as a reminder to the user that the current coordinates might not be valid
            areCurrentCoordinatesValid(true);
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
            final String latDegFrac = padZerosRight(eLatMin.getText().toString(), 5);
            final String lonDegFrac = padZerosRight(eLonMin.getText().toString(), 5);
            final String latMin = eLatMin.getText().toString();
            final String lonMin = eLonMin.getText().toString();
            final String latMinFrac = eLatSec.getText().toString();
            final String lonMinFrac = eLonSec.getText().toString();
            final String latSec = eLatSec.getText().toString();
            final String lonSec = eLonSec.getText().toString();
            // right-pad seconds fraction
            final String latSecFrac = padZerosRight(eLatSub.getText().toString(), 3);
            final String lonSecFrac = padZerosRight(eLonSub.getText().toString(), 3);

            // then convert text to geopoint
            final Geopoint current;
            switch (currentFormat) {
                case Deg:
                    current = new Geopoint(latDir, latDeg, latDegFrac, lonDir, lonDeg, lonDegFrac);
                    break;
                case Min:
                    current = new Geopoint(latDir, latDeg, latMin, latMinFrac, lonDir, lonDeg, lonMin, lonMinFrac);
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
            activity.showToast(activity.getResources().getString(R.string.err_parse_lat_lon));
        }
        return false;
    }

    private static String padZerosRight(final String value, final int len) {
        return StringUtils.rightPad(value, len, '0');
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
                if (!areCurrentCoordinatesValid(false)) {
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
                activity.showToast(activity.getResources().getString(R.string.err_location_unknown));
                return;
            }

            gp = cacheCoords;
            updateGUI();
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
            if (gp != null) {
                ((CoordinateUpdate) getActivity()).updateCoordinates(gp);
            }
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
        void updateCoordinates(final Geopoint gp);
    }

    private class PadZerosOnFocusLostListener implements OnFocusChangeListener {

        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            if (!hasFocus) {
                final EditText editText = (EditText) v;
                final int maxLength = getMaxLengthFromCurrentField(editText);
                if (editText.length() < maxLength) {
                    if ((editText.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.RIGHT) {
                        editText.setText(StringUtils.leftPad(editText.getText().toString(), maxLength, '0'));
                    } else {
                        editText.setText(StringUtils.rightPad(editText.getText().toString(), maxLength, '0'));
                    }
                }
            }
        }
    }

}
