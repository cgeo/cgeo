package cgeo.geocaching.unifiedmap.overlays;

import cgeo.geocaching.unifiedmap.tiles.TileUrlUtils;
import cgeo.geocaching.utils.JsonUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

/**
 * A user-defined tile overlay: a raster tile source drawn on top of the current map source.
 *
 * <p>Unlike a tile provider an overlay does not replace the base map, so several overlays can be
 * active at the same time. Tiles are expected to be transparent where they carry no information.</p>
 */
public class TileOverlay {

    private static final String JSON_KEY = "key";
    private static final String JSON_NAME = "name";
    private static final String JSON_URL = "url";

    /** stable identifier, used to remember which overlays are switched on */
    private final String key;
    private final String name;
    private final String url;

    public TileOverlay(@NonNull final String key, @NonNull final String name, @NonNull final String url) {
        this.key = key;
        this.name = name;
        this.url = url;
    }

    /** creates a new overlay with a freshly generated key */
    @NonNull
    public static TileOverlay create(@NonNull final String name, @NonNull final String url) {
        return new TileOverlay(UUID.randomUUID().toString(), name, url);
    }

    @NonNull
    public String getKey() {
        return key;
    }

    /** the name as entered by the user, which may be empty */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * The name to show in the settings and in the map's layer selection: the user-given name, or
     * the host of the url when no name was entered.
     *
     * <p>Derived on read rather than stored, so that correcting a typo in the url also corrects a
     * name that was never explicitly set.</p>
     */
    @NonNull
    public String getDisplayName() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        final String host = TileUrlUtils.host(url);
        return host != null ? host : url;
    }

    /** the normalised XYZ url template, see {@link TileUrlUtils} */
    @NonNull
    public String getUrl() {
        return url;
    }

    @NonNull
    public TileOverlay withName(@NonNull final String newName) {
        return new TileOverlay(key, newName, url);
    }

    @NonNull
    public TileOverlay withUrl(@NonNull final String newUrl) {
        return new TileOverlay(key, name, newUrl);
    }

    @NonNull
    public ObjectNode toJson() {
        final ObjectNode node = JsonUtils.createObjectNode();
        JsonUtils.setText(node, JSON_KEY, key);
        JsonUtils.setText(node, JSON_NAME, name);
        JsonUtils.setText(node, JSON_URL, url);
        return node;
    }

    /**
     * Reads an overlay from its json representation.
     *
     * @return the overlay, or {@code null} if the node is incomplete or holds an unusable url
     */
    @Nullable
    public static TileOverlay fromJson(@Nullable final JsonNode node) {
        if (node == null) {
            return null;
        }
        final String key = JsonUtils.getText(node, JSON_KEY, null);
        final String name = JsonUtils.getText(node, JSON_NAME, null);
        final String url = TileUrlUtils.normalize(JsonUtils.getText(node, JSON_URL, null));
        if (StringUtils.isBlank(key) || url == null) {
            return null;
        }
        return new TileOverlay(key, StringUtils.defaultString(name), url);
    }

    @NonNull
    @Override
    public String toString() {
        return getDisplayName() + " (" + url + ")";
    }
}
