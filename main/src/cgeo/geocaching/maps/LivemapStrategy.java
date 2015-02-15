package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import java.util.EnumSet;

/**
 * Defines the strategy for the Live Map
 */
public enum LivemapStrategy {
    FASTEST(1, EnumSet.of(Flag.LOAD_TILES), R.string.map_strategy_fastest),
    FAST(2, EnumSet.of(Flag.LOAD_TILES, Flag.PARSE_TILES), R.string.map_strategy_fast),
    AUTO(3, EnumSet.noneOf(Flag.class), R.string.map_strategy_auto),
    DETAILED(4, EnumSet.allOf(Flag.class), R.string.map_strategy_detailed);

    public final int id;
    public final EnumSet<Flag> flags;
    private final int stringId;

    public enum Flag {
        LOAD_TILES, // 2x2 tiles filling the complete viewport
        PARSE_TILES, // parse PNG images
        SEARCH_NEARBY // searchByCoords()
    }

    LivemapStrategy(final int id, final EnumSet<Flag> flags, final int stringId) {
        this.id = id;
        this.flags = flags;
        this.stringId = stringId;
    }

    public static LivemapStrategy getById(final int id) {
        for (final LivemapStrategy strategy : LivemapStrategy.values()) {
            if (strategy.id == id) {
                return strategy;
            }
        }
        return AUTO;
    }

    public final String getL10n() {
        return CgeoApplication.getInstance().getBaseContext().getResources().getString(stringId);
    }
}
