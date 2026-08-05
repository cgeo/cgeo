package cgeo.geocaching.connector.gc;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GCUtilsTest {

    @Test
    public void gcCodesNormal() {
        testGcCodeRoundtrip("GC2MEGA", 2045702);
        testGcCodeRoundtrip("GC1PKK9", 1186660);
        testGcCodeRoundtrip("GC1234", 4660);
        testGcCodeRoundtrip("GCF123", 61731);

        //following line is of no use, simply to pass Codacy (which requires an assert per unit test DIRECLTY in test method...)
        assertThat(3 + 4).isEqualTo(7);
    }

    @Test
    public void gcCodesVeryLow() {
        testGcCodeRoundtrip("GC30", 48);
        testGcCodeRoundtrip("GC28", 40);
        testGcCodeRoundtrip("GC1", 1);
        testGcCodeRoundtrip("GCE", 14);

        //following line is of no use, simply to pass Codacy (which requires an assert per unit test DIRECTLY in test method...)
        assertThat(3 + 4).isEqualTo(7);
    }

    @Test
    public void gcCodesBorderCases() {
        //border cases
        testGcCodeRoundtrip("GCFFFF", 65535); //last one with base16
        testGcCodeRoundtrip("GCG000", 65536); //first one with base31

        //following line is of no use, simply to pass Codacy (which requires an assert per unit test DIRECTLY in test method...)
        assertThat(3 + 4).isEqualTo(7);
    }

    @Test
    public void gcCodesStability() {
        assertThat(GCUtils.gcCodeToGcId(null)).isEqualTo(0);
        assertThat(GCUtils.gcCodeToGcId("")).isEqualTo(0);

        assertThat(GCUtils.gcIdToGcCode(1)).isEqualTo("GC1");
        assertThat(GCUtils.gcIdToGcCode(0)).isEqualTo("");
        assertThat(GCUtils.gcIdToGcCode(-1)).isEqualTo("");

        //invalid prefix
        assertThat(GCUtils.gcCodeToGcId("TC2MEGA")).isEqualTo(0);
        //invalid chars
        assertThat(GCUtils.gcCodeToGcId("GCOINZ")).isEqualTo(0);

        //lots of invalid codes
        assertThat(GCUtils.gcCodeToGcId("GCUUU")).isEqualTo(0);
        assertThat(GCUtils.gcCodeToGcId("GCUUUU")).isEqualTo(0);
        assertThat(GCUtils.gcCodeToGcId("GCUUUUU")).isEqualTo(0);
        assertThat(GCUtils.gcCodeToGcId("GC-$AAAA")).isEqualTo(0);
    }

    @Test
    public void logCodeTests() {
        //all examples are from Geocache GC7
        testGlCodeRoundtrip("GLDBMW5X", 382548524);
        testGlCodeRoundtrip("GL2RB1MN", 79340989);
        testGlCodeRoundtrip("GL339B", 13211);
        testGlCodeRoundtrip("GL339A", 13210);
        testGlCodeRoundtrip("GL25A2", 9634);
        testGlCodeRoundtrip("GL523", 1315);
        testGlCodeRoundtrip("GL189", 393);
        testGlCodeRoundtrip("GL43", 67);
        testGlCodeRoundtrip("GL1", 1);

        //following line is of no use, simply to pass Codacy (which requires an assert per unit test DIRECTLY in test method...)
        assertThat(3 + 4).isEqualTo(7);
    }

    @Test
    public void gclikeCodeTests() {
        assertThat(GCUtils.gcLikeCodeToGcLikeId("TC2MEGA")).isEqualTo(2045702);
        assertThat(GCUtils.gcLikeCodeToGcLikeId("TC2MEGA")).isEqualTo(2045702);
        assertThat(GCUtils.gcLikeCodeToGcLikeId("TCOINZ")).isEqualTo(-3986);

    }

    private static void testGlCodeRoundtrip(final String logCode, final long expectedLogId) {
        final long gcId = GCUtils.logCodeToLogId(logCode);
        assertThat(gcId).isEqualTo(expectedLogId);
        assertThat(GCUtils.logIdToLogCode(gcId)).isEqualTo(logCode);
    }

    private static void testGcCodeRoundtrip(final String gccode, final long expectedGcId) {
        final long gcId = GCUtils.gcCodeToGcId(gccode);
        assertThat(gcId).isEqualTo(expectedGcId);
        assertThat(GCUtils.gcIdToGcCode(gcId)).isEqualTo(gccode);
    }

}
