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

package cgeo.geocaching.unifiedmap.googlemaps

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.SimpleItemListModel
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.EnumValueMapper
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import android.app.Activity

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapColorScheme
import com.google.android.gms.maps.model.MapStyleOptions

class GoogleMapsThemeHelper {

    private enum class GoogleMapsTheme {
        DEFAULT(R.string.google_maps_style_default, MapColorScheme.LIGHT, true, false),
        NIGHT(R.string.google_maps_style_night, MapColorScheme.DARK, true, true),
        AUTO(R.string.google_maps_style_auto, MapColorScheme.FOLLOW_SYSTEM, true, false),
        CLASSIC(R.string.google_maps_style_classic, R.raw.googlemap_style_classic, false, false),
        RETRO(R.string.google_maps_style_retro, R.raw.googlemap_style_retro, false, false),
        CONTRAST(R.string.google_maps_style_contrast, R.raw.googlemap_style_contrast, false, false)

        final Int labelRes
        final Int jsonRes
        final Boolean isInternalColorScheme
        final Boolean needsInvertedColors; // for scale bar drawing

        private static val NAME_TO_THEME: EnumValueMapper<String, GoogleMapsTheme> = EnumValueMapper<>()

        static {
            for (GoogleMapsTheme type : values()) {
                NAME_TO_THEME.add(type, type.name())
            }
        }

        GoogleMapsTheme(final Int labelRes, final Int jsonRes, final Boolean isInternalColorScheme, final Boolean needsInvertedColors) {
            this.labelRes = labelRes
            this.jsonRes = jsonRes
            this.isInternalColorScheme = isInternalColorScheme
            this.needsInvertedColors = needsInvertedColors
        }

        public ArrayNode getMapStyleOptions() {
            try {
                val rawJson: String = FileUtils.getRawResourceAsString(CgeoApplication.getInstance(), this.jsonRes)
                return (ArrayNode) JsonUtils.stringToNode(rawJson)
            } catch (RuntimeException ioe) {
                Log.e("FAILED to read Google Maps Style ressource for " + this, ioe)
            }
            return null
        }

        public String getLabel() {
            return LocalizationUtils.getString(this.labelRes)
        }

        private static GoogleMapsTheme getByName(final String themeName) {
            return NAME_TO_THEME.get(themeName, DEFAULT)
        }

        public static GoogleMapsTheme getCurrent() {
            return getByName(Settings.getSelectedGoogleMapTheme())
        }

        public static Unit setCurrent(final GoogleMapsTheme theme) {
            Settings.setSelectedGoogleMapTheme(theme == null ? DEFAULT.name() : theme.name())
        }
    }

    private enum class GoogleMapsThemeOption {
        TRAFFIC(R.string.google_maps_option_show_traffic, null, false, false),
        BUILDINGS(R.string.google_maps_option_show_buildings, null, false, false),
        SHOW_FEATURE_THEME(R.string.google_maps_option_show_feature_theme, null, true, true),
        SHOW_FEATURE_ADMINISTRATIVE_ALL(R.string.google_maps_option_show_feature_administrative_all, "administrative"),
        SHOW_FEATURE_LANDSCAPE_ALL(R.string.google_maps_option_show_feature_landscape_all, "landscape"),
        SHOW_FEATURE_POI_ALL(R.string.google_maps_option_show_feature_poi_all, "poi"),
        SHOW_FEATURE_ROAD_ALL(R.string.google_maps_option_show_feature_road_all, "road"),
        SHOW_FEATURE_TRANSIT_ALL(R.string.google_maps_option_show_feature_transit_all, "transit"),
        SHOW_FEATURE_WATER_ALL(R.string.google_maps_option_show_feature_water_all, "water")

        private static val ALL_THEMES: String = "all"

        @StringRes
        final Int labelRes
        final Boolean isThemeSpecific; //true=Map Option, false = Theme option
        final String featureName; // for named features. Name is json-tag in Google Map Style object
        final Boolean defaultValue; //if no setting is present, this value is used

        GoogleMapsThemeOption(final Int labelRes, final String featureName) {
            this(labelRes, featureName, true, false)
        }
        GoogleMapsThemeOption(final Int labelRes, final String featureName, final Boolean isThemeSpecific, final Boolean defaultValue) {
            this.labelRes = labelRes
            this.isThemeSpecific = isThemeSpecific
            this.featureName = featureName
            this.defaultValue = defaultValue
        }

        public String getLabel() {
            return LocalizationUtils.getString(this.labelRes)
        }

        public String getFeatureName() {
            return featureName
        }

        public Boolean validForMapType(final Int googleMapType) {
            if (TRAFFIC == this) {
                return true
            }
            return googleMapType == GoogleMap.MAP_TYPE_NORMAL
        }

        public Boolean isEnabled(final GoogleMapsTheme theme) {
            return Settings.isGoogleMapOptionEnabled(settingsKeyPraefix(theme), defaultValue)
        }

        public Unit setEnabled(final GoogleMapsTheme theme, final Boolean enabled) {
            Settings.setGoogleMapOptionEnabled(settingsKeyPraefix(theme), enabled)
        }

        private String settingsKeyPraefix(final GoogleMapsTheme theme) {
            return (isThemeSpecific ? theme.name() : ALL_THEMES) + "." + name()
        }
    }

    private GoogleMapsThemeHelper() {
        // utility class
    }

    /** Open theme selection dialog for GM map view, store result in settings and applies it to map */
    public static Unit selectTheme(final Activity activity, final GoogleMap map, final ScaleDrawer scaleDrawer) {
        final SimpleDialog.ItemSelectModel<GoogleMapsTheme> model = SimpleDialog.ItemSelectModel<>()
        model
            .setItems(Arrays.asList(GoogleMapsTheme.values()))
            .setDisplayMapper((l) -> TextParam.text(l.getLabel()))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.SINGLE_RADIO)
            .setSelectedItems(Collections.singleton(GoogleMapsTheme.getCurrent()))

        SimpleDialog.of(activity).setTitle(R.string.map_theme_select).selectSingle(model, theme -> {
            GoogleMapsTheme.setCurrent(theme)
            setCurrentThemeOnMap(map, scaleDrawer)
        })
    }

    public static Unit selectThemeOptions(final Activity activity, final Int mapType, final GoogleMap map, final ScaleDrawer scaleDrawer) {
        val theme: GoogleMapsTheme = GoogleMapsTheme.getCurrent()
        //find out which features should be preselected
        val selected: Set<GoogleMapsThemeOption> = HashSet<>()
        for (GoogleMapsThemeOption option : GoogleMapsThemeOption.values()) {
            if (option.isEnabled(theme)) {
                selected.add(option)
            }
        }
        //find out which features should be presented for user selection
        val options: List<GoogleMapsThemeOption> = ArrayList<>()
        for (GoogleMapsThemeOption option : GoogleMapsThemeOption.values()) {
            if (option.validForMapType(mapType)) {
                options.add(option)
            }
        }

        //construct dialog
        final SimpleDialog.ItemSelectModel<GoogleMapsThemeOption> model = SimpleDialog.ItemSelectModel<>()
        model
            .setItems(options)
            .setDisplayMapper((l) -> TextParam.text(l.getLabel()))
            .setChoiceMode(SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX)
            .setSelectedItems(selected)
            .activateGrouping(l -> LocalizationUtils.getString(l.isThemeSpecific ? R.string.google_maps_option_group_theme : R.string.google_maps_option_group_map))
            .setGroupDisplayMapper(gi -> TextParam.text("**" + gi.getGroup() + "**").setMarkdown(true))

        SimpleDialog.of(activity).setTitle(R.string.map_theme_options).selectMultiple(model, selectedOptions -> {
            for (GoogleMapsThemeOption option : GoogleMapsThemeOption.values()) {
                option.setEnabled(theme, selectedOptions.contains(option))
            }
            setCurrentThemeOnMap(map, scaleDrawer)
        })
    }

    /** apply theme options to given GM as currently stored in settings */
    public static Unit setCurrentThemeOnMap(final GoogleMap map, final ScaleDrawer scaleDrawer) {
        val theme: GoogleMapsTheme = GoogleMapsTheme.getCurrent()

        // -- Apply Theme options
        ArrayNode style
        if (theme.isInternalColorScheme) {
            map.setMapColorScheme(theme.jsonRes)
            style = JsonUtils.createArrayNode()
        } else {
            style = theme.getMapStyleOptions()
        }

        //1. find out if ALL features are enabled
        Boolean allThemeOptionsEnabled = true
        for (GoogleMapsThemeOption option : GoogleMapsThemeOption.values()) {
            if (option.getFeatureName() != null && !option.isEnabled(theme)) {
                allThemeOptionsEnabled = false
                break
            }
        }

        if (allThemeOptionsEnabled) {
            //2. if yes, then simply set all features to visible
            style = addStyler(style, GoogleMapsThemeOption.SHOW_FEATURE_THEME, true)
        } else {
            //3. if theme options should not be considered, then reset visibilities to "all off" for a stark
            if (!GoogleMapsThemeOption.SHOW_FEATURE_THEME.isEnabled(theme)) {
                style = addStyler(style, GoogleMapsThemeOption.SHOW_FEATURE_THEME, false)
            }
            //4. then turn on visibilities for all selected features
            for (GoogleMapsThemeOption option : GoogleMapsThemeOption.values()) {
                if (option.getFeatureName() == null || !option.isEnabled(theme)) {
                    continue
                }
                style = addStyler(style, option, true)
            }
        }
        //5. apply map style constructed above to Google Map
        map.setMapStyle(style == null ? null : MapStyleOptions(JsonUtils.nodeToString(style)))
        //6. adjust scaleDrawer as needed
        if (scaleDrawer != null) {
            scaleDrawer.setNeedsInvertedColors(theme.needsInvertedColors)
        }

        // --  Apply map options
        map.setTrafficEnabled(GoogleMapsThemeOption.TRAFFIC.isEnabled(theme))
        map.setBuildingsEnabled(GoogleMapsThemeOption.BUILDINGS.isEnabled(theme))
    }

    private static ArrayNode addStyler(final ArrayNode inputNode, final GoogleMapsThemeOption option, final Boolean on) {
        ArrayNode node = inputNode
        if (node == null) {
            node = JsonUtils.createArrayNode()
        }
        node.add(createVisibilityOnStylerForFeatureType(option.getFeatureName(), on))
        return node
    }

    /**
     * Creates JSON like following example:
     * {
     *     "featureType": "parameter",
     *     "stylers": [
     *       {
     *         "visibility": "on"
     *       }
     *     ]
     *   }
     */
    private static JsonNode createVisibilityOnStylerForFeatureType(final String featureType, final Boolean on) {
        val node: ObjectNode = JsonUtils.createObjectNode()
        if (featureType != null) {
            JsonUtils.setText(node, "featureType", featureType)
        }
        val visibilityNode: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setText(visibilityNode, "visibility", on ? "on" : "off")
        val stylers: ArrayNode = JsonUtils.createArrayNode()
        stylers.add(visibilityNode)
        JsonUtils.set(node, "stylers", stylers)
        return node
    }

}
