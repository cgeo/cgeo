package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.NewCoordinateInputDialogBinding;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

// A recreation of the existing coordinate dialog
public class NewCoordinateInputDialog {

    private final Context context;
    private final DialogCallback callback;
    Spinner spinner;
    private Settings.CoordInputFormatEnum currentFormat = null;
    private EditText plainLatitude, plainLongitude;
    private LinearLayout configurableLatitude, configurableLongitude;
    private Button bLatitude, bLongitude;
    private EditText longitudeDegree, longitudeMinutes, longitudeSeconds, longitudeFraction;
    private EditText latitudeDegree, latitudeMinutes, latitudeSeconds, latitudeFraction;
    private List<EditText> orderedInputs;
    private Geopoint gp, cacheCoords;

    public NewCoordinateInputDialog(final Context context, final DialogCallback callback) {

        this.context = context;
        this.callback = callback;
    }

    @NonNull
    private static Geopoint currentCoords() {

        return LocationDataProvider.getInstance().currentGeo().getCoords();
    }

    // Will be used later by GeoKrety TB page
    public void show(final Geopoint location, final Geocache cache) {

        if (cache != null) {
            cacheCoords = cache.getCoords();
        }
        show(location);
    }

    // Main entry point for distance filter and user defined cache coordinates
    public void show(final Geopoint location) {

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
        dialog.show();

        final NewCoordinateInputDialogBinding binding = NewCoordinateInputDialogBinding.bind(theView);

        // Show title and action buttons
        final Toolbar toolbar = binding.actionbar.toolbar;
        toolbar.setTitle(R.string.cache_coordinates);
        toolbar.inflateMenu(R.menu.menu_ok_cancel);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_item_save) {
               if (saveAndFinishDialog()) {
                   dialog.dismiss();
               }
            } else {
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

        longitudeDegree = binding.editTextLonDegrees;
        longitudeMinutes = binding.editTextLonMinutes;
        longitudeSeconds = binding.editTextLonSeconds;
        longitudeFraction = binding.editTextLonFraction;

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

        // User copy/paste buttons
        final Button copyFromClipboard = binding.clipboard;
        final Button useCurrentLocation = binding.current;

        copyFromClipboard.setOnClickListener(v -> {
            try {
                gp = new Geopoint(StringUtils.defaultString(ClipboardUtils.getText()));
                updateGui();
            } catch (final Geopoint.ParseException ignored) {
                //ignore
            }
        });

        useCurrentLocation.setOnClickListener(v -> {
            gp = currentCoords();
            updateGui();
        });
    }

    // Close dialog and return selected coordinates to caller
    private boolean saveAndFinishDialog() {

        final String result = readGui();

        try {
            final Geopoint entered = new Geopoint(result);
            if (entered.isValid()) {
                // Invoke the callback to notify that the dialog is closed
                callback.onDialogClosed(result);
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
            plainLatitude.setVisibility(View.VISIBLE);
            plainLongitude.setVisibility(View.VISIBLE);
            plainLatitude.setText(String.valueOf(gp.getLatitude()));
            plainLongitude.setText(String.valueOf(gp.getLongitude()));
            configurableLatitude.setVisibility(View.GONE);
            configurableLongitude.setVisibility(View.GONE);
            return;
        }

        plainLatitude.setVisibility(View.GONE);
        plainLongitude.setVisibility(View.GONE);
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

                latitudeMinutes.setVisibility(View.VISIBLE);
                latitudeMinutes.setText(addZeros(gp.getDecMinuteLatMin(), 2));

                latitudeSeconds.setVisibility(View.GONE);

                latitudeFraction.setVisibility(View.VISIBLE);
                latitudeFraction.setText(addZeros(gp.getDecMinuteLatMinFrac(), 3));

                longitudeDegree.setVisibility(View.VISIBLE);
                longitudeDegree.setText(addZeros(gp.getDecMinuteLonDeg(), 3));

                longitudeMinutes.setVisibility(View.VISIBLE);
                longitudeMinutes.setText(addZeros(gp.getDecMinuteLonMin(), 2));

                longitudeSeconds.setVisibility(View.GONE);

                longitudeFraction.setVisibility(View.VISIBLE);
                longitudeFraction.setText(addZeros(gp.getDecMinuteLonMinFrac(), 3));
                break;

            case Sec:
                latitudeDegree.setVisibility(View.VISIBLE);
                latitudeDegree.setText(addZeros(gp.getDecDegreeLatDeg(), 2));

                latitudeMinutes.setVisibility(View.VISIBLE);
                latitudeMinutes.setText(addZeros(gp.getDMSLatMin(), 2));

                latitudeSeconds.setVisibility(View.VISIBLE);
                latitudeSeconds.setText(addZeros(gp.getDMSLatSec(), 2));

                latitudeFraction.setVisibility(View.VISIBLE);
                latitudeFraction.setText(addZeros(gp.getDMSLatSecFrac(), 3));

                longitudeDegree.setVisibility(View.VISIBLE);
                longitudeDegree.setText(addZeros(gp.getDMSLonDeg(), 3));

                longitudeMinutes.setVisibility(View.VISIBLE);
                longitudeMinutes.setText(addZeros(gp.getDMSLonMin(), 2));

                longitudeSeconds.setVisibility(View.VISIBLE);
                longitudeSeconds.setText(addZeros(gp.getDMSLonSec(), 2));

                longitudeFraction.setVisibility(View.VISIBLE);
                longitudeFraction.setText(addZeros(gp.getDMSLonSecFrac(), 3));
                break;

            case Deg:
            default:
                latitudeDegree.setVisibility(View.VISIBLE);
                latitudeDegree.setText(addZeros(gp.getDecDegreeLatDeg(), 2));

                latitudeMinutes.setVisibility(View.GONE);
                latitudeSeconds.setVisibility(View.GONE);

                latitudeFraction.setVisibility(View.VISIBLE);
                latitudeFraction.setText(addZeros(gp.getDecDegreeLatDegFrac(), 5));

                longitudeDegree.setVisibility(View.VISIBLE);
                longitudeDegree.setText(addZeros(gp.getDecDegreeLonDeg(), 3));

                longitudeMinutes.setVisibility(View.GONE);
                longitudeSeconds.setVisibility(View.GONE);

                longitudeFraction.setVisibility(View.VISIBLE);
                longitudeFraction.setText(addZeros(gp.getDecDegreeLonDegFrac(), 5));
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

