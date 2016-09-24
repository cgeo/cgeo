package cgeo.geocaching.maps.brouter;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Xml;

import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class BRouter {
    private static BRouterServiceConnection brouter;

    private BRouter() {
        // utility class
    }

    public static void connect(final Context ctx) {
        if (brouter != null && brouter.isConnected()) {
            //already connected
            return;
        }

        brouter = new BRouterServiceConnection();
        final Intent intent = new Intent();
        intent.setClassName("btools.routingapp", "btools.routingapp.BRouterService");

        if (!ctx.bindService(intent, brouter, Context.BIND_AUTO_CREATE)) {
            brouter = null;
        }
    }

    public static void disconnect(final Context ctx) {
        if (brouter != null && brouter.isConnected()) {
            ctx.unbindService(brouter);
            brouter = null;
        }
    }

    public static Geopoint[] getTrack(final Geopoint start, final Geopoint dest) throws SAXException {
        if (brouter == null) {
            return null;
        }

        final Bundle params = new Bundle();
        params.putString("trackFormat", "gpx");
        params.putString("v", "foot");
        params.putDoubleArray("lats", new double[]{start.getLatitude(), dest.getLatitude()});
        params.putDoubleArray("lons", new double[]{start.getLongitude(), dest.getLongitude()});
        params.putString("v", Settings.getRoutingMode());

        final String gpx = brouter.getTrackFromParams(params);

        final LinkedList<Geopoint> result = new LinkedList<>();

        Xml.parse(gpx, new DefaultHandler() {
            @Override
            public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
                if (qName.equalsIgnoreCase("trkpt")) {
                    final String lat = atts.getValue("lat");
                    if (lat != null) {
                        final String lon = atts.getValue("lon");
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
