package cgeo.geocaching.utils.formulas;

import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_PARAMETER_COUNT;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE;

import android.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.fail;

public class FormulaUtilsTest {

    @Test
    public void substring() {
        //Test cases independent from "zero-based" or "one-based" index decision
        assertSubString(WRONG_PARAMETER_COUNT.name(), true, false);
        assertSubString(WRONG_PARAMETER_COUNT.name(), true, false, "test", 1, 2, 3);
        assertSubString(WRONG_TYPE.name(), true, false, "test", "text", 2);
        assertSubString(WRONG_TYPE.name(), true, false, "test", 1, "text");
        assertSubString(WRONG_TYPE.name(), true, false, "test", 1, -5);

        //Test cases for one-based indexes
        assertSubString("est", false, false, "test", 2, 3);
        assertSubString(WRONG_TYPE.name(), true, false, "test", 2, 4);
        assertSubString(WRONG_TYPE.name(), true, false, "test", 0);
        assertSubString("test", false, false, "test", 1);
        assertSubString("e", false, false, "test", 2, 1);
        assertSubString("te", false, false, "test", -4, 2);
        assertSubString("est", false, false, "test", -3);
        assertSubString(WRONG_TYPE.name(), true, false, "test", -5);

        //Test cases for zero-based indexes
        assertSubString("est", false, true, "test", 1, 3);
        assertSubString(WRONG_TYPE.name(), true, true, "test", 1, 4);
        assertSubString("test", false, true, "test", 0);
        assertSubString("e", false, true, "test", 1, 1);
        assertSubString("te", false, true, "test", -4, 2);
        assertSubString("est", false, true, "test", -3);
        assertSubString(WRONG_TYPE.name(), true, true, "test", -5);
    }

    private void assertSubString(final String expectedResult, final boolean expectException, final boolean indexStartsWithZero, final Object ... values) {
        try {
            final Value result = FormulaUtils.substring(indexStartsWithZero, ValueList.ofPlain(values));
            if (expectException) {
                fail("Expected FormulaException containing '" + expectedResult + "', got Result '" + result + "'");
            }
            assertThat(result.toString()).isEqualTo(expectedResult);
        } catch (FormulaException fe) {
            if (expectException) {
                assertThat(fe.getMessage()).contains(expectedResult);
            } else {
                fail("Expected result '" + expectedResult + "', got Exception", fe);
            }
        }
    }

    @Test
    public void checksum() {
        //normal
        assertThat(FormulaUtils.checksum(Value.of(255), false)).isEqualTo(12);
        assertThat(FormulaUtils.checksum(Value.of(255), true)).isEqualTo(3);
        assertThat(FormulaUtils.checksum(Value.of("2-55"), false)).isEqualTo(12);
        assertThat(FormulaUtils.checksum(Value.of("2-55"), true)).isEqualTo(3);
        assertThat(FormulaUtils.checksum(Value.of("2 -55"), false)).isEqualTo(12);
        assertThat(FormulaUtils.checksum(Value.of("2 -55"), true)).isEqualTo(3);
        assertThat(FormulaUtils.checksum(Value.of("2a-55"), false)).isEqualTo(13);
        assertThat(FormulaUtils.checksum(Value.of("2a-55"), true)).isEqualTo(4);
        //negative
        assertThat(FormulaUtils.checksum(Value.of(-255), false)).isEqualTo(-12);
        assertThat(FormulaUtils.checksum(Value.of(-255), true)).isEqualTo(-3);
        assertThat(FormulaUtils.checksum(Value.of("-255"), false)).isEqualTo(-12);
        assertThat(FormulaUtils.checksum(Value.of("-255"), true)).isEqualTo(-3);
        assertThat(FormulaUtils.checksum(Value.of(" - 2 55a"), false)).isEqualTo(-13);
        assertThat(FormulaUtils.checksum(Value.of(" - 2 55a"), true)).isEqualTo(-4);
        //zero
        assertThat(FormulaUtils.checksum(Value.of(0), false)).isEqualTo(0);
        assertThat(FormulaUtils.checksum(Value.of(0), true)).isEqualTo(0);
        assertThat(FormulaUtils.checksum(Value.of("0000"), false)).isEqualTo(0);
        assertThat(FormulaUtils.checksum(Value.of("0000"), true)).isEqualTo(0);
        assertThat(FormulaUtils.checksum(Value.of("000 0 "), false)).isEqualTo(0);
        assertThat(FormulaUtils.checksum(Value.of("000 0 "), true)).isEqualTo(0);
        //big numbers
        final String zeroToNine = "1234567890"; //cs = 45
        final String bigNumberBase = zeroToNine + zeroToNine + zeroToNine + zeroToNine; // cs = 180, ics = 9
        assertThat(FormulaUtils.checksum(Value.of(bigNumberBase), false)).isEqualTo(180);
        assertThat(FormulaUtils.checksum(Value.of(bigNumberBase), true)).isEqualTo(9);
        assertThat(FormulaUtils.checksum(Value.of("1-" + bigNumberBase), false)).isEqualTo(181);
        assertThat(FormulaUtils.checksum(Value.of("1-" + bigNumberBase), true)).isEqualTo(1);
        assertThat(FormulaUtils.checksum(Value.of("-" + bigNumberBase), false)).isEqualTo(-180);
        assertThat(FormulaUtils.checksum(Value.of("-" + bigNumberBase), true)).isEqualTo(-9);
    }

    @Test
    public void letterValue() {
        assertThat(FormulaUtils.letterValue("abc")).isEqualTo(6);
        assertThat(FormulaUtils.letterValue("ABC")).isEqualTo(6);
        assertThat(FormulaUtils.letterValue("")).isEqualTo(0);
        assertThat(FormulaUtils.letterValue("1234")).isEqualTo(10);
        assertThat(FormulaUtils.letterValue("-1234")).isEqualTo(10);
        assertThat(FormulaUtils.letterValue("123-4")).isEqualTo(10);
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
        assertScanFormula("abcde-fghi");

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

    @Test(timeout = 1000)
    public void scanForFormulasGCAHQ78() {
        //taken from GCAHQ78, preprocessed by c:geo, shortened for test purposes.
        // Test case is related to issue #15173. The following text leads to ANR / endless scan when processed by "scanFormula"
        final String text =
            "D+C*E\n" +
            "* * * * * * * * * * " +
                "* *        " +
                "             " +
                "* * * * * * * * * " +
                "* * * * * * \n" +
            "“on the Internet” (C, D, E, F) and others “on site” (A, B). …\n" +
            "N 28° A-E+F.A*(D+E)-F*(E+C). / W016° A-B.D+C*E";

        assertScanFormula(text, "A*(D+E)-F*(E+C)", "A-B", "A-E+F", "D+C*E");
    }

    @Test(timeout = 1000)
    public void scanForFormulasGCAHQ78FullText() {
        //taken from GCAHQ78, preprocessed by c:geo. Related to issue #15173
        final String text = "„Da musst du durch!“\n" +
            "\n" +
            "Ein kleiner Rätselcache, der an einen wohl selten besuchten Ort lockt!\n" +
            "\n" +
            "Trau dich!!! Wenn nicht gerade längere Regenzeit ist oder ein Wolkenbruch niedergeht, ist der Cache sicher und recht bequem zu erreichen!\n" +
            "\n" +
            "Einige der Fragen sollten teilweise zuhause „im Internet“ (C, D, E, F)\n" +
            "und andere „vor Ort“ (A, B) einfach zu beantworten sein. …\n" +
            "\n" +
            "Dann ist nur noch eine nervige Rechnerei nötig!    :{\n" +
            "\n" +
            " \n" +
            "\n" +
            " \n" +
            "\n" +
            "A= Bestimme den Buchstabenwortwert (BWW: A=1,B=2, C=3, …) der ganz nah verlaufenden Fernverkehrsstraße.\n" +
            "\n" +
            "B= Wie viele „Spielgeräte“ hat jeder Spieler für die Freizeitsportart, die auf der gegenüberliegenden Straßenseite von den Park-/Startkoordinaten gespielt wird, wenn jede Mannschaft aus 1 oder 2 Spielern besteht?\n" +
            "\n" +
            "C= BWW des 1. Buchstaben des Landes, in dem der Cache liegt – in Landessprache!\n" +
            "\n" +
            "D= BWW des 1. Buchstaben des Landes, in dem der Cache liegt – in deutsch oder englisch!\n" +
            "\n" +
            "E= BWW des 1. Buchstaben der Insel, auf der der Cache liegt.\n" +
            "\n" +
            "F= BWW des 1. Buchstaben der Gemeinde, in dem das Fragezeichen liegt.\n" +
            "\n" +
            " \n" +
            "\n" +
            "Parken/Start:\n" +
            "\n" +
            "N28° 14.[867-(C+D+E+F)] / W16° 24.[C*D-F]\n" +
            " \n" +
            "\n" +
            "FINAL (Lösungsformel):\n" +
            "\n" +
            "N 28° A-E+F.A*(D+E)-F*(E+C). / W016° A-B.D+C*E\n" +
            "\n" +
            " \n" +
            "\n" +
            "* * * * * * * * * * * * * * *                     * * * * * * * * * * * * * * *                     * * * * * * * * * * * * * * * \n" +
            "\n" +
            "Espanol:\n" +
            "\n" +
            "\"¡Tienes que pasar por esto!\"\n" +
            "\n" +
            "¡Un pequeño caché de rompecabezas que te atrae a un lugar que probablemente rara vez se visita!\n" +
            "\n" +
            "¡¡¡Te atreves!!! A menos que haya una larga temporada de lluvias o un chaparrón, ¡el caché es seguro y de fácil acceso!\n" +
            "\n" +
            "Algunas de las preguntas deberían ser fáciles de responder en casa\n" +
            "“en Internet” (C, D, E, F) y otras “in situ” (A, B). …\n" +
            "\n" +
            "¡Entonces todo lo que necesitas son algunos cálculos molestos!   :{\n" +
            "\n" +
            " \n" +
            "\n" +
            " \n" +
            "\n" +
            "A= Determine el valor de la palabra letra (BWW: A=1, B=2, C=3, …) de la carretera que pasa muy cerca.\n" +
            "\n" +
            "B= ¿Cuántos \"equipos de juego\" tiene cada jugador para el deporte recreativo que se juega frente al parque/coordenadas de inicio si cada equipo consta de 1 o 2 jugadores?\n" +
            "\n" +
            "C= BWW de la primera letra del país en el que se encuentra el caché - ¡en el idioma nacional!\n" +
            "\n" +
            "D= BWW de la primera letra del país en el que se encuentra el caché - ¡en alemán o inglés!\n" +
            "\n" +
            "E= [BWW de la primera letra de la isla donde se encuentra el caché.\n" +
            "\n" +
            "F= BWW de la primera letra del municipio en el que se encuentra el signo de interrogación\n" +
            "\n" +
            "Estacionar/Iniciar:\n" +
            "\n" +
            "N28° 14.[867-(C+D+E+F)] / W16° 24.[C*D-F]\n" +
            "\n" +
            "FINAL (Fórmula de solución):\n" +
            "\n" +
            "N 28° A-E+F.A*(D+E)-F*(E+C). / W016° A-B.D+C*E\n" +
            "\n" +
            " \n" +
            "\n" +
            "* * * * * * * * * * * * * * *                     * * * * * * * * * * * * * * *                     * * * * * * * * * * * * * * * \n" +
            "\n" +
            "English:\n" +
            "\n" +
            "“You have to go through it!”\n" +
            "\n" +
            "A small puzzle cache that lures you to a place that is probably rarely visited!\n" +
            "\n" +
            "You dare!!! Unless there is a long rainy season or a cloudburst, the cache is safe and quite easy to reach!\n" +
            "\n" +
            "Some of the questions should be easy to answer at home\n" +
            "“on the Internet” (C, D, E, F) and others “on site” (A, B). …\n" +
            "\n" +
            "Then all you need is some annoying calculations!   :{\n" +
            "\n" +
            " \n" +
            "\n" +
            " \n" +
            "\n" +
            "A= Determine the letter word value (BWW: A=1, B=2, C=3, …) of the highway that runs very close.\n" +
            "\n" +
            "B= How many \"play equipment\" does each player have for the recreational sport played across the street from the park/start coordinates if each team consists of 1 or 2 players\n" +
            "\n" +
            "C= BWW of the 1st letter of the country in which the cache is located - in the national language!\n" +
            "\n" +
            "D= BWW of the 1st letter of the country in which the cache is located - in German or English!\n" +
            "\n" +
            "E= BWW of the 1st letter of the island where the cache is located.\n" +
            "\n" +
            "F= BWW of the 1st letter of the municipality in which the question mark is located.\n" +
            "\n" +
            " \n" +
            "\n" +
            "Park/Start:\n" +
            "\n" +
            "N28° 14.[867-(C+D+E+F)] / W16° 24.[C*D-F]\n" +
            "\n" +
            "FINAL (Solution formula):\n" +
            "\n" +
            "N 28° A-E+F.A*(D+E)-F*(E+C). / W016° A-B.D+C*E";

        //this test is not so much about found content but about timing.
        //nevertheless check the most important results
        assertScanFormula(text, false, "A*(D+E)-F*(E+C)", "A-B", "A-E+F", "C*D-F", "D+C*E");
    }

    @Test
    public void scanForFormulasGCAWBHB() {
        final String description = "Nord = KM + BCO + AFJ + DGN + AF + K + EH + CD + I - L\n" +
                "Ost = KM + BO + AF + GH + CDJN + AIN + DE + GH + A + B + C + F + I + N";
        assertScanFormula(description, "KM + BCO + AFJ + DGN + AF + K + EH + CD + I - L", "KM + BO + AF + GH + CDJN + AIN + DE + GH + A + B + C + F + I + N");
    }

    private void assertScanFormula(final String textToScan, final String... expectedFinds) {
        assertScanFormula(textToScan, true, expectedFinds);
    }
    private void assertScanFormula(final String textToScan, final boolean checkExact, final String... expectedFinds) {
        final List<String> result = FormulaUtils.scanForFormulas(Collections.singleton(textToScan), null);
        if (expectedFinds == null || expectedFinds.length == 0) {
            assertThat(result).as("Scan: '" + textToScan + "'").isEmpty();
        } else if (checkExact) {
            assertThat(result).as("Scan: '" + textToScan).containsExactlyInAnyOrder(expectedFinds);
        } else {
            assertThat(result).as("Scan: '" + textToScan).containsAll(Arrays.asList(expectedFinds));
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

        assertScanCoordinates("[N 49° 4(A-1).(B*50+85)]\n[E 008° (B+C+A).(D+45)]", "N 49° 4(A-1).(B*50+85)]|E 008° (B+C+A).(D+45)]");
        //from https://coord.info/GCW1GJ
        assertScanCoordinates("N 48° 04.ABE,\n E 11° 56.DEB.", "N 48° 04.ABE|E 11° 56.DEB");
        assertScanCoordinates("N 48° 0B.(2x C)(2x C)(2x C),\n  E 11° BB.(G+H)DF", "N 48° 0B.(2* C)(2* C)(2* C)|E 11° BB.(G+H)DF");
        assertScanCoordinates("N 48° 0B.0[I+J*D],\nE 11° BB.D(J/4)D", "N 48° 0B.0[I+J*D]|E 11° BB.D(J/4)D");

        assertScanCoordinates("N 45° A.B(C+D)'  E 9° (A-B).(2*D)EF\n", "N 45° A.B(C+D)'|E 9° (A-B).(2*D)EF");
        assertScanCoordinates("N 45° A.B(C+D!) E 9°(A-B).(2*D)EF", "N 45° A.B(C+D!)|E 9°(A-B).(2*D)EF");
    }

    @Test
    public void scanLongText() {
        assertScanCoordinates("N 50° 00.A + B + C + D + E + F + G + H + I + J + K + L + M\nE 009° 00.A * B * C * ( N + O ) + P + Q + R + S - T - U - V - W",
                "N 50° 00.A + B + C + D + E + F + G + H + I + J + K + L + M|E 009° 00.A * B * C * ( N + O ) + P + Q + R + S - T - U - V - W");
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
