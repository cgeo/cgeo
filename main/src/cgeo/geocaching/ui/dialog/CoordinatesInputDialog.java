package cgeo.geocaching.ui.dialog;

import butterknife.ButterKnife;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Geopoint.ParseException;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.Settings.CoordInputFormatEnum;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;

import org.apache.commons.lang3.StringUtils;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class CoordinatesInputDialog extends DialogFragment {

    private Geopoint gp;
    private Geopoint gpinitial;
    private Geopoint cacheCoords;

    private EditText eLat, eLon;
    private Button bLat, bLon;
    private EditText eLatDeg, eLatMin, eLatSec, eLatSub;
    private EditText eLonDeg, eLonMin, eLonSec, eLonSub;
    private TextView tLatSep1, tLatSep2, tLatSep3;
    private TextView tLonSep1, tLonSep2, tLonSep3;

    private CoordInputFormatEnum currentFormat = null;


    private static final String GEOPOINT_ARG = "GEOPOINT";
    private static final String GEOPOINT_INTIAL_ARG = "GEOPOINT_INITIAL";
    private static final String CACHECOORDS_ARG = "CACHECOORDS";


    public static CoordinatesInputDialog getInstance(final Geocache cache, final Geopoint gp, final GeoData geo) {

        final Bundle args = new Bundle();

        if (gp != null) {
            args.putParcelable(GEOPOINT_ARG, gp);
        } else {
            args.putParcelable(GEOPOINT_ARG, geo != null ? geo.getCoords() : Geopoint.ZERO);
        }

        if (geo !=null) {
            args.putParcelable(GEOPOINT_INTIAL_ARG, geo.getCoords());
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
        gpinitial = getArguments().getParcelable(GEOPOINT_INTIAL_ARG);
        cacheCoords = getArguments().getParcelable(CACHECOORDS_ARG);

        if (savedInstanceState != null && savedInstanceState.getParcelable(GEOPOINT_ARG)!=null) {
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
        // TODO: if current input is not commited in gp, read the current input into gp
        outState.putParcelable(GEOPOINT_ARG, gp);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        getDialog().setTitle(R.string.cache_coordinates);

        final View v = inflater.inflate(R.layout.coordinatesinput_dialog, container, false);
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

        eLatDeg.addTextChangedListener(new TextChanged(eLatDeg));
        eLatMin.addTextChangedListener(new TextChanged(eLatMin));
        eLatSec.addTextChangedListener(new TextChanged(eLatSec));
        eLatSub.addTextChangedListener(new TextChanged(eLatSub));
        eLonDeg.addTextChangedListener(new TextChanged(eLonDeg));
        eLonMin.addTextChangedListener(new TextChanged(eLonMin));
        eLonSec.addTextChangedListener(new TextChanged(eLonSec));
        eLonSub.addTextChangedListener(new TextChanged(eLonSub));

        EditUtils.disableSuggestions(eLatDeg);
        EditUtils.disableSuggestions(eLatMin);
        EditUtils.disableSuggestions(eLatSec);
        EditUtils.disableSuggestions(eLatSub);
        EditUtils.disableSuggestions(eLonDeg);
        EditUtils.disableSuggestions(eLonMin);
        EditUtils.disableSuggestions(eLonSec);
        EditUtils.disableSuggestions(eLonSub);

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
        buttonDone.setOnClickListener(new InputDoneListener());

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
                getView().findViewById(R.id.coordTable).setVisibility(View.GONE);
                eLat.setVisibility(View.VISIBLE);
                eLon.setVisibility(View.VISIBLE);
                eLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                eLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                break;
            case Deg: // DDD.DDDDD°
                getView().findViewById(R.id.coordTable).setVisibility(View.VISIBLE);
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
                getView().findViewById(R.id.coordTable).setVisibility(View.VISIBLE);
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
                getView().findViewById(R.id.coordTable).setVisibility(View.VISIBLE);
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
    }

    private static String addZeros(final int value, final int len) {
        return StringUtils.leftPad(Integer.toString(value), len, '0');
    }

    private class ButtonClickListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            assert view instanceof Button;
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

    private class TextChanged implements TextWatcher {

        private final EditText editText;

        public TextChanged(final EditText editText) {
            this.editText = editText;
        }

        @Override
        public void afterTextChanged(final Editable s) {
            /*
             * Max lengths, depending on currentFormat
             *
             * formatPlain = disabled
             * DEG MIN SEC SUB
             * formatDeg 2/3 5 - -
             * formatMin 2/3 2 3 -
             * formatSec 2/3 2 2 3
             */

            if (currentFormat == CoordInputFormatEnum.Plain) {
                return;
            }

            final int maxLength = getMaxLengthFromCurrentField(editText);
            if (s.length() == maxLength) {
                if (editText == eLatDeg) {
                    eLatMin.requestFocus();
                } else if (editText == eLatMin) {
                    if (eLatSec.getVisibility() == View.GONE) {
                        eLonDeg.requestFocus();
                    } else {
                        eLatSec.requestFocus();
                    }
                } else if (editText == eLatSec) {
                    if (eLatSub.getVisibility() == View.GONE) {
                        eLonDeg.requestFocus();
                    } else {
                        eLatSub.requestFocus();
                    }
                } else if (editText == eLatSub) {
                    eLonDeg.requestFocus();
                } else if (editText == eLonDeg) {
                    eLonMin.requestFocus();
                } else if (editText == eLonMin) {
                    if (eLonSec.getVisibility() == View.GONE) {
                        eLatDeg.requestFocus();
                    } else {
                        eLonSec.requestFocus();
                    }
                } else if (editText == eLonSec) {
                    if (eLonSub.getVisibility() == View.GONE) {
                        eLatDeg.requestFocus();
                    } else {
                        eLonSub.requestFocus();
                    }
                } else if (editText == eLonSub) {
                    eLatDeg.requestFocus();
                }
            }
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
        }

    }

    private boolean areCurrentCoordinatesValid(final boolean signalError) {
        try {
            Geopoint current = null;
            if (currentFormat == CoordInputFormatEnum.Plain) {
                current = new Geopoint(eLat.getText().toString(), eLon.getText().toString());
            } else {
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
                        // This case has been handled above
                }
            }
            // The null check is necessary to keep FindBugs happy
            if (current != null && current.isValid()) {
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
                // entered by the user, else our current coordinates, else (0,0).
                if (!areCurrentCoordinatesValid(false)) {
                    if (gpinitial != null) {
                        gp = gpinitial;
                    } else {
                        gp = Geopoint.ZERO;
                    }
                }
            }

            currentFormat = CoordInputFormatEnum.fromInt(pos);
            Settings.setCoordInputFormat(currentFormat);
            updateGUI();
        }

        @Override
        public void onNothingSelected(final AdapterView<?> arg0) {
        }

    }

    private class CurrentListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            if (gpinitial == null) {
                final AbstractActivity activity = (AbstractActivity) getActivity();
                activity.showToast(activity.getResources().getString(R.string.err_point_unknown_position));
                return;
            }

            gp = gpinitial;
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

    public interface CoordinateUpdate {
        public void updateCoordinates(final Geopoint gp);
    }

}
