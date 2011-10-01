package cgeo.geocaching.test;

import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;
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

    public final static Pattern PATTERN_HINT_OLD = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE);
    public final static Pattern PATTERN_HINT = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>");

    public final static Pattern PATTERN_SHORTDESC_OLD = Pattern.compile("<div class=\"UserSuppliedContent\">[^<]*<span id=\"ctl00_ContentBody_ShortDescription\"[^>]*>((?:(?!</span>[^\\w^<]*</div>).)*)</span>[^\\w^<]*</div>", Pattern.CASE_INSENSITIVE);
    public final static Pattern PATTERN_SHORTDESC = Pattern.compile("<span id=\"ctl00_ContentBody_ShortDescription\">(.*?)</span>[^\\w^<]*</div>");

    private final static Pattern PATTERN_GEOCODE_OLD = Pattern.compile("<meta name=\"og:url\" content=\"[^\"]+/(GC[0-9A-Z]+)\"[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern PATTERN_GEOCODE = Pattern.compile("<meta name=\"og:url\" content=\"[^\"]+/(GC[0-9A-Z]+)\"");

    private final static Pattern PATTERN_CACHEID_OLD = Pattern.compile("/seek/log\\.aspx\\?ID=(\\d+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern PATTERN_CACHEID = Pattern.compile("/seek/log\\.aspx\\?ID=(\\d+)");

    private final static Pattern PATTERN_GUID_OLD = Pattern.compile(Pattern.quote("&wid=") + "([0-9a-z\\-]+)" + Pattern.quote("&"), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern PATTERN_GUID = Pattern.compile(Pattern.quote("&wid=") + "([0-9a-z\\-]+)" + Pattern.quote("&"));

    private final static Pattern PATTERN_SIZE_OLD = Pattern.compile("<div class=\"CacheSize[^\"]*\">[^<]*<p[^>]*>[^S]*Size[^:]*:[^<]*<span[^>]*>[^<]*<img src=\"[^\"]*/icons/container/[a-z_]+\\.gif\" alt=\"Size: ([^\"]+)\"[^>]*>[^<]*<small>[^<]*</small>[^<]*</span>[^<]*</p>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern PATTERN_SIZE = Pattern.compile("<div class=\"CacheSize[^\"]*\">[^<]*<p[^>]*>[^S]*Size[^:]*:[^<]*<span[^>]*>[^<]*<img src=\"[^\"]*/icons/container/[a-z_]+\\.gif\" alt=\"Size: ([^\"]+)\"[^>]*>[^<]*<small>[^<]*</small>[^<]*</span>[^<]*</p>");

    private final static Pattern PATTERN_LATLON_OLD = Pattern.compile("<span id=\"ctl00_ContentBody_LatLon\"[^>]*>([^<]*)<\\/span>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_LATLON = Pattern.compile("<span id=\"ctl00_ContentBody_LatLon\"[^>]*>(.*?)</span>");

    private final static Pattern PATTERN_LOCATION_OLD = Pattern.compile("<span id=\"ctl00_ContentBody_Location\"[^>]*>In ([^<]*)", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_LOCATION = Pattern.compile("<span id=\"ctl00_ContentBody_Location\">In (.*?)</span>");

    private final static Pattern PATTERN_PERSONALNOTE_OLD = Pattern.compile("<p id=\"cache_note\"[^>]*>([^<]*)</p>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_PERSONALNOTE = Pattern.compile("<p id=\"cache_note\"[^>]*>(.*?)</p>");

    private final static Pattern PATTERN_NAME_OLD = Pattern.compile("<h2[^>]*>[^<]*<span id=\"ctl00_ContentBody_CacheName\">([^<]+)<\\/span>[^<]*<\\/h2>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern PATTERN_NAME = Pattern.compile("<span id=\"ctl00_ContentBody_CacheName\">(.*?)</span>");

    private final static Pattern PATTERN_DIFFICULTY_OLD = Pattern.compile("<span id=\"ctl00_ContentBody_uxLegendScale\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"[^\"]+\"[^>]*>[^<]*</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern PATTERN_DIFFICULTY = Pattern.compile("<span id=\"ctl00_ContentBody_uxLegendScale\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"");

    private final static Pattern PATTERN_TERRAIN_OLD = Pattern.compile("<span id=\"ctl00_ContentBody_Localize6\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"[^\"]+\"[^>]*>[^<]*</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern PATTERN_TERRAIN = Pattern.compile("<span id=\"ctl00_ContentBody_Localize6\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"");

    private final static Pattern PATTERN_OWNERREAL_OLD = Pattern.compile("<a id=\"ctl00_ContentBody_uxFindLinksHiddenByThisUser\" href=\"[^\"]*/seek/nearest\\.aspx\\?u=*([^\"]+)\">[^<]+</a>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_OWNERREAL = Pattern.compile("<a id=\"ctl00_ContentBody_uxFindLinksHiddenByThisUser\" href=\"[^\"]*/seek/nearest\\.aspx\\?u=(.*?)\"");


    public void testRegEx() {
        List<String> output = doTheTests(10);

        for (String s : output) {
            System.out.println(s);
        }
    }

    public static List<String> doTheTests(final int iterations) {

        List<String> output = new ArrayList<String>();

        output.addAll(measure(iterations, "hint", PATTERN_HINT_OLD, PATTERN_HINT));
        output.addAll(measure(iterations, "description", PATTERN_DESCRIPTION_OLD, PATTERN_DESCRIPTION));
        output.addAll(measure(iterations, "short description", PATTERN_SHORTDESC_OLD, PATTERN_SHORTDESC));
        output.addAll(measure(iterations, "geocode", PATTERN_GEOCODE_OLD, PATTERN_GEOCODE));
        output.addAll(measure(iterations, "cache id", PATTERN_CACHEID_OLD, PATTERN_CACHEID));
        output.addAll(measure(iterations, "cache guid", PATTERN_GUID_OLD, PATTERN_GUID));
        output.addAll(measure(iterations, "size", PATTERN_SIZE_OLD, PATTERN_SIZE));
        output.addAll(measure(iterations, "latlon", PATTERN_LATLON_OLD, PATTERN_LATLON));
        output.addAll(measure(iterations, "location", PATTERN_LOCATION_OLD, PATTERN_LOCATION));
        output.addAll(measure(iterations, "personal note", PATTERN_PERSONALNOTE_OLD, PATTERN_PERSONALNOTE));
        output.addAll(measure(iterations, "name", PATTERN_NAME_OLD, PATTERN_NAME));
        output.addAll(measure(iterations, "difficulty", PATTERN_DIFFICULTY_OLD, PATTERN_DIFFICULTY));
        output.addAll(measure(iterations, "terrain", PATTERN_TERRAIN_OLD, PATTERN_TERRAIN));
        output.addAll(measure(iterations, "owner real", PATTERN_OWNERREAL_OLD, PATTERN_OWNERREAL));

        return output;
    }

    private static List<String> measure(int iterations, String fieldName, Pattern p1, Pattern p2) {

        List<String> output = new ArrayList<String>();
        output.add(fieldName + ":");

        List<MockedCache> cachesForParsing = new ArrayList<MockedCache>();
        cachesForParsing.add(new GC2CJPF());
        cachesForParsing.add(new GC1ZXX2());

        for (MockedCache cache : cachesForParsing) {
            String page = cache.getData();
            String result1 = BaseUtils.getMatch(page, p1, 1, "");
            String result2 = BaseUtils.getMatch(page, p2, 1, "");
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
            Float reduction = new Float((float) diff2 * 100 / (float) diff1);
            output.add("New runtime:\t" + String.format("%.1f", reduction) + "%\n");
        }

        return output;

    }

    private static long parse(String page, Pattern pattern, int iterations) {
        long start = System.currentTimeMillis();
        for (int j = 0; j < iterations; j++) {
            BaseUtils.getMatch(page, pattern, 1, "");
        }
        return (System.currentTimeMillis() - start);

    }

}
