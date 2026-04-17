package cgeo.geocaching.filters;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;

import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests for {@link GeocacheFilter#getConnectorRelevantFilter(IConnector)} and
 * {@link GeocacheFilter#getAndChainIfPossible(IConnector)}.
 */
public class ConnectorRelevantFilterUnitTest {

    private static final IConnector GC = GCConnector.getInstance();
    private static final IConnector SU = SuConnector.getInstance();

    // --- Tests ---

    @Test
    public void nullTreeReturnsEmptyFilter() {
        final GeocacheFilter empty = GeocacheFilter.createEmpty();
        final GeocacheFilter result = empty.getConnectorRelevantFilter(GC);
        assertThat(result).isNotNull();
        assertThat(result.getTree()).isNull();
        assertThat(result.isFiltering()).isFalse();
    }

    @Test
    public void simpleAndWithMatchingOriginReturnsFilterWithoutOrigin() {
        // AND(Origin=GC, Name="test")
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false,
                AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("test")));

        final GeocacheFilter result = filter.getConnectorRelevantFilter(GC);
        assertThat(result).isNotNull();
        // Origin was removed, only NameFilter remains
        assertThat(result.getTree()).isInstanceOf(NameGeocacheFilter.class);
    }

    @Test
    public void simpleAndWithNonMatchingOriginReturnsNull() {
        // AND(Origin=GC, Name="test")
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false,
                AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("test")));

        final GeocacheFilter result = filter.getConnectorRelevantFilter(SU);
        assertThat(result).isNull();
    }

    @Test
    public void orWithDifferentOriginBranches() {
        // OR(AND(Origin=GC, Name="gc-test"), AND(Origin=SU, Name="su-test"))
        final AndGeocacheFilter gcBranch = AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("gc-test"));
        final AndGeocacheFilter suBranch = AndGeocacheFilter.create(OriginGeocacheFilter.create(SU), NameGeocacheFilter.create("su-test"));
        final OrGeocacheFilter orFilter = new OrGeocacheFilter();
        orFilter.addChild(gcBranch);
        orFilter.addChild(suBranch);

        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, orFilter);

        // For GC: only gc-branch should remain
        final GeocacheFilter gcResult = filter.getConnectorRelevantFilter(GC);
        assertThat(gcResult).isNotNull();
        assertThat(gcResult.getTree()).isInstanceOf(NameGeocacheFilter.class);

        // For SU: only su-branch should remain
        final GeocacheFilter suResult = filter.getConnectorRelevantFilter(SU);
        assertThat(suResult).isNotNull();
        assertThat(suResult.getTree()).isInstanceOf(NameGeocacheFilter.class);
    }

    @Test
    public void noOriginFilterReturnsFullTree() {
        // AND(Name="test")
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, NameGeocacheFilter.create("test"));

        final GeocacheFilter gcResult = filter.getConnectorRelevantFilter(GC);
        assertThat(gcResult).isNotNull();
        assertThat(gcResult.getTree()).isInstanceOf(NameGeocacheFilter.class);

        final GeocacheFilter suResult = filter.getConnectorRelevantFilter(SU);
        assertThat(suResult).isNotNull();
        assertThat(suResult.getTree()).isInstanceOf(NameGeocacheFilter.class);
    }

    @Test
    public void notOriginExcludesMatchingConnector() {
        // NOT(Origin=GC) means: exclude GC caches
        final NotGeocacheFilter notFilter = new NotGeocacheFilter();
        notFilter.addChild(OriginGeocacheFilter.create(GC));

        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, notFilter);

        // GC should be excluded (NOT(always-true-for-GC) = false)
        assertThat(filter.getConnectorRelevantFilter(GC)).isNull();

        // SU should NOT be excluded (NOT(false-for-SU) = true → always true)
        final GeocacheFilter suResult = filter.getConnectorRelevantFilter(SU);
        assertThat(suResult).isNotNull();
        assertThat(suResult.getTree()).isNull(); // simplified to always-true
    }

    @Test
    public void nonFilteringOriginIsAlwaysTrue() {
        // empty OriginFilter (isFiltering() == false) → always true
        final OriginGeocacheFilter emptyOrigin = GeocacheFilterType.ORIGIN.create();
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, emptyOrigin);

        final GeocacheFilter result = filter.getConnectorRelevantFilter(GC);
        assertThat(result).isNotNull();
        assertThat(result.getTree()).isNull(); // simplified to always-true
    }

    @Test
    public void getAndChainIfPossibleWithConnectorReturnsNullForExcluded() {
        // AND(Origin=GC, Name="test")
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false,
                AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("test")));

        final List<BaseGeocacheFilter> gcChain = filter.getAndChainIfPossible(GC);
        assertThat(gcChain).isNotNull();
        assertThat(gcChain).hasSize(1);
        assertThat(gcChain.get(0)).isInstanceOf(NameGeocacheFilter.class);

        final List<BaseGeocacheFilter> suChain = filter.getAndChainIfPossible(SU);
        assertThat(suChain).isNull();
    }

    @Test
    public void orWithMixedBranchesPartiallyRelevant() {
        // OR(AND(Origin=GC, Name="gc-test"), Name="general")
        final AndGeocacheFilter gcBranch = AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("gc-test"));
        final NameGeocacheFilter generalBranch = NameGeocacheFilter.create("general");
        final OrGeocacheFilter orFilter = new OrGeocacheFilter();
        orFilter.addChild(gcBranch);
        orFilter.addChild(generalBranch);

        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, orFilter);

        // For GC: both branches are relevant → OR(Name="gc-test", Name="general")
        final GeocacheFilter gcResult = filter.getConnectorRelevantFilter(GC);
        assertThat(gcResult).isNotNull();
        assertThat(gcResult.getTree()).isInstanceOf(OrGeocacheFilter.class);
        assertThat(((OrGeocacheFilter) gcResult.getTree()).getChildren()).hasSize(2);

        // For SU: only the general branch is relevant → Name="general"
        final GeocacheFilter suResult = filter.getConnectorRelevantFilter(SU);
        assertThat(suResult).isNotNull();
        assertThat(suResult.getTree()).isInstanceOf(NameGeocacheFilter.class);
    }

    @Test
    public void preservesNameAndFlags() {
        final GeocacheFilter filter = GeocacheFilter.create("myFilter", true, true,
                AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("test")));

        final GeocacheFilter result = filter.getConnectorRelevantFilter(GC);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("myFilter");
        assertThat(result.isOpenInAdvancedMode()).isTrue();
        assertThat(result.isIncludeInconclusive()).isTrue();
    }
}



