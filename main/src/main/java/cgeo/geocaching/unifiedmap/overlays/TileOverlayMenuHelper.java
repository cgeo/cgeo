package cgeo.geocaching.unifiedmap.overlays;

import cgeo.geocaching.R;

import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds the configured tile overlays to the map view popup menu.
 *
 * <p>Overlays are independent of each other, so unlike the map sources they are not part of a
 * single-choice group. Menu ids are handed out from a band of their own, well away from the ids
 * the tile providers use, and are resolved back to overlay keys via {@link #getOverlayKey(int)}.</p>
 */
public final class TileOverlayMenuHelper {

    /** tile providers count up from -1000000000, so this band cannot be reached by them */
    private static final int MENU_ID_BASE = -1500000000;

    /**
     * Menu items are sorted by this value, not by their group - the groups only control dividers
     * and checkable behaviour. The map sources count up from 0 and the offline map entries
     * start at 90, so this keeps the overlays after all map sources - which matters because
     * those are a single-choice group and must not be split by the overlay checkboxes.
     */
    private static final int MENU_ORDER = 50;

    private static final Map<Integer, String> menuIdToOverlayKey = new HashMap<>();

    private TileOverlayMenuHelper() {
        //no instance
    }

    public static void addMenuItems(@NonNull final Menu menu) {
        synchronized (menuIdToOverlayKey) {
            menuIdToOverlayKey.clear();

            final List<TileOverlay> overlays = TileOverlays.getAll();
            for (int i = 0; i < overlays.size(); i++) {
                final TileOverlay overlay = overlays.get(i);
                final int menuId = MENU_ID_BASE + i;
                menuIdToOverlayKey.put(menuId, overlay.getKey());
                menu.add(R.id.menu_group_tile_overlays, menuId, MENU_ORDER, overlay.getDisplayName())
                        .setCheckable(true)
                        .setChecked(TileOverlays.isEnabled(overlay.getKey()));
            }
        }
    }

    /**
     * Resolves a menu item id back to the overlay it stands for.
     *
     * @return the overlay key, or {@code null} if the id does not belong to an overlay
     */
    @Nullable
    public static String getOverlayKey(final int menuId) {
        synchronized (menuIdToOverlayKey) {
            return menuIdToOverlayKey.get(menuId);
        }
    }
}
