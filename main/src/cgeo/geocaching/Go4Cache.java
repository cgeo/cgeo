package cgeo.geocaching;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

import android.util.Log;

import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * Thread to send location information to go4cache.com. The singleton will be created
 * only if, at any time, the user opts in to send this information. Then the same thread
 * will take care of sending updated positions when available.
 *
 */

public class Go4Cache extends Thread {

    private static Go4Cache instance;

    final private ArrayBlockingQueue<Geopoint> queue = new ArrayBlockingQueue<Geopoint>(1);
    final private cgeoapplication app;
    final private cgBase base;

    private static Go4Cache getInstance(final cgeoapplication app) {
        if (instance == null) {
            synchronized(Go4Cache.class) {
                instance = new Go4Cache(app);
                instance.start();
            }
        }
        return instance;
    }

    private Go4Cache(final cgeoapplication app) {
        this.app = app;
        base = cgBase.getInstance(app);
        setPriority(Thread.MIN_PRIORITY);
    }

    /**
     * Send the coordinates to go4cache.com if the user opted in to do so.
     *
     * @param app
     *            the current application
     * @param coords
     *            the current coordinates
     */
    public static void signalCoordinates(final cgeoapplication app, final Geopoint coords) {
        if (Settings.isPublicLoc()) {
            getInstance(app).queue.offer(coords);
        }
    }

    @Override
    public void run() {
        Log.d(Settings.tag, "Go4Cache task started");
        Geopoint latestCoords = null;
        String latestAction = null;

        try {
            for (;;) {
                final Geopoint currentCoords = queue.take();
                final String currentAction = app.getAction();

                // If we are too close and we haven't changed our current action, no need
                // to update our situation.
                if (latestCoords != null && latestCoords.distanceTo(currentCoords) < 0.75 && StringUtils.equals(latestAction, currentAction)) {
                    continue;
                }

                final String username = Settings.getUsername();
                if (StringUtils.isBlank(username)) {
                    continue;
                }

                final String latStr = String.format((Locale) null, "%.6f", currentCoords.getLatitude());
                final String lonStr = String.format((Locale) null, "%.6f", currentCoords.getLongitude());
                final Parameters params = new Parameters(
                        "u", username,
                        "lt", latStr,
                        "ln", lonStr,
                        "a", currentAction,
                        "s", (CryptUtils.sha1(username + "|" + latStr + "|" + lonStr + "|" + currentAction + "|" + CryptUtils.md5("carnero: developing your dreams"))).toLowerCase());
                if (base.version != null) {
                    params.put("v", base.version);
                }

                cgBase.postRequest("http://api.go4cache.com/", params);

                // Update our coordinates even if the request was not succesful, as not to hammer the server
                // with invalid requests for every new GPS position.
                latestCoords = currentCoords;
                latestAction = currentAction;
            }
        } catch (InterruptedException e) {
            Log.e(Settings.tag, "Go4Cache.run: interrupted", e);
        }
    }
}
