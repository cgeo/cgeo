package cgeo.geocaching;


import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringEscapeUtils;

import android.os.SystemClock;
import android.test.AndroidTestCase;
import android.text.Html;

public class HtmlPerformanceTest extends AndroidTestCase {
    private String input;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        input = "Wei&#223;er Tiger";
    }

    public void testUnescape() {
        assert (unescapeAndroid().equals("Weißer Tiger"));
        assert (unescapeApache().equals("Weißer Tiger"));
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

    private static long measure(String label, Runnable runnable) {
        System.gc();
        final long start = SystemClock.elapsedRealtime();
        runnable.run();
        final long end = SystemClock.elapsedRealtime();
        Log.d(label + ": " + (end - start) + " ms");
        return end - start;
    }
}
