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

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.maps.CacheMarker
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.INamedGeoCoordinate
import cgeo.geocaching.utils.MapLineUtils

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class GoogleCacheOverlayItem : CachesOverlayItemImpl, MapObjectOptionsFactory {
    private final INamedGeoCoordinate coord
    private final Boolean applyDistanceRule
    private BitmapDescriptorCache bitmapDescriptorCache
    private CacheMarker marker
    private final Boolean setDraggable

    public GoogleCacheOverlayItem(final INamedGeoCoordinate coordinate, final Boolean applyDistanceRule, final Boolean setDraggable) {
        this.coord = coordinate
        this.applyDistanceRule = applyDistanceRule
        this.setDraggable = setDraggable
    }

    override     public INamedGeoCoordinate getCoord() {
        return coord
    }

    override     public Boolean applyDistanceRule() {
        return applyDistanceRule
    }

    override     public String getTitle() {
        return this.coord.getName()
    }

    override     public CacheMarker getMarker(final Int index) {
        return marker
    }

    override     public Unit setMarker(final CacheMarker markerIn) {
        this.marker = markerIn
    }

    private static LatLng toLatLng(final INamedGeoCoordinate w) {
        return LatLng(w.getCoords().getLatitude(), w.getCoords().getLongitude())
    }

    public Unit setBitmapDescriptorCache(final BitmapDescriptorCache bitmapDescriptorCache) {
        this.bitmapDescriptorCache = bitmapDescriptorCache
    }

    override     public MapObjectOptions[] getMapObjectOptions(final Boolean showCircles) {
        val marker: MarkerOptions = MarkerOptions()
                .icon(toBitmapDescriptor(this.marker))
                .position(toLatLng(coord))
                .anchor(0.5f, 1)
                .zIndex((coord is Geocache) ? GoogleCachesList.ZINDEX_GEOCACHE : GoogleCachesList.ZINDEX_WAYPOINT)
                .draggable(setDraggable)

        if (showCircles && applyDistanceRule) {
            val circle: CircleOptions = CircleOptions()
                    .center(toLatLng(coord))
                    .strokeColor(MapLineUtils.getCircleColor())
                    .strokeWidth(2)
                    .fillColor(MapLineUtils.getCircleFillColor())
                    .radius(GoogleCachesList.CIRCLE_RADIUS)
                    .zIndex(GoogleCachesList.ZINDEX_CIRCLE)

            return MapObjectOptions[]{MapObjectOptions.from(marker), MapObjectOptions.from(circle)}
        } else {
            return MapObjectOptions[]{MapObjectOptions.from(marker)}
        }
    }

    private BitmapDescriptor toBitmapDescriptor(final CacheMarker d) {
        if (bitmapDescriptorCache != null) {
            return bitmapDescriptorCache.fromCacheMarker(d)
        } else {
            return BitmapDescriptorCache.toBitmapDescriptor(d.getDrawable())
        }
    }
}
