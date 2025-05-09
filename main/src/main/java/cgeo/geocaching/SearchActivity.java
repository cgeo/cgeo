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
import cgeo.geocaching.search.SearchAutoCompleteAdapter;
import cgeo.geocaching.sensors.LocationDataProvider;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.SearchCardView;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.NewCoordinateInputDialog;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.ClipboardUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func0;
import cgeo.geocaching.utils.functions.Func1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;

import java.util.Locale;
import java.util.function.Consumer;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;
import org.apache.commons.lang3.StringUtils;

public class SearchActivity extends AbstractNavigationBarActivity {
    private SearchActivityBinding binding;

    private static final String GOOGLE_NOW_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION";
    public static final String ACTION_CLIPBOARD = "clipboard";
    private AutoCompleteTextView searchView;
    private MenuItem searchViewItem;
    private MenuItem searchButtonItem;
    private boolean searchPerformed;

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
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != searchView) {
            if (searchPerformed) {
                // search triggered from search field -> return to main screen
                searchViewItem.collapseActionView();
            } else if (searchViewItem.isActionViewExpanded() && (getSearchFieldInput().isEmpty() || (getSearchFieldInput().equals("GC") && searchView.getHint().equals(getString(R.string.search_geo))))) {
                // search triggered from suggestion without any input in search field -> return to main screen
                searchViewItem.collapseActionView();
            } else if (searchViewItem.isActionViewExpanded()) {
                // search triggered from suggestion -> hide keyboard
                searchView.clearFocus();
            } else {
                binding.gridLayout.removeAllViews();
                init();
            }
        }
        searchPerformed = false;
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
    private SearchCardView addSearchCardWithField(final int title, final int suggestionIcon, @NonNull final Consumer<String> searchFunction, final Func1<String, String[]> suggestionFunction, final Func0<String[]> historyFunction, final InputFilter inputFilter) {
        return addSearchCard(title, suggestionIcon).addOnClickListener(() -> {
            // show search field
            searchViewItem.setVisible(true);
            searchViewItem.expandActionView();
            searchButtonItem.setVisible(true);
            searchView.setMinWidth(((View) searchView.getParent()).getWidth());

            // icon in search field
            final Drawable drw = AppCompatResources.getDrawable(this, suggestionIcon);
            drw.setTint(getResources().getColor(R.color.colorTextActionBar));
            drw.setBounds(0, 0, (int) searchView.getTextSize(), (int) searchView.getTextSize());
            searchView.setCompoundDrawables(drw, null, null, null);
            searchView.setCompoundDrawablePadding(16);
            searchView.setHint(title);

            // Submit function
            searchView.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH && (null == event || event.getAction() == KeyEvent.ACTION_DOWN)) {
                    searchFunction.accept(getSearchFieldInput());
                    searchPerformed = true;
                    return true;
                }
                return false;
            });
            searchButtonItem.setOnMenuItemClickListener(v -> {
                searchFunction.accept(getSearchFieldInput());
                searchPerformed = true;
                return true;
            });

            // Default value
            searchView.setText("");

            // suggestion handler
            binding.suggestionList.setOnItemClickListener((parent, view, position, id) -> {
                final String searchTerm = (String) parent.getItemAtPosition(position);
                searchFunction.accept(searchTerm);
            });

            if (title == R.string.search_geo) {
                searchView.setText("GC");
                binding.suggestionList.setAdapter(new GeocacheAutoCompleteAdapter.GeocodeAutoCompleteAdapter(searchView.getContext(), suggestionFunction, historyFunction));
                final String clipboardGeocode = getGeocodeFromClipboard();
                if (null != clipboardGeocode) {
                    binding.suggestionList.postDelayed(() -> {
                        binding.clipboardGeocode.setVisibility(View.VISIBLE);
                        ((TextView) binding.clipboardGeocode.findViewById(R.id.text)).setText(clipboardGeocode);
                        ((TextView) binding.clipboardGeocode.findViewById(R.id.info)).setText(R.string.from_clipboard);
                        ((ImageView) binding.clipboardGeocode.findViewById(R.id.icon)).setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.search_clipboard));
                        binding.clipboardGeocode.setOnClickListener(v -> findByGeocodeFn(clipboardGeocode));
                    }, 0);
                }
            } else if (title == R.string.search_kw) {
                binding.suggestionList.setAdapter(new GeocacheAutoCompleteAdapter.KeywordAutoCompleteAdapter(searchView.getContext(), suggestionFunction, historyFunction));
                binding.suggestionList.setOnItemClickListener((parent, view, position, id) -> {
                    final String searchTerm = (String) parent.getItemAtPosition(position);
                    // suggestions are a mix of geocodes and keywords, differentiate them by layout used
                    if (null == view.findViewById(R.id.info)) {
                        findByKeywordFn(searchTerm);
                    } else {
                        findByGeocodeFn(searchTerm);
                    }
                });
            } else {
                binding.suggestionList.setAdapter(new SearchAutoCompleteAdapter(searchView.getContext(), R.layout.search_suggestion, suggestionFunction, suggestionIcon, historyFunction));
            }

            updateSuggestions();

            // caps keyboard
            if (inputFilter != null) {
                searchView.setFilters(new InputFilter[] { inputFilter });
            } else {
                searchView.setFilters(new InputFilter[] { });
            }

            // show keyboard and place cursor
            searchView.setSelection(searchView.getText().length());
            Keyboard.show(this, searchView);
        });
    }

    private void updateSuggestions() {
        final ListAdapter adapter = binding.suggestionList.getAdapter();

        if (adapter instanceof ArrayAdapter) {
            ((ArrayAdapter<?>) adapter).getFilter().filter(searchView.getText());
        }

        binding.clipboardGeocode.setVisibility(View.GONE);
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

        final CardView geocodeCard = addSearchCardWithField(R.string.search_geo, R.drawable.search_identifier, this::findByGeocodeFn, DataStore::getSuggestionsGeocode, GeocacheAutoCompleteAdapter::getLastOpenedCachesArray, new InputFilter.AllCaps());
        geocodeCard.setOnLongClickListener(v -> {
            final String geocode = getGeocodeFromClipboard();
            if (null != geocode) {
                findByGeocodeFn(geocode);
                return true;
            }
            return false;
        });

        final SearchCardView kwCard = addSearchCardWithField(R.string.search_kw, R.drawable.search_keyword, this::findByKeywordFn, DataStore::getSuggestionsKeyword, () -> Settings.getHistoryList(R.string.pref_search_history_keyword), null);
        // mitigation for #13312
        if (!Settings.isGCPremiumMember()) {
            final int activeCount = ConnectorFactory.getActiveConnectors().size();
            if (GCConnector.getInstance().isActive() && (activeCount == 1 || (activeCount == 2 && ALConnector.getInstance().isActive()))) {
                // only gc.com connectors active, and user has basic member status => disable keyword search
                kwCard.addOnClickListener(() -> SimpleDialog.of(this).setMessage(TextParam.id(R.string.search_kw_disabled_hint)).show());
                ((ImageView) kwCard.findViewById(R.id.icon)).getDrawable().setTint(getResources().getColor(R.color.colorTextHint));
            }
        }

        addSearchCard(R.string.search_coordinates, R.drawable.ic_menu_mylocation)
                .addOnClickListener(this::onClickCoordinates);

        addSearchCardWithField(R.string.search_address, R.drawable.ic_menu_home, this::findByAddressFn, null, () -> Settings.getHistoryList(R.string.pref_search_history_address), null);

        addSearchCardWithField(R.string.search_hbu, R.drawable.search_owner, this::findByOwnerFn, DataStore::getSuggestionsOwnerName, () -> Settings.getHistoryList(R.string.pref_search_history_owner), null);

        addSearchCardWithField(R.string.search_finder, R.drawable.search_finder, this::findByFinderFn, DataStore::getSuggestionsFinderName, () -> Settings.getHistoryList(R.string.pref_search_history_finder), null);

        addSearchCard(R.string.search_filter, R.drawable.ic_menu_filter)
                .addOnClickListener(() -> GeocacheFilterActivity.selectFilter(this, new GeocacheFilterContext(GeocacheFilterContext.FilterType.LIVE), null, false))
                .addOnLongClickListener(() -> SimpleDialog.of(this).setMessage(TextParam.id(R.string.search_filter_info_message).setMarkdown(true)).show());

        addSearchCardWithField(R.string.search_tb, R.drawable.trackable_all, this::findTrackableFn, DataStore::getSuggestionsTrackableCode, () -> Settings.getHistoryList(R.string.pref_search_history_trackable), new InputFilter.AllCaps());

        addSearchCard(R.string.search_own_caches, R.drawable.ic_menu_owned)
                .addOnClickListener(() -> findByOwnerFn(Settings.getUserName()));

        addSearchCard(R.string.caches_history, R.drawable.ic_menu_recent_history)
                .addOnClickListener(() -> startActivity(CacheListActivity.getHistoryIntent(this)));
    }

    private String getGeocodeFromClipboard() {
        final String clipboardText = ClipboardUtils.getText();

        final String geocode;
        if (ConnectorFactory.getConnector(clipboardText) instanceof ISearchByGeocode) {
            geocode = clipboardText;
        } else {
            geocode = ConnectorFactory.getGeocodeFromText(clipboardText);
        }
        if (!StringUtils.isEmpty(geocode)) {
            return geocode;
        } else {
            return null;
        }
    }

    private void onClickCoordinates() {
        NewCoordinateInputDialog.show(this, this::onUpdateCoordinates, LocationDataProvider.getInstance().currentGeo().getCoords());
    }

    public void onUpdateCoordinates(final Geopoint input) {
        try {
            CacheListActivity.startActivityCoordinates(this, input, null);
            ActivityMixin.overrideTransitionToFade(this);
        } catch (final Geopoint.ParseException e) {
            showToast(res.getString(e.resource));
        }
    }
    private void findByKeywordFn(final String keyText) {
        if (StringUtils.isBlank(keyText)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_keyword).show();
            return;
        }

        Settings.addToHistoryList(R.string.pref_search_history_keyword, keyText);
        CacheListActivity.startActivityKeyword(this, keyText);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByAddressFn(final String addressSearchText) {
        if (StringUtils.isBlank(addressSearchText)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_address).show();
            return;
        }

        final Intent addressesIntent = new Intent(this, AddressListActivity.class);
        addressesIntent.putExtra(Intents.EXTRA_KEYWORD, addressSearchText);
        startActivity(addressesIntent);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByOwnerFn(final String userName) {
        if (StringUtils.isBlank(userName)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_user).show();
            return;
        }

        Settings.addToHistoryList(R.string.pref_search_history_owner, userName);
        CacheListActivity.startActivityOwner(this, userName);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByFinderFn(final String usernameText) {
        if (StringUtils.isBlank(usernameText)) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_user).show();
            return;
        }

        Settings.addToHistoryList(R.string.pref_search_history_finder, usernameText);

        CacheListActivity.startActivityFinder(this, usernameText);
        ActivityMixin.overrideTransitionToFade(this);
    }

    private void findByGeocodeFn(final String geocode) {
        if (StringUtils.isBlank(geocode) || geocode.equalsIgnoreCase("GC")) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_gccode).show();
            return;
        }

        if (ConnectorFactory.anyConnectorActive()) {
            CacheDetailActivity.startActivity(this, geocode.toUpperCase(Locale.US));
        } else {
            showToast(getString(R.string.warn_no_connector));
        }
    }

    private void findTrackableFn(final String trackableText) {
        if (StringUtils.isBlank(trackableText) || trackableText.equalsIgnoreCase("TB")) {
            SimpleDialog.of(this).setTitle(R.string.warn_search_help_title).setMessage(R.string.warn_search_help_tb).show();
            return;
        }
        Settings.addToHistoryList(R.string.pref_search_history_trackable, trackableText);
        final Intent trackablesIntent = new Intent(this, TrackableActivity.class);
        trackablesIntent.putExtra(Intents.EXTRA_GEOCODE, trackableText.toUpperCase(Locale.US));
        startActivity(trackablesIntent);
    }

    private String getSearchFieldInput() {
        return StringUtils.trimToEmpty(searchView.getText().toString());
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

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.search_activity_options, menu);
        searchViewItem = menu.findItem(R.id.menu_gosearch);
        searchViewItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull final MenuItem item) {
                binding.flipper.showNext();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull final MenuItem item) {
                searchViewItem.setVisible(false);
                searchButtonItem.setVisible(false);
                binding.flipper.showPrevious();
                return true;
            }
        });
        searchButtonItem = menu.findItem(R.id.menu_gosearch_icon);
        searchView = (AutoCompleteTextView) searchViewItem.getActionView();
        searchView.setBackground(AppCompatResources.getDrawable(this, R.drawable.mark_transparent));
        // configure keyboard
        searchView.setInputType(InputType.TYPE_CLASS_TEXT);
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchView.addTextChangedListener(ViewUtils.createSimpleWatcher((s) -> updateSuggestions()));
        return true;
    }
}
