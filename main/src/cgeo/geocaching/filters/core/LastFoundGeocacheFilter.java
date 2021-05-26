package cgeo.geocaching.filters.core;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
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
        sqlBuilder.addJoin("LEFT JOIN (" + getGroupClause(sqlBuilder.getNewTableId()) + ") " + newTableId + " ON " + sqlBuilder.getMainTableId() + ".geocode = " + newTableId + ".geocode");

        addToSql(sqlBuilder, "CASE WHEN " + newTableId + ".max_date IS NULL THEN 0 ELSE " + newTableId + ".max_date END");
    }

    private static String getGroupClause(final String tid) {
        final String logIds = CollectionStream.of(Arrays.asList(LogType.getFoundLogIds())).toJoinedString(",");
        return "select " + tid + ".geocode, max(date) as max_date from cg_logs " + tid + " where " + tid + ".type in (" + logIds + ") group by " + tid + ".geocode";
    }


}
