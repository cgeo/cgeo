package cgeo.geocaching;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import cgeo.geocaching.activity.AbstractActivity;

public class cgeoadvsearch extends AbstractActivity {

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

		settings.load();
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
				final Intent cachesIntent = new Intent(this, cgeocaches.class);
				cachesIntent.putExtra("type", "keyword");
				cachesIntent.putExtra("keyword", query);
				cachesIntent.putExtra("cachetype", settings.cacheType);
				startActivity(cachesIntent);

				found = true;
			}
		} catch (Exception e) {
			Log.w(cgSettings.tag, "cgeoadvsearch.instantSearch: " + e.toString());
		}

		return found;
	}

	private void init() {
		settings.getLogin();
		settings.reloadCacheType();

		if (settings.cacheType != null && cgBase.cacheTypesInv.containsKey(settings.cacheType) == false) {
			settings.setCacheType(null);
		}

		if (geo == null) {
			geo = app.startGeo(this, geoUpdate, base, settings, 0, 0);
		}

		((EditText) findViewById(R.id.latitude)).setOnEditorActionListener(new findByCoordsAction());
		((EditText) findViewById(R.id.longitude)).setOnEditorActionListener(new findByCoordsAction());

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
		findByOwner.setOnClickListener(new findByOwnerListener());

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

				if (geo.latitudeNow != null && geo.longitudeNow != null) {
					latEdit.setHint(cgBase.formatLatitude(geo.latitudeNow, false));
					lonEdit.setHint(cgBase.formatLongitude(geo.longitudeNow, false));
				}
			} catch (Exception e) {
				Log.w(cgSettings.tag, "Failed to update location.");
			}
		}
	}

	private class findByCoordsAction implements TextView.OnEditorActionListener {

		@Override
		public boolean onEditorAction(TextView view, int action, KeyEvent event) {
			if (action == EditorInfo.IME_ACTION_GO) {
				findByCoordsFn();
				return true;
			}

			return false;
		}
	}

	private class findByCoordsListener implements View.OnClickListener {

		public void onClick(View arg0) {
			findByCoordsFn();
		}
	}

	private void findByCoordsFn() {
		final EditText latView = (EditText) findViewById(R.id.latitude);
		final EditText lonView = (EditText) findViewById(R.id.longitude);
		final String latText = latView.getText().toString();
		final String lonText = lonView.getText().toString();

		if (StringUtils.isEmpty(latText) || StringUtils.isEmpty(lonText)) {
			latView.setText(cgBase.formatLatitude(geo.latitudeNow, true));
			lonView.setText(cgBase.formatLongitude(geo.longitudeNow, true));
		} else {
			Map<String, Object> latParsed = cgBase.parseCoordinate(latText, "lat");
			Map<String, Object> lonParsed = cgBase.parseCoordinate(lonText, "lat");

			if (latParsed == null || latParsed.get("coordinate") == null || latParsed.get("string") == null) {
				showToast(res.getString(R.string.err_parse_lat));
				return;
			}

			if (lonParsed == null || lonParsed.get("coordinate") == null || lonParsed.get("string") == null) {
				showToast(res.getString(R.string.err_parse_lon));
				return;
			}

			final Intent cachesIntent = new Intent(this, cgeocaches.class);
			cachesIntent.putExtra("type", "coordinate");
			cachesIntent.putExtra("latitude", (Double) latParsed.get("coordinate"));
			cachesIntent.putExtra("longitude", (Double) lonParsed.get("coordinate"));
			cachesIntent.putExtra("cachetype", settings.cacheType);
			startActivity(cachesIntent);
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

		final Intent cachesIntent = new Intent(this, cgeocaches.class);
		cachesIntent.putExtra("type", "keyword");
		cachesIntent.putExtra("keyword", keyText);
		cachesIntent.putExtra("cachetype", settings.cacheType);
		startActivity(cachesIntent);
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

		final Intent cachesIntent = new Intent(this, cgeocaches.class);
		cachesIntent.putExtra("type", "username");
		cachesIntent.putExtra("username", usernameText);
		cachesIntent.putExtra("cachetype", settings.cacheType);
		startActivity(cachesIntent);
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

	private class findByOwnerListener implements View.OnClickListener {

		public void onClick(View arg0) {
			findByOwnerFn();
		}
	}

	private void findByOwnerFn() {
		final String usernameText = ((EditText) findViewById(R.id.owner)).getText().toString();

		if (StringUtils.isBlank(usernameText)) {
			helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_user));
			return;
		}

		final Intent cachesIntent = new Intent(this, cgeocaches.class);
		cachesIntent.putExtra("type", "owner");
		cachesIntent.putExtra("username", usernameText);
		cachesIntent.putExtra("cachetype", settings.cacheType);
		startActivity(cachesIntent);
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

		if (StringUtils.isBlank(trackableText)|| trackableText.equalsIgnoreCase("TB")) {
			helpDialog(res.getString(R.string.warn_search_help_title), res.getString(R.string.warn_search_help_tb));
			return;
		}

		final Intent trackablesIntent = new Intent(this, cgeotrackable.class);
		trackablesIntent.putExtra("geocode", trackableText.toUpperCase());
		startActivity(trackablesIntent);
	}
}
