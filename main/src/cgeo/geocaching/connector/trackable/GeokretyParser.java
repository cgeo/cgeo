package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.utils.Log;

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
            public void end(String name) {
                trackable.setName(name);
            }
        });

        geokret.setStartElementListener(new StartElementListener() {

            @Override
            public void start(Attributes attributes) {
                try {
                    if (attributes.getIndex("id") > -1) {
                        trackable.setGeocode(geocode(Integer.valueOf(attributes.getValue("id"))));
                    }
                    if (attributes.getIndex("dist") > -1) {
                        trackable.setDistance(Float.valueOf(attributes.getValue("dist")));
                    }
                    if (attributes.getIndex("type") > -1) {
                        trackable.setType(getType(Integer.valueOf(attributes.getValue("type"))));
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

    protected static String getType(int type) {
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

    protected static String geocode(final int id) {
        return String.format("GK%04X", id);
    }
}
