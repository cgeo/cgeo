package cgeo.geocaching.connector.su;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.log.LogEntry.Builder;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

public class GeocachingSuParser {

    private static final SynchronizedDateFormat DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd", Locale.US);
    private static final SynchronizedDateFormat DATE_TIME_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private GeocachingSuParser() {
        // utility class
    }

    /**
     * Collects temporary data until parsing of a single cache is completed, since not all parsed tags or attributes can
     * be stored immediately.
     */
    private static final class Parsed {
        public String id;
        private final StringBuilder description = new StringBuilder();
        public String latitude = null;
        public Builder logBuilder = new LogEntry.Builder();
        public final List<LogEntry> logs = new ArrayList<>();
        public String type;

        void addDescription(final String text) {
            if (StringUtils.isBlank(text)) {
                return;
            }
            if (description.length() > 0) {
                description.append('\n');
            }
            description.append(StringUtils.trim(text));
        }

        String getDescription() {
            return description.toString();
        }
    }

    @NonNull
    public static SearchResult parseCaches(final String endTag, final InputStream inputStream) {
        final ArrayList<Geocache> caches = new ArrayList<>();
        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "UTF-8");

            Parsed parsed = new Parsed();
            Geocache cache = createNewCache();

            String text = "";
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                final String tagname = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        // reset text value to correctly indicate empty tags
                        text = "";

                        if ("cache".equalsIgnoreCase(tagname)) {
                            cache = createNewCache();
                            parsed = new Parsed();
                        } else if ("note".equalsIgnoreCase(tagname)) {
                            parsed.logBuilder = new LogEntry.Builder();
                            parsed.logBuilder.setAuthor(parser.getAttributeValue(null, "nick"));
                            parsed.logBuilder.setDate(parseDateTime(parser.getAttributeValue(null, "date")));
                            parsed.logBuilder.setLogType(parseLogType(parser.getAttributeValue(null, "status")));
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if ("id".equalsIgnoreCase(tagname)) {
                            parsed.id = text;
                        } else if ("name".equalsIgnoreCase(tagname)) {
                            cache.setName(text);
                        } else if (endTag.equalsIgnoreCase(tagname)) {
                            storeCache(cache, caches, parsed);
                        } else if ("lat".equalsIgnoreCase(tagname)) {
                            parsed.latitude = text;
                        } else if ("lng".equalsIgnoreCase(tagname)) {
                            cache.setCoords(new Geopoint(parsed.latitude, text));
                        } else if ("nick".equalsIgnoreCase(tagname) || "autor".equalsIgnoreCase(tagname)) {
                            // sic!, "autor", not "author"
                            cache.setOwnerDisplayName(text);
                        } else if ("adesc".equalsIgnoreCase(tagname)) {
                            // description of the area
                            parsed.addDescription(text);
                        } else if ("cdesc".equalsIgnoreCase(tagname)) {
                            // description of the cache task
                            parsed.addDescription(text);
                        } else if ("tpart".equalsIgnoreCase(tagname)) {
                            // description for traditional part (optional, rarely used), where to look for the cache
                            parsed.addDescription(text);
                        } else if ("vpart".equalsIgnoreCase(tagname)) {
                            // virtual question for winter time (or just virtual question for virtual caches)
                            parsed.addDescription(text);
                        } else if ("date".equalsIgnoreCase(tagname)) {
                            cache.setHidden(parseDate(text));
                        } else if ("type".equalsIgnoreCase(tagname) || "ctype".equalsIgnoreCase(tagname)) {
                            // different tags used in single cache details and area search
                            parsed.type = text;
                            cache.setType(parseType(text));
                        } else if ("note".equalsIgnoreCase(tagname)) {
                            parsed.logBuilder.setLog(StringUtils.trim(text));
                            parsed.logs.add(parsed.logBuilder.build());
                        } else if ("img".equalsIgnoreCase(tagname)) {
                            if (text.contains("photos/caches")) {
                                cache.addSpoiler(new Image.Builder().setUrl(text).build());
                            } else {
                                parsed.addDescription("<img src=\"" + text + "\"/><br/>");
                            }
                        } else if ("status".equalsIgnoreCase(tagname)) {
                            cache.setDisabled(isDisabledStatus(text));
                        } else if ("recom".equalsIgnoreCase(tagname)) {
                            cache.setFavoritePoints(Integer.parseInt(StringUtils.trim(text)));
                        } else if ("rating".equalsIgnoreCase(tagname)) {
                            final String trimmed = StringUtils.trim(text);
                            if (StringUtils.isNotEmpty(trimmed)) {
                                cache.setRating(Float.valueOf(trimmed));
                            }
                        } else if ("container".equalsIgnoreCase(tagname)) {
                            // we only have the geocaching.com container sizes, therefore let's move this into the hint
                            final String trimmed = StringUtils.trim(text);
                            if (StringUtils.isNotEmpty(trimmed)) {
                                cache.setHint(trimmed);
                            }
                        } else if ("area_value".equalsIgnoreCase(tagname)) {
                            cache.setTerrain(Float.parseFloat(text));
                        } else if ("cache_value".equalsIgnoreCase(tagname)) {
                            cache.setDifficulty(Float.parseFloat(text));
                        }

                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException | IOException | ParseException e) {
            Log.e("Error parsing geocaching.su data", e);
        }

        return new SearchResult(caches);
    }

    private static LogType parseLogType(final String status) {
        switch (status) {
            case "1":
                return LogType.FOUND_IT;
            case "2":
                return LogType.DIDNT_FIND_IT;
            case "3":
                return LogType.NOTE;
            case "4":
                return LogType.DIDNT_FIND_IT;
            case "5":
                return LogType.OWNER_MAINTENANCE;
            case "6":
                return LogType.OWNER_MAINTENANCE;
            default:
                return LogType.UNKNOWN;
        }
    }

    private static boolean isDisabledStatus(final String status) {
        return !("1".equals(status) || "На сайте".equalsIgnoreCase(status));
    }

    private static void storeCache(final Geocache cache, final ArrayList<Geocache> caches, final Parsed parsed) {
        // finalize the data of the cache
        cache.setGeocode(getGeocode(parsed));
        final String description = parsed.getDescription();
        cache.setDescription(description);

        // differentiate between area search, and detailed request
        if (StringUtils.isNotEmpty(description)) {
            cache.setDetailedUpdatedNow();
            DataStore.saveLogs(cache.getGeocode(), parsed.logs);
        }

        // save to database
        DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));

        // append to search result
        caches.add(cache);
    }

    private static String getGeocode(final Parsed parsed) {
        return parseGeocodePrefix(parsed.type) + parsed.id;
    }

    private static CharSequence parseGeocodePrefix(final String type) {
        switch (type) {
            case "Пошаговый виртуальный":
                return GeocachingSuConnector.PREFIX_MULTISTEP_VIRTUAL;
            case "Традиционный":
                return GeocachingSuConnector.PREFIX_TRADITIONAL;
            case "Виртуальный":
                return GeocachingSuConnector.PREFIX_VIRTUAL;
            case "Сообщение о встрече":
                return GeocachingSuConnector.PREFIX_EVENT;
            case "Пошаговый традиционный":
                return GeocachingSuConnector.PREFIX_MULTISTEP;
            case "Конкурс":
                return GeocachingSuConnector.PREFIX_CONTEST;
            default:
                return "SU"; // fallback solution to not use the numeric id only
        }
    }

    @NonNull
    private static Geocache createNewCache() {
        final Geocache cache = new Geocache();
        cache.setReliableLatLon(true);
        cache.setDetailed(false);
        return cache;
    }

    @NonNull
    private static CacheType parseType(final String text) {
        if (text.equalsIgnoreCase("Традиционный")) {
            return CacheType.TRADITIONAL;
        }
        if (text.equalsIgnoreCase("Виртуальный")) {
            return CacheType.VIRTUAL;
        }
        if (text.equalsIgnoreCase("Сообщение о встрече")) {
            return CacheType.EVENT;
        }
        if (text.equalsIgnoreCase("Конкурс")) {
            return CacheType.EVENT;
        }
        if (text.equalsIgnoreCase("Пошаговый традиционный")) {
            return CacheType.MULTI;
        }
        if (text.equalsIgnoreCase("Пошаговый виртуальный")) {
            return CacheType.VIRTUAL;
        }
        return CacheType.UNKNOWN;
    }

    private static Date parseDate(final String text) throws ParseException {
        return DATE_FORMAT.parse(text);
    }

    private static long parseDateTime(final String text) throws ParseException {
        return DATE_TIME_FORMAT.parse(text).getTime();
    }

}
