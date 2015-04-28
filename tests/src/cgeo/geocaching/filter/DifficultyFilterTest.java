package cgeo.geocaching.filter;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;

public class DifficultyFilterTest extends CGeoTestCase {

    public static void testTerrainFilter() {
        final Geocache easy = new Geocache();
        easy.setDifficulty(1.5f);

        final Geocache hard = new Geocache();
        hard.setDifficulty(5f);

        final DifficultyFilter easyFilter = new DifficultyFilter(1);

        assertThat(easyFilter.accepts(easy)).isTrue();
        assertThat(easyFilter.accepts(hard)).isFalse();
    }

    public static void testAllFilters() {
        assertThat(new DifficultyFilter.Factory().getFilters()).hasSize(5); // difficulty ranges from 1 to 5
    }
}
