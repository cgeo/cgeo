package cgeo.geocaching;

import java.util.regex.Pattern;

public class Constants {

    /**
     * For further information about patters have a look at
     * http://download.oracle.com/javase/1.4.2/docs/api/java/util/regex/Pattern.html
     */

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

    public final static Pattern PATTERN_HINT = Pattern.compile("Additional Hints" + Constants.TAG_STRONG_END + "[^\\(]*\\(<a" + Constants.NEXT_END_TAG2 + ">Encrypt</a>\\)" + Constants.TAG_P_END +
            Constants.NEXT_START_TAG + "<div id=\"div_hint\"" + Constants.NEXT_END_TAG + ">(.*?)" + Constants.TAG_DIV_END + Constants.NEXT_START_TAG + "<div id='dk'");
    public final static Pattern PATTERN_DESC = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\">(.*?)</span>" + Constants.TAG_DIV_END_NEXT + Constants.TAG_P_START_NEXT + Constants.TAG_P_END_NEXT + Constants.TAG_P_START_NEXT + Constants.TAG_STRONG_START_NEXT + "\\W*Additional Hints" + Constants.TAG_STRONG_END);

}
