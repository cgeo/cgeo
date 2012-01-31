package cgeo.geocaching;

import java.util.regex.Pattern;

/**
 * These patterns have been optimized for speed. Improve them only if you can prove
 * that *YOUR* pattern is faster. Use RegExRealPerformanceTest to show.
 *
 * For further information about patterns have a look at
 * http://download.oracle.com/javase/1.4.2/docs/api/java/util/regex/Pattern.html
 *
 * @author blafoo
 */
public final class GCConstants {

    /** Number of logs to retrieve from GC.com */
    public final static int NUMBER_OF_LOGS = 35;

    /**
     * Patterns for parsing the result of a (detailed) search
     */
    public final static Pattern PATTERN_HINT = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>");
    public final static Pattern PATTERN_DESC = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\">(.*?)</span>[^<]*</div>[^<]*<p>[^<]*</p>[^<]*<p>[^<]*<strong>\\W*Additional Hints</strong>");
    public final static Pattern PATTERN_SHORTDESC = Pattern.compile("<span id=\"ctl00_ContentBody_ShortDescription\">(.*?)</span>[^\\w^<]*</div>");
    public final static Pattern PATTERN_GEOCODE = Pattern.compile("<meta name=\"og:url\" content=\"[^\"]+/(GC[0-9A-Z]+)\"");
    public final static Pattern PATTERN_CACHEID = Pattern.compile("/seek/log\\.aspx\\?ID=(\\d+)");
    public final static Pattern PATTERN_GUID = Pattern.compile(Pattern.quote("&wid=") + "([0-9a-z\\-]+)" + Pattern.quote("&"));
    public final static Pattern PATTERN_SIZE = Pattern.compile("<div class=\"CacheSize[^\"]*\">[^<]*<p[^>]*>[^S]*Size[^:]*:[^<]*<span[^>]*>[^<]*<img src=\"[^\"]*/icons/container/[a-z_]+\\.gif\" alt=\"\\w+: ([^\"]+)\"[^>]*>[^<]*<small>[^<]*</small>[^<]*</span>[^<]*</p>");
    public final static Pattern PATTERN_LATLON = Pattern.compile("<span id=\"uxLatLon\" style=\"font-weight:bold;\"[^>]*>(.*?)</span>");
    public final static Pattern PATTERN_LATLON_ORIG = Pattern.compile("\\{\"isUserDefined\":true[^}]+?\"oldLatLngDisplay\":\"([^\"]+)\"\\}");
    public final static Pattern PATTERN_LOCATION = Pattern.compile(Pattern.quote("<span id=\"ctl00_ContentBody_Location\">In ") + "(?:<a href=[^>]*>)?(.*?)<");
    public final static Pattern PATTERN_PERSONALNOTE = Pattern.compile("<p id=\"cache_note\"[^>]*>(.*?)</p>");
    public final static Pattern PATTERN_NAME = Pattern.compile("<span id=\"ctl00_ContentBody_CacheName\">(.*?)</span>");
    public final static Pattern PATTERN_DIFFICULTY = Pattern.compile("<span id=\"ctl00_ContentBody_uxLegendScale\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\"");
    public final static Pattern PATTERN_TERRAIN = Pattern.compile("<span id=\"ctl00_ContentBody_Localize[\\d]+\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\"");
    public final static Pattern PATTERN_OWNERREAL = Pattern.compile("<a id=\"ctl00_ContentBody_uxFindLinksHiddenByThisUser\" href=\"[^\"]*/seek/nearest\\.aspx\\?u=(.*?)\"");
    public final static Pattern PATTERN_FOUND = Pattern.compile("<a id=\"ctl00_ContentBody_hlFoundItLog\"[^<]*<img src=\".*/images/stockholm/16x16/check\\.gif\"[^>]*>[^<]*</a>[^<]*</p>");
    public final static Pattern PATTERN_FOUND_ALTERNATIVE = Pattern.compile("<div class=\"StatusInformationWidget FavoriteWidget\"");
    public final static Pattern PATTERN_OWNER = Pattern.compile("<span class=\"minorCacheDetails\">[^<]+<a href=\"[^\"]+\">([^<]+)</a></span>");
    public final static Pattern PATTERN_TYPE = Pattern.compile("<img src=\"[^\"]*/WptTypes/\\d+\\.gif\" alt=\"([^\"]+?)\" title=\"[^\"]+\" width=\"32\" height=\"32\"");
    public final static Pattern PATTERN_HIDDEN = Pattern.compile("<span class=\"minorCacheDetails\">\\W*Hidden[\\s:]*([^<]+?)</span>");
    public final static Pattern PATTERN_HIDDENEVENT = Pattern.compile("Event\\s*Date\\s*:\\s*([^<]+)</span>", Pattern.DOTALL);
    public final static Pattern PATTERN_FAVORITE = Pattern.compile("<img src=\"/images/icons/icon_favDelete.png\" alt=\"Remove from your Favorites\" title=\"Remove from your Favorites\" />");
    public final static Pattern PATTERN_FAVORITECOUNT = Pattern.compile("<a id=\"uxFavContainerLink\"[^>]+>[^<]*<div[^<]*<span class=\"favorite-value\">\\D*([0-9]+?)</span>");
    public final static Pattern PATTERN_COUNTLOGS = Pattern.compile("<span id=\"ctl00_ContentBody_lblFindCounts\"><p(.+?)</p></span>");
    public final static Pattern PATTERN_LOGBOOK = Pattern.compile("initalLogs = (\\{.+\\});");
    /** Two groups ! */
    public final static Pattern PATTERN_COUNTLOG = Pattern.compile("<img src=\"/images/icons/([a-z_]+)\\.gif\"[^>]+> (\\d*[,.]?\\d+)");
    public static final Pattern PATTERN_PREMIUMMEMBERS = Pattern.compile("<p class=\"Warning NoBottomSpacing\">This is a Premium Member Only cache.</p>");
    public final static Pattern PATTERN_ATTRIBUTES = Pattern.compile("<h3 class=\"WidgetHeader\">[^<]*<img[^>]+>\\W*Attributes[^<]*</h3>[^<]*<div class=\"WidgetBody\">((?:[^<]*<img src=\"[^\"]+\" alt=\"[^\"]+\"[^>]*>)+?)[^<]*<p");
    /** Two groups ! */
    public final static Pattern PATTERN_ATTRIBUTESINSIDE = Pattern.compile("[^<]*<img src=\"([^\"]+)\" alt=\"([^\"]+?)\"");
    public final static Pattern PATTERN_SPOILERS = Pattern.compile("<p class=\"NoPrint\">\\s+((?:<a href=\"http://img\\.geocaching\\.com/cache/[^.]+\\.jpg\"[^>]+><img class=\"StatusIcon\"[^>]+><span>[^<]+</span></a><br />(?:[^<]+<br /><br />)?)+)\\s+</p>");
    public final static Pattern PATTERN_SPOILERSINSIDE = Pattern.compile("<a href=\"(http://img\\.geocaching\\.com/cache/[^.]+\\.jpg)\"[^>]+><img class=\"StatusIcon\"[^>]+><span>([^<]+)</span></a><br />(?:([^<]+)<br /><br />)?");
    public final static Pattern PATTERN_INVENTORY = Pattern.compile("<span id=\"ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">\\W*Inventory[^<]*</span>[^<]*</h3>[^<]*<div class=\"WidgetBody\">([^<]*<ul>(([^<]*<li>[^<]*<a href=\"[^\"]+\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>[^<]+<\\/span>[^<]*<\\/a>[^<]*<\\/li>)+)[^<]*<\\/ul>)?");
    public final static Pattern PATTERN_INVENTORYINSIDE = Pattern.compile("[^<]*<li>[^<]*<a href=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/track\\/details\\.aspx\\?guid=([0-9a-z\\-]+)[^\"]*\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>([^<]+)<\\/span>[^<]*<\\/a>[^<]*<\\/li>");
    public final static Pattern PATTERN_WATCHLIST = Pattern.compile("icon_stop_watchlist.gif");

    // Info box top-right
    public static final Pattern PATTERN_LOGIN_NAME = Pattern.compile("\"SignedInProfileLink\">([^<]+)</a>");
    public static final Pattern PATTERN_MEMBER_STATUS = Pattern.compile("<span id=\"ctl00_litPMLevel\" style=\"display: block;\">([^<]+)</span>");
    public static final Pattern PATTERN_CACHES_FOUND = Pattern.compile("<img src=\"/images/icons/icon_smile.png\" title=\"Caches Found\" /> (\\d+)&");
    public static final Pattern PATTERN_AVATAR_IMAGE_PROFILE_PAGE = Pattern.compile("<img src=\"(http://img.geocaching.com/user/avatar/[0-9a-f-]+\\.jpg)\"[^>]*\\salt=\"Avatar\"");
    public static final Pattern PATTERN_LOGIN_NAME_LOGIN_PAGE = Pattern.compile("<span class=\"Success\">You are logged in as[^<]*<strong[^>]*>([^<]+)</strong>[^<]*</span>");
    public static final Pattern PATTERN_USERLOGGEDIN = Pattern.compile("<strong>Hello, <a href=\"/my/default.aspx\" title=\"View Profile for[^\"]*\" class=\"SignedInProfileLink\">(.*?)</a></strong>");
    public static final Pattern PATTERN_CUSTOMDATE = Pattern.compile("<option selected=\"selected\" value=\"([ /Mdy-]+)\">");

    /**
     * Patterns for parsing trackables
     */
    public final static Pattern PATTERN_TRACKABLE_GUID = Pattern.compile("<a id=\"ctl00_ContentBody_lnkPrint\" title=\"[^\"]*\" href=\".*sheet\\.aspx\\?guid=([a-z0-9\\-]+)\"[^>]*>[^<]*</a>");
    public final static Pattern PATTERN_TRACKABLE_GEOCODE = Pattern.compile("<span id=\"ctl00_ContentBody_BugDetails_BugTBNum\" String=\"[^\"]*\">Use[^<]*<strong>(TB[0-9A-Z]+)[^<]*</strong> to reference this item.[^<]*</span>");
    public final static Pattern PATTERN_TRACKABLE_NAME = Pattern.compile("<h2[^>]*>(?:[^<]*<img[^>]*>)?[^<]*<span id=\"ctl00_ContentBody_lbHeading\">([^<]+)</span>[^<]*</h2>");
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
    public final static Pattern PATTERN_TRACKABLE_LOG = Pattern.compile("<tr class=\"Data.+?src=\"/images/icons/([^.]+)\\.gif[^>]+>&nbsp;([^<]+)</td>.+?guid.+?>([^<]+)</a>.+?(?:guid=([^\"]+)\">(<span class=\"Strike\">)?([^<]+)</.+?)?<td colspan=\"4\">(.+?)(?:<ul.+?ul>)?\\s*</td>\\s*</tr>");

    /**
     * Patterns for parsing the result of a search (next)
     */
    public final static Pattern PATTERN_SEARCH_TYPE = Pattern.compile("<td class=\"Merge\">[^<]*<a href=\"[^\"]*/seek/cache_details\\.aspx\\?guid=[^\"]+\"[^>]+>[^<]*<img src=\"[^\"]*/images/wpttypes/[^.]+\\.gif\" alt=\"([^\"]+)\" title=\"[^\"]+\"[^>]*>[^<]*</a>");
    public final static Pattern PATTERN_SEARCH_GUIDANDDISABLED = Pattern.compile("<img src=\"[^\"]*/images/wpttypes/[^>]*>[^<]*</a></td><td class=\"Merge\">[^<]*<a href=\"[^\"]*/seek/cache_details\\.aspx\\?guid=([a-z0-9\\-]+)\" class=\"lnk([^\"]*)\">([^<]*<span>)?([^<]*)(</span>[^<]*)?</a>[^<]+<br />([^<]*)<span[^>]+>([^<]*)</span>([^<]*<img[^>]+>)?[^<]*<br />[^<]*</td>");
    /** Two groups **/
    public final static Pattern PATTERN_SEARCH_TRACKABLES = Pattern.compile("<a id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxTravelBugList\" class=\"tblist\" data-tbcount=\"([0-9]+)\" data-id=\"[^\"]*\"[^>]*>(.*)</a>");
    /** Second group used */
    public final static Pattern PATTERN_SEARCH_TRACKABLESINSIDE = Pattern.compile("(<img src=\"[^\"]+\" alt=\"([^\"]+)\" title=\"[^\"]*\" />[^<]*)");
    public final static Pattern PATTERN_SEARCH_DIRECTION = Pattern.compile("<img id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxDistanceAndHeading\" title=\"[^\"]*\" src=\"[^\"]*/seek/CacheDir\\.ashx\\?k=([^\"]+)\"[^>]*>");
    public final static Pattern PATTERN_SEARCH_GEOCODE = Pattern.compile("\\|\\W*(GC[0-9A-Z]+)[^\\|]*\\|");
    public final static Pattern PATTERN_SEARCH_ID = Pattern.compile("name=\"CID\"[^v]*value=\"([0-9]+)\"");
    public final static Pattern PATTERN_SEARCH_FAVORITE = Pattern.compile("<span id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxFavoritesValue\" title=\"[^\"]*\" class=\"favorite-rank\">([0-9]+)</span>");
    public final static Pattern PATTERN_SEARCH_TOTALCOUNT = Pattern.compile("<td class=\"PageBuilderWidget\"><span>Total Records[^<]*<b>(\\d+)<\\/b>");
    public final static Pattern PATTERN_SEARCH_RECAPTCHA = Pattern.compile("<script[^>]*src=\"[^\"]*/recaptcha/api/challenge\\?k=([^\"]+)\"[^>]*>");
    public final static Pattern PATTERN_SEARCH_RECAPTCHACHALLENGE = Pattern.compile("challenge : '([^']+)'");

    /**
     * Patterns for waypoints
     */
    public final static Pattern PATTERN_WPTYPE = Pattern.compile("\\/wpttypes\\/sm\\/(.+)\\.jpg");
    public final static Pattern PATTERN_WPPREFIXORLOOKUPORLATLON = Pattern.compile(">([^<]*<[^>]+>)?([^<]+)(<[^>]+>[^<]*)?<\\/td>");
    public final static Pattern PATTERN_WPNAME = Pattern.compile(">[^<]*<a[^>]+>([^<]*)<\\/a>");
    public final static Pattern PATTERN_WPNOTE = Pattern.compile("colspan=\"6\">(.*)<\\/td>");

    /**
     * Patterns for different purposes
     */
    /** replace linebreak and paragraph tags */
    public final static Pattern PATTERN_LINEBREAK = Pattern.compile("<(br|p)[^>]*>");
    public final static Pattern PATTERN_TYPEBOX = Pattern.compile("<select name=\"ctl00\\$ContentBody\\$LogBookPanel1\\$ddLogType\" id=\"ctl00_ContentBody_LogBookPanel1_ddLogType\"[^>]*>"
            + "(([^<]*<option[^>]*>[^<]+</option>)+)[^<]*</select>", Pattern.CASE_INSENSITIVE);
    public final static Pattern PATTERN_TYPE2 = Pattern.compile("<option( selected=\"selected\")? value=\"(\\d+)\">[^<]+</option>", Pattern.CASE_INSENSITIVE);
    // FIXME: pattern is over specified
    public final static Pattern PATTERN_TRACKABLE = Pattern.compile("<tr id=\"ctl00_ContentBody_LogBookPanel1_uxTrackables_repTravelBugs_ctl[0-9]+_row\"[^>]*>"
            + "[^<]*<td>[^<]*<a href=\"[^\"]+\">([A-Z0-9]+)</a>[^<]*</td>[^<]*<td>([^<]+)</td>[^<]*<td>"
            + "[^<]*<select name=\"ctl00\\$ContentBody\\$LogBookPanel1\\$uxTrackables\\$repTravelBugs\\$ctl([0-9]+)\\$ddlAction\"[^>]*>"
            + "([^<]*<option value=\"([0-9]+)(_[a-z]+)?\">[^<]+</option>)+"
            + "[^<]*</select>[^<]*</td>[^<]*</tr>", Pattern.CASE_INSENSITIVE);
    public final static Pattern PATTERN_MAINTENANCE = Pattern.compile("<span id=\"ctl00_ContentBody_LogBookPanel1_lbConfirm\"[^>]*>([^<]*<font[^>]*>)?([^<]+)(</font>[^<]*)?</span>", Pattern.CASE_INSENSITIVE);
    public final static Pattern PATTERN_OK1 = Pattern.compile("<h2[^>]*>[^<]*<span id=\"ctl00_ContentBody_lbHeading\"[^>]*>[^<]*</span>[^<]*</h2>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public final static Pattern PATTERN_OK2 = Pattern.compile("<div id=[\"|']ctl00_ContentBody_LogBookPanel1_ViewLogPanel[\"|']>", Pattern.CASE_INSENSITIVE);
    public final static Pattern PATTERN_USERTOKEN = Pattern.compile("var userToken[^=]*=[^']*'([^']+)';", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public final static Pattern PATTERN_VIEWSTATEFIELDCOUNT = Pattern.compile("id=\"__VIEWSTATEFIELDCOUNT\"[^(value)]+value=\"(\\d+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public final static Pattern PATTERN_VIEWSTATES = Pattern.compile("id=\"__VIEWSTATE(\\d*)\"[^(value)]+value=\"([^\"]+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    public final static Pattern PATTERN_USERTOKEN2 = Pattern.compile("userToken\\s*=\\s*'([^']+)'");
}
