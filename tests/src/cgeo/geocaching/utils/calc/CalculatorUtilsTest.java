package cgeo.geocaching.utils.calc;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CalculatorUtilsTest {

    @Test
    public void concat() {
        assertThat(CalculatorUtils.concat(new ValueList().add(Value.of("a"), Value.of("b"))).getRaw()).isEqualTo("ab");
    }

    @Test
    public void checksum() {
        assertThat(CalculatorUtils.checksum(255, false)).isEqualTo(12);
        assertThat(CalculatorUtils.checksum(255, true)).isEqualTo(3);
        assertThat(CalculatorUtils.checksum(-255, false)).isEqualTo(12);
        assertThat(CalculatorUtils.checksum(0, false)).isEqualTo(0);
    }

    @Test
    public void letterValue() {
        assertThat(CalculatorUtils.letterValue("abc")).isEqualTo(6);
        assertThat(CalculatorUtils.letterValue("ABC")).isEqualTo(6);
        assertThat(CalculatorUtils.letterValue("")).isEqualTo(0);
        assertThat(CalculatorUtils.letterValue("1234")).isEqualTo(10);
    }

    @Test
    public void rot() {
        assertThat(CalculatorUtils.rot("abc", 1)).isEqualTo("bcd");
        assertThat(CalculatorUtils.rot("abc", 0)).isEqualTo("abc");
        assertThat(CalculatorUtils.rot("abc", -25)).isEqualTo("bcd");
        assertThat(CalculatorUtils.rot("ab1c2", 1)).isEqualTo("bc1d2");
    }

    @Test
    public void scanForFormulas() {
        assertScanFormula("a+b", "a+b");
        assertScanFormula("(a+b)", "(a+b)");
        assertScanFormula("(c+20*b):2+5", "(c+20*b):2+5");

        //ensure that things like the following are NOT found
        assertScanFormula("abcd-efgh");

        //from cache GC86KMW
        assertScanFormula("1. Zwischenstation (Zingg´s Hotel): N 053°2*a,(c+20*b):2+5    E 009°10*b-1,c:4-a+5",
            "2*a", "(c+20*b):2+5", "10*b-1", "c:4-a+5");
    }

    private void assertScanFormula(final String textToScan, final String ... expectedFinds) {
        final List<String> result = CalculatorUtils.scanForFormulas(Collections.singleton(textToScan), null);
        if (expectedFinds == null || expectedFinds.length == 0) {
            assertThat(result).as("Scan: '" + textToScan + "'").isEmpty();
        } else {
            for (int i = 0; i < expectedFinds.length; i++) {
                assertThat(result).as("Scan: '" + textToScan).containsExactlyInAnyOrder(expectedFinds);
            }
        }
    }

}
