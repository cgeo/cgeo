package cgeo.geocaching.maps.routing;

import cgeo.geocaching.maps.routing.RouteOrderConstraints.Stage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class RouteOrderConstraintsTest {

    private static Stage a(final int number) {
        return new Stage("ALA", number);
    }

    private static Stage b(final int number) {
        return new Stage("ALB", number);
    }

    private static int[] identity(final int size) {
        final int[] route = new int[size];
        for (int i = 0; i < size; i++) {
            route[i] = i;
        }
        return route;
    }

    @Test
    public void testNoConstraints() {
        final RouteOrderConstraints c = new RouteOrderConstraints(Arrays.asList(null, null, null, null));
        assertThat(c.isEmpty()).isTrue();
        assertThat(c.hasEffectiveSwap()).isTrue();
        final int[] route = { 0, 2, 1, 3 };
        c.repair(route);
        assertThat(route).containsExactly(0, 2, 1, 3);
    }

    @Test
    public void testRepairInitialRoute() {
        // S, A3, x, A1, A2, E
        final RouteOrderConstraints c = new RouteOrderConstraints(Arrays.asList(null, a(3), null, a(1), a(2), null));
        assertThat(c.isEmpty()).isFalse();
        final int[] route = identity(6);
        c.repair(route);
        // positions {1,3,4} get A1 (idx 3), A2 (idx 4), A3 (idx 1)
        assertThat(route).containsExactly(0, 3, 2, 4, 1, 5);
    }

    @Test
    public void testRepairAfterSwap() {
        // S, A1, x, A2, y, A3, E
        final RouteOrderConstraints c = new RouteOrderConstraints(Arrays.asList(null, a(1), null, a(2), null, a(3), null));
        final int[] route = { 0, 4, 2, 3, 1, 5, 6 }; // swapped pos 1 <-> 4
        c.repair(route);
        assertThat(route).containsExactly(0, 4, 2, 1, 3, 5, 6);
    }

    @Test
    public void testTwoGroupsMixed() {
        // S, B2, A2, B1, A1, E
        final RouteOrderConstraints c = new RouteOrderConstraints(Arrays.asList(null, b(2), a(2), b(1), a(1), null));
        final int[] route = identity(6);
        c.repair(route);
        assertThat(route).containsExactly(0, 3, 4, 1, 2, 5);
        assertThat(c.groupOf(1)).isEqualTo(c.groupOf(3));
        assertThat(c.groupOf(2)).isEqualTo(c.groupOf(4));
        assertThat(c.groupOf(1)).isNotEqualTo(c.groupOf(2));
        assertThat(c.groupOf(0)).isEqualTo(-1);
        assertThat(c.groupSize(c.groupOf(1))).isEqualTo(2);
        assertThat(c.groupSize(c.groupOf(2))).isEqualTo(2);
    }

    @Test
    public void testFixedEndpointsNotConstrained() {
        // A3 at start and A1 at end stay fixed, A2 alone in the middle => no group
        final RouteOrderConstraints c = new RouteOrderConstraints(Arrays.asList(a(3), null, a(2), a(1)));
        assertThat(c.isEmpty()).isTrue();
        assertThat(c.groupOf(0)).isEqualTo(-1);
        assertThat(c.groupOf(3)).isEqualTo(-1);
    }

    @Test
    public void testDuplicateStagesKeepOrder() {
        final RouteOrderConstraints c = new RouteOrderConstraints(Arrays.asList(null, a(2), a(1), a(2), null));
        final int[] route = identity(5);
        c.repair(route);
        assertThat(route).containsExactly(0, 2, 1, 3, 4);
    }

    @Test
    public void testHasEffectiveSwap() {
        // only one group in free positions => no effective swap
        assertThat(new RouteOrderConstraints(Arrays.asList(null, a(1), a(2), null)).hasEffectiveSwap()).isFalse();
        // only one free position => no effective swap
        assertThat(new RouteOrderConstraints(Arrays.asList(null, null, null)).hasEffectiveSwap()).isFalse();
        // group plus free item
        assertThat(new RouteOrderConstraints(Arrays.asList(null, a(1), a(2), null, null)).hasEffectiveSwap()).isTrue();
        // two groups
        assertThat(new RouteOrderConstraints(Arrays.asList(null, a(1), b(1), a(2), b(2), null)).hasEffectiveSwap()).isTrue();
    }

    @Test
    public void testIsOrderViolated() {
        assertThat(RouteOrderConstraints.isOrderViolated(Collections.emptyList())).isFalse();
        assertThat(RouteOrderConstraints.isOrderViolated(Arrays.asList(null, a(1), null, a(2), a(3)))).isFalse();
        assertThat(RouteOrderConstraints.isOrderViolated(Arrays.asList(a(1), b(2), a(2), b(1)))).isTrue();
        assertThat(RouteOrderConstraints.isOrderViolated(Arrays.asList(a(1), b(1), a(2), b(2)))).isFalse();
        assertThat(RouteOrderConstraints.isOrderViolated(Arrays.asList(a(2), a(2), a(3)))).isFalse();
        // endpoints are checked as well
        final List<Stage> endpointViolation = Arrays.asList(a(3), null, a(1));
        assertThat(RouteOrderConstraints.isOrderViolated(endpointViolation)).isTrue();
    }
}
