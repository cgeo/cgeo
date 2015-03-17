package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.sax.Element;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

public class GeokretyParser {

    public static Trackable parse(final String page) {
        final Trackable trackable = new Trackable();

        final RootElement root = new RootElement("gkxml");
        final Element geokret = root.getChild("geokrety").getChild("geokret");

        geokret.setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String name) {
                trackable.setName(name);
            }
        });

        geokret.setStartElementListener(new StartElementListener() {

            @Override
            public void start(final Attributes attributes) {
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
        });

        try {
            Xml.parse(page, root.getContentHandler());
            return trackable;
        } catch (final SAXException e) {
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
