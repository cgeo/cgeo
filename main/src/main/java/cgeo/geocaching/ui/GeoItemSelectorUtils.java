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
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MapMarkerUtils;
import cgeo.geocaching.utils.TextUtils;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

public class GeoItemSelectorUtils {

    private GeoItemSelectorUtils() {
        //no instance
    }

    public static View createGeocacheItemView(final Context context, final Geocache cache, final View view) {

        final TextView tv = (TextView) view.findViewById(R.id.text);
        tv.setText(TextUtils.coloredCacheText(context, cache, cache.getName()));

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(MapMarkerUtils.getCacheMarker(context.getResources(), cache, CacheListType.MAP, Settings.getIconScaleEverywhere()).getDrawable(), null, null, null);

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

        final Geocache parentCache = waypoint.getParentGeocache();

        final TextView tv = (TextView) view.findViewById(R.id.text);
        tv.setText(parentCache != null ? TextUtils.coloredCacheText(context, parentCache, waypoint.getName()) : waypoint.getName());

        tv.setCompoundDrawablesRelativeWithIntrinsicBounds(MapMarkerUtils.getWaypointMarker(context.getResources(), waypoint, false, Settings.getIconScaleEverywhere()).getDrawable(), null, null, null);

        final StringBuilder text = new StringBuilder(waypoint.getShortGeocode());

        if (parentCache != null) {
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
        ((TextView) view.findViewById(R.id.text)).setText(new SpannableString(""));
        return view;
    }

}
