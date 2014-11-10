package cgeo.geocaching.maps;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import java.util.EnumSet;

/**
 * Defines the strategy for the Live Map
 */
public interface LiveMapStrategy {

    public enum StrategyFlag {
        LOAD_TILES, // 2x2 tiles filling the complete viewport
        PARSE_TILES, // parse PNG images
        SEARCH_NEARBY // searchByCoords()
    }

    public enum Strategy {
        FASTEST(1, EnumSet.of(StrategyFlag.LOAD_TILES), R.string.map_strategy_fastest),
        FAST(2, EnumSet.of(StrategyFlag.LOAD_TILES, StrategyFlag.PARSE_TILES), R.string.map_strategy_fast),
        AUTO(3, EnumSet.noneOf(StrategyFlag.class), R.string.map_strategy_auto),
        DETAILED(4, EnumSet.allOf(StrategyFlag.class), R.string.map_strategy_detailed);

        public final int id;
        public final EnumSet<StrategyFlag> flags;
        private final int stringId;

        Strategy(int id, EnumSet<StrategyFlag> flags, int stringId) {
            this.id = id;
            this.flags = flags;
            this.stringId = stringId;
        }

        public static Strategy getById(final int id) {
            for (Strategy strategy : Strategy.values()) {
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
}
