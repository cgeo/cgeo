package cgeo.geocaching.models;

import cgeo.geocaching.utils.formulas.FormulaUtils;

import android.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests for issue #17801: Parsing variables and coordinates from listing
 */
public class CacheArtefactParserIssue17801Test {

    @Test
    public void testParseVariablesWithRangeNotation() {
        // Variables with range notation like A=[:0-6] should be parsed when on separate lines
        final String text = "A=[:0-6]\nB=[:0-2]\n";
        final CacheArtefactParser parser = new CacheArtefactParser(null, "Test");
        parser.parse(text);
        final Map<String, String> vars = parser.getVariables();
        
        assertThat(vars).hasSize(2);
        assertThat(vars).containsEntry("A", "[:0-6]");
        assertThat(vars).containsEntry("B", "[:0-2]");
    }

    @Test
    public void testParseVariablesWithComplexFormulas() {
        // Variables with complex formulas should be parsed when on separate lines
        final String text = "C=(1-trunc((A+4)/5)%2*B%2)*A*2\nD=(1-trunc((A+4)/5)%2*B%2)*B*2\n";
        final CacheArtefactParser parser = new CacheArtefactParser(null, "Test");
        parser.parse(text);
        final Map<String, String> vars = parser.getVariables();
        
        assertThat(vars).hasSize(2);
        assertThat(vars).containsEntry("C", "(1-trunc((A+4)/5)%2*B%2)*A*2");
        assertThat(vars).containsEntry("D", "(1-trunc((A+4)/5)%2*B%2)*B*2");
    }

    @Test
    public void testParseVariablesWithFunctionCalls() {
        // Variables with function calls should be parsed when on separate lines
        final String text = "Y=abs(X-3)+1\nE=5\n";
        final CacheArtefactParser parser = new CacheArtefactParser(null, "Test");
        parser.parse(text);
        final Map<String, String> vars = parser.getVariables();
        
        assertThat(vars).hasSize(2);
        assertThat(vars).containsEntry("Y", "abs(X-3)+1");
        assertThat(vars).containsEntry("E", "5");
    }

    @Test
    public void testScanCoordinatesShouldFindMultipleCoordinates() {
        // Multiple coordinates should be scanned as separate waypoints, not one giant coordinate
        final String text = "N49 15.001 E7 03.A26\n" +
                "N 49° 15.002 E 007° 03.(A)27\n" +
                "N49° 15.003 E 007° 02.(A+A)93";
        
        final List<Pair<String, String>> coords = FormulaUtils.scanForCoordinates(Collections.singleton(text), null);
        
        // Each line should be recognized as a separate coordinate pair
        assertThat(coords).hasSize(3);
        assertThat(coords.get(0).first).contains("49");
        assertThat(coords.get(0).first).contains("15.001");
        assertThat(coords.get(1).first).contains("49");
        assertThat(coords.get(1).first).contains("15.002");
        assertThat(coords.get(2).first).contains("49");
        assertThat(coords.get(2).first).contains("15.003");
    }

    @Test
    public void testParseCoordinatesWithVariables() {
        // Waypoints with coordinates containing variables should be parsed correctly
        final String text = "N49 15.001 E7 03.A26\n" +
                "N 49° 15.002 E 007° 03.(A)27\n" +
                "N49° 15.003 E 007° 02.(A+A)93";
        
        final CacheArtefactParser parser = new CacheArtefactParser(null, "WP");
        final Collection<Waypoint> waypoints = parser.parse(text).getWaypoints();
        
        // Should parse as 3 separate waypoints
        assertThat(waypoints).hasSize(3);
    }

    @Test
    public void testFullListingFromIssue17801() {
        // Full test with the complete listing from issue #17801
        // Using newline-terminated format (without $ prefix)
        final String listing = "A=[:0-6]\n" +
                "B=[:0-2]\n" +
                "C=(1-trunc((A+4)/5)%2*B%2)*A*2\n" +
                "D=(1-trunc((A+4)/5)%2*B%2)*B*2\n" +
                "E=5\n" +
                "X=A\n" +
                "Y=abs(X-3)+1\n" +
                "\n" +
                "N49 15.001 E7 03.A26\n" +
                "N 49° 15.002 E 007° 03.(A)27\n" +
                "N49° 15.003 E 007° 02.(A+A)93\n" +
                "N49 15.004' E7 03.(A + A)26'\n" +
                "N 49° 15.005' E 007° A*A3.027'\n" +
                "N49° 15.006' E 007° 02.AxA93'\n" +
                "N49°15.007 E 007°02.A x A93\n" +
                "N49°15.008' E7°03.(AxA)26'";
        
        final CacheArtefactParser parser = new CacheArtefactParser(null, "WP");
        parser.parse(listing);
        
        final Map<String, String> vars = parser.getVariables();
        assertThat(vars).hasSize(7);
        assertThat(vars).containsEntry("A", "[:0-6]");
        assertThat(vars).containsEntry("B", "[:0-2]");
        assertThat(vars).containsEntry("C", "(1-trunc((A+4)/5)%2*B%2)*A*2");
        assertThat(vars).containsEntry("D", "(1-trunc((A+4)/5)%2*B%2)*B*2");
        assertThat(vars).containsEntry("E", "5");
        assertThat(vars).containsEntry("X", "A");
        assertThat(vars).containsEntry("Y", "abs(X-3)+1");
        
        final Collection<Waypoint> waypoints = parser.getWaypoints();
        // Should parse 8 waypoints (2 with underscore are not matched due to missing '_' in pattern)
        assertThat(waypoints.size()).isEqualTo(8);
    }
}
