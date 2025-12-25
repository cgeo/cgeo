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

package cgeo.geocaching.maps.interfaces

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport

import android.content.Context
import android.location.Location
import android.os.Bundle
import android.view.View

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity

import java.util.Collection

import javax.annotation.Nullable

/**
 * Defines common functions of the provider-specific
 * MapView implementations
 */
interface MapViewImpl<T : CachesOverlayItemImpl()> {

    Unit setBuiltInZoomControls(Boolean b)

    Unit zoomInOut(Boolean zoomIn)

    Unit clearOverlays()

    MapControllerImpl getMapController()

    Unit destroyDrawingCache()

    GeoPointImpl getMapViewCenter()

    Int getLatitudeSpan()

    Int getLongitudeSpan()

    Int getMapZoomLevel()

    Unit zoomToBounds(Viewport bounds, GeoPointImpl center)

    Float getBearing()

    Int getWidth()

    Int getHeight()

    Unit setDestinationCoords(Geopoint destCoords)

    Unit setCoordinates(Location coordinates)

    MapProjectionImpl getMapProjection()

    Context getContext()

    PositionAndHistory createAddPositionAndScaleOverlay(View root, Geopoint coords, String geocode)

    Unit setMapSource()

    /**
     * Map-library unspecific method to request a repaint of either
     * a specific overlay, that changed, or the mapview as a whole
     * (if overlay is null)
     *
     * @param overlay Overlay to repaint or null if the mapview has changed
     */
    Unit repaintRequired(GeneralOverlay overlay)

    Unit setOnDragListener(OnMapDragListener onDragListener)

    /**
     * Indicates if overlay text or line colours should be dark (normal case)
     * or light (inverted case)
     *
     * @return true - text/draw in light colors, false text/draw in dark colors
     */
    Boolean needsInvertedColors()

    Viewport getViewport()

    Unit onMapReady(MapReadyCallback callback)

    Unit updateItems(Collection<T> itemsPre)

    Boolean getCircles()

    Unit setCircles(Boolean showCircles)

    Unit setOnTapListener(OnCacheTapListener listener)

    Unit setListId(Int listId)

    Unit setCoordsMarker(Geopoint coords)

    /* From Google MapView documentation:
     * Users of this class must forward all the life cycle methods from the Activity or Fragment
     * containing this view to the corresponding ones in this class. In particular, you must
     * forward on the following methods:
     */
    Unit onCreate(Bundle b)

    Unit onResume()

    Unit onPause()

    Unit onDestroy()

    Unit onSaveInstanceState(Bundle b)

    Unit onLowMemory()

    Unit selectMapTheme(AppCompatActivity activity)

}
