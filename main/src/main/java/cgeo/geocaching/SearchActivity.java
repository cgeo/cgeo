package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractBottomNavigationActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.address.AddressListActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableTrackingCode;
import cgeo.geocaching.databinding.SearchActivityBinding;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.search.AutoCompleteAdapter;
import cgeo.geocaching.search.GeocacheAutoCompleteAdapter;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import org.apache.commons.lang3.StringUtils;

public class SearchActivity extends AbstractBottomNavigationActivity implements CoordinatesInputDialog.CoordinateUpdate {
    private SearchActivityBinding binding;

    private static final String GOOGLE_NOW_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION";
    public static final String ACTION_CLIPBOARD = "clipboard";

    @Override
    @SuppressWarnings("PMD.NPathComplexity") // split up would not help readability
    public final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        if (Intents.ACTION_GEOCACHE.equals(intent.getAction())) {
            // search suggestion for a cache
            CacheDetailActivity.startActivity(this, intent.getStringExtra(SearchManager.QUERY));
            finish();
            return;
        } else if (Intents.ACTION_TRACKABLE.equals(intent.getAction())) {
            // search suggestion for a trackable
            TrackableActivity.startActivity(this, null, intent.getStringExtra(SearchManager.QUERY), null, null, TrackableBrand.UNKNOWN.getId());
            finish();
            return;
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction()) || GOOGLE_NOW_SEARCH_ACTION.equals(intent.getAction()) || ACTION_CLIPBOARD.equals(intent.getAction())) {
            // search query, from search toolbar or from google now
            hideKeyboard();
            final String query = intent.getStringExtra(SearchManager.QUERY);
            final boolean isClipboardSearch = ACTION_CLIPBOARD.equals(intent.getAction());
            final boolean keywordSearch = (!isClipboardSearch && intent.getBooleanExtra(Intents.EXTRA_KEYWORD_SEARCH, true));

            if (instantSearch(query, keywordSearch, isClipboardSearch)) {
                setResult(RESULT_OK);
            } else {
                // send intent back so query string is known.
                // Strip away potential security-relevant things (see #12409)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.removeFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.removeFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } else {
                    intent.setFlags(intent.getFlags() & ~Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(intent.getFlags() & ~Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                setResult(RESULT_CANCELED, intent);
            }
            finish();

            return;
        }

        // called from external app via
        // * intent action view with geo: uri
        // * click on http(s)-Link to internet map service (google-maps, openstreetmap, yandex or here)
        final GeoUri parser = new GeoUri(GeoUri.OPT_DEFAULT);
        final IGeoPointInfo geo = parser.fromUri(getIntent().getDataString());
        if (geo != null) {
            String name = geo.getName();
            if (name != null && name.trim().length() == 0) {
                name = null;
            }
            Log.i("Received a geo intent: lat=" + geo.getLatitude() + ", lon=" + geo.getLongitude() + ", name=" + name
                    + " form " + getIntent().getDataString());
            if (!GeoPointDto.isEmpty(geo)) {
                // non-fuzzy-geo that already has lat/lon => search via lat/lon
                CacheListActivity.startActivityCoordinates(this, new Geopoint(geo.getLatitude(), geo.getLongitude()), name);
            } else if (name != null) {
                // fuzzy geo with seach-query and without lat/lon => search via address
                findByAddressFn(name);
            }
            finish();
        }

        setTheme();
        binding = SearchActivityBinding.inflate(getLayoutInflater());

        // adding the bottom navigation component is handled by {@link AbstractBottomNavigationActivity#setContentView}
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

    @Override
    public int getSelectedBottomItemId() {
        return MENU_SEARCH;
    }

    /**
     * Performs a search for query either as geocode, trackable code or keyword
     *
     * @param nonTrimmedQuery   String to search for
     * @param keywordSearch     Set to true if keyword search should be performed if query isn't GC or TB
     * @param isClipboardSearch Set to true if search should try to extract a geocode from the search string
     * @return true if a search was performed, else false
     */
    private boolean instantSearch(final String nonTrimmedQuery, final boolean keywordSearch, final boolean isClipboardSearch) {
        final String query = StringUtils.trim(nonTrimmedQuery);

        // try to interpret query as geocode
        if (instantSearchGeocache(query, isClipboardSearch)) {
            return true;
        }

        // try to interpret query as geocode
        if (instantSearchTrackable(query, keywordSearch)) {
            return true;
        }

        // keyword fallback, if desired by caller
        if (keywordSearch) {
            CacheListActivity.startActivityKeyword(this, query);
            ActivityMixin.overrideTransitionToFade(this);
            return true;
        }

        return false;
    }

    /**
     * try to interpret query as a geocode
     *
     * @param query             Trimmed string to search for
     * @param isClipboardSearch Set to true if search should try to extract a geocode from the search string
     * @return true if geocache was found
     */
    private boolean instantSearchGeocache(final String query, final Boolean isClipboardSearch) {
        IConnector connector = null;

        // first check if this was a scanned URL
        String geocode = ConnectorFactory.getGeocodeFromURL(query);
        if (!StringUtils.isEmpty(geocode)) {
            connector = ConnectorFactory.getConnector(geocode);
        }

        // otherwise see if this is a pure geocode
        if (!(connector instanceof ISearchByGeocode)) {
            geocode = StringUtils.upperCase(query);
            connector = ConnectorFactory.getConnector(geocode);
        }

        // otherwise try to extract a geocode from text
        if (!(connector instanceof ISearchByGeocode) && isClipboardSearch) {
            geocode = ConnectorFactory.getGeocodeFromText(query);
            connector = ConnectorFactory.getConnector(geocode);
        }

        // Finally if a geocache was found load it
        if (connector instanceof ISearchByGeocode && geocode != null) {
            CacheDetailActivity.startActivity(this, geocode.toUpperCase(Locale.US));
            return true;
        }

        return false;
    }

    /**
     * try to interpret query as trackable
     *
     * @param query         Trimmed string to search for
     * @param keywordSearch Set to true if keyword search should be performed if query isn't GC or TB
     * @return true if trackable was found
     */
    private boolean instantSearchTrackable(final String query, final Boolean keywordSearch) {
        String trackableCode = "";

        // Check if the query is a TB code
        TrackableBrand trackableBrand = ConnectorFactory.getTrackableConnector(query).getBrand();
        if (trackableBrand != TrackableBrand.UNKNOWN) {
            trackableCode = query;
        }

        // otherwise check if the query contains a TB code
        if (trackableBrand == TrackableBrand.UNKNOWN) {
            final String tbCode = ConnectorFactory.getTrackableFromURL(query);
            if (StringUtils.isNotBlank(tbCode)) {
                trackableBrand = ConnectorFactory.getTrackableConnector(tbCode).getBrand();
                trackableCode = tbCode;
            }
        }

        // check if the query contains a TB tracking code
        if (trackableBrand == TrackableBrand.UNKNOWN) {
            final TrackableTrackingCode tbTrackingCode = ConnectorFactory.getTrackableTrackingCodeFromURL(query);
            if (!tbTrackingCode.isEmpty()) {
                trackableBrand = tbTrackingCode.brand;
                trackableCode = tbTrackingCode.trackingCode;
            }
        }

        // Finally if a trackable was found load it
        if (trackableBrand != TrackableBrand.UNKNOWN && !StringUtils.isEmpty(trackableCode)) {
            final Intent trackablesIntent = new Intent(this, TrackableActivity.class);
            trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, trackableCode.toUpperCase(Locale.US));
            trackablesIntent.putExtra(Intents.EXTRA_BRAND, trackableBrand.getId());
            if (keywordSearch) { // keyword fallback, if desired by caller
                trackablesIntent.putExtra(Intents.EXTRA_KEYWORD, query);
            }
            startActivity(trackablesIntent);
            return true;
        }

        return false;
    }

    private void init() {
        binding.buttonLatLongitude.setOnClickListener(v -> updateCoordinates());

        binding.searchCoordinates.setOnClickListener(arg0 -> findByCoordsFn());

        // standard searches
        setBasicSearchAction(binding.address, binding.searchAddress, this::findByAddressFn, null);
        setBasicSearchAction(binding.owner, binding.searchOwner, this::findByOwnerFn, DataStore::getSuggestionsOwnerName);
        setBasicSearchAction(binding.finder, binding.searchFinder, this::findByFinderFn, DataStore::getSuggestionsFinderName);
        setBasicSearchAction(null, binding.searchFilter, this::findByFilterFn, null);
        setBasicSearchAction(binding.trackable, binding.displayTrackable, this::findTrackableFn, DataStore::getSuggestionsTrackableCode);

        // geocache searches
        setGeocacheSearchAction(binding.geocode, binding.displayGeocode, () -> findByGeocodeFn(binding.geocode.getText().toString()), DataStore::getSuggestionsGeocode);
        setGeocacheSearchAction(binding.keyword, binding.searchKeyword, this::findByKeywordFn, DataStore::getSuggestionsKeyword);

        binding.geocode.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
        binding.trackable.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        binding.searchFilterInfo.setOnClickListener(v -> SimpleDialog.of(this).setMessage(TextParam.id(R.string.search_filter_info_message).setMarkdown(true)).show());

        handlePotentialClipboardGeocode();
    }

    /**
     * Detect geocodes in clipboard
     *
     * Needs to run async as clipboard access is blocked if activity is not yet created.
     */
    private void handlePotentialClipboardGeocode() {
        binding.geocodeInputLayout.postDelayed(() -> {
            final String clipboardText = ClipboardUtils.getText();

            String geocode = "";

            if (ConnectorFactory.getConnector(clipboardText) instanceof ISearchByGeocode) {
                geocode = clipboardText;
            } else {
                geocode = ConnectorFactory.getGeocodeFromText(clipboardText);
            }
            if (!StringUtils.isEmpty(geocode)) {
                binding.geocode.setText(geocode);
                binding.geocodeInputLayout.setHelperText(getString(R.string.search_geocode_from_clipboard));

                // clear hint if text input get changed
                binding.geocode.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                        // nothing
                    }

                    @Override
                    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                        // nothing
                    }

                    @Override
                    public void afterTextChanged(final Editable s) {
                        binding.geocodeInputLayout.setHelperText(null);
                        binding.geocode.removeTextChangedListener(this);
                    }
                });
            }
        }, 500);
    }

    private void setBasicSearchAction(final AutoCompleteTextView editText, final Button button, @NonNull final Runnable runnable, @Nullable final Func1<String, String[]> suggestionFunction) {
        if (editText != null) {
            EditUtils.setActionListener(editText, runnable);
            if (suggestionFunction != null) {
                editText.setAdapter(new AutoCompleteAdapter(editText.getContext(), android.R.layout.simple_dropdown_item_1line, suggestionFunction));
            }
        }
        button.setOnClickListener(arg0 -> runnable.run());
    }

    private void setGeocacheSearchAction(@NonNull final AutoCompleteTextView editText, @NonNull final Button button, @NonNull final Runnable runnable, @NonNull final Func1<String, String[]> suggestionFunction) {
        EditUtils.setActionListener(editText, runnable);
        editText.setAdapter(new GeocacheAutoCompleteAdapter(editText.getContext(), suggestionFunction));
        editText.setOnItemClickListener((parent, view, position, id) -> {
            final String geocode = (String) parent.getItemAtPosition(position);
            // as we directly start the cache details activity on item selection,
            // reset the edit text to not confuse the user and to provide a better workflow.
            // use the current cache's prefix as new prefix
            editText.setText(geocode.substring(0, 2));
            findByGeocodeFn(geocode);
        });
        button.setOnClickListener(arg0 -> runnable.run());
    }

    private void updateCoordinates() {
        CoordinatesInputDialog.show(getSupportFragmentManager(), null, null);
    }

    @Override
    public void updateCoordinates(@NonNull final Geopoint gp) {
        binding.buttonLatLongitude.setText(String.format("%s%n%s", gp.format(GeopointFormatter.Format.LAT_DECMINUTE), gp.format(GeopointFormatter.Format.LON_DECMINUTE)));
    }

    @Override
    public boolean supportsNullCoordinates() {
        return false;
    }

    private void findByCoordsFn() {
        String[] latlonText = getCoordText();

        if (latlonText.length < 2) {
            final Geopoint gp = LocationDataProvider.getInstance().currentGeo().getCoords();
            if (gp.isValid()) {
                updateCoordinates(gp);
                latlonText = getCoordText();
            } else {
                return;
            }
        }

        try {
            CacheListActivity.startActivityCoordinates(this, new Geopoint(StringUtils.trim(latlonText[0]), StringUtils.trim(latlonText[1])), null);
            ActivityMixin.overrideTransitionToFade(this);
        } catch (final Geopoint.ParseException e) {
            showToast(res.getString(e.resource));
        }
    }

    private String[] getCoordText() {
        return binding.buttonLatLongitude.getText().toString().split("\n");
    }

    private void findByKeywordFn() {
        // find caches by keyword
        final String keyText = StringUtils.trim(binding.keyword.getText().toString());

        if (StringUtils.isBlank(keyText)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_keyword).show();
            return;
        }

        CacheListActivity.startActivityKeyword(this, keyText);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByAddressFn() {
        final String addressSearchText = StringUtils.trim(binding.address.getText().toString());

        if (StringUtils.isBlank(addressSearchText)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_address).show();
            return;
        }

        findByAddressFn(addressSearchText);
    }

    private void findByAddressFn(final String addressSearchText) {
        final Intent addressesIntent = new Intent(this, AddressListActivity.class);
        addressesIntent.putExtra(Intents.EXTRA_KEYWORD, addressSearchText);
        startActivity(addressesIntent);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByOwnerFn() {
        findByOwnerFn(binding.owner.getText().toString());
    }

    private void findByOwnerFn(final String userName) {
        final String usernameText = StringUtils.trimToEmpty(userName);

        if (StringUtils.isBlank(usernameText)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_user).show();
            return;
        }

        CacheListActivity.startActivityOwner(this, usernameText);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByFinderFn() {
        findByFinderFn(binding.finder.getText().toString());
    }

    private void findByFinderFn(final String userName) {
        final String usernameText = StringUtils.trimToEmpty(userName);

        if (StringUtils.isBlank(usernameText)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_user).show();
            return;
        }

        CacheListActivity.startActivityFinder(this, usernameText);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByFilterFn() {
        GeocacheFilterActivity.selectFilter(this, new GeocacheFilterContext(GeocacheFilterContext.FilterType.LIVE), null, false);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {

        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            CacheListActivity.startActivityFilter(this);
            ActivityMixin.overrideTransitionToFade(this);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void findByGeocodeFn(final String geocode) {
        final String geocodeText = StringUtils.trimToEmpty(geocode);

        if (StringUtils.isBlank(geocodeText) || geocodeText.equalsIgnoreCase("GC")) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_gccode).show();
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
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_tb).show();
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
