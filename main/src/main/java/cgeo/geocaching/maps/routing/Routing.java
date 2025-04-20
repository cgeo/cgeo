package cgeo.geocaching.maps.routing;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.brouter.core.RoutingEngine;
import cgeo.geocaching.downloader.DownloadConfirmationActivity;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.ProcessUtils;
import static cgeo.geocaching.brouter.BRouterConstants.PROFILE_PARAMTERKEY;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.sax.Element;
import android.sax.RootElement;
import android.util.Xml;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public final class Routing {
    public static final float NO_ELEVATION_AVAILABLE = Float.NaN; // check with Float.isNaN(...)

    private static final double UPDATE_MIN_DISTANCE_KILOMETERS = 0.005;
    private static final double MIN_ROUTING_DISTANCE_KILOMETERS = 0.04;
    private static final int UPDATE_MIN_DELAY_SECONDS = 3;
    private static AbstractServiceConnection routingServiceConnection;
    private static Geopoint lastDirectionUpdatePoint;
    @Nullable private static Geopoint[] lastRoutingPoints = null;
    private static Geopoint lastDestination;
    private static long timeLastUpdate;
    private static int connectCount = 0;

    private static final Map<String, Runnable> REGISTERED_CALLBACKS = new HashMap<>();
    private static final ArrayList<String> requestedTileFiles = new ArrayList<>();

    private static final Runnable SERVICE_CONNECTED_CALLBACK = () -> {
        synchronized (Routing.class) {
            for (Runnable r : REGISTERED_CALLBACKS.values()) {
                r.run();
            }
        }
    };

    private Routing() {
        // utility class
    }

    public static synchronized void connect() {
        connect(null, null);
    }

    public static synchronized void connect(@Nullable final LifecycleOwner owner) {
        connect(null, null, owner);
    }

    public static synchronized void connect(@Nullable final String callbackKey, @Nullable final Runnable onServiceConnectedCallback) {
        connect(callbackKey, onServiceConnectedCallback, null);
    }

    public static synchronized void connect(
        @Nullable final String callbackKey,
        @Nullable final Runnable onServiceConnectedCallback,
        @Nullable final LifecycleOwner owner
    ) {

        connectCount++;

        if (callbackKey != null && onServiceConnectedCallback != null) {
            REGISTERED_CALLBACKS.put(callbackKey, onServiceConnectedCallback);
        }

        if (owner != null) {
            owner.getLifecycle().addObserver(new RoutingObserver(callbackKey));
        }

        if (isConnected()) {
            //already connected
            return;
        }

        final Intent intent = new Intent();
        if (Settings.useInternalRouting()) {
            routingServiceConnection = new InternalServiceConnection(SERVICE_CONNECTED_CALLBACK);
            intent.setClassName(CgeoApplication.getInstance().getPackageName(), "cgeo.geocaching.brouter.InternalRoutingService");
        } else {
            routingServiceConnection = new BRouterServiceConnection(SERVICE_CONNECTED_CALLBACK);
            intent.setClassName("btools.routingapp", "btools.routingapp.BRouterService");
        }

        if (!getContext().bindService(intent, routingServiceConnection, Context.BIND_AUTO_CREATE)) {
            Log.d("Connecting brouter: bindService failed or delayed");
        }
    }

    private static ContextWrapper getContext() {
        return CgeoApplication.getInstance();
    }

    public static synchronized void disconnect(final String callbackKey) {

        if (callbackKey != null) {
            REGISTERED_CALLBACKS.remove(callbackKey);
        }

        connectCount--;

        if (connectCount <= 0) {
            connectCount = 0;
            if (isConnected()) {
                getContext().unbindService(routingServiceConnection);
                routingServiceConnection = null;

                Log.d("brouter disconnected");
            }
        }
    }

    /**
     * Return a valid track (with at least two points, including the start and destination).
     * In some cases (e.g., destination is too close or too far, path could not be found),
     * a straight line will be returned.
     *
     * @param start       the starting point
     * @param destination the destination point
     * @return a track with at least two points including the start and destination points
     */
    @NonNull
    public static Geopoint[] getTrack(final Geopoint start, final Geopoint destination) {
        if (routingServiceConnection == null || Settings.getRoutingMode() == RoutingMode.STRAIGHT) {
            return defaultTrack(start, destination);
        }

        // avoid updating to frequently
        final long timeNow = System.currentTimeMillis();
        if ((timeNow - timeLastUpdate) < 1000 * UPDATE_MIN_DELAY_SECONDS) {
            return ensureTrack(lastRoutingPoints, start, destination);
        }

        // Disable routing for huge distances
        final int maxThresholdKm = Settings.getBrouterThreshold();
        final float targetDistance = start.distanceTo(destination);
        if (targetDistance > maxThresholdKm) {
            return defaultTrack(start, destination);
        }

        // disable routing when near the target
        if (targetDistance < MIN_ROUTING_DISTANCE_KILOMETERS) {
            return defaultTrack(start, destination);
        }

        // Use cached route if current position has not changed more than 5m and we had a route
        // TODO: Maybe adjust this to current zoomlevel
        if (lastDirectionUpdatePoint != null && destination == lastDestination && start.distanceTo(lastDirectionUpdatePoint) < UPDATE_MIN_DISTANCE_KILOMETERS && lastRoutingPoints != null) {
            return lastRoutingPoints;
        }

        // now really calculate a new route
        lastDestination = destination;
        lastRoutingPoints = calculateRouting(start, destination, null);
        lastDirectionUpdatePoint = start;
        timeLastUpdate = timeNow;
        return ensureTrack(lastRoutingPoints, start, destination);
    }

    public static float getElevation(final Geopoint current) {
        if (routingServiceConnection == null || current == null || !routingServiceConnection.isConnected()) {
            return NO_ELEVATION_AVAILABLE;
        }
        final Bundle params = new Bundle();
        params.putInt("engineMode", RoutingEngine.BROUTER_ENGINEMODE_GETELEV);
        params.putDoubleArray("lats", new double[]{current.getLatitude(), current.getLatitude()});
        params.putDoubleArray("lons", new double[]{current.getLongitude(), current.getLongitude()});
        params.putString("v", RoutingMode.STRAIGHT.parameterValue);
        final String gpx = routingServiceConnection.getTrackFromParams(params);

        // parse result
        final boolean[] inElevationElement = new boolean[1];
        final float[] result = new float[1];
        result[0] = NO_ELEVATION_AVAILABLE;
        try {
            Xml.parse(gpx, new DefaultHandler() {
                @Override
                public void startElement(final String uri, final String localName, final String qName, final Attributes atts) {
                    if (qName.equalsIgnoreCase("ele")) {
                        inElevationElement[0] = true;
                    }
                }

                @Override
                public void endElement(final String uri, final String localName, final String qName) {
                    inElevationElement[0] = false;
                }

                @Override
                public void characters(final char[] ch, final int start, final int length) {
                    if (inElevationElement[0]) {
                        result[0] = Float.parseFloat(String.valueOf(ch));
                    }
                }
            });
        } catch (SAXException | NullPointerException ignore) {
            return NO_ELEVATION_AVAILABLE;
        }
        return result[0];
    }

    /**
     * Return a valid track (with at least two points, including the start and destination).
     * no caching
     *
     * @param start       the starting point
     * @param destination the destination point
     * @param elevation   the variable to write elevation info to (null, if no elevation info to be returned)
     * @return a track with at least two points including the start and destination points
     */
    @NonNull
    public static Geopoint[] getTrackNoCaching(final Geopoint start, final Geopoint destination, @Nullable final ArrayList<Float> elevation) {
        if (routingServiceConnection == null || Settings.getRoutingMode() == RoutingMode.STRAIGHT) {
            return defaultTrack(start, destination);
        }

        // Disable routing for huge distances
        final int maxThresholdKm = Settings.getBrouterThreshold();
        final float targetDistance = start.distanceTo(destination);
        if (targetDistance > maxThresholdKm) {
            return defaultTrack(start, destination);
        }

        // disable routing when near the target
        if (targetDistance < MIN_ROUTING_DISTANCE_KILOMETERS) {
            return defaultTrack(start, destination);
        }

        // now calculate a new route
        final Geopoint[] track = calculateRouting(start, destination, elevation);
        return ensureTrack(track, start, destination);
    }

    @NonNull
    private static Geopoint[] ensureTrack(@Nullable final Geopoint[] routingPoints, final Geopoint start, final Geopoint destination) {
        return routingPoints != null ? routingPoints : defaultTrack(start, destination);
    }

    @NonNull
    private static Geopoint[] defaultTrack(final Geopoint start, final Geopoint destination) {
        return new Geopoint[]{start, destination};
    }

    @Nullable
    @SuppressWarnings({"PMD.NPathComplexity"}) // splitting up would not improve readability
    private static Geopoint[] calculateRouting(final Geopoint start, final Geopoint dest, @Nullable final ArrayList<Float> elevation) {
        final Bundle params = new Bundle();
        params.putString("trackFormat", "gpx");
        params.putDoubleArray("lats", new double[]{start.getLatitude(), dest.getLatitude()});
        params.putDoubleArray("lons", new double[]{start.getLongitude(), dest.getLongitude()});
        params.putString("v", Settings.getRoutingMode().parameterValue);
        params.putString(PROFILE_PARAMTERKEY, Settings.getRoutingProfile()); // profile filename, used only by internal routing engine

        final String gpx = routingServiceConnection == null ? null : routingServiceConnection.getTrackFromParams(params);

        if (gpx == null) {
            Log.i("brouter returned no data");
            return null;
        }

        // missing routing data?
        if (gpx.startsWith("datafile ") && gpx.endsWith(" not found")) {
            synchronized (requestedTileFiles) {
                String filename = gpx.substring(9);
                final int pos = filename.indexOf(" ");
                if (pos != -1) {
                    filename = filename.substring(0, pos);
                }
                boolean alreadyRequested = false;
                for (String f : requestedTileFiles) {
                    if (filename.equals(f)) {
                        alreadyRequested = true;
                        break;
                    }
                }
                Log.i("routing: missing file " + filename + ", alreadyRequested=" + alreadyRequested);
                if (!alreadyRequested) {
                    requestedTileFiles.add(filename);
                    if (Settings.useInternalRouting()) {
                        if (Settings.isBrouterAutoTileDownloads() && !PersistableFolder.ROUTING_TILES.isLegacy()) {
                            final Intent intent = new Intent(getContext(), DownloadConfirmationActivity.class);
                            intent.putExtra(DownloadConfirmationActivity.BUNDLE_FILENAME, filename);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getContext().startActivity(intent);
                        }
                    } else {
                        ActivityMixin.showApplicationToast(String.format(getContext().getString(R.string.brouter_missing_data), filename));
                    }
                }
            }
        }

        // other error
        if (!gpx.startsWith("<?xml")) {
            Log.w("brouter returned an error message: " + gpx);
            return null;
        }

        return parseGpxTrack(gpx, dest, elevation);
    }

    @Nullable
    private static Geopoint[] parseGpxTrack(@NonNull final String gpx, final Geopoint destination, @Nullable final ArrayList<Float> elevation) {
        try {
            final LinkedList<Geopoint> result = new LinkedList<>();

            final String namespace = "http://www.topografix.com/GPX/1/1";
            final RootElement root = new RootElement(namespace, "gpx");
            final Element trk = root.getChild(namespace, "trk");
            final Element trkseg = trk.getChild(namespace, "trkseg");
            final Element trkpt = trkseg.getChild(namespace, "trkpt");
            final Element ele = trkpt.getChild(namespace, "ele");

            trkpt.setStartElementListener(attributes -> {
                final String lat = attributes.getValue("lat");
                if (lat != null) {
                    final String lon = attributes.getValue("lon");
                    if (lon != null) {
                        result.add(new Geopoint(lat, lon));
                    }
                }
            });

            ele.setEndTextElementListener(body -> {
                if (elevation != null) {
                    elevation.add(Float.parseFloat(body));
                }
            });

            Xml.parse(gpx, root.getContentHandler());

            // artificial straight line from track to target
            if (destination != null) {
                result.add(destination);
                if (elevation != null) {
                    elevation.add(Float.NaN);
                }
            }
            return result.toArray(new Geopoint[0]);

        } catch (SAXException e) {
            Log.w("cannot parse brouter output of length " + gpx.length() + ", gpx=" + gpx, e);
        }
        return null;
    }

    public static void invalidateRouting() {
        lastDirectionUpdatePoint = null;
        timeLastUpdate = 0;
    }

    /**
     * Are we currently connected to any routing service (internal/external)?
     */
    public static boolean isConnected() {
        return routingServiceConnection != null && routingServiceConnection.isConnected();
    }

    /**
     * Is any routing service available (internal/external)
     */
    public static boolean isAvailable() {
        return isConnected() || Settings.useInternalRouting() || isExternalRoutingInstalled();
    }

    /**
     * Is external routing app installed?
     */
    public static boolean isExternalRoutingInstalled() {
        return ProcessUtils.isInstalled(getExternalRoutingPackageName());
    }

    /**
     * Get Android package name for external routing app
     */
    public static String getExternalRoutingPackageName() {
        return getContext().getString(R.string.package_brouter);
    }

    private static class RoutingObserver implements DefaultLifecycleObserver {

        @Nullable
        private final String callbackKey;

        RoutingObserver(@Nullable final String callbackKey) {
            this.callbackKey = callbackKey;
        }

        @Override
        public void onDestroy(@NonNull final LifecycleOwner owner) {
            disconnect(callbackKey);
        }
    }
}
