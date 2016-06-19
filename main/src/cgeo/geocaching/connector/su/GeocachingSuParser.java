package cgeo.geocaching.connector.su;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.LogEntry;
import cgeo.geocaching.models.LogEntry.Builder;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class GeocachingSuParser {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private GeocachingSuParser() {
        // utility class
    }

    /**
     * Collects temporary data until parsing of a single cache is completed, since not all parsed tags or attributes can
     * be stored immediately.
     */
    private static final class Parsed {
        private final StringBuilder description = new StringBuilder();
        public String latitude = null;
        public Builder logBuilder = new LogEntry.Builder();
        public final List<LogEntry> logs = new ArrayList<>();

        void addDescription(final String text) {
            if (StringUtils.isBlank(text)) {
                return;
            }
            if (description.length() > 0) {
                description.append("\n");
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
                            parsed.logBuilder.setLogType(LogType.NOTE);
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if ("id".equalsIgnoreCase(tagname)) {
                            cache.setGeocode("SU" + text);
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
                            cache.setFavoritePoints(Integer.valueOf(StringUtils.trim(text)));
                        } else if ("rating".equalsIgnoreCase(tagname)) {
                            final String trimmed = StringUtils.trim(text);
                            if (StringUtils.isNotEmpty(trimmed)) {
                                cache.setRating(Float.valueOf(trimmed));
                            }
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

    private static boolean isDisabledStatus(final String status) {
        return !("1".equals(status) || "На сайте".equalsIgnoreCase(status));
    }

    private static void storeCache(final Geocache cache, final ArrayList<Geocache> caches, final Parsed parsed) {
        // finalize the data of the cache
        cache.setDescription(parsed.getDescription());
        cache.setDetailedUpdatedNow();

        // save to database
        DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
        DataStore.saveLogs(cache.getGeocode(), parsed.logs);

        // append to search result
        caches.add(cache);
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
