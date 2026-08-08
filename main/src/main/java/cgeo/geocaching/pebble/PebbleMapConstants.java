package cgeo.geocaching.pebble;

import java.util.UUID;

/**
 * Constants shared between c:geo's Pebble map export and the PebbleOS watchapp.
 */
public final class PebbleMapConstants {

    private PebbleMapConstants() {
        // utility class
    }

    public static final UUID PEBBLE_MAP_APP_UUID = UUID.fromString("9ec749ec-29ea-4c42-9b4b-9e1f0f1a1b0c");

    public static final int CHUNK_SIZE = 1000;
    public static final int MAP_WIDTH = 200;
    public static final int MAP_HEIGHT = 228;

    public static final int KEY_COMMAND = 0;
    public static final int KEY_VALUE = 1;
    public static final int KEY_CHUNK_INDEX = 2;
    public static final int KEY_CHUNK_TOTAL = 3;
    public static final int KEY_CHUNK_DATA = 4;
    public static final int KEY_FRAME_ID = 5;

    public static final int CMD_REFRESH = 0;
    public static final int CMD_ZOOM_IN = 1;
    public static final int CMD_ZOOM_OUT = 2;
    public static final int CMD_SET_INTERVAL = 3;
    public static final int CMD_SET_ZOOM = 4;
}
