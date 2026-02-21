/*
 File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
 Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package cz.matejcik.openwig.platform
 */
package cgeo.geocaching.wherigo.openwig.platform;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Screen identifiers for {@link UI#showScreen(UIScreen, cgeo.geocaching.wherigo.openwig.EventTable)}.
 * The numeric {@link #getId()} value matches the Wherigo protocol constants used by the Lua VM.
 */
public enum UIScreen {
    MAINSCREEN(0),
    DETAILSCREEN(1),
    INVENTORYSCREEN(2),
    ITEMSCREEN(3),
    LOCATIONSCREEN(4),
    TASKSCREEN(5);

    private final int id;

    private static final Map<Integer, UIScreen> ID_MAP = new HashMap<>();

    static {
        for (final UIScreen screen : values()) {
            ID_MAP.put(screen.id, screen);
        }
    }

    UIScreen(final int id) {
        this.id = id;
    }

    /** Returns the numeric Lua protocol value for this screen. */
    public int getId() {
        return id;
    }

    /** Returns the {@code UIScreen} whose {@link #getId()} equals {@code id}, or {@code null} if unknown. */
    @Nullable
    public static UIScreen fromId(final int id) {
        return ID_MAP.get(id);
    }
}
