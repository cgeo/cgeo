package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.enumerations.CacheAttribute;
import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.xml.XmlNode;
import cgeo.geocaching.utils.xml.XmlUtils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class GroundspeakGPXExtension implements IGPXExtension {

    //Namespaces of extensions
    private static final Set<String> GROUNDSPEAK_NS = Set.of(
            "http://www.groundspeak.com/cache/1/1", // PQ 1.1
            "http://www.groundspeak.com/cache/1/0/1", // PQ 1.0.1
            "http://www.groundspeak.com/cache/1/0" // PQ 1.0
    );

    /**
     * Groundspeak cache extension, used by geocaching.com pocket queries and most third-party tools (GSAK, ...).
     * Schema/namespace (any of 3 historic versions, unified here by local name only): PQ 1.1
     * {@code http://www.groundspeak.com/cache/1/1}, PQ 1.0.1 {@code http://www.groundspeak.com/cache/1/0/1},
     * PQ 1.0 {@code http://www.groundspeak.com/cache/1/0}. Element {@code <cache>}.
     */
    @Override
    public void enrichGeocache(final XmlNode wptNode, final Geocache cache) {
        final XmlNode gcCache = GPXUtils.gpxNodeChild(wptNode, "cache", GROUNDSPEAK_NS);
        if (gcCache == null) {
            return;
        }
        final String id = GPXUtils.gpxNodeAttrValue(gcCache, "id", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(id)) {
            cache.setCacheId(id);
        }
        final String archived = GPXUtils.gpxNodeAttrValue(gcCache, "archived", GROUNDSPEAK_NS);
        if (archived != null) {
            cache.setArchived("true".equalsIgnoreCase(archived));
        }
        final String available = GPXUtils.gpxNodeAttrValue(gcCache, "available", GROUNDSPEAK_NS);
        if (available != null) {
            cache.setDisabled(!"true".equalsIgnoreCase(available));
        }

        final String name = GPXUtils.gpxNodeChildText(gcCache, "name", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(name)) {
            cache.setName(XmlUtils.validate(name));
        }
        final String owner = GPXUtils.gpxNodeChildText(gcCache, "owner", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(owner)) {
            cache.setOwnerUserId(XmlUtils.validate(owner));
        }
        final String placedBy = GPXUtils.gpxNodeChildText(gcCache, "placed_by", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(placedBy)) {
            cache.setOwnerDisplayName(XmlUtils.validate(placedBy));
        }
        final String gcType = GPXUtils.gpxNodeChildText(gcCache, "type", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(gcType)) {
            String body = XmlUtils.validate(gcType);
            if (body.startsWith("Geocache|")) {
                body = StringUtils.substringAfter(body, "Geocache|").trim();
            }
            cache.setType(CacheType.getByPattern(body));
        }
        final String container = GPXUtils.gpxNodeChildText(gcCache, "container", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(container)) {
            cache.setSize(CacheSize.getById(XmlUtils.validate(container)));
        }
        final Float difficulty = GPXUtils.gpxNodeChildFloat(gcCache, "difficulty", GROUNDSPEAK_NS, null);
        if (difficulty != null) {
            cache.setDifficulty(difficulty);
        }
        final Float terrain = GPXUtils.gpxNodeChildFloat(gcCache, "terrain", GROUNDSPEAK_NS, null);
        if (terrain != null) {
            cache.setTerrain(terrain);
        }
        final String country = GPXUtils.gpxNodeChildText(gcCache, "country", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(country)) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? XmlUtils.validate(country) : cache.getLocation() + ", " + country.trim());
        }
        final String state = GPXUtils.gpxNodeChildText(gcCache, "state", GROUNDSPEAK_NS);
        if (StringUtils.isNotBlank(state) && StringUtils.isNotEmpty(state.trim())) {
            cache.setLocation(StringUtils.isBlank(cache.getLocation()) ? XmlUtils.validate(state) : state.trim() + ", " + cache.getLocation());
        }
        final String hints = GPXUtils.gpxNodeChildText(gcCache, "encoded_hints", GROUNDSPEAK_NS);
        if (hints != null) {
            cache.setHint(XmlUtils.validate(hints));
        }
        final String shortDesc = GPXUtils.gpxNodeChildText(gcCache, "short_description", GROUNDSPEAK_NS);
        if (shortDesc != null) {
            cache.setShortDescription(XmlUtils.validate(shortDesc));
        }
        final String longDesc = GPXUtils.gpxNodeChildText(gcCache, "long_description", GROUNDSPEAK_NS);
        if (longDesc != null) {
            cache.setDescription(XmlUtils.validate(longDesc));
        }

        parseGroundspeakAttributes(gcCache, cache);
        parseGroundspeakTravelbugs(gcCache, cache);
    }

    @Nullable
    @Override
    public List<LogEntry> extractLogs(final XmlNode wptNode, final Geocache cache) {
        final XmlNode gcNode = GPXUtils.gpxNodeChild(wptNode, "cache", GROUNDSPEAK_NS);
        if (gcNode == null) {
            return null;
        }
        final boolean gcConnector = ConnectorFactory.getConnector(cache.getGeocode()) instanceof GCConnector;
        final XmlNode logsNode = GPXUtils.gpxNodeChild(gcNode, "logs", GROUNDSPEAK_NS);
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
            final String idText = GPXUtils.gpxNodeAttrValue(logNode, "id", GROUNDSPEAK_NS);
            if (idText != null) {
                try {
                    builder.setId(Integer.parseInt(idText.trim()));
                    if (gcConnector) {
                        builder.setServiceLogId(GCUtils.logIdToLogCode(builder.getId()));
                    }
                } catch (final NumberFormatException ignored) {
                    // ignore malformed id
                }
            }
            final Date date = GPXUtils.gpxNodeChildDate(logNode, "date", GROUNDSPEAK_NS, null);
            if (date != null) {
                builder.setDate(date.getTime());
            }
            final String typeText = GPXUtils.gpxNodeChildText(logNode, "type", GROUNDSPEAK_NS);
            if (typeText != null) {
                builder.setLogType(LogType.getByType(XmlUtils.validate(typeText)));
            }
            final String finder = GPXUtils.gpxNodeChildText(logNode, "finder", GROUNDSPEAK_NS);
            if (finder != null) {
                builder.setAuthor(XmlUtils.validate(finder));
            }
            final String text = GPXUtils.gpxNodeChildText(logNode, "text", GROUNDSPEAK_NS);
            if (text != null) {
                builder.setLog(XmlUtils.validate(text));
            }
            final LogEntry log = builder.build();
            if (log.logType != LogType.UNKNOWN) {
                result.add(log);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private void parseGroundspeakAttributes(final XmlNode gcCache, final Geocache cache) {
        final XmlNode attributes = GPXUtils.gpxNodeChild(gcCache, "attributes", GROUNDSPEAK_NS);
        if (attributes == null) {
            return;
        }
        final List<XmlNode> attributeNodes = GPXUtils.gpxNodeChildren(attributes, "attribute");
        if (attributeNodes == null) {
            return;
        }
        final List<String> result = new ArrayList<>();
        for (final XmlNode attributeNode : attributeNodes) {
            final String idText = GPXUtils.gpxNodeAttrValue(attributeNode, "id", GROUNDSPEAK_NS);
            final String incText = GPXUtils.gpxNodeAttrValue(attributeNode, "inc", GROUNDSPEAK_NS);
            if (idText == null || incText == null) {
                continue;
            }
            try {
                final int attributeId = Integer.parseInt(idText.trim());
                final boolean active = Integer.parseInt(incText.trim()) != 0;
                final CacheAttribute attribute = CacheAttribute.getById(attributeId);
                if (attribute != null) {
                    result.add(attribute.getValue(active));
                }
            } catch (final NumberFormatException ignored) {
                // ignore malformed attribute entries
            }
        }
        cache.setAttributes(result);
    }

    private void parseGroundspeakTravelbugs(final XmlNode gcCache, final Geocache cache) {
        final XmlNode travelbugs = GPXUtils.gpxNodeChild(gcCache, "travelbugs", GROUNDSPEAK_NS);
        if (travelbugs == null) {
            return;
        }
        final List<XmlNode> tbNodes = GPXUtils.gpxNodeChildren(travelbugs, "travelbug");
        if (tbNodes == null) {
            return;
        }
        for (final XmlNode tbNode : tbNodes) {
            final Trackable trackable = new Trackable();
            final String ref = GPXUtils.gpxNodeAttrValue(tbNode, "ref", GROUNDSPEAK_NS);
            if (ref != null) {
                trackable.setGeocode(ref);
            }
            final String tbName = GPXUtils.gpxNodeChildText(tbNode, "name", GROUNDSPEAK_NS);
            if (tbName != null) {
                trackable.setName(XmlUtils.validate(tbName));
            }
            if (StringUtils.isNotBlank(trackable.getGeocode()) && StringUtils.isNotBlank(trackable.getName())) {
                cache.addInventoryItem(trackable);
            }
        }
    }

}
