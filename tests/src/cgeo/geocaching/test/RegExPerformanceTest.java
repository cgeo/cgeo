package cgeo.geocaching.test;

import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;
import cgeo.geocaching.test.mock.GC2JVEH;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

/**
 * Test class to compare the performance of two regular expressions on given data.
 * Can be used to improve the time needed to parse the cache data
 * Run As "JUnit Test"
 *
 * @author blafoo
 */
public class RegExPerformanceTest extends TestCase {

    // Regular expression: "<img.*src=(\S*)/>"
    // Input string 1: "<img border=1 src=image.jpg />"
    // Input string 2: "<img src=src=src=src= .... many src= ... src=src="
    // "a(.*)a", it's much better to use "a([^a]*)a".
    // The rewritten expression "<img((?!src=).)*src=(\S*)/>" will handle a large, non-matching string almost a hundred times faster then the previous one!

    /** Search until the start of the next tag. The tag can follow immediately */
    public static final String NEXT_START_TAG = "[^<]*";
    /** Search until the end of the actual tag. The closing tag can follow immediately */
    public static final String NEXT_END_TAG = "[^>]*";

    /** Search until the start of the next tag. The tag must not follow immediately */
    public static final String NEXT_START_TAG2 = "[^<]+";
    /** Search until the end of the actual tag. The closing tag must not follow immediately */
    public static final String NEXT_END_TAG2 = "[^>]+";

    /** P tag */
    public static final String TAG_P_START = "<p>";
    /** Closing P tag **/
    public static final String TAG_P_END = "</p>";
    /** Search until the next &lt;p&gt; */
    public static final String TAG_P_START_NEXT = NEXT_START_TAG + TAG_P_START;
    /** Search until the next &lt;/p&gt; */
    public static final String TAG_P_END_NEXT = NEXT_START_TAG + TAG_P_END;

    /** strong tag */
    public static final String TAG_STRONG_START = "<strong>";
    /** Closing strong tag */
    public static final String TAG_STRONG_END = "</strong>";
    /** Search until the next &lt;strong&gt; */
    public static final String TAG_STRONG_START_NEXT = NEXT_START_TAG + TAG_STRONG_START;
    /** Search until the next &lt;/strong&gt; */
    public static final String TAG_STRONG_END_NEXT = NEXT_START_TAG + TAG_STRONG_END;

    /** div tag */
    public static final String TAG_DIV_START = "<div>";
    /** closing div tag */
    public static final String TAG_DIV_END = "</div>";
    /** Search until the next &lt;div&gt; */
    public static final String TAG_DIV_START_NEXT = NEXT_START_TAG + TAG_DIV_START;
    /** Search until the next &lt;/div&gt; */
    public static final String TAG_DIV_END_NEXT = NEXT_START_TAG + TAG_DIV_END;

    public final static Pattern PATTERN_DESCRIPTION_OLD = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\"[^>]*>" + "(.*)</span>[^<]*</div>[^<]*<p>[^<]*</p>[^<]*<p>[^<]*<strong>\\W*Additional Hints</strong>", Pattern.CASE_INSENSITIVE);
    public final static Pattern PATTERN_DESCRIPTION = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\">(.*?)</span>[^<]*</div>[^<]*<p>[^<]*</p>[^<]*<p>[^<]*<strong>\\W*Additional Hints</strong>");


    public final static MockedCache[] MOCKED_CACHES = { new GC2CJPF(), new GC1ZXX2(), new GC2JVEH() };

    public static void testRegEx() {
        List<String> output = doTheTests(10);

        for (String s : output) {
            System.out.println(s);
        }
    }

    public static List<String> doTheTests(final int iterations) {

        List<String> output = new ArrayList<String>();

        output.addAll(measure(iterations, "description", PATTERN_DESCRIPTION_OLD, PATTERN_DESCRIPTION));

        return output;
    }

    private static List<String> measure(int iterations, String fieldName, Pattern p1, Pattern p2) {

        List<String> output = new ArrayList<String>();
        output.add(fieldName + ":");

        for (MockedCache cache : MOCKED_CACHES) {
            String page = cache.getData();
            String result1 = BaseUtils.getMatch(page, p1, true, "");
            String result2 = BaseUtils.getMatch(page, p2, true, "");
            assertEquals(result1, result2);

            long diff1, diff2;

            output.add("Parsing " + cache.getGeocode() + " " + cache.getName());
            {
                diff1 = parse(page, p1, iterations);
                output.add("Time pattern 1:\t" + diff1 + " ms");
            }

            {
                diff2 = parse(page, p2, iterations);
                output.add("Time pattern 2:\t" + diff2 + " ms");
            }
            float reduction = (float) diff2 * 100 / diff1;
            output.add("New runtime:\t" + String.format("%.1f", reduction) + "%\n");
        }

        return output;

    }

    private static long parse(String page, Pattern pattern, int iterations) {
        long start = System.currentTimeMillis();
        for (int j = 0; j < iterations; j++) {
            BaseUtils.getMatch(page, pattern, true, "");
        }
        return (System.currentTimeMillis() - start);

    }

}
