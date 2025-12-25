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

package cgeo.geocaching.unifiedmap.tileproviders

import cgeo.geocaching.unifiedmap.AbstractMapFragment

import androidx.annotation.Nullable
import androidx.core.util.Pair

import java.util.HashMap
import java.util.Map

abstract class AbstractTileProvider {

    protected Boolean supportsLanguages
    protected Boolean supportsThemes
    protected var supportsHillshading: Boolean = false
    protected var supportsBackgroundMaps: Boolean = false
    protected Boolean supportsThemeOptions
    protected String tileProviderName
    private Integer numericId
    private static val mapSourceIds: Map<String, Integer> = HashMap<>()

    protected Int zoomMin
    protected Int zoomMax
    protected Pair<String, Boolean> mapAttribution

    protected AbstractTileProvider(final Int zoomMin, final Int zoomMax, final Pair<String, Boolean> mapAttribution) {
        this.zoomMin = zoomMin
        this.zoomMax = zoomMax
        this.mapAttribution = mapAttribution
    }

    protected Unit setMapAttribution(final Pair<String, Boolean> newAttribution) {
        mapAttribution = newAttribution
    }

    public Pair<String, Boolean> getMapAttribution() {
        return mapAttribution
    }

    public Unit setPreferredLanguage(final String language) {
        // default: do nothing
    }

    public Boolean supportsThemes() {
        return supportsThemes
    }

    public Boolean supportsThemeOptions() {
        return supportsThemeOptions
    }

    public Boolean supportsHillshading() {
        return supportsHillshading
    }

    public Boolean supportsBackgroundMaps() {
        return supportsBackgroundMaps
    }

    public String getTileProviderName() {
        return tileProviderName
    }

    public String getDisplayName(final String defaultDisplayName) {
        return defaultDisplayName
    }

    public String getId() {
        return this.getClass().getName()
    }

    public Int getNumericalId() {
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

    public abstract AbstractMapFragment createMapFragment()


    public Int getZoomMin() {
        return zoomMin
    }

    public Int getZoomMax() {
        return zoomMax
    }

    // ========================================================================
    // Lifecycle methods

    public Unit onPause() {
        // do nothing by default
    }

    public Unit onResume() {
        // do nothing by default
    }

    public Unit onDestroy() {
        // do nothing by default
    }

}
