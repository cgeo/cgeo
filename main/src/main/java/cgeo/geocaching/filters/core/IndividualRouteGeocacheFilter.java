package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.ArrayList;

public class IndividualRouteGeocacheFilter extends BooleanGeocacheFilter {

    @Override
    public Boolean filter(final Geocache cache, final boolean value) {
        final ArrayList<RouteItem> routeItems = DataStore.loadIndividualRoute();
        return routeItems.stream().anyMatch(item -> cache.getGeocode().equals(item.getGeocode())) == value;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder, final boolean value) {
        final String routeTableId = sqlBuilder.getNewTableId();

        sqlBuilder.addWhere((value ? "" : "NOT ") +
                "EXISTS(SELECT id FROM cg_route " + routeTableId + " WHERE " + routeTableId + ".id = " + sqlBuilder.getMainTableId() + ".geocode)");
    }

}
