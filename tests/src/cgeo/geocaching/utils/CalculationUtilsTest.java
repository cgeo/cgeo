package cgeo.geocaching.utils;

import static org.assertj.core.api.Java6Assertions.assertThat;


import junit.framework.TestCase;

public class CalculationUtilsTest extends TestCase {

    public static void testEval() {
        assertThat(new CalculationUtils("-2.5 + 3 * (4-1) + 3^3").eval()).isEqualTo(33.5d);
    }

    public static void testEvalSqrt() {
        assertThat(new CalculationUtils("sqrt(9)").eval()).isEqualTo(3d);
    }

    public static void testEvalUnknownOperator() {
        try {
            new CalculationUtils("2 & 5").eval();
            fail("should throw IllegalArgumentException on unknown operator");
        } catch (final IllegalArgumentException ignored) {

        }
    }

    public static void testEvalUnknownFunction() {
        try {
            new CalculationUtils("uknown(42)").eval();
            fail("should throw IllegalArgumentException on unknown function");
        } catch (final IllegalArgumentException ignored) {

        }
    }

}
