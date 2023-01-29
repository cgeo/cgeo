package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.SeekbarPreference;
import cgeo.geocaching.settings.Settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import java.util.Locale;
import java.util.Map;

import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;

public class MapsforgeThemeSettingsFragment extends PreferenceFragmentCompat {
    public static final String RENDERTHEME_MENU = "renderthememenu";

    ListPreference baseLayerPreference;

    XmlRenderThemeStyleMenu renderthemeOptions;
    PreferenceCategory renderthemeMenu;

    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.theme_prefs, rootKey);

        // if the render theme has a style menu, its data is delivered via the intent
        renderthemeOptions = (XmlRenderThemeStyleMenu) requireActivity().getIntent().getSerializableExtra(RENDERTHEME_MENU);
        // the preference category serves as the hook to add a list preference to allow users to select a style
        this.renderthemeMenu = findPreference(getString(R.string.pref_theme_menu));
        createRenderthemeMenu();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.settings_title_map_style);
    }

    private void createRenderthemeMenu() {
        final Activity activity = requireActivity();

        this.renderthemeMenu.removeAll();

        final String themeStylePrefKey;
        if (renderthemeOptions != null) {
            final String themePrefKey = createThemePreferences(activity);
            themeStylePrefKey = themePrefKey + "-" + PreferenceManager.getDefaultSharedPreferences(activity).getString(themePrefKey, "none");
        } else {
            themeStylePrefKey = Settings.RENDERTHEMESCALE_DEFAULTKEY;
        }

        if (Settings.isDefaultMapRenderTheme()) {
            this.renderthemeMenu.addPreference(VtmThemes.getPreference(activity));
        }

        //scale preferences for theme
        addScalePreference(activity, renderthemeMenu, Settings.getMapRenderScalePreferenceKey(themeStylePrefKey, Settings.RenderThemeScaleType.MAP),
                R.string.maptheme_scale_map_title, R.string.maptheme_scale_map_summary);
        addScalePreference(activity, renderthemeMenu, Settings.getMapRenderScalePreferenceKey(themeStylePrefKey, Settings.RenderThemeScaleType.TEXT),
                R.string.maptheme_scale_text_title, R.string.maptheme_scale_text_summary);
        addScalePreference(activity, renderthemeMenu, Settings.getMapRenderScalePreferenceKey(themeStylePrefKey, Settings.RenderThemeScaleType.SYMBOL),
                R.string.maptheme_scale_symbol_title, R.string.maptheme_scale_symbol_summary);
    }

    private String createThemePreferences(final Activity activity) {
        this.baseLayerPreference = new ListPreference(activity);
        baseLayerPreference.setTitle(R.string.settings_title_map_style);

        // the id of the setting is the id of the stylemenu, that allows this
        // app to store different settings for different render themes.
        final String themePrefKey = this.renderthemeOptions.getId();
        baseLayerPreference.setKey(themePrefKey);


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
        baseLayerPreference.setIconSpaceReserved(false);
        baseLayerPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            // need to persist the new value before recreating the renderthemeMenu
            Settings.setSelectedMapRenderThemeStyle(preference.getKey(), newValue.toString());
            createRenderthemeMenu();
            return true;
        });
        renderthemeMenu.addPreference(baseLayerPreference);

        // additional theme info
        /* @todo
        if (UnifiedThemeHelper.getRenderThemeType() == UnifiedThemeHelper.RenderThemeType.RTT_ELEVATE) {
            final Preference info = new Preference(activity);
            info.setSummary(R.string.maptheme_elevate_categoryinfo);
            info.setIconSpaceReserved(false);
            renderthemeMenu.addPreference(info);
        }
        */

        String selection = baseLayerPreference.getValue();
        // need to check that the selection stored is actually a valid getLayer in the current rendertheme.
        if (selection == null || !renderthemeOptions.getLayers().containsKey(selection)) {
            selection = renderthemeOptions.getLayer(renderthemeOptions.getDefaultValue()).getId();
        }
        // the new Android style is to display information here, not instruction
        baseLayerPreference.setSummary(renderthemeOptions.getLayer(selection).getTitle(language));

        for (final XmlRenderThemeStyleLayer overlay : this.renderthemeOptions.getLayer(selection).getOverlays()) {
            final CheckBoxPreference checkbox = new CheckBoxPreference(activity);
            checkbox.setKey(overlay.getId());
            checkbox.setPersistent(true);
            checkbox.setTitle(overlay.getTitle(language));
            if (findPreference(overlay.getId()) == null) {
                // value has never been set, so set from default
                checkbox.setChecked(overlay.isEnabled());
            }
            checkbox.setIconSpaceReserved(false);
            this.renderthemeMenu.addPreference(checkbox);
        }
        return themePrefKey;
    }

    private void addScalePreference(final Context context, final PreferenceGroup cat, final String prefKey, @StringRes final int titleId, @StringRes final int summaryId) {

        final Preference info = new Preference(context) {
            public void onBindViewHolder(final PreferenceViewHolder holder) {
                super.onBindViewHolder(holder);
                holder.setDividerAllowedAbove(false);
                holder.setDividerAllowedBelow(false);
            }
        };
        info.setTitle(titleId);
        info.setSummary(summaryId);
        info.setIconSpaceReserved(false);
        cat.addPreference(info);

        final SeekbarPreference seek = new SeekbarPreference(context, 10, 500, "", "%",
                new SeekbarPreference.FactorizeValueMapper(10));
        seek.setDefaultValue(100);
        seek.setKey(prefKey);
        cat.addPreference(seek);
    }

}
