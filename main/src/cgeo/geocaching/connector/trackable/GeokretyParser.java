package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.SynchronizedDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class GeokretyParser {
    static class GeokretyHandler extends DefaultHandler {
        private static final SynchronizedDateFormat DATE_FORMAT = new SynchronizedDateFormat("yyyy-MM-dd kk:mm:ss", TimeZone.getTimeZone("UTC"), Locale.US);
        private final List<Trackable> trackables = new ArrayList<>();
        private Trackable trackable;
        private Boolean isMessage = false;
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
                    final String waypointCode = attributes.getValue("waypoint");
                    if (StringUtils.isNotBlank(waypointCode)) {
                        trackable.setSpottedName(waypointCode);
                        trackable.setSpottedType(Trackable.SPOTTED_CACHE);
                    }
                    final String imageName = attributes.getValue("image");
                    if (StringUtils.isNotBlank(imageName)) {
                        trackable.setImage("http://geokrety.org/obrazki/" + imageName);
                    }
                }
                if (localName.equalsIgnoreCase("owner")) {
                    final String owner = attributes.getValue("id");
                    if (StringUtils.isNotBlank(owner)) {
                        trackable.setOwnerGuid(getType(Integer.parseInt(owner)));
                    }
                }
                if (localName.equalsIgnoreCase("type")) {
                    final String kretyType = attributes.getValue("id");
                    if (StringUtils.isNotBlank(kretyType)) {
                        trackable.setType(getType(Integer.parseInt(kretyType)));
                    }
                }
                if (localName.equalsIgnoreCase("description")) {
                    isMessage = true;
                }
                // TODO: latitude/longitude could be parsed, but trackable doesn't support it, yet...
                //if (localName.equalsIgnoreCase("position")) {
                //final String latitude = attributes.getValue("latitude");
                //if (StringUtils.isNotBlank(latitude)) {
                //    trackable.setLatitude(getType(Integer.parseInt(latitude)));
                //}
                //final String longitude = attributes.getValue("longitude");
                //if (StringUtils.isNotBlank(longitude)) {
                //    trackable.setLongitude(getType(Integer.parseInt(longitude)));
                //}
                //}
            } catch (final NumberFormatException e) {
                Log.e("Parsing GeoKret", e);
            }
        }

        @Override
        public final void endElement(final String uri, final String localName, final String qName)
                throws SAXException {
            try {
                if (localName.equalsIgnoreCase("geokret") && StringUtils.isNotEmpty(content)) {
                    trackable.setName(content);
                }
                if (localName.equalsIgnoreCase("name")) {
                    trackable.setName(content);
                }
                if (localName.equalsIgnoreCase("description")) {
                    trackable.setDetails(content);
                    isMessage = false;
                }
                if (localName.equalsIgnoreCase("owner")) {
                    trackable.setOwner(content);
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("datecreated")) {
                    final Date date = DATE_FORMAT.parse(content);
                    trackable.setReleased(date);
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("distancetravelled")) {
                    trackable.setDistance(Float.parseFloat(content));
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("image")) {
                    trackable.setImage("http://geokrety.org/obrazki/" + content);
                }
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("state")) {
                    trackable.setSpottedType(Integer.parseInt(content));
                }
                // TODO: Can be parsed but not supported in c:geo, yet...
                //if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("missing")) {
                //    trackable.setMissing(Boolean.parseBoolean(content));
                //}
                if (StringUtils.isNotBlank(content) && localName.equalsIgnoreCase("waypoint")) {
                    trackable.setSpottedName(content);
                    trackable.setSpottedType(Trackable.SPOTTED_CACHE);
                }
            } catch (final ParseException | NumberFormatException e) {
                Log.e("Parsing GeoKret", e);
            }
        }

        @Override
        public final void characters(final char[] ch, final int start, final int length)
                throws SAXException {
            final String text = StringUtils.trim(new String(ch, start, length));
            content = isMessage ? StringUtils.join(content, text) : text;
        }
    }

    @Nullable
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
        return null;
    }

    private static class GeokretyRuchyXmlParser {
        private int gkid;
        private final List<String> errors;
        private String text;

        public GeokretyRuchyXmlParser() {
            errors = new ArrayList<>();
            gkid = 0;
        }

        public List<String> getErrors() {
            return errors;
        }

        public int getGkid() {
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
                            if (tagname.equalsIgnoreCase("error")) {
                                if (null != text && !text.trim().isEmpty()) {
                                    errors.add(text);
                                }
                            }
                            break;

                        default:
                            break;
                    }
                    eventType = parser.next();
                }

            } catch (XmlPullParserException | IOException e) {
                Log.e("GeokretyRuchyXmlParser: Error Parsing geokret", e);
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
    public static ImmutablePair<Integer, List<String>> parseResponse(final String page) {
        if (null != page) {
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
}
