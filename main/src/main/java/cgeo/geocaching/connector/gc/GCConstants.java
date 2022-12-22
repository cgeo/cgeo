package cgeo.geocaching.connector.gc;

import cgeo.geocaching.log.LogType;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * These patterns have been optimized for speed. Improve them only if you can prove
 * that *YOUR* pattern is faster.
 *
 * For further information about patterns have a look at
 * http://download.oracle.com/javase/1.4.2/docs/api/java/util/regex/Pattern.html
 */
public final class GCConstants {

    static final String GC_URL = "https://www.geocaching.com/";
    private static final String GC_TILE_URL = "https://tiles.geocaching.com/";
    /**
     * Live Map
     */
    @NonNull static final String URL_LIVE_MAP = GC_URL + "map/default.aspx";
    /**
     * Live Map pop-up
     */
    @NonNull static final String URL_LIVE_MAP_DETAILS = GC_TILE_URL + "map.details";
    /**
     * Caches in a tile
     */
    @NonNull static final String URL_MAP_INFO = GC_TILE_URL + "map.info";
    /**
     * Tile itself
     */
    @NonNull static final String URL_MAP_TILE = GC_TILE_URL + "map.png";
    /**
     * Format used by geocaching.com when user is not logged in.
     */
    public static final String DEFAULT_GC_DATE = "MM/dd/yyyy";
    public static final String GEOCODE_PATTERN = "(?<!\\w)(GC[0-9A-Z&&[^ILOSU]]{1,5})(?=$|\\W|\\s|_)";

    // Patterns for parsing the result of a (detailed) search

    static final Pattern PATTERN_HINT = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>", Pattern.DOTALL);
    static final Pattern PATTERN_DESC = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\">(.*?)</span>\\s*</div>\\s*<(p|div) id=\"ctl00_ContentBody", Pattern.DOTALL);
    static final Pattern PATTERN_SHORTDESC = Pattern.compile("<span id=\"ctl00_ContentBody_ShortDescription\">(.*?)</span>\\s*</div>", Pattern.DOTALL);
    static final Pattern PATTERN_GEOCODE = Pattern.compile("class=\"CoordInfoCode\">" + GEOCODE_PATTERN + "</span>");
    static final Pattern PATTERN_GUID = Pattern.compile(Pattern.quote("&wid=") + "([0-9a-z\\-]+)" + Pattern.quote("&"));
    static final Pattern PATTERN_SIZE = Pattern.compile("/icons/container/([a-z_]+)\\.");
    static final Pattern PATTERN_LATLON = Pattern.compile("<span id=\"uxLatLon\"[^>]*>(.*?)</span>");
    static final Pattern PATTERN_LATLON_ORIG = Pattern.compile("\\{\"isUserDefined\":true[^}]+?\"oldLatLngDisplay\":\"([^\"]+)\"\\}");
    static final Pattern PATTERN_LOCATION = Pattern.compile(Pattern.quote("<span id=\"ctl00_ContentBody_Location\">In ") + "(?:<a href=[^>]*>)?(.*?)<");
    static final Pattern PATTERN_PERSONALNOTE = Pattern.compile("<div id=\"srOnlyCacheNote\"[^>]*>(.*?)</div>", Pattern.DOTALL);
    static final Pattern PATTERN_NAME = Pattern.compile("<span id=\"ctl00_ContentBody_CacheName\".*?>(.*?)</span>");
    static final Pattern PATTERN_DIFFICULTY = Pattern.compile("<span id=\"ctl00_ContentBody_uxLegendScale\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\"");
    static final Pattern PATTERN_TERRAIN = Pattern.compile("<span id=\"ctl00_ContentBody_Localize[\\d]+\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\"");
    static final Pattern PATTERN_OWNER_USERID = Pattern.compile("<a href=\"/play/search\\?owner\\[0\\]=(.*?)&a=");
    static final Pattern PATTERN_OWNER_GUID = Pattern.compile("\\/p(rofile)?\\/\\?guid=([0-9a-z\\-]+)&");
    static final Pattern PATTERN_FOUND = Pattern.compile("logtypes/48/(" + StringUtils.join(LogType.foundLogTypes(), "|") + ").png\" id=\"ctl00_ContentBody_GeoNav_logTypeImage\"");
    static final Pattern PATTERN_DNF = Pattern.compile("logtypes/48/(" + LogType.DIDNT_FIND_IT.id + ").png\" id=\"ctl00_ContentBody_GeoNav_logTypeImage\"");
    static final Pattern PATTERN_OWNER_DISPLAYNAME = Pattern.compile("<div id=\"ctl00_ContentBody_mcd1\">[^<]+<a href=\"[^\"]+\">([^<]+)</a>");
    static final Pattern PATTERN_TYPE = Pattern.compile("<use xlink:href=\"/app/ui-icons/sprites/cache-types.svg#icon-([0-9a-f]+)");
    static final Pattern PATTERN_HIDDEN = Pattern.compile("ctl00_ContentBody_mcd2[^:]+:\\s*([^<]+?)<");
    static final Pattern PATTERN_HIDDENEVENT = Pattern.compile(":\\s*([0-9-/]+)\\s*<div id=\"calLinks\">", Pattern.DOTALL);
    static final Pattern PATTERN_IS_FAVORITE = Pattern.compile("<div id=\"pnlFavoriteCache\">"); // without 'class="hideMe"' inside the tag !
    static final Pattern PATTERN_FAVORITECOUNT = Pattern.compile("<span class=\"favorite-value\">\\D*([0-9]+?)\\D*</span>");
    static final Pattern PATTERN_COUNTLOGS = Pattern.compile("<span id=\"ctl00_ContentBody_lblFindCounts\"><ul(.+?)</ul></span>");
    static final Pattern PATTERN_WATCHLIST_COUNT = Pattern.compile("data-watchcount=\"(\\d+)\"");

    /**
     * Two groups !
     */
    static final Pattern PATTERN_COUNTLOG = Pattern.compile("logtypes/([0-9]+)\\.[^>]+>\\s*([0-9,.]+)");
    static final Pattern PATTERN_PREMIUMMEMBERS = Pattern.compile("<p class=\"Warning NoBottomSpacing\"");
    static final Pattern PATTERN_ATTRIBUTES = Pattern.compile("(<img src=\"/images/attributes.*?)</p");
    /**
     * Two groups !
     */
    static final Pattern PATTERN_ATTRIBUTESINSIDE = Pattern.compile("<img src=\"([^\"]+)\" alt=\"([^\"]+?)\"");
    private static final String IMAGE_FORMATS = "jpg|jpeg|png|gif|bmp";
    static final Pattern PATTERN_SPOILER_IMAGE = Pattern.compile("<a href=\"(https?://img(?:cdn)?\\.geocaching\\.com[^.]+\\.(?:" + IMAGE_FORMATS + "))\"[^>]+>" + "([^<]*)</a>" + ".*?(?:description\"[^>]*>([^<]+)</span>)?</li>", Pattern.DOTALL);
    static final Pattern PATTERN_INVENTORY = Pattern.compile("ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">.*?WidgetBody(.*?)<div");
    static final Pattern PATTERN_INVENTORYINSIDE = Pattern.compile("[^<]*<li>[^<]*<a href=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/track\\/details\\.aspx\\?guid=([0-9a-z\\-]+)[^\"]*\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>([^<]+)<\\/span>[^<]*<\\/a>[^<]*<\\/li>");
    static final Pattern PATTERN_WATCHLIST = Pattern.compile("data-cacheonwatchlist=\"True\"");
    static final Pattern PATTERN_RELATED_WEB_PAGE = Pattern.compile("ctl00_ContentBody_uxCacheUrl.*? href=\"(.*?)\">");
    static final Pattern PATTERN_GC_HOSTED_IMAGE = Pattern.compile("^https?://img(?:cdn)?\\.geocaching\\.com/");
    static final Pattern PATTERN_BACKGROUND_IMAGE = Pattern.compile("<body background=\"(.+?)\"");
    static final String PATTERN_GC_CHECKER = "ctl00_ContentBody_lblSolutionChecker";

    // Info box top-right
    public static final Pattern PATTERN_LOGIN_NAME = Pattern.compile("\\swindow(?>\\.|\\[')(?:headerSettings|chromeSettings)(?>'\\])?\\s*=\\s*\\{[\\S\\s]*\"username\":\\s*\"([^\"]*)\",?[\\S\\s]*\\}");
    /**
     * Use replaceAll("[,.]","") on the resulting string before converting to an int
     */
    static final Pattern PATTERN_CACHES_FOUND = Pattern.compile("\\swindow(?>\\.|\\[')(?:headerSettings|chromeSettings)(?>'\\])?\\s*=\\s*\\{[\\S\\s]*\"findCount\":\\s*([0-9]*)[\\S\\s]*\\}");

    // Patterns for parsing trackables

    static final Pattern PATTERN_TRACKABLE_GUID = Pattern.compile("<a id=\"ctl00_ContentBody_lnkPrint\" aria-labelledby=\"[^\"]*\" href=\".*sheet\\.aspx\\?guid=([a-z0-9\\-]+)\"[^>]*>[^<]*</a>");
    static final Pattern PATTERN_TRACKABLE_GEOCODE = Pattern.compile(Pattern.quote("CoordInfoCode\">") + "(TB[0-9A-Z&&[^ILOSU]]+)<");

    // multiple error codes, depending on the search term for the trackable code. Interestingly, these error messages are not localized by now.
    static final String ERROR_TB_DOES_NOT_EXIST = "does not exist in the system";
    static final String ERROR_TB_ELEMENT_EXCEPTION = "ElementNotFound Exception";
    static final String ERROR_TB_ARITHMETIC_OVERFLOW = "operation resulted in an overflow";
    static final String ERROR_TB_NOT_ACTIVATED = "hasn't been activated";
    /**
     * some parts of the webpage don't correctly encode the name, therefore this pattern must be checked with a
     * trackable name that needs HTML encoding
     */
    static final Pattern PATTERN_TRACKABLE_NAME = Pattern.compile("<span id=\"ctl00_ContentBody_lbHeading\">(.*?)</span>");
    /**
     * Three groups !
     */
    static final Pattern PATTERN_TRACKABLE_OWNER = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugOwner\"[^>]+href=\"https?://www.geocaching.com/p(rofile)?/\\?guid=(.*?)\">(.*?)</a>");
    static final Pattern PATTERN_TRACKABLE_RELEASES = Pattern.compile("<span id=\"ctl00_ContentBody_BugDetails_BugReleaseDate\">([^<]+)<\\/span>[^<]*</dd>");
    static final Pattern PATTERN_TRACKABLE_FOUND_LOG = Pattern.compile("<img src=\"/images/logtypes/(.*?).png\" id=\"ctl00_ContentBody_InteractionLogTypeImage\"[^>]*>");
    static final Pattern PATTERN_TRACKABLE_DISPOSITION_LOG = Pattern.compile("<a id=\"ctl00_ContentBody_InteractionLogLink\"[^>]*href=\"/track/log.aspx\\?LUID=([a-z0-9\\-]+)\">[^0-9]+(.*?)</a>");
    static final Pattern PATTERN_TRACKABLE_ORIGIN = Pattern.compile("<span id=\"ctl00_ContentBody_BugDetails_BugOrigin\">([^<]+)<\\/span>[^<]*</dd>");
    /**
     * Two groups !
     */
    static final Pattern PATTERN_TRACKABLE_SPOTTEDCACHE = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" title=\"[^\"]*\" data-name=\"([^\"]+)\" data-status=\"Cache\" href=\"[^\"]*/seek/cache_details.aspx\\?guid=([a-z0-9\\-]+)\">[^<]+</a>");
    /**
     * Three groups !
     */
    static final Pattern PATTERN_TRACKABLE_SPOTTEDUSER = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" data-name=\"([^\"]+)\" data-status=\"User\" href=\"[^\"]*/p(rofile)?/\\?guid=([a-z0-9\\-]+)\">");
    static final Pattern PATTERN_TRACKABLE_SPOTTEDUNKNOWN = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" data-status=\"Unknown\">[^<]*</a>");
    static final Pattern PATTERN_TRACKABLE_SPOTTEDOWNER = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" data-status=\"Owner\">[^<]*</a>");
    static final Pattern PATTERN_TRACKABLE_GOAL = Pattern.compile("<div id=\"TrackableGoal\">[^<]*<p>(.*?)</p>[^<]*</div>", Pattern.DOTALL);
    /**
     * Four groups
     */
    static final Pattern PATTERN_TRACKABLE_DETAILSIMAGE = Pattern.compile("<div id=\"TrackableDetails\">([^<]*<p>([^<]*<img id=\"ctl00_ContentBody_BugDetails_BugImage\" class=\"[^\"]+\" src=\"([^\"]+)\"[^>]*>)?[^<]*</p>)?[^<]*<p[^>]*>(.*)</p>[^<]*</div> <div class=\"Clear\">");
    static final Pattern PATTERN_TRACKABLE_ICON = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" src=\"([^\"]+)\"[^>]*>");
    static final Pattern PATTERN_TRACKABLE_TYPE = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" src=\"[^\"]+\" alt=\"([^\"]+)\"[^>]*>");
    static final Pattern PATTERN_TRACKABLE_TYPE_TITLE = Pattern.compile("<title>\\s\\(TB[0-9A-Z]*\\) ([^<]*)<\\/title>");
    static final Pattern PATTERN_TRACKABLE_DISTANCE = Pattern.compile("\\(([0-9.,]+)(km|mi)[^\\)]*\\)\\s*<a href=\"map_gm");
    static final Pattern PATTERN_TRACKABLE_LOG = Pattern.compile("<tr class=\"Data BorderTop .+?/images/logtypes/([^.]+)\\.png[^>]+>&nbsp;([^<]+)</th>.+?guid=([^\"]+).+?>([^<]+)</a>.+?(?:guid=([^\"]+)\">(<span[^>]+>)?([^<]+)</.+?)?LUID=([^\"]+)\">.+?<td colspan=\"4\">\\s*<div.*?>(.*?)</div>\\s*(?:<ul.+?ul>)?\\s*</td>\\s*</tr>");
    static final Pattern PATTERN_TRACKABLE_LOG_IMAGES = Pattern.compile("<ul class=\"log_images\"><li><a href=\"([^\"]+)\".+?class=\"tb_images\".+?alt=\"([^<]*)\"" + Pattern.quote(" />"));
    static final Pattern PATTERN_TRACKABLE_IS_LOCKED = Pattern.compile("<a id=\"ctl00_ContentBody_LogLink\"[^(]*\\(locked\\)</a></td>");

    // Patterns for parsing the result of a search (next)

    static final Pattern PATTERN_SEARCH_TYPE = Pattern.compile("<img src=\"[^\"]*/images/WptTypes/(.*?)\\.", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_SEARCH_GUIDANDDISABLED = Pattern.compile("SearchResultsWptType.*?<a href=\"[^\"]*\" class=\"lnk ([^\"]*)\"><span>([^<]*)</span>[^|]*[|][^|]*[|]([^<]*)<");
    /**
     * Two groups
     **/
    static final Pattern PATTERN_SEARCH_TRACKABLES = Pattern.compile("<a id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxTravelBugList\" class=\"tblist\" data-tbcount=\"([0-9]+)\" data-id=\"[^\"]*\"[^>]*>(.*)</a>");
    /**
     * Second group used
     */
    static final Pattern PATTERN_SEARCH_TRACKABLESINSIDE = Pattern.compile("<img src=\"[^\"]+\" alt=\"([^\"]+)\" title=\"[^\"]*\" />[^<]*");
    static final Pattern PATTERN_SEARCH_DIRECTION_DISTANCE = Pattern.compile("<img src=\"/images/icons/compass/([^\\.]+)\\.gif\"[^>]*>[^<]*<br />([0-9.,]+)\\s*?(m|km|ft|yd|mi|)?</span>");
    static final Pattern PATTERN_SEARCH_DIFFICULTY_TERRAIN = Pattern.compile("<span class=\"small\">([0-5]([\\.,]5)?)/([0-5]([\\.,]5)?)</span><br />");
    static final Pattern PATTERN_SEARCH_CONTAINER = Pattern.compile("<img src=\"/images/icons/container/([^\\.]+)\\.gif\"");
    static final Pattern PATTERN_SEARCH_GEOCODE = Pattern.compile(GEOCODE_PATTERN);
    static final Pattern PATTERN_SEARCH_FAVORITE = Pattern.compile("favorite-rank\">([0-9,.]+)</span>");
    static final Pattern PATTERN_SEARCH_TOTALCOUNT = Pattern.compile("PageBuilderWidget\"><span>[^<]*?<b>(\\d+)<");
    static final Pattern PATTERN_SEARCH_HIDDEN_DATE = Pattern.compile("<td style=\"width:70px\" data-dateplaced=\"[^\"]+\">[^<]+<span class=\"small\">([^<]+)</span>");
    static final Pattern PATTERN_SEARCH_POST_ACTION = Pattern.compile("<form method=\"post\" action=\"(.*)\" id=\"aspnetForm\"");

    // Patterns for waypoints

    static final Pattern PATTERN_WPTYPE = Pattern.compile("\\/WptTypes\\/sm\\/(.+)\\.jpg", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_WPPREFIXORLOOKUPORLATLON = Pattern.compile(">([^<]*<[^>]+>)?([^<]+)(<[^>]+>[^<]*)?<\\/td>");
    static final Pattern PATTERN_WPNAME = Pattern.compile(">[^<]*<a[^>]+>([^<]*)<\\/a>");
    static final Pattern PATTERN_WPNOTE = Pattern.compile("colspan=\"6\">(.*)" + Pattern.quote("</td>"), Pattern.DOTALL);

    // Patterns for different purposes

    /**
     * replace line break and paragraph tags
     */
    static final Pattern PATTERN_LINEBREAK = Pattern.compile("<(br|p)[^>]*>");
    static final Pattern PATTERN_TYPEBOX = Pattern.compile("<select name=\"ctl00\\$ContentBody\\$LogBookPanel1\\$ddLogType\" id=\"ctl00_ContentBody_LogBookPanel1_ddLogType\"[^>]*>"
            + "(([^<]*<option[^>]*>[^<]+</option>)+)[^<]*</select>", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_TYPE2 = Pattern.compile("<option( selected=\"selected\")? value=\"(\\d+)\">[^<]+</option>", Pattern.CASE_INSENSITIVE);
    // new logpage logtype pattern:         logSettings.logTypes.push({"Value":46,"Description":"Owner maintenance","IsRealtimeOnly":false});
    static final Pattern PATTERN_TYPE3 = Pattern.compile("logSettings.logTypes.push\\(([^;]*)\\);");
    static final Pattern PATTERN_MAINTENANCE = Pattern.compile("<span id=\"ctl00_ContentBody_LogBookPanel1_lbConfirm\"[^>]*>([^<]*<font[^>]*>)?([^<]+)(</font>[^<]*)?</span>", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_OK2 = Pattern.compile("<div id=[\"|']ctl00_ContentBody_LogBookPanel1_ViewLogPanel[\"|'] class=", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_VIEWSTATEFIELDCOUNT = Pattern.compile("id=\"__VIEWSTATEFIELDCOUNT\"[^(value)]+value=\"(\\d+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    static final Pattern PATTERN_VIEWSTATES = Pattern.compile("id=\"__VIEWSTATE(\\d*)\"[^(value)]+value=\"([^\"]+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    static final Pattern PATTERN_USERTOKEN = Pattern.compile("userToken\\s*=\\s*'([^']+)'");

    /**
     * downloadable PQs
     */
    static final Pattern PATTERN_PQ_LAST_GEN = Pattern.compile("([^(]*)(\\(([\\d]+)?)?");

    /**
     * Live Map since 14.02.2012
     */
    static final Pattern PATTERN_USERSESSION = Pattern.compile("UserSession\\('([^']+)'");
    static final Pattern PATTERN_SESSIONTOKEN = Pattern.compile("sessionToken:'([^']+)'");

    static final String STRING_PREMIUMONLY = "class=\"illustration lock-icon\"";
    static final Pattern PATTERN_PREMIUMONLY_CACHETYPE = Pattern.compile("/app/ui-icons/sprites/cache-types\\.svg#icon-([^\"\\-]+)-?([^\"]+)?");
    static final Pattern PATTERN_PREMIUMONLY_CACHENAME = Pattern.compile("<h1 class=\"heading-3\">(.+)</h1>");
    static final Pattern PATTERN_PREMIUMONLY_GEOCODE = Pattern.compile("<li class=\"li__gccode\">([^<]+)");
    static final Pattern PATTERN_PREMIUMONLY_DIFFICULTY = Pattern.compile("<span id=\"ctl00_ContentBody_lblDifficulty\"(?:.|\\s)*?<span>([0-5](?:[\\.,]5)?)</span>");
    static final Pattern PATTERN_PREMIUMONLY_TERRAIN = Pattern.compile("<span id=\"ctl00_ContentBody_lblTerrain\"(?:.|\\s)*?<span>([0-5](?:[\\.,]5)?)</span>");
    static final Pattern PATTERN_PREMIUMONLY_SIZE = Pattern.compile("<span id=\"ctl00_ContentBody_lblSize\"(?:.|\\s)*?<span>([^<]+)</span>");

    static final String STRING_UNPUBLISHED_OTHER = "you cannot view this cache listing until it has been published"; //TODO: unpublished caches return a 404 page for other users meanwhile, so they cannot be detected anymore. We could however indicate this status for owners...
    static final String STRING_UNPUBLISHED_FROM_SEARCH = "class=\"UnpublishedCacheSearchWidget"; // do not include closing brace as the CSS can contain additional styles
    static final String STRING_UNKNOWN_ERROR = "An Error Has Occurred"; //TODO: might be language dependent - but when does it occur at all?
    static final String STRING_STATUS_DISABLED = "<div id=\"ctl00_ContentBody_uxDisabledMessageBody\"";
    static final String STRING_STATUS_ARCHIVED = "<div id=\"ctl00_ContentBody_archivedMessage\"";
    static final String STRING_STATUS_LOCKED = "<div id=\"ctl00_ContentBody_lockedMessage\"";
    static final String STRING_CACHEDETAILS = "id=\"cacheDetails\"";
    static final String STRING_UNAPPROVED_LICENSE = "<span id=\"ctl00_ContentBody_lblAgreementText\"";

    // Pages with such title seem to be returned with a 200 code instead of 404
    static final String STRING_404_FILE_NOT_FOUND = "<title>404 - File Not Found</title>";

    /**
     * Number of logs to retrieve from GC.com
     */
    static final int NUMBER_OF_LOGS = 35;

    private GCConstants() {
        // this class shall not have instances
    }

}
