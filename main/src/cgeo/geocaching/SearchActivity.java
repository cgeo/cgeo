package cgeo.geocaching;

import butterknife.InjectView;
import butterknife.Views;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.trackable.TrackableConnector;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.utils.EditUtils;
import cgeo.geocaching.utils.GeoDirHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import java.util.Locale;

public class SearchActivity extends AbstractActivity {

    @InjectView(R.id.buttonLatitude) protected Button buttonLatitude;
    @InjectView(R.id.buttonLongitude) protected Button buttonLongitude;
    @InjectView(R.id.search_coordinates) protected Button findByCoords;
    @InjectView(R.id.search_address) protected Button findByAddress;
    @InjectView(R.id.geocode) protected AutoCompleteTextView geocodeEdit;
    @InjectView(R.id.display_geocode) protected Button displayByGeocode;
    @InjectView(R.id.search_keyword) protected Button findByKeyword;
    @InjectView(R.id.search_username) protected Button findByUserName;
    @InjectView(R.id.search_owner) protected Button findByOwner;
    @InjectView(R.id.trackable) protected AutoCompleteTextView trackable;
    @InjectView(R.id.display_trackable) protected Button displayTrackable;
    @InjectView(R.id.latitude) protected EditText latEdit;
    @InjectView(R.id.longitude) protected EditText lonEdit;
    @InjectView(R.id.keyword) protected EditText keywordEditText;
    @InjectView(R.id.address) protected EditText addressEditText;
    @InjectView(R.id.username) protected EditText userNameEditText;
    @InjectView(R.id.owner) protected EditText ownerNameEditText;
    @InjectView(R.id.geocode) protected EditText geocodeEditText;
    @InjectView(R.id.trackable) protected EditText trackableEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // search query
        final Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            hideKeyboard();
            final String query = intent.getStringExtra(SearchManager.QUERY);
            final boolean keywordSearch = intent.getBooleanExtra(Intents.EXTRA_KEYWORD_SEARCH, true);

            if (instantSearch(query, keywordSearch)) {
                setResult(RESULT_OK);
            } else {
                // send intent back so query string is known
                setResult(RESULT_CANCELED, intent);
            }
            finish();

            return;
        }

        setTheme();
        setContentView(R.layout.search_activity);

        // set title in code, as the activity needs a hard coded title due to the intent filters
        setTitle(res.getString(R.string.search));

        Views.inject(this);
        init();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        geoDirHandler.startGeo();
        init();
    }

    @Override
    public void onPause() {
        geoDirHandler.stopGeo();
        super.onPause();
    }

    /**
     * Performs a search for query either as geocode, trackable code or keyword
     *
     * @param query
     *            String to search for
     * @param keywordSearch
     *            Set to true if keyword search should be performed if query isn't GC or TB
     * @return true if a search was performed, else false
     */
    private boolean instantSearch(final String query, final boolean keywordSearch) {
        // first check if this was a scanned URL
        String geocode = ConnectorFactory.getGeocodeFromURL(query);

        // otherwise see if this is a pure geocode
        if (StringUtils.isEmpty(geocode)) {
            geocode = StringUtils.upperCase(StringUtils.trim(query));
        }

        final IConnector connector = ConnectorFactory.getConnector(geocode);
        if (connector instanceof ISearchByGeocode) {
            CacheDetailActivity.startActivity(this, geocode.toUpperCase(Locale.US));
            return true;
        }

        // Check if the query is a TB code
        final TrackableConnector trackableConnector = ConnectorFactory.getTrackableConnector(geocode);
        if (trackableConnector != ConnectorFactory.UNKNOWN_TRACKABLE_CONNECTOR) {
            final Intent trackablesIntent = new Intent(this, TrackableActivity.class);
            trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, geocode.toUpperCase(Locale.US));
            startActivity(trackablesIntent);
            return true;
        }

        if (keywordSearch) { // keyword fallback, if desired by caller
            cgeocaches.startActivityKeyword(this, query.trim());
            return true;
        }

        return false;
    }

    private void init() {

        buttonLatitude.setOnClickListener(new FindByCoordsAction());
        buttonLongitude.setOnClickListener(new FindByCoordsAction());
        findByCoords.setOnClickListener(new FindByCoordsListener());

        EditUtils.setActionListener((EditText) findViewById(R.id.address), new Runnable() {

            @Override
            public void run() {
                findByAddressFn();
            }
        });
        findByAddress.setOnClickListener(new FindByAddressListener());

        EditUtils.setActionListener(geocodeEdit, new Runnable() {

            @Override
            public void run() {
                findByGeocodeFn();
            }
        });
        addHistoryEntries(geocodeEdit, DataStore.getRecentGeocodesForSearch());
        displayByGeocode.setOnClickListener(new FindByGeocodeListener());

        EditUtils.setActionListener((EditText) findViewById(R.id.keyword), new Runnable() {

            @Override
            public void run() {
                findByKeywordFn();
            }
        });
        findByKeyword.setOnClickListener(new FindByKeywordListener());

        EditUtils.setActionListener((EditText) findViewById(R.id.username), new Runnable() {

            @Override
            public void run() {
                findByUsernameFn();
            }
        });
        findByUserName.setOnClickListener(new FindByUsernameListener());

        EditUtils.setActionListener((EditText) findViewById(R.id.owner), new Runnable() {

            @Override
            public void run() {
                findByOwnerFn();
            }
        });
        findByOwner.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                findByOwnerFn();
            }
        });

        EditUtils.setActionListener(trackable, new Runnable() {

            @Override
            public void run() {
                findTrackableFn();
            }
        });
        addHistoryEntries(trackable, DataStore.getTrackableCodes());
        disableSuggestions(trackable);
        displayTrackable.setOnClickListener(new FindTrackableListener());
    }

    private void addHistoryEntries(final AutoCompleteTextView textView, final String[] entries) {
        if (entries != null) {
            final ArrayAdapter<String> historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, entries);
            textView.setAdapter(historyAdapter);
        }
    }

    private final GeoDirHandler geoDirHandler = new GeoDirHandler() {
        @Override
        public void updateGeoData(final IGeoData geo) {
            try {
                if (geo.getCoords() != null) {
                    if (latEdit != null) {
                        latEdit.setHint(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE_RAW));
                    }
                    if (lonEdit != null) {
                        lonEdit.setHint(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE_RAW));
                    }
                }
            } catch (final RuntimeException e) {
                Log.w("Failed to update location.");
            }
        }
    };

    private class FindByCoordsAction implements OnClickListener {

        @Override
        public void onClick(View arg0) {
            final CoordinatesInputDialog coordsDialog = new CoordinatesInputDialog(SearchActivity.this, null, null, app.currentGeo());
            coordsDialog.setCancelable(true);
            coordsDialog.setOnCoordinateUpdate(new CoordinatesInputDialog.CoordinateUpdate() {
                @Override
                public void update(Geopoint gp) {
                    buttonLatitude.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    buttonLongitude.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                }
            });
            coordsDialog.show();
        }
    }

    private class FindByCoordsListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findByCoordsFn();
        }
    }

    private void findByCoordsFn() {
        final String latText = buttonLatitude.getText().toString();
        final String lonText = buttonLongitude.getText().toString();

        if (StringUtils.isEmpty(latText) || StringUtils.isEmpty(lonText)) {
            final IGeoData geo = app.currentGeo();
            if (geo.getCoords() != null) {
                buttonLatitude.setText(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE));
                buttonLongitude.setText(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE));
            }
        } else {
            try {
                cgeocaches.startActivityCoordinates(this, new Geopoint(StringUtils.trim(latText), StringUtils.trim(lonText)));
            } catch (final Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
            }
        }
    }

    private class FindByKeywordListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findByKeywordFn();
        }
    }

    private void findByKeywordFn() {
        // find caches by coordinates
        final String keyText = keywordEditText.getText().toString();

        if (StringUtils.isBlank(keyText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_keyword));
            return;
        }

        cgeocaches.startActivityKeyword(this, StringUtils.trim(keyText));
    }

    private class FindByAddressListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            findByAddressFn();
        }
    }

    private void findByAddressFn() {
        final String addText = addressEditText.getText().toString();

        if (StringUtils.isBlank(addText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_address));
            return;
        }

        final Intent addressesIntent = new Intent(this, AddressListActivity.class);
        addressesIntent.putExtra(Intents.EXTRA_KEYWORD, StringUtils.trim(addText));
        startActivity(addressesIntent);
    }

    private class FindByUsernameListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findByUsernameFn();
        }
    }

    public void findByUsernameFn() {
        final String usernameText = userNameEditText.getText().toString();

        if (StringUtils.isBlank(usernameText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_user));
            return;
        }

        cgeocaches.startActivityUserName(this, StringUtils.trim(usernameText));
    }

    private void findByOwnerFn() {
        findByOwnerFn(ownerNameEditText.getText().toString());
    }

    private void findByOwnerFn(String userName) {
        final String usernameText = StringUtils.trimToEmpty(userName);

        if (StringUtils.isBlank(usernameText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_user));
            return;
        }

        cgeocaches.startActivityOwner(this, StringUtils.trim(usernameText));
    }

    private class FindByGeocodeListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findByGeocodeFn();
        }
    }

    private void findByGeocodeFn() {
        final String geocodeText = geocodeEditText.getText().toString();

        if (StringUtils.isBlank(geocodeText) || geocodeText.equalsIgnoreCase("GC")) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_gccode));
            return;
        }

        CacheDetailActivity.startActivity(this, StringUtils.trim(geocodeText));
    }

    private class FindTrackableListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findTrackableFn();
        }
    }

    private void findTrackableFn() {
        final String trackableText = trackableEditText.getText().toString();

        if (StringUtils.isBlank(trackableText) || trackableText.equalsIgnoreCase("TB")) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_tb));
            return;
        }

        final Intent trackablesIntent = new Intent(this, TrackableActivity.class);
        trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, StringUtils.trim(trackableText).toUpperCase(Locale.US));
        startActivity(trackablesIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_search_own_caches) {
            findByOwnerFn(Settings.getUsername());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void startActivityScan(final String scan, final Activity fromActivity) {
        final Intent searchIntent = new Intent(fromActivity, SearchActivity.class);
        searchIntent.setAction(Intent.ACTION_SEARCH).
                putExtra(SearchManager.QUERY, scan).
                putExtra(Intents.EXTRA_KEYWORD_SEARCH, false);
        fromActivity.startActivityForResult(searchIntent, MainActivity.SEARCH_REQUEST_CODE);
    }
}
