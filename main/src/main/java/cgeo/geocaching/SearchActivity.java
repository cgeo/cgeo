package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractNavigationBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.address.AddressListActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.al.ALConnector;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.connector.trackable.TrackableTrackingCode;
import cgeo.geocaching.databinding.SearchActivityBinding;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.search.GeocacheAutoCompleteAdapter;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.SearchCardView;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.CoordinatesInputDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.EditUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import org.apache.commons.lang3.StringUtils;

public class SearchActivity extends AbstractNavigationBarActivity implements CoordinatesInputDialog.CoordinateUpdate {
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
            if (name != null && name.trim().isEmpty()) {
                name = null;
            }
            Log.i("Received a geo intent: lat=" + geo.getLatitude() + ", lon=" + geo.getLongitude() + ", name=" + name
                    + " form " + getIntent().getDataString());
            if (!GeoPointDto.isEmpty(geo)) {
                // non-fuzzy-geo that already has lat/lon => search via lat/lon
                CacheListActivity.startActivityCoordinates(this, new Geopoint(geo.getLatitude(), geo.getLongitude()), name);
            } else if (name != null) {
                // fuzzy geo with search-query and without lat/lon => search via address
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
        //init();
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

    @SuppressLint("SetTextI18n")
    private SearchCardView addSearchCardWithField(final int title, final int icon, @NonNull final Runnable runnable, final Func1<String, String[]> suggestionFunction) {
        return addSearchCard(title, icon).addOnClickListener(() -> {
            binding.searchGroup.setVisibility(View.VISIBLE);

            binding.searchLabel.setHint(title);
            EditUtils.setActionListener(binding.searchField, runnable);
            if (suggestionFunction != null) {
                binding.searchField.setAdapter(new GeocacheAutoCompleteAdapter(binding.searchField.getContext(), suggestionFunction));
            }
            binding.searchButton.setOnClickListener(arg0 -> runnable.run());

            // Todo: Temp hack for geocode field
            if (title == R.string.search_geo) {
                binding.searchField.setText("GC");
                handlePotentialClipboardGeocode(binding.searchField, binding.searchLabel);
            }

            binding.searchField.setSelection(binding.searchField.getText().length());
            Keyboard.show(this, binding.searchField);
        });
    }

    private SearchCardView addSearchCard(final int title, final int icon) {
        final SearchCardView cardView = (SearchCardView) getLayoutInflater().inflate(R.layout.search_card, binding.gridLayout, false);
        cardView.setIcon(icon);
        cardView.setTitle(title);
        binding.gridLayout.addView(cardView);
        return cardView;
    }

    private void init() {

        // don't populate if already populated
        if (binding.gridLayout.getChildCount() > 0) {
            return;
        }

        final CardView geocodecard = addSearchCardWithField(R.string.search_geo, R.drawable.search_identifier, () -> findByGeocodeFn(binding.searchField.getText().toString()), DataStore::getSuggestionsGeocode);
        geocodecard.setOnLongClickListener(v -> {
            final String clipboardText = ClipboardUtils.getText();
            final String geocode;
            if (ConnectorFactory.getConnector(clipboardText) instanceof ISearchByGeocode) {
                geocode = clipboardText;
            } else {
                geocode = ConnectorFactory.getGeocodeFromText(clipboardText);
            }
            if (!StringUtils.isEmpty(geocode)) {
                findByGeocodeFn(geocode);
            } else {
                geocodecard.callOnClick();
            }
            return true;
        });

        addSearchCard(R.string.search_filter, R.drawable.ic_menu_filter)
                .addOnClickListener(this::findByFilterFn)
                .addOnLongClickListener(() -> SimpleDialog.of(this).setMessage(TextParam.id(R.string.search_filter_info_message).setMarkdown(true)).show());

        // Todo: Change CoordinateInputDialog to support a callback
        addSearchCard(R.string.search_coordinates, R.drawable.ic_menu_mylocation)
                .addOnClickListener(this::findByCoordsFn);

        addSearchCardWithField(R.string.search_address, R.drawable.ic_menu_home, this::findByAddressFn, null);

        final SearchCardView kwcard = addSearchCardWithField(R.string.search_kw, R.drawable.search_keyword, this::findByKeywordFn, DataStore::getSuggestionsKeyword);
        // mitigation for #13312
        if (!Settings.isGCPremiumMember()) {
            final int activeCount = ConnectorFactory.getActiveConnectors().size();
            if (GCConnector.getInstance().isActive() && (activeCount == 1 || (activeCount == 2 && ALConnector.getInstance().isActive()))) {
                // only gc.com connectors active, and user has basic member status => disable keyword search
                kwcard.addOnClickListener(() -> SimpleDialog.of(this).setMessage(TextParam.id(R.string.search_kw_disabled_hint)).show());
                ((ImageView) kwcard.findViewById(R.id.icon)).setColorFilter(R.color.colorTextHint, PorterDuff.Mode.SRC_IN);
            }
        }

        addSearchCardWithField(R.string.search_hbu, R.drawable.ic_menu_owned, this::findByOwnerFn, DataStore::getSuggestionsOwnerName);

        addSearchCardWithField(R.string.search_finder, R.drawable.ic_menu_emoticons, this::findByFinderFn, DataStore::getSuggestionsFinderName);

        addSearchCardWithField(R.string.search_tb, R.drawable.trackable_all, this::findTrackableFn, DataStore::getSuggestionsTrackableCode);
    }

    /**
     * Detect geocodes in clipboard
     * <br>
     * Needs to run async as clipboard access is blocked if activity is not yet created.
     */
    private void handlePotentialClipboardGeocode(AutoCompleteTextView geocodeField, TextInputLayout geocodeInputLayout) {
        final String clipboardText = ClipboardUtils.getText();

        String geocode = "";

        if (ConnectorFactory.getConnector(clipboardText) instanceof ISearchByGeocode) {
            geocode = clipboardText;
        } else {
            geocode = ConnectorFactory.getGeocodeFromText(clipboardText);
        }
        if (!StringUtils.isEmpty(geocode)) {
            geocodeField.setText(geocode);
            geocodeInputLayout.setHelperText(getString(R.string.search_geocode_from_clipboard));

            // clear hint if text input get changed
            geocodeField.addTextChangedListener(new TextWatcher() {
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
                    geocodeInputLayout.setHelperText(null);
                    geocodeField.removeTextChangedListener(this);
                }
            });
        }
    }

    @Override
    public void updateCoordinates(@NonNull final Geopoint gp) {
        //ViewUtils.setCoordinates(gp, binding.buttonLatLongitude);
    }

    @Override
    public boolean supportsNullCoordinates() {
        return false;
    }

    private void findByCoordsFn() {
        //Geopoint geopoint = ViewUtils.getCoordinates(binding.buttonLatLongitude);
        Geopoint geopoint = null;
        if (geopoint == null || !geopoint.isValid()) {
            geopoint = LocationDataProvider.getInstance().currentGeo().getCoords();
            if (geopoint.isValid()) {
                updateCoordinates(geopoint);
            } else {
                return;
            }
        }

        try {
            CacheListActivity.startActivityCoordinates(this, geopoint, null);
            ActivityMixin.overrideTransitionToFade(this);
        } catch (final Geopoint.ParseException e) {
            showToast(res.getString(e.resource));
        }
    }

    private void findByKeywordFn() {
        // find caches by keyword
        final String keyText = StringUtils.trim(binding.searchField.getText().toString());

        if (StringUtils.isBlank(keyText)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_keyword).show();
            return;
        }

        CacheListActivity.startActivityKeyword(this, keyText);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByAddressFn() {
        final String addressSearchText = StringUtils.trim(binding.searchField.getText().toString());

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
        findByOwnerFn(binding.searchField.getText().toString());
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
        findByFinderFn(binding.searchField.getText().toString());
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
        final String trackableText = StringUtils.trimToEmpty(binding.searchField.getText().toString());

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

}
