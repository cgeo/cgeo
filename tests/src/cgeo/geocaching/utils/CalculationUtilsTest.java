package cgeo.geocaching.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;



public class CalculationUtilsTest {

    @Test
    public void testEval() {
        assertThat(new CalculationUtils("-2.5 + 3 * (4-1) + 3^3").eval()).isEqualTo(33.5d);
    }

    @Test
    public void testEvalSqrt() {
        assertThat(new CalculationUtils("sqrt(9)").eval()).isEqualTo(3d);
    }

    @Test
    public void testEvalUnknownOperator() {
        try {
            new CalculationUtils("2 & 5").eval();
            fail("should throw IllegalArgumentException on unknown operator");
        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testEvalUnknownFunction() {
        try {
            new CalculationUtils("uknown(42)").eval();
            fail("should throw IllegalArgumentException on unknown function");
        } catch (final IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testUnbalancedParentheses() {
        try {
            new CalculationUtils("3 * (2 + 4 * 2").eval();
            fail("should throw IllegalArgumentException on missing ')'");
        } catch (final IllegalArgumentException ignored) {

        }
    }

}
