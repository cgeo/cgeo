package cgeo.geocaching.filter;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;

public class DifficultyFilterTest extends CGeoTestCase {

    public static void testTerrainFilter() {
        final Geocache easy = new Geocache();
        easy.setDifficulty(1.5f);

        final Geocache hard = new Geocache();
        hard.setDifficulty(5f);

        final DifficultyFilter easyFilter = new DifficultyFilter(1);

        assertTrue(easyFilter.accepts(easy));
        assertFalse(easyFilter.accepts(hard));
    }

    public static void testAllFilters() {
        assertTrue(new DifficultyFilter.Factory().getFilters().size() == 5); // difficulty ranges from 1 to 5
    }
}
