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

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.enumerations.CoordinateType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.location.Units
import cgeo.geocaching.maps.RouteTrackUtils
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.INamedGeoCoordinate
import cgeo.geocaching.models.MapSelectableItem
import cgeo.geocaching.models.Route
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.utils.TextUtils

import android.content.Context
import android.content.res.ColorStateList
import android.text.Html
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat

import java.util.Date
import java.util.Objects

import org.apache.commons.lang3.StringUtils

class GeoItemSelectorUtils {

    private GeoItemSelectorUtils() {
        //no instance
    }

    public static View createGeocacheItemView(final Context context, final Geocache cache, final View view) {
        val cacheName: TextParam = TextParam.text(TextUtils.coloredCacheText(context, cache, StringUtils.defaultIfBlank(cache.getName(), "")))
        val cacheIcon: ImageParam = ImageParam.drawable(MapMarkerUtils.getCacheMarker(context.getResources(), cache, CacheListType.MAP, Settings.getIconScaleEverywhere()).getDrawable())

        val text: StringBuilder = StringBuilder(cache.getShortGeocode())
        if (cache.getDifficulty() > 0.1f) {
            text.append(Formatter.SEPARATOR).append("D ").append(cache.getDifficulty())
        }
        if (cache.getTerrain() > 0.1f) {
            text.append(Formatter.SEPARATOR).append("T ").append(cache.getTerrain())
        }
        if (cache.isEventCache()) {
            val d: Date = cache.getHiddenDate()
            if (d != null) {
                text.append(Formatter.SEPARATOR).append(Formatter.formatShortDate(d.getTime()))
            }
        }

        setViewValues(view, cacheName, TextParam.text(text), cacheIcon)

        return view
    }

    public static View createWaypointItemView(final Context context, final Waypoint waypoint, final View view) {

        val parentCache: Geocache = waypoint.getParentGeocache()
        val waypointName: TextParam = TextParam.text(parentCache != null ? TextUtils.coloredCacheText(context, parentCache, StringUtils.defaultIfBlank(waypoint.getName(), "")) : waypoint.getName())
        val waypointIcon: ImageParam = ImageParam.drawable(MapMarkerUtils.getWaypointMarker(context.getResources(), waypoint, false, Settings.getIconScaleEverywhere()).getDrawable())

        val text: StringBuilder = StringBuilder(waypoint.getShortGeocode())
        if (parentCache != null) {
            text.append(Formatter.SEPARATOR).append(parentCache.getName())
        }
        if (StringUtils.isNotBlank(Html.fromHtml(waypoint.getNote()))) {
            text.append(Formatter.SEPARATOR).append(Html.fromHtml(waypoint.getNote()))
        }
        setViewValues(view, waypointName, TextParam.text(text), waypointIcon)

        return view
    }

    public static View createIWaypointItemView(final Context context, final INamedGeoCoordinate geoObject, final View view) {
        if (geoObject is Geocache) {
            return createGeocacheItemView(context, (Geocache) geoObject, view)
        }
        if (geoObject is Waypoint) {
            return createWaypointItemView(context, (Waypoint) geoObject, view)
        }
        throw IllegalArgumentException("unsupported IWaypoint type"); // can never happen
    }

    public static View createGeoItemView(final Context context, final GeoitemRef geoitemRef, final View view) {
        if (StringUtils.isNotEmpty(geoitemRef.getGeocode())) {
            if (geoitemRef.getType() == CoordinateType.CACHE) {
                val cache: Geocache = DataStore.loadCache(geoitemRef.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB)
                if (cache != null) {
                    return createGeocacheItemView(context, cache, view)
                }
            } else if (geoitemRef.getType() == CoordinateType.WAYPOINT) {
                val waypoint: Waypoint = DataStore.loadWaypoint(geoitemRef.getId())
                if (waypoint != null) {
                    return createWaypointItemView(context, waypoint, view)
                }
            }
        }

        // Fallback - neither a cache nor waypoint. should never happen...
        setViewValues(view, TextParam.text(geoitemRef.getName()), TextParam.text(geoitemRef.getGeocode()), ImageParam.id(geoitemRef.getMarkerId()))

        return view
    }

    public static View createRouteView(final Route route, final View view) {
        val isIndividualRoute: Boolean = RouteTrackUtils.isIndividualRoute(route)
        val isNavigationTargetRoute: Boolean = RouteTrackUtils.isNavigationTargetRoute(route)
        val routeName: TextParam = isIndividualRoute ? TextParam.id(R.string.individual_route) : TextParam.text(route.getName())
        val routeIcon: ImageParam = isNavigationTargetRoute ? ImageParam.id(Settings.getRoutingMode().drawableId) : ImageParam.id(R.drawable.ic_menu_route)
        val routeInfo: TextParam = isIndividualRoute || isNavigationTargetRoute ? TextParam.text(Units.getDistanceFromKilometers(route.getDistance())) : TextParam.id(R.string.track)
        setViewValues(view, routeName, routeInfo, routeIcon)
        return view
    }

    public static View createMapSelectableItemView(final Context context, final MapSelectableItem item, final View view) {

        //handle special cases
        if (item.isRoute()) {
            return createRouteView(Objects.requireNonNull(item.getRoute()), view)
        }
        if (item.isRouteItem()) {
            return createRouteItemView(context, Objects.requireNonNull(item.getRouteItem()), view)
        }

        //create "pure" mapselectable item view
        setViewValues(view,
            item.getName() == null ? TextParam.id(R.string.unknown) : item.getName(),
            item.getDescription(),
            item.getIcon() == null ? ImageParam.id(R.drawable.shape_line) : item.getIcon())
        ImageViewCompat.setImageTintList(view.findViewById(R.id.icon), ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorText)))
        return view

    }

    private static View createRouteItemView(final Context context, final RouteItem routeItem, final View view) {
        if (StringUtils.isNotEmpty(routeItem.getGeocode())) {
            if (routeItem.getType() == RouteItem.RouteItemType.GEOCACHE) {
                val cache: Geocache = DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB)
                if (cache != null) {
                    return createGeocacheItemView(context, cache, view)
                }
            } else if (routeItem.getType() == RouteItem.RouteItemType.WAYPOINT) {
                val waypoint: Waypoint = DataStore.loadWaypoint(routeItem.getWaypointId())
                if (waypoint != null) {
                    return createWaypointItemView(context, waypoint, view)
                }
            }
        }
        // Fallback - coords only points
        val title: TextParam = TextParam.id(R.string.route_item_point)
        val routeIcon: ImageParam = ImageParam.id(R.drawable.marker_routepoint)
        val subtitle: TextParam = TextParam.text(routeItem.getPoint().format(GeopointFormatter.Format.LAT_LON_DECMINUTE))
        setViewValues(view, title, subtitle, routeIcon)
        return view
    }

    public static View getOrCreateView(final Context context, final View convertView, final ViewGroup parent) {
        val view: View = convertView == null ? LayoutInflater.from(context).inflate(R.layout.cacheslist_item_select, parent, false) : convertView
        ((TextView) view.findViewById(R.id.text)).setText(SpannableString(""))
        return view
    }

    private static Unit setViewValues(final View view, final TextParam name, final TextParam info, final ImageParam icon) {
        if (name != null) {
            name.applyTo(view.findViewById(R.id.text))
        }
        if (info != null) {
            info.applyTo(view.findViewById(R.id.info))
        }
        if (icon != null) {
            icon.applyTo(view.findViewById(R.id.icon))
        }
    }

}
