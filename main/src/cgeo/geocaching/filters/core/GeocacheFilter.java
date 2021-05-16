package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.expressions.ExpressionParser;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

public class GeocacheFilter {

    private static final Set<Character> DELIM_SET = new HashSet<>(Collections.singletonList(']'));

    private static final ExpressionParser<IGeocacheFilter> FILTER_PARSER = new ExpressionParser<IGeocacheFilter>()
        .register(AndGeocacheFilter::new)
        .register(OrGeocacheFilter::new)
        .register(NotGeocacheFilter::new)
        .register(InconclusiveGeocacheFilter::new);

    static {
        for (GeocacheFilterType gcf : GeocacheFilterType.values()) {
            FILTER_PARSER.register(gcf::create);
        }
    }

    private final String name;
    private final IGeocacheFilter tree;

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
            return other != null && !other.getTreeConfig().equals(filter.getTreeConfig());
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

    public GeocacheFilter(final String name, final String treeConfig) {
        this(name, FILTER_PARSER.createWithNull(treeConfig));
    }

    public GeocacheFilter(final String name, final IGeocacheFilter tree) {
        this.name = name;
        this.tree = tree;
    }

    @NonNull
    public String getName() {
        return name == null ? "" : name;
    }

    public IGeocacheFilter getTree() {
        return tree;
    }

    public String getTreeConfig() {
        return tree == null ? null : FILTER_PARSER.getConfig(tree);
    }

    @NonNull
    public static GeocacheFilter createFromConfig(final String filterConfig) {
        if (filterConfig == null) {
            return new GeocacheFilter(null, (String) null);
        }
        try {
            return createInternal(filterConfig, false);
        } catch (ParseException e) {
            //will never happen
            return new GeocacheFilter(null, (String) null);
        }
    }

    public static GeocacheFilter checkConfig(final String filterConfig) throws ParseException {
        return createInternal(filterConfig, true);
    }

    private static GeocacheFilter createInternal(final String filterConfig, final boolean throwOnParseError) throws ParseException {

        if (filterConfig == null) {
            return new GeocacheFilter(null, (String) null);
        }
        String name = null;
        IGeocacheFilter tree = null;

        //See if config contains info beside the filter expression itself
        int idx = 0;
        if (filterConfig.startsWith("[")) {
            final StringBuilder sb = new StringBuilder();
            idx = ExpressionParser.parseToNextDelim(filterConfig, 1, DELIM_SET, sb) + 1;
            name = sb.toString();
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
        return new GeocacheFilter(name, tree);
    }

    public boolean hasFilter() {
        return tree != null;
    }

    public String toConfig() {
        return "[" + (name == null ? "" : name.replaceAll("]", "\\]")) + "]" + (tree == null ? "" : FILTER_PARSER.getConfig(tree));
    }

    public Boolean filter(final Geocache cache) {
        if (tree == null) {
            return true;
        }
        return tree.filter(cache);
    }

    public void filterList(final Collection<Geocache> list) {

        final List<Geocache> itemsToKeep = new ArrayList<>();
        for (final Geocache item : list) {
            final Boolean fr = filter(item);
            if (fr != null && fr) {
                itemsToKeep.add(item);
            }
        }

        list.clear();
        //note that since both "list" and "itemsToKeep" are ArrayLists, the addAll-operation is very fast (two arraycopies of the references)
        list.addAll(itemsToKeep);
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

    public String toUserDisplayableString() {
        if (!StringUtils.isBlank(getName())) {
            return getNameForUserDisplay();
        }

        if (getTree() == null) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }

        return getTree().toUserDisplayableString(0);
    }

}
