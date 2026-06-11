package cgeo.geocaching.filters.core;
import cgeo.geocaching.models.Geocache;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GeocacheFilterTest {

    @Test
    public void filtersSameReturnsTrueForEmptyFilters() {
        final GeocacheFilter f1 = GeocacheFilter.createEmpty();
        final GeocacheFilter f2 = GeocacheFilter.createEmpty();
        assertThat(f1.filtersSame(f2)).isTrue();
    }

    @Test
    public void filtersSameReturnsFalseIfInconclusiveDiffers() {
        final GeocacheFilter f1 = GeocacheFilter.create(false, true, null);
        final GeocacheFilter f2 = GeocacheFilter.create(false, false, null);
        assertThat(f1.filtersSame(f2)).isTrue();
    }

    @Test
    public void filterListWithNullTreeKeepsAll() {
        final Geocache g1 = new Geocache();
        final Geocache g2 = new Geocache();
        final List<Geocache> caches = new ArrayList<>();
        caches.add(g1);
        caches.add(g2);

        final GeocacheFilter filter = GeocacheFilter.create(false, false, null);
        filter.filterList(caches);

        assertThat(caches).containsExactly(g1, g2);
    }
}
