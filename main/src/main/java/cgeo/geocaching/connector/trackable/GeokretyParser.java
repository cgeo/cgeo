package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

class GeokretyParser {

    private GeokretyParser() {
        // Utility class
    }

    private static class GeokretyHandler extends DefaultHandler {
        private static final SynchronizedDateFormat DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd kk:mm", TimeZone.getTimeZone("UTC"), Locale.US);
        private static final SynchronizedDateFormat DATE_FORMAT_SECONDS = new SynchronizedDateFormat("yyyy-MM-dd kk:mm:ss", TimeZone.getTimeZone("UTC"), Locale.US);
        private final List<Trackable> trackables = new ArrayList<>();
        private Trackable trackable;
        private LogEntry.Builder logEntryBuilder;
        private final List<LogEntry> logsEntries = new ArrayList<>();
        private Image.Builder imageBuilder;

        private boolean isMultiline = false;
        private boolean isInMoves = false;
        private boolean isInImages = false;
        private boolean isInComments = false;
        private String content;


        @NonNull
        public final List<Trackable> getTrackables() {
            return trackables;
        }

        @Override
        public final void startElement(final String uri, final String localName, final String qName,
                                       final Attributes attributes) throws SAXException {
            content = "";
            if (localName.equalsIgnoreCase("geokret")) {

                trackable = new Trackable();
                trackable.forceSetBrand(TrackableBrand.GEOKRETY);
                trackables.add(trackable);
                trackable.setSpottedType(Trackable.SPOTTED_OWNER);
            }
            try {
                if (localName.equalsIgnoreCase("geokret")) {
                    final String kretyId = attributes.getValue("id");
                    if (StringUtils.isNumeric(kretyId)) {
                        trackable.setGeocode(GeokretyConnector.geocode(Integer.parseInt(kretyId)));
                    }
                    final String distance = attributes.getValue("dist");
                    if (StringUtils.isNotBlank(distance)) {
                        trackable.setDistance(Float.parseFloat(distance));
                    }
                    final String trackingcode = attributes.getValue("nr");
                    if (StringUtils.isNotBlank(trackingcode)) {
                        trackable.setTrackingcode(trackingcode);
                    }
                    final String kretyType = attributes.getValue("type");
                    if (StringUtils.isNotBlank(kretyType)) {
                        trackable.setType(getType(Integer.parseInt(kretyType)));
                    }
                    final String kretyState = attributes.getValue("state");
                    if (StringUtils.isNotBlank(kretyState)) {
                        trackable.setSpottedType(getSpottedType(Integer.parseInt(kretyState)));
                    }
                    final String waypointCode = attributes.getValue("waypoint");
                    if (StringUtils.isNotBlank(waypointCode)) {
                        trackable.setSpottedName(waypointCode);
                    }
                    final String imageName = attributes.getValue("image");
                    if (StringUtils.isNotBlank(imageName)) {
                        trackable.setImage("https://geokrety.org/obrazki/" + imageName);
                    }
                    final String ownerId = attributes.getValue("owner_id");
                    if (StringUtils.isNotBlank(ownerId)) {
                        trackable.setOwner(CgeoApplication.getInstance().getString(R.string.init_geokrety_userid, ownerId));
                    }
                    final String missing = attributes.getValue("missing");
                    if (StringUtils.isNotBlank(missing)) {
                        trackable.setMissing("1".equalsIgnoreCase(missing));
                    }
                }
                if (localName.equalsIgnoreCase("owner")) {
                    final String ownerId = attributes.getValue("id");
                    if (StringUtils.isNotBlank(ownerId)) {
                        trackable.setOwner(CgeoApplication.getInstance().getString(R.string.init_geokrety_userid, ownerId));
                    }
                }
                if (localName.equalsIgnoreCase("type")) {
                    final String kretyType = attributes.getValue("id");
                    if (StringUtils.isNotBlank(kretyType)) {
                        trackable.setType(getType(Integer.parseInt(kretyType)));
                    }
                }
                if (localName.equalsIgnoreCase("description")) {
                    isMultiline = true;
                }
                // TODO: latitude/longitude could be parsed, but trackable doesn't support it, yet...
                //if (localName.equalsIgnoreCase("position")) {
                //final String latitude = attributes.getValue("latitude");
                //if (StringUtils.isNotBlank(latitude) {
                //    trackable.setLatitude(latitude);
                //}
                //final String longitude = attributes.getValue("longitude");
                //if (StringUtils.isNotBlank(longitude) {
                //    trackable.setLongitude(longitude);
                //}
                //}
                if (localName.equalsIgnoreCase("move")) {
                    logEntryBuilder = new LogEntry.Builder();
                    isInMoves = true;
                }
                if (localName.equalsIgnoreCase("date")) {
                    final String movedDate = attributes.getValue("moved");
                    if (StringUtils.isNotBlank(movedDate)) {
                        logEntryBuilder.setDate(DATE_FORMAT.parse(movedDate).getTime());
                    }
                }
                if (localName.equalsIgnoreCase("user") && !isInComments) {
                    final String userId = attributes.getValue("id");
                    if (StringUtils.isNotBlank(userId)) {
                        logEntryBuilder.setAuthor(CgeoApplication.getInstance().getString(R.string.init_geokrety_userid, userId));
                    }
                }
                if (localName.equalsIgnoreCase("comments")) {
                    isInComments = true;
                }
                if (localName.equalsIgnoreCase("comment")) {
                    isMultiline = true;
                }
                if (localName.equalsIgnoreCase("logtype")) {
                    final String logtype = attributes.getValue("id");
                    logEntryBuilder.setLogType(getLogType(Integer.parseInt(logtype)));
                }
                if (localName.equalsIgnoreCase("images")) {
                    isInImages = true;
                }
                if (localName.equalsIgnoreCase("image")) {
                    imageBuilder = new Image.Builder();
                    final String title = attributes.getValue("title");
                    if (StringUtils.isNotBlank(title)) {
                        imageBuilder.setTitle(title);
                    }
                }
            } catch (final ParseException | NumberFormatException e) {
                Log.e("Parsing GeoKret", e);
            }
        }

        @Override
        public final void endElement(final String uri, final String localName, final String qName)
                throws SAXException {
            try {
                if (localName.equalsIgnoreCase("geokret")) {
                    if (StringUtils.isNotEmpty(content) && StringUtils.isBlank(trackable.getName())) {
                        trackable.setName(content);
                    }

                    // This is a special case. Deal it at the end of the "geokret" parsing (xml close)
                    if (trackable.getSpottedType() == Trackable.SPOTTED_USER) {
                        if (trackable.getDistance() == 0) {
                            trackable.setSpottedType(Trackable.SPOTTED_OWNER);
                            trackable.setSpottedName(trackable.getOwner());
                        } else {
                            trackable.setSpottedName(getLastSpottedUsername(logsEntries));
                        }
                    }

                    trackable.setLogs(logsEntries);
                }
                if (localName.equalsIgnoreCase("name")) {
                    trackable.setName(content);
                }
                if (localName.equalsIgnoreCase("description")) {
                    trackable.setDetails(content);
                    isMultiline = false;
                }
                if (localName.equalsIgnoreCase("owner")) {
                    trackable.setOwner(content);
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("datecreated")) {
                    final Date date = DATE_FORMAT_SECONDS.parse(content);
                    trackable.setReleased(date);
                }
                if (StringUtils.isNotBlank(content) && !isInMoves && (
                        localName.equalsIgnoreCase("distancetravelled") || localName.equalsIgnoreCase("distancetraveled")
                )) {
                    trackable.setDistance(Float.parseFloat(content));
                }
                if (localName.equalsIgnoreCase("images")) {
                    isInImages = false;
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("image")) {
                    if (isInMoves) {
                        imageBuilder.setUrl("https://geokrety.org/obrazki/" + content);
                        logEntryBuilder.addLogImage(imageBuilder.build());
                    } else if (!isInImages) {
                        // TODO: Trackable doesn't support multiple image yet, so ignore other image tags if we're not in moves
                        trackable.setImage("https://geokrety.org/obrazki/" + content);
                    }
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("state")) {
                    trackable.setSpottedType(getSpottedType(Integer.parseInt(content)));
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("missing")) {
                    trackable.setMissing("1".equalsIgnoreCase(content));
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("waypoint")) {
                    trackable.setSpottedName(content);
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("user") && !isInComments) {
                    logEntryBuilder.setAuthor(content);
                }
                if (localName.equalsIgnoreCase("move")) {
                    isInMoves = false;
                    logsEntries.add(logEntryBuilder.build());
                }
                if (localName.equalsIgnoreCase("comments")) {
                    isInComments = false;
                }
                if (localName.equalsIgnoreCase("comment") && !isInComments) {
                    isMultiline = false;
                    logEntryBuilder.setLog(content);
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("wpt")) {
                    logEntryBuilder.setCacheGeocode(content);
                    logEntryBuilder.setCacheName(content);
                }
                if (localName.equalsIgnoreCase("id")) {
                    logEntryBuilder.setId(Integer.parseInt(content));
                }
            } catch (final ParseException | NumberFormatException e) {
                Log.e("Parsing GeoKret", e);
            }
        }

        @Override
        public final void characters(final char[] ch, final int start, final int length)
                throws SAXException {
            final String text = new String(ch, start, length);
            if (isMultiline) {
                content = StringUtils.join(content, text.replaceAll("(\r\n|\n)", "<br />"));
            } else {
                content = StringUtils.trim(text);
            }
        }

        /**
         * Convert states from GK to c:geo spotted types. See: http://geokrety.org/api.php
         *
         * @param state the GK state read from xml
         * @return The spotted types as defined in Trackables
         */
        private static int getSpottedType(final int state) {
            switch (state) {
                case 0: // Dropped
                case 3: // Seen in
                    return Trackable.SPOTTED_CACHE;
                case 1: // Grabbed from
                case 5: // Visiting
                    return Trackable.SPOTTED_USER;
                case 4: // Archived
                    return Trackable.SPOTTED_ARCHIVED;
                //case 2: // A comment (however this case doesn't exists in db)
            }
            return Trackable.SPOTTED_UNKNOWN;
        }

        /**
         * Convert states from GK to c:geo spotted types.
         *
         * @param type the GK Log type
         * @return The LogType
         */
        private static LogType getLogType(final int type) {
            switch (type) {
                case 0: // Dropped
                    return LogType.PLACED_IT;
                case 1: // Grabbed from
                    return LogType.GRABBED_IT;
                case 2: // A comment
                    return LogType.NOTE;
                case 3: // Seen in
                    return LogType.DISCOVERED_IT;
                case 4: // Archived
                    return LogType.ARCHIVE;
                case 5: // Visiting
                    return LogType.VISIT;
            }
            return LogType.UNKNOWN;
        }
    }

    @NonNull
    public static List<Trackable> parse(final InputSource page) {
        if (page != null) {
            try {
                // Create a new instance of the SAX parser
                final SAXParserFactory saxPF = SAXParserFactory.newInstance();
                final SAXParser saxP = saxPF.newSAXParser();
                final XMLReader xmlR = saxP.getXMLReader();

                // Create the Handler to handle each of the XML tags.
                final GeokretyHandler gkXMLHandler = new GeokretyHandler();
                xmlR.setContentHandler(gkXMLHandler);
                xmlR.parse(page);

                return gkXMLHandler.getTrackables();
            } catch (final SAXException | IOException | ParserConfigurationException e) {
                Log.w("Cannot parse GeoKrety", e);
            }
        }
        return Collections.emptyList();
    }

    private static class GeokretyRuchyXmlParser {
        private int gkid;
        private final List<String> errors;
        private String text;

        GeokretyRuchyXmlParser() {
            errors = new ArrayList<>();
            gkid = 0;
        }

        public List<String> getErrors() {
            return errors;
        }

        int getGkid() {
            return gkid;
        }

        @NonNull
        public List<String> parse(final String page) {
            try {
                final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                final XmlPullParser parser = factory.newPullParser();
                parser.setInput(new StringReader(page));

                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    final String tagname = parser.getName();
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if (tagname.equalsIgnoreCase("geokret")) {
                                gkid = Integer.parseInt(parser.getAttributeValue(null, "id"));
                            }
                            break;

                        case XmlPullParser.TEXT:
                            text = parser.getText();
                            break;

                        case XmlPullParser.END_TAG:
                            if (tagname.equalsIgnoreCase("error") && StringUtils.isNotBlank(text)) {
                                errors.add(text);
                            }
                            break;

                        default:
                            break;
                    }
                    eventType = parser.next();
                }

            } catch (XmlPullParserException | IOException e) {
                Log.e("GeokretyRuchyXmlParser: Error Parsing geokret", e);
                errors.add(CgeoApplication.getInstance().getString(R.string.geokrety_parsing_failed));
            }

            return errors;
        }
    }

    @Nullable
    protected static String getType(final int type) {
        switch (type) {
            case 0:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_traditional);
            case 1:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_book_or_media);
            case 2:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_human);
            case 3:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_coin);
            case 4:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_post);
        }
        return null;
    }

    @Nullable
    static ImmutablePair<Integer, List<String>> parseResponse(final String page) {
        if (page != null) {
            try {
                final GeokretyRuchyXmlParser parser = new GeokretyRuchyXmlParser();
                parser.parse(page);
                return new ImmutablePair<>(parser.getGkid(), parser.getErrors());
            } catch (final Exception e) {
                Log.w("Cannot parse response for the GeoKret", e);
            }
        }
        return null;
    }

    /**
     * Determine from the newest logs (ignoring Notes) if the GK is spotted
     * in the hand of someone.
     *
     * @param logsEntries the log entries to analyze
     * @return The spotted username (or unknown)
     */
    static String getLastSpottedUsername(final List<LogEntry> logsEntries) {
        for (final LogEntry log : logsEntries) {
            final LogType logType = log.logType;
            if (logType == LogType.GRABBED_IT || logType == LogType.VISIT) {
                return log.author;
            }
            if (logType != LogType.NOTE) {
                break;
            }
        }
        return CgeoApplication.getInstance().getString(R.string.user_unknown);
    }
}
