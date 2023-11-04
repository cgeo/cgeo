package cgeo.geocaching.models;

import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.Nullable;

import java.util.Comparator;

/** artificial superclass for RouteItem or Track objects */
public class RouteOrRouteItem {
    public static final Comparator<? super RouteOrRouteItem> NAME_COMPARATOR = (Comparator<RouteOrRouteItem>) (left, right) -> TextUtils.COLLATOR.compare(
            left.data instanceof RouteItem ? ((RouteItem) left.data).getIdentifier() : ((Route) left.data).getName(),
            right.data instanceof RouteItem ? ((RouteItem) right.data).getIdentifier() : ((Route) right.data).getName());

    private final Object data;

    public RouteOrRouteItem(final RouteItem routeItem) {
        this.data = routeItem;
    }

    public RouteOrRouteItem(final Route route) {
        this.data = route;
    }

    public Object getRawData() {
        return data;
    }

    public boolean isRoute() {
        return data instanceof Route;
    }

    public boolean isRouteItem() {
        return data instanceof RouteItem;
    }

    @Nullable
    public RouteItem getRouteItem() {
        return isRouteItem() ? (RouteItem) data : null;
    }

    @Nullable
    public Route getRoute() {
        return isRoute() ? (Route) data : null;
    }
}
