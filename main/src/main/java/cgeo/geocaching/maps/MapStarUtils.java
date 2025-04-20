package cgeo.geocaching.maps;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.GeoGroup;
import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoPrimitive;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.ui.dialog.SimplePopupMenu;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.graphics.Color;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods to draw Geocache "Stars" onto a map.
 * A "star" means that all waypoints of a cache are connected with the caches' header coordinate by lines.
 * This visualization helps to quickly locate which waypoints/caches belong together.
 */
public final class MapStarUtils {

    private MapStarUtils() {
        //no instance
    }

    /** Adds entries to add/remove star to a cache's context menu, if necessary */
    public static void addMenuIfNecessary(final SimplePopupMenu menu, final Geocache cache, final boolean isStarDrawnOnMap, final Action1<Boolean> setStarDrawnOnMap) {
        if (!canHaveStar(cache)) {
            return;
        }
        if (isStarDrawnOnMap) {
            menu.addMenuItem(LocalizationUtils.getString(R.string.context_map_star_hide), R.drawable.ic_menu_delete, i -> setStarDrawnOnMap.call(false));
        } else {
            menu.addMenuItem(LocalizationUtils.getString(R.string.context_map_star_show), R.drawable.ic_menu_add, i -> setStarDrawnOnMap.call(true));
        }
    }

    /** Calculates for a cache whether it can have a star (e.g. it has waypoints and header coords) */
    public static boolean canHaveStar(final Geocache cache) {
        return cache != null && !cache.getWaypoints().isEmpty() && cache.getCoords() != null;
    }

    /** Creates a GeoItem representing the "star" of a geocache. If Geocache can't have a star, then null is returned */
    @Nullable
    public static GeoItem createStar(final Geocache cache) {
        if (!canHaveStar(cache)) {
            return null;
        }

        final GeoStyle style = GeoStyle.builder().setStrokeColor(Color.RED).setStrokeWidth(2f).build();

        final List<GeoPrimitive> lines = new ArrayList<>();
        for (Waypoint w : cache.getWaypoints()) {
            if (w.getCoords() != null) {
                lines.add(GeoPrimitive.builder().setType(GeoItem.GeoType.POLYLINE)
                    .addPoints(cache.getCoords(), w.getCoords()).setStyle(style).build());
            }
        }
        return GeoGroup.create(lines);
    }

}
