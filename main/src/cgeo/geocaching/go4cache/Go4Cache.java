package cgeo.geocaching.go4cache;

import cgeo.geocaching.Parameters;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.geopoint.GeopointFormatter.Format;
import cgeo.geocaching.geopoint.Viewport;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 *
 * Thread to send location information to go4cache.com. The singleton will be created
 * only if, at any time, the user opts in to send this information. Then the same thread
 * will take care of sending updated positions when available.
 *
 */

public class Go4Cache extends Thread {

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 2010-07-25 14:44:01
    private static Go4Cache instance;

    final private ArrayBlockingQueue<Geopoint> queue = new ArrayBlockingQueue<Geopoint>(1);
    final private cgeoapplication app;
    final private cgBase base;

    private static Go4Cache getInstance(final cgeoapplication app) {
        if (null == instance) {
            synchronized(Go4Cache.class) {
                instance = new Go4Cache(app);
                instance.start();
            }
        }
        return instance;
    }

    private Go4Cache(final cgeoapplication app) {
        super("Go4Cache");
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
                if (null != latestCoords && latestCoords.distanceTo(currentCoords) < 0.75 && StringUtils.equals(latestAction, currentAction)) {
                    continue;
                }

                final String username = Settings.getUsername();
                if (StringUtils.isBlank(username)) {
                    continue;
                }

                final String latStr = currentCoords.format(Format.LAT_DECDEGREE_RAW);
                final String lonStr = currentCoords.format(Format.LON_DECDEGREE_RAW);
                final Parameters params = new Parameters(
                        "u", username,
                        "lt", latStr,
                        "ln", lonStr,
                        "a", currentAction,
                        "s", (CryptUtils.sha1(username + "|" + latStr + "|" + lonStr + "|" + currentAction + "|" + CryptUtils.md5("carnero: developing your dreams"))).toLowerCase());
                if (null != base.version) {
                    params.put("v", base.version);
                }

                cgBase.postRequest("http://api.go4cache.com/", params);

                // Update our coordinates even if the request was not successful, as not to hammer the server
                // with invalid requests for every new GPS position.
                latestCoords = currentCoords;
                latestAction = currentAction;
            }
        } catch (InterruptedException e) {
            Log.e(Settings.tag, "Go4Cache.run: interrupted", e);
        }
    }

    /**
     * Return an immutable list of users present in the given viewport.
     *
     * @param username
     *            the current username
     * @param viewport
     *            the current viewport
     * @return the list of users present in the viewport
     */
    public static List<Go4CacheUser> getGeocachersInViewport(final String username, final Viewport viewport) {
        final List<Go4CacheUser> users = new ArrayList<Go4CacheUser>();

        if (null == username) {
            return users;
        }

        final Parameters params = new Parameters(
                "u", username,
                "ltm", viewport.bottomLeft.format(Format.LAT_DECDEGREE_RAW),
                "ltx", viewport.topRight.format(Format.LAT_DECDEGREE_RAW),
                "lnm", viewport.bottomLeft.format(Format.LON_DECDEGREE_RAW),
                "lnx", viewport.topRight.format(Format.LON_DECDEGREE_RAW));

        final String data = cgBase.getResponseData(cgBase.postRequest("http://api.go4cache.com/get.php", params));

        if (StringUtils.isBlank(data)) {
            Log.e(Settings.tag, "cgeoBase.getGeocachersInViewport: No data from server");
            return null;
        }

        try {
            final JSONArray usersData = new JSONObject(data).getJSONArray("users");
            final int count = usersData.length();
            for (int i = 0; i < count; i++) {
                final JSONObject oneUser = usersData.getJSONObject(i);
                users.add(parseUser(oneUser));
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgBase.getGeocachersInViewport: " + e.toString());
        }

        return Collections.unmodifiableList(users);
    }

    /**
     * Parse user information from go4cache.com.
     *
     * @param user
     *            a JSON object
     * @return a cgCache user filled with information
     * @throws JSONException
     *             if JSON could not be parsed correctly
     * @throws ParseException
     *             if the date could not be parsed as expected
     */
    private static Go4CacheUser parseUser(final JSONObject user) throws JSONException, ParseException {
        final Date date = dateFormat.parse(user.getString("located"));
        final String username = user.getString("user");
        final Geopoint coords = new Geopoint(user.getDouble("latitude"), user.getDouble("longitude"));
        final String action = user.getString("action");
        final String client = user.getString("client");
        return new Go4CacheUser(username, coords, date, action, client);
    }
}
