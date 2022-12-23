package cgeo.geocaching;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.os.SystemClock;
import android.test.AndroidTestCase;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.text.StringEscapeUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class HtmlPerformanceTest extends AndroidTestCase {
    private String input;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        input = "Wei&#223;er Tiger";
    }

    public void testUnescape() {
        assertThat(unescapeAndroid()).isEqualTo("Weißer Tiger");
        assertThat(unescapeApache()).isEqualTo("Weißer Tiger");
    }

    private String unescapeApache() {
        return StringEscapeUtils.unescapeHtml4(input);
    }

    private String unescapeAndroid() {
        return TextUtils.stripHtml(input);
    }

    public void testUnescapePerformance() {
        final int runs = 100;
        measure("unescape Apache", () -> {
            for (int i = 0; i < runs; i++) {
                unescapeApache();
            }
        });
        measure("unescape Android", () -> {
            for (int i = 0; i < runs; i++) {
                unescapeAndroid();
            }
        });
    }

    @SuppressFBWarnings("DM_GC")
    private static long measure(final String label, final Runnable runnable) {
        System.gc();
        final long start = SystemClock.elapsedRealtime();
        runnable.run();
        final long end = SystemClock.elapsedRealtime();
        Log.d(label + ": " + (end - start) + " ms");
        return end - start;
    }
}
