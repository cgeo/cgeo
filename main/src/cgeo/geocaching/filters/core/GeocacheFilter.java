package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.expressions.ExpressionConfig;
import cgeo.geocaching.utils.expressions.ExpressionParser;
import cgeo.geocaching.utils.functions.Action1;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import static java.lang.Boolean.TRUE;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class GeocacheFilter implements Cloneable {

    public enum QuickFilter {
        FOUND, OWNED, DISABLED, ARCHIVED, HAS_OFFLINE_FOUND_LOG
    }

    private static final String CONFIG_KEY_ADV_MODE = "advanced";
    private static final String CONFIG_KEY_INCLUDE_INCLUSIVE = "inconclusive";

    private static final ExpressionParser<IGeocacheFilter> FILTER_PARSER = new ExpressionParser<IGeocacheFilter>(true)
            .register(AndGeocacheFilter::new)
            .register(OrGeocacheFilter::new)
            .register(NotGeocacheFilter::new);

    static {
        for (GeocacheFilterType gcf : GeocacheFilterType.values()) {
            FILTER_PARSER.register(gcf::create);
        }
    }

    private String name;
    private IGeocacheFilter tree;

    private final boolean openInAdvancedMode;
    private final boolean includeInconclusive;

    public static class Storage {

        private static final SortedMap<String, GeocacheFilter> storedFilters = new TreeMap<>(TextUtils.COLLATOR::compare);
        private static boolean isInitialized = false;

        public static synchronized Collection<GeocacheFilter> getStoredFilters() {
            ensureInit();
            return storedFilters.values();
        }

        public static synchronized boolean exists(final String filtername) {
            ensureInit();
            return storedFilters.containsKey(filtername);
        }

        public static synchronized boolean existsAndDiffers(final String newName, final GeocacheFilter filter) {
            ensureInit();
            final GeocacheFilter other = storedFilters.get(newName);
            return other != null &&
                    (!Objects.equals(other.getTreeConfig(), filter.getTreeConfig()) ||
                            filter.isIncludeInconclusive() != other.isIncludeInconclusive());
        }

        public static synchronized void save(final GeocacheFilter filter) {
            ensureInit();
            DataStore.DBFilters.save(filter);
            storedFilters.put(filter.name, filter);
        }

        public static synchronized void delete(final GeocacheFilter filter) {
            ensureInit();
            DataStore.DBFilters.delete(filter.name);
            storedFilters.remove(filter.name);
        }

        private static void ensureInit() {
            if (!isInitialized) {
                for (GeocacheFilter filter : DataStore.DBFilters.getAllStoredFilters()) {
                    storedFilters.put(filter.name, filter);
                }
                isInitialized = true;
            }
        }
    }

    private GeocacheFilter(final String name, final boolean openInAdvancedMode, final boolean includeInconclusive, final IGeocacheFilter tree) {
        this.name = name;
        this.tree = tree;
        this.openInAdvancedMode = openInAdvancedMode;
        this.includeInconclusive = includeInconclusive;
    }

    @NonNull
    public String getName() {
        return name == null ? "" : name;
    }

    public boolean isOpenInAdvancedMode() {
        return openInAdvancedMode;
    }

    public boolean isIncludeInconclusive() {
        return includeInconclusive;
    }

    public IGeocacheFilter getTree() {
        return tree;
    }

    public String getTreeConfig() {
        return tree == null ? null : FILTER_PARSER.getConfig(tree);
    }

    @NonNull
    public static GeocacheFilter create(final String name, final boolean openInAdvancedMode, final boolean includeInconclusive, final IGeocacheFilter tree) {
        return new GeocacheFilter(name, openInAdvancedMode, includeInconclusive, tree);
    }

    @NonNull
    public static GeocacheFilter createEmpty() {
        return createEmpty(false);
    }

    @NonNull
    public static GeocacheFilter createEmpty(final boolean openInAdvancedMode) {
        return new GeocacheFilter(null, openInAdvancedMode, false, null);
    }

    @NonNull
    public static GeocacheFilter createFromConfig(final String filterConfig) {
        return createFromConfig(null, filterConfig);
    }

    @NonNull
    public static GeocacheFilter createFromConfig(final String pName, final String filterConfig) {
        try {
            return createInternal(pName, filterConfig, false);
        } catch (ParseException e) {
            //will never happen
            return createEmpty();
        }
    }

    public static GeocacheFilter checkConfig(final String filterConfig) throws ParseException {
        return createInternal(null, filterConfig, true);
    }

    private static GeocacheFilter createInternal(final String pName, final String pFilterConfig, final boolean throwOnParseError) throws ParseException {

        final String filterConfig = pFilterConfig == null ? "" : pFilterConfig;
        String name = pName;
        IGeocacheFilter tree = null;
        boolean openInAdvancedMode = false;
        boolean includeInconclusive = false;

        //See if config contains info beside the filter expression itself
        int idx = 0;
        if (filterConfig.startsWith("[")) {
            final ExpressionConfig config = new ExpressionConfig();
            idx = ExpressionParser.parseConfiguration(filterConfig, 1, config) + 1;
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
        return new GeocacheFilter(name, openInAdvancedMode, includeInconclusive, tree);
    }

    public boolean isFiltering() {
        return tree != null && tree.isFiltering();
    }

    public String toConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        config.addToDefaultList(getName());
        config.putList(CONFIG_KEY_ADV_MODE, BooleanUtils.toStringTrueFalse(isOpenInAdvancedMode()));
        config.putList(CONFIG_KEY_INCLUDE_INCLUSIVE, BooleanUtils.toStringTrueFalse(isIncludeInconclusive()));
        return "[" + ExpressionParser.toConfig(config) + "]" + (tree == null ? "" : FILTER_PARSER.getConfig(tree));
    }

    @NonNull
    @Override
    public String toString() {
        return toConfig();
    }

    public boolean filter(final Geocache cache) {
        if (tree == null) {
            return true;
        }
        final Boolean result = tree.filter(cache);
        return result == null ? this.includeInconclusive : result;
    }

    public void filterList(final Collection<Geocache> list) {

        final List<Geocache> itemsToKeep = new ArrayList<>();
        for (final Geocache item : list) {
            if (filter(item)) {
                itemsToKeep.add(item);
            }
        }

        list.clear();
        //note that since both "list" and "itemsToKeep" are ArrayLists, the addAll-operation is very fast (two arraycopies of the references)
        list.addAll(itemsToKeep);
    }

    @Override
    @NonNull
    public GeocacheFilter clone() {
        return createFromConfig(this.toConfig());
    }

    /**
     * modifies this GeocacheFilter by adding the given AND conditions to it.
     * New filters are added BEFORE existing one (assuming that they have priority in case it is necessary to decide to filter one or the other)
     * returns this for convenience
     */
    public GeocacheFilter and(final IGeocacheFilter... filters) {

        if (filters == null || filters.length == 0) {
            return this;
        }

        if (this.tree == null && filters.length == 1) {
            this.tree = filters[0];
            this.name = null;
            return this;
        }

        final AndGeocacheFilter andFilter = new AndGeocacheFilter();
        for (IGeocacheFilter f : filters) {
            andFilter.addChild(f);
        }
        if (this.tree != null) {
            if (isAndFilter(this.tree)) {
                for (IGeocacheFilter andChild : this.tree.getChildren()) {
                    andFilter.addChild(andChild);
                }
            } else {
                andFilter.addChild(this.tree);
            }
        }
        this.name = null;
        this.tree = andFilter;
        return this;
    }

    public boolean isSaved() {
        return Storage.exists(getName());
    }

    public boolean isSavedDifferently() {
        return Storage.existsAndDiffers(getName(), this);
    }

    public String getNameForUserDisplay() {
        return getName() + (isSavedDifferently() ? "*" : "");
    }

    /**
     * Helper method to map filter to search providers only offering AND filter capability.
     * * If this filter is a base filter, this base filter is returned
     * * If this is an AND filter, extract the "AND" chain of Base filters.
     * * Otherwise return an empty list
     */
    public List<BaseGeocacheFilter> getAndChainIfPossible() {
        final List<BaseGeocacheFilter> result = new ArrayList<>();
        getAndChainIfPossibleInternal(this.getTree(), result);
        return result;
    }

    /**
     * Helper method to be used in conjunction with {@link #getAndChainIfPossible()} by search providers
     * only offering SPECIFIC filter capabilities. This method searches and returns specific base filters contained in a given filter list
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseGeocacheFilter> T findInChain(final List<BaseGeocacheFilter> filters, final Class<T> filterClazz) {
        for (BaseGeocacheFilter filter : filters) {
            if (filterClazz.isAssignableFrom(filter.getClass())) {
                return (T) filter;
            }
        }
        return null;
    }

    private void getAndChainIfPossibleInternal(final IGeocacheFilter filterToCheck, final List<BaseGeocacheFilter> chain) {

        if (isAndFilter(filterToCheck)) {
            for (IGeocacheFilter fChild : filterToCheck.getChildren()) {
                getAndChainIfPossibleInternal(fChild, chain);
            }
        } else if (filterToCheck instanceof BaseGeocacheFilter) {
            chain.add((BaseGeocacheFilter) filterToCheck);
        }
    }

    /**
     * Extracts quickfilter settings from this filter. For each quickfilter, an entry is returned
     * For each quickfilter, it is stored whether corresponding caches shall be shown (true) or not (false)
     */
    public Map<QuickFilter, Boolean> getQuickFilter() {
        final Map<QuickFilter, Boolean> result = new HashMap<>();
        final StatusGeocacheFilter statusFilter = findInChain(getAndChainIfPossible(), StatusGeocacheFilter.class);
        result.put(QuickFilter.FOUND, statusFilter == null || !Boolean.FALSE.equals(statusFilter.getStatusFound()));
        result.put(QuickFilter.OWNED, statusFilter == null || !Boolean.FALSE.equals(statusFilter.getStatusOwned()));
        result.put(QuickFilter.HAS_OFFLINE_FOUND_LOG, statusFilter == null || !Boolean.FALSE.equals(statusFilter.getStatusHasOfflineFoundLog()));
        result.put(QuickFilter.DISABLED, statusFilter == null || !statusFilter.isExcludeDisabled());
        result.put(QuickFilter.ARCHIVED, statusFilter == null || !statusFilter.isExcludeArchived());
        return result;
    }

    public boolean hasSameQuickFilter(final Map<QuickFilter, Boolean> newQuickFilter) {
        final Map<QuickFilter, Boolean> quickFilter = getQuickFilter();
        return quickFilter.equals(newQuickFilter);
    }

    public boolean canSetQuickFilterLossless() {
        return getTree() == null || getTree() instanceof BaseGeocacheFilter || isAndFilter(getTree());
    }

    public void setQuickFilterLossless(final Map<QuickFilter, Boolean> newQuickFilter) {
        if (!canSetQuickFilterLossless() || hasSameQuickFilter(newQuickFilter)) {
            return;
        }

        StatusGeocacheFilter statusFilter = findInChain(getAndChainIfPossible(), StatusGeocacheFilter.class);
        if (statusFilter == null) {
            statusFilter = GeocacheFilterType.STATUS.create();
            and(statusFilter);
        }
        final Map<QuickFilter, Boolean> quickFilter = getQuickFilter();
        final StatusGeocacheFilter sFilter = statusFilter;
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.FOUND, f -> sFilter.setStatusFound(f ? null : false));
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.OWNED, f -> sFilter.setStatusOwned(f ? null : false));
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.HAS_OFFLINE_FOUND_LOG, f -> sFilter.setStatusHasOfflineFoundLog(f ? null : false));
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.DISABLED, f -> sFilter.setExcludeDisabled(!f));
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.ARCHIVED, f -> sFilter.setExcludeArchived(!f));
    }

    private void setSingleQuickFilter(final Map<QuickFilter, Boolean> currentFilter, final Map<QuickFilter, Boolean> newFilter, final QuickFilter qf, final Action1<Boolean> setter) {
        final boolean currValue = TRUE.equals(currentFilter.get(qf));
        final boolean newValue = TRUE.equals(newFilter.get(qf));
        if (currValue == newValue) {
            return;
        }
        setter.call(newValue);
    }

    private static boolean isAndFilter(final IGeocacheFilter filter) {
        return filter instanceof AndGeocacheFilter && !(filter instanceof NotGeocacheFilter);
    }


    public String toUserDisplayableString() {
        if (!StringUtils.isBlank(getName())) {
            return getNameForUserDisplay();
        }

        if (getTree() == null) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        final String udsTree = getTree().toUserDisplayableString(0);
        if (udsTree == null) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        return udsTree;
    }

}
