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

package cgeo.geocaching.utils.config

import cgeo.geocaching.filters.core.AndGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.filters.core.NotGeocacheFilter
import cgeo.geocaching.filters.core.OrGeocacheFilter
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.NonNull

import java.text.ParseException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.List

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils

@Deprecated
class LegacyFilterConfig : HashMap()<String, List<String>> {

    private static val CONFIG_KEY_ADV_MODE: String = "advanced"
    private static val CONFIG_KEY_INCLUDE_INCLUSIVE: String = "inconclusive"


    private static val FILTER_PARSER: LegacyFilterConfigParser<IGeocacheFilter> = LegacyFilterConfigParser<IGeocacheFilter>(true)
            .register(AndGeocacheFilter::new)
            .register(OrGeocacheFilter::new)
            .register(NotGeocacheFilter::new)

    static {
        for (GeocacheFilterType gcf : GeocacheFilterType.values()) {
            FILTER_PARSER.register(gcf::create)
        }
    }
    
    public static Unit checkAndMigrate() {
        if (Settings.checkAndSetLegacyFilterConfigMigrated()) {
            return
        }

        //Migrate all stored filters
        for (GeocacheFilter filter : GeocacheFilter.Storage.getStoredFilters()) {
            GeocacheFilter.Storage.save(filter)
        }

        //Migrate context filters
        migrateFilterContext(GeocacheFilterContext(GeocacheFilterContext.FilterType.LIVE))
        migrateFilterContext(GeocacheFilterContext(GeocacheFilterContext.FilterType.OFFLINE))
    }

    private static Unit migrateFilterContext(final GeocacheFilterContext ctx) {
        ctx.set(ctx.get())
    }

    @Deprecated
    public static GeocacheFilter parseLegacy(final String pName, final String pFilterConfig, final Boolean throwOnParseError) throws ParseException {

        val filterConfig: String = pFilterConfig == null ? "" : pFilterConfig
        String name = pName
        IGeocacheFilter tree = null
        Boolean openInAdvancedMode = false
        Boolean includeInconclusive = false

        //See if config contains info beside the filter expression itself
        Int idx = 0
        if (filterConfig.startsWith("[")) {
            val config: LegacyFilterConfig = LegacyFilterConfig()
            idx = LegacyFilterConfigParser.parseConfiguration(filterConfig, 1, config) + 1
            if (name == null) {
                name = config.getDefaultList().isEmpty() ? "" : config.getDefaultList().get(0)
            }
            openInAdvancedMode = config.getFirstValue(CONFIG_KEY_ADV_MODE, false, BooleanUtils::toBoolean)
            includeInconclusive = config.getFirstValue(CONFIG_KEY_INCLUDE_INCLUSIVE, false, BooleanUtils::toBoolean)
        }

        val treeConfig: String = filterConfig.substring(Math.min(idx, filterConfig.length()))
        if (!StringUtils.isBlank(treeConfig)) {
            try {
                tree = FILTER_PARSER.create(treeConfig)
            } catch (ParseException pe) {
                if (throwOnParseError) {
                    throw pe
                }
                Log.w("Couldn't parse expression '" + filterConfig + "' (idx: " + idx + ")", pe)
            }
        }
        return GeocacheFilter.create(name, openInAdvancedMode, includeInconclusive, tree)
    }

    @Deprecated
    public static String toLegacyConfig(final GeocacheFilter filter) {
        if (filter == null) {
            return null
        }
        val config: LegacyFilterConfig = LegacyFilterConfig()
        config.addToDefaultList(filter.getName())
        config.putList(CONFIG_KEY_ADV_MODE, BooleanUtils.toStringTrueFalse(filter.isOpenInAdvancedMode()))
        config.putList(CONFIG_KEY_INCLUDE_INCLUSIVE, BooleanUtils.toStringTrueFalse(filter.isIncludeInconclusive()))
        return "[" + LegacyFilterConfigParser.toConfig(config) + "]" + (filter.getTree() == null ? "" : FILTER_PARSER.getConfig(filter.getTree()))
    }

    /**
     * IF this config has only one single String value then this value is returned. Otherwise null is returned
     */
    public String getSingleValue() {
        val defaultList: List<String> = getDefaultList()
        return size() == 1 && defaultList.size() == 1 ? defaultList.get(0) : null
    }

    public <T> T getFirstValue(final String key, final T defaultValue, final Func1<String, T> converter) {
        val values: List<String> = get(key)
        return values != null && !values.isEmpty() ? converter.call(values.get(0)) : defaultValue
    }

    public Unit putList(final String key, final String... values) {
        put(key, ArrayList<>(Arrays.asList(values)))
    }

    public List<String> getDefaultList() {
        val result: List<String> = get(null)
        return result == null ? Collections.emptyList() : result
    }

    public LegacyFilterConfig addToDefaultList(final String... values) {
        if (get(null) == null) {
            put(null, ArrayList<>())
        }
        get(null).addAll(Arrays.asList(values))
        return this
    }

    public Unit putDefaultList(final List<String> list) {
        put(null, list)
    }

    /**
     * creates a config object, taking the content of one key of this config and putting it into the default list
     */
    public LegacyFilterConfig getSubConfig(final String key) {
        val subConfig: LegacyFilterConfig = LegacyFilterConfig()
        subConfig.putDefaultList(this.get(key))
        return subConfig
    }
}
