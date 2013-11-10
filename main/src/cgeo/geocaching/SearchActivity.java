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
    @InjectView(R.id.search_coordinates) protected Button buttonSearchCoords;

    @InjectView(R.id.address) protected EditText addressEditText;
    @InjectView(R.id.search_address) protected Button buttonSearchAddress;

    @InjectView(R.id.geocode) protected AutoCompleteTextView geocodeEditText;
    @InjectView(R.id.display_geocode) protected Button buttonSearchGeocode;

    @InjectView(R.id.keyword) protected EditText keywordEditText;
    @InjectView(R.id.search_keyword) protected Button buttonSearchKeyword;

    @InjectView(R.id.username) protected EditText userNameEditText;
    @InjectView(R.id.search_username) protected Button buttonSearchUserName;

    @InjectView(R.id.owner) protected EditText ownerNameEditText;
    @InjectView(R.id.search_owner) protected Button buttonSearchOwner;

    @InjectView(R.id.trackable) protected AutoCompleteTextView trackableEditText;
    @InjectView(R.id.display_trackable) protected Button buttonSearchTrackable;

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
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
    public final void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        init();
    }

    @Override
    public final void onResume() {
        super.onResume();
        init();
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
        TrackableConnector trackableConnector = ConnectorFactory.getTrackableConnector(geocode);

        // check if the query contains a TB code
        if (trackableConnector == ConnectorFactory.UNKNOWN_TRACKABLE_CONNECTOR) {
            final String tbCode = ConnectorFactory.getTrackableFromURL(query);
            if (StringUtils.isNotBlank(tbCode)) {
                trackableConnector = ConnectorFactory.getTrackableConnector(tbCode);
                geocode = tbCode;
            }
        }

        if (trackableConnector != ConnectorFactory.UNKNOWN_TRACKABLE_CONNECTOR) {
            final Intent trackablesIntent = new Intent(this, TrackableActivity.class);
            trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, geocode.toUpperCase(Locale.US));
            startActivity(trackablesIntent);
            return true;
        }

        if (keywordSearch) { // keyword fallback, if desired by caller
            CacheListActivity.startActivityKeyword(this, query.trim());
            return true;
        }

        return false;
    }

    private void init() {
        buttonLatitude.setOnClickListener(new FindByCoordsAction());
        buttonLongitude.setOnClickListener(new FindByCoordsAction());

        buttonSearchCoords.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View arg0) {
                findByCoordsFn();
            }
        });

        setSearchAction(addressEditText, buttonSearchAddress, new Runnable() {

            @Override
            public void run() {
                findByAddressFn();
            }
        });

        setSearchAction(geocodeEditText, buttonSearchGeocode, new Runnable() {

            @Override
            public void run() {
                findByGeocodeFn();
            }
        });
        addHistoryEntries(geocodeEditText, DataStore.getRecentGeocodesForSearch());

        setSearchAction(keywordEditText, buttonSearchKeyword, new Runnable() {

            @Override
            public void run() {
                findByKeywordFn();
            }
        });

        setSearchAction(userNameEditText, buttonSearchUserName, new Runnable() {

            @Override
            public void run() {
                findByUsernameFn();
            }
        });

        setSearchAction(ownerNameEditText, buttonSearchOwner, new Runnable() {

            @Override
            public void run() {
                findByOwnerFn();
            }
        });

        setSearchAction(trackableEditText, buttonSearchTrackable, new Runnable() {

            @Override
            public void run() {
                findTrackableFn();
            }
        });
        addHistoryEntries(trackableEditText, DataStore.getTrackableCodes());
        disableSuggestions(trackableEditText);
    }

    private static void setSearchAction(final EditText editText, final Button button, final Runnable runnable) {
        EditUtils.setActionListener(editText, runnable);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View arg0) {
                runnable.run();
            }
        });
    }

    private void addHistoryEntries(final AutoCompleteTextView textView, final String[] entries) {
        if (entries != null) {
            final ArrayAdapter<String> historyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, entries);
            textView.setAdapter(historyAdapter);
        }
    }

    private class FindByCoordsAction implements OnClickListener {

        @Override
        public void onClick(final View arg0) {
            final CoordinatesInputDialog coordsDialog = new CoordinatesInputDialog(SearchActivity.this, null, null, app.currentGeo());
            coordsDialog.setCancelable(true);
            coordsDialog.setOnCoordinateUpdate(new CoordinatesInputDialog.CoordinateUpdate() {
                @Override
                public void update(final Geopoint gp) {
                    buttonLatitude.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    buttonLongitude.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                }
            });
            coordsDialog.show();
        }
    }

    private void findByCoordsFn() {
        final String latText = StringUtils.trim(buttonLatitude.getText().toString());
        final String lonText = StringUtils.trim(buttonLongitude.getText().toString());

        if (StringUtils.isEmpty(latText) || StringUtils.isEmpty(lonText)) {
            final IGeoData geo = app.currentGeo();
            if (geo.getCoords() != null) {
                buttonLatitude.setText(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE));
                buttonLongitude.setText(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE));
            }
        } else {
            try {
                CacheListActivity.startActivityCoordinates(this, new Geopoint(latText, lonText));
            } catch (final Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
            }
        }
    }

    private void findByKeywordFn() {
        // find caches by coordinates
        final String keyText = StringUtils.trim(keywordEditText.getText().toString());

        if (StringUtils.isBlank(keyText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_keyword));
            return;
        }

        CacheListActivity.startActivityKeyword(this, keyText);
    }

    private void findByAddressFn() {
        final String addText = StringUtils.trim(addressEditText.getText().toString());

        if (StringUtils.isBlank(addText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_address));
            return;
        }

        final Intent addressesIntent = new Intent(this, AddressListActivity.class);
        addressesIntent.putExtra(Intents.EXTRA_KEYWORD, addText);
        startActivity(addressesIntent);
    }

    public final void findByUsernameFn() {
        final String usernameText = StringUtils.trim(userNameEditText.getText().toString());

        if (StringUtils.isBlank(usernameText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_user));
            return;
        }

        CacheListActivity.startActivityUserName(this, usernameText);
    }

    private void findByOwnerFn() {
        findByOwnerFn(ownerNameEditText.getText().toString());
    }

    private void findByOwnerFn(final String userName) {
        final String usernameText = StringUtils.trimToEmpty(userName);

        if (StringUtils.isBlank(usernameText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_user));
            return;
        }

        CacheListActivity.startActivityOwner(this, usernameText);
    }

    private void findByGeocodeFn() {
        final String geocodeText = StringUtils.trim(geocodeEditText.getText().toString());

        if (StringUtils.isBlank(geocodeText) || geocodeText.equalsIgnoreCase("GC")) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_gccode));
            return;
        }

        CacheDetailActivity.startActivity(this, geocodeText);
    }

    private void findTrackableFn() {
        final String trackableText = StringUtils.trim(trackableEditText.getText().toString());

        if (StringUtils.isBlank(trackableText) || trackableText.equalsIgnoreCase("TB")) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_tb));
            return;
        }

        final Intent trackablesIntent = new Intent(this, TrackableActivity.class);
        trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, trackableText.toUpperCase(Locale.US));
        startActivity(trackablesIntent);
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity_options, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
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
