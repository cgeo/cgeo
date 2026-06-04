package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.list.AbstractList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.config.LegacyFilterConfig;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.math.NumberUtils;

public class StoredListGeocacheFilter extends BaseGeocacheFilter {

    private final Set<Integer> filterListIds = new HashSet<>();

    public Set<StoredList> getFilterLists() {
        final Set<StoredList> filterLists = new HashSet<>();
        for (Integer listId : filterListIds) {
            final AbstractList al = AbstractList.getListById(listId);
            if (al instanceof StoredList) {
                filterLists.add((StoredList) al);
            }
        }
        return filterLists;
    }

    public void setFilterLists(final Collection<StoredList> lists) {
        filterListIds.clear();
        for (StoredList list : lists) {
            filterListIds.add(list.id);
        }
    }


    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        if (filterListIds.isEmpty()) {
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
        return !filterListIds.isEmpty();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (filterListIds.isEmpty()) {
            sqlBuilder.addWhereTrue();
        } else {
            final String idString = CollectionStream.of(filterListIds).toJoinedString(",");
            final String clId = sqlBuilder.getNewTableId();
            sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " IN (SELECT " + clId + "." + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableCachesLists + " " + clId +
                    " WHERE " + DataStore.dbFieldCachesLists_list_id + " IN (" + idString + "))");
        }
    }


    @Override
    public void setConfig(final LegacyFilterConfig config) {
        setConfigInternal(config.getDefaultList());
    }

    private void setConfigInternal(final List<String> configValues) {
        filterListIds.clear();
        for (String value : configValues) {
            if (NumberUtils.isParsable(value)) {
                filterListIds.add(Integer.parseInt(value));
            }
        }
    }

    @Override
    public LegacyFilterConfig getConfig() {
        final LegacyFilterConfig config = new LegacyFilterConfig();
        config.putDefaultList(getConfigInternal());
        return config;
    }

    private List<String> getConfigInternal() {
        final List<String> result = new ArrayList<>();
        for (int list : filterListIds) {
            result.add("" + list);
        }
        return result;
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setTextCollection(node, "values", getConfigInternal());
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        setConfigInternal(JsonUtils.getTextList(node, "values"));
    }

    @Override
    protected String getUserDisplayableConfig() {
        if (filterListIds.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none);
        }
        if (filterListIds.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, filterListIds.size());
        }
        final AbstractList al = AbstractList.getListById(filterListIds.iterator().next());
        return al == null ? "?" : al.getTitle();
    }
}
