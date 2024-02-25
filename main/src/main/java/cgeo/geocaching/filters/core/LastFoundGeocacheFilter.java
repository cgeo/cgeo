package cgeo.geocaching.filters.core;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.CollectionStream;

import java.util.Arrays;
import java.util.Date;


public class LastFoundGeocacheFilter extends DateRangeGeocacheFilter {

    @Override
    protected Date getDate(final Geocache cache) {
        return cache.getLastFound();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!isFiltering()) {
            super.addToSql(sqlBuilder);
            return;
        }

        final String newTableId = sqlBuilder.getNewTableId();
        sqlBuilder.addJoin("LEFT JOIN (" + getGroupClause(sqlBuilder.getNewTableId()) + ") " + newTableId + " ON " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode + " = " + newTableId + "." + DataStore.dbField_Geocode);

        addToSql(sqlBuilder, "CASE WHEN " + newTableId + ".max_date IS NULL THEN 0 ELSE " + newTableId + ".max_date END");
    }

    private static String getGroupClause(final String tid) {
        final String logIds = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",");
        return "select " + tid + "." + DataStore.dbField_Geocode + ", max(date) as max_date from " + DataStore.dbTableLogs + " " + tid + " where " + tid + "." + DataStore.dbFieldLogs_Type + " in (" + logIds + ") group by " + tid + "." + DataStore.dbField_Geocode;
    }


}
