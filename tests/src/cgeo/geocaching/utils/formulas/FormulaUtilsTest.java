package cgeo.geocaching.utils.formulas;

import android.util.Pair;

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
        assertThat(FormulaUtils.letterValue("äöüß")).isEqualTo(27 + 28 + 29 + 30);
        assertThat(FormulaUtils.letterValue("ÄÖÜß")).isEqualTo(27 + 28 + 29 + 30);
        assertThat(FormulaUtils.letterValue("--")).isEqualTo(0);
        assertThat(FormulaUtils.letterValue("Ç")).isEqualTo(3);
        assertThat(FormulaUtils.letterValue("eêéè")).isEqualTo(20);
        assertThat(FormulaUtils.letterValue("^ee^ä")).isEqualTo(37);
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

    @Test
    public void scanForFormulasNoBreakSpaceDoesNotSplitFormulas() {
        assertScanFormula("X\u00A0+\u00A0Y", "X + Y");
    }

    @Test
    public void scanForFormulasGC96KBEFindsAllFormulas() {
        assertScanFormula("N 48° (F-H)/2-1.((J-H)*I-2*A+E+9) E 008° (A/G+12).(A+D)*B/2-6*C+49", "(F-H)/2-1", "((J-H)*I-2*A+E+9)", "(A/G+12)", "(A+D)*B/2-6*C+49");
    }

    private void assertScanFormula(final String textToScan, final String... expectedFinds) {
        final List<String> result = FormulaUtils.scanForFormulas(Collections.singleton(textToScan), null);
        if (expectedFinds == null || expectedFinds.length == 0) {
            assertThat(result).as("Scan: '" + textToScan + "'").isEmpty();
        } else {
            for (int i = 0; i < expectedFinds.length; i++) {
                assertThat(result).as("Scan: '" + textToScan).containsExactlyInAnyOrder(expectedFinds);
            }
        }
    }

    @Test
    public void scanForCoordinatesBasics() {
        assertScanCoordinates("N48 12.345 E10 67.890", "N48 12.345|E10 67.890");
        assertScanCoordinates("N48° 12.345' E10° 67.890'", "N48° 12.345'|E10° 67.890'");
        assertScanCoordinates("N48° 12.ABC' E10 67.DEF", "N48° 12.ABC'|E10 67.DEF");

        assertScanCoordinates("text before N48 12.ABC text inbetween E10 67.DEF txt after", "N48 12.ABC|E10 67.DEF");
        assertScanCoordinates("text before N48° 12.ABC' text inbetween E10° 67.DEF' txt after", "N48° 12.ABC'|E10° 67.DEF'");

        assertScanCoordinates("N48 12.(A+1)(B/2)(C/2) text inbetween E10 67.(D+D) (E+E) F txt after", "N48 12.(A+1)(B/2)(C/2)|E10 67.(D+D) (E+E) F");

        assertScanCoordinates("N 053°2*a,(c+20*b):2+5  E 009°10*b-1,c:4-a+5", "N 053°2*a.(c+20*b):2+5|E 009°10*b-1.c:4-a+5");
    }

    @Test
    public void scanCoordinatesRemoveDuplicates() {
        assertScanCoordinates("N48 12.345 E10 67.890 something else N48 12.345 E10 67.890", "N48 12.345|E10 67.890");
    }

    @Test
    public void scanForCoordinatesGC86KMW() {
        final String description = "Gründungsjahr c.\n" +
                "\n" +
                "1. Zwischenstation (Zingg´s Hotel): N 053°2*a,(c+20*b):2+5      E 009°10*b-1,c:4-a+5 \n" +
                "\n" +
                "Hier findest du eine Hilfe, um die Koordinaten von Zwischenstation 2 zu bekommen.\n" +
                "\n" +
                "2. Zwischenstation:\n" +
                "\n" +
                "Hier findest du einen Begriff, wenn auch nicht als Nomen, der typisch ist für Gesellschaften mit kapitalistischer Produktionsweise. BWW = d.\n" +
                "\n" +
                "Final: N 053° 33,13*(d+7)+1    E 09° 59,18*(d-6)+2\n" +
                "\n" +
                " \n" +
                "\n" +
                "English Version";

        assertScanCoordinates(description, "N 053°2*a.(c+20*b):2+5|E 009°10*b-1.c:4-a+5", "N 053° 33.13*(d+7)+1|E 09° 59.18*(d-6)+2");
    }

    @Test
    public void scanForCoordinatesRealLifeCases() {
        //from Issue #12867
        assertScanCoordinates("N 49° 53.(H+1) (H-A) (B-2) \nE 008° 37. (B) (C) (H-1)", "N 49° 53.(H+1) (H-A) (B-2)|E 008° 37. (B) (C) (H-1)");
        assertScanCoordinates("N 49° (A + 42).0(B + 10)\nE 008° (C*6 + 2).(D + 256)", "N 49° (A + 42).0(B + 10)|E 008° (C*6 + 2).(D + 256)");
        assertScanCoordinates("N AB CD.EFG E HI JK.LMN", "N AB CD.EFG|E HI JK.LMN");

        assertScanCoordinates("[N 49° 4(A-1).(B*50+85)]\n[E 008° (B+C+A).(D+45)]", "N 49° 4(A-1).(B*50+85))|E 008° (B+C+A).(D+45))");
        //from https://coord.info/GCW1GJ
        assertScanCoordinates("N 48° 04.ABE,\n E 11° 56.DEB.", "N 48° 04.ABE|E 11° 56.DEB");
        assertScanCoordinates("N 48° 0B.(2x C)(2x C)(2x C),\n  E 11° BB.(G+H)DF", "N 48° 0B.(2* C)(2* C)(2* C)|E 11° BB.(G+H)DF");
        assertScanCoordinates("N 48° 0B.0[I+J*D],\nE 11° BB.D(J/4)D", "N 48° 0B.0(I+J*D)|E 11° BB.D(J/4)D");

    }

    private void assertScanCoordinates(final String textToScan, final String... expectedFindPairs) {
        final List<Pair<String, String>> result = FormulaUtils.scanForCoordinates(Collections.singleton(textToScan), null);
        if (expectedFindPairs == null || expectedFindPairs.length == 0) {
            assertThat(result).as("ScanCoord: '" + textToScan + "'").isEmpty();
        } else {
            assertThat(result.size()).as("Didn't find number of expected coordinate pairs").isEqualTo(expectedFindPairs.length);
            int idx = 0;
            for (String expectedPair : expectedFindPairs) {
                final String desc = "-ScanCoord(" + idx + ") in '" + textToScan + "'";
                assertThat(result.get(idx).first + "|" + result.get(idx).second).as(desc).isEqualTo(expectedPair);
                idx++;
            }
        }
    }

}
