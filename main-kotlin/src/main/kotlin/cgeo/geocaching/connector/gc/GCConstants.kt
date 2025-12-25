// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.gc

import cgeo.geocaching.log.LogType

import androidx.annotation.NonNull

import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

/**
 * These patterns have been optimized for speed. Improve them only if you can prove
 * that *YOUR* pattern is faster.
 * <br>
 * For further information about patterns have a look at
 * <a href="http://download.oracle.com/javase/1.4.2/docs/api/java/util/regex/Pattern.html">...</a>
 */
class GCConstants {

    static val GC_URL: String = "https://www.geocaching.com/"
    /**
     * Live Map
     */
    static val URL_LIVE_MAP: String = GC_URL + "map/default.aspx"
    /**
     * Format used by geocaching.com when user is not logged in.
     */
    public static val DEFAULT_GC_DATE: String = "MM/dd/yyyy"
    public static val GEOCODE_PATTERN: String = "(?<!\\w)(GC[0-9A-Z&&[^ILOSU]]{1,5})(?=$|\\W|\\s|_)"

    // Patterns for parsing the result of a (detailed) search

    static val PATTERN_HINT: Pattern = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>", Pattern.DOTALL)
    static val PATTERN_DESC: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\">(.*?)</span>\\s*</div>\\s*<(p|div) id=\"ctl00_ContentBody", Pattern.DOTALL)
    static val PATTERN_SHORTDESC: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_ShortDescription\">(.*?)</span>\\s*</div>", Pattern.DOTALL)
    static val PATTERN_GEOCODE: Pattern = Pattern.compile("class=\"CoordInfoCode\">" + GEOCODE_PATTERN + "</span>")
    static val PATTERN_GUID: Pattern = Pattern.compile(Pattern.quote("&wid=") + "([0-9a-z\\-]+)" + Pattern.quote("&"))
    static val PATTERN_SIZE: Pattern = Pattern.compile("/icons/container/([a-z_]+)\\.")
    static val PATTERN_LATLON: Pattern = Pattern.compile("<span id=\"uxLatLon\"[^>]*>(.*?)</span>")
    static val PATTERN_LATLON_ORIG: Pattern = Pattern.compile("\\{\"isUserDefined\":true[^}]+?\"oldLatLngDisplay\":\"([^\"]+)\"\\}")
    static val PATTERN_LOCATION: Pattern = Pattern.compile(Pattern.quote("<span id=\"ctl00_ContentBody_Location\">In ") + "(?:<a href=[^>]*>)?(.*?)<")
    // homeLocation: {"Latitude":50.12345,"Longitude":10.98765}
    static val PATTERN_LOCATION_LOGIN: Pattern = Pattern.compile("homeLocation:\\s*\\{\\\"Latitude\\\":(-?\\d*\\.\\d*),\\\"Longitude\\\":(-?\\d*\\.\\d*)\\}")
    static val PATTERN_PERSONALNOTE: Pattern = Pattern.compile("<div id=\"srOnlyCacheNote\"[^>]*>(.*?)</div>", Pattern.DOTALL)
    static val PATTERN_NAME: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_CacheName\".*?>(.*?)</span>")
    static val PATTERN_DIFFICULTY: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_uxLegendScale\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\"")
    static val PATTERN_TERRAIN: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_Localize[\\d]+\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\"")
    static val PATTERN_OWNER_USERID: Pattern = Pattern.compile("<a href=\"/play/search\\?owner\\[0\\]=(.*?)&a=")
    static val PATTERN_OWNER_GUID: Pattern = Pattern.compile("\\/p(rofile)?\\/\\?guid=([0-9a-z\\-]+)&")
    static val PATTERN_FOUND: Pattern = Pattern.compile("logtypes/48/(" + StringUtils.join(LogType.foundLogTypes(), "|") + ").png\" id=\"ctl00_ContentBody_GeoNav_logTypeImage\"")
    static val PATTERN_DNF: Pattern = Pattern.compile("logtypes/48/(" + LogType.DIDNT_FIND_IT.id + ").png\" id=\"ctl00_ContentBody_GeoNav_logTypeImage\"")
    static val PATTERN_OWNER_DISPLAYNAME: Pattern = Pattern.compile("<div id=\"ctl00_ContentBody_mcd1\">[^<]+<a href=\"[^\"]+\">([^<]+)</a>")
    static val PATTERN_TYPE: Pattern = Pattern.compile("<use xlink:href=\"/app/ui-icons/sprites/cache-types.svg#icon-([0-9a-f]+)")
    static val PATTERN_HIDDEN: Pattern = Pattern.compile("ctl00_ContentBody_mcd2[^:]+:\\s*([^<]+?)<")
    static val PATTERN_HIDDENEVENT: Pattern = Pattern.compile(":\\s*([0-9-/]+)\\s*<div id=\"calLinks\">", Pattern.DOTALL)
    public static val PATTERN_EVENTTIMES: Pattern = Pattern.compile("<div id=\"mcd3\">\\s*[^<]*?\\s+([APM ]*)([0-9]+)[\\.:]([0-9]+)([APM ]*)\\s*</div>\\s*<div id=\"mcd4\">\\s*[^<]*?\\s+([APM ]*)([0-9]+)[\\.:]([0-9]+)([APM ]*)\\s*</div>")
    static val PATTERN_IS_FAVORITE: Pattern = Pattern.compile("<div id=\"pnlFavoriteCache\">"); // without 'class="hideMe"' inside the tag !
    static val PATTERN_FAVORITECOUNT: Pattern = Pattern.compile("<span class=\"favorite-value\">\\D*([0-9]+?)\\D*</span>")
    static val PATTERN_COUNTLOGS: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_lblFindCounts\"><ul(.+?)</ul></span>")
    static val PATTERN_WATCHLIST_COUNT: Pattern = Pattern.compile("data-watchcount=\"(\\d+)\"")

    //example gallery count:
    //<li><a href="/seek/gallery.aspx?guid=3d6f0c14-7d72-43a2-a704-dc4a2b879c73">View gallery (1885)</a></li>
    static val PATTERN_GALLERY_COUNT: Pattern = Pattern.compile("<a href=\"/seek/gallery.aspx\\?guid=[^(]+?\\((\\d+)\\)</a>")
    // matches: the inner JSON Code (w/o {}) of "currentGeocache":{"id":123,"referenceCode":"GCxyz","name":"somename"}
    static val PATTERN_TB_CURRENT_GEOCACHE_JSON: Pattern = Pattern.compile("\"currentGeocache\":\\{([^}]+)\\}")

    /**
     * Two groups !
     */
    static val PATTERN_COUNTLOG: Pattern = Pattern.compile("logtypes/([0-9]+)\\.[^>]+>\\s*([0-9,.]+)")
    static val PATTERN_PREMIUMMEMBERS: Pattern = Pattern.compile("<p class=\"Warning NoBottomSpacing\"")
    static val PATTERN_ATTRIBUTES: Pattern = Pattern.compile("(<img src=\"/images/attributes.*?)</p")
    /**
     * Two groups !
     */
    static val PATTERN_ATTRIBUTESINSIDE: Pattern = Pattern.compile("<img src=\"([^\"]+)\" alt=\"([^\"]+?)\"")
    private static val IMAGE_FORMATS: String = "jpg|jpeg|png|gif|bmp|JPG|JPEG|PNG|GIF|BMP"
    private static val IMAGE_URL: String = "<a href=[\"'](https?://img(?:cdn)?\\.geocaching\\.com[^.]+\\.(?:" + IMAGE_FORMATS + "))[\"'][^>]+>(?:[^>]*</strong>[^>]+>)?"
    //Example HTML to match: <a href="https://img.geocaching.com/cache/large/1711f8a1-796a-405b-82ba-8685f2e9f024.jpg" class="owner-image" rel="owner_image_group" data-title="<strong>indy mit text netz Kopie</strong>">indy mit text netz Kopie</a></li>
    static val PATTERN_SPOILER_IMAGE: Pattern = Pattern.compile(IMAGE_URL + "([^<]*)</a>" + ".*?(?:description\"[^>]*>([^<]+)</span>)?</li>", Pattern.DOTALL)

    //gallery image example:
    //<td>
    //        <span class="date-stamp">15.01.2025</span>
    //        <a href='https://img.geocaching.com/cache/log/large/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg' data-title='&lt;span class=&quot;LogImgTitle&quot;&gt;Image 2&nbsp;&lt;/span&gt;&lt;span class=&quot;LogImgLink&quot;&gt;&lt;a href=&quot;https://www.geocaching.com/seek/log.aspx?LUID=7f9ba457-022d-4269-a6f9-e15721fdf0c2&IID=3b8ffe04-f32e-4ec8-833e-f8ffd647187c&quot;>View Log&lt;/a&gt; &lt;a href=&quot;https://img.geocaching.com/cache/large/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg&quot;>Print Picture&lt;/a&gt;&lt;/span&gt;' class="imageLink" rel="gallery">
    //            <img src='https://img.geocaching.com/cache/log/thumb/3b8ffe04-f32e-4ec8-833e-f8ffd647187c.jpg' alt='View Image' /></a>
    //            <span>Image 2 </span>
    //    </td>
    static val PATTERN_GALLERY_IMAGE: Pattern = Pattern.compile("<span(?: [^>+]+)>([^>]+)</span>\\s*?" + IMAGE_URL + ".*?<span>([^<]*)</span>", Pattern.DOTALL)
    static val PATTERN_INVENTORY: Pattern = Pattern.compile("ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">.*?WidgetBody(.*?)<div")
    static val PATTERN_INVENTORYINSIDE: Pattern = Pattern.compile("[^<]*<li>[^<]*<a href=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/(?:track|hide)\\/details\\.aspx\\?(guid|TB)=([0-9a-zA-Z\\-]+)[^\"]*\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>([^<]+)<\\/span>[^<]*<\\/a>[^<]*<\\/li>")
    static val PATTERN_WATCHLIST: Pattern = Pattern.compile("data-cacheonwatchlist=\"True\"")
    static val PATTERN_RELATED_WEB_PAGE: Pattern = Pattern.compile("ctl00_ContentBody_uxCacheUrl.*? href=\"(.*?)\">")
    static val PATTERN_BACKGROUND_IMAGE: Pattern = Pattern.compile("<body background=\"(.+?)\"")
    static val PATTERN_GC_CHECKER: String = "ctl00_ContentBody_lblSolutionChecker"


    //Try to find pattern as follows:
    //"publicGuid":"abc.def","referenceCode":"PRxyz","id":1234567,"username":"user","dateCreated":"2012-01-21T20:51:14","findCount":15123,
    public static val PATTERN_LOGIN_NAME1: Pattern = Pattern.compile(",\"referenceCode\":\"[^\"]+\",\"id\":[0-9]+,\"username\":\\s*\"(([^\\\\\\\"]*(\\\\[a-z\\\"])*)*)\\\",")
    public static val PATTERN_FINDCOUNT: Pattern = Pattern.compile("\"findCount\":\\s*([0-9]+)[,\\s]")

    // Info box top-right
    public static val PATTERN_LOGIN_NAME2: Pattern = Pattern.compile("window(?>\\.|\\[')(?:headerSettings|chromeSettings)(?>'\\])?\\s*=\\s*\\{[\\S\\s]*\"username\":\\s*\"([^\"]*)\",?[\\S\\s]*\\}")
    /**
     * Use replaceAll("[,.]","") on the resulting string before converting to an Int
     */
    static val PATTERN_CACHES_FOUND: Pattern = Pattern.compile("window(?>\\.|\\[')(?:headerSettings|chromeSettings)(?>'\\])?\\s*=\\s*\\{[\\S\\s]*\"findCount\":\\s*([0-9]*)[\\S\\s]*\\}")

    // Patterns for parsing trackables

    static val PATTERN_TRACKABLE_GUID: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_lnkPrint\" aria-labelledby=\"[^\"]*\" href=\".*sheet\\.aspx\\?guid=([a-z0-9\\-]+)\"[^>]*>[^<]*</a>")
    static val PATTERN_TRACKABLE_GEOCODE: Pattern = Pattern.compile(Pattern.quote("CoordInfoCode\">") + "(TB[0-9A-Z&&[^ILOSU]]+)<")

    // multiple error codes, depending on the search term for the trackable code. Interestingly, these error messages are not localized by now.
    static val ERROR_TB_DOES_NOT_EXIST: String = "does not exist in the system"
    static val ERROR_TB_ELEMENT_EXCEPTION: String = "ElementNotFound Exception"
    static val ERROR_TB_ARITHMETIC_OVERFLOW: String = "operation resulted in an overflow"
    static val ERROR_TB_NOT_ACTIVATED: String = "hasn't been activated"
    /**
     * some parts of the webpage don't correctly encode the name, therefore this pattern must be checked with a
     * trackable name that needs HTML encoding
     */
    static val PATTERN_TRACKABLE_NAME: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_lbHeading\">(.*?)</span>")
    /**
     * Three groups !
     */
    static val PATTERN_TRACKABLE_OWNER: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugOwner\"[^>]+href=\"https?://www.geocaching.com/p(rofile)?/\\?guid=(.*?)\">(.*?)</a>")
    static val PATTERN_TRACKABLE_RELEASES: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_BugDetails_BugReleaseDate\">([^<]+)<\\/span>[^<]*</dd>")
    static val PATTERN_TRACKABLE_FOUND_LOG: Pattern = Pattern.compile("<img src=\"/images/logtypes/(.*?).png\" id=\"ctl00_ContentBody_InteractionLogTypeImage\"[^>]*>")
    static val PATTERN_TRACKABLE_DISPOSITION_LOG: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_InteractionLogLink\"[^>]*href=\"/track/log.aspx\\?LUID=([a-z0-9\\-]+)\">[^0-9]+(.*?)</a>")
    static val PATTERN_TRACKABLE_ORIGIN: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_BugDetails_BugOrigin\">([^<]+)<\\/span>[^<]*</dd>")
    /**
     * Two groups !
     * Example matching text:
     * <a id="ctl00_ContentBody_BugDetails_BugLocation" title="Visit Listing" data-name="Kärntner Adventkalender 2023 - Frohe Weihnachten!" data-status="Cache" href="https://www.geocaching.com/geocache/GCAH5AA">In Kärntner Adventkalender 2023 - Frohe Weihnachten!</a>
     */
    static val PATTERN_TRACKABLE_SPOTTEDCACHE_BY_GEOCODE: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" title=\"[^\"]*\" data-name=\"([^\"]+)\" data-status=\"Cache\" href=\"[^\"]*/geocache/(GC[A-Z0-9]+)\">[^<]+</a>")
    /**
     * Two groups !
     */
    static val PATTERN_TRACKABLE_SPOTTEDCACHE_BY_GUID: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" title=\"[^\"]*\" data-name=\"([^\"]+)\" data-status=\"Cache\" href=\"[^\"]*/seek/cache_details.aspx\\?guid=([a-z0-9\\-]+)\">[^<]+</a>")
    /**
     * Three groups !
     */
    static val PATTERN_TRACKABLE_SPOTTEDUSER: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" data-name=\"([^\"]+)\" data-status=\"User\" href=\"[^\"]*/p(rofile)?/\\?guid=([a-z0-9\\-]+)\">")
    static val PATTERN_TRACKABLE_SPOTTEDUNKNOWN: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" data-status=\"Unknown\">[^<]*</a>")
    static val PATTERN_TRACKABLE_SPOTTEDOWNER: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" data-status=\"Owner\">[^<]*</a>")
    static val PATTERN_TRACKABLE_GOAL: Pattern = Pattern.compile("<div id=\"TrackableGoal\">[^<]*<p>(.*?)</p>[^<]*</div>", Pattern.DOTALL)
    /**
     * Four groups
     */
    static val PATTERN_TRACKABLE_DETAILSIMAGE: Pattern = Pattern.compile("<div id=\"TrackableDetails\">([^<]*<p>([^<]*<img id=\"ctl00_ContentBody_BugDetails_BugImage\" class=\"[^\"]+\" src=\"([^\"]+)\"[^>]*>)?[^<]*</p>)?[^<]*<p[^>]*>(.*)</p>[^<]*</div> <div class=\"Clear\">")
    static val PATTERN_TRACKABLE_ICON: Pattern = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" (?:aria-hidden=\"true\" )?src=\"([^\"]+)\"[^>]*>")
    static val PATTERN_TRACKABLE_TYPE: Pattern = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" (?:aria-hidden=\"true\" )?src=\"[^\"]+\" alt=\"([^\"]+)\"[^>]*>")
    static val PATTERN_TRACKABLE_TYPE_TITLE: Pattern = Pattern.compile("<title>\\s\\(TB[0-9A-Z]*\\) ([^<]*)<\\/title>")
    static val PATTERN_TRACKABLE_DISTANCE: Pattern = Pattern.compile("\\(([0-9.,]+)(km|mi)[^\\)]*\\)\\s*<a href=\"map_gm")
    static val PATTERN_TRACKABLE_LOG_OUTER: Pattern = Pattern.compile("<tr class=\"Data BorderTop \\w*\">[\\S\\s]+?(?=</tr>)[\\S\\s]+?(?=<tr)[\\S\\s]+?(?=</tr)")
    static val PATTERN_TRACKABLE_LOG_INNER: Pattern = Pattern.compile("/images/logtypes/([^.]+)\\.png[^>]+>&nbsp;([^<]+)</th>[\\S\\s]+?(?=\\?guid=)\\?guid=([^\"]+)\">([^<]+)</a>(?:.+?(?=https://www\\.geocaching\\.com/geocache/)https://www\\.geocaching\\.com/geocache/([^\"]+)\">(<span[^>]+>)?([^<]+))?[\\S\\s]+?(?=/live/log)/live/log/([^\"]+)[\\S\\s]+?(?=TrackLogText)[^\"]+\">([\\S\\s]*?(?=</div>))")
    static val PATTERN_TRACKABLE_LOG_IMAGES: Pattern = Pattern.compile("<ul class=\"log_images\"><li><a href=\"([^\"]+)\".+?(?= alt) alt=\"([^\"]+)\"")
    static val PATTERN_TRACKABLE_IS_LOCKED: Pattern = Pattern.compile("<a id=\"ctl00_ContentBody_LogLink\"[^(]*\\(locked\\)</a></td>")

    // Patterns for waypoints

    static val PATTERN_WPTYPE: Pattern = Pattern.compile("\\/WptTypes\\/sm\\/(.+)\\.jpg", Pattern.CASE_INSENSITIVE)
    static val PATTERN_WPPREFIXORLOOKUPORLATLON: Pattern = Pattern.compile(">([^<]*<[^>]+>)?([^<]+)(<[^>]+>[^<]*)?<\\/td>")
    static val PATTERN_WPNAME: Pattern = Pattern.compile(">[^<]*<a[^>]+>([^<]*)<\\/a>")
    static val PATTERN_WPNOTE: Pattern = Pattern.compile("colspan=\"6\">(.*)" + Pattern.quote("</td>"), Pattern.DOTALL)

    // Patterns for different purposes

    /**
     * replace line break and paragraph tags
     */
    static val PATTERN_LINEBREAK: Pattern = Pattern.compile("<(br|p)[^>]*>")
    // logpage logtype pattern:         logSettings.logTypes.push({"Value":46,"Description":"Owner maintenance","IsRealtimeOnly":false})
    static val PATTERN_TYPE4: Pattern = Pattern.compile("\"logTypes\":\\[([^]]+)]")
    static val PATTERN_TOTAL_TRACKABLES: Pattern = Pattern.compile("\"totalTrackables\":\"([\\d]+)\"")
    static val PATTERN_LOGPAGE_TRACKABLES: Pattern = Pattern.compile("\"trackables\":\\[(.+?\\})\\],[\"_sentry|\\},\"__N_SSP]")
    static val PATTERN_MAINTENANCE: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_LogBookPanel1_lbConfirm\"[^>]*>([^<]*<font[^>]*>)?([^<]+)(</font>[^<]*)?</span>", Pattern.CASE_INSENSITIVE)
    static val PATTERN_VIEWSTATEFIELDCOUNT: Pattern = Pattern.compile("id=\"__VIEWSTATEFIELDCOUNT\"[^(value)]+value=\"(\\d+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
    static val PATTERN_VIEWSTATES: Pattern = Pattern.compile("id=\"__VIEWSTATE(\\d*)\"[^(value)]+value=\"([^\"]+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
    static val PATTERN_USERTOKEN: Pattern = Pattern.compile("userToken\\s*=\\s*'([^']+)'")
    static val PATTERN_REQUESTVERIFICATIONTOKEN: Pattern = Pattern.compile("__RequestVerificationToken\" type=\"hidden\" value=\"([^\"]+)\"")

    /**
     * downloadable PQs
     */
    static val PATTERN_PQ_LAST_GEN: Pattern = Pattern.compile("([^(]*)(\\(([\\d]+)?)?")

    /**
     * Live Map since 14.02.2012
     */
    static val PATTERN_USERSESSION: Pattern = Pattern.compile("UserSession\\('([^']+)'")
    static val PATTERN_SESSIONTOKEN: Pattern = Pattern.compile("sessionToken:'([^']+)'")

    static val STRING_PREMIUMONLY: String = "class=\"illustration lock-icon\""
    static val PATTERN_PREMIUMONLY_CACHETYPE: Pattern = Pattern.compile("/app/ui-icons/sprites/cache-types\\.svg#icon-([^\"\\-]+)-?([^\"]+)?")
    static val PATTERN_PREMIUMONLY_CACHENAME: Pattern = Pattern.compile("<h1 class=\"heading-3\">(.+)</h1>")
    static val PATTERN_PREMIUMONLY_GEOCODE: Pattern = Pattern.compile("<li class=\"li__gccode\">([^<]+)")
    static val PATTERN_PREMIUMONLY_DIFFICULTY: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_lblDifficulty\"(?:.|\\s)*?<span>([0-5](?:[\\.,]5)?)</span>")
    static val PATTERN_PREMIUMONLY_TERRAIN: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_lblTerrain\"(?:.|\\s)*?<span>([0-5](?:[\\.,]5)?)</span>")
    static val PATTERN_PREMIUMONLY_SIZE: Pattern = Pattern.compile("<span id=\"ctl00_ContentBody_lblSize\"(?:.|\\s)*?<span>([^<]+)</span>")

    static val STRING_UNPUBLISHED_OTHER: String = "you cannot view this cache listing until it has been published"; //TODO: unpublished caches return a 404 page for other users meanwhile, so they cannot be detected anymore. We could however indicate this status for owners...
    static val STRING_UNPUBLISHED_FROM_SEARCH: String = "class=\"UnpublishedCacheSearchWidget"; // do not include closing brace as the CSS can contain additional styles
    static val STRING_UNKNOWN_ERROR: String = "An Error Has Occurred"; //TODO: might be language dependent - but when does it occur at all?
    static val STRING_STATUS_DISABLED: String = "<div id=\"ctl00_ContentBody_uxDisabledMessageBody\""
    static val STRING_STATUS_ARCHIVED: String = "<div id=\"ctl00_ContentBody_archivedMessage\""
    static val STRING_STATUS_LOCKED: String = "<div id=\"ctl00_ContentBody_lockedMessage\""
    static val STRING_CACHEDETAILS: String = "id=\"cacheDetails\""

    // Pages with such title seem to be returned with a 200 code instead of 404
    static val STRING_404_FILE_NOT_FOUND: String = "<title>404 - File Not Found</title>"

    // URL to message center
    public static val URL_MESSAGECENTER: String = "https://www.geocaching.com/account/messagecenter"

    /**
     * Number of logs to retrieve from GC.com
     */
    static val NUMBER_OF_LOGS: Int = 35

    private GCConstants() {
        // this class shall not have instances
    }

}
