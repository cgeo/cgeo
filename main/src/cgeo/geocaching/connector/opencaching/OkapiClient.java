package cgeo.geocaching.connector.opencaching;

import cgeo.geocaching.Parameters;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgImage;
import cgeo.geocaching.cgLog;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.geopoint.GeopointParser;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.text.Html;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final public class OkapiClient {
    private static final String CACHE_VOTES = "rating_votes";
    private static final String CACHE_NOTFOUNDS = "notfounds";
    private static final String CACHE_FOUNDS = "founds";
    private static final String CACHE_HIDDEN = "date_hidden";
    private static final String CACHE_LATEST_LOGS = "latest_logs";
    private static final String CACHE_IMAGE_URL = "url";
    private static final String CACHE_IMAGE_CAPTION = "caption";
    private static final String CACHE_IMAGE_IS_SPOILER = "is_spoiler";
    private static final String CACHE_IMAGES = "images";
    private static final String CACHE_HINT = "hint";
    private static final String CACHE_DESCRIPTION = "description";
    private static final String CACHE_RECOMMENDATIONS = "recommendations";
    private static final String CACHE_RATING = "rating";
    private static final String CACHE_TERRAIN = "terrain";
    private static final String CACHE_DIFFICULTY = "difficulty";
    private static final String CACHE_OWNER = "owner";
    private static final String CACHE_STATUS = "status";
    private static final String CACHE_TYPE = "type";
    private static final String CACHE_LOCATION = "location";
    private static final String CACHE_NAME = "name";
    private static final String CACHE_CODE = "code";

    private static final String LOG_TYPE = "type";
    private static final String LOG_COMMENT = "comment";
    private static final String LOG_DATE = "date";
    private static final String LOG_USER = "user";

    private static final String USER_USERNAME = "username";

    private static final String SERVICE_CACHE = "/okapi/services/caches/geocache";
    private static final String SERVICE_CACHE_FIELDS = "code|name|location|type|status|owner|founds|notfounds|size|difficulty|terrain|rating|rating_votes|recommendations|description|hint|images|latest_logs|date_hidden";

    public static cgCache getCache(final String geoCode) {
        final Parameters params = new Parameters("cache_code", geoCode, "fields", SERVICE_CACHE_FIELDS);
        final JSONObject data = request(geoCode, SERVICE_CACHE, params);

        if (data == null) {
            return null;
        }

        final cgCache cache = parseCache(data);

        cache.updated = new Date().getTime();
        cache.detailedUpdate = new Date().getTime();

        return cache;
    }

    private static cgCache parseCache(final JSONObject response) {
        final cgCache cache = new cgCache();
        try {
            cache.geocode = response.getString(CACHE_CODE);
            cache.name = response.getString(CACHE_NAME);
            // not used: names
            setLocation(cache, response.getString(CACHE_LOCATION));
            cache.type = getCacheType(response.getString(CACHE_TYPE));

            final String status = response.getString(CACHE_STATUS);
            cache.disabled = status.equalsIgnoreCase("Temporarily unavailable");
            cache.archived = status.equalsIgnoreCase("Archived");

            // not used: url
            final JSONObject owner = response.getJSONObject(CACHE_OWNER);
            cache.owner = parseUser(owner);

            cache.logCounts.put(cgBase.LOG_FOUND_IT, response.getInt(CACHE_FOUNDS));
            cache.logCounts.put(cgBase.LOG_DIDNT_FIND_IT, response.getInt(CACHE_NOTFOUNDS));
            cache.size = getCacheSize(response);
            cache.difficulty = (float) response.getDouble(CACHE_DIFFICULTY);
            cache.terrain = (float) response.getDouble(CACHE_TERRAIN);
            if (response.has(CACHE_RATING) && !isNull(response.getString(CACHE_RATING))) {
                cache.rating = (float) response.getDouble(CACHE_RATING);
            }
            cache.votes = response.getInt(CACHE_VOTES);

            cache.favouriteCnt = response.getInt(CACHE_RECOMMENDATIONS);
            // not used: req_password
            cache.description = response.getString(CACHE_DESCRIPTION);
            cache.hint = Html.fromHtml(response.getString(CACHE_HINT)).toString();
            // not used: hints

            final JSONArray images = response.getJSONArray(CACHE_IMAGES);
            if (images != null) {
                JSONObject imageResponse;
                cgImage image;
                for (int i = 0; i < images.length(); i++) {
                    imageResponse = images.getJSONObject(i);
                    if (imageResponse.getBoolean(CACHE_IMAGE_IS_SPOILER)) {
                        image = new cgImage();
                        image.title = imageResponse.getString(CACHE_IMAGE_CAPTION);
                        image.url = absoluteUrl(imageResponse.getString(CACHE_IMAGE_URL), cache.geocode);
                        if (cache.spoilers == null) {
                            cache.spoilers = new ArrayList<cgImage>();
                        }
                        cache.spoilers.add(image);
                    }
                }
            }

            // not used: attrnames
            cache.logs = parseLogs(response.getJSONArray(CACHE_LATEST_LOGS));
            cache.hidden = parseDate(response.getString(CACHE_HIDDEN));

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return cache;
    }

    private static String absoluteUrl(String url, String geocode) {
        final Uri uri = Uri.parse(url);

        if (!uri.isAbsolute()) {
            final IConnector connector = ConnectorFactory.getConnector(geocode);
            return "http://" + connector.getHost() + "/" + url;
        }
        return url;
    }

    private static boolean isNull(String string) {
        return string.equalsIgnoreCase("null");
    }

    private static String parseUser(JSONObject user) throws JSONException {
        return user.getString(USER_USERNAME);
    }

    private static List<cgLog> parseLogs(JSONArray logsJSON) {
        List<cgLog> result = null;
        for (int i = 0; i < logsJSON.length(); i++) {
            try {
                JSONObject logResponse = logsJSON.getJSONObject(i);
                cgLog log = new cgLog();
                log.date = parseDate(logResponse.getString(LOG_DATE)).getTime();
                log.log = logResponse.getString(LOG_COMMENT).trim();
                log.type = parseLogType(logResponse.getString(LOG_TYPE));
                log.author = parseUser(logResponse.getJSONObject(LOG_USER));
                if (result == null) {
                    result = new ArrayList<cgLog>();
                }
                result.add(log);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        return result;
    }

    private static int parseLogType(String logType) {
        if ("Found it".equalsIgnoreCase(logType)) {
            return cgBase.LOG_FOUND_IT;
        }
        else if ("Didn't find it".equalsIgnoreCase(logType)) {
            return cgBase.LOG_DIDNT_FIND_IT;
        }
        return cgBase.LOG_NOTE;
    }

    private static Date parseDate(final String date) {
        final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault());
        final String strippedDate = date.replaceAll("\\+0([0-9]){1}\\:00", "+0$100");
        try {
            return ISO8601DATEFORMAT.parse(strippedDate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private static void setLocation(final cgCache cache, final String location) {
        final String latitude = StringUtils.substringBefore(location, "|");
        final String longitude = StringUtils.substringAfter(location, "|");
        // FIXME: the next line should be a setter at cgCache
        cache.coords = GeopointParser.parse(latitude, longitude);
    }

    private static CacheSize getCacheSize(final JSONObject response) {
        double size = 0;
        try {
            size = response.getDouble("size");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        switch ((int) Math.round(size)) {
            case 1:
                return CacheSize.MICRO;
            case 2:
                return CacheSize.SMALL;
            case 3:
                return CacheSize.REGULAR;
            case 4:
                return CacheSize.LARGE;
            case 5:
                return CacheSize.LARGE;
            default:
                break;
        }
        return CacheSize.NOT_CHOSEN;
    }

    private static String getCacheType(final String type) {
        if (type.equalsIgnoreCase("Traditional")) {
            return "traditional";
        } else if (type.equalsIgnoreCase("Multi")) {
            return "multi";
        } else if (type.equalsIgnoreCase("Quiz")) {
            return "mystery";
        } else if (type.equalsIgnoreCase("Virtual")) {
            return "virtual";
        }
        return "other";
    }

    private static JSONObject request(final String geoCode, final String service, final Parameters params) {
        final IConnector connector = ConnectorFactory.getConnector(geoCode);
        if (connector == null) {
            return null;
        }
        if (!(connector instanceof ApiOpenCachingConnector)) {
            return null;
        }

        final String uri = "http://" + connector.getHost() + service;
        ((ApiOpenCachingConnector) connector).addAuthentication(params);
        return cgBase.requestJSON(uri, params);
    }
}
