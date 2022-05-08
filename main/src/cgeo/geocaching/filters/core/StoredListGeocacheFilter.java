package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;

public class StoredListGeocacheFilter extends BaseGeocacheFilter {

    private final Set<StoredList> filterLists = new HashSet<>();
    private final Set<Integer> filterListIds = new HashSet<>();

    public Set<StoredList> getFilterLists() {
        return filterLists;
    }

    public void setFilterLists(final Collection<StoredList> lists) {
        filterLists.clear();
        filterListIds.clear();
        for (StoredList list : lists) {
            filterLists.add(list);
            filterListIds.add(list.id);
        }
    }


    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        if (filterLists.isEmpty()) {
            return true;
        }

        if (!cache.isOffline()) {
            return false;
        }

        for (Integer listId : cache.getLists()) {
            if (filterListIds.contains(listId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFiltering() {
        return !filterLists.isEmpty();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (filterLists.isEmpty()) {
            sqlBuilder.addWhereTrue();
        } else {
            final String idString = CollectionStream.of(filterListIds).toJoinedString(",");
            final String clId = sqlBuilder.getNewTableId();
            sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".geocode IN (SELECT " + clId + ".geocode FROM cg_caches_lists " + clId +
                    " WHERE list_id IN (" + idString + "))");
        }
    }


    @Override
    public void setConfig(final ExpressionConfig config) {
        final List<StoredList> lists = DataStore.getLists();
        final Map<Integer, StoredList> listsById = new HashMap<>();
        final Map<String, StoredList> listsByName = new HashMap<>();
        for (StoredList list : lists) {
            listsById.put(list.id, list);
            listsByName.put(list.title.toLowerCase(Locale.getDefault()), list);
        }

        filterLists.clear();
        filterListIds.clear();
        for (String value : config.getDefaultList()) {
            StoredList list = null;
            if (NumberUtils.isParsable(value)) {
                list = listsById.get(Integer.parseInt(value));
            }
            if (list == null) {
                list = listsByName.get(value.toLowerCase(Locale.getDefault()));
            }
            if (list != null) {
                filterLists.add(list);
                filterListIds.add(list.id);
            }
        }
    }

    @Override
    public ExpressionConfig getConfig() {
        final ExpressionConfig config = new ExpressionConfig();
        for (StoredList list : filterLists) {
            config.addToDefaultList("" + list.id);
        }
        return config;
    }

    @Override
    protected String getUserDisplayableConfig() {
        if (filterLists.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (filterLists.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, filterLists.size());
        }

        return filterLists.iterator().next().title;
    }
}
