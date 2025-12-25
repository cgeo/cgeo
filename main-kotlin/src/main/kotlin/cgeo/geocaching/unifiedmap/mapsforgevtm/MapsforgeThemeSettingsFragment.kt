// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.unifiedmap.mapsforgevtm

import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings

import android.app.Activity
import android.os.Bundle

import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

import java.util.Locale
import java.util.Map

import org.oscim.theme.XmlRenderThemeStyleLayer
import org.oscim.theme.XmlRenderThemeStyleMenu

class MapsforgeThemeSettingsFragment : PreferenceFragmentCompat() {
    public static val RENDERTHEME_MENU: String = "renderthememenu"
    public static val SHOW3DOPTION: String = "show3Doption"

    ListPreference baseLayerPreference

    XmlRenderThemeStyleMenu renderthemeOptions
    PreferenceCategory renderthemeMenu
    Boolean show3Doption = false

    override     public Unit onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        setPreferencesFromResource(R.xml.theme_prefs, rootKey)

        // if the render theme has a style menu, its data is delivered via the intent
        renderthemeOptions = (XmlRenderThemeStyleMenu) requireActivity().getIntent().getSerializableExtra(RENDERTHEME_MENU)
        // show 3D option for building layer
        show3Doption = requireActivity().getIntent().getBooleanExtra(SHOW3DOPTION, false)
        // the preference category serves as the hook to add a list preference to allow users to select a style
        this.renderthemeMenu = findPreference(getString(R.string.pref_theme_menu))
        createRenderthemeMenu()
    }

    override     public Unit onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.settings_title_map_style)
    }

    private Unit createRenderthemeMenu() {
        val activity: Activity = requireActivity()

        this.renderthemeMenu.removeAll()

        final String themeStylePrefKey
        if (renderthemeOptions != null) {
            val themePrefKey: String = createThemePreferences(activity)
            themeStylePrefKey = themePrefKey + "-" + PreferenceManager.getDefaultSharedPreferences(activity).getString(themePrefKey, "none")
        } else {
            themeStylePrefKey = Settings.RENDERTHEMESCALE_DEFAULTKEY
        }

        if (Settings.isDefaultMapRenderTheme()) {
            this.renderthemeMenu.addPreference(VtmThemes.getPreference(activity))
        }

        if (show3Doption) {
            val cb3D: CheckBoxPreference = CheckBoxPreference(activity)
            cb3D.setKey(activity.getString(R.string.pref_buildingLayer3D))
            cb3D.setTitle(R.string.maptheme_show3Dbuildings)
            cb3D.setChecked(Settings.getBuildings3D())
            cb3D.setIconSpaceReserved(false)
            this.renderthemeMenu.addPreference(cb3D)
        }

    }

    private String createThemePreferences(final Activity activity) {
        this.baseLayerPreference = ListPreference(activity)
        baseLayerPreference.setTitle(R.string.settings_title_map_style)

        // the id of the setting is the id of the stylemenu, that allows this
        // app to store different settings for different render themes.
        val themePrefKey: String = this.renderthemeOptions.getId()
        baseLayerPreference.setKey(themePrefKey)


        // this is the user language for the app, in 'en', 'de' etc format
        // no dialects are supported at the moment
        val language: String = Locale.getDefault().getLanguage()

        // build data structure for the ListPreference
        val baseLayers: Map<String, XmlRenderThemeStyleLayer> = renderthemeOptions.getLayers()

        Int visibleStyles = 0
        for (final XmlRenderThemeStyleLayer baseLayer : baseLayers.values()) {
            if (baseLayer.isVisible()) {
                ++visibleStyles
            }
        }

        final CharSequence[] entries = CharSequence[visibleStyles]
        final CharSequence[] values = CharSequence[visibleStyles]
        Int i = 0
        for (final XmlRenderThemeStyleLayer baseLayer : baseLayers.values()) {
            if (baseLayer.isVisible()) {
                // build up the entries in the list
                entries[i] = baseLayer.getTitle(language)
                values[i] = baseLayer.getId()
                ++i
            }
        }

        baseLayerPreference.setEntries(entries)
        baseLayerPreference.setEntryValues(values)
        baseLayerPreference.setEnabled(true)
        baseLayerPreference.setPersistent(true)
        baseLayerPreference.setDefaultValue(renderthemeOptions.getDefaultValue())
        baseLayerPreference.setIconSpaceReserved(false)
        baseLayerPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            // need to persist the value before recreating the renderthemeMenu
            Settings.setSelectedMapRenderThemeStyle(preference.getKey(), newValue.toString())
            createRenderthemeMenu()
            return true
        })
        renderthemeMenu.addPreference(baseLayerPreference)

        // additional theme info
        /* @todo
        if (UnifiedThemeHelper.getRenderThemeType() == UnifiedThemeHelper.RenderThemeType.RTT_ELEVATE) {
            val info: Preference = Preference(activity)
            info.setSummary(R.string.maptheme_elevate_categoryinfo)
            info.setIconSpaceReserved(false)
            renderthemeMenu.addPreference(info)
        }
        */

        String selection = baseLayerPreference.getValue()
        // need to check that the selection stored is actually a valid getLayer in the current rendertheme.
        if (selection == null || !renderthemeOptions.getLayers().containsKey(selection)) {
            selection = renderthemeOptions.getLayer(renderthemeOptions.getDefaultValue()).getId()
        }
        // the Android style is to display information here, not instruction
        baseLayerPreference.setSummary(renderthemeOptions.getLayer(selection).getTitle(language))

        for (final XmlRenderThemeStyleLayer overlay : this.renderthemeOptions.getLayer(selection).getOverlays()) {
            val checkbox: CheckBoxPreference = CheckBoxPreference(activity)
            checkbox.setKey(overlay.getId())
            checkbox.setPersistent(true)
            checkbox.setTitle(overlay.getTitle(language))
            if (findPreference(overlay.getId()) == null) {
                // value has never been set, so set from default
                checkbox.setChecked(overlay.isEnabled())
            }
            checkbox.setIconSpaceReserved(false)
            this.renderthemeMenu.addPreference(checkbox)
        }
        return themePrefKey
    }

}
