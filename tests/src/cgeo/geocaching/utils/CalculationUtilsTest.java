package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CalculationUtilsTest {

    private static double eval(final String expression) {
        return new CalculationUtils(expression).eval();
    }

    @Test
    public void testEval() {
        assertThat(eval("-2.5 + 3 * (4-1) + 3^3")).isEqualTo(33.5d);
    }

    @Test
    public void testEvalSqrt() {
        assertThat(eval("sqrt(9)")).isEqualTo(3d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEvalUnknownOperator() {
        eval("2 & 5");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEvalUnknownFunction() {
        eval("uknown(42)");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnbalancedParentheses() {
        eval("3 * (2 + 4 * 2");
    }

}
