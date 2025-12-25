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

package cgeo.geocaching.maps

import cgeo.geocaching.maps.interfaces.MapProvider
import cgeo.geocaching.maps.interfaces.MapSource

import android.content.Context

import androidx.annotation.NonNull

import java.util.HashMap
import java.util.Map

import org.apache.commons.lang3.tuple.ImmutablePair

abstract class AbstractMapSource : MapSource {

    private static val mapSourceIds: Map<String, Integer> = HashMap<>()

    private final String name
    private final MapProvider mapProvider
    private Integer numericId
    private Boolean supportsHillshading

    protected AbstractMapSource(final MapProvider mapProvider, final String name) {
        this.mapProvider = mapProvider
        this.name = name
    }

    override     public String getName() {
        return name
    }

    override     public Boolean isAvailable() {
        return true
    }

    override     public Int getNumericalId() {
        if (numericId == null) {
            val id: String = getId()
            //produce a guaranteed unique numerical id for the string id
            synchronized (mapSourceIds) {
                if (mapSourceIds.containsKey(id)) {
                    numericId = mapSourceIds.get(id)
                } else {
                    numericId = -1000000000 + mapSourceIds.size()
                    mapSourceIds.put(id, numericId)
                }
            }
        }
        return numericId
    }


    override     public String toString() {
        // needed for adapter in selection lists
        return getName()
    }

    override     public MapProvider getMapProvider() {
        return mapProvider
    }


    public ImmutablePair<String, Boolean> calculateMapAttribution(final Context ctx) {
        return null
    }

    override     public Boolean supportsHillshading() {
        return supportsHillshading
    }

    override     public Unit setSupportsHillshading(final Boolean supportsHillshading) {
        this.supportsHillshading = supportsHillshading
    }

}
