package cgeo.geocaching.utils.formulas;

import android.util.Pair;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests for coordinate scanning improvements from issue #17801.
 * These tests verify that multiple coordinates on the same line or across lines
 * are correctly identified as separate coordinate pairs.
 */
public class FormulaUtilsCoordinateScanTest {

    @Test
    public void scanForCoordinatesMultipleOnSameLine() {
        // Multiple coordinates on the same line should be scanned as separate pairs
        final String text = "N49 15.001 E7 03.A26 N 49° 15.002 E 007° 03.(A)27 N49° 15.003 E 007° 02.(A+A)93";
        
        final List<Pair<String, String>> result = FormulaUtils.scanForCoordinates(Collections.singleton(text), null);
        
        // Should find 3 separate coordinate pairs
        assertThat(result).hasSize(3);
        assertThat(result.get(0).first).contains("49");
        assertThat(result.get(0).first).contains("15.001");
        assertThat(result.get(1).first).contains("49");
        assertThat(result.get(1).first).contains("15.002");
        assertThat(result.get(2).first).contains("49");
        assertThat(result.get(2).first).contains("15.003");
    }

    @Test
    public void scanForCoordinatesMultipleOnDifferentLines() {
        // Multiple coordinates on different lines should be scanned as separate pairs
        final String text = "N49 15.001 E7 03.A26\nN 49° 15.002 E 007° 03.(A)27\nN49° 15.003 E 007° 02.(A+A)93";
        
        final List<Pair<String, String>> result = FormulaUtils.scanForCoordinates(Collections.singleton(text), null);
        
        // Should find 3 separate coordinate pairs
        assertThat(result).hasSize(3);
    }

    @Test
    public void scanForCoordinatesMultiLine() {
        // Single coordinate spanning multiple lines should be recognized as one pair
        final String text = "N 45°3.5\nE 27°7.5";
        
        final List<Pair<String, String>> result = FormulaUtils.scanForCoordinates(Collections.singleton(text), null);
        
        // Should find 1 coordinate pair
        assertThat(result).hasSize(1);
        assertThat(result.get(0).first).contains("45");
        assertThat(result.get(0).second).contains("27");
    }

    @Test
    public void scanForCoordinatesWithTextBetween() {
        // Coordinate with text between lat and lon should still be recognized
        final String text = "text before N48 12.ABC text inbetween E10 67.DEF txt after";
        
        final List<Pair<String, String>> result = FormulaUtils.scanForCoordinates(Collections.singleton(text), null);
        
        // Should find 1 coordinate pair
        assertThat(result).hasSize(1);
        assertThat(result.get(0).first).contains("48");
        assertThat(result.get(0).first).contains("12.ABC");
        assertThat(result.get(0).second).contains("10");
        assertThat(result.get(0).second).contains("67.DEF");
    }

    @Test
    public void scanForCoordinatesWithComplexFormulas() {
        // Coordinates with complex formulas should be recognized
        final String text = "N 49° 53.(H+1) (H-A) (B-2) E 008° 37. (B) (C) (H-1)";
        
        final List<Pair<String, String>> result = FormulaUtils.scanForCoordinates(Collections.singleton(text), null);
        
        // Should find 1 coordinate pair
        assertThat(result).hasSize(1);
        assertThat(result.get(0).first).contains("49");
        assertThat(result.get(0).first).contains("53");
        assertThat(result.get(0).second).contains("008");
        assertThat(result.get(0).second).contains("37");
    }

    @Test
    public void scanForCoordinatesSimple() {
        // Simple coordinate should work as before
        final String text = "N48 12.345 E10 67.890";
        
        final List<Pair<String, String>> result = FormulaUtils.scanForCoordinates(Collections.singleton(text), null);
        
        // Should find 1 coordinate pair
        assertThat(result).hasSize(1);
        assertThat(result.get(0).first).contains("48");
        assertThat(result.get(0).first).contains("12.345");
        assertThat(result.get(0).second).contains("10");
        assertThat(result.get(0).second).contains("67.890");
    }

    @Test
    public void scanForCoordinatesFromIssue17801() {
        // Full test with the complete listing from issue #17801
        final String listing = "N49 15.001 E7 03.A26\n" +
                "N 49° 15.002 E 007° 03.(A)27\n" +
                "N49° 15.003 E 007° 02.(A+A)93\n" +
                "N49 15.004' E7 03.(A + A)26'\n" +
                "N 49° 15.005' E 007° A*A3.027'\n" +
                "N49° 15.006' E 007° 02.AxA93'\n" +
                "N49°15.007 E 007°02.A x A93\n" +
                "N49°15.008' E7°03.(AxA)26'";
        
        final List<Pair<String, String>> result = FormulaUtils.scanForCoordinates(Collections.singleton(listing), null);
        
        // Should find 8 separate coordinate pairs (the ones without underscore)
        assertThat(result.size()).isGreaterThanOrEqualTo(8);
    }
}
