package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.address.AndroidGeocoder;
import cgeo.geocaching.address.OsmNominatumGeocoder;
import cgeo.geocaching.databinding.CoordinateInputDialogBinding;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.RDPoint;
import cgeo.geocaching.location.SwissGridPoint;
import cgeo.geocaching.location.UTMPoint;
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
import cgeo.geocaching.unifiedmap.DefaultMap;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Address;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.google.android.material.textfield.TextInputLayout;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;

// A recreation of the original coordinate dialog as an Alert based dialog
public class CoordinateInputDialog {

    private final Context context;
    private final DialogCallback callback;
    Spinner spinner;
    private Settings.CoordInputFormatEnum currentFormat = null;

    private CoordinateInputDialogBinding binding;

    private TextInputLayout eLatFrame, eLonFrame, eThirdFrame;
    private EditText plainLatitude, plainLongitude, plainThird;
    private LinearLayout configurableLatitude, configurableLongitude;
    private Button bLatitude, bLongitude;
    private EditText longitudeDegree, longitudeMinutes, longitudeSeconds, longitudeFraction;
    private EditText latitudeDegree, latitudeMinutes, latitudeSeconds, latitudeFraction;
    private TextView latSymbol1, latSymbol2, latSymbol3, latSymbol4;
    private TextView lonSymbol1, lonSymbol2, lonSymbol3, lonSymbol4;
    private View quickMapTargetButton;
    private TextView quickMapCoordinates;
    private TextView quickMapCountry;
    private TextView quickMapDistanceDirection;
    private List<EditText> orderedInputs;
    private Geopoint gp;
    private static Geopoint cacheCoordinates;
    private Geopoint quickMapTarget;
    private Geopoint quickMapCountryTarget;
    private Disposable geoDisposable;
    private Disposable reverseGeocodeDisposable;
    private CoordinateDialogDisplayModeEnum waypointOptions = CoordinateDialogDisplayModeEnum.Normal;
    private final GeoDirHandler geoUpdate = new GeoDirHandler() {
        @Override
        public void updateGeoData(final GeoData geo) {
            final String label = LocalizationUtils.getString(R.string.waypoint_my_coordinates_accuracy, Units.getDistanceFromMeters(geo.getAccuracy()));
            binding.current.setText(label);
            updateQuickMapDistanceAndDirection(quickMapTarget, geo.getCoords());
        }
    };

    private CoordinateInputDialog(final Context context, final DialogCallback callback, final CoordinateDialogDisplayModeEnum showWaypointButtons) {

        this.context = context;
        this.callback = callback;
        this.waypointOptions = showWaypointButtons;
    }

    // Entry point for user defined cache, search card and GK TB
    public static void showLocation(final Context context, final DialogCallback callback, final Geopoint location) {
        cacheCoordinates = null;
        final CoordinateInputData cid = new CoordinateInputData();
        cid.setGeopoint(location);
        new CoordinateInputDialog(context, callback, CoordinateDialogDisplayModeEnum.Normal).show(cid);
    }

    //Entry point for a plain waypoint returning from the calculator page
    public static void showSimple(final Context context, final DialogCallback callback, final CoordinateInputData inputData) {
        cacheCoordinates = null;
        new CoordinateInputDialog(context, callback, CoordinateDialogDisplayModeEnum.Simple).show(inputData);
    }

    //Main entry point for the waypoint page
    public static void show(final Context context, final DialogCallback callback, final CoordinateInputData inputData) {

        final String geocode = inputData.getGeocode();
        if (!StringUtils.isBlank(geocode)) {
            final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
            cacheCoordinates = cache == null ? null : cache.getCoords();
        }

        if (inputData.getCalculatedCoordinate() != null && inputData.getCalculatedCoordinate().isFilled()) {
            final AbstractActivity activity = (AbstractActivity) context;
            final androidx.fragment.app.FragmentManager fragmentManager = activity.getSupportFragmentManager();
            CoordinatesCalculateGlobalDialog.show(fragmentManager, callback, inputData);
            return;
        }
        new CoordinateInputDialog(context, callback, CoordinateDialogDisplayModeEnum.Waypoint).show(inputData);
    }

    @NonNull
    private static Geopoint currentCoords() {

        return LocationDataProvider.getInstance().currentGeo().getCoords();
    }

    private void show(final CoordinateInputData inputData) {

        gp = inputData.getGeopoint();
        if (gp == null) {
            gp = currentCoords();
        }

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View theView = inflater.inflate(R.layout.coordinate_input_dialog, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(theView);

        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> disposeDialogResources());

        binding = CoordinateInputDialogBinding.bind(theView);

        // Show title and action buttons
        final Toolbar toolbar = binding.actionbar.toolbar;
        toolbar.setTitle(R.string.cache_coordinates);
        toolbar.inflateMenu(R.menu.menu_ok_cancel);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_item_save) {
               if (saveAndFinishDialog()) {
                   disposeDialogResources();
                   dialog.dismiss();
               }
            } else {
                disposeDialogResources();
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
        eThirdFrame = binding.thirdCoordinateFrame;

        plainLatitude = binding.latitude;
        plainLongitude = binding.longitude;
        plainThird = binding.thirdCoordinate;

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

        quickMapTargetButton = binding.quickMapTargetButton;
        quickMapCoordinates = binding.quickMapCoordinates;
        quickMapCountry = binding.quickMapCountry;
        quickMapDistanceDirection = binding.quickMapDistanceDirection;
        quickMapTargetButton.setOnClickListener(v -> {
            final Geopoint parsedInput = parseCurrentInputAsGeopoint();
            if (parsedInput == null) {
                hideQuickMapTargetButton();
                return;
            }
            disposeDialogResources();
            dialog.dismiss();
            DefaultMap.startActivityInitialCoords(context, parsedInput);
        });

        // Handle the hemisphere buttons
        bLatitude.setOnClickListener(v -> {
            final CharSequence text = bLatitude.getText();
            if (text.equals("N")) {
                bLatitude.setText("S");
            } else {
                bLatitude.setText("N");
            }
            refreshQuickMapTargetButton();
        });

        bLongitude.setOnClickListener(v -> {
                    final CharSequence text = bLongitude.getText();
                    if (text.equals("E")) {
                        bLongitude.setText("W");
                    } else {
                        bLongitude.setText("E");
                    }
                    refreshQuickMapTargetButton();
        });

        // Handle the text fields
        orderedInputs = Arrays.asList(latitudeDegree, latitudeMinutes, latitudeSeconds, latitudeFraction,
                  longitudeDegree, longitudeMinutes, longitudeSeconds, longitudeFraction);

        for (final EditText editText : orderedInputs) {
            editText.addTextChangedListener(new SwitchToNextFieldWatcher(editText));
            editText.setOnFocusChangeListener(new PadZerosOnFocusLostListener());
            EditUtils.disableSuggestions(editText);
        }

        final TextWatcher quickMapPreviewWatcher = new QuickMapPreviewWatcher();
        plainLatitude.addTextChangedListener(quickMapPreviewWatcher);
        plainLongitude.addTextChangedListener(quickMapPreviewWatcher);
        plainThird.addTextChangedListener(quickMapPreviewWatcher);

        // Manage the options buttons
        final Button useCurrentLocation = binding.current;
        final Button useCacheCoordinates = binding.cache;
        final Button calculateCoordinates = binding.calculate;
        final Button copyFromClipboard = binding.clipboard;
        final Button clearCoordinates = binding.clear;

        // Do noy display any option buttons if simple mode
        if (waypointOptions == CoordinateDialogDisplayModeEnum.Simple) {
            useCurrentLocation.setVisibility(View.GONE);
            useCacheCoordinates.setVisibility(View.GONE);
            calculateCoordinates.setVisibility(View.GONE);
            copyFromClipboard.setVisibility(View.GONE);
            clearCoordinates.setVisibility(View.GONE);
            quickMapTargetButton.setVisibility(View.GONE);
        } else {
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
            if (waypointOptions != CoordinateDialogDisplayModeEnum.Normal) {
                calculateCoordinates.setVisibility(View.VISIBLE);

                calculateCoordinates.setOnClickListener(v -> {
                    final AbstractActivity activity = (AbstractActivity) context;
                    final androidx.fragment.app.FragmentManager fragmentManager = activity.getSupportFragmentManager();

                    final CalculatedCoordinate cc = new CalculatedCoordinate();
                    cc.setType(getCalculatedCoordinateTypeForCurrentFormat());

                    //try to set patterns from GUI
                    final Pair<String, String> patternsFromGui = getLatLonPatternFromGui();
                    cc.setLatitudePattern(patternsFromGui.first);
                    cc.setLongitudePattern(patternsFromGui.second);

                    inputData.setCalculatedCoordinate(cc);
                    CoordinatesCalculateGlobalDialog.show(fragmentManager, callback, inputData);
                    disposeDialogResources();
                    dialog.dismiss();
                });
            } else {
                calculateCoordinates.setVisibility(View.GONE);
            }

            if (hasClipboardCoordinates()) {
                copyFromClipboard.setVisibility(View.VISIBLE);
                copyFromClipboard.setOnClickListener(v -> {
                    try {
                        gp = new Geopoint(StringUtils.defaultString(ClipboardUtils.getText()));
                        updateGui();
                    } catch (final Geopoint.ParseException ignored) {
                        //ignore
                    }
                });
            } else {
                copyFromClipboard.setVisibility(View.GONE);
            }

            if (waypointOptions == CoordinateDialogDisplayModeEnum.Waypoint) {
                clearCoordinates.setVisibility(View.VISIBLE);
                clearCoordinates.setOnClickListener(v -> {
                    callback.onDialogClosed(null);
                    disposeDialogResources();
                    dialog.dismiss();
                });
            } else {
                clearCoordinates.setVisibility(View.GONE);
            }
        }

        dialog.show();

        // Make this dialog completely fill the screen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        refreshQuickMapTargetButton();

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

    @Nullable
    private Geopoint parseCurrentInputAsGeopoint() {
        final String result = StringUtils.trimToEmpty(readGui());
        if (StringUtils.isBlank(result)) {
            return null;
        }

        try {
            final Geopoint entered = new Geopoint(result);
            return entered.isValid() ? entered : null;
        } catch (final Geopoint.ParseException ignored) {
            return null;
        }
    }

    private void refreshQuickMapTargetButton() {
        if (quickMapTargetButton == null || waypointOptions == CoordinateDialogDisplayModeEnum.Simple) {
            return;
        }

        if (currentFormat == null) {
            hideQuickMapTargetButton();
            return;
        }

        final Geopoint parsedInput = parseCurrentInputAsGeopoint();
        if (parsedInput == null) {
            hideQuickMapTargetButton();
            return;
        }

        quickMapTarget = parsedInput;
        quickMapTargetButton.setVisibility(View.VISIBLE);
        quickMapCoordinates.setText(parsedInput.format(getQuickMapPreviewFormat(currentFormat)));
        updateQuickMapDistanceAndDirection(parsedInput, currentCoords());
        resolveQuickMapCountry(parsedInput);
    }

    private void hideQuickMapTargetButton() {
        quickMapTarget = null;
        quickMapCountryTarget = null;
        if (reverseGeocodeDisposable != null) {
            reverseGeocodeDisposable.dispose();
            reverseGeocodeDisposable = null;
        }
        if (quickMapTargetButton != null) {
            quickMapTargetButton.setVisibility(View.GONE);
        }
    }

    private void updateQuickMapDistanceAndDirection(@Nullable final Geopoint target, @Nullable final Geopoint source) {
        if (target == null || source == null || quickMapDistanceDirection == null) {
            return;
        }

        final String distance = Units.getDistanceFromKilometers(source.distanceTo(target));
        final String direction = Units.getDirectionFromBearing(source.bearingTo(target));
        quickMapDistanceDirection.setText(StringUtils.isBlank(direction) ? distance : distance + " " + direction);
    }

    private void resolveQuickMapCountry(@NonNull final Geopoint target) {
        if (quickMapCountryTarget != null && quickMapCountryTarget.equals(target)) {
            return;
        }

        quickMapCountryTarget = target;
        quickMapCountry.setText(R.string.coord_input_country_loading);

        if (reverseGeocodeDisposable != null) {
            reverseGeocodeDisposable.dispose();
        }

        reverseGeocodeDisposable = new AndroidGeocoder(context)
                .getFromLocation(target)
                .onErrorResumeNext(throwable -> OsmNominatumGeocoder.getFromLocation(target))
                .observeOn(AndroidRxUtils.mainThreadScheduler)
                .subscribe(address -> {
                    if (quickMapTarget == null || !quickMapTarget.equals(target)) {
                        return;
                    }
                    final String countryName = getLocalizedCountryName(address);
                    quickMapCountry.setText(StringUtils.defaultIfBlank(countryName,
                            LocalizationUtils.getString(R.string.coord_input_country_unknown)));
                }, throwable -> {
                    if (quickMapTarget != null && quickMapTarget.equals(target)) {
                        quickMapCountry.setText(R.string.coord_input_country_unknown);
                    }
                });
    }

    private static String getLocalizedCountryName(@NonNull final Address address) {
        final String countryCode = StringUtils.trimToEmpty(address.getCountryCode());
        if (StringUtils.isNotBlank(countryCode)) {
            final Locale countryLocale = new Locale("", countryCode.toUpperCase(Locale.ROOT));
            final String displayCountry = countryLocale.getDisplayCountry(Locale.getDefault());
            if (StringUtils.isNotBlank(displayCountry)) {
                return displayCountry;
            }
        }
        return StringUtils.defaultString(address.getCountryName());
    }

    private static GeopointFormatter.Format getQuickMapPreviewFormat(@NonNull final Settings.CoordInputFormatEnum format) {
        switch (format) {
            case Deg:
                return GeopointFormatter.Format.LAT_LON_DECDEGREE;
            case Sec:
                return GeopointFormatter.Format.LAT_LON_DECSECOND;
            case UTM:
                return GeopointFormatter.Format.UTM;
            case MGRS:
                return GeopointFormatter.Format.MGRS;
            case OLC:
                return GeopointFormatter.Format.OLC;
            case SwissGrid:
                return GeopointFormatter.Format.SWISS_GRID;
            case RD:
                return GeopointFormatter.Format.RD;
            case Plain:
            case Min:
            default:
                return GeopointFormatter.Format.LAT_LON_DECMINUTE;
        }
    }

    private void disposeDialogResources() {
        if (geoDisposable != null) {
            geoDisposable.dispose();
        }
        if (reverseGeocodeDisposable != null) {
            reverseGeocodeDisposable.dispose();
            reverseGeocodeDisposable = null;
        }
    }

    // Extract coordinates from the data fields
    private String readGui() {

        if (currentFormat.equals(Settings.CoordInputFormatEnum.Plain)) {
            if (StringUtils.isBlank(plainLongitude.getText())) {
                return plainLatitude.getText().toString();
            }
            return plainLatitude.getText().toString() + " " + plainLongitude.getText().toString();
        }

        if (currentFormat.equals(Settings.CoordInputFormatEnum.UTM)) {
            final String zone = plainLatitude.getText().toString().trim();
            final String easting = plainLongitude.getText().toString().trim();
            final String northing = plainThird.getText().toString().trim();
            if (StringUtils.isBlank(easting) && StringUtils.isBlank(northing)) {
                return zone;
            }
            return zone + " E " + easting + " N " + northing;
        }

        if (isSingleInputFormat(currentFormat)) {
            return plainLatitude.getText().toString();
        }

        if (currentFormat.equals(Settings.CoordInputFormatEnum.SwissGrid)) {
            return "LV95 E " + plainLatitude.getText().toString().trim() + " N " + plainLongitude.getText().toString().trim();
        }

        if (currentFormat.equals(Settings.CoordInputFormatEnum.RD)) {
            return "RD X " + plainLatitude.getText().toString().trim() + " Y " + plainLongitude.getText().toString().trim();
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
            default:
                break;
        }
        return lat + " " + lon;
    }

    // Refresh the text fields according to the selected coordinate format
    private void updateGui() {

        final Geopoint geopoint = gp != null ? gp : currentCoords();

        if (currentFormat.equals(Settings.CoordInputFormatEnum.Plain)) {
            showDualPlainInput(
                    geopoint.format(GeopointFormatter.Format.LAT_DECMINUTE),
                    geopoint.format(GeopointFormatter.Format.LON_DECMINUTE),
                    R.string.latitude,
                    R.string.longitude
            );
            return;
        }

        if (currentFormat.equals(Settings.CoordInputFormatEnum.UTM)) {
            final UTMPoint utmPoint = UTMPoint.latLong2UTM(geopoint);
            showTriplePlainInput(
                    utmPoint.getZoneNumber() + String.valueOf(utmPoint.getZoneLetter()),
                    Long.toString(Math.round(utmPoint.getEasting())),
                    Long.toString(Math.round(utmPoint.getNorthing())),
                    R.string.coord_input_zone,
                    R.string.coord_input_easting,
                    R.string.coord_input_northing
            );
            return;
        }

        if (isSingleInputFormat(currentFormat)) {
            final GeopointFormatter.Format format = getSingleInputGeopointFormat(currentFormat);
            showSinglePlainInput(geopoint.format(format), getSingleInputHint(currentFormat));
            return;
        }

        if (currentFormat.equals(Settings.CoordInputFormatEnum.SwissGrid)) {
            final SwissGridPoint swissGridPoint = SwissGridPoint.latLong2SwissGrid(geopoint);
            showDualPlainInput(
                    Long.toString(Math.round(swissGridPoint.getLv95Easting())),
                    Long.toString(Math.round(swissGridPoint.getLv95Northing())),
                    R.string.coord_input_easting,
                    R.string.coord_input_northing
            );
            return;
        }

        if (currentFormat.equals(Settings.CoordInputFormatEnum.RD)) {
            final RDPoint rdPoint = RDPoint.latLong2RD(geopoint);
            showDualPlainInput(
                    Long.toString(Math.round(rdPoint.getX())),
                    Long.toString(Math.round(rdPoint.getY())),
                    R.string.coord_input_x,
                    R.string.coord_input_y
            );
            return;
        }

        showStructuredInput();

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

    private void showDualPlainInput(final String latitudeText, final String longitudeText,
                                    @StringRes final int latitudeHint, @StringRes final int longitudeHint) {
        eLatFrame.setVisibility(View.VISIBLE);
        eLonFrame.setVisibility(View.VISIBLE);
        eThirdFrame.setVisibility(View.GONE);
        plainLatitude.setVisibility(View.VISIBLE);
        plainLongitude.setVisibility(View.VISIBLE);
        plainThird.setVisibility(View.GONE);
        setFrameHint(eLatFrame, plainLatitude, latitudeHint);
        setFrameHint(eLonFrame, plainLongitude, longitudeHint);
        plainLatitude.setText(latitudeText);
        plainLongitude.setText(longitudeText);
        configurableLatitude.setVisibility(View.GONE);
        configurableLongitude.setVisibility(View.GONE);
    }

    private void showSinglePlainInput(final String coordinateText, @StringRes final int coordinateHint) {
        eLatFrame.setVisibility(View.VISIBLE);
        eLonFrame.setVisibility(View.GONE);
        eThirdFrame.setVisibility(View.GONE);
        plainLatitude.setVisibility(View.VISIBLE);
        plainLongitude.setVisibility(View.GONE);
        plainThird.setVisibility(View.GONE);
        setFrameHint(eLatFrame, plainLatitude, coordinateHint);
        plainLatitude.setText(coordinateText);
        configurableLatitude.setVisibility(View.GONE);
        configurableLongitude.setVisibility(View.GONE);
    }

    private void showTriplePlainInput(final String firstText, final String secondText, final String thirdText,
                                      @StringRes final int firstHint, @StringRes final int secondHint,
                                      @StringRes final int thirdHint) {
        eLatFrame.setVisibility(View.VISIBLE);
        eLonFrame.setVisibility(View.VISIBLE);
        eThirdFrame.setVisibility(View.VISIBLE);
        plainLatitude.setVisibility(View.VISIBLE);
        plainLongitude.setVisibility(View.VISIBLE);
        plainThird.setVisibility(View.VISIBLE);
        setFrameHint(eLatFrame, plainLatitude, firstHint);
        setFrameHint(eLonFrame, plainLongitude, secondHint);
        setFrameHint(eThirdFrame, plainThird, thirdHint);
        plainLatitude.setText(firstText);
        plainLongitude.setText(secondText);
        plainThird.setText(thirdText);
        configurableLatitude.setVisibility(View.GONE);
        configurableLongitude.setVisibility(View.GONE);
    }

    private void showStructuredInput() {
        plainLatitude.setVisibility(View.GONE);
        plainLongitude.setVisibility(View.GONE);
        plainThird.setVisibility(View.GONE);
        eLatFrame.setVisibility(View.GONE);
        eLonFrame.setVisibility(View.GONE);
        eThirdFrame.setVisibility(View.GONE);
        configurableLatitude.setVisibility(View.VISIBLE);
        configurableLongitude.setVisibility(View.VISIBLE);
    }

    private static void setFrameHint(final TextInputLayout frame, final EditText field, @StringRes final int hintRes) {
        final String hintText = LocalizationUtils.getString(hintRes);
        frame.setHint(hintText);
        field.setHint(hintText);
    }

    private static boolean isSingleInputFormat(final Settings.CoordInputFormatEnum format) {
        switch (format) {
            case MGRS:
            case OLC:
                return true;
            default:
                return false;
        }
    }

    private static GeopointFormatter.Format getSingleInputGeopointFormat(final Settings.CoordInputFormatEnum format) {
        switch (format) {
            case MGRS:
                return GeopointFormatter.Format.MGRS;
            case OLC:
                return GeopointFormatter.Format.OLC;
            default:
                return GeopointFormatter.Format.LAT_LON_DECMINUTE;
        }
    }

    private static int getSingleInputHint(final Settings.CoordInputFormatEnum format) {
        switch (format) {
            case MGRS:
                return R.string.coord_input_mgrs;
            case OLC:
                return R.string.coord_input_olc;
            default:
                return R.string.latitude;
        }
    }

    private CalculatedCoordinateType getCalculatedCoordinateTypeForCurrentFormat() {
        switch (currentFormat) {
            case Deg:
                return CalculatedCoordinateType.DEGREE;
            case Min:
                return CalculatedCoordinateType.DEGREE_MINUTE;
            case Sec:
                return CalculatedCoordinateType.DEGREE_MINUTE_SEC;
            default:
                return CalculatedCoordinateType.PLAIN;
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
        final String lat;
        final String lon;
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
            case UTM:
            case MGRS:
            case OLC:
            case SwissGrid:
            case RD:
                lat = readGui();
                lon = "";
                break;
            case Plain:
            default:
                lat = plainLatitude.getText().toString();
                lon = plainLongitude.getText().toString();
                break;
        }
        return new Pair<>(lat, lon);
    }

    private static boolean hasClipboardCoordinates() {
        try {
            new Geopoint(StringUtils.defaultString(ClipboardUtils.getText()));
        } catch (final Geopoint.ParseException ignored) {
            return false;
        }
        return true;
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
            if (currentFormat == Settings.CoordInputFormatEnum.Plain || isSingleInputFormat(currentFormat)) {
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

            refreshQuickMapTargetButton();
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

    private class QuickMapPreviewWatcher implements TextWatcher {

        @Override
        public void afterTextChanged(final Editable s) {
            refreshQuickMapTargetButton();
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

    private enum CoordinateDialogDisplayModeEnum {
        Normal,
        Waypoint,
        Simple
    }

    public interface CoordinateUpdate {
        void updateCoordinates(Geopoint gp);

        default void updateCoordinates(CoordinateInputData coordinateInputData) {
            updateCoordinates(coordinateInputData.getGeopoint());
        }
    }
}

