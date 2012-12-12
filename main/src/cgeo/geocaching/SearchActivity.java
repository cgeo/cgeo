package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.ISearchByGeocode;
import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.utils.BaseUtils;
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

public class SearchActivity extends AbstractActivity {

    private static final String EXTRAS_KEYWORDSEARCH = "keywordsearch";

    private static final int MENU_SEARCH_OWN_CACHES = 1;
    private EditText latEdit = null;
    private EditText lonEdit = null;

    public SearchActivity() {
        super("c:geo-search");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // search query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            final boolean keywordSearch = intent.getBooleanExtra(EXTRAS_KEYWORDSEARCH, true);

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
        setContentView(R.layout.search);
        setTitle(res.getString(R.string.search));

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
            geocode = StringUtils.trim(query);
        }

        final IConnector connector = ConnectorFactory.getConnector(geocode);
        if (connector instanceof ISearchByGeocode) {
            final Intent cachesIntent = new Intent(this, CacheDetailActivity.class);
            cachesIntent.putExtra("geocode", geocode.toUpperCase());
            startActivity(cachesIntent);
            return true;
        }

        // Check if the query is a TB code
        final String trackable = BaseUtils.getMatch(query, GCConstants.PATTERN_TB_CODE, true, 0, "", false);
        if (StringUtils.isNotBlank(trackable)) {
            final Intent trackablesIntent = new Intent(this, cgeotrackable.class);
            trackablesIntent.putExtra("geocode", trackable.toUpperCase());
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
        Settings.getLogin();

        findViewById(R.id.buttonLatitude).setOnClickListener(new findByCoordsAction());
        findViewById(R.id.buttonLongitude).setOnClickListener(new findByCoordsAction());

        final Button findByCoords = (Button) findViewById(R.id.search_coordinates);
        findByCoords.setOnClickListener(new findByCoordsListener());

        EditUtils.setActionListener((EditText) findViewById(R.id.address), new Runnable() {

            @Override
            public void run() {
                findByAddressFn();
            }
        });

        final Button findByAddress = (Button) findViewById(R.id.search_address);
        findByAddress.setOnClickListener(new findByAddressListener());

        final AutoCompleteTextView geocodeEdit = (AutoCompleteTextView) findViewById(R.id.geocode);
        EditUtils.setActionListener(geocodeEdit, new Runnable() {

            @Override
            public void run() {
                findByGeocodeFn();
            }
        });
        addHistoryEntries(geocodeEdit, app.geocodesInCache());

        final Button displayByGeocode = (Button) findViewById(R.id.display_geocode);
        displayByGeocode.setOnClickListener(new findByGeocodeListener());

        EditUtils.setActionListener((EditText) findViewById(R.id.keyword), new Runnable() {

            @Override
            public void run() {
                findByKeywordFn();
            }
        });

        final Button findByKeyword = (Button) findViewById(R.id.search_keyword);
        findByKeyword.setOnClickListener(new findByKeywordListener());

        EditUtils.setActionListener((EditText) findViewById(R.id.username), new Runnable() {

            @Override
            public void run() {
                findByUsernameFn();
            }
        });

        final Button findByUserName = (Button) findViewById(R.id.search_username);
        findByUserName.setOnClickListener(new findByUsernameListener());

        EditUtils.setActionListener((EditText) findViewById(R.id.owner), new Runnable() {

            @Override
            public void run() {
                findByOwnerFn();
            }
        });

        final Button findByOwner = (Button) findViewById(R.id.search_owner);
        findByOwner.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                findByOwnerFn();
            }
        });

        AutoCompleteTextView trackable = (AutoCompleteTextView) findViewById(R.id.trackable);
        EditUtils.setActionListener(trackable, new Runnable() {

            @Override
            public void run() {
                findTrackableFn();
            }
        });
        addHistoryEntries(trackable, app.getTrackableCodes());

        disableSuggestions(trackable);

        final Button displayTrackable = (Button) findViewById(R.id.display_trackable);
        displayTrackable.setOnClickListener(new findTrackableListener());
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
                if (latEdit == null) {
                    latEdit = (EditText) findViewById(R.id.latitude);
                }
                if (lonEdit == null) {
                    lonEdit = (EditText) findViewById(R.id.longitude);
                }

                if (geo.getCoords() != null) {
                    if (latEdit != null) {
                        latEdit.setHint(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE_RAW));
                    }
                    if (lonEdit != null) {
                        lonEdit.setHint(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE_RAW));
                    }
                }
            } catch (Exception e) {
                Log.w("Failed to update location.");
            }
        }
    };

    private class findByCoordsAction implements OnClickListener {

        @Override
        public void onClick(View arg0) {
            cgeocoords coordsDialog = new cgeocoords(SearchActivity.this, null, null, app.currentGeo());
            coordsDialog.setCancelable(true);
            coordsDialog.setOnCoordinateUpdate(new cgeocoords.CoordinateUpdate() {
                @Override
                public void update(Geopoint gp) {
                    ((Button) findViewById(R.id.buttonLatitude)).setText(gp.format(GeopointFormatter.Format.LAT_DECMINUTE));
                    ((Button) findViewById(R.id.buttonLongitude)).setText(gp.format(GeopointFormatter.Format.LON_DECMINUTE));
                }
            });
            coordsDialog.show();
        }
    }

    private class findByCoordsListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findByCoordsFn();
        }
    }

    private void findByCoordsFn() {
        final Button latView = (Button) findViewById(R.id.buttonLatitude);
        final Button lonView = (Button) findViewById(R.id.buttonLongitude);
        final String latText = latView.getText().toString();
        final String lonText = lonView.getText().toString();

        if (StringUtils.isEmpty(latText) || StringUtils.isEmpty(lonText)) {
            final IGeoData geo = app.currentGeo();
            if (geo.getCoords() != null) {
                latView.setText(geo.getCoords().format(GeopointFormatter.Format.LAT_DECMINUTE));
                lonView.setText(geo.getCoords().format(GeopointFormatter.Format.LON_DECMINUTE));
            }
        } else {
            try {
                cgeocaches.startActivityCoordinates(this, new Geopoint(latText, lonText));
            } catch (Geopoint.ParseException e) {
                showToast(res.getString(e.resource));
            }
        }
    }

    private class findByKeywordListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findByKeywordFn();
        }
    }

    private void findByKeywordFn() {
        // find caches by coordinates
        String keyText = ((EditText) findViewById(R.id.keyword)).getText().toString();

        if (StringUtils.isBlank(keyText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_keyword));
            return;
        }

        cgeocaches.startActivityKeyword(this, keyText);
    }

    private class findByAddressListener implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            findByAddressFn();
        }
    }

    private void findByAddressFn() {
        final String addText = ((EditText) findViewById(R.id.address)).getText().toString();

        if (StringUtils.isBlank(addText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_address));
            return;
        }

        final Intent addressesIntent = new Intent(this, AdressListActivity.class);
        addressesIntent.putExtra("keyword", addText);
        startActivity(addressesIntent);
    }

    private class findByUsernameListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findByUsernameFn();
        }
    }

    public void findByUsernameFn() {
        final String usernameText = ((EditText) findViewById(R.id.username)).getText().toString();

        if (StringUtils.isBlank(usernameText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_user));
            return;
        }

        cgeocaches.startActivityUserName(this, usernameText);
    }

    private void findByOwnerFn() {
        findByOwnerFn(((EditText) findViewById(R.id.owner)).getText().toString());
    }

    private void findByOwnerFn(String userName) {
        final String usernameText = StringUtils.trimToEmpty(userName);

        if (StringUtils.isBlank(usernameText)) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_user));
            return;
        }

        cgeocaches.startActivityOwner(this, usernameText);
    }

    private class findByGeocodeListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findByGeocodeFn();
        }
    }

    private void findByGeocodeFn() {
        final String geocodeText = ((EditText) findViewById(R.id.geocode)).getText().toString();

        if (StringUtils.isBlank(geocodeText) || geocodeText.equalsIgnoreCase("GC")) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_gccode));
            return;
        }

        CacheDetailActivity.startActivity(this, geocodeText);
    }

    private class findTrackableListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            findTrackableFn();
        }
    }

    private void findTrackableFn() {
        final String trackableText = ((EditText) findViewById(R.id.trackable)).getText().toString();

        if (StringUtils.isBlank(trackableText) || trackableText.equalsIgnoreCase("TB")) {
            helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_tb));
            return;
        }

        final Intent trackablesIntent = new Intent(this, cgeotrackable.class);
        trackablesIntent.putExtra("geocode", trackableText.toUpperCase());
        startActivity(trackablesIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_SEARCH_OWN_CACHES, 0, res.getString(R.string.search_own_caches)).setIcon(R.drawable.ic_menu_myplaces);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_SEARCH_OWN_CACHES) {
            findByOwnerFn(Settings.getUsername());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void startActivityScan(final String scan, final Activity fromActivity) {
        final Intent searchIntent = new Intent(fromActivity, SearchActivity.class);
        searchIntent.setAction(Intent.ACTION_SEARCH).
                putExtra(SearchManager.QUERY, scan).
                putExtra(SearchActivity.EXTRAS_KEYWORDSEARCH, false);
        fromActivity.startActivityForResult(searchIntent, cgeo.SEARCH_REQUEST_CODE);
    }
}
