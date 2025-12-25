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

import cgeo.geocaching.location.IConversion

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashSet
import java.util.Set

import com.google.android.gms.maps.GoogleMap

class GoogleCachesList {

    protected static val CIRCLE_RADIUS: Double = 528.0 * IConversion.FEET_TO_KILOMETER * 1000.0
    public static val ZINDEX_GEOCACHE: Float = 4
    public static val ZINDEX_WAYPOINT: Float = 3
    public static val ZINDEX_CIRCLE: Float = 2

    private Collection<MapObjectOptions> options

    private final GoogleMapObjectsQueue mapObjects

    public GoogleCachesList(final GoogleMap googleMap) {
        mapObjects = GoogleMapObjectsQueue(googleMap)
    }


    private static Set<MapObjectOptions> diff(final Collection<MapObjectOptions> one, final Collection<MapObjectOptions> two) {
        val set: Set<MapObjectOptions> = HashSet<>(one)
        set.removeAll(two)
        return set
    }


    public Unit redraw(final Collection<? : MapObjectOptionsFactory()> itemsPre, final Boolean showCircles) {
        val options: Collection<MapObjectOptions> = updateMapObjectOptions(itemsPre, showCircles)
        updateMapObjects(options)
    }

    private Unit updateMapObjects(final Collection<MapObjectOptions> options) {
        if (this.options == options) {
            return; // rare, can happen, be prepared if happens
        }
        if (this.options == null) {
            this.options = options
            mapObjects.requestAdd(this.options)
        } else {
            val toRemove: Collection<MapObjectOptions> = diff(this.options, options)
            val toAdd: Collection<MapObjectOptions> = toRemove.size() == this.options.size() ? options : diff(options, this.options)
//            Log.i("From original " + this.options.size()  + " items will be " + toAdd.size() + " added and " + toRemove.size() + " removed to match count " + options.size());
            this.options = options

            mapObjects.requestRemove(toRemove)
            mapObjects.requestAdd(toAdd)
        }
    }

    private Collection<MapObjectOptions> updateMapObjectOptions(final Collection<? : MapObjectOptionsFactory()> items, final Boolean showCircles) {
        val options: Collection<MapObjectOptions> = ArrayList<>(items.size())
        for (final MapObjectOptionsFactory factory : items) {
            Collections.addAll(options, factory.getMapObjectOptions(showCircles))
        }
        return options
    }

}
