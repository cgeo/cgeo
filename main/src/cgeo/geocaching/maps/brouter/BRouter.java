package cgeo.geocaching.maps.brouter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedList;

import cgeo.geocaching.location.Geopoint;

/**
 * Created by lukeIam on 11.09.2016.
 */
public class BRouter {
    private static BRouterServiceConnection brouter;

    public static void connect(Context ctx) {
        if (brouter != null && brouter.isConnected()) {
            //already connected
            return; //Exception?
        }

        brouter = new BRouterServiceConnection();
        Intent intent = new Intent();
        intent.setClassName("btools.routingapp", "btools.routingapp.BRouterService");
        boolean hasBRouter = ctx.bindService(intent, brouter, Context.BIND_AUTO_CREATE);
        if (!hasBRouter) {
            brouter = null;
        }
    }

    public static Geopoint[] getTrack(Geopoint start, Geopoint dest) throws SAXException {
        if (brouter == null) {
            return null; //Exception
        }

        Bundle params = new Bundle();
        params.putString("trackFormat", "gpx");
        params.putString("v", "foot");
        params.putDoubleArray("lats", new double[]{start.getLatitude(), dest.getLatitude()});
        params.putDoubleArray("lons", new double[]{start.getLongitude(), dest.getLongitude()});

        String gpx = brouter.getTrackFromParams(params);

        final LinkedList<Geopoint> result = new LinkedList<>();

        Xml.parse(gpx, new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                if (qName.equalsIgnoreCase("trkpt")) {
                    String lat = atts.getValue("lat");
                    if (lat != null) {
                        String lon = atts.getValue("lon");
                        if (lon != null) {
                            result.add(new Geopoint(lat, lon));
                        }
                    }
                }
            }
        });

        return result.toArray(new Geopoint[result.size()]);
    }
}
