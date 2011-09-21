package cgeo.geocaching;

import java.util.regex.Pattern;

public class Constants {

    /**
     * For further information about patterns have a look at
     * http://download.oracle.com/javase/1.4.2/docs/api/java/util/regex/Pattern.html
     */

    public final static Pattern PATTERN_HINT = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>");
    public final static Pattern PATTERN_DESC = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\">(.*?)</span>[^<]*</div>[^<]*<p>[^<]*</p>[^<]*<p>[^<]*<strong>\\W*Additional Hints</strong>");

}
