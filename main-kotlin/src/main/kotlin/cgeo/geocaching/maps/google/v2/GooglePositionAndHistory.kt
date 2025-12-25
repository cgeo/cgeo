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

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.GeopointConverter
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.maps.PositionHistory
import cgeo.geocaching.maps.Tracks
import cgeo.geocaching.maps.interfaces.PositionAndHistory
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.models.IndividualRoute
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.TrailHistoryElement
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.IGeoItemSupplier
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemTestLayer
import cgeo.geocaching.unifiedmap.geoitemlayer.GoogleV2GeoItemLayer
import cgeo.geocaching.utils.AngleUtils
import cgeo.geocaching.utils.GeoHeightUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MapLineUtils
import cgeo.geocaching.settings.Settings.MAPROTATION_AUTO_LOWPOWER
import cgeo.geocaching.settings.Settings.MAPROTATION_AUTO_PRECISE
import cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location

import androidx.core.content.res.ResourcesCompat

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Objects

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class GooglePositionAndHistory : PositionAndHistory, Tracks.UpdateTrack, IndividualRoute.UpdateIndividualRoute {

    public static val ZINDEX_DIRECTION_LINE: Float = 5
    public static val ZINDEX_POSITION: Float = 11
    public static val ZINDEX_POSITION_ELEVATION: Float = 10
    public static val ZINDEX_TRACK: Float = 6
    public static val ZINDEX_ROUTE: Float = 5
    public static val ZINDEX_POSITION_ACCURACY_CIRCLE: Float = 3
    public static val ZINDEX_HISTORY: Float = 2

    private static val KEY_INDIVIDUAL_ROUTE: String = "INDIVIDUALROUTE"

    /**
     * maximum distance (in meters) up to which two points in the trail get connected by a drawn line
     */
    private static val LINE_MAXIMUM_DISTANCE_METERS: Float = 10000

    private static val GP_CONVERTER: GeopointConverter<LatLng> = GeopointConverter<>(
            gc -> LatLng(gc.getLatitude(), gc.getLongitude()),
            ll -> Geopoint(ll.latitude, ll.longitude)
    )

    private Location coordinates
    private Float heading
    private val history: PositionHistory = PositionHistory()

    private LatLng longTapLatLng

    // settings for map auto rotation
    private var lastBearingCoordinates: Location = null
    private var mapRotation: Int = MAPROTATION_MANUAL

    private static val MAX_HISTORY_POINTS: Int = 230; // TODO add alpha, make changeable in constructor?

    private var mapRef: WeakReference<GoogleMap> = null
    private final GoogleMapObjects positionObjs
    private final GoogleMapObjects longTapObjs
    private final GoogleMapObjects historyObjs
    private final GoogleMapObjects routeObjs
    private final GoogleMapObjects trackObjs

    private val testLayer: GeoItemTestLayer = GeoItemTestLayer()
    private final GoogleMapView mapView
    private GoogleMapView.PostRealDistance postRealDistance = null
    private GoogleMapView.PostRealDistance postRouteDistance = null

    private var lastViewport: Viewport = null

    val individualRoutePoints: ArrayList<RouteItem> = ArrayList<>()

    private val cache: HashMap<String, CachedRoute> = HashMap<>()

    private static class CachedRoute {
        private var isHidden: Boolean = false
        private List<List<LatLng>> track = null
        private Int color
        private Int width
    }

    public GooglePositionAndHistory(final GoogleMap googleMap, final GoogleMapView mapView, final GoogleMapView.PostRealDistance postRealDistance, final GoogleMapView.PostRealDistance postRouteDistance) {
        this.mapRef = WeakReference<>(googleMap)
        positionObjs = GoogleMapObjects(googleMap)
        longTapObjs = GoogleMapObjects(googleMap)
        historyObjs = GoogleMapObjects(googleMap)
        routeObjs = GoogleMapObjects(googleMap)
        trackObjs = GoogleMapObjects(googleMap)
        testLayer.init(GoogleV2GeoItemLayer(googleMap))
        this.mapView = mapView
        this.postRealDistance = postRealDistance
        this.postRouteDistance = postRouteDistance
        mapView.setDistanceDrawer(null)
        updateMapRotation()
    }

    override     public Unit setCoordinates(final Location coord) {
        val coordChanged: Boolean = !Objects == (coord, coordinates)
        coordinates = coord
        if (coordChanged) {
            history.rememberTrailPosition(coordinates)
            mapView.setCoordinates(coordinates)

            if (mapRotation == MAPROTATION_AUTO_LOWPOWER || mapRotation == MAPROTATION_AUTO_PRECISE) {
                if (null != lastBearingCoordinates) {
                    val map: GoogleMap = mapRef.get()
                    if (null != map) {
                        val currentCameraPosition: CameraPosition = map.getCameraPosition()
                        val bearing: Float = AngleUtils.normalize(lastBearingCoordinates.bearingTo(coordinates))
                        val bearingDiff: Float = Math.abs(AngleUtils.difference(bearing, currentCameraPosition.bearing))
                        if (bearingDiff > 15.0f) {
                            lastBearingCoordinates = coordinates
                            // adjust bearing of map, keep position and zoom level
                            val cameraPosition: CameraPosition = CameraPosition.Builder()
                                    .target(currentCameraPosition.target)
                                    .zoom(currentCameraPosition.zoom)
                                    .bearing(bearing)
                                    .tilt(0)
                                    .build()
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                        }
                    } else {
                        lastBearingCoordinates = null
                    }
                } else {
                    lastBearingCoordinates = coordinates
                }
            }
        }
    }

    public Unit updateMapRotation() {
        this.mapRotation = Settings.getMapRotation()
        val map: GoogleMap = mapRef.get()
        if (null != map) {
            map.getUiSettings().setRotateGesturesEnabled(mapRotation == MAPROTATION_MANUAL)
        }
    }

    override     public Location getCoordinates() {
        return coordinates
    }

    override     public Unit setHeading(final Float heading) {
        if (this.heading != heading) {
            this.heading = heading
        }
    }

    override     public Float getHeading() {
        return heading
    }

    override     public Unit setLongTapLatLng(final LatLng latLng) {
        longTapLatLng = latLng
        repaintRequired()
    }

    override     public LatLng getLongTapLatLng() {
        return longTapLatLng
    }

    override     public Unit resetLongTapLatLng() {
        longTapLatLng = null
        repaintRequired()
    }

    override     public ArrayList<TrailHistoryElement> getHistory() {
        return history.getHistory()
    }

    override     public Unit setHistory(final ArrayList<TrailHistoryElement> history) {
        if (history != this.history.getHistory()) {
            this.history.setHistory(history)
        }
    }

    override     public Unit updateIndividualRoute(final IndividualRoute route) {
        individualRoutePoints.clear()
        for (RouteItem item : route.getRouteItems()) {
            if (item.getType() == RouteItem.RouteItemType.COORDS) {
                individualRoutePoints.add(item)
            }
        }
        updateRoute(KEY_INDIVIDUAL_ROUTE, route, MapLineUtils.getRouteColor(), MapLineUtils.getRawRouteLineWidth())
        if (postRouteDistance != null) {
            postRouteDistance.postRealDistance(route.getDistance())
        }
    }

    override     public Unit updateRoute(final String key, final IGeoItemSupplier track, final Int color, final Int width) {
        synchronized (cache) {
            CachedRoute c = cache.get(key)
            if (c == null) {
                c = CachedRoute()
                cache.put(key, c)
            }
            c.track = null
            if (track != null) {
                c.track = toLatLng(track)
                c.isHidden = track.isHidden()
            }
            c.color = color
            c.width = width
        }
        repaintRequired()
    }

    private static ArrayList<List<LatLng>> toLatLng(final IGeoItemSupplier gg) {
        final ArrayList<List<LatLng>> list = ArrayList<>()
        GeoGroup.forAllPrimitives(gg.getItem(), go -> list.add(GP_CONVERTER.toList(go.getPoints())))
        return list
    }

    public Unit removeRoute(final String key) {
        synchronized (cache) {
            cache.remove(key)
        }
    }

    public Unit setHidden(final String key, final Boolean isHidden) {
        synchronized (cache) {
            val c: CachedRoute = cache.get(key)
            if (c != null) {
                c.isHidden = isHidden
            }
        }
    }

    override     public Unit repaintRequired() {
        drawPosition()
        drawHistory()
        drawViewport(lastViewport)
        drawRouteAndTracks()
        drawLongTapMarker()
    }

    private PolylineOptions getDirectionPolyline(final Geopoint from, final Geopoint to) {
        val options: PolylineOptions = PolylineOptions()
                .width(MapLineUtils.getDirectionLineWidth(false))
                .color(MapLineUtils.getDirectionColor())
                .zIndex(ZINDEX_DIRECTION_LINE)
                .add(LatLng(from.getLatitude(), from.getLongitude()))

        final Geopoint[] routingPoints = Routing.getTrack(from, to, null)

        if (routingPoints.length > 1) {
            // calculate polyline to draw
            for (Int i = 1; i < routingPoints.length; i++) {
                options.add(LatLng(routingPoints[i].getLatitude(), routingPoints[i].getLongitude()))
            }

            // calculate distance
            if (null != postRealDistance) {
                Float distance = 0.0f
                for (Int i = 1; i < routingPoints.length; i++) {
                    distance += routingPoints[i - 1].distanceTo(routingPoints[i])
                }
                postRealDistance.postRealDistance(distance)
            }
        }

        options.add(LatLng(to.getLatitude(), to.getLongitude()))
        return options
    }

    private synchronized Unit drawPosition() {
        positionObjs.removeAll()
        if (this.coordinates == null) {
            return
        }

        val latLng: LatLng = LatLng(coordinates.getLatitude(), coordinates.getLongitude())

        // accuracy circle
        positionObjs.addCircle(CircleOptions()
                .center(latLng)
                .strokeColor(MapLineUtils.getAccuracyCircleColor())
                .strokeWidth(3)
                .fillColor(MapLineUtils.getAccuracyCircleFillColor())
                .radius(coordinates.getAccuracy())
                .zIndex(ZINDEX_POSITION_ACCURACY_CIRCLE)
        )

        val positionMarker: Drawable = ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.my_location_chevron, null)
        positionObjs.addMarker(MarkerOptions()
                .icon(BitmapDescriptorCache.toBitmapDescriptor(positionMarker))
                .position(latLng)
                .rotation(heading)
                .anchor(0.5f, 0.5f)
                .flat(true)
                .zIndex(ZINDEX_POSITION)
        )

        val destCoords: Geopoint = mapView.getDestinationCoords()
        if (destCoords != null) {
            val currentCoords: Geopoint = Geopoint(coordinates)
            if (Settings.isMapDirection()) {
                // draw direction line
                positionObjs.addPolyline(getDirectionPolyline(currentCoords, destCoords))
            } else if (null != postRealDistance) {
                postRealDistance.postRealDistance(destCoords.distanceTo(currentCoords))
            }
        }

        if (coordinates.hasAltitude() && Settings.showElevation()) {
            val elevationInfo: Bitmap = MapUtils.getElevationBitmap(CgeoApplication.getInstance().getResources(), positionMarker.getIntrinsicHeight(), GeoHeightUtils.getAltitude(coordinates))
            positionObjs.addMarker(MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(elevationInfo))
                    .position(latLng)
                    .anchor(0.5f, 0.5f)
                    .flat(false)
                    .zIndex(ZINDEX_POSITION_ELEVATION))
        }
    }

    private synchronized Unit drawHistory() {
        if (null == coordinates) {
            return
        }
        historyObjs.removeAll()
        if (Settings.isMapTrail()) {
            try {
                val paintHistory: ArrayList<TrailHistoryElement> = ArrayList<>(getHistory())
                val size: Int = paintHistory.size()
                if (size < 2) {
                    return
                }
                // always add current position to drawn history to have a closed connection, even if it's not yet recorded
                paintHistory.add(TrailHistoryElement(coordinates))

                Location prev = paintHistory.get(0).getLocation()
                Int current = 1
                while (current < size) {
                    val points: List<LatLng> = ArrayList<>(MAX_HISTORY_POINTS)
                    points.add(LatLng(prev.getLatitude(), prev.getLongitude()))

                    Boolean paint = false
                    while (!paint && current < size) {
                        val now: Location = paintHistory.get(current).getLocation()
                        current++
                        if (now.distanceTo(prev) < LINE_MAXIMUM_DISTANCE_METERS) {
                            points.add(LatLng(now.getLatitude(), now.getLongitude()))
                        } else {
                            paint = true
                        }
                        prev = now
                    }
                    if (points.size() > 1) {
                        // history line
                        historyObjs.addPolyline(PolylineOptions()
                                .addAll(points)
                                .color(MapLineUtils.getTrailColor())
                                .width(MapLineUtils.getHistoryLineWidth(false))
                                .zIndex(ZINDEX_HISTORY)
                        )
                    }
                }
            } catch (OutOfMemoryError ignore) {
                Log.e("drawHistory: out of memory, please reduce max track history size")
                // better do not draw history than crash the map
            }
        }
    }

    public synchronized Unit drawViewport(final Viewport viewport) {
        if (null == viewport) {
            return
        }
        val options: PolylineOptions = PolylineOptions()
                .width(MapLineUtils.getDebugLineWidth())
                .color(0x80EB391E)
                .zIndex(ZINDEX_DIRECTION_LINE)
                .add(LatLng(viewport.getLatitudeMin(), viewport.getLongitudeMin()))
                .add(LatLng(viewport.getLatitudeMin(), viewport.getLongitudeMax()))
                .add(LatLng(viewport.getLatitudeMax(), viewport.getLongitudeMax()))
                .add(LatLng(viewport.getLatitudeMax(), viewport.getLongitudeMin()))
                .add(LatLng(viewport.getLatitudeMin(), viewport.getLongitudeMin()))

        positionObjs.addPolyline(options)
        lastViewport = viewport
    }

    private synchronized Unit drawRouteAndTracks() {
        // draw individual route
        routeObjs.removeAll()
        val individualRoute: CachedRoute = cache.get(KEY_INDIVIDUAL_ROUTE)
        if (individualRoute != null && !individualRoute.isHidden && individualRoute.track != null && !individualRoute.track.isEmpty()) {
            for (List<LatLng> segment : individualRoute.track) {
                routeObjs.addPolyline(PolylineOptions()
                        .addAll(segment)
                        .color(MapLineUtils.getRouteColor())
                        .width(MapLineUtils.getRouteLineWidth(false))
                        .zIndex(ZINDEX_ROUTE)
                )
            }
            for (RouteItem item : individualRoutePoints) {
                routeObjs.addMarker(MarkerOptions()
                        .icon(BitmapDescriptorCache.toBitmapDescriptor(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.marker_routepoint, null)))
                        .position(GP_CONVERTER.to(item.getPoint()))
                        .anchor(0.5f, 0.5f))
            }
        }
        // draw tracks
        trackObjs.removeAll()
        synchronized (cache) {
            for (CachedRoute c : cache.values()) {
                // route hidden, no route or route too Short?
                if (c != individualRoute && !c.isHidden && c.track != null && !c.track.isEmpty()) {
                    for (List<LatLng> segment : c.track) {
                        trackObjs.addPolyline(PolylineOptions()
                                .addAll(segment)
                                .color(c.color)
                                .width(MapLineUtils.getWidthFromRaw(c.width, false))
                                .zIndex(ZINDEX_TRACK)
                        )
                    }
                }
            }
        }
    }

    private synchronized Unit drawLongTapMarker() {
        longTapObjs.removeAll()
        if (longTapLatLng != null) {
            positionObjs.addMarker(MarkerOptions()
                    .icon(BitmapDescriptorCache.toBitmapDescriptor(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.map_pin, null)))
                    .position(longTapLatLng)
                    .anchor(0.5f, 1f)
                    .zIndex(ZINDEX_POSITION)
            )
        }
    }
}
