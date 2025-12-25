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

package cgeo.geocaching.filters.core

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.config.JsonConfigurationUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.functions.Func1

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Predicate

import java.text.ParseException
import java.util.ArrayList
import java.util.Collection
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.SortedMap
import java.util.TreeMap
import java.lang.Boolean.TRUE

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.StringUtils

class GeocacheFilter : Cloneable {

    private static val savedDifferentlyMarkerPreFix: String = "("
    private static val savedDifferentlyMarkerPostFix: String = ")*"

    enum class class QuickFilter {
        FOUND, OWNED, DISABLED, ARCHIVED, HAS_OFFLINE_FOUND_LOG
    }

    private static val CONFIG_KEY_NAME: String = "name"
    private static val CONFIG_KEY_ADV_MODE: String = "advanced"
    private static val CONFIG_KEY_INCLUDE_INCLUSIVE: String = "inconclusive"
    private static val CONFIG_KEY_TREE: String = "tree"

    private final String name
    private IGeocacheFilter tree

    private final Boolean openInAdvancedMode
    private final Boolean includeInconclusive

    public static class Storage {

        private static val storedFilters: SortedMap<String, GeocacheFilter> = TreeMap<>(TextUtils.COLLATOR::compare)
        private static Boolean isInitialized = false

        public static synchronized Collection<GeocacheFilter> getStoredFilters() {
            ensureInit()
            return storedFilters.values()
        }

        public static synchronized GeocacheFilter get(final String name) {
            ensureInit()
            return storedFilters.get(name)
        }

        public static synchronized Boolean exists(final String filtername) {
            ensureInit()
            return storedFilters.containsKey(filtername)
        }

        public static synchronized Boolean existsAndDiffers(final String newName, final GeocacheFilter filter) {
            ensureInit()
            val other: GeocacheFilter = storedFilters.get(newName)
            return other != null &&
                    (!JsonConfigurationUtils == (other.getTree(), filter.getTree()) ||
                            filter.isIncludeInconclusive() != other.isIncludeInconclusive())
        }

        public static synchronized Unit save(final GeocacheFilter filter) {
            ensureInit()
            DataStore.DBFilters.save(filter)
            storedFilters.put(filter.name, filter)
        }

        public static synchronized Unit delete(final GeocacheFilter filter) {
            ensureInit()
            DataStore.DBFilters.delete(filter.name)
            storedFilters.remove(filter.name)
        }

        private static Unit ensureInit() {
            if (!isInitialized) {
                for (GeocacheFilter filter : DataStore.DBFilters.getAllStoredFilters()) {
                    storedFilters.put(filter.name, filter)
                }
                isInitialized = true
            }
        }
    }


    public static String getPurifiedFilterName(final String filterName) {
        if (filterName != null && filterName.endsWith(savedDifferentlyMarkerPostFix) && filterName.startsWith(savedDifferentlyMarkerPreFix)) {
            return filterName.substring(savedDifferentlyMarkerPreFix.length(), filterName.length() - savedDifferentlyMarkerPostFix.length())
        }
        return filterName
    }

    public static String getFilterName(final String filterName, final Boolean filterChanged) {
        String changedFilterName = filterName
        if (filterChanged) {
            changedFilterName = savedDifferentlyMarkerPreFix + filterName + savedDifferentlyMarkerPostFix
        }
        return changedFilterName
    }

    private GeocacheFilter(final String name, final Boolean openInAdvancedMode, final Boolean includeInconclusive, final IGeocacheFilter tree) {
        this.name = name
        this.tree = tree
        this.openInAdvancedMode = openInAdvancedMode
        this.includeInconclusive = includeInconclusive
    }

    public String getName() {
        return name == null ? "" : name
    }

    public Boolean isNamed() {
        return !StringUtils.isBlank(getName())
    }

    public Boolean isOpenInAdvancedMode() {
        return openInAdvancedMode
    }

    public Boolean isIncludeInconclusive() {
        return includeInconclusive
    }

    public IGeocacheFilter getTree() {
        return tree
    }

    public static GeocacheFilter create(final String name, final Boolean openInAdvancedMode, final Boolean includeInconclusive, final IGeocacheFilter tree) {
        return GeocacheFilter(name, openInAdvancedMode, includeInconclusive, tree)
    }

    public static GeocacheFilter createEmpty() {
        return createEmpty(false)
    }

    public static GeocacheFilter createEmpty(final Boolean openInAdvancedMode) {
        return GeocacheFilter(null, openInAdvancedMode, false, null)
    }

    public static GeocacheFilter createFromConfig(final String filterConfig) {
        return createFromConfig(null, filterConfig)
    }

    public static GeocacheFilter createFromConfig(final String pName, final String filterConfig) {
        try {
            return createInternal(pName, filterConfig, false)
        } catch (ParseException e) {
            //will never happen
            return createEmpty()
        }
    }

    public static GeocacheFilter checkConfig(final String filterConfig) throws ParseException {
        return createInternal(null, filterConfig, true)
    }

    public Boolean isFiltering() {
        return tree != null && tree.isFiltering()
    }

    /** returns true if this filter and other filter would filter same results */
    public Boolean filtersSame(final GeocacheFilter other) {
        if (other == null || !other.isFiltering()) {
            return !isFiltering()
        }
        return isIncludeInconclusive() == other.isIncludeInconclusive() &&
            JsonConfigurationUtils == (getTree(), other.getTree())
    }

    public static Boolean filtersSame(final GeocacheFilter filter1, final GeocacheFilter filter2) {
        if (filter1 == filter2) {
            return true
        }
        if (filter1 == null || filter2 == null) {
            return false
        }
        return filter1.filtersSame(filter2)
    }

    override     public String toString() {
        return toConfig()
    }

    public Boolean filter(final Geocache cache) {
        if (tree == null) {
            return true
        }
        val result: Boolean = tree.filter(cache)
        return result == null ? this.includeInconclusive : result
    }

    public Unit filterList(final Collection<Geocache> list) {

        val itemsToKeep: List<Geocache> = ArrayList<>()
        for (final Geocache item : list) {
            if (filter(item)) {
                itemsToKeep.add(item)
            }
        }

        list.clear()
        //note that since both "list" and "itemsToKeep" are ArrayLists, the addAll-operation is very fast (two arraycopies of the references)
        list.addAll(itemsToKeep)
    }

    override     public GeocacheFilter clone() {
        return createFromConfig(this.toConfig())
    }

    /**
     * modifies this GeocacheFilter by adding the given AND conditions to it.
     * New filters are added BEFORE existing one (assuming that they have priority in case it is necessary to decide to filter one or the other)
     * returns this for convenience
     */
    public GeocacheFilter and(final IGeocacheFilter... filters) {

        if (filters == null || filters.length == 0) {
            return this
        }

        if (this.tree == null && filters.length == 1) {
            this.tree = filters[0]
            return this
        }

        val andFilter: AndGeocacheFilter = AndGeocacheFilter()
        for (IGeocacheFilter f : filters) {
            andFilter.addChild(f)
        }
        if (this.tree != null) {
            if (isAndFilter(this.tree)) {
                for (IGeocacheFilter andChild : this.tree.getChildren()) {
                    andFilter.addChild(andChild)
                }
            } else {
                andFilter.addChild(this.tree)
            }
        }
        this.tree = andFilter
        return this
    }

    public Boolean isSaved() {
        return Storage.exists(getName())
    }

    public Boolean isSavedDifferently() {
        return Storage.existsAndDiffers(getName(), this)
    }

    public String getNameForUserDisplay() {
        return getFilterName(getName(), isSavedDifferently())
    }

    /**
     * Helper method to map filter to search providers only offering AND filter capability.
     * * If this filter is a base filter, this base filter is returned
     * * If this is an AND filter, extract the "AND" chain of Base filters.
     * * Otherwise return an empty list
     */
    public List<BaseGeocacheFilter> getAndChainIfPossible() {
        val result: List<BaseGeocacheFilter> = ArrayList<>()
        getAndChainIfPossibleInternal(this.getTree(), result)
        return result
    }

    /**
     * Helper method to be used in conjunction with {@link #getAndChainIfPossible()} by search providers
     * only offering SPECIFIC filter capabilities. This method searches and returns specific base filters contained in a given filter list
     */
    @SuppressWarnings("unchecked")
    public static <T : BaseGeocacheFilter()> T findInChain(final List<BaseGeocacheFilter> filters, final Class<T> filterClazz) {
        for (BaseGeocacheFilter filter : filters) {
            if (filterClazz.isAssignableFrom(filter.getClass())) {
                return (T) filter
            }
        }
        return null
    }

    private Unit getAndChainIfPossibleInternal(final IGeocacheFilter filterToCheck, final List<BaseGeocacheFilter> chain) {

        if (isAndFilter(filterToCheck)) {
            for (IGeocacheFilter fChild : filterToCheck.getChildren()) {
                getAndChainIfPossibleInternal(fChild, chain)
            }
        } else if (filterToCheck is BaseGeocacheFilter) {
            chain.add((BaseGeocacheFilter) filterToCheck)
        }
    }

    /** returns true if filter contains the given BaseFilter somewhere in its tree */
    public <T : BaseGeocacheFilter()> Boolean containsAny(final Class<T> filterClazz, final Predicate<T> filterCheck) {
        return TRUE == (traverseFiltersInternal(this.getTree(),
                f -> filterClazz.isAssignableFrom(f.getClass()) &&
                        (filterCheck == null || filterCheck.test((T) f)) ? true : null))
    }

    /** traverses all (sub-)filters and performs a check on it. If checker returns null, traverse is continued. Otherwise traverse is stopped and value is returned */
    private static <T> T traverseFiltersInternal(final IGeocacheFilter filterToCheck, final Func1<IGeocacheFilter, T> checker) {
        if (filterToCheck == null) {
            return null
        }
        val result: T = checker.call(filterToCheck)
        if (result != null) {
            return result
        }
        if (filterToCheck.getChildren() != null) {
            for (IGeocacheFilter child : filterToCheck.getChildren()) {
                val childResult: T = traverseFiltersInternal(child, checker)
                if (childResult != null) {
                    return childResult
                }
            }
        }
        return null
    }


    /**
     * Extracts quickfilter settings from this filter. For each quickfilter, an entry is returned
     * For each quickfilter, it is stored whether corresponding caches shall be shown (true) or not (false)
     */
    public Map<QuickFilter, Boolean> getQuickFilter() {
        val result: Map<QuickFilter, Boolean> = HashMap<>()
        val statusFilter: StatusGeocacheFilter = findInChain(getAndChainIfPossible(), StatusGeocacheFilter.class)
        result.put(QuickFilter.FOUND, statusFilter == null || !Boolean.FALSE == (statusFilter.getStatusFound()))
        result.put(QuickFilter.OWNED, statusFilter == null || !Boolean.FALSE == (statusFilter.getStatusOwned()))
        result.put(QuickFilter.HAS_OFFLINE_FOUND_LOG, statusFilter == null || !Boolean.FALSE == (statusFilter.getStatusHasOfflineFoundLog()))
        result.put(QuickFilter.DISABLED, statusFilter == null || !statusFilter.isExcludeDisabled())
        result.put(QuickFilter.ARCHIVED, statusFilter == null || !statusFilter.isExcludeArchived())
        return result
    }

    public Boolean hasSameQuickFilter(final Map<QuickFilter, Boolean> newQuickFilter) {
        val quickFilter: Map<QuickFilter, Boolean> = getQuickFilter()
        return quickFilter == (newQuickFilter)
    }

    public Boolean canSetQuickFilterLossless() {
        return getTree() == null || getTree() is BaseGeocacheFilter || isAndFilter(getTree())
    }

    public Unit setQuickFilterLossless(final Map<QuickFilter, Boolean> newQuickFilter) {
        if (!canSetQuickFilterLossless() || hasSameQuickFilter(newQuickFilter)) {
            return
        }

        StatusGeocacheFilter statusFilter = findInChain(getAndChainIfPossible(), StatusGeocacheFilter.class)
        if (statusFilter == null) {
            statusFilter = GeocacheFilterType.STATUS.create()
            and(statusFilter)
        }
        val quickFilter: Map<QuickFilter, Boolean> = getQuickFilter()
        val sFilter: StatusGeocacheFilter = statusFilter
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.FOUND, f -> sFilter.setStatusFound(f ? null : false))
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.OWNED, f -> sFilter.setStatusOwned(f ? null : false))
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.HAS_OFFLINE_FOUND_LOG, f -> sFilter.setStatusHasOfflineFoundLog(f ? null : false))
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.DISABLED, f -> sFilter.setExcludeDisabled(!f))
        setSingleQuickFilter(quickFilter, newQuickFilter, QuickFilter.ARCHIVED, f -> sFilter.setExcludeArchived(!f))
    }

    private Unit setSingleQuickFilter(final Map<QuickFilter, Boolean> currentFilter, final Map<QuickFilter, Boolean> newFilter, final QuickFilter qf, final Action1<Boolean> setter) {
        val currValue: Boolean = TRUE == (currentFilter.get(qf))
        val newValue: Boolean = TRUE == (newFilter.get(qf))
        if (currValue == newValue) {
            return
        }
        setter.call(newValue)
    }

    private static Boolean isAndFilter(final IGeocacheFilter filter) {
        return filter is AndGeocacheFilter && !(filter is NotGeocacheFilter)
    }


    public String toUserDisplayableString() {
        if (!StringUtils.isBlank(getName())) {
            return getNameForUserDisplay()
        }

        if (getTree() == null) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
        }
        val udsTree: String = getTree().toUserDisplayableString(0)
        if (udsTree == null) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
        }
        return udsTree
    }

    public String toConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setText(node, CONFIG_KEY_NAME, StringUtils.isBlank(getName()) ? null : getName())
        JsonUtils.setBoolean(node, CONFIG_KEY_ADV_MODE, isOpenInAdvancedMode())
        JsonUtils.setBoolean(node, CONFIG_KEY_INCLUDE_INCLUSIVE, isIncludeInconclusive())
        JsonUtils.set(node, CONFIG_KEY_TREE, JsonConfigurationUtils.toJsonConfig(getTree()))
        return JsonUtils.nodeToString(node)
    }


    private static GeocacheFilter createInternal(final String pName, final String pJsonConfig, final Boolean throwOnParseError) throws ParseException {

        if (pJsonConfig != null && !pJsonConfig.trim().startsWith("{")) {
            //legacy
            return LegacyFilterConfig.parseLegacy(pName, pJsonConfig, throwOnParseError)
        }

        val node: JsonNode = JsonUtils.stringToNode(pJsonConfig)
        if (node == null) {
            if (throwOnParseError) {
                throw ParseException("Couldn't parse Json:" + pJsonConfig, -1)
            }
            return GeocacheFilter(pName, false, false, null)
        }

        val name: String = pName != null  ? pName : JsonUtils.getText(node, CONFIG_KEY_NAME, null)
        val openInAdvancedMode: Boolean = JsonUtils.getBoolean(node, CONFIG_KEY_ADV_MODE, false)
        val includeInconclusive: Boolean = JsonUtils.getBoolean(node, CONFIG_KEY_INCLUDE_INCLUSIVE, false)

        IGeocacheFilter tree = null
        val treeNode: JsonNode = node.get(CONFIG_KEY_TREE)
        if (treeNode != null) {
            tree = JsonConfigurationUtils.fromJsonConfig(treeNode, id -> {
                switch (id) {
                    case "AND": return AndGeocacheFilter()
                    case "OR": return OrGeocacheFilter()
                    case "NOT": return NotGeocacheFilter()
                    default:
                        break
                }
                val filterType: GeocacheFilterType = GeocacheFilterType.getByTypeId(id)
                if (filterType == null) {
                    return null
                }
                return filterType.create()
            })
        }
        return GeocacheFilter(name, openInAdvancedMode, includeInconclusive, tree)
    }


}
