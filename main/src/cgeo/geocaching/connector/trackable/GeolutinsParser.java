package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import androidx.annotation.NonNull;

import java.io.IOException;
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
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class GeolutinsParser {

    private GeolutinsParser() {
        // Utility class
    }

    static class GeolutinsHandler extends DefaultHandler {
        private static final SynchronizedDateFormat DATE_FORMAT = new SynchronizedDateFormat("dd/MM/yyyy", TimeZone.getTimeZone("UTC"), Locale.US);

        private final List<Trackable> trackables = new ArrayList<>();
        private Trackable trackable;
        private LogEntry.Builder logEntryBuilder;
        private final List<LogEntry> logsEntries = new ArrayList<>();

        private String content;
        private boolean isMultiline = false;
        private boolean isInApparition;

        @NonNull
        public final List<Trackable> getTrackables() {
            return trackables;
        }

        @Override
        public final void startElement(final String uri, final String localName, final String qName,
                                       final Attributes attributes) throws SAXException {
            content = "";
            if (localName.equalsIgnoreCase("geolutin")) {
                trackable = new Trackable();
                trackable.forceSetBrand(TrackableBrand.GEOLUTINS);
                trackables.add(trackable);
                trackable.setSpottedType(Trackable.SPOTTED_OWNER);
            }
            if (localName.equalsIgnoreCase("apparition_disparition")) {
                logEntryBuilder = new LogEntry.Builder();
                isInApparition = true;
            }
            if (localName.equalsIgnoreCase("description")) {
                isMultiline = true;
            }
        }

        @Override
        public final void endElement(final String uri, final String localName, final String qName)
                throws SAXException {
            try {
                if (localName.equalsIgnoreCase("geolutin_id")) {
                    trackable.setGeocode(content);
                }
                if (localName.equalsIgnoreCase("nom")) {
                    trackable.setName(content);
                }
                if (localName.equalsIgnoreCase("description")) {
                    trackable.setDetails(content);
                    isMultiline = false;
                }
                if (localName.equalsIgnoreCase("esprit_nom")) {
                    if (isInApparition) {
                        logEntryBuilder.setAuthor(content);
                    } else {
                        trackable.setOwner(content);
                    }
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("date_naissance")) {
                    final Date date = DATE_FORMAT.parse(content);
                    trackable.setReleased(date);
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("distance_parcourue")) {
                    trackable.setDistance(Float.parseFloat(content));
                }
                if (localName.equalsIgnoreCase("date_apparition_disparition")) {
                    logEntryBuilder.setDate(DATE_FORMAT.parse(content).getTime());
                }
                if (localName.equalsIgnoreCase("commentaires")) {
                    logEntryBuilder.setLog(content);
                }
                if (localName.equalsIgnoreCase("type")) {
                    logEntryBuilder.setLogType(getLogType(content));
                }

                if (localName.equalsIgnoreCase("geolutin")) {
                    trackable.setLogs(logsEntries);

                    // manage spotted field
                    if (!logsEntries.isEmpty()) {
                        // retrieve the first logEntry
                        final LogEntry lastLog = logsEntries.get(0);
                        if (lastLog.getType() == LogType.PLACED_IT) {
                            // it's in a cache
                            trackable.setSpottedType(Trackable.SPOTTED_CACHE);
                            trackable.setSpottedName(lastLog.cacheName);
                        } else if (lastLog.getType() == LogType.RETRIEVED_IT) {
                            trackable.setSpottedName(lastLog.author);
                            // it's in someone hands
                            trackable.setSpottedType(Trackable.SPOTTED_USER);
                            trackable.setSpottedName(lastLog.author);
                        } else {
                            Log.w("GeolutinsHandler.endElement unknown logtype:" + lastLog.getType());
                        }
                    }
                }
                if (localName.equalsIgnoreCase("apparition_disparition")) {
                    isInApparition = false;
                    logsEntries.add(logEntryBuilder.build());
                }
            } catch (final ParseException | NumberFormatException e) {
                Log.w("Parsing GeoLutins", e);
            }
        }

        @Override
        public final void characters(final char[] ch, final int start, final int length)
                throws SAXException {
            final String text = StringUtils.trim(new String(ch, start, length));
            content = isMultiline ? StringUtils.join(content, text) : text;
        }

        /**
         * Convert states from GL to c:geo spotted types.
         *
         * @param type
         *          the GL Log type
         * @return
         *          The LogType
         */
        private static LogType getLogType(final String type) {
            switch (type) {
                case "Apparition": // Dropped
                    return LogType.PLACED_IT;
                case "Disparition": // Retrieved
                    return LogType.RETRIEVED_IT;
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
                final GeolutinsHandler glXMLHandler = new GeolutinsHandler();
                xmlR.setContentHandler(glXMLHandler);
                xmlR.parse(page);

                return glXMLHandler.getTrackables();
            } catch (final SAXException | IOException | ParserConfigurationException e) {
                Log.w("Cannot parse GeoLutins", e);
            }
        }
        return Collections.emptyList();
    }
}
