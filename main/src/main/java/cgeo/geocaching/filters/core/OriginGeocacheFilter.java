package cgeo.geocaching.filters.core;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.Collections;

public class OriginGeocacheFilter extends ValueGroupGeocacheFilter<IConnector, IConnector> {

    public static OriginGeocacheFilter create(final IConnector... connectors) {
        final OriginGeocacheFilter origin = GeocacheFilterType.ORIGIN.create();
        for (final IConnector c : connectors) {
            origin.setValues(Collections.singletonList(c));
        }
        return origin;
    }

    @Override
    public IConnector getRawCacheValue(final Geocache cache) {
        return ConnectorFactory.getConnector(cache);
    }

    @Override
    public IConnector valueFromString(final String stringValue) {
        return ConnectorFactory.getConnectorByName(stringValue);
    }

    @Override
    public String valueToString(final IConnector value) {
        return value.getName();
    }

    public String valueToUserDisplayableValue(final IConnector value) {
        return value.getDisplayName();
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!getValues().isEmpty()) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
            for (IConnector con : getRawValues()) {
                for (String sqlLikeExp : con.getGeocodeSqlLikeExpressions()) {
                    sqlBuilder.addWhere(sqlBuilder.getMainTableId() + ".geocode LIKE ?", sqlLikeExp);
                }
            }
            sqlBuilder.closeWhere();
        } else {
            sqlBuilder.addWhereTrue();
        }
    }

    public boolean allowsCachesOf(final IConnector connector) {
        return !isFiltering() || getValues().contains(connector);
    }

}
