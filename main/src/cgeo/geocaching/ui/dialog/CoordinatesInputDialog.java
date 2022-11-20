package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.databinding.CoordinatesInputDialogBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Geopoint.ParseException;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.models.CalculatedCoordinate;
import cgeo.geocaching.models.CalculatedCoordinateType;
import cgeo.geocaching.models.CoordinateInputData;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.permission.PermissionHandler;
import cgeo.geocaching.permission.PermissionRequestContext;
import cgeo.geocaching.permission.RestartLocationPermissionGrantedCallback;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.Settings.CoordInputFormatEnum;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import com.google.android.material.textfield.TextInputLayout;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import org.apache.commons.lang3.StringUtils;

public class CoordinatesInputDialog extends DialogFragment {

    private CoordinateInputData inputData;
    private Geopoint gp;
    private Geopoint cacheCoords;

    private TextInputLayout eLatFrame;
    private TextInputLayout eLonFrame;
    private EditText eLat;
    private EditText eLon;
    private Button bLat;
    private Button bLon;
    private EditText eLatDeg;
    private EditText eLatMin;
    private EditText eLatSec;
    private EditText eLatSub;
    private EditText eLonDeg;
    private EditText eLonMin;
    private EditText eLonSec;
    private EditText eLonSub;
    private TextView tLatSep1;
    private TextView tLatSep2;
    private TextView tLatSep3;
    private TextView tLonSep1;
    private TextView tLonSep2;
    private TextView tLonSep3;
    private CoordinatesInputDialogBinding binding;

    private CoordInputFormatEnum currentFormat = null;
    private List<EditText> orderedInputs;

    private static final String INPUT_DATA_ARG = "arg_input_data";

    private FragmentActivity myContext;

    private final CompositeDisposable resumeDisposables = new CompositeDisposable();
    private final GeoDirHandler geoUpdate = new GeoDirHandler() {
        @Override
        public void updateGeoData(final GeoData geo) {
            binding.current.setText(getString(R.string.waypoint_my_coordinates_accuracy, Units.getDistanceFromMeters(geo.getAccuracy())));
        }
    };

    @NonNull
    private static Geopoint currentCoords() {
        return Sensors.getInstance().currentGeo().getCoords();
    }

    public static void show(final FragmentManager mgr, @Nullable final Geocache cache, @Nullable final Geopoint gp) {
        final CoordinateInputData cid = new CoordinateInputData();
        cid.setGeopoint(gp);
        if (cache != null) {
            cid.setGeocode(cache.getGeocode());
        }
        show(mgr, cid);
    }

    public static void show(final FragmentManager mgr, final CoordinateInputData inputData) {

        if (inputData.getCalculatedCoordinate() != null && inputData.getCalculatedCoordinate().isFilled()) {
            CoordinatesCalculateGlobalDialog.show(mgr, inputData);
            return;
        }

        final Bundle args = new Bundle();
        args.putParcelable(INPUT_DATA_ARG, inputData);

        final CoordinatesInputDialog ciDialog = new CoordinatesInputDialog();
        ciDialog.setArguments(args);
        ciDialog.setCancelable(true);
        ciDialog.show(mgr, "coord_input_dialog");
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final CoordinateInputData cid = savedInstanceState != null && savedInstanceState.containsKey(INPUT_DATA_ARG) ?
                savedInstanceState.getParcelable(INPUT_DATA_ARG) :
                (getArguments() == null ? null : getArguments().getParcelable(INPUT_DATA_ARG));
        inputData = cid == null ? new CoordinateInputData() : cid;
        gp = inputData.getGeopoint();
        if (gp == null && !supportsNullCoordinates()) {
            gp = currentCoords();
        }

        final String geocode = inputData.getGeocode();
        if (!StringUtils.isBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            cacheCoords = cache == null ? null : cache.getCoords();
        }
    }

    private boolean supportsNullCoordinates() {
        return ((CoordinateUpdate) getActivity()).supportsNullCoordinates();
    }

    @Override
    public void onPause() {
        super.onPause();
        resumeDisposables.clear();
        Keyboard.hide(getActivity());

    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume location access
        PermissionHandler.executeIfLocationPermissionGranted(this.getActivity(), new RestartLocationPermissionGrantedCallback(PermissionRequestContext.EditWaypointActivity) {
            @Override
            protected void executeAfter() {
                resumeDisposables.add(geoUpdate.start(GeoDirHandler.UPDATE_GEODATA));
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        inputData.setGeopoint(gp);
        outState.putParcelable(INPUT_DATA_ARG, inputData);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final Dialog dialog = getDialog();
        final boolean noTitle = dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final View v = inflater.inflate(R.layout.coordinates_input_dialog, container, false);
        binding = CoordinatesInputDialogBinding.bind(v);

        // change input lines border color depending on "any child has focus" state
        binding.input.latLonFrame.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> binding.input.latLonFrame.setBackgroundResource(binding.input.latLonFrame.findFocus() != null ? R.drawable.textinputlayout_bcg_active : R.drawable.textinputlayout_bcg_default));

        if (!noTitle) {
            dialog.setTitle(R.string.cache_coordinates);
        } else {
            final Toolbar toolbar = binding.actionbar.toolbar;
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
        }

        final Spinner spinner = binding.input.spinnerCoordinateFormats;
        final ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(getActivity(),
                        R.array.waypoint_coordinate_formats,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Settings.getCoordInputFormat().ordinal());
        spinner.setOnItemSelectedListener(new CoordinateFormatListener());

        eLatFrame = binding.input.latitudeFrame;
        bLat = binding.input.latitudeBundle.hemisphere;
        eLat = binding.input.latitude;
        eLatDeg = binding.input.latitudeBundle.EditTextLatDeg;
        eLatMin = binding.input.latitudeBundle.EditTextLatMin;
        eLatSec = binding.input.latitudeBundle.EditTextLatSec;
        eLatSub = binding.input.latitudeBundle.EditTextLatSecFrac;
        tLatSep1 = binding.input.latitudeBundle.LatSeparator1;
        tLatSep2 = binding.input.latitudeBundle.LatSeparator2;
        tLatSep3 = binding.input.latitudeBundle.LatSeparator3;

        eLonFrame = binding.input.longitudeFrame;
        bLon = binding.input.longitudeBundle.hemisphere;
        eLon = binding.input.longitude;
        eLonDeg = binding.input.longitudeBundle.EditTextLatDeg;
        eLonMin = binding.input.longitudeBundle.EditTextLatMin;
        eLonSec = binding.input.longitudeBundle.EditTextLatSec;
        eLonSub = binding.input.longitudeBundle.EditTextLatSecFrac;
        tLonSep1 = binding.input.longitudeBundle.LatSeparator1;
        tLonSep2 = binding.input.longitudeBundle.LatSeparator2;
        tLonSep3 = binding.input.longitudeBundle.LatSeparator3;

        orderedInputs = Arrays.asList(eLatDeg, eLatMin, eLatSec, eLatSub, eLonDeg, eLonMin, eLonSec, eLonSub);

        for (final EditText editText : orderedInputs) {
            editText.addTextChangedListener(new SwitchToNextFieldWatcher(editText));
            editText.setOnFocusChangeListener(new PadZerosOnFocusLostListener());
            EditUtils.disableSuggestions(editText);
        }

        bLat.setOnClickListener(new ButtonClickListener());
        bLon.setOnClickListener(new ButtonClickListener());

        binding.current.setOnClickListener(new CurrentListener());

        if (cacheCoords != null) {
            binding.cache.setOnClickListener(new CacheListener());
        } else {
            binding.cache.setVisibility(View.GONE);
        }

        if (inputData.getGeocode() != null) {
            binding.calculateGlobal.setVisibility(View.VISIBLE);
            binding.calculateGlobal.setOnClickListener(vv -> {
                inputData.setGeopoint(gp);
                final CalculatedCoordinate cc = new CalculatedCoordinate();
                cc.setType(
                        CalculatedCoordinateType.values()[binding.input.spinnerCoordinateFormats.getSelectedItemPosition()]);

                //try to set patterns from GUI
                final Pair<String, String> patternsFromGui = getLatLonPatternFromGui();
                cc.setLatitudePattern(patternsFromGui.first);
                cc.setLongitudePattern(patternsFromGui.second);

                inputData.setCalculatedCoordinate(cc);
                CoordinatesCalculateGlobalDialog.show(myContext.getSupportFragmentManager(), inputData);
                dismiss();
            });
        }

        if (supportsNullCoordinates()) {
            binding.clear.setOnClickListener(new ClearCoordinatesListener());
            binding.clear.setVisibility(View.VISIBLE);
        }

        if (hasClipboardCoordinates()) {
            binding.clipboard.setOnClickListener(new ClipboardListener());
            binding.clipboard.setVisibility(View.VISIBLE);
        }

        if (noTitle) {
            binding.done.setVisibility(View.GONE);
        } else {
            binding.done.setOnClickListener(view -> saveAndFinishDialog());
        }

        return binding.getRoot();
    }

    @Override
    public void onAttach(@NonNull final Activity activity) {
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

    // splitting up that method would not help improve readability
    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"})
    private void config(final boolean plain, final String sep1, final String sep2, final String sep3) {
        eLatFrame.setVisibility(plain ? View.VISIBLE : View.GONE);
        eLonFrame.setVisibility(plain ? View.VISIBLE : View.GONE);
        binding.input.latLonFrame.setVisibility(plain ? View.GONE : View.VISIBLE);
        if (!plain) {
            final boolean bSecVisible = !StringUtils.equals(sep1, ".");
            final boolean bFracVisible = StringUtils.equals(sep3, ".");

            tLatSep1.setText(sep1);
            tLonSep1.setText(sep1);
            eLatMin.setHint(bSecVisible ? R.string.cc_hint_minutes : R.string.cc_hint_fraction);
            eLatMin.setGravity(bSecVisible ? Gravity.RIGHT : Gravity.NO_GRAVITY);
            eLonMin.setHint(bSecVisible ? R.string.cc_hint_minutes : R.string.cc_hint_fraction);
            eLonMin.setGravity(bSecVisible ? Gravity.RIGHT : Gravity.NO_GRAVITY);

            tLatSep2.setText(sep2);
            tLonSep2.setText(sep2);
            eLatSec.setVisibility(bSecVisible ? View.VISIBLE : View.GONE);
            eLatSec.setHint(bFracVisible ? R.string.cc_hint_seconds : R.string.cc_hint_fraction);
            eLatSec.setGravity(bFracVisible ? Gravity.RIGHT : Gravity.LEFT);
            eLonSec.setVisibility(bSecVisible ? View.VISIBLE : View.GONE);
            eLonSec.setHint(bFracVisible ? R.string.cc_hint_seconds : R.string.cc_hint_fraction);
            eLonSec.setGravity(bFracVisible ? Gravity.RIGHT : Gravity.LEFT);

            tLatSep3.setText(sep3);
            tLonSep3.setText(sep3);
            eLatSub.setVisibility(bFracVisible ? View.VISIBLE : View.GONE);
            eLatSub.setHint(R.string.cc_hint_fraction);
            eLonSub.setVisibility(bFracVisible ? View.VISIBLE : View.GONE);
            eLonSub.setHint(R.string.cc_hint_fraction);
        }
    }

    @SuppressWarnings({"PMD.NPathComplexity", "PMD.ExcessiveMethodLength"}) // not now
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
                config(true, "", "", "");
                if (gp != null) {
                    eLat.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    eLon.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                }
                break;
            case Deg: // DDD.DDDDD°
                config(false, ".", "°", "");
                if (gp != null) {
                    eLatDeg.setText(addZeros(gp.getDecDegreeLatDeg(), 2));
                    eLatMin.setText(addZeros(gp.getDecDegreeLatDegFrac(), 5));
                    eLonDeg.setText(addZeros(gp.getDecDegreeLonDeg(), 3));
                    eLonMin.setText(addZeros(gp.getDecDegreeLonDegFrac(), 5));
                }
                break;
            case Min: // DDD° MM.MMM
                config(false, "°", ".", "'");
                if (gp != null) {
                    eLatDeg.setText(addZeros(gp.getDecMinuteLatDeg(), 2));
                    eLatMin.setText(addZeros(gp.getDecMinuteLatMin(), 2));
                    eLatSec.setText(addZeros(gp.getDecMinuteLatMinFrac(), 3));
                    eLonDeg.setText(addZeros(gp.getDecMinuteLonDeg(), 3));
                    eLonMin.setText(addZeros(gp.getDecMinuteLonMin(), 2));
                    eLonSec.setText(addZeros(gp.getDecMinuteLonMinFrac(), 3));
                }
                break;
            case Sec: // DDD° MM SS.SSS
                config(false, "°", "'", ".");
                if (gp != null) {
                    eLatDeg.setText(addZeros(gp.getDMSLatDeg(), 2));
                    eLatMin.setText(addZeros(gp.getDMSLatMin(), 2));
                    eLatSec.setText(addZeros(gp.getDMSLatSec(), 2));
                    eLatSub.setText(addZeros(gp.getDMSLatSecFrac(), 3));
                    eLonDeg.setText(addZeros(gp.getDMSLonDeg(), 3));
                    eLonMin.setText(addZeros(gp.getDMSLonMin(), 2));
                    eLonSec.setText(addZeros(gp.getDMSLonSec(), 2));
                    eLonSub.setText(addZeros(gp.getDMSLonSecFrac(), 3));
                }
                break;
        }

        for (final EditText editText : orderedInputs) {
            setSize(editText);
        }

        binding.calculateGlobal.setTypeface(null, inputData != null && inputData.getCalculatedCoordinate() != null && inputData.getCalculatedCoordinate().isFilled() ?
                Typeface.ITALIC : Typeface.NORMAL);
    }

    private Pair<String, String> getLatLonPatternFromGui() {
        String lat = null;
        String lon = null;
        switch (currentFormat) {
            case Deg: // DDD.DDDDD°
                lat = bLat.getText().toString() + eLatDeg.getText() + "." + eLatMin.getText() + "°";
                lon = bLon.getText().toString() + eLonDeg.getText() + "." + eLonMin.getText() + "°";
                break;
            case Min: // DDD° MM.MMM
                lat = bLat.getText().toString() + eLatDeg.getText() + "°" + eLatMin.getText() + "." + eLatSec.getText() + "'";
                lon = bLon.getText().toString() + eLonDeg.getText() + "°" + eLonMin.getText() + "." + eLonSec.getText() + "'";
                break;
            case Sec: // DDD° MM SS.SSS
                lat = bLat.getText().toString() + eLatDeg.getText() + "°" + eLatMin.getText() + "'" + eLatSec.getText() + "." + eLatSub.getText() + "\"";
                lon = bLon.getText().toString() + eLonDeg.getText() + "°" + eLonMin.getText() + "'" + eLonSec.getText() + "." + eLonSub.getText() + "\"";
                break;
            case Plain:
            default:
                lat = eLat.getText().toString();
                lon = eLon.getText().toString();
                break;
        }
        return new Pair<>(lat, lon);

    }

    private void setSize(final EditText someEditText) {
        if (someEditText.getVisibility() == View.GONE) {
            return;
        }
        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.weight = someEditText == eLatDeg ? 3 : getMaxLengthFromCurrentField(someEditText);
        someEditText.setLayoutParams(lp);
    }

    private static String addZeros(final int value, final int len) {
        return StringUtils.leftPad(Integer.toString(value), len, '0');
    }


    private static class ButtonClickListener implements View.OnClickListener {

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

    private void saveAndFinishDialog() {
        if (!areCurrentCoordinatesValid(true)) {
            return;
        }
        inputData.setCalculatedCoordinate(null);
        inputData.setGeopoint(gp);
        ((CoordinateUpdate) requireActivity()).updateCoordinates(inputData);
        dismiss();
    }

    private class ClearCoordinatesListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            inputData.setCalculatedCoordinate(null);
            inputData.setGeopoint(null);
            ((CoordinateUpdate) getActivity()).updateCoordinates(inputData);
            dismiss();
        }
    }

    public interface CoordinateUpdate {
        void updateCoordinates(Geopoint gp);

        boolean supportsNullCoordinates();

        default void updateCoordinates(CoordinateInputData coordinateInputData) {
            updateCoordinates(coordinateInputData.getGeopoint());
        }
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
