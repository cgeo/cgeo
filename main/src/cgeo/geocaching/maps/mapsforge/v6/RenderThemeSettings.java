package cgeo.geocaching.maps.mapsforge.v6;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.app.ActionBar;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Locale;
import java.util.Map;

import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

public class RenderThemeSettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String RENDERTHEME_MENU = "renderthememenu";

    ListPreference baseLayerPreference;
    SharedPreferences prefs;
    XmlRenderThemeStyleMenu renderthemeOptions;
    PreferenceCategory renderthemeMenu;

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }
    @Override
    public void onSharedPreferenceChanged(final SharedPreferences preferences,
                                          final String key) {
        if (this.renderthemeOptions != null && this.renderthemeOptions.getId().equals(key)) {
            createRenderthemeMenu();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(Settings.isLightSkin() ? R.style.settings_light : R.style.settings);
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        addPreferencesFromResource(R.xml.theme_prefs);

        // if the render theme has a style menu, its data is delivered via the intent
        renderthemeOptions = (XmlRenderThemeStyleMenu) getIntent().getSerializableExtra(RENDERTHEME_MENU);
        if (renderthemeOptions != null) {

            // the preference category serves as the hook to add a list preference to allow users to select a style
            this.renderthemeMenu = (PreferenceCategory) findPreference(getString(R.string.pref_theme_menu));

            createRenderthemeMenu();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.prefs.registerOnSharedPreferenceChangeListener(this);
    }


    @SuppressWarnings("deprecation")
    private void createRenderthemeMenu() {
        this.renderthemeMenu.removeAll();

        this.baseLayerPreference = new ListPreference(this);

        // the id of the setting is the id of the stylemenu, that allows this
        // app to store different settings for different render themes.
        baseLayerPreference.setKey(this.renderthemeOptions.getId());

        baseLayerPreference.setTitle("Map style");

        // this is the user language for the app, in 'en', 'de' etc format
        // no dialects are supported at the moment
        final String language = Locale.getDefault().getLanguage();

        // build data structure for the ListPreference
        final Map<String, XmlRenderThemeStyleLayer> baseLayers = renderthemeOptions.getLayers();

        int visibleStyles = 0;
        for (final XmlRenderThemeStyleLayer baseLayer : baseLayers.values()) {
            if (baseLayer.isVisible()) {
                ++visibleStyles;
            }
        }

        final CharSequence[] entries = new CharSequence[visibleStyles];
        final CharSequence[] values = new CharSequence[visibleStyles];
        int i = 0;
        for (final XmlRenderThemeStyleLayer baseLayer : baseLayers.values()) {
            if (baseLayer.isVisible()) {
                // build up the entries in the list
                entries[i] = baseLayer.getTitle(language);
                values[i] = baseLayer.getId();
                ++i;
            }
        }

        baseLayerPreference.setEntries(entries);
        baseLayerPreference.setEntryValues(values);
        baseLayerPreference.setEnabled(true);
        baseLayerPreference.setPersistent(true);
        baseLayerPreference.setDefaultValue(renderthemeOptions.getDefaultValue());

        renderthemeMenu.addPreference(baseLayerPreference);

        String selection = baseLayerPreference.getValue();
        // need to check that the selection stored is actually a valid getLayer in the current
        // rendertheme.
        if (selection == null || !renderthemeOptions.getLayers().containsKey(selection)) {
            selection = renderthemeOptions.getLayer(renderthemeOptions.getDefaultValue()).getId();
        }
        // the new Android style is to display information here, not instruction
        baseLayerPreference.setSummary(renderthemeOptions.getLayer(selection).getTitle(language));

        for (final XmlRenderThemeStyleLayer overlay : this.renderthemeOptions.getLayer(selection).getOverlays()) {
            final CheckBoxPreference checkbox = new CheckBoxPreference(this);
            checkbox.setKey(overlay.getId());
            checkbox.setPersistent(true);
            checkbox.setTitle(overlay.getTitle(language));
            if (findPreference(overlay.getId()) == null) {
                // value has never been set, so set from default
                checkbox.setChecked(overlay.isEnabled());
            }
            this.renderthemeMenu.addPreference(checkbox);
        }
    }
}
