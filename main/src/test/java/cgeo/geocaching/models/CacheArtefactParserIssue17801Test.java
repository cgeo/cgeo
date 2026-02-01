package cgeo.geocaching.models;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests for issue #17801: Parsing variables from listing
 * This issue identified that variables with formulas (not just simple numeric values)
 * were not being parsed from listings when written without the $ prefix.
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
    public void testFullVariableListingFromIssue17801() {
        // Full test with all variables from issue #17801
        // Using newline-terminated format (without $ prefix)
        final String listing = "A=[:0-6]\n" +
                "B=[:0-2]\n" +
                "C=(1-trunc((A+4)/5)%2*B%2)*A*2\n" +
                "D=(1-trunc((A+4)/5)%2*B%2)*B*2\n" +
                "E=5\n" +
                "X=A\n" +
                "Y=abs(X-3)+1\n";
        
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
    }
    
    @Test
    public void testParseCoordinatesWithVariables() {
        // Waypoints with coordinates containing variables should still be parsed
        final String text = "N49 15.001 E7 03.A26\n" +
                "N 49° 15.002 E 007° 03.(A)27\n" +
                "N49° 15.003 E 007° 02.(A+A)93";
        
        final CacheArtefactParser parser = new CacheArtefactParser(null, "WP");
        final Collection<Waypoint> waypoints = parser.parse(text).getWaypoints();
        
        // Note: Coordinate parsing behavior unchanged by this fix
        // This test documents current behavior
        assertThat(waypoints).isNotEmpty();
    }
}
