package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GeocacheFilterTest {

    // =====================================================================
    // Helper factory methods
    // =====================================================================

    /** Creates a leaf filter whose {@code isFiltering()} always returns {@code true}. */
    private static IGeocacheFilter leaf(final String id) {
        return new BaseGeocacheFilter() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public Boolean filter(final Geocache cache) {
                return true;
            }

            @Override
            public boolean isFiltering() {
                return true;
            }

            @Override
            public ObjectNode getJsonConfig() {
                return null;
            }

            @Override
            public void setJsonConfig(@NonNull final ObjectNode node) {
                // empty
            }
        };
    }

    private static AndGeocacheFilter and(final IGeocacheFilter... children) {
        final AndGeocacheFilter and = new AndGeocacheFilter();
        for (final IGeocacheFilter c : children) {
            and.addChild(c);
        }
        return and;
    }

    private static OrGeocacheFilter or(final IGeocacheFilter... children) {
        final OrGeocacheFilter or = new OrGeocacheFilter();
        for (final IGeocacheFilter c : children) {
            or.addChild(c);
        }
        return or;
    }

    private static NotGeocacheFilter not(final IGeocacheFilter... children) {
        final NotGeocacheFilter not = new NotGeocacheFilter();
        for (final IGeocacheFilter c : children) {
            not.addChild(c);
        }
        return not;
    }

    // =====================================================================
    // Existing tests
    // =====================================================================

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

    // =====================================================================
    // BaseGeocacheFilter.simplify
    // =====================================================================

    @Test
    public void baseFilterSimplifyCriterionNullIsFilteringReturnsSelf() {
        final IGeocacheFilter filter = leaf("A");
        assertThat(filter.simplify(f -> null)).isSameAs(filter);
    }

    @Test
    public void baseFilterSimplifyCriterionNullNotFilteringReturnsAlwaysTrue() {
        // ALWAYS_TRUE.isFiltering() == false  →  simplify must return ALWAYS_TRUE
        assertThat(ConstantGeocacheFilter.ALWAYS_TRUE.simplify(f -> null))
                .isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void baseFilterSimplifyCriterionTrueReturnsAlwaysTrue() {
        final IGeocacheFilter filter = leaf("A");
        assertThat(filter.simplify(f -> Boolean.TRUE))
                .isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void baseFilterSimplifyCriterionFalseReturnsAlwaysFalse() {
        final IGeocacheFilter filter = leaf("A");
        assertThat(filter.simplify(f -> Boolean.FALSE))
                .isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    // =====================================================================
    // AndGeocacheFilter.simplify
    // =====================================================================

    @Test
    public void andSimplifyEmptyAndReturnsSelf() {
        final AndGeocacheFilter andFilter = and();
        assertThat(andFilter.simplify(f -> null)).isSameAs(andFilter);
    }

    @Test
    public void andSimplifySingleChildReturnsChild() {
        final IGeocacheFilter a = leaf("A");
        assertThat(and(a).simplify(f -> null)).isSameAs(a);
    }

    @Test
    public void andSimplifyTwoChildrenReturnsAndWithBoth() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        final IGeocacheFilter result = and(a, b).simplify(f -> null);
        assertThat(result).isInstanceOf(AndGeocacheFilter.class);
        assertThat(result).isNotInstanceOf(NotGeocacheFilter.class);
        assertThat(((AndGeocacheFilter) result).getChildren()).containsExactly(a, b);
    }

    @Test
    public void andSimplifyAlwaysTrueChildFirstAbsorbed() {
        final IGeocacheFilter a = leaf("A");
        // ALWAYS_TRUE must be absorbed; remaining single child returned directly
        final IGeocacheFilter result = and(ConstantGeocacheFilter.ALWAYS_TRUE, a).simplify(f -> null);
        assertThat(result).isSameAs(a);
    }

    @Test
    public void andSimplifyAlwaysTrueChildLastAbsorbed() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter result = and(a, ConstantGeocacheFilter.ALWAYS_TRUE).simplify(f -> null);
        assertThat(result).isSameAs(a);
    }

    @Test
    public void andSimplifyAlwaysFalseChildShortCircuitsToAlwaysFalse() {
        final IGeocacheFilter a = leaf("A");
        assertThat(and(ConstantGeocacheFilter.ALWAYS_FALSE, a).simplify(f -> null))
                .isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    @Test
    public void andSimplifyAllAlwaysTrueReturnsAlwaysTrue() {
        final IGeocacheFilter result = and(
                ConstantGeocacheFilter.ALWAYS_TRUE,
                ConstantGeocacheFilter.ALWAYS_TRUE).simplify(f -> null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void andSimplifyCriterionMarkFirstChildTrueSecondChildReturned() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        // criterion marks a as always-true → absorbed; b remains
        final IGeocacheFilter result = and(a, b).simplify(f -> f == a ? Boolean.TRUE : null);
        assertThat(result).isSameAs(b);
    }

    @Test
    public void andSimplifyCriterionMarkChildFalseReturnsAlwaysFalse() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        final IGeocacheFilter result = and(a, b).simplify(f -> f == a ? Boolean.FALSE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    @Test
    public void andSimplifyNestedAndFlattened() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        final IGeocacheFilter c = leaf("C");
        final IGeocacheFilter result = and(and(a, b), c).simplify(f -> null);
        assertThat(result).isInstanceOf(AndGeocacheFilter.class);
        assertThat(result).isNotInstanceOf(NotGeocacheFilter.class);
        assertThat(((AndGeocacheFilter) result).getChildren()).containsExactlyInAnyOrder(a, b, c);
    }

    @Test
    public void andSimplifyNotChildNotFlattenedIntoAnd() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        final IGeocacheFilter result = and(not(a), b).simplify(f -> null);
        assertThat(result).isInstanceOf(AndGeocacheFilter.class);
        assertThat(result).isNotInstanceOf(NotGeocacheFilter.class);
        // NOT must NOT be unpacked into the parent AND; two children remain
        final List<IGeocacheFilter> children = ((AndGeocacheFilter) result).getChildren();
        assertThat(children).hasSize(2);
        assertThat(children.get(0)).isInstanceOf(NotGeocacheFilter.class);
        assertThat(children.get(1)).isSameAs(b);
    }

    @Test
    public void andSimplifyCriterionOnAndItselfReturnsAlwaysTrue() {
        final AndGeocacheFilter andFilter = and(leaf("A"), leaf("B"));
        final IGeocacheFilter result = andFilter.simplify(f -> f == andFilter ? Boolean.TRUE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void andSimplifyCriterionOnAndItselfReturnsAlwaysFalse() {
        final AndGeocacheFilter andFilter = and(leaf("A"), leaf("B"));
        final IGeocacheFilter result = andFilter.simplify(f -> f == andFilter ? Boolean.FALSE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    // =====================================================================
    // OrGeocacheFilter.simplify
    // =====================================================================

    @Test
    public void orSimplifyEmptyOrReturnsSelf() {
        final OrGeocacheFilter orFilter = or();
        assertThat(orFilter.simplify(f -> null)).isSameAs(orFilter);
    }

    @Test
    public void orSimplifySingleChildReturnsChild() {
        final IGeocacheFilter a = leaf("A");
        assertThat(or(a).simplify(f -> null)).isSameAs(a);
    }

    @Test
    public void orSimplifyTwoChildrenReturnsOrWithBoth() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        final IGeocacheFilter result = or(a, b).simplify(f -> null);
        assertThat(result).isInstanceOf(OrGeocacheFilter.class);
        assertThat(((OrGeocacheFilter) result).getChildren()).containsExactly(a, b);
    }

    @Test
    public void orSimplifyAlwaysFalseChildFirstAbsorbed() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter result = or(ConstantGeocacheFilter.ALWAYS_FALSE, a).simplify(f -> null);
        assertThat(result).isSameAs(a);
    }

    @Test
    public void orSimplifyAlwaysFalseChildLastAbsorbed() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter result = or(a, ConstantGeocacheFilter.ALWAYS_FALSE).simplify(f -> null);
        assertThat(result).isSameAs(a);
    }

    @Test
    public void orSimplifyAlwaysTrueChildShortCircuitsToAlwaysTrue() {
        final IGeocacheFilter a = leaf("A");
        assertThat(or(ConstantGeocacheFilter.ALWAYS_TRUE, a).simplify(f -> null))
                .isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void orSimplifyAllAlwaysFalseReturnsAlwaysFalse() {
        final IGeocacheFilter result = or(
                ConstantGeocacheFilter.ALWAYS_FALSE,
                ConstantGeocacheFilter.ALWAYS_FALSE).simplify(f -> null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    @Test
    public void orSimplifyCriterionMarkFirstChildFalseSecondChildReturned() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        // criterion marks a as always-false → absorbed; b remains
        final IGeocacheFilter result = or(a, b).simplify(f -> f == a ? Boolean.FALSE : null);
        assertThat(result).isSameAs(b);
    }

    @Test
    public void orSimplifyCriterionMarkChildTrueReturnsAlwaysTrue() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        final IGeocacheFilter result = or(a, b).simplify(f -> f == a ? Boolean.TRUE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void orSimplifyNestedOrFlattened() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        final IGeocacheFilter c = leaf("C");
        final IGeocacheFilter result = or(or(a, b), c).simplify(f -> null);
        assertThat(result).isInstanceOf(OrGeocacheFilter.class);
        assertThat(((OrGeocacheFilter) result).getChildren()).containsExactlyInAnyOrder(a, b, c);
    }

    @Test
    public void orSimplifyCriterionOnOrItselfReturnsAlwaysFalse() {
        final OrGeocacheFilter orFilter = or(leaf("A"), leaf("B"));
        final IGeocacheFilter result = orFilter.simplify(f -> f == orFilter ? Boolean.FALSE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    @Test
    public void orSimplifyCriterionOnOrItselfReturnsAlwaysTrue() {
        final OrGeocacheFilter orFilter = or(leaf("A"), leaf("B"));
        final IGeocacheFilter result = orFilter.simplify(f -> f == orFilter ? Boolean.TRUE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    // =====================================================================
    // NotGeocacheFilter.simplify
    // =====================================================================

    @Test
    public void notSimplifySingleChildStaysNot() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter result = not(a).simplify(f -> null);
        assertThat(result).isInstanceOf(NotGeocacheFilter.class);
        assertThat(((NotGeocacheFilter) result).getChildren()).containsExactly(a);
    }

    @Test
    public void notSimplifyDoubleNotSingleInnerChildUnwrapsToChild() {
        final IGeocacheFilter a = leaf("A");
        // NOT(NOT(A)) → A
        assertThat(not(not(a)).simplify(f -> null)).isSameAs(a);
    }

    @Test
    public void notSimplifyDoubleNotMultipleInnerChildrenReturnsPlainAnd() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        // NOT(NOT(A, B)) → AND(A, B)  (plain AND, not NOT)
        final IGeocacheFilter result = not(not(a, b)).simplify(f -> null);
        assertThat(result).isInstanceOf(AndGeocacheFilter.class);
        assertThat(result).isNotInstanceOf(NotGeocacheFilter.class);
        assertThat(((AndGeocacheFilter) result).getChildren()).containsExactly(a, b);
    }

    @Test
    public void notSimplifyAlwaysFalseChildReturnsAlwaysTrue() {
        // NOT(false) → true
        assertThat(not(ConstantGeocacheFilter.ALWAYS_FALSE).simplify(f -> null))
                .isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void notSimplifyAlwaysTrueChildReturnsAlwaysFalse() {
        // NOT(true) → false
        assertThat(not(ConstantGeocacheFilter.ALWAYS_TRUE).simplify(f -> null))
                .isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    @Test
    public void notSimplifyCriterionOnNotItselfReturnsAlwaysTrue() {
        final NotGeocacheFilter notFilter = not(leaf("A"));
        final IGeocacheFilter result = notFilter.simplify(f -> f == notFilter ? Boolean.TRUE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void notSimplifyCriterionOnNotItselfReturnsAlwaysFalse() {
        final NotGeocacheFilter notFilter = not(leaf("A"));
        final IGeocacheFilter result = notFilter.simplify(f -> f == notFilter ? Boolean.FALSE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    @Test
    public void notSimplifyInnerAndFlattened() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        // NOT(AND(A, B)) → NOT(A, B)  — the inner AND is flattened into NOT's own children
        final IGeocacheFilter result = not(and(a, b)).simplify(f -> null);
        assertThat(result).isInstanceOf(NotGeocacheFilter.class);
        assertThat(((NotGeocacheFilter) result).getChildren()).containsExactly(a, b);
    }

    @Test
    public void notSimplifyCriterionMarkChildFalseReturnsAlwaysTrue() {
        final IGeocacheFilter a = leaf("A");
        // NOT( ALWAYS_FALSE ) → ALWAYS_TRUE
        final IGeocacheFilter result = not(a).simplify(f -> f == a ? Boolean.FALSE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_TRUE);
    }

    @Test
    public void notSimplifyCriterionMarkChildTrueReturnsAlwaysFalse() {
        final IGeocacheFilter a = leaf("A");
        // NOT( ALWAYS_TRUE ) → ALWAYS_FALSE
        final IGeocacheFilter result = not(a).simplify(f -> f == a ? Boolean.TRUE : null);
        assertThat(result).isSameAs(ConstantGeocacheFilter.ALWAYS_FALSE);
    }

    @Test
    public void notSimplifyOrChildNotFlattened() {
        final IGeocacheFilter a = leaf("A");
        final IGeocacheFilter b = leaf("B");
        // OR is NOT an AND subtype → must NOT be merged into NOT's children
        final IGeocacheFilter result = not(or(a, b)).simplify(f -> null);
        assertThat(result).isInstanceOf(NotGeocacheFilter.class);
        final List<IGeocacheFilter> children = ((NotGeocacheFilter) result).getChildren();
        assertThat(children).hasSize(1);
        assertThat(children.get(0)).isInstanceOf(OrGeocacheFilter.class);
    }
}
