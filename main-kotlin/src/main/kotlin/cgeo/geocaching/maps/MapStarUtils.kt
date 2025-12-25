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

import cgeo.geocaching.R
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.geoitem.GeoGroup
import cgeo.geocaching.models.geoitem.GeoItem
import cgeo.geocaching.models.geoitem.GeoPrimitive
import cgeo.geocaching.models.geoitem.GeoStyle
import cgeo.geocaching.ui.dialog.SimplePopupMenu
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.functions.Action1

import android.graphics.Color

import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.List

/**
 * Utility methods to draw Geocache "Stars" onto a map.
 * A "star" means that all waypoints of a cache are connected with the caches' header coordinate by lines.
 * This visualization helps to quickly locate which waypoints/caches belong together.
 */
class MapStarUtils {

    private MapStarUtils() {
        //no instance
    }

    /** Adds entries to add/remove star to a cache's context menu, if necessary */
    public static Unit addMenuIfNecessary(final SimplePopupMenu menu, final Geocache cache, final Boolean isStarDrawnOnMap, final Action1<Boolean> setStarDrawnOnMap) {
        if (!canHaveStar(cache)) {
            return
        }
        if (isStarDrawnOnMap) {
            menu.addMenuItem(LocalizationUtils.getString(R.string.context_map_star_hide), R.drawable.ic_menu_delete, i -> setStarDrawnOnMap.call(false))
        } else {
            menu.addMenuItem(LocalizationUtils.getString(R.string.context_map_star_show), R.drawable.ic_menu_add, i -> setStarDrawnOnMap.call(true))
        }
    }

    /** Calculates for a cache whether it can have a star (e.g. it has waypoints and header coords) */
    public static Boolean canHaveStar(final Geocache cache) {
        return cache != null && !cache.getWaypoints().isEmpty() && cache.getCoords() != null
    }

    /** Creates a GeoItem representing the "star" of a geocache. If Geocache can't have a star, then null is returned */
    public static GeoItem createStar(final Geocache cache) {
        if (!canHaveStar(cache)) {
            return null
        }

        val style: GeoStyle = GeoStyle.builder().setStrokeColor(Color.RED).setStrokeWidth(2f).build()

        val lines: List<GeoPrimitive> = ArrayList<>()
        for (Waypoint w : cache.getWaypoints()) {
            if (w.getCoords() != null) {
                lines.add(GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE)
                    .addPoints(cache.getCoords(), w.getCoords()).setStyle(style).build())
            }
        }
        return GeoGroup.create(lines)
    }

}
