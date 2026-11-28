package cgeo.geocaching.maps.routing;

import java.util.Random;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class RouteOptimizationHelperTest {

    /** random asymmetric distance matrix */
    private static int[][] randomMatrix(final Random random, final int size) {
        final int[][] d = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                d[i][j] = i == j ? 0 : random.nextInt(10000);
            }
        }
        return d;
    }

    private static int[] randomRoute(final Random random, final int size) {
        final int[] route = new int[size];
        for (int i = 0; i < size; i++) {
            route[i] = i;
        }
        for (int i = size - 1; i > 0; i--) {
            final int k = random.nextInt(i + 1);
            final int temp = route[i];
            route[i] = route[k];
            route[k] = temp;
        }
        return route;
    }

    @Test
    public void testSwapDeltaMatchesRecalculation() {
        final Random random = new Random(18908);
        for (int size = 4; size <= 12; size++) {
            for (int run = 0; run < 200; run++) {
                final int[][] d = randomMatrix(random, size);
                final int[] route = randomRoute(random, size);
                final int posA = 1 + random.nextInt(size - 2);
                int posB = 1 + random.nextInt(size - 3);
                if (posB >= posA) {
                    posB++;
                }
                final int before = RouteOptimizationHelper.calculateRouteLength(d, route);
                final int delta = RouteOptimizationHelper.swapDelta(d, route, posA, posB);

                final int temp = route[posA];
                route[posA] = route[posB];
                route[posB] = temp;
                final int after = RouteOptimizationHelper.calculateRouteLength(d, route);

                assertThat(before + delta).as("size %d, swap %d <-> %d", size, posA, posB).isEqualTo(after);
            }
        }
    }

    @Test
    public void testSwapDeltaAdjacentPositions() {
        // route 0-1-2-3, swap positions 1 and 2 => 0-2-1-3
        final int[][] d = {
                { 0, 1, 10, 100 },
                { 2, 0, 20, 200 },
                { 3, 30, 0, 300 },
                { 4, 40, 400, 0 }
        };
        final int[] route = { 0, 1, 2, 3 };
        // old: d01 + d12 + d23 = 1 + 20 + 300 = 321, new: d02 + d21 + d13 = 10 + 30 + 200 = 240
        assertThat(RouteOptimizationHelper.swapDelta(d, route, 1, 2)).isEqualTo(240 - 321);
        assertThat(RouteOptimizationHelper.swapDelta(d, route, 2, 1)).isEqualTo(240 - 321);
    }

    @Test
    public void testRandBounds() {
        for (int i = 0; i < 1000; i++) {
            assertThat(RouteOptimizationHelper.rand(1, 3)).isBetween(1, 3);
        }
        assertThat(RouteOptimizationHelper.rand(5, 5)).isEqualTo(5);
    }
}
