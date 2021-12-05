package cgeo.geocaching.utils.formulas;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class FormulaUtilsTest {

    @Test
    public void substring() {
        assertThat(FormulaUtils.substring("test", 1, 2)).isEqualTo("es");
        assertThat(FormulaUtils.substring(null, 1, 2)).isEqualTo("");
        assertThat(FormulaUtils.substring("t", 1, 2)).isEqualTo("");
        assertThat(FormulaUtils.substring("te", 1, 2)).isEqualTo("e");
    }

    @Test
    public void checksum() {
        assertThat(FormulaUtils.checksum(255, false)).isEqualTo(12);
        assertThat(FormulaUtils.checksum(255, true)).isEqualTo(3);
        assertThat(FormulaUtils.checksum(-255, false)).isEqualTo(12);
        assertThat(FormulaUtils.checksum(0, false)).isEqualTo(0);
    }

    @Test
    public void letterValue() {
        assertThat(FormulaUtils.letterValue("abc")).isEqualTo(6);
        assertThat(FormulaUtils.letterValue("ABC")).isEqualTo(6);
        assertThat(FormulaUtils.letterValue("")).isEqualTo(0);
        assertThat(FormulaUtils.letterValue("1234")).isEqualTo(10);
    }

    @Test
    public void rot() {
        assertThat(FormulaUtils.rot("abc", 1)).isEqualTo("bcd");
        assertThat(FormulaUtils.rot("abc", 0)).isEqualTo("abc");
        assertThat(FormulaUtils.rot("abc", -25)).isEqualTo("bcd");
        assertThat(FormulaUtils.rot("ab1c2", 1)).isEqualTo("bc1d2");
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

        //DON't find dates, times and URLs
        assertScanFormula("13:00");
        assertScanFormula("13:00:00");
        assertScanFormula("2021/11");
        assertScanFormula("2021/11/5");
        assertScanFormula("www.cgeo.com:443");
    }

    private void assertScanFormula(final String textToScan, final String ... expectedFinds) {
        final List<String> result = FormulaUtils.scanForFormulas(Collections.singleton(textToScan), null);
        if (expectedFinds == null || expectedFinds.length == 0) {
            assertThat(result).as("Scan: '" + textToScan + "'").isEmpty();
        } else {
            for (int i = 0; i < expectedFinds.length; i++) {
                assertThat(result).as("Scan: '" + textToScan).containsExactlyInAnyOrder(expectedFinds);
            }
        }
    }

}
