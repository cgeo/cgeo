package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.cgCache;

public class TerrainFilterTest extends CGeoTestCase {

    public static void testTerrainFilter() {
        final cgCache easy = new cgCache();
        easy.setTerrain(1.5f);

        final cgCache hard = new cgCache();
        hard.setTerrain(5f);

        final AbstractRangeFilter easyFilter = new TerrainFilter(1);

        assertTrue(easyFilter.accepts(easy));
        assertFalse(easyFilter.accepts(hard));
    }

    public static void testAllFilters() {
        assertTrue(new TerrainFilter.Factory().getFilters().length == 5); // terrain ranges from 1 to 5
    }
}
