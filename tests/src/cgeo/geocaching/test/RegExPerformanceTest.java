package cgeo.geocaching.test;

import cgeo.geocaching.Constants;
import cgeo.geocaching.test.mock.GC1ZXX2;
import cgeo.geocaching.test.mock.GC2CJPF;
import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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

    private final static Pattern PATTERN_ACTUAL = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_IMPROVED = Pattern.compile(
            "Additional Hints" + Constants.TAG_STRONG_END +
                    "[^\\(]*\\(<a" + Constants.NEXT_END_TAG2 + ">Encrypt</a>\\)" + Constants.TAG_P_END +
                    Constants.NEXT_START_TAG + "<div id=\"div_hint\"" + Constants.NEXT_END_TAG + ">(.*?)" + Constants.TAG_DIV_END + Constants.NEXT_START_TAG + "<div id='dk'");


    private String parseHint(String data, Pattern p, int group) {
        String result = "";
        final Matcher matcherHint = p.matcher(data);
        if (matcherHint.find() && matcherHint.groupCount() >= group && matcherHint.group(group) != null) {
            // replace linebreak and paragraph tags
            String hint = Pattern.compile("<(br|p)" + Constants.NEXT_END_TAG + ">").matcher(matcherHint.group(group)).replaceAll("\n");
            if (hint != null) {
                result = hint.replaceAll(Pattern.quote(Constants.TAG_P_END), "").trim();
            }
        }
        return result;
    }

    private String parseDescription(String data, Pattern p, int group) {
        String result = null;
        final Matcher matcher = p.matcher(data);
        if (matcher.find() && matcher.groupCount() >= group) {
            result = BaseUtils.getMatch(matcher.group(group));
        }
        return result;
    }


    public void testRegEx() {

        List<MockedCache> cachesForParsing = new ArrayList<MockedCache>();
        cachesForParsing.add(new GC2CJPF());
        cachesForParsing.add(new GC1ZXX2());

        int ITERATIONS = 250; // 250 for an fast evaluation, 10000 else

        for (MockedCache cache : cachesForParsing) {
            String page = cache.getData();
            String resultOld = parseHint(page, PATTERN_ACTUAL, 1);
            String resultNew = parseHint(page, PATTERN_IMPROVED, 1);
            assertEquals(resultOld, resultNew);

            long diffOld, diffNew;

            System.out.println("Parsing " + cache.getGeocode() + " " + cache.getName());
            {
                System.out.println(("Result actual pattern:\t<<" + resultOld + ">>"));

                long start = System.currentTimeMillis();
                for (int j = 0; j < ITERATIONS; j++) {
                    parseHint(page, PATTERN_ACTUAL, 1);
                }
                diffOld = (System.currentTimeMillis() - start);
                System.out.println("Time actual pattern:\t" + diffOld + " ms");
            }

            {
                System.out.println(("Result new pattern:\t<<" + resultNew + ">>"));
                long start = System.currentTimeMillis();
                for (int j = 0; j < ITERATIONS; j++) {
                    parseHint(page, PATTERN_IMPROVED, 1);
                }
                diffNew = (System.currentTimeMillis() - start);
                System.out.println("Time new pattern:\t" + diffNew + " ms");
            }
            Float reduction = new Float((float) diffNew * 100 / (float) diffOld);
            System.out.println("Reduction to x percent:\t" + reduction.toString() + "\n");
        }

    }
}
