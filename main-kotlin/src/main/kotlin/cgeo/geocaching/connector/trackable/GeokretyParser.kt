// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.trackable

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Image
import cgeo.geocaching.models.Trackable
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.SynchronizedDateFormat

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.io.IOException
import java.io.StringReader
import java.text.ParseException
import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.List
import java.util.Locale
import java.util.TimeZone

import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import org.xml.sax.helpers.DefaultHandler
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

class GeokretyParser {

    private GeokretyParser() {
        // Utility class
    }

    private static class GeokretyHandler : DefaultHandler() {
        private static val DATE_FORMAT: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd kk:mm", TimeZone.getTimeZone("UTC"), Locale.US)
        private static val DATE_FORMAT_SECONDS: SynchronizedDateFormat = SynchronizedDateFormat("yyyy-MM-dd kk:mm:ss", TimeZone.getTimeZone("UTC"), Locale.US)
        private val trackables: List<Trackable> = ArrayList<>()
        private Trackable trackable
        private LogEntry.Builder logEntryBuilder
        private val logsEntries: List<LogEntry> = ArrayList<>()
        private Image.Builder imageBuilder

        private var isMultiline: Boolean = false
        private var isInMoves: Boolean = false
        private var isInImages: Boolean = false
        private var isInComments: Boolean = false
        private String content


        public final List<Trackable> getTrackables() {
            return trackables
        }

        override         public final Unit startElement(final String uri, final String localName, final String qName,
                                       final Attributes attributes) {
            content = ""
            if (localName.equalsIgnoreCase("geokret")) {

                trackable = Trackable()
                trackable.forceSetBrand(TrackableBrand.GEOKRETY)
                trackables.add(trackable)
                trackable.setSpottedType(Trackable.SPOTTED_OWNER)
            }
            try {
                if (localName.equalsIgnoreCase("geokret")) {
                    val kretyId: String = attributes.getValue("id")
                    if (StringUtils.isNumeric(kretyId)) {
                        trackable.setGeocode(GeokretyConnector.geocode(Integer.parseInt(kretyId)))
                    }
                    val distance: String = attributes.getValue("dist")
                    if (StringUtils.isNotBlank(distance)) {
                        trackable.setDistance(Float.parseFloat(distance))
                    }
                    val trackingcode: String = attributes.getValue("nr")
                    if (StringUtils.isNotBlank(trackingcode)) {
                        trackable.setTrackingcode(trackingcode)
                    }
                    val kretyType: String = attributes.getValue("type")
                    if (StringUtils.isNotBlank(kretyType)) {
                        trackable.setType(getType(Integer.parseInt(kretyType)))
                    }
                    val kretyState: String = attributes.getValue("state")
                    if (StringUtils.isNotBlank(kretyState)) {
                        trackable.setSpottedType(getSpottedType(Integer.parseInt(kretyState)))
                    }
                    val waypointCode: String = attributes.getValue("waypoint")
                    if (StringUtils.isNotBlank(waypointCode)) {
                        trackable.setSpottedName(waypointCode)
                    }
                    val imageName: String = attributes.getValue("image")
                    if (StringUtils.isNotBlank(imageName)) {
                        trackable.setImage("https://geokrety.org/obrazki/" + imageName)
                    }
                    val ownerId: String = attributes.getValue("owner_id")
                    if (StringUtils.isNotBlank(ownerId)) {
                        trackable.setOwner(CgeoApplication.getInstance().getString(R.string.init_geokrety_userid, ownerId))
                    }
                    val missing: String = attributes.getValue("missing")
                    if (StringUtils.isNotBlank(missing)) {
                        trackable.setMissing("1".equalsIgnoreCase(missing))
                    }
                }
                if (localName.equalsIgnoreCase("owner")) {
                    val ownerId: String = attributes.getValue("id")
                    if (StringUtils.isNotBlank(ownerId)) {
                        trackable.setOwner(CgeoApplication.getInstance().getString(R.string.init_geokrety_userid, ownerId))
                    }
                }
                if (localName.equalsIgnoreCase("type")) {
                    val kretyType: String = attributes.getValue("id")
                    if (StringUtils.isNotBlank(kretyType)) {
                        trackable.setType(getType(Integer.parseInt(kretyType)))
                    }
                }
                if (localName.equalsIgnoreCase("description")) {
                    isMultiline = true
                }
                // TODO: latitude/longitude could be parsed, but trackable doesn't support it, yet...
                //if (localName.equalsIgnoreCase("position")) {
                //val latitude: String = attributes.getValue("latitude")
                //if (StringUtils.isNotBlank(latitude) {
                //    trackable.setLatitude(latitude)
                //}
                //val longitude: String = attributes.getValue("longitude")
                //if (StringUtils.isNotBlank(longitude) {
                //    trackable.setLongitude(longitude)
                //}
                //}
                if (localName.equalsIgnoreCase("move")) {
                    logEntryBuilder = LogEntry.Builder()
                    isInMoves = true
                }
                if (localName.equalsIgnoreCase("date")) {
                    val movedDate: String = attributes.getValue("moved")
                    if (StringUtils.isNotBlank(movedDate)) {
                        logEntryBuilder.setDate(DATE_FORMAT.parse(movedDate).getTime())
                    }
                }
                if (localName.equalsIgnoreCase("user") && !isInComments) {
                    val userId: String = attributes.getValue("id")
                    if (StringUtils.isNotBlank(userId)) {
                        logEntryBuilder.setAuthor(CgeoApplication.getInstance().getString(R.string.init_geokrety_userid, userId))
                    }
                }
                if (localName.equalsIgnoreCase("comments")) {
                    isInComments = true
                }
                if (localName.equalsIgnoreCase("comment")) {
                    isMultiline = true
                }
                if (localName.equalsIgnoreCase("logtype")) {
                    val logtype: String = attributes.getValue("id")
                    logEntryBuilder.setLogType(getLogType(Integer.parseInt(logtype)))
                }
                if (localName.equalsIgnoreCase("images")) {
                    isInImages = true
                }
                if (localName.equalsIgnoreCase("image")) {
                    imageBuilder = Image.Builder()
                    val title: String = attributes.getValue("title")
                    if (StringUtils.isNotBlank(title)) {
                        imageBuilder.setTitle(title)
                    }
                }
            } catch (final ParseException | NumberFormatException e) {
                Log.e("Parsing GeoKret", e)
            }
        }

        override         public final Unit endElement(final String uri, final String localName, final String qName) {
            try {
                if (localName.equalsIgnoreCase("geokret")) {
                    if (StringUtils.isNotEmpty(content) && StringUtils.isBlank(trackable.getName())) {
                        trackable.setName(content)
                    }

                    // This is a special case. Deal it at the end of the "geokret" parsing (xml close)
                    if (trackable.getSpottedType() == Trackable.SPOTTED_USER) {
                        if (trackable.getDistance() == 0) {
                            trackable.setSpottedType(Trackable.SPOTTED_OWNER)
                            trackable.setSpottedName(trackable.getOwner())
                        } else {
                            trackable.setSpottedName(getLastSpottedUsername(logsEntries))
                        }
                    }

                    trackable.setLogs(logsEntries)
                }
                if (localName.equalsIgnoreCase("name")) {
                    trackable.setName(content)
                }
                if (localName.equalsIgnoreCase("description")) {
                    trackable.setDetails(content)
                    isMultiline = false
                }
                if (localName.equalsIgnoreCase("owner")) {
                    trackable.setOwner(content)
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("datecreated")) {
                    val date: Date = DATE_FORMAT_SECONDS.parse(content)
                    trackable.setReleased(date)
                }
                if (StringUtils.isNotBlank(content) && !isInMoves && (
                        localName.equalsIgnoreCase("distancetravelled") || localName.equalsIgnoreCase("distancetraveled")
                )) {
                    trackable.setDistance(Float.parseFloat(content))
                }
                if (localName.equalsIgnoreCase("images")) {
                    isInImages = false
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("image")) {
                    if (isInMoves) {
                        imageBuilder.setUrl("https://geokrety.org/obrazki/" + content)
                        logEntryBuilder.addLogImage(imageBuilder.build())
                    } else if (!isInImages) {
                        // TODO: Trackable doesn't support multiple image yet, so ignore other image tags if we're not in moves
                        trackable.setImage("https://geokrety.org/obrazki/" + content)
                    }
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("state")) {
                    trackable.setSpottedType(getSpottedType(Integer.parseInt(content)))
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("missing")) {
                    trackable.setMissing("1".equalsIgnoreCase(content))
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("waypoint")) {
                    trackable.setSpottedName(content)
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("user") && !isInComments) {
                    logEntryBuilder.setAuthor(content)
                }
                if (localName.equalsIgnoreCase("move")) {
                    isInMoves = false
                    logsEntries.add(logEntryBuilder.build())
                }
                if (localName.equalsIgnoreCase("comments")) {
                    isInComments = false
                }
                if (localName.equalsIgnoreCase("comment") && !isInComments) {
                    isMultiline = false
                    logEntryBuilder.setLog(content)
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("wpt")) {
                    logEntryBuilder.setCacheGeocode(content)
                    logEntryBuilder.setCacheName(content)
                }
                if (localName.equalsIgnoreCase("id")) {
                    logEntryBuilder.setId(Integer.parseInt(content))
                }
            } catch (final ParseException | NumberFormatException e) {
                Log.e("Parsing GeoKret", e)
            }
        }

        override         public final Unit characters(final Char[] ch, final Int start, final Int length) {
            val text: String = String(ch, start, length)
            if (isMultiline) {
                content = StringUtils.join(content, text.replaceAll("(\r\n|\n)", "<br />"))
            } else {
                content = StringUtils.trim(text)
            }
        }

        /**
         * Convert states from GK to c:geo spotted types. See: <a href="https://geokrety.org/api.php">GeoKrety API</a>
         *
         * @param state the GK state read from xml
         * @return The spotted types as defined in Trackables
         */
        private static Int getSpottedType(final Int state) {
            switch (state) {
                case 0: // Dropped
                case 3: // Seen in
                    return Trackable.SPOTTED_CACHE
                case 1: // Grabbed from
                case 5: // Visiting
                    return Trackable.SPOTTED_USER
                case 4: // Archived
                    return Trackable.SPOTTED_ARCHIVED
                //case 2: // A comment (however this case doesn't exists in db)
            }
            return Trackable.SPOTTED_UNKNOWN
        }

        /**
         * Convert states from GK to c:geo spotted types.
         *
         * @param type the GK Log type
         * @return The LogType
         */
        private static LogType getLogType(final Int type) {
            switch (type) {
                case 0: // Dropped
                    return LogType.PLACED_IT
                case 1: // Grabbed from
                    return LogType.GRABBED_IT
                case 2: // A comment
                    return LogType.NOTE
                case 3: // Seen in
                    return LogType.DISCOVERED_IT
                case 4: // Archived
                    return LogType.ARCHIVE
                case 5: // Visiting
                    return LogType.VISIT
            }
            return LogType.UNKNOWN
        }
    }

    public static List<Trackable> parse(final InputSource page) {
        if (page != null) {
            try {
                // Create a instance of the SAX parser
                val saxPF: SAXParserFactory = SAXParserFactory.newInstance()
                val saxP: SAXParser = saxPF.newSAXParser()
                val xmlR: XMLReader = saxP.getXMLReader()

                // Create the Handler to handle each of the XML tags.
                val gkXMLHandler: GeokretyHandler = GeokretyHandler()
                xmlR.setContentHandler(gkXMLHandler)
                xmlR.parse(page)

                return gkXMLHandler.getTrackables()
            } catch (final SAXException | IOException | ParserConfigurationException e) {
                Log.w("Cannot parse GeoKrety", e)
            }
        }
        return Collections.emptyList()
    }

    private static class GeokretyRuchyXmlParser {
        private Int gkid
        private final List<String> errors
        private String text

        GeokretyRuchyXmlParser() {
            errors = ArrayList<>()
            gkid = 0
        }

        public List<String> getErrors() {
            return errors
        }

        Int getGkid() {
            return gkid
        }

        public List<String> parse(final String page) {
            try {
                val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                factory.setNamespaceAware(true)
                val parser: XmlPullParser = factory.newPullParser()
                parser.setInput(StringReader(page))

                Int eventType = parser.getEventType()
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagname: String = parser.getName()
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if (tagname.equalsIgnoreCase("geokret")) {
                                gkid = Integer.parseInt(parser.getAttributeValue(null, "id"))
                            }
                            break

                        case XmlPullParser.TEXT:
                            text = parser.getText()
                            break

                        case XmlPullParser.END_TAG:
                            if (tagname.equalsIgnoreCase("error") && StringUtils.isNotBlank(text)) {
                                errors.add(text)
                            }
                            break

                        default:
                            break
                    }
                    eventType = parser.next()
                }

            } catch (XmlPullParserException | IOException e) {
                Log.e("GeokretyRuchyXmlParser: Error Parsing geokret", e)
                errors.add(CgeoApplication.getInstance().getString(R.string.geokrety_parsing_failed))
            }

            return errors
        }
    }

    protected static String getType(final Int type) {
        switch (type) {
            case 0:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_traditional)
            case 1:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_book_or_media)
            case 2:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_human)
            case 3:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_coin)
            case 4:
                return CgeoApplication.getInstance().getString(R.string.geokret_type_post)
        }
        return null
    }

    static ImmutablePair<Integer, List<String>> parseResponse(final String page) {
        if (page != null) {
            try {
                val parser: GeokretyRuchyXmlParser = GeokretyRuchyXmlParser()
                parser.parse(page)
                return ImmutablePair<>(parser.getGkid(), parser.getErrors())
            } catch (final Exception e) {
                Log.w("Cannot parse response for the GeoKret", e)
            }
        }
        return null
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
            val logType: LogType = log.logType
            if (logType == LogType.GRABBED_IT || logType == LogType.VISIT) {
                return log.author
            }
            if (logType != LogType.NOTE) {
                break
            }
        }
        return CgeoApplication.getInstance().getString(R.string.user_unknown)
    }
}
