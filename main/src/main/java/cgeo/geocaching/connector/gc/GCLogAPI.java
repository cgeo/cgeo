package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ImageResult;
import cgeo.geocaching.connector.LogResult;
import cgeo.geocaching.connector.trackable.TrackableBrand;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogTypeTrackable;
import cgeo.geocaching.log.OfflineLogEntry;
import cgeo.geocaching.log.TrackableLogEntry;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.connector.gc.GCAuthAPI.WEBSITE_URL;
import static cgeo.geocaching.connector.gc.GCAuthAPI.httpReq;
import static cgeo.geocaching.connector.gc.GCAuthAPI.websiteReq;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/** Provides methods to handle all aspects of creating, editing and deleting Log entries for GC.com (both caches and trackables) */
public class GCLogAPI {


    private static final String HTML_HEADER_CSRF_TOKEN = "CSRF-Token";

    private GCLogAPI() {
        // Utility class, do not instantiate
    }

    //Generic Format of a "log entry" Request towards GC.com (for cache or trackable) is as follows:
    // (note request to edit existing log is very similar, but this is not supported by c:geo as of now)
    // {
    //   'geocacheReferenceCode': "GCxyz", // Used only for trackable Logs of type RETRIEVED. Contains GCcode of geocache where tb was retrieved from. mandatory!
    //   'images': [], // an array of image GUIDs  (String). Can be used to assign images uploaded previously with log entry
    //   'logDate': logdate, // timestamp, e.g. "2023-09-08T22:31:54.004Z"
    //   'logText': logtext, //string (logtext)
    //   'logType': logtype, //integer. Available types depend on whether log is for cache or tb, and on state of that cache/tb
    //   'trackables': [], //Only used on cache logs to incidate for own inventory what to do with it. Array of object. Example: [{"trackableCode":"TBxyz","trackableLogTypeId":75}]
    //   'updatedCoordinates': null, //unknown, most likely only used for Owner log when setting new header coords
    //   'usedFavoritePoint': false //boolean. Used on cache logs to add a fav point to the cache
    //  }
    //
    //Generic Format of a log entry Reply from gc.com:
    // {"guid":"xyz","logReferenceCode":"GLxyz","dateTimeCreatedUtc":"2023-09-17T14:03:26","dateTimeLastUpdatedUtc":"2023-09-17T14:03:26","logDate":"2023-09-08T12:00:00","logType":4,"images":[],"trackables":[],"cannotDelete":false,"usedFavoritePoint":false}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GCWebLogTrackable {
        //Log Creation Fields
        @JsonProperty("trackableCode")
        String trackableCode; // e.g. "TBxyz"
        @JsonProperty("trackableLogTypeId")
        Integer trackableLogTypeId;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebLogRequest extends GCWebLogBase {
        @JsonProperty("logType")
        Integer logType;
        @JsonProperty("trackables")
        GCWebLogTrackable[] trackables;
        @JsonProperty("usedFavoritePoint")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean usedFavoritePoint;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebLogCsrfRequest {
        @JsonProperty("csrfToken")
        String csrfToken;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebLogResponse extends GCWebLogRequest {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebTrackableLogRequest extends GCWebLogBase {

        //only used for Logs of type RETRIEVED. In this case, field is mandatory
        @JsonProperty("geocacheReferenceCode")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String geocacheReferenceCode;

        @JsonProperty("logType")
        Integer logType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GCWebTrackableLogResponse extends GCWebLogBase {
        @JsonProperty("logType")
        GCWebLogTrackableResponseLogType logType;
    }

    //Contains common fields in JSONs related to Log request and response for both Caches and Trackables
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GCWebLogBase extends HttpResponse {

        // --- common request fields ---

        @JsonProperty("images")
        String[] images; //image GUIDs
        @JsonProperty("logDate")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN)
        Date logDate;
        @JsonProperty("logText")
        String logText;

        @JsonProperty("trackingCode")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String trackingCode; //used only for Trackable Logs

        // --- common response fields ---

        @JsonProperty("guid")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String guid;
        @JsonProperty("logReferenceCode")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String logReferenceCode;
        @JsonProperty("dateTimeCreatedUtc")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN)
        Date dateTimeCreatedUtc;
        @JsonProperty("dateTimeLastUpdatedUtc")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JsonUtils.JSON_LOCAL_TIMESTAMP_PATTERN)
        Date dateTimeLastUpdatedUtc;
        @JsonProperty("cannotDelete")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean cannotDelete;
        @JsonProperty("isArchived")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean isArchived;

    }

    // Response for GC Log Image create/update request:
    // {
    //    "guid": "c7xyz-xyz-xyz-xyz-xyz",
    //    "url": "https://img.geocaching.com/c7xyz-xyz-xyz-xyz-xyz.jpg",
    //    "thumbnailUrl": "https://img.geocaching.com/large/c7xyz-xyz-xyz-xyz-xyz.jpg",
    //    "success": true
    //}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GCWebLogImageResponse extends HttpResponse {
        @JsonProperty("guid")
        String guid;
        @JsonProperty("url")
        String url;
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl;
        @JsonProperty("success")
        Boolean success;

    }

    //Helper JSOn subtypes

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GCWebLogTrackableResponseLogType {
        @JsonProperty("id")
        Integer id;
    }

    //matches a JSOn snippet like: {"id":123,"referenceCode":"GCxyz","name":"somename"}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GCGeocacheReference {
        @JsonProperty("id")
        Integer id;
        @JsonProperty("referenceCode")
        String referenceCode;
        @JsonProperty("name")
        String name;
    }

    //message for log delete
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GCWebLogDelete extends HttpResponse {
        @JsonProperty("reasonText")
        String reasonText;

    }

    public static String getUrlForNewTrackableLog(final String trackableCode) {
        return WEBSITE_URL + "/live/trackable/" + trackableCode + "/log";
    }

    public static String getUrlForNewLog(final String geocode) {
        return WEBSITE_URL + "/live/geocache/" + geocode + "/log";
    }

    public static String getUrlForEditLog(final String geocode, final String serviceLogId) {
        return WEBSITE_URL + "/live/geocache/" + geocode + "/log/" + serviceLogId + "/edit";
    }

    @NonNull
    @WorkerThread
    public static LogResult createLog(
        final String geocode, final OfflineLogEntry logEntry, final Map<String, Trackable> inventory) {
        if (StringUtils.isBlank(logEntry.log)) {
            return generateLogError("GCWebAPI.postLog: No log text given");
        }

        //1.) Call log page and get a valid CSRF Token
        final String csrfToken = getCsrfToken();
        if (csrfToken == null) {
            return generateLogError("Log Post: unable to extract CSRF Token");
        }

        //2,) Fill Log Entry object and post it
        final GCWebLogRequest logEntryRequest = createLogRequest(logEntry, inventory);

        try (GCWebLogResponse response = websiteReq().uri("/api/live/v1/logs/" + geocode + "/geocacheLog")
                .method(HttpRequest.Method.POST)
                .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
                .bodyJson(logEntryRequest)
                .requestJson(GCWebLogResponse.class).blockingGet()) {

            if (response.logReferenceCode == null) {
                return generateLogError("CreateLog: Problem with server, request JSON=[" + HttpRequest.getJsonBody(logEntryRequest) +
                        "], response=[" + response + "]");
            }

            return LogResult.ok(response.logReferenceCode);
        }
    }

    public static LogResult editLog(final String geocode, final LogEntry logEntry) {

        if (StringUtils.isBlank(logEntry.serviceLogId)) {
            return generateLogError("Need a serviceLogId to edit a log entry");
        }

        final String logId = logEntry.serviceLogId;

        //https://www.geocaching.com/live/geocache/GCxyz/log/GLabc/edit
        //1.) Call log edit page and get a valid CSRF Token
        final String csrfToken = getCsrfToken();
        if (csrfToken == null) {
            return generateLogError("Log Post: unable to extract CSRF Token");
        }

        //2,) Fill Log Entry object and PUT it
        final GCWebLogRequest logEntryRequest = createLogRequest(logEntry, null);

        try (GCWebLogResponse response = websiteReq().uri("/api/live/v1/logs/geocacheLog/" + logId)
            .method(HttpRequest.Method.PUT)
            .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
            .bodyJson(logEntryRequest)
            .requestJson(GCWebLogResponse.class).blockingGet()) {

            if (response.logReferenceCode == null) {
                return generateLogError("EditLog: problem with server, request JSON=[" + HttpRequest.getJsonBody(logEntryRequest) +
                    "], response=[" + response + "]");
            }

            return LogResult.ok(response.logReferenceCode);
        }

    }

    public static LogResult deleteLog(final String logId, final String reasonText) {

        //{"reasonText":"Deleting test log"}

        //1.) Call log view page and get a valid CSRF Token
        final String csrfToken = getCsrfToken();
        if (csrfToken == null) {
            return generateLogError("DeleteLog: unable to extract CSRF Token");
        }

        String reason = reasonText;
        if (StringUtils.isBlank(reason)) {
            reason = LocalizationUtils.getString(R.string.cache_log_delete_reason_default);
        }
        final GCWebLogDelete deleteBody = new GCWebLogDelete();
        deleteBody.reasonText = reason;

        //2,) Send a DELETE Request (which is actually a POST)
        try (HttpResponse response = websiteReq().uri("/api/live/v1/logs/geocacheLog/delete/" + logId)
            .method(HttpRequest.Method.POST)
            .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
            .bodyJson(reasonText == null ? null : deleteBody)
            .request().blockingGet()) {

            if (!response.isSuccessful()) {
                return generateLogError("DeleteLog: Problem deleting, response is: " + response);
            }

            return LogResult.ok(logId);
        }

    }

    @WorkerThread
    @NonNull
    public static ImageResult addLogImage(final String logId, final Image image) {
        //1) Get CSRF Token from "Edit Log" page. URL is https://www.geocaching.com/live/log/GLxyz
        final String csrfToken = getCsrfToken();
        if (csrfToken == null) {
            Log.w("Log Image Post: unable to extract CSRF Token in new Log Flow Page");
            return generateImageLogError("No CSRFToken found");
        }

        //2) Create a new "image" attached to the log, uploading only image data
        //   (Do not yet upload name + description, for some reason this results in a server timeout)
        // via POST to https://www.geocaching.com/api/live/v1/logs/GLxyz/images with image payload
        try (GCWebLogImageResponse imgResponse = websiteReq().uri("/api/live/v1/logs/" + logId + "/images")
                .method(HttpRequest.Method.POST)
                .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
                .bodyForm(null, "image", "image/jpeg", image.getFile())
                .requestJson(GCWebLogImageResponse.class).blockingGet()) {
            if (imgResponse.guid == null || imgResponse.url == null) {
                return generateImageLogError("Problem posting image, logId='" + logId + "', response is: " + imgResponse);
            }

            //3) Post the image name + description via PUT
            final ImageResult resp = putChangeImageData(logId, imgResponse.guid + "::", csrfToken, image.getTitle(), image.getDescription());
            if (resp != null) {
                return resp;
            }

            return ImageResult.ok(imgResponse.url, getLogImageId(imgResponse.guid, null));
        }
    }

    public static ImageResult editLogImageData(final String logId, final String logImageId, final String name, final String description) {

        //1) Get CSRF Token from "Edit Log" page. URL is https://www.geocaching.com/live/log/GLxyz
        final String csrfToken = getCsrfToken();
        if (csrfToken == null) {
            Log.w("Log Image Edit: unable to extract CSRF Token in new Log Flow Page");
            return generateImageLogError("No CSRFToken found");
        }

        //2) Send edit request for image data
        final ImageResult  resp = putChangeImageData(logId, logImageId, csrfToken, name, description);
        return resp == null ? ImageResult.ok("", logImageId) : resp;

     }

    public static ImageResult deleteLogImage(final String logId, final String logImageId) {

        //1) Get CSRF Token from "Edit Log" page. URL is https://www.geocaching.com/live/log/GLxyz
        final String csrfToken = getCsrfToken();
        if (csrfToken == null) {
            Log.w("Log Image Edit: unable to extract CSRF Token in new Log Flow Page");
            return generateImageLogError("No CSRFToken found");
        }

        //2,) Send a DELETE Request (which is a POST)
        try (HttpResponse response = websiteReq().uri("/api/live/v1/images/delete/" + logId + "/" + getGuidFrom(logImageId))
            .method(HttpRequest.Method.POST)
            .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
            .request().blockingGet()) {

            if (!response.isSuccessful()) {
                return generateImageLogError("Problem pasting log, response is: " + response);
            }

            return ImageResult.ok("", logImageId);
        }

    }

    @SuppressWarnings("PMD.NPathComplexity") // readability won't be improved upon split
    public static LogResult createLogTrackable(final TrackableLogEntry trackableLog, final Date date, final String log) {
        final String tbCode = trackableLog.geocode;

        if (StringUtils.isBlank(tbCode) || trackableLog == null || trackableLog.getAction() == LogTypeTrackable.DO_NOTHING) {
            generateLogError("Incomplete data for logging: " + trackableLog);
        }

        //1) Get CSRF Token
        final String csrfToken = getCsrfToken();
        if (csrfToken == null) {
            return generateLogError("Log Trackable Post: unable to extract CSRF Token in new Log Flow Page");
        }

        //2,) Fill Trackable Log Entry object and post it
        //  Exemplary JSOn to send: {"images":[],"logDate":"2023-09-08T23:13:36.414Z","logText":"Write a note for a trackable","logType":4,"trackingCode":null}
        final GCWebTrackableLogRequest logEntry = new GCWebTrackableLogRequest();
        logEntry.images = new String[0];
        logEntry.logDate = date;
        logEntry.logType = trackableLog.getAction().gcApiId;
        logEntry.logText = log;
        logEntry.trackingCode = trackableLog.trackingCode;

        // special case: if type is RETRIEVED, we need to fill reference code
        if (trackableLog.getAction() == LogTypeTrackable.RETRIEVED_IT) {
            //1.5) see if we find a geocache reference in the HTML from Trackable "Edit Log" page. URL is https://www.geocaching.com/live/trackable/TBxyz/log
            final String logPageHtml = httpReq().uri(getUrlForNewTrackableLog(tbCode)).request().blockingGet().getBodyString();
            final String geocacheReferenceJson = TextUtils.getMatch(logPageHtml, GCConstants.PATTERN_TB_CURRENT_GEOCACHE_JSON, null);
            if (geocacheReferenceJson != null) {
                final GCGeocacheReference gcRef = JsonUtils.readValueFailSilently("{" + geocacheReferenceJson + "}", GCGeocacheReference.class, null);
                if (gcRef != null) {
                    logEntry.geocacheReferenceCode = gcRef.referenceCode;
                }
            }
        }

        //URL: https://www.geocaching.com/api/live/v1/logs/TBxyz/trackableLog
        try (GCWebTrackableLogResponse response = websiteReq().uri("/api/live/v1/logs/" + tbCode + "/trackableLog")
                .method(HttpRequest.Method.POST)
                .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
                .bodyJson(logEntry)
                .requestJson(GCWebTrackableLogResponse.class).blockingGet()) {
            if (response.logReferenceCode == null) {
                return generateLogError("Problem pasting trackable log, response is: " + response + ", request was:" + HttpRequest.safeGetJsonBody(logEntry));
            }

            return LogResult.ok(response.logReferenceCode);
        }


    }

    public static LogResult deleteLogTrackable(final String logId) {

        //1.) Call log view page and get a valid CSRF Token
        final String csrfToken = getCsrfToken();
        if (csrfToken == null) {
            return generateLogError("DeleteLogTrackable: unable to extract CSRF Token");
        }

        //2,) Send a DELETE Request (which is actually a POST)
        try (HttpResponse response = websiteReq().uri("/api/live/v1/logs/trackableLog/delete/" + logId)
            .method(HttpRequest.Method.POST)
            .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
            .request().blockingGet()) {

            if (!response.isSuccessful()) {
                return generateLogError("DeleteLogTrackable: Problem deleting, response is: " + response);
            }

            return LogResult.ok(logId);
        }

    }

    public static String getLogImageId(final String imageGuid, final String imageId) {
        return (imageGuid == null ? "" : imageGuid) + "::" + (imageId == null ? "" : imageId);
    }

    public static String getGuidFrom(final String logImageId) {
        return tokenizeLogImageId(logImageId)[0];
    }

    public static String getImageIdFrom(final String logImageId) {
        return tokenizeLogImageId(logImageId)[1];
    }

    private static GCWebLogRequest createLogRequest(final LogEntry logEntry, @Nullable final Map<String, Trackable> inventory) {

        final OfflineLogEntry offlineLogEntry = logEntry instanceof OfflineLogEntry ? (OfflineLogEntry) logEntry : null;

        final GCWebLogRequest logEntryRequest = new GCWebLogRequest();
        logEntryRequest.images = CollectionStream.of(logEntry.logImages).filter(img -> img.serviceImageId != null).map(img -> getGuidFrom(img.serviceImageId)).toArray(String.class);
        logEntryRequest.logDate = new Date(logEntry.date);
        logEntryRequest.logType = logEntry.logType.id;
        logEntryRequest.logText = logEntry.log;
        logEntryRequest.trackables = offlineLogEntry == null ? new GCWebLogTrackable[0] : CollectionStream.of(offlineLogEntry.inventoryActions.entrySet())
            .filter(a ->
                inventory != null && inventory.containsKey(a.getKey()) && inventory.get(a.getKey()).getBrand() == TrackableBrand.TRAVELBUG)
            .map(entry -> {
                final GCWebLogTrackable tLog = new GCWebLogTrackable();
                tLog.trackableCode = entry.getKey();
                tLog.trackableLogTypeId = entry.getValue().gcApiId;
                return tLog;
        }).toArray(GCWebLogTrackable.class);
        logEntryRequest.usedFavoritePoint = !Settings.isGCPremiumMember() ? null :
                (offlineLogEntry != null && offlineLogEntry.favorite); //not used by web page, but seems to work

        return logEntryRequest;
    }

    private static ImageResult putChangeImageData(final String logId, final String logImageId, final String csrfToken, final String name, final String description) {
        //3) Post the image name + description via PUT
        // URL like: https://www.geocaching.com/api/live/v1/images/GLxyz/c7xyz-xyz-xyz-xyz-xyz/replace (PUT)
        final Parameters params = new Parameters();
        if (!StringUtils.isBlank(name)) {
            params.put("name", name);
        }
        if (!StringUtils.isBlank(description)) {
            params.put("description", description);
        }

        if (!params.isEmpty()) {
            //We can reuse same CSRF-Token in this second request
            try (GCWebLogImageResponse putImgResponse = websiteReq().uri("/api/live/v1/images/" + logId + "/" + getGuidFrom(logImageId) + "/replace")
                .method(HttpRequest.Method.PUT)
                .headers(HTML_HEADER_CSRF_TOKEN, csrfToken)
                //.bodyForm(params, "image", "image/jpeg", image.getFile())
                .bodyForm(params, null, null, null)
                .requestJson(GCWebLogImageResponse.class).blockingGet()) {
                if (putImgResponse.url == null) {
                    return generateImageLogError("Problem putting image: " + putImgResponse);
                }
            }
        }

        return null;
    }


    private static String[] tokenizeLogImageId(final String logImageId) {
        if (logImageId == null) {
            return new String[] { "", ""};
        }
        final String[] tokens = logImageId.split("::");
        return new String[]{ tokens.length < 1 ? "" : tokens[0], tokens.length < 2 ? "" : tokens[1]};
    }

    private static LogResult generateLogError(final String errorMsg) {
        final String msg = "LOG ERROR(user=" + Settings.getUserName() + "):" + errorMsg;
        Log.w(msg);
        return LogResult.error(StatusCode.LOG_POST_ERROR, msg, null);
    }

    private static ImageResult generateImageLogError(final String errorMsg) {
        final String msg = "LOG IMAGE ERROR(user=" + Settings.getUserName() + "):" + errorMsg;
        Log.w(msg);
        return ImageResult.error(StatusCode.LOGIMAGE_POST_ERROR, msg, null);
    }

    private static String getCsrfToken() {
        final GCWebLogCsrfRequest csrfResponse = websiteReq().uri("/api/auth/csrf")
                .method(HttpRequest.Method.GET)
                .requestJson(GCWebLogCsrfRequest.class)
                .blockingGet();
        return StringUtils.isBlank(csrfResponse.csrfToken) ? null : csrfResponse.csrfToken;
    }
}
