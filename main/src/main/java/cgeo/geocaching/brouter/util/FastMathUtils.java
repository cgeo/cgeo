package cgeo.geocaching.brouter.util;

/**
 * Some fast approximations to mathematical functions
 *
 * @author ab
 */
public class FastMathUtils {

    private FastMathUtils() {
        // utility class
    }

    /**
     * Approximation to Math.exp for small negative arguments
     */
    public static double exp(double e) {
        final double x = e;
        double f = 1.;
        while (e < -1.) {
            e += 1.;
            f *= 0.367879;
        }
        return f * (1. + x * (1. + x * (0.5 + x * (0.166667 + 0.0416667 * x))));
    }
}
