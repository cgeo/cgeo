package cgeo.geocaching.filters.core;

import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** filters for any one listid. Supports Storedlist as well as Psuedolist */
public class ListIdGeocacheFilter extends BaseGeocacheFilter {

    private int listId;

    public ListIdGeocacheFilter setListId(final int listId) {
        this.listId = listId;
        return this;
    }

    public int getListId() {
        return listId;
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (cache == null) {
            return null;
        }
        if (listId < 0) {
            return true;
        }
        return cache.getLists().contains(listId);
    }

    @Override
    public boolean isFiltering() {
        return listId > 0;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        addToSqlWhere(sqlBuilder, listId);
    }

    public static void addToSqlWhere(final SqlBuilder sqlBuilder, final int listId) {

        if (listId == PseudoList.HISTORY_LIST.id) {
            sqlBuilder.addWhere("( visiteddate > 0 OR geocode IN (SELECT geocode FROM " + DataStore.dbTableLogsOffline + ") )");
        } else if (listId > 0) {
            final String clId = sqlBuilder.getNewTableId();
            sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".geocode IN (SELECT " + clId + ".geocode FROM " + DataStore.dbTableCachesLists + " " + clId + " WHERE list_id " +
                    (listId != PseudoList.ALL_LIST.id ? "=" + listId : ">= " + StoredList.STANDARD_LIST_ID) + ")");
        } else {
            sqlBuilder.addWhereTrue();
        }
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setInt(node, "listid", listId);
        return node;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        this.listId = JsonUtils.getInt(node, "listid", -1);
    }

    @Override
    protected String getUserDisplayableConfig() {
        return "" + listId;
    }
}
