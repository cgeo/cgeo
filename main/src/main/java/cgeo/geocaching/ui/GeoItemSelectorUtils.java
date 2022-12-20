package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.maps.mapsforge.v6.caches.GeoitemRef;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.content.Context;
import android.graphics.Paint;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.apache.commons.lang3.StringUtils;

public class GeoItemSelectorUtils {

    private GeoItemSelectorUtils() {
        //no instance
    }

    public static View createGeocacheItemView(final Context context, final Geocache cache, final View view) {

        final TextView tv = (TextView) view.findViewById(R.id.text);
        setTitleTextStyle(context, tv, cache);
        tv.setText(cache.getName());

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(MapMarkerUtils.getCacheMarker(context.getResources(), cache, CacheListType.MAP).getDrawable(), null, null, null);

        final StringBuilder text = new StringBuilder(cache.getShortGeocode());
        if (cache.getDifficulty() > 0.1f) {
            text.append(Formatter.SEPARATOR).append("D ").append(cache.getDifficulty());
        }
        if (cache.getTerrain() > 0.1f) {
            text.append(Formatter.SEPARATOR).append("T ").append(cache.getTerrain());
        }

        final TextView infoView = (TextView) view.findViewById(R.id.info);
        infoView.setText(text);

        return view;
    }

    public static View createWaypointItemView(final Context context, final Waypoint waypoint, final View view) {

        final TextView tv = (TextView) view.findViewById(R.id.text);
        tv.setText(waypoint.getName());

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(MapMarkerUtils.getWaypointMarker(context.getResources(), waypoint, false).getDrawable(), null, null, null);

        final StringBuilder text = new StringBuilder(waypoint.getShortGeocode());
        final Geocache parentCache = waypoint.getParentGeocache();

        if (parentCache != null) {
            setTitleTextStyle(context, tv, parentCache);
            text.append(Formatter.SEPARATOR).append(parentCache.getName());
        }

        if (StringUtils.isNotBlank(Html.fromHtml(waypoint.getNote()))) {
            text.append(Formatter.SEPARATOR).append(Html.fromHtml(waypoint.getNote()));
        }

        final TextView infoView = (TextView) view.findViewById(R.id.info);
        infoView.setText(text);

        return view;
    }

    public static View createIWaypointItemView(final Context context, final IWaypoint geoObject, final View view) {
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
            if (geoitemRef.getType() == CoordinatesType.CACHE) {
                final Geocache cache = DataStore.loadCache(geoitemRef.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                if (cache != null) {
                    return createGeocacheItemView(context, cache, view);
                }
            } else if (geoitemRef.getType() == CoordinatesType.WAYPOINT) {
                final Waypoint waypoint = DataStore.loadWaypoint(geoitemRef.getId());
                if (waypoint != null) {
                    return createWaypointItemView(context, waypoint, view);
                }
            }
        }

        // Fallback - neither a cache nor waypoint. should never happen...

        final TextView tv = (TextView) view.findViewById(R.id.text);
        tv.setText(geoitemRef.getName());

        final TextView infoView = (TextView) view.findViewById(R.id.info);
        infoView.setText(geoitemRef.getGeocode());

        tv.setCompoundDrawablesWithIntrinsicBounds(geoitemRef.getMarkerId(), 0, 0, 0);


        return view;
    }

    public static View createRouteItemView(final Context context, final RouteItem routeItem, final View view) {
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

        // Fallback - neither a cache nor waypoint. should never happen...
        final TextView tv = view.findViewById(R.id.text);
        tv.setText(routeItem.getIdentifier());

        final TextView infoView = view.findViewById(R.id.info);
        infoView.setText(routeItem.getGeocode());

        // tv.setCompoundDrawablesWithIntrinsicBounds(routeItem.getMarkerId(), 0, 0, 0);
        return view;
    }

    public static View getOrCreateView(final Context context, final View convertView, final ViewGroup parent) {
        final View view = convertView == null ? LayoutInflater.from(context).inflate(R.layout.cacheslist_item_select, parent, false) : convertView;
        setTitleTextStyle(context, view.findViewById(R.id.text), null); // reset title style
        return view;
    }

    private static void setTitleTextStyle(final Context context, final TextView tv, final Geocache cache) {
        if (cache != null && (cache.isDisabled() || cache.isArchived() || CalendarUtils.isPastEvent(cache))) { // strike
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }
        if (cache != null && cache.isArchived()) { // red color
            tv.setTextColor(ContextCompat.getColor(context, R.color.archived_cache_color));
        } else {
            tv.setTextColor(ContextCompat.getColor(context, R.color.colorText));
        }
    }

}
