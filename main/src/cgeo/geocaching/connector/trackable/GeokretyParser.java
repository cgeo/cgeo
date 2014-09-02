package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class GeokretyParser {

    static class GeokretyHandler extends DefaultHandler {
        private final List<Trackable> trackables = new ArrayList<Trackable>();
        Trackable trackable;
        private String content;

        public final List<Trackable> getTrackables() { return trackables; }

        public GeokretyHandler() {}

        @Override
        public final void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            if (localName.equalsIgnoreCase("geokret")) {

                trackable = new Trackable();
                trackables.add(trackable);

                try {
                    final String kretyId = attributes.getValue("id");
                    if (StringUtils.isNumeric(kretyId)) {
                        trackable.setGeocode(GeokretyConnector.geocode(Integer.parseInt(kretyId)));
                    }
                    final String distance = attributes.getValue("dist");
                    if (StringUtils.isNotBlank(distance)) {
                        trackable.setDistance(Float.parseFloat(distance));
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
                } catch (final NumberFormatException e) {
                    Log.e("Parsing geokret", e);
                }
            }
        }

        @Override
        public final void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (localName.equalsIgnoreCase("geokret")) {
                trackable.setName(content);
            }
        };

        @Override
        public final void characters(char[] ch, int start, int length)
                throws SAXException {
            content = new String(ch, start, length);
        }
    }

    public static List<Trackable> parse(final InputSource page) {

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
