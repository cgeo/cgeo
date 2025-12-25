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
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.JsonUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.config.LegacyFilterConfig

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collection
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Set

import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.lang3.math.NumberUtils

class StoredListGeocacheFilter : BaseGeocacheFilter() {

    private val filterLists: Set<StoredList> = HashSet<>()
    private val filterListIds: Set<Integer> = HashSet<>()

    public Set<StoredList> getFilterLists() {
        return filterLists
    }

    public Unit setFilterLists(final Collection<StoredList> lists) {
        filterLists.clear()
        filterListIds.clear()
        for (StoredList list : lists) {
            filterLists.add(list)
            filterListIds.add(list.id)
        }
    }


    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }
        if (filterLists.isEmpty()) {
            return true
        }

        if (!cache.isOffline()) {
            return false
        }

        for (Integer listId : cache.getLists()) {
            if (filterListIds.contains(listId)) {
                return true
            }
        }
        return false
    }

    override     public Boolean isFiltering() {
        return !filterLists.isEmpty()
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        if (filterLists.isEmpty()) {
            sqlBuilder.addWhereTrue()
        } else {
            val idString: String = CollectionStream.of(filterListIds).toJoinedString(",")
            val clId: String = sqlBuilder.getNewTableId()
            sqlBuilder.addWhere(sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " IN (SELECT " + clId + "." + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableCachesLists + " " + clId +
                    " WHERE " + DataStore.dbFieldCachesLists_list_id + " IN (" + idString + "))")
        }
    }


    override     public Unit setConfig(final LegacyFilterConfig config) {
        setConfigInternal(config.getDefaultList())
    }

    private Unit setConfigInternal(final List<String> configValues) {
        val lists: List<StoredList> = DataStore.getLists()
        val listsById: Map<Integer, StoredList> = HashMap<>()
        val listsByName: Map<String, StoredList> = HashMap<>()
        for (StoredList list : lists) {
            listsById.put(list.id, list)
            listsByName.put(list.title.toLowerCase(Locale.getDefault()), list)
        }

        filterLists.clear()
        filterListIds.clear()
        for (String value : configValues) {
            StoredList list = null
            if (NumberUtils.isParsable(value)) {
                list = listsById.get(Integer.parseInt(value))
            }
            if (list == null) {
                list = listsByName.get(value.toLowerCase(Locale.getDefault()))
            }
            if (list != null) {
                filterLists.add(list)
                filterListIds.add(list.id)
            }
        }
    }

    override     public LegacyFilterConfig getConfig() {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        config.putDefaultList(getConfigInternal())
        return config
    }

    private List<String> getConfigInternal() {
        val result: List<String> = ArrayList<>()
        for (StoredList list : filterLists) {
            result.add("" + list.id)
        }
        return result
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setTextCollection(node, "values", getConfigInternal())
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        setConfigInternal(JsonUtils.getTextList(node, "values"))
    }

    override     protected String getUserDisplayableConfig() {
        if (filterLists.isEmpty()) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_none)
        }
        if (filterLists.size() > 1) {
            return LocalizationUtils.getPlural(R.plurals.cache_filter_userdisplay_multi_item, filterLists.size())
        }

        return filterLists.iterator().next().title
    }
}
