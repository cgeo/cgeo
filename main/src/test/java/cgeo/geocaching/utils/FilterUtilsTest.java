package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FilterUtilsTest {

    @Test
    public void getFilterNameChanged() {
        final String filterName = "FilterName";
        final String purifiedName = "(FilterName)*";
        assertThat(FilterUtils.getFilterName(filterName, true)).isEqualTo(purifiedName);

        final String filterNameAsterix = "(FilterName*)*";
        final String purifiedNameAsterix = "FilterName*";
        assertThat(FilterUtils.getPurifiedFilterName(filterNameAsterix)).isEqualTo(purifiedNameAsterix);

        final String filterNameBrackets = "((FilterName))*";
        final String purifiedNameBrackets = "(FilterName)";
        assertThat(FilterUtils.getPurifiedFilterName(filterNameBrackets)).isEqualTo(purifiedNameBrackets);
    }

    @Test
    public void getFilterNameUnchanged() {
        final String filterName = "FilterName";
        assertThat(FilterUtils.getFilterName(filterName, false)).isEqualTo(filterName);

        final String filterNameAsterix = "FilterName*";
        assertThat(FilterUtils.getPurifiedFilterName(filterNameAsterix)).isEqualTo(filterNameAsterix);

        final String filterNameBrackets = "(FilterName)";
        assertThat(FilterUtils.getPurifiedFilterName(filterNameBrackets)).isEqualTo(filterNameBrackets);
    }

    @Test
    public void getPurifiedFilterNameChanged() {
        final String filterName = "(FilterName)*";
        final String purifiedName = "FilterName";
        assertThat(FilterUtils.getPurifiedFilterName(filterName)).isEqualTo(purifiedName);

        final String filterNameAsterix = "(FilterName*)*";
        final String purifiedNameAsterix = "FilterName*";
        assertThat(FilterUtils.getPurifiedFilterName(filterNameAsterix)).isEqualTo(purifiedNameAsterix);

        final String filterNameBrackets = "((FilterName))*";
        final String purifiedNameBrackets = "(FilterName)";
        assertThat(FilterUtils.getPurifiedFilterName(filterNameBrackets)).isEqualTo(purifiedNameBrackets);
    }

    @Test
    public void getPurifiedFilterNameUnchanged() {
        final String filterName = "FilterName";
        assertThat(FilterUtils.getPurifiedFilterName(filterName)).isEqualTo(filterName);

        final String filterNameAsterix = "FilterName*";
        assertThat(FilterUtils.getPurifiedFilterName(filterNameAsterix)).isEqualTo(filterNameAsterix);

        final String filterNameBrackets = "(FilterName)";
        assertThat(FilterUtils.getPurifiedFilterName(filterNameBrackets)).isEqualTo(filterNameBrackets);
    }
}
