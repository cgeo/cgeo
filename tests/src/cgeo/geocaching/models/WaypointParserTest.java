package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class WaypointParserTest {

    private static WaypointParser createParser(final String prefix) {
        return new WaypointParser(null, prefix, true);
    }

    @Test
    public void testParseNoWaypoints() {
        final String note = "1 T 126\n" +
                "2 B 12\n" +
                "3 S 630\n" +
                "4c P 51\n" +
                "L 1\n" +
                "E 14\n" +
                "J 11\n" +
                "U 12\n" +
                "D 1\n" +
                "M 7\n" +
                "N 5\n" +
                "5 IFG 257";
        final WaypointParser waypointParser = createParser("Prefix");
        assertThat(waypointParser.parseWaypoints(note)).isEmpty();
    }

    @Test
    public void testParseWaypointsOneLine() {
        final String note = "Dummy note\nn 45° 3.5 e 27° 7.5\nNothing else";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        assertWaypoint(waypoints.iterator().next(), "Prefix 1", new Geopoint("N 45°3.5 E 27°7.5"));
    }

    @Test
    public void testParseWaypointsUserNoteWithDotAndWord() {
        final String note = "1. 0815 - Word 1\n2. 4711 - Word 2\nn 45° 3.565 e 27° 7.578";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Waypoint wp = waypoints.iterator().next();
        assertWaypoint(wp, "Prefix 1", new Geopoint("N 45°3.565 E 27°7.578"));
        assertThat(wp.getUserNote()).isEmpty();
    }

    @Test
    public void testParseWaypointsUserNoteWithDot() {
        final String note = "1. 0815\n2. 4711\nn 45° 3.565 e 27° 7.578";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(2);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp1 = iterator.next();
        assertWaypoint(wp1, "Prefix 1", new Geopoint("N 01° 4.890 E 2°28.266"));
        assertThat(wp1.getUserNote()).isEmpty();
        final Waypoint wp2 = iterator.next();
        assertWaypoint(wp2, "Prefix 2", new Geopoint("N 45°3.565 E 27°7.578"));
        assertThat(wp2.getUserNote()).isEmpty();
    }

    @Test
    public void testParseWaypointsUserNoteWithDotTwice() {
        final String note = "1. 0815  2. 4711. 0815 2.4711 \n n 45° 3.565 e 27° 7.578";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(2);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp1 = iterator.next();
        assertWaypoint(wp1, "Prefix 1", new Geopoint("N 01° 4.890 E 2°28.266"));
        assertThat(wp1.getUserNote()).isEqualTo(". 0815 2.4711");
        final Waypoint wp2 = iterator.next();
        assertWaypoint(wp2, "Prefix 2", new Geopoint("N 45°3.565 E 27°7.578"));
        assertThat(wp2.getUserNote()).isEmpty();
    }

    private static void parseAndAssertFirstWaypoint(final String text, final String name, final WaypointType wpType, final String userNote) {
        final WaypointParser waypointParser = createParser("Praefix");
        final Collection<Waypoint> coll = waypointParser.parseWaypoints(text);
        assertThat(coll.size()).isEqualTo(1);
        final Iterator<Waypoint> iterator = coll.iterator();
        final Waypoint wp = iterator.next();
        assertWaypoint(wp, name, wp.getCoords(), wpType, userNote);
    }

    private static void assertWaypoint(final Waypoint waypoint, final Waypoint expectedWaypoint) {
        assertWaypoint(waypoint, expectedWaypoint.getName(), expectedWaypoint.getCoords(), expectedWaypoint.getWaypointType(), expectedWaypoint.getUserNote());
    }

    private static void assertWaypoint(final Waypoint waypoint, final String name, final Geopoint geopoint, final WaypointType wpType, @Nullable final String userNote) {
        assertWaypoint(waypoint, name, geopoint);
        assertThat(waypoint.getWaypointType()).isEqualTo(wpType);
        if (null != userNote) {
            assertThat(waypoint.getUserNote()).isEqualTo(userNote);
        }
    }


    private static void assertWaypoint(final Waypoint waypoint, final String name, final Geopoint geopoint) {
        assertThat(waypoint.getName()).isEqualTo(name);
        assertThat(waypoint.getCoords()).isEqualTo(geopoint);
    }

    @Test
    public void testParseWaypointsMultiLine() {
        final String note2 = "Waypoint on two lines\nN 45°3.5\nE 27°7.5\nNothing else";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note2);
        assertThat(waypoints).hasSize(1);
        assertWaypoint(waypoints.iterator().next(), "Prefix 1", new Geopoint("N 45°3.5 E 27°7.5"));
    }

    /**
     * Taken from GCM4Y8
     */
    @Test
    public void testParseWaypointsMultiLineWithDuplicates() {
        final String text = "La cache si ... (N45 49.739 E9 45.038 altitudine 860 m. s.l.m.), si prosegue ...\n" +
                "Proseguendo ancora nel sentiero ... all’agriturismo La Peta (N45 50.305 E9 43.991) vi è possibilità di pranzare e soggiornare.\n" +
                "You go to Costa Serina ... sanctuary “Mother of the snow” (N45 49.739 E9 45.038); then you have a walk towards Tagliata...\n" +
                "The path is part of two paths ... is a rural restaurant called \"la Peta\" (N45 50.305 E9 43.991): here you are able to have lunch ...";

        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(text);
        assertThat(waypoints).hasSize(4);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        assertWaypoint(iterator.next(), "Prefix 1", new Geopoint("N 45°49.739 E 9°45.038"));
        assertWaypoint(iterator.next(), "Prefix 2", new Geopoint("N 45°50.305 E 9°43.991"));
        assertWaypoint(iterator.next(), "Prefix 3", new Geopoint("N 45°49.739 E 9°45.038"));
        assertWaypoint(iterator.next(), "Prefix 4", new Geopoint("N 45°50.305 E 9°43.991"));
    }

    @Test
    public void testParseWaypointWithNameAndDescription() {
        final String note = "@WPName X N45 49.739 E9 45.038 this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        assertWaypoint(iterator.next(), "WPName", new Geopoint("N 45°49.739 E 9°45.038"), WaypointType.PUZZLE, "this is the description");
    }

    @Test
    public void testParseWaypointWithMultiwordNameAndMultilineDescription() {
        final String note = "@ A   longer  name \twith (r) (o) whitespaces  N45 49.739 E9 45.038 \"this is the \\\"description\\\"\nit goes on and on\" some more text";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        assertWaypoint(iterator.next(), "A longer name with whitespaces", new Geopoint("N 45°49.739 E 9°45.038"), WaypointType.OWN,
                "this is the \"description\"\nit goes on and on");
    }


    @Test
    public void testParseWaypointWithNameAndNoDescription() {
        final String note = "@WPName X N45 49.739 E9 45.038\nthis shall NOT be part of the note";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        assertWaypoint(iterator.next(), "WPName", new Geopoint("N 45°49.739 E 9°45.038"), WaypointType.PUZZLE, "");
    }

    @Test
    public void testCreateParseableWaypointTextAndParseIt() {
        final Waypoint wp = new Waypoint("name", WaypointType.FINAL, true);
        final Geopoint gp = new Geopoint("N 45°49.739 E 9°45.038");
        wp.setCoords(gp);
        wp.setPrefix("PR");
        wp.setUserNote("user note with \"escaped\" text");
        assertThat(WaypointParser.getParseableText(wp)).isEqualTo(
                "@name (F) " + toParseableWpString(gp) + " " +
                        "\"user note with \\\"escaped\\\" text\"");

        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> parsedWaypoints = waypointParser.parseWaypoints(WaypointParser.getParseableText(wp));
        assertThat(parsedWaypoints).hasSize(1);
        final Iterator<Waypoint> iterator = parsedWaypoints.iterator();
        assertWaypoint(iterator.next(), wp);

    }

    @Test
    public void testCreateParseableWaypointTextWithoutCoordinateAndParseIt() {
        final Waypoint wp = new Waypoint("name", WaypointType.FINAL, false);
        wp.setCoords(null);
        wp.setPrefix("EE");
        wp.setUserNote("user note with \"escaped\" text\nand a newline");
        assertThat(WaypointParser.getParseableText(wp)).isEqualTo(
                "@[EE]name (F) (NO-COORD)\n" +
                        "\"user note with \\\"escaped\\\" text\nand a newline\"");

        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> parsedWaypoints = waypointParser.parseWaypoints(WaypointParser.getParseableText(wp));
        assertThat(parsedWaypoints).hasSize(1);
        final Iterator<Waypoint> iterator = parsedWaypoints.iterator();
        assertWaypoint(iterator.next(), wp);

    }

    @Test
    public void testCreateParseableWaypointTextUserWpWithoutCoordinateAndParseIt() {
        final Waypoint wp = new Waypoint("name", WaypointType.FINAL, false);
        wp.setCoords(null);
        wp.setUserDefined();
        wp.setUserNote("user note with \"escaped\" text\nand a newline");
        final String parseableText = WaypointParser.getParseableText(wp);
        assertThat(parseableText).isEqualTo(
                "@name (F) (NO-COORD)\n" +
                        "\"user note with \\\"escaped\\\" text\nand a newline\"");

        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> parsedWaypoints = waypointParser.parseWaypoints(parseableText);
        assertThat(parsedWaypoints).hasSize(1);
        final Iterator<Waypoint> iterator = parsedWaypoints.iterator();
        final Waypoint newWp = iterator.next();
        assertWaypoint(newWp, wp);
        assertThat(newWp.isUserDefined()).isTrue();
    }

    @Test
    public void testParseTwoUserDefinedWaypointWithSameNameAndWithoutCoordinate() {
        final String parseableText = "@name (F) (NO-COORD)\n" +
                "\"user note with \\\"escaped\\\" text\nand a newline\"" +
                "@name (F) (NO-COORD)\n" +
                "\"user note 2 with \\\"escaped\\\" text\nand a newline\"";

        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> parsedWaypoints = waypointParser.parseWaypoints(parseableText);
        assertThat(parsedWaypoints).hasSize(2);

    }

    /**
     * Parse Waypoint with formula and variables and get parseable text.
     * Formula and description should be correct.
     */
    @Test
    public void testParseWaypointWithFormulaAndCreateParseableWaypointText() {
        final String note = "@name (F) " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)  E 9° (A-B).(2*D)EF | A = a + b |B=|a=2|b=| this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();

        final String parseableText = WaypointParser.getParseableText(wp, true);
        assertThat(parseableText).isEqualTo(
                "@name (F) " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)' E 9° (A-B).(2*D)EF' |A=a + b|a=2| \"this is the description\"");
    }

    /**
     * Parse Waypoint with formula and variables and get parseable text.
     * Formula and description should be correct.
     */
    @Test
    public void testParseWaypointWithCompleteFormulaAndCreateParseableWaypointText() {
        final String note = "@name (F) " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)  E 9° (A-B).(2*D)EF | A = a + b |B=1|a=2|b=47|C=10|D=4|E=2|F=3| this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();

        final String parseableText = WaypointParser.getParseableText(wp, true);
        assertThat(parseableText).isEqualTo(
                "@name (F) " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)' E 9° (A-B).(2*D)EF' |A=a + b|B=1|C=10|D=4|E=2|F=3|a=2|b=47| \"this is the description\"");
    }

    /**
     * Waypoint with formula and variables should be created
     */
    @Test
    public void testParseWaypointWithFormulaWithNameAndDescription() {
        final String note = "@WPName X " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + "N 45° A.B(C+D)  E 9° (A-B).(2*D)EF |A = a+b|B=|a=2|b=| this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();
        assertWaypoint(wp, "WPName", null, WaypointType.PUZZLE, "this is the description");
        final String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        final CalcState calcState = CalcState.fromJSON(calcStateJson);
        assertThat(calcState.plainLat).isEqualTo("N 45° A.B(C+D)'");
        assertThat(calcState.plainLon).isEqualTo("E 9° (A-B).(2*D)EF'");
        assertThat(calcState.equations).hasSize(6);
        assertThat(calcState.freeVariables).hasSize(2);
    }

    /**
     * Waypoint with formula with lower case letters and variables should be created without formula
     * - only upper case letters are supported
     */
    @Test
    public void testParseWaypointWithFormulaWithLowerCase() {
        final String note = WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° a.b(C+D)  E 9° (A-B).(2*D)EF |A = a+b||B=|a=2|b=| this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();
        assertWaypoint(wp, "Prefix 1", null, WaypointType.WAYPOINT, null);
        final String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNull();
    }

    /**
     * Waypoint with calculated Geopoint should be created
     */
    @Test
    public void testParseWaypointWithFormulaEvaluateCoordinates() {
        final String note = "@WPName X " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)  E 9° (A-B).(2*D)EF |A = a*b|B=3|C=8|D=4|E=b-a|F=b/3|a=2|b=9| this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();
        assertWaypoint(wp, "WPName", new Geopoint("N 45 18.312", "E 9 15.873"), WaypointType.PUZZLE, "this is the description");
        final String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        final CalcState calcState = CalcState.fromJSON(calcStateJson);
        assertThat(calcState.plainLat).isEqualTo("N 45° A.B(C+D)'");
        assertThat(calcState.plainLon).isEqualTo("E 9° (A-B).(2*D)EF'");
    }

    /**
     * Formula incomplete, Geopoint should not be created
     */
    @Test
    public void testParseWaypointWithFormulaEvaluateIncompleteCoordinates() {
        final String note = "@WPName X " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° 42.ABC  E 9° 7.DEB |A = a*b|B=3|E=b-a|a=2| this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();
        assertWaypoint(wp, "WPName", null, WaypointType.PUZZLE, "this is the description");
        final String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        final CalcState calcState = CalcState.fromJSON(calcStateJson);
        assertThat(calcState.plainLat).isEqualTo("N 45° 42.ABC'");
        assertThat(calcState.plainLon).isEqualTo("E 9° 7.DEB'");
    }

    /**
     * 2 Waypoints with formula and variables should be created
     */
    @Test
    public void testParseTwoWaypointsWithFormulaAndNameAndDescription() {
        final String note = "@WPName 1 X " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)'  E 9° (A-B).(2*D)EF\n" +
                "@WPName 2 X " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45 C.A(D+B)'  E 9 (D-C).(2*A)EF' |A = a+b|B=|a=2|b=| \"this is the description for the second point\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(2);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        Waypoint wp = iterator.next();
        assertWaypoint(wp, "WPName 1", null, WaypointType.PUZZLE, "");
        String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        CalcState calcState = CalcState.fromJSON(calcStateJson);
        assertThat(calcState.plainLat).isEqualTo("N 45° A.B(C+D)'");
        assertThat(calcState.plainLon).isEqualTo("E 9° (A-B).(2*D)EF'");
        assertThat(calcState.equations).hasSize(6);
        assertThat(calcState.freeVariables).hasSize(0);

        wp = iterator.next();
        assertWaypoint(wp, "WPName 2", null, WaypointType.PUZZLE, "this is the description for the second point");
        calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        calcState = CalcState.fromJSON(calcStateJson);
        assertThat(calcState.plainLat).isEqualTo("N 45° C.A(D+B)'");
        assertThat(calcState.plainLon).isEqualTo("E 9° (D-C).(2*A)EF'");
        assertThat(calcState.equations).hasSize(6);
        assertThat(calcState.freeVariables).hasSize(2);
    }

    /**
     * Waypoint with formula and variables should be created
     */
    @Test
    public void testParseWaypointWithFormulaPrefix() {
        final String note = "@[S2]Stage 2 X " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)  E 9° (A-B).(2*D)EF \"this is the description\"\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();
        assertWaypoint(wp, "Stage 2", null, WaypointType.PUZZLE, "this is the description");
        final String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        assertThat(wp.getPrefix()).isEqualTo("S2");
    }

    /**
     * Waypoint with formula and variables should be created
     */
    @Test
    public void testParseWaypointWithFormulaWithoutDescription() {
        final String note = "@[S2]Stage 2 X " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)  E 9° (A-B).(2*D)EF |A = a + b|B=|a=|b=3|";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();
        assertWaypoint(wp, "Stage 2", null, WaypointType.PUZZLE, "");
        final String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        final CalcState calcState = CalcState.fromJSON(calcStateJson);
        assertThat(calcState.equations).hasSize(6);
        assertThat(calcState.equations.get(0).getExpression()).isEqualTo("a + b");
        assertThat(calcState.equations.get(1).getExpression()).isEqualTo("");
        assertThat(calcState.freeVariables).hasSize(2);
        assertThat(calcState.freeVariables.get(0).getExpression()).isEqualTo("");
        assertThat(calcState.freeVariables.get(1).getExpression()).isEqualTo("3");
    }

    /**
     * Waypoint with formula and variables should be created with automatic calculated name
     */
    @Test
    public void testParseWaypointWithFormulaWithoutName() {
        final String note = WaypointParser.PARSING_COORD_FORMULA_PLAIN + " N 45° A.B(C+D)  E 9° (A-B).(2*D)EF |A = a+b||B=|a=2|b=| this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);
        final Iterator<Waypoint> iterator = waypoints.iterator();
        final Waypoint wp = iterator.next();
        assertWaypoint(wp, "Prefix 1", null, WaypointType.WAYPOINT, "this is the description");
        final String calcStateJson = wp.getCalcStateConfig();
        assertThat(calcStateJson).isNotNull();
        final CalcState calcState = CalcState.fromJSON(calcStateJson);
        assertThat(calcState.plainLat).isEqualTo("N 45° A.B(C+D)'");
        assertThat(calcState.plainLon).isEqualTo("E 9° (A-B).(2*D)EF'");
        assertThat(calcState.equations).hasSize(6);
        assertThat(calcState.freeVariables).hasSize(2);
    }

    @Test
    public void testParseWaypointWithFormulaStability() {
        //parse formulas for waypoints which might lead to unexpected fillings (and NEVER to exceptions...)
        final String formulaTypeStr = " " + WaypointParser.PARSING_COORD_FORMULA_PLAIN + " ";
        final String formulaStr = " N 45° A.B(C+D) E 9°(A-B).(2*D)EF ";
        final String variableStrWithoutDelim = "A=a+b|B=2|C=3|D=208|a=3|b=40";
        final String variableStrWithDelim = "|" + variableStrWithoutDelim + "|";

        // one assert is necessary in a Test-method
        final String note = "@final (F) " + formulaTypeStr + formulaStr + variableStrWithDelim + "\"this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note);
        assertThat(waypoints).hasSize(1);

        // without delimiter between formula and equations, should work
        parseAndAssertFirstWaypoint("@final (f)" + formulaTypeStr + formulaStr + variableStrWithoutDelim + "|" + "\"this is the description\"\n\"this shall NOT be part of the note\"", "final", WaypointType.FINAL, "this is the description");
        parseAndAssertFirstWaypoint("@final (f)" + formulaTypeStr + formulaStr + variableStrWithoutDelim + "|" + "this is the description", "final", WaypointType.FINAL, "this is the description");

        // incomplete variables
        parseAndAssertFirstWaypoint("@parking (p)" + formulaTypeStr + formulaStr + "|A=2|B= this is the description", "parking", WaypointType.PARKING, "B= this is the description");

        // FORMULA-PLAIN, but no formula
        parseAndAssertFirstWaypoint("@" + formulaTypeStr, "Praefix 1", WaypointType.WAYPOINT, "");
        parseAndAssertFirstWaypoint("@puzzle (s)" + formulaTypeStr + "|A = 1|this is the description", "puzzle", WaypointType.STAGE, "|A = 1|this is the description");

        // unsupported operator used
        parseAndAssertFirstWaypoint("@stage 1 (s)" + formulaTypeStr + "N 45° A.B(C+D!) E 9°(A-B).(2*D)EF" + " \\n this is the description", "stage 1", WaypointType.STAGE, "\\n this is the description");

    }

    @Test
    public void testParseWaypointWithFormulaStabilityWrongKeyword() {

        //  with (NO-COORD) formula should not be parsed and goes there in the user note
        final String note1 = "@WPName X (NO-COORD) N 45° A.B(C+D)  E 9° (A-B).(2*D)EF |A = a+b|B=|a=2|b=| this is the description\n\"this shall NOT be part of the note\"";
        parseAndAssertFirstWaypoint(note1, "WPName", WaypointType.PUZZLE, "N 45° A.B(C+D)  E 9° (A-B).(2*D)EF |A = a+b|B=|a=2|b=| this is the description");

        final String note2 = "@WPName X (FORMULA_PLAIN) N 45° 48.123  E 9° 01.456 this is the description\n\"this shall NOT be part of the note\"";
        parseAndAssertFirstWaypoint(note2, "WPName X", WaypointType.WAYPOINT, "this is the description");

        // Then the normal parsing takes places -> no valid coords if not (NO-COORDS) -> no waypoint created
        final String note3 = "@WPName X (FORMULA_PLAIN) | N 45° A.B(C+D)  E 9° (A-B).(2*D)EF |A = a+b|B=|a=2|b=| this is the description\n\"this shall NOT be part of the note\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> waypoints = waypointParser.parseWaypoints(note3);
        assertThat(waypoints).hasSize(0);
    }

    private static String toParseableWpString(final Geopoint gp) {
        return gp.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_SHORT_RAW);

    }

    @Test
    public void testCreateNotReducedParseableWaypointText() {
        final Waypoint wp1 = new Waypoint("name", WaypointType.FINAL, true);
        final Geopoint gp = new Geopoint("N 45°49.739 E 9°45.038");
        wp1.setCoords(gp);
        wp1.setUserNote("This is a user note");
        final Waypoint wp2 = new Waypoint("name2", WaypointType.ORIGINAL, true);
        wp2.setCoords(gp);
        wp2.setUserNote("This is a user note 2");

        final Collection<Waypoint> wpColl = new ArrayList<>();
        wpColl.add(wp1);
        wpColl.add(wp2);
        final String gpStr = toParseableWpString(gp);

        assertThat(WaypointParser.getParseableText(wpColl, null, false)).isNotNull();

        final String fullExpected = "@name (F) " + gpStr + " \"This is a user note\"\n@name2 (H) " + gpStr + " \"This is a user note 2\"";
        //no limits
        assertThat(WaypointParser.getParseableText(wpColl, null, false)).isEqualTo(fullExpected);

        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> parsedWaypoints = waypointParser.parseWaypoints(WaypointParser.getParseableText(wpColl, null, false));
        assertThat(parsedWaypoints).hasSize(2);
        final Iterator<Waypoint> iterator = parsedWaypoints.iterator();
        assertWaypoint(iterator.next(), wp1);
        assertWaypoint(iterator.next(), wp2);
    }

    @Test
    public void testParseMultipleWaypointsAtOnce() {
        final Geopoint gp = new Geopoint("N 45°49.739 E 9°45.038");
        final String gpStr = gp.toString();
        final Geopoint gp2 = new Geopoint("N 45°49.745 E 9°45.038");
        final String gp2Str = gp2.toString();

        final String note = "@wp1 (x)" + gpStr + "\n@wp2 (f)" + gp2Str;
        final WaypointParser waypointParser = createParser("Prefix");
        Collection<Waypoint> wps = waypointParser.parseWaypoints(note);
        assertThat(wps.size()).isEqualTo(2);
        Iterator<Waypoint> it = wps.iterator();
        assertWaypoint(it.next(), "wp1", gp, WaypointType.PUZZLE, "");
        assertWaypoint(it.next(), "wp2", gp2, WaypointType.FINAL, "");

        final String note2 = "<----->\n" +
                "@Reference Point 1 (W) N 48° 01.194' · E 011° 43.814'\n" +
                "@Reference Point 1 (W) N 48° 01.194' · E 011° 43.814'\n" +
                "</----->\n";
        final Geopoint gp3 = new Geopoint("N 48° 01.194' · E 011° 43.814'");
        wps = waypointParser.parseWaypoints(note2);
        assertThat(wps.size()).isEqualTo(2);
        it = wps.iterator();
        assertWaypoint(it.next(), "Reference Point 1", gp3, WaypointType.WAYPOINT, "");
        assertWaypoint(it.next(), "Reference Point 1", gp3, WaypointType.WAYPOINT, "");


    }

    @Test
    public void putParseableWaypointsInText() {
        final Geopoint gp = new Geopoint("N 45°49.739 E 9°45.038");
        final String gpStr = toParseableWpString(gp);
        final Geopoint gp2 = new Geopoint("N 45°49.745 E 9°45.038");
        final String gp2Str = toParseableWpString(gp2);

        final String waypoints = "@wp1 (X) " + gpStr + " \"note\"\n@wp2 (F) " + gp2Str + " \"note2\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> wps = waypointParser.parseWaypoints(waypoints);

        final String note = "";
        final String noteAfter = WaypointParser.putParseableWaypointsInText(note, wps, null);
        assertThat(noteAfter).isEqualTo("{c:geo-start}\n" + waypoints + "\n{c:geo-end}");

        //check that continuous appliance of same waypoints will result in identical text
        final String noteAfter2 = WaypointParser.putParseableWaypointsInText(noteAfter, wps, null);
        assertThat(noteAfter2).isEqualTo(noteAfter);
    }

    @Test
    public void testGetAndReplaceExistingStoredWaypoints() {
        final Geopoint gp = new Geopoint("N 45°49.739 E 9°45.038");
        final String gpStr = toParseableWpString(gp);
        final Geopoint gp2 = new Geopoint("N 45°49.745 E 9°45.038");
        final String gp2Str = toParseableWpString(gp2);

        final String waypoints = "@wp1 (X) " + gpStr + " \"note\"\n@wp2 (F) " + gp2Str + " \"note2\"";
        final WaypointParser waypointParser = createParser("Prefix");
        final Collection<Waypoint> wps = waypointParser.parseWaypoints(waypoints);

        final String note = "before {c:geo-start}" + waypoints + "{c:geo-end} after";
        final String noteAfter = WaypointParser.putParseableWaypointsInText(note, wps, null);
        assertThat(noteAfter).isEqualTo("before  after\n\n{c:geo-start}\n" + waypoints + "\n{c:geo-end}");

        //check that continuous appliance of same waypoints will result in identical text
        final String noteAfter2 = WaypointParser.putParseableWaypointsInText(noteAfter, wps, null);
        assertThat(noteAfter2).isEqualTo(noteAfter);
    }

    @Test
    public void testWaypointParseStability() {
        final WaypointParser waypointParser = createParser("Praefix");
        //try to parse texts with empty input which should not lead to errors or waypoints
        assertThat(waypointParser.parseWaypoints("")).isEmpty();
        assertThat(waypointParser.parseWaypoints("@ ")).isEmpty();

        final String gpStr = new Geopoint("N 45°49.739 E 9°45.038").toString();

        //parse texts for waypoints which might lead to unexpected fillings (and NEVER to exceptions...)
        parseAndAssertFirstWaypoint("@" + gpStr, "Praefix 1", WaypointType.WAYPOINT, "");
        parseAndAssertFirstWaypoint("@ abc (f " + gpStr, "abc (f", WaypointType.WAYPOINT, "");
        //waypoint selection
        parseAndAssertFirstWaypoint("@ parking (f)" + gpStr, "parking", WaypointType.FINAL, "");
        parseAndAssertFirstWaypoint("@ parking " + gpStr, "parking", WaypointType.PARKING, "");
        //user notes
        parseAndAssertFirstWaypoint("@  " + gpStr + "note", "Praefix 1", WaypointType.WAYPOINT, "note");
        parseAndAssertFirstWaypoint(gpStr + "\n\"\\'note'\"", "Praefix 1", WaypointType.WAYPOINT, "'note'");
        parseAndAssertFirstWaypoint(gpStr + "\n\"note\"", "Praefix 1", WaypointType.WAYPOINT, "note");
        parseAndAssertFirstWaypoint(gpStr + "\nnote", "Praefix 1", WaypointType.WAYPOINT, "");
    }

    @Test
    public void testParseVariables() {
        assertParseVars("$TEST=3+4\n$B=4+5|c=6", "TEST", "3+4", "B", "4+5", "c", "6");
    }

    @Test
    public void testParseComplexVariables() {
        assertParseVars("A=?, B=?, C = ?, D=6, E = 5x, Fg = 8, h9", "D", "6", "E", "5", "Fg", "8");
        assertParseVars("test\nA=?, B=5\n$C=4*y|D=8|$R=3*5", "B", "5", "C", "4*y", "D", "8", "R", "3*5");
    }

    @Test
    public void testParseEmptyVariables() {
        assertParseVars("$TEST=\n$B= |c=", "TEST", "", "B", "");
        assertParseVars("$a= | $a=5 | $b=6 | $b= | $c=", "a", "5", "b", "6", "c", "");

    }

    private void assertParseVars(final String text, final String ... expectedVars) {
        final WaypointParser parser = createParser("Praefix");
        parser.parseWaypoints(text);
        final Map<String, String> expectedVarMap = new HashMap<>();
        for (int i = 0; i < expectedVars.length; i += 2) {
            expectedVarMap.put(expectedVars[i], expectedVars[i + 1]);
        }
        final Map<String, String> vars = parser.getParsedVariables();
        assertThat(vars).isEqualTo(expectedVarMap);
    }

    @Test
    public void testParseFormulaContainingPlainCoords() {
        final WaypointParser parser = createParser("Praefix");
        assertWaypoint(parser.parseWaypoints("37.000 +0.123").iterator().next(), "Praefix 1", new Geopoint(37d, 0.123d));
        assertWaypoint(parser.parseWaypoints("16.000 -0.321").iterator().next(), "Praefix 1", new Geopoint(16d, -0.321d));

        //Formulas may contain text which would be parseable as standalone coord.
        //In this case however, such standalone coord should NOT be parsed
        final Collection<Waypoint> wps =
                parser.parseWaypoints("@Finale (F) {CC|N47° (37.000 +0.123)|E012° (16.000 -0.321)}");
        assertThat(wps).hasSize(1);
        final Waypoint wp = wps.iterator().next();
        assertThat(wp.getCalcStateConfig()).isEqualTo("{CC|N47° (37.000 +0.123)|E012° (16.000 -0.321)}");

        final Collection<Waypoint> wps2 = parser.parseWaypoints("{CC|N48|E11} 13.5 8.3 {CC|N34|E09}");
        assertThat(wps2).hasSize(3);
        Waypoint nonCalc = null;
        int nonCalcCnt = 0;
        int calcCnt = 0;
        for (Waypoint w : wps2) {
            if (!w.isCalculated()) {
                nonCalc = w;
                nonCalcCnt++;
            } else {
                calcCnt++;
            }
        }
        assertThat(calcCnt).isEqualTo(2);
        assertThat(nonCalcCnt).isEqualTo(1);
        assertThat(nonCalc).isNotNull();
        assertThat(nonCalc.getCoords()).isEqualTo(new Geopoint(13.5, 8.3));
    }
}
