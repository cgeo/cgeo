package cgeo.geocaching.pebble;

import androidx.core.util.Consumer;

/**
 * Implemented by the map activity to provide map capture and control to the Pebble handler.
 */
public interface PebbleMapHost {
    void capturePebbleMap(Consumer<byte[]> callback);

    void zoomIn();

    void zoomOut();

    void setZoom(int zoomLevel);
}
