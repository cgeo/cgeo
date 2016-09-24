package cgeo.geocaching.maps.brouter;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Xml;

import java.util.LinkedList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class BRouter {
    private static final double MAX_ROUTING_DISTANCE_KILOMETERS = 5.0;
    private static BRouterServiceConnection brouter;
    private static Geopoint lastDirectionUpdatePoint;
    private static Geopoint[] lastRoutingPoints;
    private static Geopoint lastDestination;

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

    @Nullable
    public static Geopoint[] getTrack(final Geopoint start, final Geopoint destination) {
        if (brouter == null) {
            return null;
        }

        // Disable routing for huge distances
        if (start.distanceTo(destination) > MAX_ROUTING_DISTANCE_KILOMETERS) {
            return null;
        }

        // Use cached route if current position has not changed more than 5m
        // TODO: Maybe adjust this to current zoomlevel
        if (lastDirectionUpdatePoint != null && destination == lastDestination && start.distanceTo(lastDirectionUpdatePoint) < 0.005) {
            return lastRoutingPoints;
        }

        // now really calculate a new route
        lastDestination = destination;
        lastRoutingPoints = calculateRouting(start, destination);
        lastDirectionUpdatePoint = start;
        return lastRoutingPoints;
    }

    private static Geopoint[] calculateRouting(final Geopoint start, final Geopoint dest) {
        final Bundle params = new Bundle();
        params.putString("trackFormat", "gpx");
        params.putString("v", "foot");
        params.putDoubleArray("lats", new double[]{start.getLatitude(), dest.getLatitude()});
        params.putDoubleArray("lons", new double[]{start.getLongitude(), dest.getLongitude()});
        params.putString("v", Settings.getRoutingMode().parameterValue);

        final String gpx = brouter.getTrackFromParams(params);

        return parseGpxTrack(gpx);
    }

    @Nullable
    private static Geopoint[] parseGpxTrack(final String gpx) {
        try {
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

        } catch (final SAXException e) {
            Log.e("cannot parse brouter output", e);
        }
        return null;
    }

    public static void invalidateRouting() {
        lastDirectionUpdatePoint = null;
    }

    public static boolean isAvailable() {
        return brouter != null;
    }
}
