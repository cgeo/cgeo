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
import java.util.Locale;

public class GeocachingSuParser {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    private GeocachingSuParser() {
        // utility class
    }

    @NonNull
    public static SearchResult parseCaches(final String endTag, final InputStream inputStream) {
        final ArrayList<Geocache> caches = new ArrayList<>();
        try {
            final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, "UTF-8");

            String text = "";
            Geocache cache = createNewCache();
            String lattitude = "";
            Builder logEntry = new LogEntry.Builder();
            ArrayList<LogEntry> logs = new ArrayList<>();

            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                final String tagname = parser.getName();
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("cache".equalsIgnoreCase(tagname)) {
                            cache = createNewCache();
                            logs = new ArrayList<>();
                        } else if ("note".equalsIgnoreCase(tagname)) {
                            logEntry = new LogEntry.Builder();
                            logEntry.setAuthor(parser.getAttributeValue(null, "nick"));
                            logEntry.setDate(parseDateTime(parser.getAttributeValue(null, "date")));
                            logEntry.setLogType(LogType.NOTE);
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
                            DataStore.saveCache(cache, EnumSet.of(SaveFlag.DB));
                            DataStore.saveLogs(cache.getGeocode(), logs);
                            caches.add(cache);
                        } else if ("lat".equalsIgnoreCase(tagname)) {
                            lattitude = text;
                        } else if ("lng".equalsIgnoreCase(tagname)) {
                            cache.setCoords(new Geopoint(lattitude, text));
                        } else if ("nick".equalsIgnoreCase(tagname)) {
                            cache.setOwnerDisplayName(text);
                        } else if ("adesc".equalsIgnoreCase(tagname)) {
                            cache.setDescription(text);
                            cache.setDetailedUpdatedNow();
                        } else if ("date".equalsIgnoreCase(tagname)) {
                            cache.setHidden(parseDate(text));
                        } else if ("type".equalsIgnoreCase(tagname)) {
                            cache.setType(parseType(text));
                        } else if ("note".equalsIgnoreCase(tagname)) {
                            logEntry.setLog(StringUtils.trim(text));
                            logs.add(logEntry.build());
                        } else if ("img".equalsIgnoreCase(tagname)) {
                            cache.addSpoiler(new Image.Builder().setUrl(text).build());
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
