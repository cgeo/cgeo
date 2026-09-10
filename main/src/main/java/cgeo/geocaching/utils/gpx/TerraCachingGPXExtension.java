package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.connector.tc.TerraCachingLogType;
import cgeo.geocaching.connector.tc.TerraCachingType;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.html.HtmlUtils;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

public class TerraCachingGPXExtension implements IGPXExtension {

    //Namespaces of extensions
    private static final Set<String> TERRA_NS = Set.of(
            "http://www.TerraCaching.com/GPX/1/0"
    );

    /** TerraCaching extension. */
    @Override
    public void enrichGeocache(final XmlNode wptType, final Geocache cache) {
        final XmlNode terraCache = GPXUtils.gpxNodeChild(wptType, "terracache", TERRA_NS);
        if (terraCache == null) {
            return;
        }
        final String name = GPXUtils.gpxNodeChildText(terraCache, "name", TERRA_NS);
        if (StringUtils.isNotBlank(name)) {
            cache.setName(StringUtils.trim(name));
        }
        final String owner = GPXUtils.gpxNodeChildText(terraCache, "owner", TERRA_NS);
        if (StringUtils.isNotBlank(owner)) {
            cache.setOwnerDisplayName(XmlUtils.validate(owner));
        }
        final String style = GPXUtils.gpxNodeChildText(terraCache, "style", TERRA_NS);
        if (StringUtils.isNotBlank(style)) {
            cache.setType(TerraCachingType.getCacheType(style));
        }
        final String size = GPXUtils.gpxNodeChildText(terraCache, "size", TERRA_NS);
        if (StringUtils.isNotBlank(size)) {
            cache.setSize(CacheSize.getById(size));
        }
        final String country = GPXUtils.gpxNodeChildText(terraCache, "country", TERRA_NS);
        if (StringUtils.isNotBlank(country)) {
            cache.setLocation(StringUtils.trim(country));
        }
        final String state = GPXUtils.gpxNodeChildText(terraCache, "state", TERRA_NS);
        if (StringUtils.isNotBlank(state) && StringUtils.isNotEmpty(state.trim())) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? XmlUtils.validate(state) : state.trim() + ", " + cache.getLocation());
        }
        final String description = GPXUtils.gpxNodeChildText(terraCache, "description", TERRA_NS);
        if (description != null) {
            cache.setDescription(trimHtml(description));
        }
        final String hint = GPXUtils.gpxNodeChildText(terraCache, "hint", TERRA_NS);
        if (hint != null) {
            cache.setHint(HtmlUtils.extractText(hint));
        }
    }

    /** TerraCaching logs ({@code <terracache><logs><log>...}). */
    public List<LogEntry> extractLogs(final XmlNode wptNode, final Geocache cache) {
        final XmlNode terraCache = GPXUtils.gpxNodeChild(wptNode, "terracache", TERRA_NS);
        if (terraCache == null) {
            return null;
        }
        final XmlNode logsNode = GPXUtils.gpxNodeChild(terraCache, "logs", TERRA_NS);
        if (logsNode == null) {
            return null;
        }
        final List<XmlNode> logNodes = GPXUtils.gpxNodeChildren(logsNode, "log");
        if (logNodes == null) {
            return null;
        }
        final List<LogEntry> result = new ArrayList<>();
        for (final XmlNode logNode : logNodes) {
            final LogEntry.Builder builder = new LogEntry.Builder();
            final Integer idText = XmlUtils.toInteger(GPXUtils.gpxNodeAttrValue(logNode, "id", TERRA_NS), null);
            if (idText != null) {
                builder.setId(idText);
            }
            final Date date = GPXUtils.gpxNodeChildDate(logNode, "date", TERRA_NS, null);
            if (date != null) {
                builder.setDate(date.getTime());
            }
            final String typeText = GPXUtils.gpxNodeChildText(logNode, "type", TERRA_NS);
            if (typeText != null) {
                builder.setLogType(TerraCachingLogType.getLogType(XmlUtils.validate(typeText)));
            }
            final String finder = GPXUtils.gpxNodeChildText(logNode, "user", TERRA_NS);
            if (finder != null) {
                builder.setAuthor(XmlUtils.validate(finder));
            }
            final String text = GPXUtils.gpxNodeChildText(logNode, "entry", TERRA_NS);
            if (text != null) {
                builder.setLog(trimHtml(XmlUtils.validate(text)));
            }
            final LogEntry log = builder.build();
            if (log.logType != LogType.UNKNOWN) {
                result.add(log);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static String trimHtml(final String html) {
        return StringUtils.trim(Strings.CS.removeEnd(Strings.CS.removeStart(html, "<br>"), "<br>"));
    }

}
