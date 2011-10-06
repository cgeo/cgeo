package cgeo.geocaching;

import java.util.regex.Pattern;

public final class Constants {

    /**
     * For further information about patterns have a look at
     * http://download.oracle.com/javase/1.4.2/docs/api/java/util/regex/Pattern.html
     */

    public final static Pattern PATTERN_HINT = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>");
    public final static Pattern PATTERN_DESC = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\">(.*?)</span>[^<]*</div>[^<]*<p>[^<]*</p>[^<]*<p>[^<]*<strong>\\W*Additional Hints</strong>");
    public final static Pattern PATTERN_SHORTDESC = Pattern.compile("<span id=\"ctl00_ContentBody_ShortDescription\">(.*?)</span>[^\\w^<]*</div>");
    public final static Pattern PATTERN_GEOCODE = Pattern.compile("<meta name=\"og:url\" content=\"[^\"]+/(GC[0-9A-Z]+)\"");
    public final static Pattern PATTERN_CACHEID = Pattern.compile("/seek/log\\.aspx\\?ID=(\\d+)");
    public final static Pattern PATTERN_GUID = Pattern.compile(Pattern.quote("&wid=") + "([0-9a-z\\-]+)" + Pattern.quote("&"));
    public final static Pattern PATTERN_SIZE = Pattern.compile("<div class=\"CacheSize[^\"]*\">[^<]*<p[^>]*>[^S]*Size[^:]*:[^<]*<span[^>]*>[^<]*<img src=\"[^\"]*/icons/container/[a-z_]+\\.gif\" alt=\"Size: ([^\"]+)\"[^>]*>[^<]*<small>[^<]*</small>[^<]*</span>[^<]*</p>");
    public final static Pattern PATTERN_LATLON = Pattern.compile("<span id=\"ctl00_ContentBody_LatLon\"[^>]*>(.*?)</span>");
    public final static Pattern PATTERN_LOCATION = Pattern.compile("<span id=\"ctl00_ContentBody_Location\">In (.*?)<");
    public final static Pattern PATTERN_PERSONALNOTE = Pattern.compile("<p id=\"cache_note\"[^>]*>(.*?)</p>");
    public final static Pattern PATTERN_NAME = Pattern.compile("<span id=\"ctl00_ContentBody_CacheName\">(.*?)</span>");
    public final static Pattern PATTERN_DIFFICULTY = Pattern.compile("<span id=\"ctl00_ContentBody_uxLegendScale\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"");
    public final static Pattern PATTERN_TERRAIN = Pattern.compile("<span id=\"ctl00_ContentBody_Localize12\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"");
    public final static Pattern PATTERN_OWNERREAL = Pattern.compile("<a id=\"ctl00_ContentBody_uxFindLinksHiddenByThisUser\" href=\"[^\"]*/seek/nearest\\.aspx\\?u=(.*?)\"");
    public final static Pattern PATTERN_FOUND = Pattern.compile("<a id=\"ctl00_ContentBody_hlFoundItLog\"[^<]*<img src=\".*/images/stockholm/16x16/check\\.gif\"[^>]*>[^<]*</a>[^<]*</p>");
    public final static Pattern PATTERN_FOUND_ALTERNATIVE = Pattern.compile("<div class=\"StatusInformationWidget FavoriteWidget\"");

}
