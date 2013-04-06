package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;

public class TerrainFilterTest extends CGeoTestCase {

    public static void testTerrainFilter() {
        final Geocache easy = new Geocache();
        easy.setTerrain(1.5f);

        final Geocache hard = new Geocache();
        hard.setTerrain(5f);

        final AbstractRangeFilter easyFilter = new TerrainFilter(1);

        assertTrue(easyFilter.accepts(easy));
        assertFalse(easyFilter.accepts(hard));
    }

    public static void testAllFilters() {
        assertTrue(new TerrainFilter.Factory().getFilters().size() == 5); // terrain ranges from 1 to 5
    }
}
