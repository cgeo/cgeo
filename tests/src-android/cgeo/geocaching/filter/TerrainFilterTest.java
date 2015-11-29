package cgeo.geocaching.filter;

import junit.framework.TestCase;

import cgeo.geocaching.Geocache;

import static org.assertj.core.api.Assertions.assertThat;

public class TerrainFilterTest extends TestCase {

    public static void testTerrainFilter() {
        final Geocache easy = new Geocache();
        easy.setTerrain(1.5f);

        final Geocache hard = new Geocache();
        hard.setTerrain(5f);

        final AbstractRangeFilter easyFilter = new TerrainFilter(1);

        assertThat(easyFilter.accepts(easy)).isTrue();
        assertThat(easyFilter.accepts(hard)).isFalse();
    }

    public static void testAllFilters() {
        assertThat(new TerrainFilter.Factory().getFilters().size() == 7); // terrain ranges from 1 to 7 (due to ExtremCaching.com using that value).isTrue()
    }
}
