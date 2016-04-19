package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringEscapeUtils;

import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.text.Html;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
        return Html.fromHtml(input).toString();
    }

    public void testUnescapePerformance() {
        final int runs = 100;
        measure("unescape Apache", new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < runs; i++) {
                    unescapeApache();
                }
            }
        });
        measure("unescape Android", new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < runs; i++) {
                    unescapeAndroid();
                }
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
