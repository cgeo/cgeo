package cgeo.geocaching.connector.gc;

import cgeo.geocaching.log.LogType;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * These patterns have been optimized for speed. Improve them only if you can prove
 * that *YOUR* pattern is faster.
 * <br>
 * For further information about patterns have a look at
 * <a href="http://download.oracle.com/javase/1.4.2/docs/api/java/util/regex/Pattern.html">...</a>
 */
public final class GCConstants {

    static final String GC_URL = "https://www.geocaching.com/";
    /**
     * Live Map
     */
    @NonNull static final String URL_LIVE_MAP = GC_URL + "map/default.aspx";
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
    // homeLocation: {"Latitude":50.12345,"Longitude":10.98765}
    static final Pattern PATTERN_LOCATION_LOGIN = Pattern.compile("homeLocation:\\s*\\{\\\"Latitude\\\":(-?\\d*\\.\\d*),\\\"Longitude\\\":(-?\\d*\\.\\d*)\\}");
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
    public static final Pattern PATTERN_EVENTTIMES = Pattern.compile("<div id=\"mcd3\">\\s*[^<]*?\\s+([APM ]*)([0-9]+)[\\.:]([0-9]+)([APM ]*)\\s*</div>\\s*<div id=\"mcd4\">\\s*[^<]*?\\s+([APM ]*)([0-9]+)[\\.:]([0-9]+)([APM ]*)\\s*</div>");
    static final Pattern PATTERN_IS_FAVORITE = Pattern.compile("<div id=\"pnlFavoriteCache\">"); // without 'class="hideMe"' inside the tag !
    static final Pattern PATTERN_FAVORITECOUNT = Pattern.compile("<span class=\"favorite-value\">\\D*([0-9]+?)\\D*</span>");
    static final Pattern PATTERN_COUNTLOGS = Pattern.compile("<span id=\"ctl00_ContentBody_lblFindCounts\"><ul(.+?)</ul></span>");
    static final Pattern PATTERN_WATCHLIST_COUNT = Pattern.compile("data-watchcount=\"(\\d+)\"");

    //example gallery count:
    //<li><a href="/seek/gallery.aspx?guid=3d6f0c14-7d72-43a2-a704-dc4a2b879c73">View gallery (1885)</a></li>
    static final Pattern PATTERN_GALLERY_COUNT = Pattern.compile("<a href=\"/seek/gallery.aspx\\?guid=[^(]+?\\((\\d+)\\)</a>");
    // matches: the inner JSON Code (w/o {}) of "currentGeocache":{"id":123,"referenceCode":"GCxyz","name":"somename"}
    static final Pattern PATTERN_TB_CURRENT_GEOCACHE_JSON = Pattern.compile("\"currentGeocache\":\\{([^}]+)\\}");

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
    private static final String IMAGE_FORMATS = "jpg|jpeg|png|gif|bmp|JPG|JPEG|PNG|GIF|BMP";
    private static final String IMAGE_URL = "<a href=[\"'](https?://img(?:cdn)?\\.geocaching\\.com[^.]+\\.(?:" + IMAGE_FORMATS + "))[\"'][^>]+>(?:[^>]*</strong>[^>]+>)?";
    //Example HTML to match: <a href="https://img.geocaching.com/cache/large/1711f8a1-796a-405b-82ba-8685f2e9f024.jpg" class="owner-image" rel="owner_image_group" data-title="<strong>indy mit text netz Kopie</strong>">indy mit text netz Kopie</a></li>
    static final Pattern PATTERN_SPOILER_IMAGE = Pattern.compile(IMAGE_URL + "([^<]*)</a>" + ".*?(?:description\"[^>]*>([^<]+)</span>)?</li>", Pattern.DOTALL);

    //gallery image example:
    //<td>
    //        <span class="date-stamp">15.01.2025</span>
    //        <a href='https://img.geocaching.com/cache/log/large/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg' data-title='&lt;span class=&quot;LogImgTitle&quot;&gt;Image 2&nbsp;&lt;/span&gt;&lt;span class=&quot;LogImgLink&quot;&gt;&lt;a href=&quot;https://www.geocaching.com/seek/log.aspx?LUID=7f9ba457-022d-4269-a6f9-e15721fdf0c2&IID=3b8ffe04-f32e-4ec8-833e-f8ffd647187c&quot;>View Log&lt;/a&gt; &lt;a href=&quot;https://img.geocaching.com/cache/large/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg&quot;>Print Picture&lt;/a&gt;&lt;/span&gt;' class="imageLink" rel="gallery">
    //            <img src='https://img.geocaching.com/cache/log/thumb/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg' alt='View Image' /></a>
    //            <span>Image 2 </span>
    //    </td>
    static final Pattern PATTERN_GALLERY_IMAGE = Pattern.compile("<span(?: [^>+]+)>([^>]+)</span>\\s*?" + IMAGE_URL + ".*?<span>([^<]*)</span>", Pattern.DOTALL);
    static final Pattern PATTERN_INVENTORY = Pattern.compile("ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">.*?WidgetBody(.*?)<div");
    static final Pattern PATTERN_INVENTORYINSIDE = Pattern.compile("[^<]*<li>[^<]*<a href=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/(?:track|hide)\\/details\\.aspx\\?(guid|TB)=([0-9a-zA-Z\\-]+)[^\"]*\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>([^<]+)<\\/span>[^<]*<\\/a>[^<]*<\\/li>");
    static final Pattern PATTERN_WATCHLIST = Pattern.compile("data-cacheonwatchlist=\"True\"");
    static final Pattern PATTERN_RELATED_WEB_PAGE = Pattern.compile("ctl00_ContentBody_uxCacheUrl.*? href=\"(.*?)\">");
    static final Pattern PATTERN_BACKGROUND_IMAGE = Pattern.compile("<body background=\"(.+?)\"");
    static final String PATTERN_GC_CHECKER = "ctl00_ContentBody_lblSolutionChecker";


    //Try to find pattern as follows:
    //"publicGuid":"abc.def","referenceCode":"PRxyz","id":1234567,"username":"user","dateCreated":"2012-01-21T20:51:14","findCount":15123,
    public static final Pattern PATTERN_LOGIN_NAME1 = Pattern.compile(",\"referenceCode\":\"[^\"]+\",\"id\":[0-9]+,\"username\":\\s*\"(([^\\\\\\\"]*(\\\\[a-z\\\"])*)*)\\\",");
    public static final Pattern PATTERN_FINDCOUNT = Pattern.compile("\"findCount\":\\s*([0-9]+)[,\\s]");

    // Info box top-right
    public static final Pattern PATTERN_LOGIN_NAME2 = Pattern.compile("\\swindow(?>\\.|\\[')(?:headerSettings|chromeSettings)(?>'\\])?\\s*=\\s*\\{[\\S\\s]*\"username\":\\s*\"([^\"]*)\",?[\\S\\s]*\\}");
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
     * Example matching text:
     * <a id="ctl00_ContentBody_BugDetails_BugLocation" title="Visit Listing" data-name="Kärntner Adventkalender 2023 - Frohe Weihnachten!" data-status="Cache" href="https://www.geocaching.com/geocache/GCAH5AA">In Kärntner Adventkalender 2023 - Frohe Weihnachten!</a>
     */
    static final Pattern PATTERN_TRACKABLE_SPOTTEDCACHE_BY_GEOCODE = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" title=\"[^\"]*\" data-name=\"([^\"]+)\" data-status=\"Cache\" href=\"[^\"]*/geocache/(GC[A-Z0-9]+)\">[^<]+</a>");
    /**
     * Two groups !
     */
    static final Pattern PATTERN_TRACKABLE_SPOTTEDCACHE_BY_GUID = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" title=\"[^\"]*\" data-name=\"([^\"]+)\" data-status=\"Cache\" href=\"[^\"]*/seek/cache_details.aspx\\?guid=([a-z0-9\\-]+)\">[^<]+</a>");
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
    static final Pattern PATTERN_TRACKABLE_ICON = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" (?:aria-hidden=\"true\" )?src=\"([^\"]+)\"[^>]*>");
    static final Pattern PATTERN_TRACKABLE_TYPE = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" aria-hidden=\"true\" src=\"[^\"]+\" alt=\"([^\"]+)\"[^>]*>");
    static final Pattern PATTERN_TRACKABLE_TYPE_TITLE = Pattern.compile("<title>\\s\\(TB[0-9A-Z]*\\) ([^<]*)<\\/title>");
    static final Pattern PATTERN_TRACKABLE_DISTANCE = Pattern.compile("\\(([0-9.,]+)(km|mi)[^\\)]*\\)\\s*<a href=\"map_gm");
    static final Pattern PATTERN_TRACKABLE_LOG_OUTER = Pattern.compile("<tr class=\"Data BorderTop \\w*\">[\\S\\s]+?(?=</tr>)[\\S\\s]+?(?=<tr)[\\S\\s]+?(?=</tr)");
    static final Pattern PATTERN_TRACKABLE_LOG_INNER = Pattern.compile("/images/logtypes/([^.]+)\\.png[^>]+>&nbsp;([^<]+)</th>[\\S\\s]+?(?=\\?guid=)\\?guid=([^\"]+)\">([^<]+)</a>(?:.+?(?=https://www\\.geocaching\\.com/geocache/)https://www\\.geocaching\\.com/geocache/([^\"]+)\">(<span[^>]+>)?([^<]+))?[\\S\\s]+?(?=/live/log)/live/log/([^\"]+)[\\S\\s]+?(?=TrackLogText)[^\"]+\">([\\S\\s]*?(?=</div>))");
    static final Pattern PATTERN_TRACKABLE_LOG_IMAGES = Pattern.compile("<ul class=\"log_images\"><li><a href=\"([^\"]+)\".+?(?= alt) alt=\"([^\"]+)\"");
    static final Pattern PATTERN_TRACKABLE_IS_LOCKED = Pattern.compile("<a id=\"ctl00_ContentBody_LogLink\"[^(]*\\(locked\\)</a></td>");

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
    // new logpage logtype pattern:         logSettings.logTypes.push({"Value":46,"Description":"Owner maintenance","IsRealtimeOnly":false});
    static final Pattern PATTERN_TYPE4 = Pattern.compile("\"logTypes\":\\[([^]]+)]");
    static final Pattern PATTERN_MAINTENANCE = Pattern.compile("<span id=\"ctl00_ContentBody_LogBookPanel1_lbConfirm\"[^>]*>([^<]*<font[^>]*>)?([^<]+)(</font>[^<]*)?</span>", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_VIEWSTATEFIELDCOUNT = Pattern.compile("id=\"__VIEWSTATEFIELDCOUNT\"[^(value)]+value=\"(\\d+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    static final Pattern PATTERN_VIEWSTATES = Pattern.compile("id=\"__VIEWSTATE(\\d*)\"[^(value)]+value=\"([^\"]+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    static final Pattern PATTERN_USERTOKEN = Pattern.compile("userToken\\s*=\\s*'([^']+)'");
    static final Pattern PATTERN_REQUESTVERIFICATIONTOKEN = Pattern.compile("__RequestVerificationToken\" type=\"hidden\" value=\"([^\"]+)\"");

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

    // Pages with such title seem to be returned with a 200 code instead of 404
    static final String STRING_404_FILE_NOT_FOUND = "<title>404 - File Not Found</title>";

    // URL to message center
    public static final String URL_MESSAGECENTER = "https://www.geocaching.com/account/messagecenter";

    /**
     * Number of logs to retrieve from GC.com
     */
    static final int NUMBER_OF_LOGS = 35;

    private GCConstants() {
        // this class shall not have instances
    }

}
