package cgeo.geocaching.filters.core;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import cgeo.geocaching.models.Geocache;

public class GeocacheFilterTest {

    @Test
    public void getFilterNameChanged() {
        final String filterName = "FilterName";
        final String purifiedName = "(FilterName)*";
        assertThat(GeocacheFilter.getFilterName(filterName, true)).isEqualTo(purifiedName);

        final String filterNameAsterix = "(FilterName*)*";
        final String purifiedNameAsterix = "FilterName*";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(purifiedNameAsterix);

        final String filterNameBrackets = "((FilterName))*";
        final String purifiedNameBrackets = "(FilterName)";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(purifiedNameBrackets);
    }

    @Test
    public void getFilterNameUnchanged() {
        final String filterName = "FilterName";
        assertThat(GeocacheFilter.getFilterName(filterName, false)).isEqualTo(filterName);

        final String filterNameAsterix = "FilterName*";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(filterNameAsterix);

        final String filterNameBrackets = "(FilterName)";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(filterNameBrackets);
    }

    @Test
    public void getPurifiedFilterNameChanged() {
        final String filterName = "(FilterName)*";
        final String purifiedName = "FilterName";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterName)).isEqualTo(purifiedName);

        final String filterNameAsterix = "(FilterName*)*";
        final String purifiedNameAsterix = "FilterName*";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(purifiedNameAsterix);

        final String filterNameBrackets = "((FilterName))*";
        final String purifiedNameBrackets = "(FilterName)";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(purifiedNameBrackets);
    }

    @Test
    public void getPurifiedFilterNameUnchanged() {
        final String filterName = "FilterName";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterName)).isEqualTo(filterName);

        final String filterNameAsterix = "FilterName*";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameAsterix)).isEqualTo(filterNameAsterix);

        final String filterNameBrackets = "(FilterName)";
        assertThat(GeocacheFilter.getPurifiedFilterName(filterNameBrackets)).isEqualTo(filterNameBrackets);
    }

    @Test
    public void filtersSameReturnsTrueForEmptyFilters() {
        GeocacheFilter f1 = GeocacheFilter.createEmpty();
        GeocacheFilter f2 = GeocacheFilter.createEmpty();
        assertThat(f1.filtersSame(f2)).isTrue();
    }


    @Test
    public void filtersSameReturnsFalseIfInconclusiveDiffers() {
        GeocacheFilter f1 = GeocacheFilter.create("Test", false, true, null);
        GeocacheFilter f2 = GeocacheFilter.create("Test", false, false, null);
        assertThat(f1.filtersSame(f2)).isTrue();
    }

    @Test
    public void filtersSameReturnsFalseIfNameDiffers() {
        GeocacheFilter f1 = GeocacheFilter.create("NameA", false, false, null);
        GeocacheFilter f2 = GeocacheFilter.create("NameB", false, false, null);
        assertThat(f1.filtersSame(f2)).isTrue();
    }

    @Test
    public void filterListWithNullTreeKeepsAll() {
        Geocache g1 = new Geocache();
        Geocache g2 = new Geocache();
        List<Geocache> caches = new ArrayList<>();
        caches.add(g1);
        caches.add(g2);

        GeocacheFilter filter = GeocacheFilter.create("NoTree", false, false, null);
        filter.filterList(caches);

        assertThat(caches).containsExactly(g1, g2);
    }
}
