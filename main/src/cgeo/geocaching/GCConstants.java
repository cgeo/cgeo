package cgeo.geocaching;

import java.util.regex.Pattern;

/**
 * These patterns have been optimized for speed. Improve them only if you can prove
 * that *YOUR* pattern is faster. Use RegExRealPerformanceTest to show.
 *
 * @author blafoo
 */
public final class GCConstants {

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
    public final static Pattern PATTERN_TERRAIN = Pattern.compile("<span id=\"ctl00_ContentBody_Localize[\\d]+\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"");
    public final static Pattern PATTERN_OWNERREAL = Pattern.compile("<a id=\"ctl00_ContentBody_uxFindLinksHiddenByThisUser\" href=\"[^\"]*/seek/nearest\\.aspx\\?u=(.*?)\"");
    public final static Pattern PATTERN_FOUND = Pattern.compile("<a id=\"ctl00_ContentBody_hlFoundItLog\"[^<]*<img src=\".*/images/stockholm/16x16/check\\.gif\"[^>]*>[^<]*</a>[^<]*</p>");
    public final static Pattern PATTERN_FOUND_ALTERNATIVE = Pattern.compile("<div class=\"StatusInformationWidget FavoriteWidget\"");
    public final static Pattern PATTERN_OWNER = Pattern.compile("<span class=\"minorCacheDetails\">[^<]+<a href=\"[^\"]+\">([^<]+)</a></span>");
    public final static Pattern PATTERN_TYPE = Pattern.compile("<img src=\"[^\"]*/WptTypes/\\d+\\.gif\" alt=\"([^\"]+?)\" title=\"[^\"]+\" width=\"32\" height=\"32\"");
    public final static Pattern PATTERN_HIDDEN = Pattern.compile("<span class=\"minorCacheDetails\">\\W*Hidden[\\s:]*([^<]+?)</span>");
    public final static Pattern PATTERN_HIDDENEVENT = Pattern.compile("<span[^>]*>\\W*Event\\W*Date[^:]*:([^<]*?)</span>");
    public final static Pattern PATTERN_FAVORITE = Pattern.compile("<img src=\"/images/icons/icon_favDelete.png\" alt=\"Remove from your Favorites\" title=\"Remove from your Favorites\" />");
    public final static Pattern PATTERN_FAVORITECOUNT = Pattern.compile("<a id=\"uxFavContainerLink\"[^>]+>[^<]*<div[^<]*<span class=\"favorite-value\">\\D*([0-9]+?)</span>");
    public final static Pattern PATTERN_COUNTLOGS = Pattern.compile("<span id=\"ctl00_ContentBody_lblFindCounts\"><p(.+?)</p></span>");
    /** Two groups ! */
    public final static Pattern PATTERN_COUNTLOG = Pattern.compile("<img src=\"/images/icons/([a-z_]+)\\.gif\"[^>]+> (\\d*[,.]?\\d+)");
    public static final Pattern PATTERN_MEMBERS = Pattern.compile("<p class=\"Warning NoBottomSpacing\">This is a Premium Member Only cache.</p>");
    public final static Pattern PATTERN_ATTRIBUTES = Pattern.compile("<h3 class=\"WidgetHeader\">[^<]*<img[^>]+>\\W*Attributes[^<]*</h3>[^<]*<div class=\"WidgetBody\">((?:[^<]*<img src=\"[^\"]+\" alt=\"[^\"]+\"[^>]*>)+?)[^<]*<p");
    /** Two groups ! */
    public final static Pattern PATTERN_ATTRIBUTESINSIDE = Pattern.compile("[^<]*<img src=\"([^\"]+)\" alt=\"([^\"]+?)\"");
    public final static Pattern PATTERN_SPOILERS = Pattern.compile("<p class=\"NoPrint\">\\s+((?:<a href=\"http://img\\.geocaching\\.com/cache/[^.]+\\.jpg\"[^>]+><img class=\"StatusIcon\"[^>]+><span>[^<]+</span></a><br />(?:[^<]+<br /><br />)?)+)\\s+</p>");
    public final static Pattern PATTERN_SPOILERSINSIDE = Pattern.compile("<a href=\"(http://img\\.geocaching\\.com/cache/[^.]+\\.jpg)\"[^>]+><img class=\"StatusIcon\"[^>]+><span>([^<]+)</span></a><br />(?:([^<]+)<br /><br />)?");
    public final static Pattern PATTERN_INVENTORY = Pattern.compile("<span id=\"ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">\\W*Inventory[^<]*</span>[^<]*</h3>[^<]*<div class=\"WidgetBody\">([^<]*<ul>(([^<]*<li>[^<]*<a href=\"[^\"]+\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>[^<]+<\\/span>[^<]*<\\/a>[^<]*<\\/li>)+)[^<]*<\\/ul>)?");
    public final static Pattern PATTERN_INVENTORYINSIDE = Pattern.compile("[^<]*<li>[^<]*<a href=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/track\\/details\\.aspx\\?guid=([0-9a-z\\-]+)[^\"]*\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>([^<]+)<\\/span>[^<]*<\\/a>[^<]*<\\/li>");
    public final static Pattern PATTERN_WATCHLIST = Pattern.compile("<img src=\"\\/images/stockholm/16x16/icon_stop_watchlist.gif\"");

    public static final Pattern PATTERN_USERLOGGEDIN = Pattern.compile("<strong>Hello, <a href=\"/my/default.aspx\" title=\"View Profile for[^\"]*\" class=\"SignedInProfileLink\">(.*?)</a></strong>");
    public static final Pattern PATTERN_AVATAR_IMAGE = Pattern.compile("<img src=\"(http://img.geocaching.com/user/avatar/[0-9a-f-]+\\.jpg)\"[^>]*\\salt=\"Avatar\"");

    public final static Pattern PATTERN_TRACKABLE_ID = Pattern.compile("<a id=\"ctl00_ContentBody_LogLink\" title=\"[^\"]*\" href=\".*log\\.aspx\\?wid=([a-z0-9\\-]+)\"[^>]*>[^<]*</a>");
    public final static Pattern PATTERN_TRACKABLE_GEOCODE = Pattern.compile("<span id=\"ctl00_ContentBody_BugDetails_BugTBNum\" String=\"[^\"]*\">Use[^<]*<strong>(TB[0-9A-Z]+)[^<]*</strong> to reference this item.[^<]*</span>");
    public final static Pattern PATTERN_TRACKABLE_NAME = Pattern.compile("<h2>(?:[^<]*<img[^>]*>)?[^<]*<span id=\"ctl00_ContentBody_lbHeading\">([^<]+)</span>[^<]*</h2>");
    /** Two groups ! */
    public final static Pattern PATTERN_TRACKABLE_OWNER = Pattern.compile("<dt>\\W*Owner:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugOwner\" title=\"[^\"]*\" href=\"[^\"]*/profile/\\?guid=([a-z0-9\\-]+)\">([^<]+)<\\/a>[^<]*</dd>");
    public final static Pattern PATTERN_TRACKABLE_RELEASES = Pattern.compile("<dt>\\W*Released:[^<]*</dt>[^<]*<dd>[^<]*<span id=\"ctl00_ContentBody_BugDetails_BugReleaseDate\">([^<]+)<\\/span>[^<]*</dd>");
    public final static Pattern PATTERN_TRACKABLE_ORIGIN = Pattern.compile("<dt>\\W*Origin:[^<]*</dt>[^<]*<dd>[^<]*<span id=\"ctl00_ContentBody_BugDetails_BugOrigin\">([^<]+)<\\/span>[^<]*</dd>");
    /** Two groups ! */
    public final static Pattern PATTERN_TRACKABLE_SPOTTEDCACHE = Pattern.compile("<dt>\\W*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" title=\"[^\"]*\" href=\"[^\"]*/seek/cache_details.aspx\\?guid=([a-z0-9\\-]+)\">In ([^<]+)</a>[^<]*</dd>");
    /** Two groups ! */
    public final static Pattern PATTERN_TRACKABLE_SPOTTEDUSER = Pattern.compile("<dt>\\W*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" href=\"[^\"]*/profile/\\?guid=([a-z0-9\\-]+)\">In the hands of ([^<]+).</a>[^<]*</dd>");
    public final static Pattern PATTERN_TRACKABLE_SPOTTEDUNKNOWN = Pattern.compile("<dt>\\W*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\">Unknown Location[^<]*</a>[^<]*</dd>");
    public final static Pattern PATTERN_TRACKABLE_SPOTTEDOWNER = Pattern.compile("<dt>\\W*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\">In the hands of the owner[^<]*</a>[^<]*</dd>");
    public final static Pattern PATTERN_TRACKABLE_GOAL = Pattern.compile("<div id=\"TrackableGoal\">[^<]*<p>(.*?)</p>[^<]*</div>", Pattern.DOTALL);
    /** Four groups */
    public final static Pattern PATTERN_TRACKABLE_DETAILSIMAGE = Pattern.compile("<h3>\\W*About This Item[^<]*</h3>[^<]*<div id=\"TrackableDetails\">([^<]*<p>([^<]*<img id=\"ctl00_ContentBody_BugDetails_BugImage\" class=\"[^\"]+\" src=\"([^\"]+)\"[^>]*>)?[^<]*</p>)?[^<]*<p[^>]*>(.*)</p>[^<]*</div> <div id=\"ctl00_ContentBody_BugDetails_uxAbuseReport\">");
    public final static Pattern PATTERN_TRACKABLE_ICON = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" src=\"([^\"]+)\"[^>]*>");
    public final static Pattern PATTERN_TRACKABLE_TYPE = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" src=\"[^\"]+\" alt=\"([^\"]+)\"[^>]*>");
    public final static Pattern PATTERN_TRACKABLE_DISTANCE = Pattern.compile("<h4[^>]*\\W*Tracking History \\(([0-9.,]+(km|mi))[^\\)]*\\)");
    public final static Pattern PATTERN_TRACKABLE_LOG = Pattern.compile("<tr class=\"Data.+?src=\"/images/icons/([^.]+)\\.gif[^>]+>&nbsp;([^<]+)</td>.+?guid.+?>([^<]+)</a>.+?(?:guid=([^\"]+)\">(<span class=\"Strike\">)?([^<]+)</.+?)?<td colspan=\"4\">(.+?)(?:<ul.+?ul>)?\\s*</td>\\s*</tr>", Pattern.CASE_INSENSITIVE);
}
