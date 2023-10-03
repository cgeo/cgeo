package cgeo.geocaching.utils.config;

import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@Deprecated
public class LegacyFilterConfig extends HashMap<String, List<String>> {

    private static final String CONFIG_KEY_ADV_MODE = "advanced";
    private static final String CONFIG_KEY_INCLUDE_INCLUSIVE = "inconclusive";


    private static final LegacyFilterConfigParser<IGeocacheFilter> FILTER_PARSER = new LegacyFilterConfigParser<IGeocacheFilter>(true)
            .register(AndGeocacheFilter::new)
            .register(OrGeocacheFilter::new)
            .register(NotGeocacheFilter::new);

    static {
        for (GeocacheFilterType gcf : GeocacheFilterType.values()) {
            FILTER_PARSER.register(gcf::create);
        }
    }
    
    public static void checkAndMigrate() {
        if (Settings.checkAndSetLegacyFilterConfigMigrated()) {
            return;
        }

        //Migrate all stored filters
        for (GeocacheFilter filter : GeocacheFilter.Storage.getStoredFilters()) {
            GeocacheFilter.Storage.save(filter);
        }

        //Migrate context filters
        migrateFilterContext(new GeocacheFilterContext(GeocacheFilterContext.FilterType.LIVE));
        migrateFilterContext(new GeocacheFilterContext(GeocacheFilterContext.FilterType.OFFLINE));
    }

    private static void migrateFilterContext(final GeocacheFilterContext ctx) {
        ctx.set(ctx.get());
    }

    @Deprecated
    public static GeocacheFilter parseLegacy(final String pName, final String pFilterConfig, final boolean throwOnParseError) throws ParseException {

        final String filterConfig = pFilterConfig == null ? "" : pFilterConfig;
        String name = pName;
        IGeocacheFilter tree = null;
        boolean openInAdvancedMode = false;
        boolean includeInconclusive = false;

        //See if config contains info beside the filter expression itself
        int idx = 0;
        if (filterConfig.startsWith("[")) {
            final LegacyFilterConfig config = new LegacyFilterConfig();
            idx = LegacyFilterConfigParser.parseConfiguration(filterConfig, 1, config) + 1;
            if (name == null) {
                name = config.getDefaultList().isEmpty() ? "" : config.getDefaultList().get(0);
            }
            openInAdvancedMode = config.getFirstValue(CONFIG_KEY_ADV_MODE, false, BooleanUtils::toBoolean);
            includeInconclusive = config.getFirstValue(CONFIG_KEY_INCLUDE_INCLUSIVE, false, BooleanUtils::toBoolean);
        }

        final String treeConfig = filterConfig.substring(Math.min(idx, filterConfig.length()));
        if (!StringUtils.isBlank(treeConfig)) {
            try {
                tree = FILTER_PARSER.create(treeConfig);
            } catch (ParseException pe) {
                if (throwOnParseError) {
                    throw pe;
                }
                Log.w("Couldn't parse expression '" + filterConfig + "' (idx: " + idx + ")", pe);
            }
        }
        return GeocacheFilter.create(name, openInAdvancedMode, includeInconclusive, tree);
    }

    @Deprecated
    public static String toLegacyConfig(final GeocacheFilter filter) {
        if (filter == null) {
            return null;
        }
        final LegacyFilterConfig config = new LegacyFilterConfig();
        config.addToDefaultList(filter.getName());
        config.putList(CONFIG_KEY_ADV_MODE, BooleanUtils.toStringTrueFalse(filter.isOpenInAdvancedMode()));
        config.putList(CONFIG_KEY_INCLUDE_INCLUSIVE, BooleanUtils.toStringTrueFalse(filter.isIncludeInconclusive()));
        return "[" + LegacyFilterConfigParser.toConfig(config) + "]" + (filter.getTree() == null ? "" : FILTER_PARSER.getConfig(filter.getTree()));
    }

    /**
     * IF this config has only one single String value then this value is returned. Otherwise null is returned
     */
    public String getSingleValue() {
        final List<String> defaultList = getDefaultList();
        return size() == 1 && defaultList.size() == 1 ? defaultList.get(0) : null;
    }

    public <T> T getFirstValue(final String key, final T defaultValue, final Func1<String, T> converter) {
        final List<String> values = get(key);
        return values != null && !values.isEmpty() ? converter.call(values.get(0)) : defaultValue;
    }

    public void putList(final String key, final String... values) {
        put(key, new ArrayList<>(Arrays.asList(values)));
    }

    @NonNull
    public List<String> getDefaultList() {
        final List<String> result = get(null);
        return result == null ? Collections.emptyList() : result;
    }

    public LegacyFilterConfig addToDefaultList(final String... values) {
        if (get(null) == null) {
            put(null, new ArrayList<>());
        }
        get(null).addAll(Arrays.asList(values));
        return this;
    }

    public void putDefaultList(final List<String> list) {
        put(null, list);
    }

    /**
     * creates a new config object, taking the content of one key of this config and putting it into the default list
     */
    public LegacyFilterConfig getSubConfig(final String key) {
        final LegacyFilterConfig subConfig = new LegacyFilterConfig();
        subConfig.putDefaultList(this.get(key));
        return subConfig;
    }
}
