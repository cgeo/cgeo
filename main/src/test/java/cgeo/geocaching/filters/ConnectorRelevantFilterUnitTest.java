package cgeo.geocaching.filters;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.NameGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.filters.core.OriginGeocacheFilter;
import cgeo.geocaching.filters.core.TypeGeocacheFilter;

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
        final GeocacheFilter gcResult = empty.getConnectorRelevantFilter(GC);
        assertThat(gcResult).isNotNull();
        assertThat(gcResult.getTree()).isNull();
        assertThat(gcResult.isFiltering()).isFalse();
    }

    @Test
    public void simpleAndWithMatchingOriginReturnsFilterWithoutOrigin() {
        // AND(Origin=GC, Name="test")
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false,
                AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("test")));

        final GeocacheFilter gcResult = filter.getConnectorRelevantFilter(GC);
        assertThat(gcResult).isNotNull();
        // Origin was removed, only NameFilter remains
        assertThat(gcResult.getTree()).isInstanceOf(NameGeocacheFilter.class);
    }

    @Test
    public void simpleAndWithNonMatchingOriginReturnsNull() {
        // AND(Origin=GC, Name="test")
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false,
                AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("test")));

        final GeocacheFilter suResult = filter.getConnectorRelevantFilter(SU);
        assertThat(suResult).isNull();
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
        final IGeocacheFilter gcTree = gcResult.getTree();
        assertThat(gcTree).isInstanceOf(NameGeocacheFilter.class);
        assertThat(((NameGeocacheFilter) gcTree).getStringFilter().getTextValue()).isEqualTo("gc-test");

        // For SU: only su-branch should remain
        final GeocacheFilter suResult = filter.getConnectorRelevantFilter(SU);
        assertThat(suResult).isNotNull();
        final IGeocacheFilter suTree = suResult.getTree();
        assertThat(suTree).isInstanceOf(NameGeocacheFilter.class);
        assertThat(((NameGeocacheFilter) suTree).getStringFilter().getTextValue()).isEqualTo("su-test");
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
        final IGeocacheFilter gcTree = gcResult.getTree();
        assertThat(gcTree).isInstanceOf(OrGeocacheFilter.class);
        final List<IGeocacheFilter> gcChildren = gcTree.getChildren();
        assertThat(gcChildren).hasSize(2);
        final IGeocacheFilter gcChild1 = gcChildren.get(0);
        final IGeocacheFilter gcChild2 = gcChildren.get(1);
        assertThat(((NameGeocacheFilter) gcChild1).getStringFilter().getTextValue()).isEqualTo("gc-test");
        assertThat(((NameGeocacheFilter) gcChild2).getStringFilter().getTextValue()).isEqualTo("general");

        // For SU: only the general branch is relevant → Name="general"
        final GeocacheFilter suResult = filter.getConnectorRelevantFilter(SU);
        assertThat(suResult).isNotNull();
        final IGeocacheFilter suTree = suResult.getTree();
        assertThat(suTree).isInstanceOf(NameGeocacheFilter.class);
        assertThat(((NameGeocacheFilter) suTree).getStringFilter().getTextValue()).isEqualTo("general");
    }

    @Test
    public void preservesNameAndFlags() {
        final GeocacheFilter filter = GeocacheFilter.create("myFilter", true, true,
                AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("test")));

        final GeocacheFilter gcResult = filter.getConnectorRelevantFilter(GC);
        assertThat(gcResult).isNotNull();
        assertThat(gcResult.getName()).isEqualTo("myFilter");
        assertThat(gcResult.isOpenInAdvancedMode()).isTrue();
        assertThat(gcResult.isIncludeInconclusive()).isTrue();
    }

    @Test
    public void getAndChainIfPossibleWithConnectorReturnsNullForExcluded() {
        // AND(Origin=GC, Name="test")
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false,
                AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("gc-test")));

        final List<BaseGeocacheFilter> gcChain = filter.getAndChainIfPossible(GC);
        assertThat(gcChain).isNotNull();
        assertThat(gcChain).hasSize(1);
        assertThat(gcChain.get(0)).isInstanceOf(NameGeocacheFilter.class);

        final List<BaseGeocacheFilter> suChain = filter.getAndChainIfPossible(SU);
        assertThat(suChain).isNull();
    }

    @Test
    public void getAndChainIfPossibleWithComplexFilter() {
        // AND(OR(AND(Origin=GC, Name="gc-test"), AND(Origin=SU, Name="su-test")), Type=TRADITIONAL)
        final AndGeocacheFilter gcBranch = AndGeocacheFilter.create(OriginGeocacheFilter.create(GC), NameGeocacheFilter.create("gc-test"));
        final AndGeocacheFilter suBranch = AndGeocacheFilter.create(OriginGeocacheFilter.create(SU), NameGeocacheFilter.create("su-test"));
        final OrGeocacheFilter orFilter = new OrGeocacheFilter();
        orFilter.addChild(gcBranch);
        orFilter.addChild(suBranch);

        final TypeGeocacheFilter typeFilter = TypeGeocacheFilter.create(CacheType.TRADITIONAL);
        final AndGeocacheFilter complexFilter = AndGeocacheFilter.create(orFilter, typeFilter);

        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, complexFilter);

        // For GC: only gc-branch and type should remain
        final List<BaseGeocacheFilter> gcChain = filter.getAndChainIfPossible(GC);
        assertThat(gcChain).hasSize(2);
        final IGeocacheFilter gcChild1 = gcChain.get(0);
        final IGeocacheFilter gcChild2 = gcChain.get(1);
        assertThat(((NameGeocacheFilter) gcChild1).getStringFilter().getTextValue()).isEqualTo("gc-test");
        assertThat(((TypeGeocacheFilter) gcChild2).getValues()).contains(CacheType.TRADITIONAL);

        // For SU: only su-branch should remain
        final List<BaseGeocacheFilter> suChain = filter.getAndChainIfPossible(SU);
        assertThat(suChain).hasSize(2);
        final IGeocacheFilter suChild1 = suChain.get(0);
        final IGeocacheFilter suChild2 = suChain.get(1);
        assertThat(((NameGeocacheFilter) suChild1).getStringFilter().getTextValue()).isEqualTo("su-test");
        assertThat(((TypeGeocacheFilter) suChild2).getValues()).contains(CacheType.TRADITIONAL);
    }

    @Test
    public void notWithNonAndInnerFilterIsPreserved() {
        // NOT(Name="test")
        // Expected: the NOT filter is preserved as-is for any connector.
        final NotGeocacheFilter notFilter = NotGeocacheFilter.create(NameGeocacheFilter.create("test"));
        final GeocacheFilter filter = GeocacheFilter.create(null, false, false, notFilter);

        final GeocacheFilter result = filter.getConnectorRelevantFilter(GC);
        assertThat(result).isNotNull();
        assertThat(result.getTree()).isInstanceOf(NotGeocacheFilter.class);
        final IGeocacheFilter child = result.getTree().getChildren().get(0);
        assertThat(child).isInstanceOf(NameGeocacheFilter.class);
        assertThat(((NameGeocacheFilter) child).getStringFilter().getTextValue()).isEqualTo("test");
    }

}
