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
    public void ifFunction() {
        assertThat(FormulaUtils.ifFunction(new ValueList())).isEqualTo(Value.of(0));
        assertThat(FormulaUtils.ifFunction(new ValueList().add(Value.of(""), Value.of("a"), Value.of("b")))).isEqualTo(Value.of("b"));
        assertThat(FormulaUtils.ifFunction(new ValueList().add(Value.of("true"), Value.of("a"), Value.of("b")))).isEqualTo(Value.of("a"));
        assertThat(FormulaUtils.ifFunction(new ValueList().add(Value.of(""), Value.of("a")))).isEqualTo(Value.of(0));
        assertThat(FormulaUtils.ifFunction(new ValueList().add(Value.of(0), Value.of("a"), Value.of(1), Value.of("b"), Value.of("c")))).isEqualTo(Value.of("b"));
    }


    @Test
    public void roman() {
        //I=1, V=5, X=10, L=50, C=100, D=500, M=1000
        assertThat(FormulaUtils.roman("")).isEqualTo(0);
        assertThat(FormulaUtils.roman("I")).isEqualTo(1);
        assertThat(FormulaUtils.roman("V")).isEqualTo(5);
        assertThat(FormulaUtils.roman("VIII")).isEqualTo(8);
        assertThat(FormulaUtils.roman("VII I")).isEqualTo(8);
        assertThat(FormulaUtils.roman("VI IV")).isEqualTo(10);
        assertThat(FormulaUtils.roman("IV")).isEqualTo(4);
        assertThat(FormulaUtils.roman("MDCLXVI")).isEqualTo(1666);
        assertThat(FormulaUtils.roman("mmmDDDcccLLLxxxVVViii")).isEqualTo(1666 * 3);
        assertThat(FormulaUtils.roman("MCD")).isEqualTo(1400);
    }

    @Test
    public void vanity() {
        assertThat(FormulaUtils.vanity("")).isEqualTo(0);
        assertThat(FormulaUtils.vanity("A")).isEqualTo(2);
        assertThat(FormulaUtils.vanity("AD")).isEqualTo(23);
        assertThat(FormulaUtils.vanity("ABCDEF")).isEqualTo(222333);
        assertThat(FormulaUtils.vanity("ghijkl")).isEqualTo(444555);
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

        //find many basic calculations
        assertScanFormula("a * b", "a * b");
        assertScanFormula("a / b", "a / b");
        assertScanFormula("a : b", "a : b");
        assertScanFormula("a + b", "a + b");

        assertThat("a x b".replaceAll(" x ", " * ")).isEqualTo("a * b");

        //find x as multiply
        assertScanFormula("a x b", "a * b");

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
