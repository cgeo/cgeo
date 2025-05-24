package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.databinding.NewCoordinateInputDialogBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.models.CalculatedCoordinate;
import cgeo.geocaching.models.CalculatedCoordinateType;
import cgeo.geocaching.models.CoordinateInputData;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.GeoDirHandler;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import com.google.android.material.textfield.TextInputLayout;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;

// A recreation of the existing coordinate dialog
public class NewCoordinateInputDialog {

    private final Context context;
    private final DialogCallback callback;
    Spinner spinner;
    private Settings.CoordInputFormatEnum currentFormat = null;

    private NewCoordinateInputDialogBinding binding;

    private TextInputLayout eLatFrame, eLonFrame;
    private EditText plainLatitude, plainLongitude;
    private LinearLayout configurableLatitude, configurableLongitude;
    private Button bLatitude, bLongitude;
    private EditText longitudeDegree, longitudeMinutes, longitudeSeconds, longitudeFraction;
    private EditText latitudeDegree, latitudeMinutes, latitudeSeconds, latitudeFraction;
    private TextView latSymbol1, latSymbol2, latSymbol3, latSymbol4;
    private TextView lonSymbol1, lonSymbol2, lonSymbol3, lonSymbol4;
    private List<EditText> orderedInputs;
    private Geopoint gp;
    private static Geopoint cacheCoordinates;
    private Disposable geoDisposable;
    private boolean showWaypointOptions = false;
    private final GeoDirHandler geoUpdate = new GeoDirHandler() {
        @Override
        public void updateGeoData(final GeoData geo) {
            final String label = context.getString(R.string.waypoint_my_coordinates_accuracy, Units.getDistanceFromMeters(geo.getAccuracy()));
            binding.current.setText(label);
        }
    };

    private NewCoordinateInputDialog(final Context context, final DialogCallback callback, final boolean showWaypointButtons) {

        this.context = context;
        this.callback = callback;
        this.showWaypointOptions = showWaypointButtons;
    }

    // Entry point for user defined cache, search card and GK TB
    public static void show(final Context context, final DialogCallback callback, final Geopoint location) {
        cacheCoordinates = null;
        new NewCoordinateInputDialog(context, callback, false).show(location);
    }

    //Entry point for Waypoint page
    public static void show(final Context context, final DialogCallback callback, final CoordinateInputData inputData) {

        final String geocode = inputData.getGeocode();
        if (!StringUtils.isBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            cacheCoordinates = cache == null ? null : cache.getCoords();
        }

        if (inputData.getCalculatedCoordinate() != null && inputData.getCalculatedCoordinate().isFilled()) {
            final AbstractActivity activity = (AbstractActivity) context;
            final androidx.fragment.app.FragmentManager fragmentManager = activity.getSupportFragmentManager();
            CoordinatesCalculateGlobalDialog.show(fragmentManager, inputData);
            return;
        }
        new NewCoordinateInputDialog(context, callback, true).show(null);
    }

    @NonNull
    private static Geopoint currentCoords() {

        return LocationDataProvider.getInstance().currentGeo().getCoords();
    }

    private void show(final Geopoint location) {

        if (location != null) {
            gp = location;
        } else {
            gp = currentCoords();
        }

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View theView = inflater.inflate(R.layout.new_coordinate_input_dialog, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(theView);

        final AlertDialog dialog = builder.create();

        binding = NewCoordinateInputDialogBinding.bind(theView);

        // Show title and action buttons
        final Toolbar toolbar = binding.actionbar.toolbar;
        toolbar.setTitle(R.string.cache_coordinates);
        toolbar.inflateMenu(R.menu.menu_ok_cancel);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_item_save) {
               if (saveAndFinishDialog()) {
                   geoDisposable.dispose();
                   dialog.dismiss();
               }
            } else {
                geoDisposable.dispose();
                dialog.dismiss();
            }
            return true;
        });

        // Populate the spinner with options
        spinner = binding.dialogSpinner;
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.waypoint_coordinate_formats, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Settings.getCoordInputFormat().ordinal());

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                currentFormat = Settings.CoordInputFormatEnum.fromInt(position);
                Settings.setCoordInputFormat(currentFormat);
                updateGui();
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                Toast.makeText(context, "Nothing Selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Populate the text fields
        eLatFrame = binding.latitudeFrame;
        eLonFrame = binding.longitudeFrame;

        plainLatitude = binding.latitude;
        plainLongitude = binding.longitude;

        configurableLatitude = binding.configurableLatitude;
        configurableLongitude = binding.configurableLongitude;

        bLatitude = binding.hemisphereLatitude;
        bLongitude = binding.hemisphereLongitude;

        latitudeDegree = binding.editTextLatDegrees;
        latitudeMinutes = binding.editTextLatMinutes;
        latitudeSeconds = binding.editTextLatSeconds;
        latitudeFraction = binding.editTextLatFraction;

        latSymbol1 = binding.txtLatSymbol1;
        latSymbol2 = binding.txtLatSymbol2;
        latSymbol3 = binding.txtLatSymbol3;
        latSymbol4 = binding.txtLatSymbol4;

        longitudeDegree = binding.editTextLonDegrees;
        longitudeMinutes = binding.editTextLonMinutes;
        longitudeSeconds = binding.editTextLonSeconds;
        longitudeFraction = binding.editTextLonFraction;

        lonSymbol1 = binding.txtLonSymbol1;
        lonSymbol2 = binding.txtLonSymbol2;
        lonSymbol3 = binding.txtLonSymbol3;
        lonSymbol4 = binding.txtLonSymbol4;

        // Handle the hemisphere buttons
        bLatitude.setOnClickListener(v -> {
            final CharSequence text = bLatitude.getText();
            if (text.equals("N")) {
                bLatitude.setText("S");
            } else {
                bLatitude.setText("N");
            }
        });

        bLongitude.setOnClickListener(v -> {
                    final CharSequence text = bLongitude.getText();
                    if (text.equals("E")) {
                        bLongitude.setText("W");
                    } else {
                        bLongitude.setText("E");
                    }
        });

        // Handle the text fields
        orderedInputs = Arrays.asList(latitudeDegree, latitudeMinutes, latitudeSeconds, latitudeFraction,
                  longitudeDegree, longitudeMinutes, longitudeSeconds, longitudeFraction);

        for (final EditText editText : orderedInputs) {
            editText.addTextChangedListener(new SwitchToNextFieldWatcher(editText));
            editText.setOnFocusChangeListener(new PadZerosOnFocusLostListener());
            EditUtils.disableSuggestions(editText);
        }

        // Manage the options buttons
        final Button useCurrentLocation = binding.current;
        final Button useCacheCoordinates = binding.cache;
        final Button calculateCoordinates = binding.calculate;
        final Button copyFromClipboard = binding.clipboard;
        final Button clearCoordinates = binding.clear;

        useCurrentLocation.setOnClickListener(v -> {
            gp = currentCoords();
            updateGui();
        });

        if (cacheCoordinates == null) {
            useCacheCoordinates.setVisibility(View.GONE);
        } else {
            useCacheCoordinates.setVisibility(View.VISIBLE);
            useCacheCoordinates.setOnClickListener(v -> {
                gp = cacheCoordinates;
                updateGui();
            });
        }

        // For waypoints only, launch the calculator dialog that is still fragment based atm
        if (showWaypointOptions) {
            calculateCoordinates.setVisibility(View.VISIBLE);

            calculateCoordinates.setOnClickListener(v -> {
                final AbstractActivity activity = (AbstractActivity) context;
                final androidx.fragment.app.FragmentManager fragmentManager = activity.getSupportFragmentManager();

                final CoordinateInputData inputData = new CoordinateInputData();
                inputData.setGeopoint(gp);
                final CalculatedCoordinate cc = new CalculatedCoordinate();
                cc.setType(CalculatedCoordinateType.values()[spinner.getSelectedItemPosition()]);

                //try to set patterns from GUI
                final Pair<String, String> patternsFromGui = getLatLonPatternFromGui();
                cc.setLatitudePattern(patternsFromGui.first);
                cc.setLongitudePattern(patternsFromGui.second);

                inputData.setCalculatedCoordinate(cc);
                CoordinatesCalculateGlobalDialog.show(fragmentManager, inputData);
                geoDisposable.dispose();
                dialog.dismiss();
            });
        } else  {
            calculateCoordinates.setVisibility(View.GONE);
        }

        copyFromClipboard.setOnClickListener(v -> {
            try {
                gp = new Geopoint(StringUtils.defaultString(ClipboardUtils.getText()));
                updateGui();
            } catch (final Geopoint.ParseException ignored) {
                //ignore
            }
        });

        if (showWaypointOptions) {
            clearCoordinates.setVisibility(View.VISIBLE);
            clearCoordinates.setOnClickListener( v -> {
                callback.onDialogClosed(null);
                dialog.dismiss();
            });
        } else {
            clearCoordinates.setVisibility(View.GONE);
        }

        dialog.show();

        geoDisposable = geoUpdate.start(GeoDirHandler.UPDATE_GEODATA);
    }

    // Close dialog and return selected coordinates to caller
    private boolean saveAndFinishDialog() {

        final String result = readGui();

        try {
            final Geopoint entered = new Geopoint(result);
            if (entered.isValid()) {
                // Invoke the callback to notify that the dialog is closed
                callback.onDialogClosed(entered);
                return true;
            }
        } catch (Geopoint.ParseException e) {
            Toast.makeText(context, e.resource, Toast.LENGTH_SHORT).show();
            return false;
        }
        return false;
    }

    // Extract coordinates from the data fields
    private String readGui() {

        if (currentFormat.equals(Settings.CoordInputFormatEnum.Plain)) {
            return plainLatitude.getText().toString() + " " + plainLongitude.getText().toString();
        }

        String lat = bLatitude.getText().toString();
        String lon = bLongitude.getText().toString();

        if (currentFormat.equals(Settings.CoordInputFormatEnum.Deg)) {
            lat = "S".equalsIgnoreCase(lat) ? "-" : "";
            lon = "W".equalsIgnoreCase(lon) ? "-" : "";
        }

        lat += String.valueOf(latitudeDegree.getText());
        lon += String.valueOf(longitudeDegree.getText());

        switch (currentFormat) {
            case Min:
                lat += " " + latitudeMinutes.getText() + "." + latitudeFraction.getText();
                lon += " " + longitudeMinutes.getText() + "." + longitudeFraction.getText();
                break;
            case Sec:
                lat += " " + latitudeMinutes.getText() + " " + latitudeSeconds.getText() + "." + latitudeFraction.getText();
                lon += " " + longitudeMinutes.getText() + " " + longitudeSeconds.getText() + "." + longitudeFraction.getText();
                break;
            case Deg:
                lat += "." + latitudeFraction.getText();
                lon += "." + longitudeFraction.getText();
                break;
        }
        return lat + " " + lon;
    }

    // Refresh the text fields according to the selected coordinate format
    private void updateGui() {

        if (currentFormat.equals(Settings.CoordInputFormatEnum.Plain)) {
            eLatFrame.setVisibility(View.VISIBLE);
            eLonFrame.setVisibility(View.VISIBLE);
            plainLatitude.setVisibility(View.VISIBLE);
            plainLongitude.setVisibility(View.VISIBLE);
            plainLatitude.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
            plainLongitude.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
            configurableLatitude.setVisibility(View.GONE);
            configurableLongitude.setVisibility(View.GONE);
            return;
        }

        plainLatitude.setVisibility(View.GONE);
        plainLongitude.setVisibility(View.GONE);
        eLatFrame.setVisibility(View.GONE);
        eLonFrame.setVisibility(View.GONE);
        configurableLatitude.setVisibility(View.VISIBLE);
        configurableLongitude.setVisibility(View.VISIBLE);

        if (gp != null) {
            bLatitude.setText(String.valueOf(gp.getLatDir()));
            bLongitude.setText(String.valueOf(gp.getLonDir()));
        } else {
            bLatitude.setText(String.valueOf(currentCoords().getLatDir()));
            bLongitude.setText(String.valueOf(currentCoords().getLonDir()));
        }

        switch (currentFormat) {
            case Min:
                latitudeDegree.setVisibility(View.VISIBLE);
                latitudeDegree.setText(addZeros(gp.getDecMinuteLatDeg(), 2));
                latSymbol1.setText("°");

                latitudeMinutes.setVisibility(View.VISIBLE);
                latitudeMinutes.setText(addZeros(gp.getDecMinuteLatMin(), 2));
                latSymbol2.setVisibility(View.VISIBLE);
                latSymbol2.setText(".");

                latitudeSeconds.setVisibility(View.GONE);
                latSymbol3.setVisibility(View.GONE);

                latitudeFraction.setVisibility(View.VISIBLE);
                latitudeFraction.setText(addZeros(gp.getDecMinuteLatMinFrac(), 3));
                latSymbol4.setVisibility(View.VISIBLE);
                latSymbol4.setText("'");


                longitudeDegree.setVisibility(View.VISIBLE);
                longitudeDegree.setText(addZeros(gp.getDecMinuteLonDeg(), 3));
                lonSymbol1.setText("°");

                longitudeMinutes.setVisibility(View.VISIBLE);
                longitudeMinutes.setText(addZeros(gp.getDecMinuteLonMin(), 2));
                lonSymbol2.setVisibility(View.VISIBLE);
                lonSymbol2.setText(".");

                longitudeSeconds.setVisibility(View.GONE);
                lonSymbol3.setVisibility(View.GONE);

                longitudeFraction.setVisibility(View.VISIBLE);
                longitudeFraction.setText(addZeros(gp.getDecMinuteLonMinFrac(), 3));
                lonSymbol4.setVisibility(View.VISIBLE);
                lonSymbol4.setText("'");
                break;

            case Sec:
                latitudeDegree.setVisibility(View.VISIBLE);
                latitudeDegree.setText(addZeros(gp.getDecDegreeLatDeg(), 2));
                latSymbol1.setText("°");

                latitudeMinutes.setVisibility(View.VISIBLE);
                latitudeMinutes.setText(addZeros(gp.getDMSLatMin(), 2));
                latSymbol2.setVisibility(View.VISIBLE);
                latSymbol2.setText("'");

                latitudeSeconds.setVisibility(View.VISIBLE);
                latitudeSeconds.setText(addZeros(gp.getDMSLatSec(), 2));
                latSymbol3.setVisibility(View.VISIBLE);
                latSymbol3.setText(".");

                latitudeFraction.setVisibility(View.VISIBLE);
                latitudeFraction.setText(addZeros(gp.getDMSLatSecFrac(), 3));
                latSymbol4.setVisibility(View.VISIBLE);
                latSymbol4.setText("\"");

                longitudeDegree.setVisibility(View.VISIBLE);
                longitudeDegree.setText(addZeros(gp.getDMSLonDeg(), 3));
                lonSymbol1.setText("°");

                longitudeMinutes.setVisibility(View.VISIBLE);
                longitudeMinutes.setText(addZeros(gp.getDMSLonMin(), 2));
                lonSymbol2.setVisibility(View.VISIBLE);
                lonSymbol2.setText("'");

                longitudeSeconds.setVisibility(View.VISIBLE);
                longitudeSeconds.setText(addZeros(gp.getDMSLonSec(), 2));
                lonSymbol3.setVisibility(View.VISIBLE);
                lonSymbol3.setText(".");

                longitudeFraction.setVisibility(View.VISIBLE);
                longitudeFraction.setText(addZeros(gp.getDMSLonSecFrac(), 3));
                lonSymbol4.setVisibility(View.VISIBLE);
                lonSymbol4.setText("\"");
                break;

            case Deg:
            default:
                latitudeDegree.setVisibility(View.VISIBLE);
                latitudeDegree.setText(addZeros(gp.getDecDegreeLatDeg(), 2));
                latSymbol1.setText(".");

                latitudeMinutes.setVisibility(View.GONE);
                latitudeSeconds.setVisibility(View.GONE);
                latSymbol2.setVisibility(View.GONE);
                latSymbol3.setVisibility(View.GONE);

                latitudeFraction.setVisibility(View.VISIBLE);
                latitudeFraction.setText(addZeros(gp.getDecDegreeLatDegFrac(), 5));
                latSymbol4.setVisibility(View.VISIBLE);
                latSymbol4.setText("°");

                longitudeDegree.setVisibility(View.VISIBLE);
                longitudeDegree.setText(addZeros(gp.getDecDegreeLonDeg(), 3));
                lonSymbol1.setText(".");

                longitudeMinutes.setVisibility(View.GONE);
                longitudeSeconds.setVisibility(View.GONE);
                lonSymbol2.setVisibility(View.GONE);
                lonSymbol3.setVisibility(View.GONE);

                longitudeFraction.setVisibility(View.VISIBLE);
                longitudeFraction.setText(addZeros(gp.getDecDegreeLonDegFrac(), 5));
                lonSymbol4.setVisibility(View.VISIBLE);
                lonSymbol4.setText("°");
                break;
        }
    }

    // Following methods lifted from existing code with minimal changes
    private static String addZeros(final int value, final int len) {

        return StringUtils.leftPad(Integer.toString(value), len, '0');
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

    public int getMaxLengthFromCurrentField(final EditText editText) {

        if ((editText == latitudeFraction || editText == longitudeFraction) && currentFormat == Settings.CoordInputFormatEnum.Deg) {
            return 5;
        }
        if ((editText == latitudeFraction || editText == longitudeFraction) && currentFormat == Settings.CoordInputFormatEnum.Min) {
            return 3;
        }
        if (editText == longitudeDegree || editText == latitudeFraction || editText == longitudeFraction) {
            return 3;
        }
        return 2;
    }

    private Pair<String, String> getLatLonPatternFromGui() {
        String lat = null;
        String lon = null;
        switch (currentFormat) {
            case Deg: // DDD.DDDDD°
                lat = bLatitude.getText().toString() + latitudeDegree.getText() + "." + latitudeFraction.getText() + "°";
                lon = bLongitude.getText().toString() + longitudeDegree.getText() + "." + longitudeFraction.getText() + "°";
                break;
            case Min: // DDD° MM.MMM
                lat = bLatitude.getText().toString() + latitudeDegree.getText() + "°" + latitudeMinutes.getText() + "." + latitudeFraction.getText() + "'";
                lon = bLongitude.getText().toString() + longitudeDegree.getText() + "°" + longitudeMinutes.getText() + "." + longitudeFraction.getText() + "'";
                break;
            case Sec: // DDD° MM SS.SSS
                lat = bLatitude.getText().toString() + latitudeDegree.getText() + "°" + latitudeMinutes.getText() + "'" + latitudeSeconds.getText() + "." + latitudeFraction.getText() + "\"";
                lon = bLongitude.getText().toString() + longitudeDegree.getText() + "°" + longitudeMinutes.getText() + "'" + longitudeSeconds.getText() + "." + longitudeFraction.getText() + "\"";
                break;
            case Plain:
            default:
                lat = bLatitude.getText().toString();
                lon = bLongitude.getText().toString();
                break;
        }
        return new Pair<>(lat, lon);

    }
    private class PadZerosOnFocusLostListener implements View.OnFocusChangeListener {

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
            if (currentFormat == Settings.CoordInputFormatEnum.Plain) {
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
}

