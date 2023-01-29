package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.maps.CacheMarker;
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.utils.MapLineUtils;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class GoogleCacheOverlayItem implements CachesOverlayItemImpl, MapObjectOptionsFactory {
    private final IWaypoint coord;
    private final boolean applyDistanceRule;
    private BitmapDescriptorCache bitmapDescriptorCache;
    private CacheMarker marker;

    public GoogleCacheOverlayItem(final IWaypoint coordinate, final boolean applyDistanceRule) {
        this.coord = coordinate;
        this.applyDistanceRule = applyDistanceRule;
    }

    @Override
    public IWaypoint getCoord() {
        return coord;
    }

    @Override
    public boolean applyDistanceRule() {
        return applyDistanceRule;
    }

    @Override
    public String getTitle() {
        return this.coord.getName();
    }

    @Override
    public CacheMarker getMarker(final int index) {
        return marker;
    }

    @Override
    public void setMarker(final CacheMarker markerIn) {
        this.marker = markerIn;
    }

    private static LatLng toLatLng(final IWaypoint w) {
        return new LatLng(w.getCoords().getLatitude(), w.getCoords().getLongitude());
    }

    public void setBitmapDescriptorCache(final BitmapDescriptorCache bitmapDescriptorCache) {
        this.bitmapDescriptorCache = bitmapDescriptorCache;
    }

    @Override
    public MapObjectOptions[] getMapObjectOptions(final boolean showCircles) {
        final MarkerOptions marker = new MarkerOptions()
                .icon(toBitmapDescriptor(this.marker))
                .position(toLatLng(coord))
                .anchor(0.5f, 1)
                .zIndex((coord instanceof Geocache) ? GoogleCachesList.ZINDEX_GEOCACHE : GoogleCachesList.ZINDEX_WAYPOINT);

        if (showCircles && applyDistanceRule) {
            final CircleOptions circle = new CircleOptions()
                    .center(toLatLng(coord))
                    .strokeColor(MapLineUtils.getCircleColor())
                    .strokeWidth(2)
                    .fillColor(MapLineUtils.getCircleFillColor())
                    .radius(GoogleCachesList.CIRCLE_RADIUS)
                    .zIndex(GoogleCachesList.ZINDEX_CIRCLE);

            return new MapObjectOptions[]{MapObjectOptions.marker(marker), MapObjectOptions.circle(circle)};
        } else {
            return new MapObjectOptions[]{MapObjectOptions.marker(marker)};
        }
    }

    private BitmapDescriptor toBitmapDescriptor(final CacheMarker d) {
        if (bitmapDescriptorCache != null) {
            return bitmapDescriptorCache.fromCacheMarker(d);
        } else {
            return BitmapDescriptorCache.toBitmapDescriptor(d.getDrawable());
        }
    }
}
