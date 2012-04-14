package cgeo.geocaching.filter;

import cgeo.geocaching.cgCache;

public class DifficultyFilterTest extends AbstractFilterTestCase {

    public static void testTerrainFilter() {
        final cgCache easy = new cgCache();
        easy.setDifficulty(1.5f);

        final cgCache hard = new cgCache();
        hard.setDifficulty(5f);

        final DifficultyFilter easyFilter = new DifficultyFilter(1);

        assertTrue(easyFilter.accepts(easy));
        assertFalse(easyFilter.accepts(hard));
    }

    public static void testAllFilters() {
        assertTrue(new DifficultyFilter.Factory().getFilters().length == 5); // difficulty ranges from 1 to 5
    }
}
