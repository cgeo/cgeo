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

import cgeo.geocaching.list.PseudoList
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.storage.SqlBuilder
import cgeo.geocaching.utils.JsonUtils

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import com.fasterxml.jackson.databind.node.ObjectNode

/** filters for any one listid. Supports Storedlist as well as Psuedolist */
class ListIdGeocacheFilter : BaseGeocacheFilter() {

    private Int listId

    public ListIdGeocacheFilter setListId(final Int listId) {
        this.listId = listId
        return this
    }

    public Int getListId() {
        return listId
    }

    override     public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null
        }
        if (listId < 0) {
            return true
        }
        return cache.getLists().contains(listId)
    }

    override     public Boolean isFiltering() {
        return listId > 0
    }

    override     public Unit addToSql(final SqlBuilder sqlBuilder) {
        addToSqlWhere(sqlBuilder, listId)
    }

    public static Unit addToSqlWhere(final SqlBuilder sqlBuilder, final Int listId) {

        if (listId == PseudoList.HISTORY_LIST.id) {
            sqlBuilder.addWhere("( visiteddate > 0 OR geocode IN (SELECT geocode FROM " + DataStore.dbTableLogsOffline + ") )")
        } else if (listId > 0) {
            val clId: String = sqlBuilder.getNewTableId()
            sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".geocode IN (SELECT " + clId + ".geocode FROM " + DataStore.dbTableCachesLists + " " + clId + " WHERE list_id " +
                    (listId != PseudoList.ALL_LIST.id ? "=" + listId : ">= " + StoredList.STANDARD_LIST_ID) + ")")
        } else {
            sqlBuilder.addWhereTrue()
        }
    }

    override     public ObjectNode getJsonConfig() {
        val node: ObjectNode = JsonUtils.createObjectNode()
        JsonUtils.setInt(node, "listid", listId)
        return node
    }

    override     public Unit setJsonConfig(final ObjectNode node) {
        this.listId = JsonUtils.getInt(node, "listid", -1)
    }

    override     protected String getUserDisplayableConfig() {
        return "" + listId
    }
}
