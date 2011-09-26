// $codepro.audit.disable logExceptions
package cgeo.geocaching;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.LocParser;
import cgeo.geocaching.geopoint.DistanceParser;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.CollectionUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class cgBase {

    private final static Pattern patternGeocode = Pattern.compile("<meta name=\"og:url\" content=\"[^\"]+/(GC[0-9A-Z]+)\"[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternCacheId = Pattern.compile("/seek/log\\.aspx\\?ID=(\\d+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternCacheGuid = Pattern.compile(Pattern.quote("&wid=") + "([0-9a-z\\-]+)" + Pattern.quote("&"), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternType = Pattern.compile("<img src=\"[^\"]*/WptTypes/\\d+\\.gif\" alt=\"([^\"]+)\" (title=\"[^\"]*\" )?width=\"32\" height=\"32\"[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private final static Pattern patternName = Pattern.compile("<h2[^>]*>[^<]*<span id=\"ctl00_ContentBody_CacheName\">([^<]+)<\\/span>[^<]*<\\/h2>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternSize = Pattern.compile("<div class=\"CacheSize[^\"]*\">[^<]*<p[^>]*>[^S]*Size[^:]*:[^<]*<span[^>]*>[^<]*<img src=\"[^\"]*/icons/container/[a-z_]+\\.gif\" alt=\"Size: ([^\"]+)\"[^>]*>[^<]*<small>[^<]*</small>[^<]*</span>[^<]*</p>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternDifficulty = Pattern.compile("<span id=\"ctl00_ContentBody_uxLegendScale\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"[^\"]+\"[^>]*>[^<]*</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternTerrain = Pattern.compile("<span id=\"ctl00_ContentBody_Localize6\"[^>]*>[^<]*<img src=\"[^\"]*/images/stars/stars([0-9_]+)\\.gif\" alt=\"[^\"]+\"[^>]*>[^<]*</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternOwner = Pattern.compile("<span class=\"minorCacheDetails\">\\W*An?(\\W*Event)?\\W*cache\\W*by[^<]*<a href=\"[^\"]+\">([^<]+)</a>[^<]*</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternOwnerReal = Pattern.compile("<a id=\"ctl00_ContentBody_uxFindLinksHiddenByThisUser\" href=\"[^\"]*/seek/nearest\\.aspx\\?u=*([^\"]+)\">[^<]+</a>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternHidden = Pattern.compile("<span[^>]*>\\W*Hidden[\\s:]*([^<]+)</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternHiddenEvent = Pattern.compile("<span[^>]*>\\W*Event\\W*Date[^:]*:([^<]*)</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternFavourite = Pattern.compile("<a id=\"uxFavContainerLink\"[^>]*>[^<]*<div[^<]*<span class=\"favorite-value\">[^\\d]*([0-9]+)[^\\d^<]*</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private final static Pattern patternFound = Pattern.compile("<p>[^<]*<a id=\"ctl00_ContentBody_hlFoundItLog\"[^<]*<img src=\".*/images/stockholm/16x16/check\\.gif\"[^>]*>[^<]*</a>[^<]*</p>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternFoundAlternative = Pattern.compile("<div class=\"StatusInformationWidget FavoriteWidget\"", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternLatLon = Pattern.compile("<span id=\"ctl00_ContentBody_LatLon\"[^>]*>(<b>)?([^<]*)(<\\/b>)?<\\/span>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternLocation = Pattern.compile("<span id=\"ctl00_ContentBody_Location\"[^>]*>In ([^<]*)", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternHint = Pattern.compile("<div id=\"div_hint\"[^>]*>(.*?)</div>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternPersonalNote = Pattern.compile("<p id=\"cache_note\"[^>]*>([^<]*)</p>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternDescShort = Pattern.compile("<div class=\"UserSuppliedContent\">[^<]*<span id=\"ctl00_ContentBody_ShortDescription\"[^>]*>((?:(?!</span>[^\\w^<]*</div>).)*)</span>[^\\w^<]*</div>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternDesc = Pattern.compile("<span id=\"ctl00_ContentBody_LongDescription\"[^>]*>" + "(.*)</span>[^<]*</div>[^<]*<p>[^<]*</p>[^<]*<p>[^<]*<strong>\\W*Additional Hints</strong>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternCountLogs = Pattern.compile("<span id=\"ctl00_ContentBody_lblFindCounts\"><p(.+?)<\\/p><\\/span>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternCountLog = Pattern.compile("src=\"\\/images\\/icons\\/(.+?).gif\"[^>]+> (\\d*[,.]?\\d+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternAttributes = Pattern.compile("<h3 class=\"WidgetHeader\">[^<]*<img[^>]+>\\W*Attributes[^<]*</h3>[^<]*<div class=\"WidgetBody\">(([^<]*<img src=\"[^\"]+\" alt=\"[^\"]+\"[^>]*>)+)[^<]*<p", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternAttributesInside = Pattern.compile("[^<]*<img src=\"([^\"]+)\" alt=\"([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternSpoilers = Pattern.compile("<p class=\"NoPrint\">\\s+((?:<a href=\"http://img\\.geocaching\\.com/cache/[^.]+\\.jpg\"[^>]+><img class=\"StatusIcon\"[^>]+><span>[^<]+</span></a><br />(?:[^<]+<br /><br />)?)+)\\s+</p>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private final static Pattern patternSpoilersInside = Pattern.compile("<a href=\"(http://img\\.geocaching\\.com/cache/[^.]+\\.jpg)\"[^>]+><img class=\"StatusIcon\"[^>]+><span>([^<]+)</span></a><br />(?:([^<]+)<br /><br />)?", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternInventory = Pattern.compile("<span id=\"ctl00_ContentBody_uxTravelBugList_uxInventoryLabel\">\\W*Inventory[^<]*</span>[^<]*</h3>[^<]*<div class=\"WidgetBody\">([^<]*<ul>(([^<]*<li>[^<]*<a href=\"[^\"]+\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>[^<]+<\\/span>[^<]*<\\/a>[^<]*<\\/li>)+)[^<]*<\\/ul>)?", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternInventoryInside = Pattern.compile("[^<]*<li>[^<]*<a href=\"[a-z0-9\\-\\_\\.\\?\\/\\:\\@]*\\/track\\/details\\.aspx\\?guid=([0-9a-z\\-]+)[^\"]*\"[^>]*>[^<]*<img src=\"[^\"]+\"[^>]*>[^<]*<span>([^<]+)<\\/span>[^<]*<\\/a>[^<]*<\\/li>", Pattern.CASE_INSENSITIVE);
    private final static Pattern patternOnWatchlist = Pattern.compile("<img\\s*src=\"\\/images\\/stockholm\\/16x16\\/icon_stop_watchlist.gif\"", Pattern.CASE_INSENSITIVE);

    private final static Pattern PATTERN_TRACKABLE_TrackableId = Pattern.compile("<a id=\"ctl00_ContentBody_LogLink\" title=\"[^\"]*\" href=\".*log\\.aspx\\?wid=([a-z0-9\\-]+)\"[^>]*>[^<]*</a>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Geocode = Pattern.compile("<span id=\"ctl00_ContentBody_BugDetails_BugTBNum\" String=\"[^\"]*\">Use[^<]*<strong>(TB[0-9a-z]+)[^<]*</strong> to reference this item.[^<]*</span>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Name = Pattern.compile("<h2>([^<]*<img[^>]*>)?[^<]*<span id=\"ctl00_ContentBody_lbHeading\">([^<]+)</span>[^<]*</h2>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Owner = Pattern.compile("<dt>\\W*Owner:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugOwner\" title=\"[^\"]*\" href=\"[^\"]*/profile/\\?guid=([a-z0-9\\-]+)\">([^<]+)<\\/a>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Released = Pattern.compile("<dt>\\W*Released:[^<]*</dt>[^<]*<dd>[^<]*<span id=\"ctl00_ContentBody_BugDetails_BugReleaseDate\">([^<]+)<\\/span>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Origin = Pattern.compile("<dt>\\W*Origin:[^<]*</dt>[^<]*<dd>[^<]*<span id=\"ctl00_ContentBody_BugDetails_BugOrigin\">([^<]+)<\\/span>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_SpottedCache = Pattern.compile("<dt>\\W*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" title=\"[^\"]*\" href=\"[^\"]*/seek/cache_details.aspx\\?guid=([a-z0-9\\-]+)\">In ([^<]+)</a>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_SpottedUser = Pattern.compile("<dt>\\W*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\" href=\"[^\"]*/profile/\\?guid=([a-z0-9\\-]+)\">In the hands of ([^<]+).</a>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_SpottedUnknown = Pattern.compile("<dt>\\W*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\">Unknown Location[^<]*</a>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_SpottedOwner = Pattern.compile("<dt>\\W*Recently Spotted:[^<]*</dt>[^<]*<dd>[^<]*<a id=\"ctl00_ContentBody_BugDetails_BugLocation\">In the hands of the owner[^<]*</a>[^<]*</dd>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Goal = Pattern.compile("<h3>\\W*Current GOAL[^<]*</h3>[^<]*<p[^>]*>(.*)</p>[^<]*<h3>\\W*About This Item[^<]*</h3>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_DetailsImage = Pattern.compile("<h3>\\W*About This Item[^<]*</h3>([^<]*<p>([^<]*<img id=\"ctl00_ContentBody_BugDetails_BugImage\" class=\"[^\"]+\" src=\"([^\"]+)\"[^>]*>)?[^<]*</p>)?[^<]*<p[^>]*>(.*)</p>[^<]*<div id=\"ctl00_ContentBody_BugDetails_uxAbuseReport\">", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Icon = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" src=\"([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Type = Pattern.compile("<img id=\"ctl00_ContentBody_BugTypeImage\" class=\"TravelBugHeaderIcon\" src=\"[^\"]+\" alt=\"([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Distance = Pattern.compile("<h4[^>]*\\W*Tracking History \\(([0-9.,]+(km|mi))[^\\)]*\\)", Pattern.CASE_INSENSITIVE);
    private final static Pattern PATTERN_TRACKABLE_Log = Pattern.compile("<tr class=\"Data.+?src=\"/images/icons/([^.]+)\\.gif[^>]+>&nbsp;([^<]+)</td>.+?guid.+?>([^<]+)</a>.+?(?:guid=([^\"]+)\">([^<]+)</a>.+?)?<td colspan=\"4\">(.+?)(?:<ul.+?ul>)?\\s*</td>\\s*</tr>", Pattern.CASE_INSENSITIVE);

    private static final String passMatch = "(?<=[\\?&])[Pp]ass(w(or)?d)?=[^&#$]+";

    public final static Map<String, String> cacheTypes = new HashMap<String, String>();
    public final static Map<String, String> cacheIDs = new HashMap<String, String>();
    static {
        for (CacheType ct : CacheType.values()) {
            cacheTypes.put(ct.pattern, ct.id);
            cacheIDs.put(ct.id, ct.guid);
        }
    }
    public final static Map<String, String> cacheTypesInv = new HashMap<String, String>();
    public final static Map<String, String> cacheIDsChoices = new HashMap<String, String>();
    public final static Map<CacheSize, String> cacheSizesInv = new HashMap<CacheSize, String>();
    public final static Map<String, String> waypointTypes = new HashMap<String, String>();
    public final static Map<String, Integer> logTypes = new HashMap<String, Integer>();
    public final static Map<String, Integer> logTypes0 = new HashMap<String, Integer>();
    public final static Map<Integer, String> logTypes1 = new HashMap<Integer, String>();
    public final static Map<Integer, String> logTypes2 = new HashMap<Integer, String>();
    public final static Map<Integer, String> logTypesTrackable = new HashMap<Integer, String>();
    public final static Map<Integer, String> logTypesTrackableAction = new HashMap<Integer, String>();
    public final static Map<Integer, String> errorRetrieve = new HashMap<Integer, String>();
    public final static Map<String, SimpleDateFormat> gcCustomDateFormats;
    static {
        final String[] formats = new String[] {
                "MM/dd/yyyy",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd/MMM/yyyy",
                "MMM/dd/yyyy",
                "dd MMM yy",
                "dd/MM/yyyy"
        };

        Map<String, SimpleDateFormat> map = new HashMap<String, SimpleDateFormat>();

        for (String format : formats)
        {
            map.put(format, new SimpleDateFormat(format, Locale.ENGLISH));
        }

        gcCustomDateFormats = Collections.unmodifiableMap(map);
    }
    public final static SimpleDateFormat dateTbIn1 = new SimpleDateFormat("EEEEE, dd MMMMM yyyy", Locale.ENGLISH); // Saturday, 28 March 2009
    public final static SimpleDateFormat dateTbIn2 = new SimpleDateFormat("EEEEE, MMMMM dd, yyyy", Locale.ENGLISH); // Saturday, March 28, 2009
    public final static SimpleDateFormat dateSqlIn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 2010-07-25 14:44:01
    private Resources res = null;
    private static final Pattern patternLoggedIn = Pattern.compile("<span class=\"Success\">You are logged in as[^<]*<strong[^>]*>([^<]+)</strong>[^<]*</span>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern patternLogged2In = Pattern.compile("<strong>\\W*Hello,[^<]*<a[^>]+>([^<]+)</a>[^<]*</strong>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern patternViewstateFieldCount = Pattern.compile("id=\"__VIEWSTATEFIELDCOUNT\"[^(value)]+value=\"(\\d+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern patternViewstates = Pattern.compile("id=\"__VIEWSTATE(\\d*)\"[^(value)]+value=\"([^\"]+)\"[^>]+>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern patternIsPremium = Pattern.compile("<span id=\"ctl00_litPMLevel\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
    private static final Pattern patternUserToken = Pattern.compile("userToken\\s*=\\s*'([^']+)'");
    public static final float miles2km = 1.609344f;
    public static final float feet2km = 0.0003048f;
    public static final float yards2km = 0.0009144f;
    public static final double deg2rad = Math.PI / 180;
    public static final double rad2deg = 180 / Math.PI;
    public static final float erad = 6371.0f;
    private cgeoapplication app = null;
    private cgSettings settings = null;
    public String version = null;
    private String idBrowser = "Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.86 Safari/533.4";
    Context context = null;
    final private static Map<String, Integer> gcIcons = new HashMap<String, Integer>();
    final private static Map<String, Integer> wpIcons = new HashMap<String, Integer>();

    public static final int LOG_FOUND_IT = 2;
    public static final int LOG_DIDNT_FIND_IT = 3;
    public static final int LOG_NOTE = 4;
    public static final int LOG_PUBLISH_LISTING = 1003; // unknown ID; used number doesn't match any GC.com's ID
    public static final int LOG_ENABLE_LISTING = 23;
    public static final int LOG_ARCHIVE = 5;
    public static final int LOG_TEMP_DISABLE_LISTING = 22;
    public static final int LOG_NEEDS_ARCHIVE = 7;
    public static final int LOG_WILL_ATTEND = 9;
    public static final int LOG_ATTENDED = 10;
    public static final int LOG_RETRIEVED_IT = 13;
    public static final int LOG_PLACED_IT = 14;
    public static final int LOG_GRABBED_IT = 19;
    public static final int LOG_NEEDS_MAINTENANCE = 45;
    public static final int LOG_OWNER_MAINTENANCE = 46;
    public static final int LOG_UPDATE_COORDINATES = 47;
    public static final int LOG_DISCOVERED_IT = 48;
    public static final int LOG_POST_REVIEWER_NOTE = 18;
    public static final int LOG_VISIT = 1001; // unknown ID; used number doesn't match any GC.com's ID
    public static final int LOG_WEBCAM_PHOTO_TAKEN = 11;
    public static final int LOG_ANNOUNCEMENT = 74;

    public cgBase(cgeoapplication appIn, cgSettings settingsIn) {
        context = appIn.getBaseContext();
        res = appIn.getBaseContext().getResources();

        // setup cache type mappings

        final String CACHETYPE_ALL_GUID = "9a79e6ce-3344-409c-bbe9-496530baf758";

        cacheIDs.put("all", CACHETYPE_ALL_GUID);
        cacheIDsChoices.put(res.getString(R.string.all), CACHETYPE_ALL_GUID);

        for (CacheType ct : CacheType.values()) {
            String l10n = res.getString(ct.stringId);
            cacheTypesInv.put(ct.id, l10n);
            cacheIDsChoices.put(l10n, ct.guid);
        }

        for (CacheSize cs : CacheSize.values()) {
            cacheSizesInv.put(cs, res.getString(cs.stringId));
        }

        // waypoint types
        waypointTypes.put("flag", res.getString(WaypointType.FLAG.stringId));
        waypointTypes.put("stage", res.getString(WaypointType.STAGE.stringId));
        waypointTypes.put("puzzle", res.getString(WaypointType.PUZZLE.stringId));
        waypointTypes.put("pkg", res.getString(WaypointType.PKG.stringId));
        waypointTypes.put("trailhead", res.getString(WaypointType.TRAILHEAD.stringId));
        waypointTypes.put("waypoint", res.getString(WaypointType.WAYPOINT.stringId));

        // log types
        logTypes.put("icon_smile", LOG_FOUND_IT);
        logTypes.put("icon_sad", LOG_DIDNT_FIND_IT);
        logTypes.put("icon_note", LOG_NOTE);
        logTypes.put("icon_greenlight", LOG_PUBLISH_LISTING);
        logTypes.put("icon_enabled", LOG_ENABLE_LISTING);
        logTypes.put("traffic_cone", LOG_ARCHIVE);
        logTypes.put("icon_disabled", LOG_TEMP_DISABLE_LISTING);
        logTypes.put("icon_remove", LOG_NEEDS_ARCHIVE);
        logTypes.put("icon_rsvp", LOG_WILL_ATTEND);
        logTypes.put("icon_attended", LOG_ATTENDED);
        logTypes.put("picked_up", LOG_RETRIEVED_IT);
        logTypes.put("dropped_off", LOG_PLACED_IT);
        logTypes.put("transfer", LOG_GRABBED_IT);
        logTypes.put("icon_needsmaint", LOG_NEEDS_MAINTENANCE);
        logTypes.put("icon_maint", LOG_OWNER_MAINTENANCE);
        logTypes.put("coord_update", LOG_UPDATE_COORDINATES);
        logTypes.put("icon_discovered", LOG_DISCOVERED_IT);
        logTypes.put("big_smile", LOG_POST_REVIEWER_NOTE);
        logTypes.put("icon_visited", LOG_VISIT); // unknown ID; used number doesn't match any GC.com's ID
        logTypes.put("icon_camera", LOG_WEBCAM_PHOTO_TAKEN); // unknown ID; used number doesn't match any GC.com's ID
        logTypes.put("icon_announcement", LOG_ANNOUNCEMENT); // unknown ID; used number doesn't match any GC.com's ID

        logTypes0.put("found it", LOG_FOUND_IT);
        logTypes0.put("didn't find it", LOG_DIDNT_FIND_IT);
        logTypes0.put("write note", LOG_NOTE);
        logTypes0.put("publish listing", LOG_PUBLISH_LISTING);
        logTypes0.put("enable listing", LOG_ENABLE_LISTING);
        logTypes0.put("archive", LOG_ARCHIVE);
        logTypes0.put("temporarily disable listing", LOG_TEMP_DISABLE_LISTING);
        logTypes0.put("needs archived", LOG_NEEDS_ARCHIVE);
        logTypes0.put("will attend", LOG_WILL_ATTEND);
        logTypes0.put("attended", LOG_ATTENDED);
        logTypes0.put("retrieved it", LOG_RETRIEVED_IT);
        logTypes0.put("placed it", LOG_PLACED_IT);
        logTypes0.put("grabbed it", LOG_GRABBED_IT);
        logTypes0.put("needs maintenance", LOG_NEEDS_MAINTENANCE);
        logTypes0.put("owner maintenance", LOG_OWNER_MAINTENANCE);
        logTypes0.put("update coordinates", LOG_UPDATE_COORDINATES);
        logTypes0.put("discovered it", LOG_DISCOVERED_IT);
        logTypes0.put("post reviewer note", LOG_POST_REVIEWER_NOTE);
        logTypes0.put("visit", LOG_VISIT); // unknown ID; used number doesn't match any GC.com's ID
        logTypes0.put("webcam photo taken", LOG_WEBCAM_PHOTO_TAKEN); // unknown ID; used number doesn't match any GC.com's ID
        logTypes0.put("announcement", LOG_ANNOUNCEMENT); // unknown ID; used number doesn't match any GC.com's ID

        logTypes1.put(LOG_FOUND_IT, res.getString(R.string.log_found));
        logTypes1.put(LOG_DIDNT_FIND_IT, res.getString(R.string.log_dnf));
        logTypes1.put(LOG_NOTE, res.getString(R.string.log_note));
        logTypes1.put(LOG_PUBLISH_LISTING, res.getString(R.string.log_published));
        logTypes1.put(LOG_ENABLE_LISTING, res.getString(R.string.log_enabled));
        logTypes1.put(LOG_ARCHIVE, res.getString(R.string.log_archived));
        logTypes1.put(LOG_TEMP_DISABLE_LISTING, res.getString(R.string.log_disabled));
        logTypes1.put(LOG_NEEDS_ARCHIVE, res.getString(R.string.log_needs_archived));
        logTypes1.put(LOG_WILL_ATTEND, res.getString(R.string.log_attend));
        logTypes1.put(LOG_ATTENDED, res.getString(R.string.log_attended));
        logTypes1.put(LOG_RETRIEVED_IT, res.getString(R.string.log_retrieved));
        logTypes1.put(LOG_PLACED_IT, res.getString(R.string.log_placed));
        logTypes1.put(LOG_GRABBED_IT, res.getString(R.string.log_grabbed));
        logTypes1.put(LOG_NEEDS_MAINTENANCE, res.getString(R.string.log_maintenance_needed));
        logTypes1.put(LOG_OWNER_MAINTENANCE, res.getString(R.string.log_maintained));
        logTypes1.put(LOG_UPDATE_COORDINATES, res.getString(R.string.log_update));
        logTypes1.put(LOG_DISCOVERED_IT, res.getString(R.string.log_discovered));
        logTypes1.put(LOG_POST_REVIEWER_NOTE, res.getString(R.string.log_reviewed));
        logTypes1.put(LOG_VISIT, res.getString(R.string.log_taken));
        logTypes1.put(LOG_WEBCAM_PHOTO_TAKEN, res.getString(R.string.log_webcam));
        logTypes1.put(LOG_ANNOUNCEMENT, res.getString(R.string.log_announcement));

        logTypes2.put(LOG_FOUND_IT, res.getString(R.string.log_found)); // traditional, multi, unknown, earth, wherigo, virtual, letterbox
        logTypes2.put(LOG_DIDNT_FIND_IT, res.getString(R.string.log_dnf)); // traditional, multi, unknown, earth, wherigo, virtual, letterbox, webcam
        logTypes2.put(LOG_NOTE, res.getString(R.string.log_note)); // traditional, multi, unknown, earth, wherigo, virtual, event, letterbox, webcam, trackable
        logTypes2.put(LOG_PUBLISH_LISTING, res.getString(R.string.log_published)); // X
        logTypes2.put(LOG_ENABLE_LISTING, res.getString(R.string.log_enabled)); // owner
        logTypes2.put(LOG_ARCHIVE, res.getString(R.string.log_archived)); // traditional, multi, unknown, earth, event, wherigo, virtual, letterbox, webcam
        logTypes2.put(LOG_TEMP_DISABLE_LISTING, res.getString(R.string.log_disabled)); // owner
        logTypes2.put(LOG_NEEDS_ARCHIVE, res.getString(R.string.log_needs_archived)); // traditional, multi, unknown, earth, event, wherigo, virtual, letterbox, webcam
        logTypes2.put(LOG_WILL_ATTEND, res.getString(R.string.log_attend)); // event
        logTypes2.put(LOG_ATTENDED, res.getString(R.string.log_attended)); // event
        logTypes2.put(LOG_WEBCAM_PHOTO_TAKEN, res.getString(R.string.log_webcam)); // webcam
        logTypes2.put(LOG_RETRIEVED_IT, res.getString(R.string.log_retrieved)); //trackable
        logTypes2.put(LOG_GRABBED_IT, res.getString(R.string.log_grabbed)); //trackable
        logTypes2.put(LOG_NEEDS_MAINTENANCE, res.getString(R.string.log_maintenance_needed)); // traditional, unknown, multi, wherigo, virtual, letterbox, webcam
        logTypes2.put(LOG_OWNER_MAINTENANCE, res.getString(R.string.log_maintained)); // owner
        logTypes2.put(LOG_DISCOVERED_IT, res.getString(R.string.log_discovered)); //trackable
        logTypes2.put(LOG_POST_REVIEWER_NOTE, res.getString(R.string.log_reviewed)); // X
        logTypes2.put(LOG_ANNOUNCEMENT, res.getString(R.string.log_announcement)); // X

        // trackables for logs
        logTypesTrackable.put(0, res.getString(R.string.log_tb_nothing)); // do nothing
        logTypesTrackable.put(1, res.getString(R.string.log_tb_visit)); // visit cache
        logTypesTrackable.put(2, res.getString(R.string.log_tb_drop)); // drop here
        logTypesTrackableAction.put(0, ""); // do nothing
        logTypesTrackableAction.put(1, "_Visited"); // visit cache
        logTypesTrackableAction.put(2, "_DroppedOff"); // drop here

        // retrieving errors (because of ____ )
        errorRetrieve.put(1, res.getString(R.string.err_none));
        errorRetrieve.put(0, res.getString(R.string.err_start));
        errorRetrieve.put(-1, res.getString(R.string.err_parse));
        errorRetrieve.put(-2, res.getString(R.string.err_server));
        errorRetrieve.put(-3, res.getString(R.string.err_login));
        errorRetrieve.put(-4, res.getString(R.string.err_unknown));
        errorRetrieve.put(-5, res.getString(R.string.err_comm));
        errorRetrieve.put(-6, res.getString(R.string.err_wrong));
        errorRetrieve.put(-7, res.getString(R.string.err_license));

        // init
        app = appIn;
        settings = settingsIn;

        try {
            final PackageManager manager = app.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(app.getPackageName(), 0);
            version = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(cgSettings.tag, "unable to get version information", e);
            version = null;
        }

        if (settings.asBrowser == 1) {
            final long rndBrowser = Math.round(Math.random() * 6);
            if (rndBrowser == 0) {
                idBrowser = "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US) AppleWebKit/533.1 (KHTML, like Gecko) Chrome/5.0.322.2 Safari/533.1";
            } else if (rndBrowser == 1) {
                idBrowser = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; MDDC)";
            } else if (rndBrowser == 2) {
                idBrowser = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.2.3) Gecko/20100401 Firefox/3.6.3";
            } else if (rndBrowser == 3) {
                idBrowser = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_2; en-us) AppleWebKit/531.21.8 (KHTML, like Gecko) Version/4.0.4 Safari/531.21.10";
            } else if (rndBrowser == 4) {
                idBrowser = "Mozilla/5.0 (iPod; U; CPU iPhone OS 2_2_1 like Mac OS X; en-us) AppleWebKit/525.18.1 (KHTML, like Gecko) Version/3.1.1 Mobile/5H11a Safari/525.20";
            } else if (rndBrowser == 5) {
                idBrowser = "Mozilla/5.0 (Linux; U; Android 1.1; en-gb; dream) AppleWebKit/525.10+ (KHTML, like Gecko) Version/3.0.4 Mobile Safari/523.12.2";
            } else if (rndBrowser == 6) {
                idBrowser = "Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/533.4 (KHTML, like Gecko) Chrome/5.0.375.86 Safari/533.4";
            } else {
                idBrowser = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_2; en-US) AppleWebKit/532.9 (KHTML, like Gecko) Chrome/5.0.307.11 Safari/532.9";
            }
        }
    }

    public static String hidePassword(final String message) {
        return message.replaceAll(passMatch, "password=***");
    }

    /**
     * read all viewstates from page
     *
     * @return String[] with all view states
     */
    public static String[] getViewstates(String page) {
        // Get the number of viewstates.
        // If there is only one viewstate, __VIEWSTATEFIELDCOUNT is not present
        int count = 1;
        final Matcher matcherViewstateCount = patternViewstateFieldCount.matcher(page);
        if (matcherViewstateCount.find())
            count = Integer.parseInt(matcherViewstateCount.group(1));

        String[] viewstates = new String[count];

        // Get the viewstates
        int no;
        final Matcher matcherViewstates = patternViewstates.matcher(page);
        while (matcherViewstates.find()) {
            String sno = matcherViewstates.group(1); // number of viewstate
            if ("".equals(sno))
                no = 0;
            else
                no = Integer.parseInt(sno);
            viewstates[no] = matcherViewstates.group(2);
        }

        if (viewstates.length == 1 && viewstates[0] == null)
            // no viewstates were present
            return null;
        else
            return viewstates;
    }

    /**
     * put viewstates into request parameters
     */
    private static void setViewstates(final String[] viewstates, final Parameters params) {
        if (ArrayUtils.isEmpty(viewstates))
            return;
        params.put("__VIEWSTATE", viewstates[0]);
        if (viewstates.length > 1) {
            for (int i = 1; i < viewstates.length; i++)
                params.put("__VIEWSTATE" + i, viewstates[i]);
            params.put("__VIEWSTATEFIELDCOUNT", viewstates.length + "");
        }
    }

    /**
     * transfers the viewstates variables from a page (response) to parameters
     * (next request)
     */
    public static void transferViewstates(final String page, final Parameters params) {
        setViewstates(getViewstates(page), params);
    }

    /**
     * checks if an Array of Strings is empty or not. Empty means:
     * - Array is null
     * - or all elements are null or empty strings
     */
    public static boolean isEmpty(String[] a) {
        if (a == null)
            return true;

        for (String s : a) {
            if (StringUtils.isNotEmpty(s)) {
                return false;
            }
        }
        return true;
    }

    public class loginThread extends Thread {

        @Override
        public void run() {
            login();
        }
    }

    public int login() {
        HttpResponse loginResponse = null;
        String loginData = null;

        String[] viewstates = null;

        final Map<String, String> loginStart = settings.getLogin();

        if (loginStart == null) {
            return -3; // no login information stored
        }

        loginResponse = request("https://www.geocaching.com/login/default.aspx", new HashMap<String, String>(), false, false, false);
        loginData = getResponseData(loginResponse);
        if (StringUtils.isNotBlank(loginData)) {
            if (checkLogin(loginData)) {
                Log.i(cgSettings.tag, "Already logged in Geocaching.com as " + loginStart.get("username"));

                switchToEnglish(viewstates);

                return 1; // logged in
            }

            viewstates = getViewstates(loginData);

            if (isEmpty(viewstates)) {
                Log.e(cgSettings.tag, "cgeoBase.login: Failed to find viewstates");
                return -1; // no viewstates
            }
        } else {
            Log.e(cgSettings.tag, "cgeoBase.login: Failed to retrieve login page (1st)");
            return -2; // no loginpage
        }

        final Map<String, String> login = settings.getLogin();

        if (login == null || StringUtils.isEmpty(login.get("username")) || StringUtils.isEmpty(login.get("password"))) {
            Log.e(cgSettings.tag, "cgeoBase.login: No login information stored");
            return -3;
        }

        clearCookies();

        final Parameters params = new Parameters();

        params.put("__EVENTTARGET", "");
        params.put("__EVENTARGUMENT", "");
        setViewstates(viewstates, params);
        params.put("ctl00$SiteContent$tbUsername", login.get("username"));
        params.put("ctl00$SiteContent$tbPassword", login.get("password"));
        params.put("ctl00$SiteContent$cbRememberMe", "on");
        params.put("ctl00$SiteContent$btnSignIn", "Login");

        loginResponse = postRequest("https://www.geocaching.com/login/default.aspx", params);
        loginData = getResponseData(loginResponse);

        if (StringUtils.isNotBlank(loginData)) {
            if (checkLogin(loginData)) {
                Log.i(cgSettings.tag, "Successfully logged in Geocaching.com as " + login.get("username"));

                switchToEnglish(getViewstates(loginData));

                return 1; // logged in
            } else {
                if (loginData.contains("Your username/password combination does not match.")) {
                    Log.i(cgSettings.tag, "Failed to log in Geocaching.com as " + login.get("username") + " because of wrong username/password");

                    return -6; // wrong login
                } else {
                    Log.i(cgSettings.tag, "Failed to log in Geocaching.com as " + login.get("username") + " for some unknown reason");

                    return -4; // can't login
                }
            }
        } else {
            Log.e(cgSettings.tag, "cgeoBase.login: Failed to retrieve login page (2nd)");

            return -5; // no login page
        }
    }

    public static Boolean isPremium(String page)
    {
        if (checkLogin(page)) {
            final Matcher matcherIsPremium = patternIsPremium.matcher(page);
            return matcherIsPremium.find();
        } else
            return false;
    }

    public static Boolean checkLogin(String page) {
        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.checkLogin: No page given");
            return false;
        }

        // on every page
        final Matcher matcherLogged2In = patternLogged2In.matcher(page);
        if (matcherLogged2In.find()) {
            return true;
        }

        // after login
        final Matcher matcherLoggedIn = patternLoggedIn.matcher(page);
        if (matcherLoggedIn.find()) {
            return true;
        }

        return false;
    }

    public String switchToEnglish(String[] viewstates) {
        final Parameters params = new Parameters();

        setViewstates(viewstates, params);
        params.put("__EVENTTARGET", "ctl00$uxLocaleList$uxLocaleList$ctl00$uxLocaleItem"); // switch to english
        params.put("__EVENTARGUMENT", "");

        return cgBase.getResponseData(postRequest("http://www.geocaching.com/default.aspx", params));
    }

    public cgCacheWrap parseSearch(cgSearchThread thread, String url, String page, boolean showCaptcha) {
        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.parseSearch: No page given");
            return null;
        }

        final cgCacheWrap caches = new cgCacheWrap();
        final List<String> cids = new ArrayList<String>();
        final List<String> guids = new ArrayList<String>();
        String recaptchaChallenge = null;
        String recaptchaText = null;

        caches.url = url;

        final Pattern patternCacheType = Pattern.compile("<td class=\"Merge\">[^<]*<a href=\"[^\"]*/seek/cache_details\\.aspx\\?guid=[^\"]+\"[^>]+>[^<]*<img src=\"[^\"]*/images/wpttypes/[^.]+\\.gif\" alt=\"([^\"]+)\" title=\"[^\"]+\"[^>]*>[^<]*</a>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Pattern patternGuidAndDisabled = Pattern.compile("<img src=\"[^\"]*/images/wpttypes/[^>]*>[^<]*</a></td><td class=\"Merge\">[^<]*<a href=\"[^\"]*/seek/cache_details\\.aspx\\?guid=([a-z0-9\\-]+)\" class=\"lnk([^\"]*)\">([^<]*<span>)?([^<]*)(</span>[^<]*)?</a>[^<]+<br />([^<]*)<span[^>]+>([^<]*)</span>([^<]*<img[^>]+>)?[^<]*<br />[^<]*</td>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Pattern patternTbs = Pattern.compile("<a id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxTravelBugList\" class=\"tblist\" data-tbcount=\"([0-9]+)\" data-id=\"[^\"]*\"[^>]*>(.*)</a>", Pattern.CASE_INSENSITIVE);
        final Pattern patternTbsInside = Pattern.compile("(<img src=\"[^\"]+\" alt=\"([^\"]+)\" title=\"[^\"]*\" />[^<]*)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Pattern patternDirection = Pattern.compile("<img id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxDistanceAndHeading\" title=\"[^\"]*\" src=\"[^\"]*/seek/CacheDir\\.ashx\\?k=([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
        final Pattern patternCode = Pattern.compile("\\|\\W*(GC[a-z0-9]+)[^\\|]*\\|", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        final Pattern patternId = Pattern.compile("name=\"CID\"[^v]*value=\"([0-9]+)\"", Pattern.CASE_INSENSITIVE);
        final Pattern patternFavourite = Pattern.compile("<span id=\"ctl00_ContentBody_dlResults_ctl[0-9]+_uxFavoritesValue\" title=\"[^\"]*\" class=\"favorite-rank\">([0-9]+)</span>", Pattern.CASE_INSENSITIVE);
        final Pattern patternTotalCnt = Pattern.compile("<td class=\"PageBuilderWidget\"><span>Total Records[^<]*<b>(\\d+)<\\/b>", Pattern.CASE_INSENSITIVE);
        final Pattern patternRecaptcha = Pattern.compile("<script[^>]*src=\"[^\"]*/recaptcha/api/challenge\\?k=([^\"]+)\"[^>]*>", Pattern.CASE_INSENSITIVE);
        final Pattern patternRecaptchaChallenge = Pattern.compile("challenge : '([^']+)'", Pattern.CASE_INSENSITIVE);

        caches.viewstates = getViewstates(page);

        // recaptcha
        if (showCaptcha) {
            try {
                String recaptchaJsParam = null;
                final Matcher matcherRecaptcha = patternRecaptcha.matcher(page);
                while (matcherRecaptcha.find()) {
                    if (matcherRecaptcha.groupCount() > 0) {
                        recaptchaJsParam = matcherRecaptcha.group(1);
                    }
                }

                if (recaptchaJsParam != null) {
                    final String recaptchaJs = cgBase.getResponseData(request("http://www.google.com/recaptcha/api/challenge", "k=" + urlencode_rfc3986(recaptchaJsParam.trim()), true));

                    if (StringUtils.isNotBlank(recaptchaJs)) {
                        final Matcher matcherRecaptchaChallenge = patternRecaptchaChallenge.matcher(recaptchaJs);
                        while (matcherRecaptchaChallenge.find()) {
                            if (matcherRecaptchaChallenge.groupCount() > 0) {
                                recaptchaChallenge = matcherRecaptchaChallenge.group(1).trim();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // failed to parse recaptcha challenge
                Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse recaptcha challenge");
            }

            if (thread != null && StringUtils.isNotBlank(recaptchaChallenge)) {
                thread.setChallenge(recaptchaChallenge);
                thread.notifyNeed();
            }
        }

        if (!page.contains("SearchResultsTable")) {
            // there are no results. aborting here avoids a wrong error log in the next parsing step
            return caches;
        }

        int startPos = page.indexOf("<div id=\"ctl00_ContentBody_ResultsPanel\"");
        if (startPos == -1) {
            Log.e(cgSettings.tag, "cgeoBase.parseSearch: ID \"ctl00_ContentBody_dlResults\" not found on page");
            return null;
        }

        page = page.substring(startPos); // cut on <table

        startPos = page.indexOf(">");
        int endPos = page.indexOf("ctl00_ContentBody_UnitTxt");
        if (startPos == -1 || endPos == -1) {
            Log.e(cgSettings.tag, "cgeoBase.parseSearch: ID \"ctl00_ContentBody_UnitTxt\" not found on page");
            return null;
        }

        page = page.substring(startPos + 1, endPos - startPos + 1); // cut between <table> and </table>

        final String[] rows = page.split("<tr class=");
        final int rows_count = rows.length;

        for (int z = 1; z < rows_count; z++) {
            cgCache cache = new cgCache();
            String row = rows[z];

            // check for cache type presence
            if (!row.contains("images/wpttypes")) {
                continue;
            }

            try {
                final Matcher matcherGuidAndDisabled = patternGuidAndDisabled.matcher(row);

                while (matcherGuidAndDisabled.find()) {
                    if (matcherGuidAndDisabled.groupCount() > 0) {
                        guids.add(matcherGuidAndDisabled.group(1));

                        cache.guid = matcherGuidAndDisabled.group(1);
                        if (matcherGuidAndDisabled.group(4) != null) {
                            cache.name = Html.fromHtml(matcherGuidAndDisabled.group(4).trim()).toString();
                        }
                        if (matcherGuidAndDisabled.group(6) != null) {
                            cache.location = Html.fromHtml(matcherGuidAndDisabled.group(6).trim()).toString();
                        }

                        final String attr = matcherGuidAndDisabled.group(2);
                        if (attr != null) {
                            if (attr.contains("Strike")) {
                                cache.disabled = true;
                            } else {
                                cache.disabled = false;
                            }

                            if (attr.contains("OldWarning")) {
                                cache.archived = true;
                            } else {
                                cache.archived = false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // failed to parse GUID and/or Disabled
                Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse GUID and/or Disabled data");
            }

            if (settings.excludeDisabled == 1 && (cache.disabled || cache.archived)) {
                // skip disabled and archived caches
                cache = null;
                continue;
            }

            String inventoryPre = null;

            // GC* code
            try {
                final Matcher matcherCode = patternCode.matcher(row);
                while (matcherCode.find()) {
                    if (matcherCode.groupCount() > 0) {
                        cache.geocode = matcherCode.group(1).toUpperCase();
                    }
                }
            } catch (Exception e) {
                // failed to parse code
                Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache code");
            }

            // cache type
            try {
                final Matcher matcherCacheType = patternCacheType.matcher(row);
                while (matcherCacheType.find()) {
                    if (matcherCacheType.groupCount() > 0) {
                        cache.type = cacheTypes.get(matcherCacheType.group(1).toLowerCase());
                    }
                }
            } catch (Exception e) {
                // failed to parse type
                Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache type");
            }

            // cache direction - image
            if (settings.getLoadDirImg())
            {
                try {
                    final Matcher matcherDirection = patternDirection.matcher(row);
                    while (matcherDirection.find()) {
                        if (matcherDirection.groupCount() > 0) {
                            cache.directionImg = matcherDirection.group(1);
                        }
                    }
                } catch (Exception e) {
                    // failed to parse direction image
                    Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache direction image");
                }
            }

            // cache inventory
            try {
                final Matcher matcherTbs = patternTbs.matcher(row);
                while (matcherTbs.find()) {
                    if (matcherTbs.groupCount() > 0) {
                        cache.inventoryItems = Integer.parseInt(matcherTbs.group(1));
                        inventoryPre = matcherTbs.group(2);
                    }
                }
            } catch (Exception e) {
                // failed to parse inventory
                Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache inventory (1)");
            }

            if (StringUtils.isNotBlank(inventoryPre)) {
                try {
                    final Matcher matcherTbsInside = patternTbsInside.matcher(inventoryPre);
                    while (matcherTbsInside.find()) {
                        if (matcherTbsInside.groupCount() == 2 && matcherTbsInside.group(2) != null) {
                            final String inventoryItem = matcherTbsInside.group(2).toLowerCase();
                            if (inventoryItem.equals("premium member only cache")) {
                                continue;
                            } else {
                                if (cache.inventoryItems <= 0) {
                                    cache.inventoryItems = 1;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // failed to parse cache inventory info
                    Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache inventory info");
                }
            }

            // premium cache
            cache.members = row.contains("/images/small_profile.gif");

            // found it
            cache.found = row.contains("/images/icons/icon_smile");

            // own it
            cache.own = row.contains("/images/silk/star.png");

            // id
            try {
                final Matcher matcherId = patternId.matcher(row);
                while (matcherId.find()) {
                    if (matcherId.groupCount() > 0) {
                        cache.cacheId = matcherId.group(1);
                        cids.add(cache.cacheId);
                    }
                }
            } catch (Exception e) {
                // failed to parse cache id
                Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache id");
            }

            // favourite count
            try {
                final Matcher matcherFavourite = patternFavourite.matcher(row);
                while (matcherFavourite.find()) {
                    if (matcherFavourite.groupCount() > 0) {
                        cache.favouriteCnt = Integer.parseInt(matcherFavourite.group(1));
                    }
                }
            } catch (Exception e) {
                // failed to parse favourite count
                Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse favourite count");
            }

            if (cache.nameSp == null) {
                cache.nameSp = (new Spannable.Factory()).newSpannable(cache.name);
                if (cache.disabled || cache.archived) { // strike
                    cache.nameSp.setSpan(new StrikethroughSpan(), 0, cache.nameSp.toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            caches.cacheList.add(cache);
        }

        // total caches found
        try {
            final Matcher matcherTotalCnt = patternTotalCnt.matcher(page);
            while (matcherTotalCnt.find()) {
                if (matcherTotalCnt.groupCount() > 0) {
                    if (matcherTotalCnt.group(1) != null) {
                        caches.totalCnt = Integer.valueOf(matcherTotalCnt.group(1));
                    }
                }
            }
        } catch (Exception e) {
            // failed to parse cache count
            Log.w(cgSettings.tag, "cgeoBase.parseSearch: Failed to parse cache count");
        }

        if (thread != null && recaptchaChallenge != null) {
            if (thread.getText() == null) {
                thread.waitForUser();
            }

            recaptchaText = thread.getText();
        }

        if (cids.size() > 0 && (recaptchaChallenge == null || (recaptchaChallenge != null && StringUtils.isNotBlank(recaptchaText)))) {
            Log.i(cgSettings.tag, "Trying to get .loc for " + cids.size() + " caches");

            try {
                // get coordinates for parsed caches
                final Parameters params = new Parameters();
                params.put("__EVENTTARGET", "");
                params.put("__EVENTARGUMENT", "");
                if (ArrayUtils.isNotEmpty(caches.viewstates)) {
                    params.put("__VIEWSTATE", caches.viewstates[0]);
                    if (caches.viewstates.length > 1) {
                        for (int i = 1; i < caches.viewstates.length; i++) {
                            params.put("__VIEWSTATE" + i, caches.viewstates[i]);
                        }
                        params.put("__VIEWSTATEFIELDCOUNT", "" + caches.viewstates.length);
                    }
                }
                for (String cid : cids) {
                    params.put("CID", cid);
                }

                if (recaptchaChallenge != null && StringUtils.isNotBlank(recaptchaText)) {
                    params.put("recaptcha_challenge_field", recaptchaChallenge);
                    params.put("recaptcha_response_field", recaptchaText);
                }
                params.put("ctl00$ContentBody$uxDownloadLoc", "Download Waypoints");

                final String coordinates = getResponseData(postRequest("http://www.geocaching.com/seek/nearest.aspx", params));

                if (StringUtils.isNotBlank(coordinates)) {
                    if (coordinates.contains("You have not agreed to the license agreement. The license agreement is required before you can start downloading GPX or LOC files from Geocaching.com")) {
                        Log.i(cgSettings.tag, "User has not agreed to the license agreement. Can\'t download .loc file.");

                        caches.error = errorRetrieve.get(-7);

                        return caches;
                    }
                }

                LocParser.parseLoc(caches, coordinates);
            } catch (Exception e) {
                Log.e(cgSettings.tag, "cgBase.parseSearch.CIDs: " + e.toString());
            }
        }

        // get direction images
        if (settings.getLoadDirImg())
        {
            for (cgCache oneCache : caches.cacheList) {
                if (oneCache.coords == null && oneCache.directionImg != null) {
                    cgDirectionImg.getDrawable(oneCache.geocode, oneCache.directionImg);
                }
            }
        }

        // get ratings
        if (guids.size() > 0) {
            Log.i(cgSettings.tag, "Trying to get ratings for " + cids.size() + " caches");

            try {
                final Map<String, cgRating> ratings = getRating(guids, null);

                if (CollectionUtils.isNotEmpty(ratings)) {
                    // save found cache coordinates
                    for (cgCache oneCache : caches.cacheList) {
                        if (ratings.containsKey(oneCache.guid)) {
                            cgRating thisRating = ratings.get(oneCache.guid);

                            oneCache.rating = thisRating.rating;
                            oneCache.votes = thisRating.votes;
                            oneCache.myVote = thisRating.myVote;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(cgSettings.tag, "cgBase.parseSearch.GCvote: " + e.toString());
            }
        }

        return caches;
    }

    public static cgCacheWrap parseMapJSON(final String uri, final String data) {
        if (StringUtils.isEmpty(data)) {
            Log.e(cgSettings.tag, "cgeoBase.parseMapJSON: No page given");
            return null;
        }

        final cgCacheWrap caches = new cgCacheWrap();
        caches.url = uri;

        try {
            final JSONObject yoDawg = new JSONObject(data);
            final String json = yoDawg.getString("d");

            if (StringUtils.isBlank(json)) {
                Log.e(cgSettings.tag, "cgeoBase.parseMapJSON: No JSON inside JSON");
                return null;
            }

            final JSONObject dataJSON = new JSONObject(json);
            final JSONObject extra = dataJSON.getJSONObject("cs");
            if (extra != null && extra.length() > 0) {
                int count = extra.getInt("count");

                if (count > 0 && extra.has("cc")) {
                    final JSONArray cachesData = extra.getJSONArray("cc");
                    if (cachesData != null && cachesData.length() > 0) {
                        JSONObject oneCache = null;
                        for (int i = 0; i < count; i++) {
                            oneCache = cachesData.getJSONObject(i);
                            if (oneCache == null) {
                                break;
                            }

                            final cgCache cacheToAdd = new cgCache();
                            cacheToAdd.reliableLatLon = false;
                            cacheToAdd.geocode = oneCache.getString("gc");
                            cacheToAdd.coords = new Geopoint(oneCache.getDouble("lat"), oneCache.getDouble("lon"));
                            cacheToAdd.name = oneCache.getString("nn");
                            cacheToAdd.found = oneCache.getBoolean("f");
                            cacheToAdd.own = oneCache.getBoolean("o");
                            cacheToAdd.disabled = !oneCache.getBoolean("ia");
                            int ctid = oneCache.getInt("ctid");
                            if (ctid == 2) {
                                cacheToAdd.type = "traditional";
                            } else if (ctid == 3) {
                                cacheToAdd.type = "multi";
                            } else if (ctid == 4) {
                                cacheToAdd.type = "virtual";
                            } else if (ctid == 5) {
                                cacheToAdd.type = "letterbox";
                            } else if (ctid == 6) {
                                cacheToAdd.type = "event";
                            } else if (ctid == 8) {
                                cacheToAdd.type = "mystery";
                            } else if (ctid == 11) {
                                cacheToAdd.type = "webcam";
                            } else if (ctid == 13) {
                                cacheToAdd.type = "cito";
                            } else if (ctid == 137) {
                                cacheToAdd.type = "earth";
                            } else if (ctid == 453) {
                                cacheToAdd.type = "mega";
                            } else if (ctid == 1858) {
                                cacheToAdd.type = "wherigo";
                            } else if (ctid == 3653) {
                                cacheToAdd.type = "lost";
                            }

                            caches.cacheList.add(cacheToAdd);
                        }
                    }
                } else {
                    Log.w(cgSettings.tag, "There are no caches in viewport");
                }
                caches.totalCnt = caches.cacheList.size();
            }
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.parseMapJSON", e);
        }

        return caches;
    }

    public cgCacheWrap parseCache(String page, int reason) {
        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.parseCache: No page given");
            return null;
        }

        final cgCacheWrap caches = new cgCacheWrap();
        final cgCache cache = new cgCache();

        if (page.contains("Cache is Unpublished")) {
            caches.error = "cache was unpublished";
            return caches;
        }

        if (page.contains("Sorry, the owner of this listing has made it viewable to Premium Members only.")) {
            caches.error = "requested cache is for premium members only";
            return caches;
        }

        if (page.contains("has chosen to make this cache listing visible to Premium Members only.")) {
            caches.error = "requested cache is for premium members only";
            return caches;
        }

        cache.disabled = page.contains("<li>This cache is temporarily unavailable.");

        cache.archived = page.contains("<li>This cache has been archived,");

        cache.members = page.contains("<p class=\"Warning\">This is a Premium Member Only cache.</p>");

        cache.reason = reason;

        // cache geocode
        try {
            final Matcher matcherGeocode = patternGeocode.matcher(page);
            if (matcherGeocode.find() && matcherGeocode.groupCount() > 0) {
                cache.geocode = getMatch(matcherGeocode.group(1));
            }
        } catch (Exception e) {
            // failed to parse cache geocode
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache geocode");
        }

        // cache id
        try {
            final Matcher matcherCacheId = patternCacheId.matcher(page);
            if (matcherCacheId.find() && matcherCacheId.groupCount() > 0) {
                cache.cacheId = getMatch(matcherCacheId.group(1));
            }
        } catch (Exception e) {
            // failed to parse cache id
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache id");
        }

        // cache guid
        try {
            final Matcher matcherCacheGuid = patternCacheGuid.matcher(page);
            if (matcherCacheGuid.find() && matcherCacheGuid.groupCount() > 0) {
                cache.guid = getMatch(matcherCacheGuid.group(1));
            }
        } catch (Exception e) {
            // failed to parse cache guid
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache guid");
        }

        // name
        try {
            final Matcher matcherName = patternName.matcher(page);
            if (matcherName.find() && matcherName.groupCount() > 0) {
                cache.name = Html.fromHtml(matcherName.group(1)).toString();
            }
        } catch (Exception e) {
            // failed to parse cache name
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache name");
        }

        // owner real name
        try {
            final Matcher matcherOwnerReal = patternOwnerReal.matcher(page);
            if (matcherOwnerReal.find() && matcherOwnerReal.groupCount() > 0) {
                cache.ownerReal = URLDecoder.decode(matcherOwnerReal.group(1));
            }
        } catch (Exception e) {
            // failed to parse owner real name
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache owner real name");
        }

        final String username = settings.getUsername();
        if (cache.ownerReal != null && username != null && cache.ownerReal.equalsIgnoreCase(username)) {
            cache.own = true;
        }

        int pos = -1;
        String tableInside = page;

        pos = tableInside.indexOf("id=\"cacheDetails\"");
        if (pos == -1) {
            Log.e(cgSettings.tag, "cgeoBase.parseCache: ID \"cacheDetails\" not found on page");
            return null;
        }

        tableInside = tableInside.substring(pos);

        pos = tableInside.indexOf("<div class=\"CacheInformationTable\"");
        if (pos == -1) {
            Log.e(cgSettings.tag, "cgeoBase.parseCache: ID \"CacheInformationTable\" not found on page");
            return null;
        }

        tableInside = tableInside.substring(0, pos);

        if (StringUtils.isNotBlank(tableInside)) {
            // cache terrain
            try {
                final Matcher matcherTerrain = patternTerrain.matcher(tableInside);
                if (matcherTerrain.find() && matcherTerrain.groupCount() > 0) {
                    cache.terrain = new Float(Pattern.compile("_").matcher(matcherTerrain.group(1)).replaceAll("."));
                }
            } catch (Exception e) {
                // failed to parse terrain
                Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache terrain");
            }

            // cache difficulty
            try {
                final Matcher matcherDifficulty = patternDifficulty.matcher(tableInside);
                if (matcherDifficulty.find() && matcherDifficulty.groupCount() > 0) {
                    cache.difficulty = new Float(Pattern.compile("_").matcher(matcherDifficulty.group(1)).replaceAll("."));
                }
            } catch (Exception e) {
                // failed to parse difficulty
                Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache difficulty");
            }

            // owner
            try {
                final Matcher matcherOwner = patternOwner.matcher(tableInside);
                if (matcherOwner.find() && matcherOwner.groupCount() > 0) {
                    cache.owner = Html.fromHtml(matcherOwner.group(2)).toString();
                }
            } catch (Exception e) {
                // failed to parse owner
                Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache owner");
            }

            // hidden
            try {
                final Matcher matcherHidden = patternHidden.matcher(tableInside);
                if (matcherHidden.find() && matcherHidden.groupCount() > 0) {
                    cache.hidden = parseGcCustomDate(matcherHidden.group(1));
                }
            } catch (ParseException e) {
                // failed to parse cache hidden date
                Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache hidden date");
            }

            if (cache.hidden == null) {
                // event date
                try {
                    final Matcher matcherHiddenEvent = patternHiddenEvent.matcher(tableInside);
                    if (matcherHiddenEvent.find() && matcherHiddenEvent.groupCount() > 0) {
                        cache.hidden = parseGcCustomDate(matcherHiddenEvent.group(1));
                    }
                } catch (ParseException e) {
                    // failed to parse cache event date
                    Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache event date");
                }
            }

            // favourite
            try {
                final Matcher matcherFavourite = patternFavourite.matcher(tableInside);
                if (matcherFavourite.find() && matcherFavourite.groupCount() > 0) {
                    cache.favouriteCnt = Integer.parseInt(matcherFavourite.group(1));
                }
            } catch (Exception e) {
                // failed to parse favourite count
                Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse favourite count");
            }

            // cache size
            try {
                final Matcher matcherSize = patternSize.matcher(tableInside);
                if (matcherSize.find() && matcherSize.groupCount() > 0) {
                    cache.size = CacheSize.FIND_BY_ID.get(getMatch(matcherSize.group(1)).toLowerCase());
                }
            } catch (Exception e) {
                // failed to parse size
                Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache size");
            }
        }

        // cache found
        cache.found = patternFound.matcher(page).find() || patternFoundAlternative.matcher(page).find();

        // cache type
        try {
            final Matcher matcherType = patternType.matcher(page);
            if (matcherType.find() && matcherType.groupCount() > 0) {
                cache.type = cacheTypes.get(matcherType.group(1).toLowerCase());
            }
        } catch (Exception e) {
            // failed to parse type
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache type");
        }

        // on watchlist
        try {
            final Matcher matcher = patternOnWatchlist.matcher(page);
            cache.onWatchlist = matcher.find();
        } catch (Exception e) {
            // failed to parse watchlist state
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse watchlist state");
        }

        // latitude and logitude
        try {
            final Matcher matcherLatLon = patternLatLon.matcher(page);
            if (matcherLatLon.find() && matcherLatLon.groupCount() > 0) {
                cache.latlon = getMatch(matcherLatLon.group(2)); // first is <b>

                Map<String, Object> tmp = cgBase.parseLatlon(cache.latlon);
                if (tmp.size() > 0) {
                    cache.coords = new Geopoint((Double) tmp.get("latitude"), (Double) tmp.get("longitude"));
                    cache.latitudeString = (String) tmp.get("latitudeString");
                    cache.longitudeString = (String) tmp.get("longitudeString");
                    cache.reliableLatLon = true;
                }
                tmp = null;
            }
        } catch (Exception e) {
            // failed to parse latitude and/or longitude
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache coordinates");
        }

        // cache location
        try {
            final Matcher matcherLocation = patternLocation.matcher(page);
            if (matcherLocation.find() && matcherLocation.groupCount() > 0) {
                cache.location = getMatch(matcherLocation.group(1));
            }
        } catch (Exception e) {
            // failed to parse location
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache location");
        }

        // cache hint
        try {
            final Matcher matcherHint = patternHint.matcher(page);
            if (matcherHint.find() && matcherHint.group(1) != null) {
                // replace linebreak and paragraph tags
                String hint = Pattern.compile("<(br|p)[^>]*>").matcher(matcherHint.group(1)).replaceAll("\n");
                if (hint != null) {
                    cache.hint = hint.replaceAll(Pattern.quote("</p>"), "").trim();
                }
            }
        } catch (Exception e) {
            // failed to parse hint
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache hint");
        }

        checkFields(cache);
        /*
         * // short info debug
         * Log.d(cgSettings.tag, "gc-code: " + cache.geocode);
         * Log.d(cgSettings.tag, "id: " + cache.cacheid);
         * Log.d(cgSettings.tag, "guid: " + cache.guid);
         * Log.d(cgSettings.tag, "name: " + cache.name);
         * Log.d(cgSettings.tag, "terrain: " + cache.terrain);
         * Log.d(cgSettings.tag, "difficulty: " + cache.difficulty);
         * Log.d(cgSettings.tag, "owner: " + cache.owner);
         * Log.d(cgSettings.tag, "owner (real): " + cache.ownerReal);
         * Log.d(cgSettings.tag, "hidden: " + dateOutShort.format(cache.hidden));
         * Log.d(cgSettings.tag, "favorite: " + cache.favouriteCnt);
         * Log.d(cgSettings.tag, "size: " + cache.size);
         * if (cache.found) {
         * Log.d(cgSettings.tag, "found!");
         * } else {
         * Log.d(cgSettings.tag, "not found");
         * }
         * Log.d(cgSettings.tag, "type: " + cache.type);
         * Log.d(cgSettings.tag, "latitude: " + String.format("%.6f", cache.latitude));
         * Log.d(cgSettings.tag, "longitude: " + String.format("%.6f", cache.longitude));
         * Log.d(cgSettings.tag, "location: " + cache.location);
         * Log.d(cgSettings.tag, "hint: " + cache.hint);
         */

        // cache personal note
        try {
            final Matcher matcherPersonalNote = patternPersonalNote.matcher(page);
            if (matcherPersonalNote.find() && matcherPersonalNote.groupCount() > 0) {
                cache.personalNote = getMatch(matcherPersonalNote.group(1));
            }
        } catch (Exception e) {
            // failed to parse cache personal note
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache personal note");
        }

        // cache short description
        try {
            final Matcher matcherDescShort = patternDescShort.matcher(page);
            if (matcherDescShort.find() && matcherDescShort.groupCount() > 0) {
                cache.shortdesc = getMatch(matcherDescShort.group(1));
            }
        } catch (Exception e) {
            // failed to parse short description
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache short description");
        }

        // cache description
        try {
            final Matcher matcherDesc = patternDesc.matcher(page);
            if (matcherDesc.find() && matcherDesc.groupCount() > 0) {
                cache.description = getMatch(matcherDesc.group(1));
            }
        } catch (Exception e) {
            // failed to parse short description
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache description");
        }

        // cache attributes
        try {
            final Matcher matcherAttributes = patternAttributes.matcher(page);
            if (matcherAttributes.find() && matcherAttributes.groupCount() > 0) {
                final String attributesPre = matcherAttributes.group(1);
                final Matcher matcherAttributesInside = patternAttributesInside.matcher(attributesPre);

                while (matcherAttributesInside.find()) {
                    if (matcherAttributesInside.groupCount() > 1 && matcherAttributesInside.group(2).equalsIgnoreCase("blank") != true) {
                        if (cache.attributes == null) {
                            cache.attributes = new ArrayList<String>();
                        }
                        // by default, use the tooltip of the attribute
                        String attribute = matcherAttributesInside.group(2).toLowerCase();

                        // if the image name can be recognized, use the image name as attribute
                        String imageName = matcherAttributesInside.group(1).trim();
                        if (imageName.length() > 0) {
                            int start = imageName.lastIndexOf('/');
                            int end = imageName.lastIndexOf('.');
                            if (start >= 0 && end >= 0) {
                                attribute = imageName.substring(start + 1, end).replace('-', '_').toLowerCase();
                            }
                        }
                        cache.attributes.add(attribute);
                    }
                }
            }
        } catch (Exception e) {
            // failed to parse cache attributes
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache attributes");
        }

        // cache spoilers
        try {
            final Matcher matcherSpoilers = patternSpoilers.matcher(page);
            if (matcherSpoilers.find()) {
                final Matcher matcherSpoilersInside = patternSpoilersInside.matcher(matcherSpoilers.group(1));

                while (matcherSpoilersInside.find()) {
                    final cgImage spoiler = new cgImage();
                    spoiler.url = matcherSpoilersInside.group(1);

                    if (matcherSpoilersInside.group(2) != null) {
                        spoiler.title = matcherSpoilersInside.group(2);
                    }
                    if (matcherSpoilersInside.group(3) != null) {
                        spoiler.description = matcherSpoilersInside.group(3);
                    }

                    if (cache.spoilers == null) {
                        cache.spoilers = new ArrayList<cgImage>();
                    }
                    cache.spoilers.add(spoiler);
                }
            }
        } catch (Exception e) {
            // failed to parse cache spoilers
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache spoilers");
        }

        // cache inventory
        try {
            cache.inventoryItems = 0;

            final Matcher matcherInventory = patternInventory.matcher(page);
            if (matcherInventory.find()) {
                if (cache.inventory == null) {
                    cache.inventory = new ArrayList<cgTrackable>();
                }

                if (matcherInventory.groupCount() > 1) {
                    final String inventoryPre = matcherInventory.group(2);

                    if (StringUtils.isNotBlank(inventoryPre)) {
                        final Matcher matcherInventoryInside = patternInventoryInside.matcher(inventoryPre);

                        while (matcherInventoryInside.find()) {
                            if (matcherInventoryInside.groupCount() > 0) {
                                final cgTrackable inventoryItem = new cgTrackable();
                                inventoryItem.guid = matcherInventoryInside.group(1);
                                inventoryItem.name = matcherInventoryInside.group(2);

                                cache.inventory.add(inventoryItem);
                                cache.inventoryItems++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // failed to parse cache inventory
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache inventory (2)");
        }

        // cache logs counts
        try
        {
            final Matcher matcherLogCounts = patternCountLogs.matcher(page);

            if (matcherLogCounts.find())
            {
                final Matcher matcherLog = patternCountLog.matcher(matcherLogCounts.group(1));

                while (matcherLog.find())
                {
                    String typeStr = matcherLog.group(1);
                    String countStr = matcherLog.group(2).replaceAll("[.,]", "");

                    if (StringUtils.isNotBlank(typeStr)
                            && logTypes.containsKey(typeStr.toLowerCase())
                            && StringUtils.isNotBlank(countStr))
                    {
                        cache.logCounts.put(logTypes.get(typeStr.toLowerCase()), Integer.parseInt(countStr));
                    }
                }
            }
        } catch (Exception e)
        {
            // failed to parse logs
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache log count");
        }

        // cache logs
        loadLogsFromDetails(page, cache);

        int wpBegin = 0;
        int wpEnd = 0;

        wpBegin = page.indexOf("<table class=\"Table\" id=\"ctl00_ContentBody_Waypoints\">");
        if (wpBegin != -1) { // parse waypoints
            final Pattern patternWpType = Pattern.compile("\\/wpttypes\\/sm\\/(.+)\\.jpg", Pattern.CASE_INSENSITIVE);
            final Pattern patternWpPrefixOrLookupOrLatlon = Pattern.compile(">([^<]*<[^>]+>)?([^<]+)(<[^>]+>[^<]*)?<\\/td>", Pattern.CASE_INSENSITIVE);
            final Pattern patternWpName = Pattern.compile(">[^<]*<a[^>]+>([^<]*)<\\/a>", Pattern.CASE_INSENSITIVE);
            final Pattern patternWpNote = Pattern.compile("colspan=\"6\">(.*)<\\/td>", Pattern.CASE_INSENSITIVE);

            String wpList = page.substring(wpBegin);

            wpEnd = wpList.indexOf("</p>");
            if (wpEnd > -1 && wpEnd <= wpList.length()) {
                wpList = wpList.substring(0, wpEnd);
            }

            if (!wpList.contains("No additional waypoints to display.")) {
                wpEnd = wpList.indexOf("</table>");
                wpList = wpList.substring(0, wpEnd);

                wpBegin = wpList.indexOf("<tbody>");
                wpEnd = wpList.indexOf("</tbody>");
                if (wpBegin >= 0 && wpEnd >= 0 && wpEnd <= wpList.length()) {
                    wpList = wpList.substring(wpBegin + 7, wpEnd);
                }

                final String[] wpItems = wpList.split("<tr");

                String[] wp;
                for (int j = 1; j < wpItems.length; j++) {
                    final cgWaypoint waypoint = new cgWaypoint();

                    wp = wpItems[j].split("<td");

                    // waypoint type
                    try {
                        final Matcher matcherWpType = patternWpType.matcher(wp[3]);
                        if (matcherWpType.find() && matcherWpType.groupCount() > 0) {
                            waypoint.type = matcherWpType.group(1).trim();
                        }
                    } catch (Exception e) {
                        // failed to parse type
                        Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint type");
                    }

                    // waypoint prefix
                    try {
                        final Matcher matcherWpPrefix = patternWpPrefixOrLookupOrLatlon.matcher(wp[4]);
                        if (matcherWpPrefix.find() && matcherWpPrefix.groupCount() > 1) {
                            waypoint.setPrefix(matcherWpPrefix.group(2).trim());
                        }
                    } catch (Exception e) {
                        // failed to parse prefix
                        Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint prefix");
                    }

                    // waypoint lookup
                    try {
                        final Matcher matcherWpLookup = patternWpPrefixOrLookupOrLatlon.matcher(wp[5]);
                        if (matcherWpLookup.find() && matcherWpLookup.groupCount() > 1) {
                            waypoint.lookup = matcherWpLookup.group(2).trim();
                        }
                    } catch (Exception e) {
                        // failed to parse lookup
                        Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint lookup");
                    }

                    // waypoint name
                    try {
                        final Matcher matcherWpName = patternWpName.matcher(wp[6]);
                        while (matcherWpName.find()) {
                            if (matcherWpName.groupCount() > 0) {
                                waypoint.name = matcherWpName.group(1);
                                if (StringUtils.isNotBlank(waypoint.name)) {
                                    waypoint.name = waypoint.name.trim();
                                }
                            }
                            if (matcherWpName.find() && matcherWpName.groupCount() > 0) {
                                waypoint.name = matcherWpName.group(1).trim();
                            }
                        }
                    } catch (Exception e) {
                        // failed to parse name
                        Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint name");
                    }

                    // waypoint latitude and logitude
                    try {
                        final Matcher matcherWpLatLon = patternWpPrefixOrLookupOrLatlon.matcher(wp[7]);
                        if (matcherWpLatLon.find() && matcherWpLatLon.groupCount() > 1) {
                            waypoint.latlon = Html.fromHtml(matcherWpLatLon.group(2)).toString();

                            final Map<String, Object> tmp = cgBase.parseLatlon(waypoint.latlon);
                            if (tmp.size() > 0) {
                                waypoint.coords = new Geopoint((Double) tmp.get("latitude"), (Double) tmp.get("longitude"));
                                waypoint.latitudeString = (String) tmp.get("latitudeString");
                                waypoint.longitudeString = (String) tmp.get("longitudeString");
                            }
                        }
                    } catch (Exception e) {
                        // failed to parse latitude and/or longitude
                        Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint coordinates");
                    }

                    j++;
                    if (wpItems.length > j) {
                        wp = wpItems[j].split("<td");
                    }

                    // waypoint note
                    try {
                        final Matcher matcherWpNote = patternWpNote.matcher(wp[3]);
                        if (matcherWpNote.find() && matcherWpNote.groupCount() > 0) {
                            waypoint.note = matcherWpNote.group(1).trim();
                        }
                    } catch (Exception e) {
                        // failed to parse note
                        Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse waypoint note");
                    }

                    if (cache.waypoints == null) {
                        cache.waypoints = new ArrayList<cgWaypoint>();
                    }
                    cache.waypoints.add(waypoint);
                }
            }
        }

        if (cache.coords != null) {
            cache.elevation = getElevation(cache.coords);
        }

        final cgRating rating = getRating(cache.guid, cache.geocode);
        if (rating != null) {
            cache.rating = rating.rating;
            cache.votes = rating.votes;
            cache.myVote = rating.myVote;
        }

        cache.updated = System.currentTimeMillis();
        cache.detailedUpdate = System.currentTimeMillis();
        cache.detailed = true;
        caches.cacheList.add(cache);

        return caches;
    }

    /**
     * Load logs from a cache details page.
     *
     * @param page
     *            the text of the details page
     * @param cache
     *            the cache object to put the logs in
     */
    private void loadLogsFromDetails(final String page, final cgCache cache) {
        final Matcher userTokenMatcher = patternUserToken.matcher(page);
        if (!userTokenMatcher.find()) {
            Log.e(cgSettings.tag, "cgBase.loadLogsFromDetails: unable to extract userToken");
            return;
        }

        final String userToken = userTokenMatcher.group(1);
        final HashMap<String, String> params = new HashMap<String, String>();
        params.put("tkn", userToken);
        params.put("idx", "1");
        params.put("num", "35");
        params.put("decrypt", "true");
        final HttpResponse response = request("http://www.geocaching.com/seek/geocache.logbook", params, false, false, false);
        if (response == null) {
            Log.e(cgSettings.tag, "cgBase.loadLogsFromDetails: cannot log logs, response is null");
            return;
        }
        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            Log.e(cgSettings.tag, "cgBase.loadLogsFromDetails: error " + statusCode + " when requesting log information");
            return;
        }

        try {
            final JSONObject resp = new JSONObject(cgBase.getResponseData(response));
            if (!resp.getString("status").equals("success")) {
                Log.e(cgSettings.tag, "cgBase.loadLogsFromDetails: status is " + resp.getString("status"));
                return;
            }

            final JSONArray data = resp.getJSONArray("data");

            for (int index = 0; index < data.length(); index++) {
                final JSONObject entry = data.getJSONObject(index);
                final cgLog logDone = new cgLog();

                // FIXME: use the "LogType" field instead of the "LogTypeImage" one.
                final String logIconNameExt = entry.optString("LogTypeImage", ".gif");
                final String logIconName = logIconNameExt.substring(0, logIconNameExt.length() - 4);
                if (logTypes.containsKey(logIconName)) {
                    logDone.type = logTypes.get(logIconName);
                } else {
                    logDone.type = logTypes.get("icon_note");
                }

                try {
                    logDone.date = parseGcCustomDate(entry.getString("Visited")).getTime();
                } catch (ParseException e) {
                    Log.e(cgSettings.tag, "cgBase.loadLogsFromDetails: failed to parse log date.");
                }

                logDone.author = entry.getString("UserName");
                logDone.found = entry.getInt("GeocacheFindCount");
                logDone.log = entry.getString("LogText");

                final JSONArray images = entry.getJSONArray("Images");
                for (int i = 0; i < images.length(); i++) {
                    final JSONObject image = images.getJSONObject(i);
                    final cgImage logImage = new cgImage();
                    logImage.url = "http://img.geocaching.com/cache/log/" + image.getString("FileName");
                    logImage.title = image.getString("Name");
                    if (logDone.logImages == null) {
                        logDone.logImages = new ArrayList<cgImage>();
                    }
                    logDone.logImages.add(logImage);
                }

                if (null == cache.logs) {
                    cache.logs = new ArrayList<cgLog>();
                }
                cache.logs.add(logDone);
            }
        } catch (JSONException e) {
            // failed to parse logs
            Log.w(cgSettings.tag, "cgBase.loadLogsFromDetails: Failed to parse cache logs", e);
        }
    }

    private static void checkFields(cgCache cache) {
        if (StringUtils.isBlank(cache.geocode)) {
            Log.w(cgSettings.tag, "cgBase.loadLogsFromDetails: geo code not parsed correctly");
        }
        if (StringUtils.isBlank(cache.name)) {
            Log.w(cgSettings.tag, "name not parsed correctly");
        }
        if (StringUtils.isBlank(cache.guid)) {
            Log.w(cgSettings.tag, "guid not parsed correctly");
        }
        if (cache.terrain == null || cache.terrain == 0.0) {
            Log.w(cgSettings.tag, "terrain not parsed correctly");
        }
        if (cache.difficulty == null || cache.difficulty == 0.0) {
            Log.w(cgSettings.tag, "difficulty not parsed correctly");
        }
        if (StringUtils.isBlank(cache.owner)) {
            Log.w(cgSettings.tag, "owner not parsed correctly");
        }
        if (StringUtils.isBlank(cache.ownerReal)) {
            Log.w(cgSettings.tag, "owner real not parsed correctly");
        }
        if (cache.hidden == null) {
            Log.w(cgSettings.tag, "hidden not parsed correctly");
        }
        if (cache.favouriteCnt == null) {
            Log.w(cgSettings.tag, "favoriteCount not parsed correctly");
        }
        if (cache.size == null) {
            Log.w(cgSettings.tag, "size not parsed correctly");
        }
        if (StringUtils.isBlank(cache.type)) {
            Log.w(cgSettings.tag, "type not parsed correctly");
        }
        if (cache.coords == null) {
            Log.w(cgSettings.tag, "coordinates not parsed correctly");
        }
        if (StringUtils.isBlank(cache.location)) {
            Log.w(cgSettings.tag, "location not parsed correctly");
        }
    }

    private static String getMatch(String match) {
        // creating a new String via String constructor is necessary here!!
        return new String(match.trim());
        // Java copies the whole page String, when matching with regular expressions
        // later this would block the garbage collector, as we only need tiny parts of the page
        // see http://developer.android.com/reference/java/lang/String.html#backing_array

        // And BTW: You cannot even see that effect in the debugger, but must use a separate memory profiler!
    }

    public Date parseGcCustomDate(String input)
            throws ParseException
    {
        if (StringUtils.isBlank(input))
        {
            throw new ParseException("Input is null", 0);
        }

        input = input.trim();

        if (null != settings
                //&& null != settings.getGcCustomDate()
                && gcCustomDateFormats.containsKey(settings.getGcCustomDate()))
        {
            try
            {
                return gcCustomDateFormats.get(settings.getGcCustomDate()).parse(input);
            } catch (ParseException e) {
            }
        }

        for (SimpleDateFormat format : gcCustomDateFormats.values())
        {
            try
            {
                return format.parse(input);
            } catch (ParseException e) {
            }
        }

        throw new ParseException("No matching pattern", 0);
    }

    public void detectGcCustomDate()
    {
        final String result = getResponseData(request("http://www.geocaching.com/account/ManagePreferences.aspx", null, false, false, false));

        if (null == result) {
            Log.w(cgSettings.tag, "cgeoBase.detectGcCustomDate: result is null");
            return;
        }

        final Pattern pattern = Pattern.compile("<option selected=\"selected\" value=\"([ /Mdy-]+)\">", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(result);

        if (matcher.find())
        {
            settings.setGcCustomDate(matcher.group(1));
        }
    }

    public cgRating getRating(String guid, String geocode) {
        List<String> guids = null;
        List<String> geocodes = null;

        if (StringUtils.isNotBlank(guid)) {
            guids = new ArrayList<String>();
            guids.add(guid);
        } else if (StringUtils.isNotBlank(geocode)) {
            geocodes = new ArrayList<String>();
            geocodes.add(geocode);
        } else {
            return null;
        }

        final Map<String, cgRating> ratings = getRating(guids, geocodes);
        if (ratings != null) {
            for (Entry<String, cgRating> entry : ratings.entrySet()) {
                return entry.getValue();
            }
        }

        return null;
    }

    public Map<String, cgRating> getRating(List<String> guids, List<String> geocodes) {
        if (guids == null && geocodes == null) {
            return null;
        }

        final Map<String, cgRating> ratings = new HashMap<String, cgRating>();

        try {
            final Map<String, String> params = new HashMap<String, String>();
            if (settings.isLogin()) {
                final Map<String, String> login = settings.getGCvoteLogin();
                if (login != null) {
                    params.put("userName", login.get("username"));
                    params.put("password", login.get("password"));
                }
            }
            if (CollectionUtils.isNotEmpty(guids)) {
                params.put("cacheIds", StringUtils.join(guids.toArray(), ','));
            } else {
                params.put("waypoints", StringUtils.join(geocodes.toArray(), ','));
            }
            params.put("version", "cgeo");
            final String votes = getResponseData(request("http://gcvote.com/getVotes.php", params, false, false, false));
            if (votes == null) {
                return null;
            }

            final Pattern patternLogIn = Pattern.compile("loggedIn='([^']+)'", Pattern.CASE_INSENSITIVE);
            final Pattern patternGuid = Pattern.compile("cacheId='([^']+)'", Pattern.CASE_INSENSITIVE);
            final Pattern patternRating = Pattern.compile("voteAvg='([0-9.]+)'", Pattern.CASE_INSENSITIVE);
            final Pattern patternVotes = Pattern.compile("voteCnt='([0-9]+)'", Pattern.CASE_INSENSITIVE);
            final Pattern patternVote = Pattern.compile("voteUser='([0-9.]+)'", Pattern.CASE_INSENSITIVE);

            String voteData = null;
            final Pattern patternVoteElement = Pattern.compile("<vote ([^>]+)>", Pattern.CASE_INSENSITIVE);
            final Matcher matcherVoteElement = patternVoteElement.matcher(votes);
            while (matcherVoteElement.find()) {
                if (matcherVoteElement.groupCount() > 0) {
                    voteData = matcherVoteElement.group(1);
                }

                if (voteData == null) {
                    continue;
                }

                String guid = null;
                cgRating rating = new cgRating();
                boolean loggedIn = false;

                try {
                    final Matcher matcherGuid = patternGuid.matcher(voteData);
                    if (matcherGuid.find()) {
                        if (matcherGuid.groupCount() > 0) {
                            guid = (String) matcherGuid.group(1);
                        }
                    }
                } catch (Exception e) {
                    Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse guid");
                }

                try {
                    final Matcher matcherLoggedIn = patternLogIn.matcher(votes);
                    if (matcherLoggedIn.find()) {
                        if (matcherLoggedIn.groupCount() > 0) {
                            if (matcherLoggedIn.group(1).equalsIgnoreCase("true")) {
                                loggedIn = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse loggedIn");
                }

                try {
                    final Matcher matcherRating = patternRating.matcher(voteData);
                    if (matcherRating.find()) {
                        if (matcherRating.groupCount() > 0) {
                            rating.rating = Float.parseFloat(matcherRating.group(1));
                        }
                    }
                } catch (Exception e) {
                    Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse rating");
                }

                try {
                    final Matcher matcherVotes = patternVotes.matcher(voteData);
                    if (matcherVotes.find()) {
                        if (matcherVotes.groupCount() > 0) {
                            rating.votes = Integer.parseInt(matcherVotes.group(1));
                        }
                    }
                } catch (Exception e) {
                    Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse vote count");
                }

                if (loggedIn) {
                    try {
                        final Matcher matcherVote = patternVote.matcher(voteData);
                        if (matcherVote.find()) {
                            if (matcherVote.groupCount() > 0) {
                                rating.myVote = Float.parseFloat(matcherVote.group(1));
                            }
                        }
                    } catch (Exception e) {
                        Log.w(cgSettings.tag, "cgBase.getRating: Failed to parse user's vote");
                    }
                }

                if (StringUtils.isNotBlank(guid)) {
                    ratings.put(guid, rating);
                }
            }
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.getRating: " + e.toString());
        }

        return ratings;
    }

    public cgTrackable parseTrackable(String page) {
        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.parseTrackable: No page given");
            return null;
        }

        final cgTrackable trackable = new cgTrackable();

        // trackable geocode
        try {
            final Matcher matcherGeocode = PATTERN_TRACKABLE_Geocode.matcher(page);
            if (matcherGeocode.find() && matcherGeocode.groupCount() > 0) {
                trackable.geocode = matcherGeocode.group(1).toUpperCase();
            }
        } catch (Exception e) {
            // failed to parse trackable geocode
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable geocode");
        }

        // trackable id
        try {
            final Matcher matcherTrackableId = PATTERN_TRACKABLE_TrackableId.matcher(page);
            if (matcherTrackableId.find() && matcherTrackableId.groupCount() > 0) {
                trackable.guid = matcherTrackableId.group(1);
            }
        } catch (Exception e) {
            // failed to parse trackable id
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable id");
        }

        // trackable icon
        try {
            final Matcher matcherTrackableIcon = PATTERN_TRACKABLE_Icon.matcher(page);
            if (matcherTrackableIcon.find() && matcherTrackableIcon.groupCount() > 0) {
                trackable.iconUrl = matcherTrackableIcon.group(1);
            }
        } catch (Exception e) {
            // failed to parse trackable icon
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable icon");
        }

        // trackable name
        try {
            final Matcher matcherName = PATTERN_TRACKABLE_Name.matcher(page);
            if (matcherName.find() && matcherName.groupCount() > 1) {
                trackable.name = matcherName.group(2);
            }
        } catch (Exception e) {
            // failed to parse trackable name
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable name");
        }

        // trackable type
        if (StringUtils.isNotBlank(trackable.name)) {
            try {
                final Matcher matcherType = PATTERN_TRACKABLE_Type.matcher(page);
                if (matcherType.find() && matcherType.groupCount() > 0) {
                    trackable.type = matcherType.group(1);
                }
            } catch (Exception e) {
                // failed to parse trackable type
                Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable type");
            }
        }

        // trackable owner name
        try {
            final Matcher matcherOwner = PATTERN_TRACKABLE_Owner.matcher(page);
            if (matcherOwner.find() && matcherOwner.groupCount() > 0) {
                trackable.ownerGuid = matcherOwner.group(1);
                trackable.owner = matcherOwner.group(2);
            }
        } catch (Exception e) {
            // failed to parse trackable owner name
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable owner name");
        }

        // trackable origin
        try {
            final Matcher matcherOrigin = PATTERN_TRACKABLE_Origin.matcher(page);
            if (matcherOrigin.find() && matcherOrigin.groupCount() > 0) {
                trackable.origin = matcherOrigin.group(1);
            }
        } catch (Exception e) {
            // failed to parse trackable origin
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable origin");
        }

        // trackable spotted
        try {
            final Matcher matcherSpottedCache = PATTERN_TRACKABLE_SpottedCache.matcher(page);
            if (matcherSpottedCache.find() && matcherSpottedCache.groupCount() > 0) {
                trackable.spottedGuid = matcherSpottedCache.group(1);
                trackable.spottedName = matcherSpottedCache.group(2);
                trackable.spottedType = cgTrackable.SPOTTED_CACHE;
            }

            final Matcher matcherSpottedUser = PATTERN_TRACKABLE_SpottedUser.matcher(page);
            if (matcherSpottedUser.find() && matcherSpottedUser.groupCount() > 0) {
                trackable.spottedGuid = matcherSpottedUser.group(1);
                trackable.spottedName = matcherSpottedUser.group(2);
                trackable.spottedType = cgTrackable.SPOTTED_USER;
            }

            final Matcher matcherSpottedUnknown = PATTERN_TRACKABLE_SpottedUnknown.matcher(page);
            if (matcherSpottedUnknown.find()) {
                trackable.spottedType = cgTrackable.SPOTTED_UNKNOWN;
            }

            final Matcher matcherSpottedOwner = PATTERN_TRACKABLE_SpottedOwner.matcher(page);
            if (matcherSpottedOwner.find()) {
                trackable.spottedType = cgTrackable.SPOTTED_OWNER;
            }
        } catch (Exception e) {
            // failed to parse trackable last known place
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable last known place");
        }

        // released
        try {
            final Matcher matcherReleased = PATTERN_TRACKABLE_Released.matcher(page);
            if (matcherReleased.find() && matcherReleased.groupCount() > 0 && matcherReleased.group(1) != null) {
                try {
                    if (trackable.released == null) {
                        trackable.released = dateTbIn1.parse(matcherReleased.group(1));
                    }
                } catch (Exception e) {
                    //
                }

                try {
                    if (trackable.released == null) {
                        trackable.released = dateTbIn2.parse(matcherReleased.group(1));
                    }
                } catch (Exception e) {
                    //
                }
            }
        } catch (Exception e) {
            // failed to parse trackable released date
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable released date");
        }

        // trackable distance
        try {
            final Matcher matcherDistance = PATTERN_TRACKABLE_Distance.matcher(page);
            if (matcherDistance.find() && matcherDistance.groupCount() > 0) {
                try {
                    trackable.distance = DistanceParser.parseDistance(matcherDistance.group(1), settings.units);
                } catch (NumberFormatException e) {
                    trackable.distance = null;
                    throw e;
                }
            }
        } catch (Exception e) {
            // failed to parse trackable distance
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable distance");
        }

        // trackable goal
        try {
            final Matcher matcherGoal = PATTERN_TRACKABLE_Goal.matcher(page);
            if (matcherGoal.find() && matcherGoal.groupCount() > 0) {
                trackable.goal = matcherGoal.group(1);
            }
        } catch (Exception e) {
            // failed to parse trackable goal
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable goal");
        }

        // trackable details & image
        try {
            final Matcher matcherDetailsImage = PATTERN_TRACKABLE_DetailsImage.matcher(page);
            if (matcherDetailsImage.find() && matcherDetailsImage.groupCount() > 0) {
                final String image = matcherDetailsImage.group(3);
                final String details = matcherDetailsImage.group(4);

                if (image != null) {
                    trackable.image = image;
                }
                if (details != null) {
                    trackable.details = details;
                }
            }
        } catch (Exception e) {
            // failed to parse trackable details & image
            Log.w(cgSettings.tag, "cgeoBase.parseTrackable: Failed to parse trackable details & image");
        }

        // trackable logs
        try
        {
            final Matcher matcherLogs = PATTERN_TRACKABLE_Log.matcher(page);
            /*
             * 1. Type (img)
             * 2. Date
             * 3. Author
             * 4. Cache-GUID
             * 5. Cache-name
             * 6. Logtext
             */
            while (matcherLogs.find())
            {
                final cgLog logDone = new cgLog();

                if (logTypes.containsKey(matcherLogs.group(1).toLowerCase()))
                {
                    logDone.type = logTypes.get(matcherLogs.group(1).toLowerCase());
                }
                else
                {
                    logDone.type = logTypes.get("icon_note");
                }

                logDone.author = Html.fromHtml(matcherLogs.group(3)).toString();

                try
                {
                    logDone.date = parseGcCustomDate(matcherLogs.group(2)).getTime();
                } catch (ParseException e) {
                }

                logDone.log = matcherLogs.group(6).trim();

                if (matcherLogs.group(4) != null && matcherLogs.group(5) != null)
                {
                    logDone.cacheGuid = matcherLogs.group(4);
                    logDone.cacheName = matcherLogs.group(5);
                }

                trackable.logs.add(logDone);
            }
        } catch (Exception e) {
            // failed to parse logs
            Log.w(cgSettings.tag, "cgeoBase.parseCache: Failed to parse cache logs");
        }

        app.saveTrackable(trackable);

        return trackable;
    }

    public static List<Integer> parseTypes(String page) {
        if (StringUtils.isEmpty(page)) {
            return null;
        }

        final List<Integer> types = new ArrayList<Integer>();

        final Pattern typeBoxPattern = Pattern.compile("<select name=\"ctl00\\$ContentBody\\$LogBookPanel1\\$ddLogType\" id=\"ctl00_ContentBody_LogBookPanel1_ddLogType\"[^>]*>"
                + "(([^<]*<option[^>]*>[^<]+</option>)+)[^<]*</select>", Pattern.CASE_INSENSITIVE);
        final Matcher typeBoxMatcher = typeBoxPattern.matcher(page);
        String typesText = null;
        if (typeBoxMatcher.find()) {
            if (typeBoxMatcher.groupCount() > 0) {
                typesText = typeBoxMatcher.group(1);
            }
        }

        if (typesText != null) {
            final Pattern typePattern = Pattern.compile("<option( selected=\"selected\")? value=\"(\\d+)\">[^<]+</option>", Pattern.CASE_INSENSITIVE);
            final Matcher typeMatcher = typePattern.matcher(typesText);
            while (typeMatcher.find()) {
                if (typeMatcher.groupCount() > 1) {
                    final int type = Integer.parseInt(typeMatcher.group(2));

                    if (type > 0) {
                        types.add(type);
                    }
                }
            }
        }

        return types;
    }

    public static List<cgTrackableLog> parseTrackableLog(String page) {
        if (StringUtils.isEmpty(page)) {
            return null;
        }

        final List<cgTrackableLog> trackables = new ArrayList<cgTrackableLog>();

        int startPos = -1;
        int endPos = -1;

        startPos = page.indexOf("<table id=\"tblTravelBugs\"");
        if (startPos == -1) {
            Log.e(cgSettings.tag, "cgeoBase.parseTrackableLog: ID \"tblTravelBugs\" not found on page");
            return null;
        }

        page = page.substring(startPos); // cut on <table

        endPos = page.indexOf("</table>");
        if (endPos == -1) {
            Log.e(cgSettings.tag, "cgeoBase.parseTrackableLog: end of ID \"tblTravelBugs\" not found on page");
            return null;
        }

        page = page.substring(0, endPos); // cut on </table>

        startPos = page.indexOf("<tbody>");
        if (startPos == -1) {
            Log.e(cgSettings.tag, "cgeoBase.parseTrackableLog: tbody not found on page");
            return null;
        }

        page = page.substring(startPos); // cut on <tbody>

        endPos = page.indexOf("</tbody>");
        if (endPos == -1) {
            Log.e(cgSettings.tag, "cgeoBase.parseTrackableLog: end of tbody not found on page");
            return null;
        }

        page = page.substring(0, endPos); // cut on </tbody>

        final Pattern trackablePattern = Pattern.compile("<tr id=\"ctl00_ContentBody_LogBookPanel1_uxTrackables_repTravelBugs_ctl[0-9]+_row\"[^>]*>"
                + "[^<]*<td>[^<]*<a href=\"[^\"]+\">([A-Z0-9]+)</a>[^<]*</td>[^<]*<td>([^<]+)</td>[^<]*<td>"
                + "[^<]*<select name=\"ctl00\\$ContentBody\\$LogBookPanel1\\$uxTrackables\\$repTravelBugs\\$ctl([0-9]+)\\$ddlAction\"[^>]*>"
                + "([^<]*<option value=\"([0-9]+)(_[a-z]+)?\">[^<]+</option>)+"
                + "[^<]*</select>[^<]*</td>[^<]*</tr>", Pattern.CASE_INSENSITIVE);
        final Matcher trackableMatcher = trackablePattern.matcher(page);
        while (trackableMatcher.find()) {
            if (trackableMatcher.groupCount() > 0) {
                final cgTrackableLog trackable = new cgTrackableLog();

                if (trackableMatcher.group(1) != null) {
                    trackable.trackCode = trackableMatcher.group(1);
                } else {
                    continue;
                }
                if (trackableMatcher.group(2) != null) {
                    trackable.name = Html.fromHtml(trackableMatcher.group(2)).toString();
                } else {
                    continue;
                }
                if (trackableMatcher.group(3) != null) {
                    trackable.ctl = Integer.valueOf(trackableMatcher.group(3));
                } else {
                    continue;
                }
                if (trackableMatcher.group(5) != null) {
                    trackable.id = Integer.valueOf(trackableMatcher.group(5));
                } else {
                    continue;
                }

                Log.i(cgSettings.tag, "Trackable in inventory (#" + trackable.ctl + "/" + trackable.id + "): " + trackable.trackCode + " - " + trackable.name);

                trackables.add(trackable);
            }
        }

        return trackables;
    }

    public static String stripParagraphs(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }

        final Pattern patternP = Pattern.compile("(<p>|</p>|<br \\/>|<br>)", Pattern.CASE_INSENSITIVE);
        final Pattern patternP2 = Pattern.compile("([ ]+)", Pattern.CASE_INSENSITIVE);
        final Matcher matcherP = patternP.matcher(text);
        final Matcher matcherP2 = patternP2.matcher(text);

        matcherP.replaceAll(" ");
        matcherP2.replaceAll(" ");

        return text.trim();
    }

    public static String stripTags(String text) {
        if (StringUtils.isBlank(text)) {
            return "";
        }

        final Pattern patternP = Pattern.compile("(<[^>]+>)", Pattern.CASE_INSENSITIVE);
        final Matcher matcherP = patternP.matcher(text);

        matcherP.replaceAll(" ");

        return text.trim();
    }

    public String getHumanDistance(Float distance) {
        if (distance == null) {
            return "?";
        }

        if (settings.units == cgSettings.unitsImperial) {
            distance /= miles2km;
            if (distance > 100) {
                return String.format(Locale.getDefault(), "%.0f", Double.valueOf(Math.round(distance))) + " mi";
            } else if (distance > 0.5) {
                return String.format(Locale.getDefault(), "%.1f", Double.valueOf(Math.round(distance * 10.0) / 10.0)) + " mi";
            } else if (distance > 0.1) {
                return String.format(Locale.getDefault(), "%.2f", Double.valueOf(Math.round(distance * 100.0) / 100.0)) + " mi";
            } else if (distance > 0.05) {
                return String.format(Locale.getDefault(), "%.0f", Double.valueOf(Math.round(distance * 5280.0))) + " ft";
            } else if (distance > 0.01) {
                return String.format(Locale.getDefault(), "%.1f", Double.valueOf(Math.round(distance * 5280 * 10.0) / 10.0)) + " ft";
            } else {
                return String.format(Locale.getDefault(), "%.2f", Double.valueOf(Math.round(distance * 5280 * 100.0) / 100.0)) + " ft";
            }
        } else {
            if (distance > 100) {
                return String.format(Locale.getDefault(), "%.0f", Double.valueOf(Math.round(distance))) + " km";
            } else if (distance > 10) {
                return String.format(Locale.getDefault(), "%.1f", Double.valueOf(Math.round(distance * 10.0) / 10.0)) + " km";
            } else if (distance > 1) {
                return String.format(Locale.getDefault(), "%.2f", Double.valueOf(Math.round(distance * 100.0) / 100.0)) + " km";
            } else if (distance > 0.1) {
                return String.format(Locale.getDefault(), "%.0f", Double.valueOf(Math.round(distance * 1000.0))) + " m";
            } else if (distance > 0.01) {
                return String.format(Locale.getDefault(), "%.1f", Double.valueOf(Math.round(distance * 1000.0 * 10.0) / 10.0)) + " m";
            } else {
                return String.format(Locale.getDefault(), "%.2f", Double.valueOf(Math.round(distance * 1000.0 * 100.0) / 100.0)) + " m";
            }
        }
    }

    public String getHumanSpeed(float speed) {
        double kph = speed * 3.6;
        String unit = "km/h";

        if (this.settings.units == cgSettings.unitsImperial) {
            kph /= miles2km;
            unit = "mph";
        }

        if (kph < 10.0) {
            return String.format(Locale.getDefault(), "%.1f", Double.valueOf((Math.round(kph * 10.0) / 10.0))) + " " + unit;
        } else {
            return String.format(Locale.getDefault(), "%.0f", Double.valueOf(Math.round(kph))) + " " + unit;
        }
    }

    public static Map<String, Object> parseLatlon(String latlon) {
        final Map<String, Object> result = new HashMap<String, Object>();
        final Pattern patternLatlon = Pattern.compile("([NS])[^\\d]*(\\d+)[^°]*° (\\d+)\\.(\\d+) ([WE])[^\\d]*(\\d+)[^°]*° (\\d+)\\.(\\d+)", Pattern.CASE_INSENSITIVE);
        final Matcher matcherLatlon = patternLatlon.matcher(latlon);

        while (matcherLatlon.find()) {
            if (matcherLatlon.groupCount() > 0) {
                result.put("latitudeString", (String) (matcherLatlon.group(1) + " " + matcherLatlon.group(2) + "° " + matcherLatlon.group(3) + "." + matcherLatlon.group(4)));
                result.put("longitudeString", (String) (matcherLatlon.group(5) + " " + matcherLatlon.group(6) + "° " + matcherLatlon.group(7) + "." + matcherLatlon.group(8)));
                int latNegative = -1;
                int lonNegative = -1;
                if (matcherLatlon.group(1).equalsIgnoreCase("N")) {
                    latNegative = 1;
                }
                if (matcherLatlon.group(5).equalsIgnoreCase("E")) {
                    lonNegative = 1;
                }
                result.put("latitude", Double.valueOf(latNegative * (Float.valueOf(matcherLatlon.group(2)) + Float.valueOf(matcherLatlon.group(3) + "." + matcherLatlon.group(4)) / 60)));
                result.put("longitude", Double.valueOf(lonNegative * (Float.valueOf(matcherLatlon.group(6)) + Float.valueOf(matcherLatlon.group(7) + "." + matcherLatlon.group(8)) / 60)));
            } else {
                Log.w(cgSettings.tag, "cgBase.parseLatlon: Failed to parse coordinates.");
            }
        }

        return result;
    }

    private static String formatCoordinate(final Double coordIn, final boolean degrees, final String direction, final String digitsFormat) {
        if (coordIn == null) {
            return "";
        }
        StringBuilder formatted = new StringBuilder(direction);

        double coordAbs = Math.abs(coordIn);
        Locale locale = Locale.getDefault();
        double floor = Math.floor(coordAbs);

        formatted.append(String.format(locale, digitsFormat, floor));

        if (degrees) {
            formatted.append("° ");
        } else {
            formatted.append(' ');
        }
        formatted.append(String.format(locale, "%06.3f", ((coordAbs - floor) * 60)));

        return formatted.toString();
    }

    public static String formatLatitude(final Double coord, final boolean degrees) {
        return formatCoordinate(coord, degrees, (coord >= 0) ? "N " : "S ", "%02.0f");
    }

    public static String formatLongitude(final Double coord, final boolean degrees) {
        return formatCoordinate(coord, degrees, (coord >= 0) ? "E " : "W ", "%03.0f");
    }

    public static String formatCoords(final Geopoint coords, final boolean degrees) {
        return formatLatitude(coords.getLatitude(), degrees) + " | " + formatLongitude(coords.getLongitude(), degrees);
    }

    public UUID searchByNextPage(cgSearchThread thread, final UUID searchId, int reason, boolean showCaptcha) {
        final String[] viewstates = app.getViewstates(searchId);

        final String url = app.getUrl(searchId);

        if (StringUtils.isBlank(url)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: No url found");
            return searchId;
        }

        if (isEmpty(viewstates)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: No viewstate given");
            return searchId;
        }

        // As in the original code, remove the query string
        final String uri = Uri.parse(url).buildUpon().query(null).build().toString();

        final Parameters params = new Parameters();
        setViewstates(viewstates, params);
        params.put("__EVENTTARGET", "ctl00$ContentBody$pgrBottom$ctl08");
        params.put("__EVENTARGUMENT", "");

        String page = getResponseData(postRequest(uri, params));
        if (checkLogin(page) == false) {
            int loginState = login();
            if (loginState == 1) {
                page = getResponseData(postRequest(uri, params));
            } else if (loginState == -3) {
                Log.i(cgSettings.tag, "Working as guest.");
            } else {
                app.setError(searchId, errorRetrieve.get(loginState));
                Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: Can not log in geocaching");
                return searchId;
            }
        }

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: No data from server");
            return searchId;
        }

        final cgCacheWrap caches = parseSearch(thread, url, page, showCaptcha);
        if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
            Log.e(cgSettings.tag, "cgeoBase.searchByNextPage: No cache parsed");
            return searchId;
        }

        // save to application
        app.setError(searchId, caches.error);
        app.setViewstates(searchId, caches.viewstates);

        final List<cgCache> cacheList = new ArrayList<cgCache>();
        for (cgCache cache : caches.cacheList) {
            app.addGeocode(searchId, cache.geocode);
            cacheList.add(cache);
        }

        app.addSearch(searchId, cacheList, true, reason);

        return searchId;
    }

    public UUID searchByGeocode(Map<String, String> parameters, int reason, boolean forceReload) {
        final cgSearch search = new cgSearch();
        String geocode = parameters.get("geocode");
        String guid = parameters.get("guid");

        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByGeocode: No geocode nor guid given");
            return null;
        }

        if (forceReload == false && reason == 0 && (app.isOffline(geocode, guid) || app.isThere(geocode, guid, true, true))) {
            if (StringUtils.isBlank(geocode) && StringUtils.isNotBlank(guid)) {
                geocode = app.getGeocode(guid);
            }

            List<cgCache> cacheList = new ArrayList<cgCache>();
            cacheList.add(app.getCacheByGeocode(geocode, true, true, true, true, true, true));
            search.addGeocode(geocode);

            app.addSearch(search, cacheList, false, reason);

            cacheList.clear();
            cacheList = null;

            return search.getCurrentId();
        }

        return ConnectorFactory.getConnector(geocode).searchByGeocode(this, geocode, guid, app, search, reason);
    }

    public UUID searchByOffline(Map<String, Object> parameters) {
        if (app == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchByOffline: No application found");
            return null;
        }

        Geopoint coords = null;
        String cachetype = null;
        Integer list = 1;

        if (parameters.containsKey("latitude") && parameters.containsKey("longitude")) {
            coords = new Geopoint((Double) parameters.get("latitude"), (Double) parameters.get("longitude"));
        }

        if (parameters.containsKey("cachetype")) {
            cachetype = (String) parameters.get("cachetype");
        }

        if (parameters.containsKey("list")) {
            list = (Integer) parameters.get("list");
        }

        final cgSearch search = app.getBatchOfStoredCaches(true, coords, cachetype, list);
        search.totalCnt = app.getAllStoredCachesCount(true, cachetype, list);

        return search.getCurrentId();
    }

    public UUID searchByHistory(Map<String, Object> parameters) {
        if (app == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchByHistory: No application found");
            return null;
        }

        String cachetype = null;

        if (parameters.containsKey("cachetype")) {
            cachetype = (String) parameters.get("cachetype");
        }

        final cgSearch search = app.getHistoryOfCaches(true, cachetype);
        search.totalCnt = app.getAllHistoricCachesCount(true, cachetype);

        return search.getCurrentId();
    }

    public UUID searchByCoords(cgSearchThread thread, Map<String, String> parameters, int reason, boolean showCaptcha) {
        final cgSearch search = new cgSearch();
        final String latitude = parameters.get("latitude");
        final String longitude = parameters.get("longitude");
        String cacheType = parameters.get("cachetype");

        if (StringUtils.isBlank(latitude)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No latitude given");
            return null;
        }

        if (StringUtils.isBlank(longitude)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No longitude given");
            return null;
        }

        if (StringUtils.isBlank(cacheType)) {
            cacheType = null;
        }

        final Map<String, String> params = new HashMap<String, String>();
        if (cacheType != null && cacheIDs.containsKey(cacheType)) {
            params.put("tx", cacheIDs.get(cacheType));
        } else {
            params.put("tx", cacheIDs.get("all"));
        }
        params.put("lat", latitude);
        params.put("lng", longitude);

        final String uri = "http://www.geocaching.com/seek/nearest.aspx";
        final String fullUri = uri + "?" + prepareParameters(params, false, true);
        String page = requestLogged(uri, params, false, false, true);

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No data from server");
            return null;
        }

        final cgCacheWrap caches = parseSearch(thread, fullUri, page, showCaptcha);
        if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
            Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No cache parsed");
        }

        if (app == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No application found");
            return null;
        }

        List<cgCache> cacheList = processSearchResults(search, caches, settings.excludeDisabled, 0, null);

        app.addSearch(search, cacheList, true, reason);

        return search.getCurrentId();
    }

    public UUID searchByKeyword(cgSearchThread thread, Map<String, String> parameters, int reason, boolean showCaptcha) {
        final cgSearch search = new cgSearch();
        final String keyword = parameters.get("keyword");
        String cacheType = parameters.get("cachetype");

        if (StringUtils.isBlank(keyword)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByKeyword: No keyword given");
            return null;
        }

        if (StringUtils.isBlank(cacheType)) {
            cacheType = null;
        }

        final Map<String, String> params = new HashMap<String, String>();
        if (cacheType != null && cacheIDs.containsKey(cacheType)) {
            params.put("tx", cacheIDs.get(cacheType));
        } else {
            params.put("tx", cacheIDs.get("all"));
        }
        params.put("key", keyword);

        final String uri = "http://www.geocaching.com/seek/nearest.aspx";
        final String fullUri = uri + "?" + prepareParameters(params, false, true);
        String page = requestLogged(uri, params, false, false, true);

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByKeyword: No data from server");
            return null;
        }

        final cgCacheWrap caches = parseSearch(thread, fullUri, page, showCaptcha);
        if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
            Log.e(cgSettings.tag, "cgeoBase.searchByKeyword: No cache parsed");
        }

        if (app == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No application found");
            return null;
        }

        List<cgCache> cacheList = processSearchResults(search, caches, settings.excludeDisabled, 0, null);

        app.addSearch(search, cacheList, true, reason);

        return search.getCurrentId();
    }

    public UUID searchByUsername(cgSearchThread thread, Map<String, String> parameters, int reason, boolean showCaptcha) {
        final cgSearch search = new cgSearch();
        final String userName = parameters.get("username");
        String cacheType = parameters.get("cachetype");

        if (StringUtils.isBlank(userName)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByUsername: No user name given");
            return null;
        }

        if (StringUtils.isBlank(cacheType)) {
            cacheType = null;
        }

        final Map<String, String> params = new HashMap<String, String>();
        if (cacheType != null && cacheIDs.containsKey(cacheType)) {
            params.put("tx", cacheIDs.get(cacheType));
        } else {
            params.put("tx", cacheIDs.get("all"));
        }
        params.put("ul", userName);

        boolean my = false;
        if (userName.equalsIgnoreCase(settings.getLogin().get("username"))) {
            my = true;
            Log.i(cgSettings.tag, "cgBase.searchByUsername: Overriding users choice, downloading all caches.");
        }

        final String uri = "http://www.geocaching.com/seek/nearest.aspx";
        final String fullUri = uri + "?" + prepareParameters(params, my, true);
        String page = requestLogged(uri, params, false, my, true);

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByUsername: No data from server");
            return null;
        }

        final cgCacheWrap caches = parseSearch(thread, fullUri, page, showCaptcha);
        if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
            Log.e(cgSettings.tag, "cgeoBase.searchByUsername: No cache parsed");
        }

        if (app == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchByUsername: No application found");
            return null;
        }

        List<cgCache> cacheList = processSearchResults(search, caches, settings.excludeDisabled, 0, null);

        app.addSearch(search, cacheList, true, reason);

        return search.getCurrentId();
    }

    public UUID searchByOwner(cgSearchThread thread, Map<String, String> parameters, int reason, boolean showCaptcha) {
        final cgSearch search = new cgSearch();
        final String userName = parameters.get("username");
        String cacheType = parameters.get("cachetype");

        if (StringUtils.isBlank(userName)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByOwner: No user name given");
            return null;
        }

        if (StringUtils.isBlank(cacheType)) {
            cacheType = null;
        }

        final Map<String, String> params = new HashMap<String, String>();
        if (cacheType != null && cacheIDs.containsKey(cacheType)) {
            params.put("tx", cacheIDs.get(cacheType));
        } else {
            params.put("tx", cacheIDs.get("all"));
        }
        params.put("u", userName);

        final String uri = "http://www.geocaching.com/seek/nearest.aspx";
        final String fullUri = uri + "?" + prepareParameters(params, false, true);
        String page = requestLogged(uri, params, false, false, true);

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByOwner: No data from server");
            return null;
        }

        final cgCacheWrap caches = parseSearch(thread, fullUri, page, showCaptcha);
        if (caches == null || caches.cacheList == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchByOwner: No cache parsed");
        }

        if (app == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchByCoords: No application found");
            return null;
        }

        List<cgCache> cacheList = processSearchResults(search, caches, settings.excludeDisabled, 0, null);

        app.addSearch(search, cacheList, true, reason);

        return search.getCurrentId();
    }

    public UUID searchByViewport(Map<String, String> parameters, int reason) {
        final cgSearch search = new cgSearch();
        final String latMin = parameters.get("latitude-min");
        final String latMax = parameters.get("latitude-max");
        final String lonMin = parameters.get("longitude-min");
        final String lonMax = parameters.get("longitude-max");

        String usertoken = null;
        if (parameters.get("usertoken") != null) {
            usertoken = parameters.get("usertoken");
        } else {
            usertoken = "";
        }

        String page = null;

        if (StringUtils.isBlank(latMin) || StringUtils.isBlank(latMax) || StringUtils.isBlank(lonMin) || StringUtils.isBlank(lonMax)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByViewport: Not enough parameters to recognize viewport");
            return null;
        }

        final String params = "{\"dto\":{\"data\":{\"c\":1,\"m\":\"\",\"d\":\"" + latMax + "|" + latMin + "|" + lonMax + "|" + lonMin + "\"},\"ut\":\"" + usertoken + "\"}}";

        final String uri = "http://www.geocaching.com/map/default.aspx/MapAction";
        page = requestJSONgc(uri, params);

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.searchByViewport: No data from server");
            return null;
        }

        final cgCacheWrap caches = parseMapJSON(Uri.parse(uri).buildUpon().encodedQuery(params).build().toString(), page);
        if (caches == null || caches.cacheList == null || caches.cacheList.isEmpty()) {
            Log.e(cgSettings.tag, "cgeoBase.searchByViewport: No cache parsed");
        }

        if (app == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchByViewport: No application found");
            return null;
        }

        List<cgCache> cacheList = processSearchResults(search, caches, settings.excludeDisabled, settings.excludeMine, settings.cacheType);

        app.addSearch(search, cacheList, true, reason);

        return search.getCurrentId();
    }

    private String requestJSONgc(final String uri, final String params) {
        String page;
        final HttpPost request = new HttpPost("http://www.geocaching.com/map/default.aspx/MapAction");
        try {
            request.setEntity(new StringEntity(params, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            Log.e(cgSettings.tag, "cgeoBase.searchByViewport", e);
        }

        request.addHeader("Content-Type", "application/json; charset=UTF-8");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        request.addHeader("Referer", uri);
        page = getResponseData(request(request));
        return page;
    }

    public List<cgUser> getGeocachersInViewport(String username, Double latMin, Double latMax, Double lonMin, Double lonMax) {
        final List<cgUser> users = new ArrayList<cgUser>();

        if (username == null) {
            return users;
        }
        if (latMin == null || latMax == null || lonMin == null || lonMax == null) {
            return users;
        }

        final Parameters params = new Parameters();

        params.put("u", username);
        params.put("ltm", String.format((Locale) null, "%.6f", latMin));
        params.put("ltx", String.format((Locale) null, "%.6f", latMax));
        params.put("lnm", String.format((Locale) null, "%.6f", lonMin));
        params.put("lnx", String.format((Locale) null, "%.6f", lonMax));

        final String data = getResponseData(postRequest("http://api.go4cache.com/get.php", params));

        if (StringUtils.isBlank(data)) {
            Log.e(cgSettings.tag, "cgeoBase.getGeocachersInViewport: No data from server");
            return null;
        }

        try {
            final JSONObject dataJSON = new JSONObject(data);

            final JSONArray usersData = dataJSON.getJSONArray("users");
            if (usersData != null && usersData.length() > 0) {
                int count = usersData.length();
                JSONObject oneUser = null;
                for (int i = 0; i < count; i++) {
                    final cgUser user = new cgUser();
                    oneUser = usersData.getJSONObject(i);
                    if (oneUser != null) {
                        final String located = oneUser.getString("located");
                        if (located != null) {
                            user.located = dateSqlIn.parse(located);
                        } else {
                            user.located = new Date();
                        }
                        user.username = oneUser.getString("user");
                        user.coords = new Geopoint(oneUser.getDouble("latitude"), oneUser.getDouble("longitude"));
                        user.action = oneUser.getString("action");
                        user.client = oneUser.getString("client");

                        if (user.coords != null) {
                            users.add(user);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.getGeocachersInViewport: " + e.toString());
        }

        return users;
    }

    public List<cgCache> processSearchResults(cgSearch search, cgCacheWrap caches, int excludeDisabled, int excludeMine, String cacheType) {
        List<cgCache> cacheList = new ArrayList<cgCache>();
        if (caches != null) {
            if (StringUtils.isNotBlank(caches.error)) {
                search.error = caches.error;
            }
            if (StringUtils.isNotBlank(caches.url)) {
                search.url = caches.url;
            }
            search.viewstates = caches.viewstates;
            search.totalCnt = caches.totalCnt;

            if (CollectionUtils.isNotEmpty(caches.cacheList)) {
                for (cgCache cache : caches.cacheList) {
                    if ((excludeDisabled == 0 || (excludeDisabled == 1 && cache.disabled == false))
                            && (excludeMine == 0 || (excludeMine == 1 && cache.own == false))
                            && (excludeMine == 0 || (excludeMine == 1 && cache.found == false))
                            && (cacheType == null || (cacheType.equals(cache.type)))) {
                        search.addGeocode(cache.geocode);
                        cacheList.add(cache);
                    }
                }
            }
        }
        return cacheList;
    }

    public cgTrackable searchTrackable(Map<String, String> parameters) {
        final String geocode = parameters.get("geocode");
        final String guid = parameters.get("guid");
        final String id = parameters.get("id");
        cgTrackable trackable = new cgTrackable();

        if (StringUtils.isBlank(geocode) && StringUtils.isBlank(guid) && StringUtils.isBlank(id)) {
            Log.e(cgSettings.tag, "cgeoBase.searchTrackable: No geocode nor guid nor id given");
            return null;
        }

        final Map<String, String> params = new HashMap<String, String>();
        if (StringUtils.isNotBlank(geocode)) {
            params.put("tracker", geocode);
        } else if (StringUtils.isNotBlank(guid)) {
            params.put("guid", guid);
        } else if (StringUtils.isNotBlank(id)) {
            params.put("id", id);
        }

        String page = requestLogged("http://www.geocaching.com/track/details.aspx", params, false, false, false);

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.searchTrackable: No data from server");
            return trackable;
        }

        trackable = parseTrackable(page);
        if (trackable == null) {
            Log.e(cgSettings.tag, "cgeoBase.searchTrackable: No trackable parsed");
            return trackable;
        }

        return trackable;
    }

    public int postLog(cgeoapplication app, String geocode, String cacheid, String[] viewstates,
            int logType, int year, int month, int day, String log, List<cgTrackableLog> trackables) {
        if (isEmpty(viewstates)) {
            Log.e(cgSettings.tag, "cgeoBase.postLog: No viewstate given");
            return 1000;
        }

        if (logTypes2.containsKey(logType) == false) {
            Log.e(cgSettings.tag, "cgeoBase.postLog: Unknown logtype");
            return 1000;
        }

        if (StringUtils.isBlank(log)) {
            Log.e(cgSettings.tag, "cgeoBase.postLog: No log text given");
            return 1001;
        }

        // fix log (non-Latin characters converted to HTML entities)
        final int logLen = log.length();
        final StringBuilder logUpdated = new StringBuilder();

        for (int i = 0; i < logLen; i++) {
            char c = log.charAt(i);

            if (c > 300) {
                logUpdated.append("&#");
                logUpdated.append(Integer.toString((int) c));
                logUpdated.append(';');
            } else {
                logUpdated.append(c);
            }
        }
        log = logUpdated.toString();

        log = log.replace("\n", "\r\n"); // windows' eol

        if (trackables != null) {
            Log.i(cgSettings.tag, "Trying to post log for cache #" + cacheid + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log + "; trackables: " + trackables.size());
        } else {
            Log.i(cgSettings.tag, "Trying to post log for cache #" + cacheid + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log + "; trackables: 0");
        }

        final Parameters params = new Parameters();

        setViewstates(viewstates, params);
        params.put("__EVENTTARGET", "");
        params.put("__EVENTARGUMENT", "");
        params.put("__LASTFOCUS", "");
        params.put("ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType));
        params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", String.format("%02d", month) + "/" + String.format("%02d", day) + "/" + String.format("%04d", year));
        params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Month", Integer.toString(month));
        params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Day", Integer.toString(day));
        params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Year", Integer.toString(year));
        params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", log);
        params.put("ctl00$ContentBody$LogBookPanel1$LogButton", "Submit Log Entry");
        params.put("ctl00$ContentBody$uxVistOtherListingGC", "");
        if (trackables != null && trackables.isEmpty() == false) { //  we have some trackables to proceed
            final StringBuilder hdnSelected = new StringBuilder();

            for (cgTrackableLog tb : trackables) {
                final String action = Integer.toString(tb.id) + logTypesTrackableAction.get(tb.action);

                if (tb.action > 0) {
                    hdnSelected.append(action);
                    hdnSelected.append(',');
                }
            }

            params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnSelectedActions", hdnSelected.toString()); // selected trackables
            params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnCurrentFilter", "");
        }

        final String uri = new Uri.Builder().scheme("http").authority("www.geocaching.com").path("/seek/log.aspx").encodedQuery("ID=" + cacheid).build().toString();
        String page = getResponseData(postRequest(uri, params));
        if (!checkLogin(page)) {
            int loginState = login();
            if (loginState == 1) {
                page = getResponseData(postRequest(uri, params));
            } else {
                Log.e(cgSettings.tag, "cgeoBase.postLog: Can not log in geocaching (error: " + loginState + ")");
                return loginState;
            }
        }

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.postLog: No data from server");
            return 1002;
        }

        // maintenance, archived needs to be confirmed
        final Pattern pattern = Pattern.compile("<span id=\"ctl00_ContentBody_LogBookPanel1_lbConfirm\"[^>]*>([^<]*<font[^>]*>)?([^<]+)(</font>[^<]*)?</span>", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(page);

        try {
            if (matcher.find() && matcher.groupCount() > 0) {
                final String[] viewstatesConfirm = getViewstates(page);

                if (isEmpty(viewstatesConfirm)) {
                    Log.e(cgSettings.tag, "cgeoBase.postLog: No viewstate for confirm log");
                    return 1000;
                }

                params.clear();
                setViewstates(viewstatesConfirm, params);
                params.put("__EVENTTARGET", "");
                params.put("__EVENTARGUMENT", "");
                params.put("__LASTFOCUS", "");
                params.put("ctl00$ContentBody$LogBookPanel1$btnConfirm", "Yes");
                params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", log);
                params.put("ctl00$ContentBody$uxVistOtherListingGC", "");
                if (trackables != null && trackables.isEmpty() == false) { //  we have some trackables to proceed
                    final StringBuilder hdnSelected = new StringBuilder();

                    for (cgTrackableLog tb : trackables) {
                        String ctl = null;
                        final String action = Integer.toString(tb.id) + logTypesTrackableAction.get(tb.action);

                        if (tb.ctl < 10) {
                            ctl = "0" + Integer.toString(tb.ctl);
                        } else {
                            ctl = Integer.toString(tb.ctl);
                        }

                        params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$repTravelBugs$ctl" + ctl + "$ddlAction", action);
                        if (tb.action > 0) {
                            hdnSelected.append(action);
                            hdnSelected.append(',');
                        }
                    }

                    params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnSelectedActions", hdnSelected.toString()); // selected trackables
                    params.put("ctl00$ContentBody$LogBookPanel1$uxTrackables$hdnCurrentFilter", "");
                }

                page = getResponseData(postRequest(uri, params));
            }
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgeoBase.postLog.confim: " + e.toString());
        }

        try {
            final Pattern patternOk = Pattern.compile("<h2[^>]*>[^<]*<span id=\"ctl00_ContentBody_lbHeading\"[^>]*>[^<]*</span>[^<]*</h2>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            final Matcher matcherOk = patternOk.matcher(page);
            if (matcherOk.find()) {
                Log.i(cgSettings.tag, "Log successfully posted to cache #" + cacheid);

                if (app != null && geocode != null) {
                    app.saveVisitDate(geocode);
                }

                return 1;
            }
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgeoBase.postLog.check: " + e.toString());
        }

        Log.e(cgSettings.tag, "cgeoBase.postLog: Failed to post log because of unknown error");
        return 1000;
    }

    public int postLogTrackable(String tbid, String trackingCode, String[] viewstates,
            int logType, int year, int month, int day, String log) {
        if (isEmpty(viewstates)) {
            Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: No viewstate given");
            return 1000;
        }

        if (logTypes2.containsKey(logType) == false) {
            Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: Unknown logtype");
            return 1000;
        }

        if (StringUtils.isBlank(log)) {
            Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: No log text given");
            return 1001;
        }

        Log.i(cgSettings.tag, "Trying to post log for trackable #" + trackingCode + " - action: " + logType + "; date: " + year + "." + month + "." + day + ", log: " + log);

        log = log.replace("\n", "\r\n"); // windows' eol

        final Calendar currentDate = Calendar.getInstance();
        final Parameters params = new Parameters();

        setViewstates(viewstates, params);
        params.put("__EVENTTARGET", "");
        params.put("__EVENTARGUMENT", "");
        params.put("__LASTFOCUS", "");
        params.put("ctl00$ContentBody$LogBookPanel1$ddLogType", Integer.toString(logType));
        params.put("ctl00$ContentBody$LogBookPanel1$tbCode", trackingCode);
        if (currentDate.get(Calendar.YEAR) == year && (currentDate.get(Calendar.MONTH) + 1) == month && currentDate.get(Calendar.DATE) == day) {
            params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", "");
        } else {
            params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged", Integer.toString(month) + "/" + Integer.toString(day) + "/" + Integer.toString(year));
        }
        params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Day", Integer.toString(day));
        params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Month", Integer.toString(month));
        params.put("ctl00$ContentBody$LogBookPanel1$DateTimeLogged$Year", Integer.toString(year));
        params.put("ctl00$ContentBody$LogBookPanel1$uxLogInfo", log);
        params.put("ctl00$ContentBody$LogBookPanel1$LogButton", "Submit Log Entry");
        params.put("ctl00$ContentBody$uxVistOtherListingGC", "");

        final String uri = new Uri.Builder().scheme("http").authority("www.geocaching.com").path("/track/log.aspx").encodedQuery("wid=" + tbid).build().toString();
        String page = getResponseData(postRequest(uri, params));
        if (checkLogin(page) == false) {
            int loginState = login();
            if (loginState == 1) {
                page = getResponseData(postRequest(uri, params));
            } else {
                Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: Can not log in geocaching (error: " + loginState + ")");
                return loginState;
            }
        }

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: No data from server");
            return 1002;
        }

        try {
            final Pattern patternOk = Pattern.compile("<div id=[\"|']ctl00_ContentBody_LogBookPanel1_ViewLogPanel[\"|']>", Pattern.CASE_INSENSITIVE);
            final Matcher matcherOk = patternOk.matcher(page);
            if (matcherOk.find()) {
                Log.i(cgSettings.tag, "Log successfully posted to trackable #" + trackingCode);
                return 1;
            }
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgeoBase.postLogTrackable.check: " + e.toString());
        }

        Log.e(cgSettings.tag, "cgeoBase.postLogTrackable: Failed to post log because of unknown error");
        return 1000;
    }

    /**
     * Adds the cache to the watchlist of the user.
     *
     * @param cache
     *            the cache to add
     * @return -1: error occured
     */
    public int addToWatchlist(cgCache cache) {
        final String uri = "http://www.geocaching.com/my/watchlist.aspx?w=" + cache.cacheId;
        String page = postRequestLogged(uri);

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgBase.addToWatchlist: No data from server");
            return -1; // error
        }

        boolean guidOnPage = cache.isGuidContainedInPage(page);
        if (guidOnPage) {
            Log.i(cgSettings.tag, "cgBase.addToWatchlist: cache is on watchlist");
            cache.onWatchlist = true;
        } else {
            Log.e(cgSettings.tag, "cgBase.addToWatchlist: cache is not on watchlist");
        }
        return guidOnPage ? 1 : -1; // on watchlist (=added) / else: error
    }

    /**
     * Removes the cache from the watchlist
     *
     * @param cache
     *            the cache to remove
     * @return -1: error occured
     */
    public int removeFromWatchlist(cgCache cache) {
        final String uri = "http://www.geocaching.com/my/watchlist.aspx?ds=1&action=rem&id=" + cache.cacheId;
        String page = postRequestLogged(uri);

        if (StringUtils.isBlank(page)) {
            Log.e(cgSettings.tag, "cgBase.removeFromWatchlist: No data from server");
            return -1; // error
        }

        // removing cache from list needs approval by hitting "Yes" button
        final Parameters params = new Parameters();
        transferViewstates(page, params);
        params.put("__EVENTTARGET", "");
        params.put("__EVENTARGUMENT", "");
        params.put("ctl00$ContentBody$btnYes", "Yes");

        page = getResponseData(postRequest(uri, params));
        boolean guidOnPage = cache.isGuidContainedInPage(page);
        if (!guidOnPage) {
            Log.i(cgSettings.tag, "cgBase.removeFromWatchlist: cache removed from watchlist");
            cache.onWatchlist = false;
        } else {
            Log.e(cgSettings.tag, "cgBase.removeFromWatchlist: cache not removed from watchlist");
        }
        return guidOnPage ? -1 : 0; // on watchlist (=error) / not on watchlist
    }

    final public static HostnameVerifier doNotVerify = new HostnameVerifier() {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    public static void trustAllHosts() {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[] {};
                    }

                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }
                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.trustAllHosts: " + e.toString());
        }
    }

    public static void postTweetCache(cgeoapplication app, cgSettings settings, String geocode) {
        final cgCache cache = app.getCacheByGeocode(geocode);
        String name = cache.name;
        if (name.length() > 84) {
            name = name.substring(0, 81) + "...";
        }
        final String status = "I found " + name + " (http://coord.info/" + cache.geocode.toUpperCase() + ")! #cgeo #geocaching"; // 56 chars + cache name

        postTweet(app, settings, status, null);
    }

    public static void postTweetTrackable(cgeoapplication app, cgSettings settings, String geocode) {
        final cgTrackable trackable = app.getTrackableByGeocode(geocode);
        String name = trackable.name;
        if (name.length() > 82) {
            name = name.substring(0, 79) + "...";
        }
        final String status = "I touched " + name + " (http://coord.info/" + trackable.geocode.toUpperCase() + ")! #cgeo #geocaching"; // 58 chars + trackable name

        postTweet(app, settings, status, null);
    }

    public static void postTweet(cgeoapplication app, cgSettings settings, String status, final Geopoint coords) {
        if (app == null) {
            return;
        }
        if (settings == null || StringUtils.isBlank(settings.tokenPublic) || StringUtils.isBlank(settings.tokenSecret)) {
            return;
        }

        try {
            Map<String, String> parameters = new HashMap<String, String>();

            parameters.put("status", status);
            if (coords != null) {
                parameters.put("lat", String.format("%.6f", coords.getLatitude()));
                parameters.put("long", String.format("%.6f", coords.getLongitude()));
                parameters.put("display_coordinates", "true");
            }

            final String paramsDone = cgOAuth.signOAuth("api.twitter.com", "/1/statuses/update.json", "POST", false, parameters, settings.tokenPublic, settings.tokenSecret);

            HttpURLConnection connection = null;
            try {
                final StringBuffer buffer = new StringBuffer();
                final URL u = new URL("http://api.twitter.com/1/statuses/update.json");
                final URLConnection uc = u.openConnection();

                uc.setRequestProperty("Host", "api.twitter.com");

                connection = (HttpURLConnection) uc;
                connection.setReadTimeout(30000);
                connection.setRequestMethod("POST");
                HttpURLConnection.setFollowRedirects(true);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                final OutputStream out = connection.getOutputStream();
                final OutputStreamWriter wr = new OutputStreamWriter(out);
                wr.write(paramsDone);
                wr.flush();
                wr.close();

                Log.i(cgSettings.tag, "Twitter.com: " + connection.getResponseCode() + " " + connection.getResponseMessage());

                InputStream ins;
                final String encoding = connection.getContentEncoding();

                if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                    ins = new GZIPInputStream(connection.getInputStream());
                } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
                    ins = new InflaterInputStream(connection.getInputStream(), new Inflater(true));
                } else {
                    ins = connection.getInputStream();
                }

                final InputStreamReader inr = new InputStreamReader(ins);
                final BufferedReader br = new BufferedReader(inr);

                readIntoBuffer(br, buffer);

                br.close();
                ins.close();
                inr.close();
                connection.disconnect();
            } catch (IOException e) {
                Log.e(cgSettings.tag, "cgBase.postTweet.IO: " + connection.getResponseCode() + ": " + connection.getResponseMessage() + " ~ " + e.toString());

                final InputStream ins = connection.getErrorStream();
                final StringBuffer buffer = new StringBuffer();
                final InputStreamReader inr = new InputStreamReader(ins);
                final BufferedReader br = new BufferedReader(inr);

                readIntoBuffer(br, buffer);

                br.close();
                ins.close();
                inr.close();
            } catch (Exception e) {
                Log.e(cgSettings.tag, "cgBase.postTweet.inner: " + e.toString());
            }

            connection.disconnect();
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.postTweet: " + e.toString());
        }
    }

    private static void readIntoBuffer(BufferedReader br, StringBuffer buffer) throws IOException {
        final int bufferSize = 1024 * 16;
        final char[] bytes = new char[bufferSize];
        int bytesRead;
        while ((bytesRead = br.read(bytes)) > 0) {
            buffer.append(bytes, 0, bytesRead);
        }
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // nothing
        }

        return null;
    }

    public static String urlencode_rfc3986(String text) {
        final String encoded = URLEncoder.encode(text).replace("+", "%20").replaceAll("%7E", "~");

        return encoded;
    }

    public String prepareParameters(Map<String, String> params, boolean my, boolean addF) {
        String paramsDone = null;

        if (my != true && settings.excludeMine > 0) {
            if (params == null) {
                params = new HashMap<String, String>();
            }
            if (addF) {
                params.put("f", "1");
            }

            Log.i(cgSettings.tag, "Skipping caches found or hidden by user.");
        }

        if (params != null) {
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            List<String> paramsEncoded = new ArrayList<String>();

            for (Map.Entry<String, String> entry : entrySet)
            {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.charAt(0) == '^') {
                    key = "";
                }
                if (value == null) {
                    value = "";
                }

                paramsEncoded.add(key + "=" + urlencode_rfc3986(value));
            }

            paramsDone = StringUtils.join(paramsEncoded.toArray(), '&');
        } else {
            paramsDone = "";
        }

        return paramsDone;
    }

    public String[] requestViewstates(final String uri, Map<String, String> params, boolean xContentType, boolean my) {
        final HttpResponse response = request(uri, params, xContentType, my, false);

        return getViewstates(getResponseData(response));
    }

    static public String getResponseData(final HttpResponse response) {
        if (response == null) {
            return null;
        }
        try {
            return replaceWhitespace(EntityUtils.toString(response.getEntity(), HTTP.UTF_8));
        } catch (Exception e) {
            Log.e(cgSettings.tag, "getResponseData", e);
            return null;
        }
    }

    public String postRequestLogged(final String uri) {
        final String data = getResponseData(postRequest(uri, null));
        if (!checkLogin(data)) {
            if (login() == 1) {
                return getResponseData(postRequest(uri, null));
            } else {
                Log.i(cgSettings.tag, "Working as guest.");
            }
        }
        return data;
    }

    public String requestLogged(final String uri, Map<String, String> params, boolean xContentType, boolean my, boolean addF) {
        HttpResponse response = request(uri, params, xContentType, my, addF);
        String data = getResponseData(response);

        if (checkLogin(data) == false) {
            int loginState = login();
            if (loginState == 1) {
                response = request(uri, params, xContentType, my, addF);
                data = getResponseData(response);
            } else {
                Log.i(cgSettings.tag, "Working as guest.");
            }
        }

        return data;
    }

    public HttpResponse request(final String uri, Map<String, String> params, boolean xContentType, boolean my, boolean addF) {
        final String paramsDone = prepareParameters(params, my, addF);
        return request(uri, paramsDone, xContentType);
    }

    private static DefaultHttpClient httpClient;

    public static DefaultHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (cgBase.class) {
                if (httpClient == null) {
                    httpClient = new DefaultHttpClient();
                    // TODO: check redirect strategy
                }
            }
        }
        return httpClient;
    }

    public static void clearCookies() {
        getHttpClient().getCookieStore().clear();
    }

    public HttpResponse postRequest(final String uri, final List<? extends NameValuePair> params) {
        try {
            HttpPost request = new HttpPost(uri);
            if (params != null) {
                request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            }
            request.setHeader("X-Requested-With", "XMLHttpRequest");
            return request(request);
        } catch (Exception e) {
            // Can be UnsupportedEncodingException, ClientProtocolException or IOException
            Log.e(cgSettings.tag, "postRequest", e);
            return null;
        }
    }

    public HttpResponse request(final String uri, final String params, final Boolean xContentType) {
        final HttpRequestBase request = new HttpGet(Uri.parse(uri).buildUpon().encodedQuery(params).build().toString());

        request.setHeader("X-Requested-With", "XMLHttpRequest");

        if (xContentType) {
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        }

        return request(request);
    }

    private HttpResponse request(final HttpRequestBase request) {
        final DefaultHttpClient client = getHttpClient();

        if (settings.asBrowser == 1) {
            request.setHeader("Accept-Charset", "utf-8, iso-8859-1, utf-16, *;q=0.7");
            request.setHeader("Accept-Language", "en-US");
            request.setHeader("User-Agent", idBrowser);
        }

        for (int i = 0; i < 5; i++) {
            if (i > 0) {
                Log.w(cgSettings.tag, "Failed to download data, retrying. Attempt #" + (i + 1));
            }

            Log.d(cgSettings.tag, "request: " + request.getMethod() + " " + hidePassword(request.getURI().toString()));

            try {
                return client.execute(request);
            } catch (Exception e) {
                Log.e(cgSettings.tag, "cgeoBase.request", e);
            }
        }

        return null;
    }

    /**
     * Replace the characters \n, \r and \t with a space
     *
     * @param buffer
     *            The data
     */
    public static String replaceWhitespace(final String data) {
        return StringUtils.join(StringUtils.split(data, " \n\r\t"), ' ');
    }

    public static JSONObject requestJSON(final String uri) {
        final HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        request.setHeader("Content-Type", "application/json; charset=UTF-8");
        request.setHeader("X-Requested-With", "XMLHttpRequest");

        for (int i = 0; i < 3; i++) {
            try {
                final HttpResponse response = getHttpClient().execute(request);
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 403) {
                    // Forbidden, do not retry
                    break;
                }
                final StringBuffer buffer = new StringBuffer();
                final BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                readIntoBuffer(br, buffer);

                Log.i(cgSettings.tag, "[POST " + statusCode + "] Downloaded " + uri);

                return new JSONObject(buffer.toString());
            } catch (Exception e) {
                Log.e(cgSettings.tag, "cgeoBase.requestJSON", e);
            }
        }

        return null;
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();

            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }

        return (path.delete());
    }

    public void storeCache(cgeoapplication app, Activity activity, cgCache cache, String geocode, int listId, Handler handler) {
        try {
            // get cache details, they may not yet be complete
            if (cache != null) {
                // only reload the cache, if it was already stored or has not all details (by checking the description)
                if (cache.reason > 0 || StringUtils.isBlank(cache.description)) {
                    final Map<String, String> params = new HashMap<String, String>();
                    params.put("geocode", cache.geocode);
                    final UUID searchId = searchByGeocode(params, listId, false);
                    cache = app.getCache(searchId);
                }
            } else if (StringUtils.isNotBlank(geocode)) {
                final Map<String, String> params = new HashMap<String, String>();
                params.put("geocode", geocode);
                final UUID searchId = searchByGeocode(params, listId, false);
                cache = app.getCache(searchId);
            }

            if (cache == null) {
                if (handler != null) {
                    handler.sendMessage(new Message());
                }

                return;
            }

            final cgHtmlImg imgGetter = new cgHtmlImg(activity, cache.geocode, false, listId, true);

            // store images from description
            if (StringUtils.isNotBlank(cache.description)) {
                Html.fromHtml(cache.description, imgGetter, null);
            }

            // store spoilers
            if (CollectionUtils.isNotEmpty(cache.spoilers)) {
                for (cgImage oneSpoiler : cache.spoilers) {
                    imgGetter.getDrawable(oneSpoiler.url);
                }
            }

            // store images from logs
            if (settings.storelogimages && cache.logs != null) {
                for (cgLog log : cache.logs) {
                    if (CollectionUtils.isNotEmpty(log.logImages)) {
                        for (cgImage oneLogImg : log.logImages) {
                            imgGetter.getDrawable(oneLogImg.url);
                        }
                    }
                }
            }

            // store map previews
            StaticMapsProvider.downloadMaps(cache, settings, activity);

            app.markStored(cache.geocode, listId);
            app.removeCacheFromCache(cache.geocode);

            if (handler != null) {
                handler.sendMessage(new Message());
            }
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.storeCache: " + e.toString());
        }
    }

    public static void dropCache(cgeoapplication app, Activity activity, cgCache cache, Handler handler) {
        try {
            app.markDropped(cache.geocode);
            app.removeCacheFromCache(cache.geocode);

            handler.sendMessage(new Message());
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.dropCache: " + e.toString());
        }
    }

    public static boolean isInViewPort(int centerLat1, int centerLon1, int centerLat2, int centerLon2, int spanLat1, int spanLon1, int spanLat2, int spanLon2) {
        try {
            // expects coordinates in E6 format
            final int left1 = centerLat1 - (spanLat1 / 2);
            final int right1 = centerLat1 + (spanLat1 / 2);
            final int top1 = centerLon1 + (spanLon1 / 2);
            final int bottom1 = centerLon1 - (spanLon1 / 2);

            final int left2 = centerLat2 - (spanLat2 / 2);
            final int right2 = centerLat2 + (spanLat2 / 2);
            final int top2 = centerLon2 + (spanLon2 / 2);
            final int bottom2 = centerLon2 - (spanLon2 / 2);

            if (left2 <= left1) {
                return false;
            }
            if (right2 >= right1) {
                return false;
            }
            if (top2 >= top1) {
                return false;
            }
            if (bottom2 <= bottom1) {
                return false;
            }

            return true;
        } catch (Exception e) {
            Log.e(cgSettings.tag, "cgBase.isInViewPort: " + e.toString());
            return false;
        }
    }

    // viewport is defined by center, span and some (10%) reserve on every side
    /**
     * Check if coordinates are located in a viewport (defined by its center and span
     * in each direction). The viewport also includes a 10% extension on each side.
     *
     * @param centerLat
     *            the viewport center latitude
     * @param centerLon
     *            the viewport center longitude
     * @param spanLat
     *            the latitude span
     * @param spanLon
     *            the longitude span
     * @param coords
     *            the coordinates to check
     * @return true if the coordinates are in the viewport
     */
    public static boolean isCacheInViewPort(int centerLat, int centerLon, int spanLat, int spanLon, final Geopoint coords) {
        return Math.abs(coords.getLatitudeE6() - centerLat) <= Math.abs(spanLat) * 0.6 &&
                Math.abs(coords.getLongitudeE6() - centerLon) <= Math.abs(spanLon) * 0.6;
    }

    private static char[] base64map1 = new char[64];

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            base64map1[i++] = c;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            base64map1[i++] = c;
        }
        for (char c = '0'; c <= '9'; c++) {
            base64map1[i++] = c;
        }
        base64map1[i++] = '+';
        base64map1[i++] = '/';
    }
    private static byte[] base64map2 = new byte[128];

    static {
        for (int i = 0; i < base64map2.length; i++) {
            base64map2[i] = -1;
        }
        for (int i = 0; i < 64; i++) {
            base64map2[base64map1[i]] = (byte) i;
        }
    }

    public static String base64Encode(byte[] in) {
        int iLen = in.length;
        int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
        int oLen = ((iLen + 2) / 3) * 4; // output length including padding
        char[] out = new char[oLen];
        int ip = 0;
        int op = 0;

        while (ip < iLen) {
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iLen ? in[ip++] & 0xff : 0;
            int i2 = ip < iLen ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = base64map1[o0];
            out[op++] = base64map1[o1];
            out[op] = op < oDataLen ? base64map1[o2] : '=';
            op++;
            out[op] = op < oDataLen ? base64map1[o3] : '=';
            op++;
        }

        return new String(out);
    }

    public static byte[] base64Decode(String text) {
        char[] in = text.toCharArray();

        int iLen = in.length;
        if (iLen % 4 != 0) {
            throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
        }
        while (iLen > 0 && in[iLen - 1] == '=') {
            iLen--;
        }
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        int ip = 0;
        int op = 0;
        while (ip < iLen) {
            int i0 = in[ip++];
            int i1 = in[ip++];
            int i2 = ip < iLen ? in[ip++] : 'A';
            int i3 = ip < iLen ? in[ip++] : 'A';
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int b0 = base64map2[i0];
            int b1 = base64map2[i1];
            int b2 = base64map2[i2];
            int b3 = base64map2[i3];
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int o0 = (b0 << 2) | (b1 >>> 4);
            int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
            int o2 = ((b2 & 3) << 6) | b3;
            out[op++] = (byte) o0;
            if (op < oLen) {
                out[op++] = (byte) o1;
            }
            if (op < oLen) {
                out[op++] = (byte) o2;
            }
        }
        return out;
    }

    public static int getCacheIcon(final String type) {
        fillIconsMap();
        Integer iconId = gcIcons.get("type_" + type);
        if (iconId != null) {
            return iconId;
        }
        // fallback to traditional if some icon type is not correct
        return gcIcons.get("type_traditional");
    }

    public static int getMarkerIcon(final boolean cache, final String type, final boolean own, final boolean found, final boolean disabled) {
        fillIconsMap();

        if (wpIcons.isEmpty()) {
            wpIcons.put("waypoint", R.drawable.marker_waypoint_waypoint);
            wpIcons.put("flag", R.drawable.marker_waypoint_flag);
            wpIcons.put("pkg", R.drawable.marker_waypoint_pkg);
            wpIcons.put("puzzle", R.drawable.marker_waypoint_puzzle);
            wpIcons.put("stage", R.drawable.marker_waypoint_stage);
            wpIcons.put("trailhead", R.drawable.marker_waypoint_trailhead);
        }

        int icon = -1;
        String iconTxt = null;

        if (cache) {
            if (StringUtils.isNotBlank(type)) {
                if (own) {
                    iconTxt = type + "-own";
                } else if (found) {
                    iconTxt = type + "-found";
                } else if (disabled) {
                    iconTxt = type + "-disabled";
                } else {
                    iconTxt = type;
                }
            } else {
                iconTxt = "traditional";
            }

            if (gcIcons.containsKey(iconTxt)) {
                icon = gcIcons.get(iconTxt);
            } else {
                icon = gcIcons.get("traditional");
            }
        } else {
            if (StringUtils.isNotBlank(type)) {
                iconTxt = type;
            } else {
                iconTxt = "waypoint";
            }

            if (wpIcons.containsKey(iconTxt)) {
                icon = wpIcons.get(iconTxt);
            } else {
                icon = wpIcons.get("waypoint");
            }
        }

        return icon;
    }

    private static void fillIconsMap() {
        if (gcIcons.isEmpty()) {
            gcIcons.put("type_ape", R.drawable.type_ape);
            gcIcons.put("type_cito", R.drawable.type_cito);
            gcIcons.put("type_earth", R.drawable.type_earth);
            gcIcons.put("type_event", R.drawable.type_event);
            gcIcons.put("type_letterbox", R.drawable.type_letterbox);
            gcIcons.put("type_locationless", R.drawable.type_locationless);
            gcIcons.put("type_mega", R.drawable.type_mega);
            gcIcons.put("type_multi", R.drawable.type_multi);
            gcIcons.put("type_traditional", R.drawable.type_traditional);
            gcIcons.put("type_virtual", R.drawable.type_virtual);
            gcIcons.put("type_webcam", R.drawable.type_webcam);
            gcIcons.put("type_wherigo", R.drawable.type_wherigo);
            gcIcons.put("type_mystery", R.drawable.type_mystery);
            gcIcons.put("type_gchq", R.drawable.type_hq);
            // default markers
            gcIcons.put("ape", R.drawable.marker_cache_ape);
            gcIcons.put("cito", R.drawable.marker_cache_cito);
            gcIcons.put("earth", R.drawable.marker_cache_earth);
            gcIcons.put("event", R.drawable.marker_cache_event);
            gcIcons.put("letterbox", R.drawable.marker_cache_letterbox);
            gcIcons.put("locationless", R.drawable.marker_cache_locationless);
            gcIcons.put("mega", R.drawable.marker_cache_mega);
            gcIcons.put("multi", R.drawable.marker_cache_multi);
            gcIcons.put("traditional", R.drawable.marker_cache_traditional);
            gcIcons.put("virtual", R.drawable.marker_cache_virtual);
            gcIcons.put("webcam", R.drawable.marker_cache_webcam);
            gcIcons.put("wherigo", R.drawable.marker_cache_wherigo);
            gcIcons.put("mystery", R.drawable.marker_cache_mystery);
            gcIcons.put("gchq", R.drawable.marker_cache_gchq);
            // own cache markers
            gcIcons.put("ape-own", R.drawable.marker_cache_ape_own);
            gcIcons.put("cito-own", R.drawable.marker_cache_cito_own);
            gcIcons.put("earth-own", R.drawable.marker_cache_earth_own);
            gcIcons.put("event-own", R.drawable.marker_cache_event_own);
            gcIcons.put("letterbox-own", R.drawable.marker_cache_letterbox_own);
            gcIcons.put("locationless-own", R.drawable.marker_cache_locationless_own);
            gcIcons.put("mega-own", R.drawable.marker_cache_mega_own);
            gcIcons.put("multi-own", R.drawable.marker_cache_multi_own);
            gcIcons.put("traditional-own", R.drawable.marker_cache_traditional_own);
            gcIcons.put("virtual-own", R.drawable.marker_cache_virtual_own);
            gcIcons.put("webcam-own", R.drawable.marker_cache_webcam_own);
            gcIcons.put("wherigo-own", R.drawable.marker_cache_wherigo_own);
            gcIcons.put("mystery-own", R.drawable.marker_cache_mystery_own);
            gcIcons.put("gchq-own", R.drawable.marker_cache_gchq_own);
            // found cache markers
            gcIcons.put("ape-found", R.drawable.marker_cache_ape_found);
            gcIcons.put("cito-found", R.drawable.marker_cache_cito_found);
            gcIcons.put("earth-found", R.drawable.marker_cache_earth_found);
            gcIcons.put("event-found", R.drawable.marker_cache_event_found);
            gcIcons.put("letterbox-found", R.drawable.marker_cache_letterbox_found);
            gcIcons.put("locationless-found", R.drawable.marker_cache_locationless_found);
            gcIcons.put("mega-found", R.drawable.marker_cache_mega_found);
            gcIcons.put("multi-found", R.drawable.marker_cache_multi_found);
            gcIcons.put("traditional-found", R.drawable.marker_cache_traditional_found);
            gcIcons.put("virtual-found", R.drawable.marker_cache_virtual_found);
            gcIcons.put("webcam-found", R.drawable.marker_cache_webcam_found);
            gcIcons.put("wherigo-found", R.drawable.marker_cache_wherigo_found);
            gcIcons.put("mystery-found", R.drawable.marker_cache_mystery_found);
            gcIcons.put("gchq-found", R.drawable.marker_cache_gchq_found);
            // disabled cache markers
            gcIcons.put("ape-disabled", R.drawable.marker_cache_ape_disabled);
            gcIcons.put("cito-disabled", R.drawable.marker_cache_cito_disabled);
            gcIcons.put("earth-disabled", R.drawable.marker_cache_earth_disabled);
            gcIcons.put("event-disabled", R.drawable.marker_cache_event_disabled);
            gcIcons.put("letterbox-disabled", R.drawable.marker_cache_letterbox_disabled);
            gcIcons.put("locationless-disabled", R.drawable.marker_cache_locationless_disabled);
            gcIcons.put("mega-disabled", R.drawable.marker_cache_mega_disabled);
            gcIcons.put("multi-disabled", R.drawable.marker_cache_multi_disabled);
            gcIcons.put("traditional-disabled", R.drawable.marker_cache_traditional_disabled);
            gcIcons.put("virtual-disabled", R.drawable.marker_cache_virtual_disabled);
            gcIcons.put("webcam-disabled", R.drawable.marker_cache_webcam_disabled);
            gcIcons.put("wherigo-disabled", R.drawable.marker_cache_wherigo_disabled);
            gcIcons.put("mystery-disabled", R.drawable.marker_cache_mystery_disabled);
            gcIcons.put("gchq-disabled", R.drawable.marker_cache_gchq_disabled);
        }
    }

    public static boolean runNavigation(Activity activity, Resources res, cgSettings settings, final Geopoint coords) {
        return runNavigation(activity, res, settings, coords, null);
    }

    public static boolean runNavigation(Activity activity, Resources res, cgSettings settings, final Geopoint coords, final Geopoint coordsNow) {
        if (activity == null) {
            return false;
        }
        if (settings == null) {
            return false;
        }

        // Google Navigation
        if (settings.useGNavigation == 1) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:ll=" + coords.getLatitude() + "," + coords.getLongitude())));

                return true;
            } catch (Exception e) {
                // nothing
            }
        }

        // Google Maps Directions
        try {
            if (coordsNow != null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?f=d&saddr=" + coordsNow.getLatitude() + "," + coordsNow.getLongitude() +
                                "&daddr=" + coords.getLatitude() + "," + coords.getLongitude())));
            } else {
                activity.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?f=d&daddr=" + coords.getLatitude() + "," + coords.getLongitude())));
            }

            return true;
        } catch (Exception e) {
            // nothing
        }

        Log.i(cgSettings.tag, "cgBase.runNavigation: No navigation application available.");

        if (res != null) {
            ActivityMixin.showToast(activity, res.getString(R.string.err_navigation_no));
        }

        return false;
    }

    public String getMapUserToken(Handler noTokenHandler) {
        final HttpResponse response = request("http://www.geocaching.com/map/default.aspx", "", false);
        final String data = getResponseData(response);
        String usertoken = null;

        if (StringUtils.isNotBlank(data)) {
            final Pattern pattern = Pattern.compile("var userToken[^=]*=[^']*'([^']+)';", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

            final Matcher matcher = pattern.matcher(data);
            while (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    usertoken = matcher.group(1);
                }
            }
        }

        if (noTokenHandler != null && StringUtils.isBlank(usertoken)) {
            noTokenHandler.sendEmptyMessage(0);
        }

        return usertoken;
    }

    public static Double getElevation(final Geopoint coords) {
        try {
            final String uri = "http://maps.googleapis.com/maps/api/elevation/json?sensor=false&locations=" +
                    String.format((Locale) null, "%.6f", coords.getLatitude()) + "," +
                    String.format((Locale) null, "%.6f", coords.getLongitude());

            final JSONObject response = requestJSON(uri);

            if (response == null) {
                return null;
            }

            String status = response.getString("status");

            if (status == null || status.equalsIgnoreCase("OK") == false) {
                return null;
            }

            if (response.has("results")) {
                JSONArray results = response.getJSONArray("results");
                JSONObject result = results.getJSONObject(0);
                return result.getDouble("elevation");
            }
        } catch (Exception e) {
            Log.w(cgSettings.tag, "cgBase.getElevation: " + e.toString());
        }

        return null;
    }

    /**
     * Generate a time string according to system-wide settings (locale, 12/24 hour)
     * such as "13:24".
     *
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public String formatTime(long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_TIME);
    }

    /**
     * Generate a date string according to system-wide settings (locale, date format)
     * such as "20 December" or "20 December 2010". The year will only be included when necessary.
     *
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public String formatDate(long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE);
    }

    /**
     * Generate a date string according to system-wide settings (locale, date format)
     * such as "20 December 2010". The year will always be included, making it suitable
     * to generate long-lived log entries.
     *
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public String formatFullDate(long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_YEAR);
    }

    /**
     * Generate a numeric date string according to system-wide settings (locale, date format)
     * such as "10/20/2010".
     *
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public String formatShortDate(long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_NUMERIC_DATE);
    }

    /**
     * Generate a numeric date and time string according to system-wide settings (locale,
     * date format) such as "7 sept. à 12:35".
     *
     * @param context
     *            a Context
     * @param date
     *            milliseconds since the epoch
     * @return the formatted string
     */
    public static String formatShortDateTime(Context context, long date) {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL);
    }

    /**
     * TODO This method is only needed until the settings are a singleton
     *
     * @return
     */
    public String getUserName() {
        return settings.getUsername();
    }

    /**
     * insert text into the EditText at the current cursor position
     *
     * @param editText
     * @param insertText
     * @param addSpace
     *            add a space character, if there is no whitespace before the current cursor position
     * @param moveCursor
     *            place the cursor after the inserted text
     */
    static void insertAtPosition(final EditText editText, String insertText, final boolean addSpace, final boolean moveCursor) {
        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);

        String content = editText.getText().toString();
        if (start > 0 && !Character.isWhitespace(content.charAt(start - 1))) {
            insertText = " " + insertText;
        }

        editText.getText().replace(start, end, insertText);
        int newCursor = moveCursor ? start + insertText.length() : start;
        editText.setSelection(newCursor, newCursor);
    }
}
