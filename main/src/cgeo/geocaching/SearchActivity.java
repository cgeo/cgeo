package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.address.AddressListActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableTrackingCode;
import cgeo.geocaching.databinding.SearchActivityBinding;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.search.AutoCompleteAdapter;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.EditUtils;
import cgeo.geocaching.utils.functions.Func1;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

public class SearchActivity extends AbstractActionBarActivity implements CoordinatesInputDialog.CoordinateUpdate {
    private SearchActivityBinding binding;

    private static final String GOOGLE_NOW_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION";

    @Override
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        // search suggestion for a cache
        if (Intents.ACTION_GEOCACHE.equals(intent.getAction())) {
            CacheDetailActivity.startActivity(this, intent.getStringExtra(SearchManager.QUERY));
            finish();
            return;
        }

        // search suggestion for a trackable
        if (Intents.ACTION_TRACKABLE.equals(intent.getAction())) {
            TrackableActivity.startActivity(this, null, intent.getStringExtra(SearchManager.QUERY), null, null, TrackableBrand.UNKNOWN.getId());
            finish();
            return;
        }

        // search query, from search toolbar or from google now
        if (Intent.ACTION_SEARCH.equals(intent.getAction()) || GOOGLE_NOW_SEARCH_ACTION.equals(intent.getAction())) {
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
        binding = SearchActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // set title in code, as the activity needs a hard coded title due to the intent filters
        setTitle(res.getString(R.string.search));

        init();
    }

    @Override
    public final void onConfigurationChanged(@NonNull final Configuration newConfig) {
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
     * @param nonTrimmedQuery
     *            String to search for
     * @param keywordSearch
     *            Set to true if keyword search should be performed if query isn't GC or TB
     * @return true if a search was performed, else false
     */
    private boolean instantSearch(final String nonTrimmedQuery, final boolean keywordSearch) {
        final String query = StringUtils.trim(nonTrimmedQuery);

        // first check if this was a scanned URL
        String geocode = ConnectorFactory.getGeocodeFromURL(query);

        // otherwise see if this is a pure geocode
        if (StringUtils.isEmpty(geocode)) {
            geocode = StringUtils.upperCase(StringUtils.trim(query));
        }

        final IConnector connector = ConnectorFactory.getConnector(geocode);
        if (connector instanceof ISearchByGeocode && geocode != null) {
            CacheDetailActivity.startActivity(this, geocode.toUpperCase(Locale.US));
            return true;
        }

        // Check if the query is a TB code
        TrackableBrand trackableBrand = ConnectorFactory.getTrackableConnector(geocode).getBrand();

        // check if the query contains a TB code
        if (trackableBrand == TrackableBrand.UNKNOWN) {
            final String tbCode = ConnectorFactory.getTrackableFromURL(query);
            if (StringUtils.isNotBlank(tbCode)) {
                trackableBrand = ConnectorFactory.getTrackableConnector(tbCode).getBrand();
                geocode = tbCode;
            }
        }

        // check if the query contains a TB tracking code
        if (trackableBrand == TrackableBrand.UNKNOWN) {
            final TrackableTrackingCode tbTrackingCode = ConnectorFactory.getTrackableTrackingCodeFromURL(query);
            if (!tbTrackingCode.isEmpty()) {
                trackableBrand = tbTrackingCode.brand;
                geocode = tbTrackingCode.trackingCode;
            }
        }

        if (trackableBrand != TrackableBrand.UNKNOWN && geocode != null) {
            final Intent trackablesIntent = new Intent(this, TrackableActivity.class);
            trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, geocode.toUpperCase(Locale.US));
            trackablesIntent.putExtra(Intents.EXTRA_BRAND, trackableBrand.getId());
            if (keywordSearch) { // keyword fallback, if desired by caller
                trackablesIntent.putExtra(Intents.EXTRA_KEYWORD, query.trim());
            }
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
        binding.buttonLatitude.setOnClickListener(v -> updateCoordinates());
        binding.buttonLongitude.setOnClickListener(v -> updateCoordinates());

        binding.searchCoordinates.setOnClickListener(arg0 -> findByCoordsFn());

        setSearchAction(binding.address, binding.searchAddress, this::findByAddressFn, null);
        setSearchAction(binding.geocode, binding.displayGeocode, this::findByGeocodeFn, DataStore::getSuggestionsGeocode);
        setSearchAction(binding.keyword, binding.searchKeyword, this::findByKeywordFn, DataStore::getSuggestionsKeyword);
        setSearchAction(binding.finder, binding.searchFinder, this::findByFinderFn, DataStore::getSuggestionsFinderName);
        setSearchAction(binding.owner, binding.searchOwner, this::findByOwnerFn, DataStore::getSuggestionsOwnerName);
        setSearchAction(null, binding.searchFilter, this::findByFilterFn, null);
        setSearchAction(binding.trackable, binding.displayTrackable, this::findTrackableFn, DataStore::getSuggestionsTrackableCode);

        binding.geocode.setFilters(new InputFilter[] { new InputFilter.AllCaps() });
        binding.trackable.setFilters(new InputFilter[] { new InputFilter.AllCaps() });

        binding.searchFilterInfo.setOnClickListener(v -> Dialogs.messageMarkdown(this, null, this.getString(R.string.search_filter_info_message)));
    }

    private static void setSearchAction(final AutoCompleteTextView editText, final Button button, @NonNull final Runnable runnable, @Nullable final Func1<String, String[]> suggestionFunction) {
        if (editText != null) {
            EditUtils.setActionListener(editText, runnable);
        }
        button.setOnClickListener(arg0 -> runnable.run());
        if (suggestionFunction != null) {
            editText.setAdapter(new AutoCompleteAdapter(editText.getContext(), android.R.layout.simple_dropdown_item_1line, suggestionFunction));
        }
    }

    private void updateCoordinates() {
        final CoordinatesInputDialog coordsDialog = CoordinatesInputDialog.getInstance(null, null);
        coordsDialog.setCancelable(true);
        coordsDialog.show(getSupportFragmentManager(), "wpedit_dialog");
    }

    @Override
    public void updateCoordinates(final Geopoint gp) {
        binding.buttonLatitude.setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
        binding.buttonLongitude.setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
    }

    @Override
    public boolean supportsNullCoordinates() {
        return false;
    }

    private void findByCoordsFn() {
        final String latText = StringUtils.trim(binding.buttonLatitude.getText().toString());
        final String lonText = StringUtils.trim(binding.buttonLongitude.getText().toString());

        if (StringUtils.isEmpty(latText) || StringUtils.isEmpty(lonText)) {
            final GeoData geo = Sensors.getInstance().currentGeo();
            binding.buttonLatitude.setText(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE));
            binding.buttonLongitude.setText(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE));
        } else {
            try {
                CacheListActivity.startActivityCoordinates(this, new Geopoint(latText, lonText), null);
            } catch (final Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
            }
        }
    }

    private void findByKeywordFn() {
        // find caches by coordinates
        final String keyText = StringUtils.trim(binding.keyword.getText().toString());

        if (StringUtils.isBlank(keyText)) {
            Dialogs.message(this, R.string.warn_search_help_title, R.string.warn_search_help_keyword);
            return;
        }

        CacheListActivity.startActivityKeyword(this, keyText);
    }

    private void findByAddressFn() {
        final String addText = StringUtils.trim(binding.address.getText().toString());

        if (StringUtils.isBlank(addText)) {
            Dialogs.message(this, R.string.warn_search_help_title, R.string.warn_search_help_address);
            return;
        }

        final Intent addressesIntent = new Intent(this, AddressListActivity.class);
        addressesIntent.putExtra(Intents.EXTRA_KEYWORD, addText);
        startActivity(addressesIntent);
    }

    private void findByFinderFn() {
        final String usernameText = StringUtils.trim(binding.finder.getText().toString());

        if (StringUtils.isBlank(usernameText)) {
            Dialogs.message(this, R.string.warn_search_help_title, R.string.warn_search_help_user);
            return;
        }

        CacheListActivity.startActivityFinder(this, usernameText);
    }

    private void findByOwnerFn() {
        findByOwnerFn(binding.owner.getText().toString());
    }

    private void findByOwnerFn(final String userName) {
        final String usernameText = StringUtils.trimToEmpty(userName);

        if (StringUtils.isBlank(usernameText)) {
            Dialogs.message(this, R.string.warn_search_help_title, R.string.warn_search_help_user);
            return;
        }

        CacheListActivity.startActivityOwner(this, usernameText);
    }

    private void findByFilterFn() {
        GeocacheFilterActivity.selectFilter(this, GeocacheFilter.loadFromSettings(), null, false);

    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {

        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            final GeocacheFilter selectedFilter = GeocacheFilter.createFromConfig(data.getStringExtra(GeocacheFilterActivity.EXTRA_FILTER_RESULT));
            CacheListActivity.startActivityFilter(this, selectedFilter);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void findByGeocodeFn() {
        final String geocodeText = StringUtils.trimToEmpty(binding.geocode.getText().toString());

        if (StringUtils.isBlank(geocodeText) || geocodeText.equalsIgnoreCase("GC")) {
            Dialogs.message(this, R.string.warn_search_help_title, R.string.warn_search_help_gccode);
            return;
        }

        if (ConnectorFactory.anyConnectorActive()) {
            CacheDetailActivity.startActivity(this, geocodeText.toUpperCase(Locale.US));
        } else {
            showToast(getString(R.string.warn_no_connector));
        }
    }

    private void findTrackableFn() {
        final String trackableText = StringUtils.trimToEmpty(binding.trackable.getText().toString());

        if (StringUtils.isBlank(trackableText) || trackableText.equalsIgnoreCase("TB")) {
            Dialogs.message(this, R.string.warn_search_help_title, R.string.warn_search_help_tb);
            return;
        }

        // check temporaribly disabled due to #7617
        // if (ConnectorFactory.anyTrackableConnectorActive()) {
            final Intent trackablesIntent = new Intent(this, TrackableActivity.class);
            trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, trackableText.toUpperCase(Locale.US));
            startActivity(trackablesIntent);
        /*
        } else {
            showToast(getString(R.string.warn_no_connector));
        }
        */
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity_options, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.menu_search_own_caches) {
            findByOwnerFn(Settings.getUserName());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void startActivityScan(final String scan, final Activity fromActivity) {
        final Intent searchIntent = new Intent(fromActivity, SearchActivity.class);
        searchIntent.setAction(Intent.ACTION_SEARCH).
                putExtra(SearchManager.QUERY, scan).
                putExtra(Intents.EXTRA_KEYWORD_SEARCH, false);
        fromActivity.startActivityForResult(searchIntent, Intents.SEARCH_REQUEST_CODE);
    }

}
