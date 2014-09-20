package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.enumerations.TrackableBrand;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class GeokretyParser {
    static class GeokretyHandler extends DefaultHandler {
        private final List<Trackable> trackables = new ArrayList<>();
        private Trackable trackable;
        private String content;

        public final List<Trackable> getTrackables() {
            return trackables;
        }

        @Override
        public final void startElement(final String uri, final String localName, final String qName,
                                       final Attributes attributes) throws SAXException {
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
                Log.e("Parsing geokret", e);
            }
        }

        @Override
        public final void endElement(final String uri, final String localName, final String qName)
                throws SAXException {
            try {
                if (localName.equalsIgnoreCase("geokret") && !content.trim().isEmpty()) {
                    trackable.setName(content);
                }
                if (localName.equalsIgnoreCase("name")) {
                    trackable.setName(content);
                }
                if (localName.equalsIgnoreCase("description")) {
                    trackable.setDetails(content);
                }
                if (localName.equalsIgnoreCase("owner")) {
                    trackable.setOwner(content);
                }
                if (localName.equalsIgnoreCase("datecreated")) {
                    final Date date = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss").parse(content);
                    trackable.setReleased(date);
                }
                if (localName.equalsIgnoreCase("distancetravelled")) {
                    trackable.setDistance(Float.parseFloat(content));
                }
                if (localName.equalsIgnoreCase("image")) {
                    trackable.setImage("http://geokrety.org/obrazki/" + content);
                }
                if (localName.equalsIgnoreCase("state")) {
                    trackable.setSpottedType(Integer.parseInt(content));
                }
                // TODO: Can be parsed but not supported in c:geo, yet...
                //if (localName.equalsIgnoreCase("missing")) {
                //    trackable.setMissing(Boolean.parseBoolean(content));
                //}
                if (localName.equalsIgnoreCase("waypoint")) {
                    trackable.setSpottedName(content);
                    trackable.setSpottedType(Trackable.SPOTTED_CACHE);
                }
            } catch (final ParseException e) {
                Log.e("Parsing geokret", e);
            }
        };

        @Override
        public final void characters(final char[] ch, final int start, final int length)
                throws SAXException {
            content = new String(ch, start, length);
        }
    }

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
            } catch (final Exception e) {
                Log.w("Cannot parse geokrety", e);
            }
        }
        return null;
    }

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
}
