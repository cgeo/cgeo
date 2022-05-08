package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;


public class OfflineLogGeocacheFilter extends StringGeocacheFilter {


    @Override
    protected String getValue(final Geocache cache) {
        if (!getStringFilter().isFilled()) {
            return "";
        }
        return cache.hasLogOffline() ? cache.getOfflineLog().log : "";
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        final String logTableId = sqlBuilder.getNewTableId();
        getStringFilter().addToSqlForSubquery(sqlBuilder,
                "SELECT geocode FROM cg_logs_offline " + logTableId + " WHERE " + logTableId + ".geocode = " + sqlBuilder.getMainTableId() + ".geocode",
                true, logTableId + ".log");
    }
}
