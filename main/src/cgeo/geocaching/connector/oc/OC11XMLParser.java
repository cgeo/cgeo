package cgeo.geocaching.connector.oc;

import cgeo.geocaching.LogEntry;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class OC11XMLParser {

    private static Pattern STRIP_DATE = Pattern.compile("\\+0([0-9]){1}\\:00");

    private static class CacheHolder {
        public cgCache cache;
        public String latitude;
        public String longitude;
    }

    private static class CacheLog {
        public String cacheId;
        public LogEntry logEntry;
    }

    private static class CacheDescription {
        public String cacheId;
        public String shortDesc;
        public String desc;
        public String hint;
    }

    private static Date parseFullDate(final String date) {
        final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String strippedDate = STRIP_DATE.matcher(date).replaceAll("+0$100");
        try {
            return ISO8601DATEFORMAT.parse(strippedDate);
        } catch (ParseException e) {
            Log.e("OC11XMLParser.parseFullDate", e);
        }
        return null;
    }

    private static Date parseDayDate(final String date) {
        final SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String strippedDate = STRIP_DATE.matcher(date).replaceAll("+0$100");
        try {
            return ISO8601DATEFORMAT.parse(strippedDate);
        } catch (ParseException e) {
            Log.e("OC11XMLParser.parseDayDate", e);
        }
        return null;
    }

    private static CacheSize getCacheSize(final String sizeId) {
        int size = Integer.parseInt(sizeId);

        switch (size) {
            case 1:
                return CacheSize.OTHER;
            case 2:
                return CacheSize.MICRO;
            case 3:
                return CacheSize.SMALL;
            case 4:
                return CacheSize.REGULAR;
            case 5:
            case 6:
                return CacheSize.LARGE;
            case 8:
                return CacheSize.VIRTUAL;
            default:
                break;
        }
        return CacheSize.NOT_CHOSEN;
    }

    private static CacheType getCacheType(final String typeId) {
        int type = Integer.parseInt(typeId);
        switch (type) {
            case 1: // Other/unbekannter Cachetyp
                return CacheType.UNKNOWN;
            case 2: // Trad./normaler Cache
                return CacheType.TRADITIONAL;
            case 3: // Multi/Multicache
                return CacheType.MULTI;
            case 4: // Virt./virtueller Cache
                return CacheType.VIRTUAL;
            case 5: // ICam./Webcam-Cache
                return CacheType.WEBCAM;
            case 6: // Event/Event-Cache
                return CacheType.EVENT;
            case 7: // Quiz/Rätselcache
                return CacheType.MYSTERY;
            case 8: // Math/Mathe-/Physikcache
                return CacheType.MYSTERY;
            case 9: // Moving/beweglicher Cache
                return CacheType.UNKNOWN;
            case 10: // Driv./Drive-In
                return CacheType.TRADITIONAL;
        }
        return CacheType.UNKNOWN;
    }

    private static LogType getLogType(final int typeId) {
        switch (typeId) {
            case 1:
                return LogType.FOUND_IT;
            case 2:
                return LogType.DIDNT_FIND_IT;
            case 3:
                return LogType.NOTE;
            case 7:
                return LogType.ATTENDED;
            case 8:
                return LogType.WILL_ATTEND;
        }
        return LogType.UNKNOWN;
    }

    private static void setCacheStatus(final int statusId, final cgCache cache) {

        switch (statusId) {
            case 1:
                cache.setArchived(false);
                cache.setDisabled(false);
                break;
            case 2:
                cache.setArchived(false);
                cache.setDisabled(true);
                break;
            default:
                cache.setArchived(true);
                cache.setDisabled(false);
                break;
        }
    }

    private static void resetCache(final CacheHolder cacheHolder) {
        cacheHolder.cache = new cgCache(null);
        cacheHolder.cache.setReliableLatLon(true);
        cacheHolder.cache.setDescription(StringUtils.EMPTY);
        cacheHolder.latitude = "0.0";
        cacheHolder.longitude = "0.0";
    }

    private static void resetLog(final CacheLog log) {
        log.cacheId = StringUtils.EMPTY;
        log.logEntry = new LogEntry("", 0, LogType.UNKNOWN, "");
    }

    private static void resetDesc(final CacheDescription desc) {
        desc.cacheId = StringUtils.EMPTY;
        desc.shortDesc = StringUtils.EMPTY;
        desc.desc = StringUtils.EMPTY;
        desc.hint = StringUtils.EMPTY;
    }

    public static Collection<cgCache> parseCaches(final InputStream stream) throws IOException {

        final int CACHE_PARSE_LIMIT = 250;

        final Map<String, cgCache> caches = new HashMap<String, cgCache>();

        final CacheHolder cacheHolder = new CacheHolder();
        final CacheLog logHolder = new CacheLog();
        final CacheDescription descHolder = new CacheDescription();

        final RootElement root = new RootElement("oc11xml");
        final Element cacheNode = root.getChild("cache");

        // cache
        cacheNode.setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attributes) {
                resetCache(cacheHolder);
            }

        });

        cacheNode.setEndElementListener(new EndElementListener() {

            @Override
            public void end() {
                cgCache cache = cacheHolder.cache;
                Geopoint coords = new Geopoint(cacheHolder.latitude, cacheHolder.longitude);
                if (StringUtils.isNotBlank(cache.getGeocode())
                        && !coords.equals(Geopoint.ZERO)
                        && !cache.isArchived()
                        && caches.size() < CACHE_PARSE_LIMIT) {
                    cache.setCoords(coords);
                    caches.put(cache.getCacheId(), cache);
                }
            }
        });

        // cache.id
        cacheNode.getChild("id").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                cacheHolder.cache.setCacheId(body);
            }
        });

        // cache.longitude
        cacheNode.getChild("longitude").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                String longitude = body.trim();
                if (StringUtils.isNotBlank(longitude)) {
                    cacheHolder.longitude = longitude;
                }
            }
        });

        // cache.latitude
        cacheNode.getChild("latitude").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                String latitude = body.trim();
                if (StringUtils.isNotBlank(latitude)) {
                    cacheHolder.latitude = latitude;
                }
            }
        });

        // cache.name
        cacheNode.getChild("name").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                final String content = body.trim();
                cacheHolder.cache.setName(content);
            }
        });

        // cache.waypoints[oc]
        cacheNode.getChild("waypoints").setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attrs) {
                if (attrs.getIndex("oc") > -1) {
                    cacheHolder.cache.setGeocode(attrs.getValue("oc"));
                }
                if (attrs.getIndex("gccom") > -1) {
                    String gccode = attrs.getValue("gccom");
                    if (!StringUtils.isBlank(gccode)) {
                        cacheHolder.cache.setDescription(String.format("Listed on geocaching com: <a href=\"http://coord.info/%s\">%s</a><br /><br />", gccode, gccode));
                    }
                }
            }
        });

        // cache.type[id]
        cacheNode.getChild("type").setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attrs) {
                if (attrs.getIndex("id") > -1) {
                    final String typeId = attrs.getValue("id");
                    cacheHolder.cache.setType(getCacheType(typeId));
                }
            }
        });

        // cache.status[id]
        cacheNode.getChild("status").setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attrs) {
                if (attrs.getIndex("id") > -1) {
                    try {
                        final int statusId = Integer.parseInt(attrs.getValue("id"));
                        setCacheStatus(statusId, cacheHolder.cache);
                    } catch (NumberFormatException e) {
                        Log.w(String.format("Failed to parse status of cache '%s'.", cacheHolder.cache.getGeocode()));
                    }
                }
            }
        });

        // cache.size[id]
        cacheNode.getChild("size").setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attrs) {
                if (attrs.getIndex("id") > -1) {
                    final String typeId = attrs.getValue("id");
                    cacheHolder.cache.setSize(getCacheSize(typeId));
                }
            }
        });

        // cache.difficulty
        cacheNode.getChild("difficulty").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                final String content = body.trim();
                cacheHolder.cache.setDifficulty(Float.valueOf(content));
            }
        });

        // cache.terrain
        cacheNode.getChild("terrain").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                final String content = body.trim();
                cacheHolder.cache.setTerrain(Float.valueOf(content));
            }
        });

        // cache.terrain
        cacheNode.getChild("datehidden").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                final String content = body.trim();
                cacheHolder.cache.setHidden(parseFullDate(content));
            }
        });

        // cache.attributes.attribute
        cacheNode.getChild("attributes").getChild("attribute").setEndTextElementListener(new EndTextElementListener() {
            @Override
            public void end(String body) {
                if (StringUtils.isNotBlank(body)) {
                    cacheHolder.cache.getAttributes().add(body.trim());
                }
            }
        });

        // cachedesc
        final Element cacheDesc = root.getChild("cachedesc");

        cacheDesc.setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attributes) {
                resetDesc(descHolder);
            }
        });

        cacheDesc.setEndElementListener(new EndElementListener() {

            @Override
            public void end() {
                final cgCache cache = caches.get(descHolder.cacheId);
                if (cache != null) {
                    cache.setShortdesc(descHolder.shortDesc);
                    cache.setDescription(cache.getDescription() + descHolder.desc);
                    cache.setHint(descHolder.hint);
                }
            }
        });

        // cachedesc.cacheid
        cacheDesc.getChild("cacheid").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                descHolder.cacheId = body;
            }
        });

        // cachedesc.desc
        cacheDesc.getChild("shortdesc").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                final String content = body.trim();
                descHolder.shortDesc = content;
            }
        });

        // cachedesc.desc
        cacheDesc.getChild("desc").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                final String content = body.trim();
                descHolder.desc = content;
            }
        });

        // cachedesc.hint
        cacheDesc.getChild("hint").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                final String content = body.trim();
                descHolder.hint = content;
            }
        });

        // cachelog
        final Element cacheLog = root.getChild("cachelog");

        cacheLog.setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attrs) {
                resetLog(logHolder);
            }
        });

        cacheLog.setEndElementListener(new EndElementListener() {

            @Override
            public void end() {
                final cgCache cache = caches.get(logHolder.cacheId);
                if (cache != null && logHolder.logEntry.type != LogType.UNKNOWN) {
                    cache.getLogs().prepend(logHolder.logEntry);
                    if (logHolder.logEntry.type == LogType.FOUND_IT
                            && StringUtils.equals(logHolder.logEntry.author, Settings.getOCConnectorUserName())) {
                        cache.setFound(true);
                        cache.setVisitedDate(logHolder.logEntry.date);
                    }
                }
            }
        });

        // cachelog.cacheid
        cacheLog.getChild("cacheid").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                logHolder.cacheId = body;
            }
        });

        // cachelog.date
        cacheLog.getChild("date").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                try {
                    logHolder.logEntry.date = parseDayDate(body).getTime();
                } catch (Exception e) {
                    Log.w("Failed to parse log date: " + e.toString());
                }
            }
        });

        // cachelog.logtype
        cacheLog.getChild("logtype").setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attrs) {
                if (attrs.getIndex("id") > -1) {
                    final int typeId = Integer.parseInt(attrs.getValue("id"));
                    logHolder.logEntry.type = getLogType(typeId);
                }
            }
        });

        // cachelog.userid
        cacheLog.getChild("userid").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String finderName) {
                logHolder.logEntry.author = finderName;
            }
        });

        // cachelog.text
        cacheLog.getChild("text").setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String logText) {
                logHolder.logEntry.log = logText;
            }
        });

        try {
            Xml.parse(stream, Xml.Encoding.UTF_8, root.getContentHandler());
            return caches.values();
        } catch (SAXException e) {
            Log.e("Cannot parse .gpx file as oc11xml: could not parse XML - " + e.toString());
            return null;
        }
    }
}
