package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CoordinateType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.location.Units;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.INamedGeoCoordinate;
import cgeo.geocaching.models.MapSelectableItem;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Html;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class GeoItemSelectorUtils {

    private GeoItemSelectorUtils() {
        //no instance
    }

    public static View createGeocacheItemView(final Context context, final Geocache cache, final View view) {
        final TextParam cacheName = TextParam.text(TextUtils.coloredCacheText(context, cache, StringUtils.defaultIfBlank(cache.getName(), "")));
        final ImageParam cacheIcon = ImageParam.drawable(MapMarkerUtils.getCacheMarker(context.getResources(), cache, CacheListType.MAP, Settings.getIconScaleEverywhere()).getDrawable());

        final StringBuilder text = new StringBuilder(cache.getShortGeocode());
        if (cache.getDifficulty() > 0.1f) {
            text.append(Formatter.SEPARATOR).append("D ").append(cache.getDifficulty());
        }
        if (cache.getTerrain() > 0.1f) {
            text.append(Formatter.SEPARATOR).append("T ").append(cache.getTerrain());
        }
        if (cache.isEventCache()) {
            final Date d = cache.getHiddenDate();
            if (d != null) {
                text.append(Formatter.SEPARATOR).append(Formatter.formatShortDate(d.getTime()));
            }
        }

        setViewValues(view, cacheName, TextParam.text(text), cacheIcon);

        return view;
    }

    public static View createWaypointItemView(final Context context, final Waypoint waypoint, final View view) {

        final Geocache parentCache = waypoint.getParentGeocache();
        final TextParam waypointName = TextParam.text(parentCache != null ? TextUtils.coloredCacheText(context, parentCache, StringUtils.defaultIfBlank(waypoint.getName(), "")) : waypoint.getName());
        final ImageParam waypointIcon = ImageParam.drawable(MapMarkerUtils.getWaypointMarker(context.getResources(), waypoint, false, Settings.getIconScaleEverywhere()).getDrawable());

        final StringBuilder text = new StringBuilder(waypoint.getShortGeocode());
        if (parentCache != null) {
            text.append(Formatter.SEPARATOR).append(parentCache.getName());
        }
        if (StringUtils.isNotBlank(Html.fromHtml(waypoint.getNote()))) {
            text.append(Formatter.SEPARATOR).append(Html.fromHtml(waypoint.getNote()));
        }
        setViewValues(view, waypointName, TextParam.text(text), waypointIcon);

        return view;
    }

    public static View createIWaypointItemView(final Context context, final INamedGeoCoordinate geoObject, final View view) {
        if (geoObject instanceof Geocache) {
            return createGeocacheItemView(context, (Geocache) geoObject, view);
        }
        if (geoObject instanceof Waypoint) {
            return createWaypointItemView(context, (Waypoint) geoObject, view);
        }
        throw new IllegalArgumentException("unsupported IWaypoint type"); // can never happen
    }

    public static View createGeoItemView(final Context context, final GeoitemRef geoitemRef, final View view) {
        if (StringUtils.isNotEmpty(geoitemRef.getGeocode())) {
            if (geoitemRef.getType() == CoordinateType.CACHE) {
                final Geocache cache = DataStore.loadCache(geoitemRef.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    return createGeocacheItemView(context, cache, view);
                }
            } else if (geoitemRef.getType() == CoordinateType.WAYPOINT) {
                final Waypoint waypoint = DataStore.loadWaypoint(geoitemRef.getId());
                if (waypoint != null) {
                    return createWaypointItemView(context, waypoint, view);
                }
            }
        }

        // Fallback - neither a cache nor waypoint. should never happen...
        setViewValues(view, TextParam.text(geoitemRef.getName()), TextParam.text(geoitemRef.getGeocode()), ImageParam.id(geoitemRef.getMarkerId()));

        return view;
    }

    public static View createRouteView(final Route route, final View view) {
        final boolean isIndividualRoute = route.getName().isEmpty();
        final TextParam routeName = isIndividualRoute ? TextParam.id(R.string.individual_route) : TextParam.text(route.getName());
        final ImageParam routeIcon = ImageParam.id(R.drawable.ic_menu_route);
        final TextParam routeInfo = isIndividualRoute ? TextParam.text(Units.getDistanceFromKilometers(route.getDistance())) : TextParam.id(R.string.track);
        setViewValues(view, routeName, routeInfo, routeIcon);
        return view;
    }

    public static View createMapSelectableItemView(final Context context, final MapSelectableItem item, final View view) {

        //handle special cases
        if (item.isRoute()) {
            return createRouteView(Objects.requireNonNull(item.getRoute()), view);
        }
        if (item.isRouteItem()) {
            return createRouteItemView(context, Objects.requireNonNull(item.getRouteItem()), view);
        }

        //create "pure" mapselectable item view
        setViewValues(view,
            item.getName() == null ? TextParam.id(R.string.unknown) : item.getName(),
            item.getDescription(),
            item.getIcon() == null ? ImageParam.id(R.drawable.shape_line) : item.getIcon());
        ImageViewCompat.setImageTintList(view.findViewById(R.id.icon), ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorText)));
        return view;

    }

    private static View createRouteItemView(final Context context, final RouteItem routeItem, final View view) {
        if (StringUtils.isNotEmpty(routeItem.getGeocode())) {
            if (routeItem.getType() == RouteItem.RouteItemType.GEOCACHE) {
                final Geocache cache = DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    return createGeocacheItemView(context, cache, view);
                }
            } else if (routeItem.getType() == RouteItem.RouteItemType.WAYPOINT) {
                final Waypoint waypoint = DataStore.loadWaypoint(routeItem.getWaypointId());
                if (waypoint != null) {
                    return createWaypointItemView(context, waypoint, view);
                }
            }
        }
        // Fallback - coords only points
        final TextParam title = TextParam.id(R.string.route_item_point);
        final ImageParam routeIcon = ImageParam.id(R.drawable.marker_routepoint);
        final TextParam subtitle = TextParam.text(routeItem.getPoint().format(GeopointFormatter.Format.LAT_LON_DECMINUTE));
        setViewValues(view, title, subtitle, routeIcon);
        return view;
    }

    public static View getOrCreateView(final Context context, final View convertView, final ViewGroup parent) {
        final View view = convertView == null ? LayoutInflater.from(context).inflate(R.layout.cacheslist_item_select, parent, false) : convertView;
        ((TextView) view.findViewById(R.id.text)).setText(new SpannableString(""));
        return view;
    }

    private static void setViewValues(final View view, final TextParam name, final TextParam info, final ImageParam icon) {
        if (name != null) {
            name.applyTo(view.findViewById(R.id.text));
        }
        if (info != null) {
            info.applyTo(view.findViewById(R.id.info));
        }
        if (icon != null) {
            icon.applyTo(view.findViewById(R.id.icon));
        }
    }

}
