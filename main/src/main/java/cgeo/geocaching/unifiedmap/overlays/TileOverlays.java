package cgeo.geocaching.unifiedmap.overlays;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Storage of the user-defined tile overlays.
 *
 * <p>The configured overlays live in a single preference holding a json array, while the subset
 * currently switched on in the map view is kept in a separate string set of overlay keys. Keeping
 * the two apart means toggling an overlay on the map never rewrites the configuration itself, and
 * mirrors how hidden tile providers are stored.</p>
 */
public final class TileOverlays {

    private TileOverlays() {
        //no instance
    }

    /** all configured overlays, in the order the user arranged them */
    @NonNull
    public static List<TileOverlay> getAll() {
        final List<TileOverlay> result = new ArrayList<>();
        final JsonNode root = JsonUtils.stringToNode(Settings.getTileOverlaysConfig(), true);
        if (root == null || !root.isArray()) {
            return result;
        }
        for (JsonNode node : root) {
            final TileOverlay overlay = TileOverlay.fromJson(node);
            if (overlay != null) {
                result.add(overlay);
            }
        }
        return result;
    }

    public static void save(@NonNull final List<TileOverlay> overlays) {
        final ArrayNode array = JsonUtils.createArrayNode();
        for (TileOverlay overlay : overlays) {
            array.add(overlay.toJson());
        }
        Settings.setTileOverlaysConfig(JsonUtils.nodeToString(array, true));

        // drop enabled-flags of overlays that no longer exist
        final Set<String> stillKnown = new HashSet<>();
        for (TileOverlay overlay : overlays) {
            stillKnown.add(overlay.getKey());
        }
        final Set<String> enabled = new HashSet<>(Settings.getTileOverlaysEnabled());
        if (enabled.retainAll(stillKnown)) {
            Settings.setTileOverlaysEnabled(enabled);
        }
    }

    /** the overlays that should currently be drawn, in configuration order */
    @NonNull
    public static List<TileOverlay> getEnabled() {
        final Set<String> enabled = Settings.getTileOverlaysEnabled();
        if (enabled.isEmpty()) {
            return Collections.emptyList();
        }
        final List<TileOverlay> result = new ArrayList<>();
        for (TileOverlay overlay : getAll()) {
            if (enabled.contains(overlay.getKey())) {
                result.add(overlay);
            }
        }
        return result;
    }

    public static boolean isEnabled(@NonNull final String key) {
        return Settings.getTileOverlaysEnabled().contains(key);
    }

    public static void setEnabled(@NonNull final String key, final boolean enabled) {
        final Set<String> current = new HashSet<>(Settings.getTileOverlaysEnabled());
        final boolean changed = enabled ? current.add(key) : current.remove(key);
        if (changed) {
            Settings.setTileOverlaysEnabled(current);
        }
    }

    /** adds a new overlay, or replaces the existing one carrying the same key */
    public static void addOrUpdate(@NonNull final TileOverlay overlay) {
        final List<TileOverlay> overlays = getAll();
        for (int i = 0; i < overlays.size(); i++) {
            if (overlays.get(i).getKey().equals(overlay.getKey())) {
                overlays.set(i, overlay);
                save(overlays);
                return;
            }
        }
        overlays.add(overlay);
        save(overlays);
    }

    public static void remove(@NonNull final String key) {
        final List<TileOverlay> overlays = getAll();
        boolean removed = false;
        for (int i = overlays.size() - 1; i >= 0; i--) {
            if (overlays.get(i).getKey().equals(key)) {
                overlays.remove(i);
                removed = true;
            }
        }
        if (removed) {
            save(overlays);
        }
    }

    @Nullable
    public static TileOverlay get(@NonNull final String key) {
        for (TileOverlay overlay : getAll()) {
            if (overlay.getKey().equals(key)) {
                return overlay;
            }
        }
        return null;
    }
}
