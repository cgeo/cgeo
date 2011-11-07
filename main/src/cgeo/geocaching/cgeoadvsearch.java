package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.geopoint.GeopointParser;

import org.apache.commons.lang3.StringUtils;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class cgeoadvsearch extends AbstractActivity {

    private static final int MENU_SEARCH_OWN_CACHES = 1;
    private cgGeo geo = null;
    private cgUpdateLoc geoUpdate = new update();
    private EditText latEdit = null;
    private EditText lonEdit = null;
    private String[] geocodesInCache = null;

    public cgeoadvsearch() {
        super("c:geo-search");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init
        app.setAction(null);

        // search query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            final boolean found = instantSearch(query);

            if (found) {
                finish();

                return;
            }
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

        init();
    }

    @Override
    public void onDestroy() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onDestroy();
    }

    @Override
    public void onStop() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onStop();
    }

    @Override
    public void onPause() {
        if (geo != null) {
            geo = app.removeGeo();
        }

        super.onPause();
    }

    private boolean instantSearch(String query) {
        boolean found = false;

        final Pattern gcCode = Pattern.compile("^GC[0-9A-Z]+$", Pattern.CASE_INSENSITIVE);
        final Pattern tbCode = Pattern.compile("^TB[0-9A-Z]+$", Pattern.CASE_INSENSITIVE);
        final Matcher gcCodeM = gcCode.matcher(query);
        final Matcher tbCodeM = tbCode.matcher(query);

        try {
            if (gcCodeM.find()) { // GC-code
                final Intent cachesIntent = new Intent(this, cgeodetail.class);
                cachesIntent.putExtra("geocode", query.trim().toUpperCase());
                startActivity(cachesIntent);

                found = true;
            } else if (tbCodeM.find()) { // TB-code
                final Intent trackablesIntent = new Intent(this, cgeotrackable.class);
                trackablesIntent.putExtra("geocode", query.trim().toUpperCase());
                startActivity(trackablesIntent);

                found = true;
            } else { // keyword (fallback)
                cgeocaches.startActivityKeyword(this, query);
                found = true;
            }
        } catch (Exception e) {
            Log.w(Settings.tag, "cgeoadvsearch.instantSearch: " + e.toString());
        }

        return found;
    }

    private void init() {
        Settings.getLogin();

        if (Settings.getCacheType() != null && !cgBase.cacheTypesInv.containsKey(Settings.getCacheType())) {
            Settings.setCacheType(null);
        }

        if (geo == null) {
            geo = app.startGeo(this, geoUpdate, base, 0, 0);
        }

        ((Button) findViewById(R.id.buttonLatitude)).setOnClickListener(new findByCoordsAction());
        ((Button) findViewById(R.id.buttonLongitude)).setOnClickListener(new findByCoordsAction());

        final Button findByCoords = (Button) findViewById(R.id.search_coordinates);
        findByCoords.setOnClickListener(new findByCoordsListener());

        ((EditText) findViewById(R.id.address)).setOnEditorActionListener(new findByAddressAction());

        final Button findByAddress = (Button) findViewById(R.id.search_address);
        findByAddress.setOnClickListener(new findByAddressListener());

        final AutoCompleteTextView geocodeEdit = (AutoCompleteTextView) findViewById(R.id.geocode);
        geocodeEdit.setOnEditorActionListener(new findByGeocodeAction());
        geocodesInCache = app.geocodesInCache();
        if (geocodesInCache != null) {
            final ArrayAdapter<String> geocodesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, geocodesInCache);
            geocodeEdit.setAdapter(geocodesAdapter);
        }

        final Button displayByGeocode = (Button) findViewById(R.id.display_geocode);
        displayByGeocode.setOnClickListener(new findByGeocodeListener());

        ((EditText) findViewById(R.id.keyword)).setOnEditorActionListener(new findByKeywordAction());

        final Button findByKeyword = (Button) findViewById(R.id.search_keyword);
        findByKeyword.setOnClickListener(new findByKeywordListener());

        ((EditText) findViewById(R.id.username)).setOnEditorActionListener(new findByUsernameAction());

        final Button findByUserName = (Button) findViewById(R.id.search_username);
        findByUserName.setOnClickListener(new findByUsernameListener());

        ((EditText) findViewById(R.id.owner)).setOnEditorActionListener(new findByOwnerAction());

        final Button findByOwner = (Button) findViewById(R.id.search_owner);
        findByOwner.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                findByOwnerFn();
            }
        });

        EditText trackable = (EditText) findViewById(R.id.trackable);
        trackable.setOnEditorActionListener(new findTrackableAction());

        final Button displayTrackable = (Button) findViewById(R.id.display_trackable);
        displayTrackable.setOnClickListener(new findTrackableListener());
    }

    private class update extends cgUpdateLoc {

        @Override
        public void updateLoc(cgGeo geo) {
            if (geo == null) {
                return;
            }

            try {
                if (latEdit == null) {
                    latEdit = (EditText) findViewById(R.id.latitude);
                }
                if (lonEdit == null) {
                    lonEdit = (EditText) findViewById(R.id.longitude);
                }

                if (geo.coordsNow != null) {
                    if (latEdit != null) {
                        latEdit.setHint(geo.coordsNow.format(GeopointFormatter.Format.LAT_DECMINUTE_RAW));
                    }
                    if (lonEdit != null) {
                        lonEdit.setHint(geo.coordsNow.format(GeopointFormatter.Format.LON_DECMINUTE_RAW));
                    }
                }
            } catch (Exception e) {
                Log.w(Settings.tag, "Failed to update location.");
            }
        }
    }

    private class findByCoordsAction implements OnClickListener {

        @Override
        public void onClick(View arg0) {
            cgeocoords coordsDialog = new cgeocoords(cgeoadvsearch.this, null, geo);
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
            if (geo.coordsNow != null) {
                latView.setText(geo.coordsNow.format(GeopointFormatter.Format.LAT_DECMINUTE));
                lonView.setText(geo.coordsNow.format(GeopointFormatter.Format.LON_DECMINUTE));
            }
        } else {
            try {
                cgeocaches.startActivityCoordinates(this, GeopointParser.parseLatitude(latText), GeopointParser.parseLongitude(lonText));
            } catch (GeopointParser.ParseException e) {
                showToast(res.getString(e.resource));
            }
        }
    }

    private class findByKeywordAction implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView view, int action, KeyEvent event) {
            if (action == EditorInfo.IME_ACTION_GO) {
                findByKeywordFn();
                return true;
            }

            return false;
        }
    }

    private class findByKeywordListener implements View.OnClickListener {

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

    private class findByAddressAction implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView view, int action, KeyEvent event) {
            if (action == EditorInfo.IME_ACTION_GO) {
                findByAddressFn();
                return true;
            }

            return false;
        }
    }

    private class findByAddressListener implements View.OnClickListener {

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

        final Intent addressesIntent = new Intent(this, cgeoaddresses.class);
        addressesIntent.putExtra("keyword", addText);
        startActivity(addressesIntent);
    }

    private class findByUsernameAction implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView view, int action, KeyEvent event) {
            if (action == EditorInfo.IME_ACTION_GO) {
                findByUsernameFn();
                return true;
            }

            return false;
        }
    }

    private class findByUsernameListener implements View.OnClickListener {

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

    private class findByOwnerAction implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView view, int action, KeyEvent event) {
            if (action == EditorInfo.IME_ACTION_GO) {
                findByOwnerFn();
                return true;
            }

            return false;
        }
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

    private class findByGeocodeAction implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView view, int action, KeyEvent event) {
            if (action == EditorInfo.IME_ACTION_GO) {
                findByGeocodeFn();
                return true;
            }

            return false;
        }
    }

    private class findByGeocodeListener implements View.OnClickListener {

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

        cgeodetail.startActivity(this, geocodeText);
    }

    private class findTrackableAction implements TextView.OnEditorActionListener {

        @Override
        public boolean onEditorAction(TextView view, int action, KeyEvent event) {
            if (action == EditorInfo.IME_ACTION_GO) {
                findTrackableFn();
                return true;
            }

            return false;
        }
    }

    private class findTrackableListener implements View.OnClickListener {

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
        menu.add(0, MENU_SEARCH_OWN_CACHES, 0, res.getString(R.string.search_own_caches)).setIcon(android.R.drawable.ic_menu_myplaces);
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
}
