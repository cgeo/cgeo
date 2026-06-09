package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;


public class OfflineLogGeocacheFilter extends StringGeocacheFilter {

    public static OfflineLogGeocacheFilter create(final String text) {
        return OfflineLogGeocacheFilter.create(text, false, StringFilter.StringFilterType.CONTAINS);
    }

    public static OfflineLogGeocacheFilter create(final String text, final boolean matchCase, final StringFilter.StringFilterType filterType) {
        return StringGeocacheFilter.create(GeocacheFilterType.OFFLINE_LOG, text, matchCase, filterType);
    }

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
                "SELECT " + DataStore.dbField_Geocode + " FROM " + DataStore.dbTableLogsOffline + " " + logTableId + " WHERE " + logTableId + "." + DataStore.dbField_Geocode + " = " + sqlBuilder.getMainTableId() + "." + DataStore.dbField_Geocode,
                true, logTableId + "." + DataStore.dbFieldLogsOffline_log);
    }
}
