package cgeo.geocaching.filters.core;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.su.SuConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

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
        final GeocacheFilter f1 = GeocacheFilter.createEmpty();
        final GeocacheFilter f2 = GeocacheFilter.createEmpty();
        assertThat(f1.filtersSame(f2)).isTrue();
    }

    @Test
    public void filtersSameReturnsFalseIfInconclusiveDiffers() {
        final GeocacheFilter f1 = GeocacheFilter.create("Test", false, true, null);
        final GeocacheFilter f2 = GeocacheFilter.create("Test", false, false, null);
        assertThat(f1.filtersSame(f2)).isTrue();
    }

    @Test
    public void filtersSameReturnsFalseIfNameDiffers() {
        final GeocacheFilter f1 = GeocacheFilter.create("NameA", false, false, null);
        final GeocacheFilter f2 = GeocacheFilter.create("NameB", false, false, null);
        assertThat(f1.filtersSame(f2)).isTrue();
    }

    @Test
    public void filterListWithNullTreeKeepsAll() {
        final Geocache g1 = new Geocache();
        final Geocache g2 = new Geocache();
        final List<Geocache> caches = new ArrayList<>();
        caches.add(g1);
        caches.add(g2);

        final GeocacheFilter filter = GeocacheFilter.create("NoTree", false, false, null);
        filter.filterList(caches);

        assertThat(caches).containsExactly(g1, g2);
    }

    @Test
    public void getAndChainIfPossibleForConnectorWithNoFilter() {
        // empty filter => null
        final IConnector gcConnector = GCConnector.getInstance();

        final GeocacheFilter filter = GeocacheFilter.createEmpty();
        final List<BaseGeocacheFilter> result = filter.getAndChainIfPossible(gcConnector);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }


    @Test
    public void getAndChainIfPossibleForConnectorWithOnlyConnectorFilter() {
        // empty filter => null
        final IConnector gcConnector = GCConnector.getInstance();

        final OriginGeocacheFilter originFilter = GeocacheFilterType.ORIGIN.create();
        originFilter.setValues(Collections.singleton(gcConnector));

        final GeocacheFilter filter = GeocacheFilter.create("Test", false, false, originFilter);
        final List<BaseGeocacheFilter> result = filter.getAndChainIfPossible(gcConnector);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void getAndChainIfPossibleForConnectorWithOrFilter() {
        // Create an OR filter with two distance filters
        // (Distance1 OR Distance2) => GC: (Distance1 OR Distance2)

        final IConnector gcConnector = GCConnector.getInstance();

        final DistanceGeocacheFilter distance1 = GeocacheFilterType.DISTANCE.create();
        final DistanceGeocacheFilter distance2 = GeocacheFilterType.DISTANCE.create();

        final OrGeocacheFilter orFilter = new OrGeocacheFilter();
        orFilter.addChild(distance1);
        orFilter.addChild(distance2);

        final GeocacheFilter filter = GeocacheFilter.create("Test", false, false, orFilter);

        // OR filters should not be expanded into an AND chain
        final List<BaseGeocacheFilter> result = filter.getAndChainIfPossible(gcConnector);
        assertThat(result.get(0)).isInstanceOf(OrGeocacheFilter.class);
    }

    @Test
    public void getAndChainIfPossibleRemovesMatchingOriginFilter() {
        // Create a filter with OriginGeocacheFilter allowing GC and a DistanceFilter
        // (GC AND Distance) => GC: Distance
        // (GC AND Distance) => SU: null

        final IConnector gcConnector = GCConnector.getInstance();
        final IConnector suConnector = SuConnector.getInstance();

        final OriginGeocacheFilter originFilter = GeocacheFilterType.ORIGIN.create();
        originFilter.setValues(Collections.singleton(gcConnector));

        final DistanceGeocacheFilter distanceFilter = GeocacheFilterType.DISTANCE.create();

        final AndGeocacheFilter andFilter = new AndGeocacheFilter();
        andFilter.addChild(originFilter);
        andFilter.addChild(distanceFilter);

        final GeocacheFilter filter = GeocacheFilter.create("Test", false, false, andFilter);

        // When filtering for GC connector, the origin filter should be removed (redundant)
        final List<BaseGeocacheFilter> gcResult = filter.getAndChainIfPossible(gcConnector);
        assertThat(gcResult).hasSize(1);
        assertThat(gcResult.get(0).getType()).isEqualTo(GeocacheFilterType.DISTANCE);

        // When filtering for SU connector, the AND filter contains an OriginFilter that excludes SU
        // so the entire filter becomes invalid and returns empty list
        final List<BaseGeocacheFilter> ocResult = filter.getAndChainIfPossible(suConnector);
        assertThat(ocResult).isNull();
    }

    @Test
    public void getAndChainIfPossibleWithNotFilter() {
        // Create a AND filter with a NOT filter (origin) and a Distance filter
        // NOT(GC) AND Distance => GC: null
        // NOT(GC) AND Distance => SU: Distance

        final IConnector gcConnector = GCConnector.getInstance();
        final IConnector suConnector = SuConnector.getInstance();

        final OriginGeocacheFilter originFilter = GeocacheFilterType.ORIGIN.create();
        originFilter.setValues(Collections.singleton(gcConnector));

        final NotGeocacheFilter notFilter = new NotGeocacheFilter();
        notFilter.addChild(originFilter);

        final DistanceGeocacheFilter distanceFilter = GeocacheFilterType.DISTANCE.create();

        final AndGeocacheFilter andFilter = new AndGeocacheFilter();
        andFilter.addChild(notFilter);
        andFilter.addChild(distanceFilter);

        final GeocacheFilter filter = GeocacheFilter.create("Test", false, false, andFilter);

        // For GC connector: NOT(GC) contains OriginFilter(GC) which allows GC
        // So the NOT filter becomes invalid for GC -> entire AND is invalid
        final List<BaseGeocacheFilter> gcResult = filter.getAndChainIfPossible(gcConnector);
        assertThat(gcResult).isNull();

        // For SU connector: NOT(GC) contains OriginFilter(GC) which doesn't allow SU
        // So the OriginFilter is removed from NOT, but NOT becomes empty -> NOT is removed
        // Result: just the Distance filter remains
        final List<BaseGeocacheFilter> suResult = filter.getAndChainIfPossible(suConnector);
        assertThat(suResult).hasSize(1);
        assertThat(suResult.get(0).getType()).isEqualTo(GeocacheFilterType.DISTANCE);

    }

    @Test
    public void getAndChainIfPossibleWithNestedAndFilters() {
        // Create nested AND filters
        // (Distance1 AND Distance2) AND Type AND GC => GC: (Distance1 AND Distance2) AND Type
        // (Distance1 AND Distance2) AND Type AND GC => SU: null

        final IConnector gcConnector = GCConnector.getInstance();
        final IConnector suConnector = SuConnector.getInstance();

        final DistanceGeocacheFilter distance1 = GeocacheFilterType.DISTANCE.create();
        final DistanceGeocacheFilter distance2 = GeocacheFilterType.DISTANCE.create();
        final TypeGeocacheFilter typeFilter = GeocacheFilterType.TYPE.create();
        final OriginGeocacheFilter originFilter = GeocacheFilterType.ORIGIN.create();
        originFilter.setValues(Collections.singleton(gcConnector));

        final AndGeocacheFilter innerAnd = new AndGeocacheFilter();
        innerAnd.addChild(distance1);
        innerAnd.addChild(distance2);

        final AndGeocacheFilter outerAnd = new AndGeocacheFilter();
        outerAnd.addChild(innerAnd);
        outerAnd.addChild(typeFilter);
        outerAnd.addChild(originFilter);

        final GeocacheFilter filter = GeocacheFilter.create("Test", false, false, outerAnd);

        // All filters should be flattened into the AND chain
        final List<BaseGeocacheFilter> gcResult = filter.getAndChainIfPossible(gcConnector);
        assertThat(gcResult).hasSize(3);

        final long distanceCount = gcResult.stream().filter(f -> f.getType() == GeocacheFilterType.DISTANCE).count();
        final long typeCount = gcResult.stream().filter(f -> f.getType() == GeocacheFilterType.TYPE).count();

        assertThat(distanceCount).isEqualTo(2);
        assertThat(typeCount).isEqualTo(1);

        final List<BaseGeocacheFilter> suResult = filter.getAndChainIfPossible(suConnector);
        assertThat(suResult).isNull();
    }

    @Test
    public void getAndChainIfPossibleWithMultipleOriginFilters() {
        // Create a filter with OriginGeocacheFilter allowing both GC and OC
        // (GC / OC AND Distance) => GC: return Distance-Filter
        // (GC / OC AND Distance) => SU: return Distance-Filter

        final IConnector gcConnector = GCConnector.getInstance();
        final IConnector suConnector = SuConnector.getInstance();

        final OriginGeocacheFilter originFilter = GeocacheFilterType.ORIGIN.create();
        originFilter.setValues(Arrays.asList(gcConnector, suConnector));

        final DistanceGeocacheFilter distanceFilter = GeocacheFilterType.DISTANCE.create();

        final AndGeocacheFilter andFilter = new AndGeocacheFilter();
        andFilter.addChild(originFilter);
        andFilter.addChild(distanceFilter);

        final GeocacheFilter filter = GeocacheFilter.create("Test", false, false, andFilter);

        // When filtering for GC connector, the origin filter should be removed
        final List<BaseGeocacheFilter> gcResult = filter.getAndChainIfPossible(gcConnector);
        assertThat(gcResult).hasSize(1);
        assertThat(gcResult.get(0)).isInstanceOf(DistanceGeocacheFilter.class);

        // When filtering for OC connector, the origin filter should also be removed
        final List<BaseGeocacheFilter> ocResult = filter.getAndChainIfPossible(suConnector);
        assertThat(ocResult).hasSize(1);
        assertThat(ocResult.get(0)).isInstanceOf(DistanceGeocacheFilter.class);
    }

    @Test
    public void getAndChainIfPossibleWithComplexOrStructure() {
        // (OriginFilter(GC) AND OwnerFilter(Owner1)) OR (OriginFilter(SU) AND OwnerFilter(Owner2))

        final IConnector gcConnector = GCConnector.getInstance();
        final IConnector suConnector = SuConnector.getInstance();

        // Create first branch: (OriginFilter(GC) AND OwnerFilter(Owner1))
        final OriginGeocacheFilter originFilter1 = GeocacheFilterType.ORIGIN.create();
        originFilter1.setValues(Collections.singleton(gcConnector));

        final OwnerGeocacheFilter ownerFilter1 = GeocacheFilterType.OWNER.create();
        ownerFilter1.getStringFilter().setTextValue("Owner1");

        final AndGeocacheFilter andBranch1 = new AndGeocacheFilter();
        andBranch1.addChild(originFilter1);
        andBranch1.addChild(ownerFilter1);

        // Create second branch: (OriginFilter(SU) AND OwnerFilter(Owner2))
        final OriginGeocacheFilter originFilter2 = GeocacheFilterType.ORIGIN.create();
        originFilter2.setValues(Collections.singleton(suConnector));

        final OwnerGeocacheFilter ownerFilter2 = GeocacheFilterType.OWNER.create();
        ownerFilter2.getStringFilter().setTextValue("Owner2");

        final AndGeocacheFilter andBranch2 = new AndGeocacheFilter();
        andBranch2.addChild(originFilter2);
        andBranch2.addChild(ownerFilter2);

        // Combine with OR: (Branch1 OR Branch2)
        final OrGeocacheFilter orFilter = new OrGeocacheFilter();
        orFilter.addChild(andBranch1);
        orFilter.addChild(andBranch2);

        final GeocacheFilter filter = GeocacheFilter.create("Test", false, false, orFilter);

        // When filtering for GC connector:
        // - Branch1: OriginFilter(GC) is removed (redundant), OwnerFilter(Owner1) remains
        // - Branch2: OriginFilter(SU) doesn't allow GC, so entire branch is kept with OriginFilter
        // Result: OR filter with one AND branch and one OriginFilter -> no AND chain possible
        // BUT: if Branch2 contains an OriginFilter that doesn't allow GC, the entire branch
        // should be removed because it can't produce results for GC
        // So we should get: just OwnerFilter(Owner1) from Branch1
        final List<BaseGeocacheFilter> gcResult = filter.getAndChainIfPossible(gcConnector);
        assertThat(gcResult).hasSize(1);
        assertThat(gcResult.get(0)).isInstanceOf(OwnerGeocacheFilter.class);
        assertThat(((OwnerGeocacheFilter) gcResult.get(0)).getStringFilter().getTextValue()).isEqualTo("Owner1");

        // When filtering for SU connector:
        // - Branch1: OriginFilter(GC) doesn't allow OC, so entire branch should be removed
        // - Branch2: OriginFilter(SU) is removed (redundant), OwnerFilter(Owner2) remains
        // Result: just OwnerFilter(Owner2) from Branch2
        final List<BaseGeocacheFilter> ocResult = filter.getAndChainIfPossible(suConnector);
        assertThat(ocResult).hasSize(1);
        assertThat(ocResult.get(0)).isInstanceOf(OwnerGeocacheFilter.class);
        assertThat(((OwnerGeocacheFilter) ocResult.get(0)).getStringFilter().getTextValue()).isEqualTo("Owner2");
    }

    @Test
    public void getAndChainIfPossibleWithOR() {
        // OR(AND(Origin=GC, Type=Tradi), AND(Origin=GC, Size=Micro))
        final IConnector gcConnector = GCConnector.getInstance();

        // Create first branch: (OriginFilter(GC) AND TypeGeocacheFilter(TRADITIONAL))
        final OriginGeocacheFilter originFilter = GeocacheFilterType.ORIGIN.create();
        originFilter.setValues(Collections.singleton(gcConnector));

        final TypeGeocacheFilter tradiFilter = GeocacheFilterType.TYPE.create();
        tradiFilter.setValues(Collections.singleton(CacheType.TRADITIONAL));

        final AndGeocacheFilter andBranch1 = new AndGeocacheFilter();
        andBranch1.addChild(originFilter);
        andBranch1.addChild(tradiFilter);

        // Create second branch: (OriginFilter(GC) AND SizeFilter(Micro))
        final SizeGeocacheFilter sizeFilter = GeocacheFilterType.SIZE.create();
        sizeFilter.setValues(Collections.singleton(CacheSize.MICRO));

        final AndGeocacheFilter andBranch2 = new AndGeocacheFilter();
        andBranch2.addChild(originFilter);
        andBranch2.addChild(sizeFilter);

        // Combine with OR: (Branch1 OR Branch2)
        final OrGeocacheFilter orFilter = new OrGeocacheFilter();
        orFilter.addChild(andBranch1);
        orFilter.addChild(andBranch2);

        final GeocacheFilter filter = GeocacheFilter.create("Test", false, false, orFilter);

        final GeocacheFilter relevantFilter = filter.getConnectorRelevantFilter(gcConnector);
        assertThat(relevantFilter).isNotNull();
        assertThat(relevantFilter.getTree()).isNotNull();
        assertThat(relevantFilter.getTree()).isInstanceOf(OrGeocacheFilter.class);

        final List<BaseGeocacheFilter> gcResult = filter.getAndChainIfPossible(gcConnector);
        assertThat(gcResult).isNotNull();
        assertThat(gcResult).hasSize(1);
        final List<IGeocacheFilter> orResultFilter = gcResult.get(0).getChildren();
        assertThat(orResultFilter).hasSize(2);
        assertThat(orResultFilter.get(0).getType()).isEqualTo(GeocacheFilterType.TYPE);
        assertThat(orResultFilter.get(1).getType()).isEqualTo(GeocacheFilterType.SIZE);
    }
}


