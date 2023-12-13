package app.organicmaps.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.ArrayList;

public class OrganicMapsApi {

    static final String PACKAGE_NAME_RELEASE = "app.organicmaps";
    static final String PACKAGE_NAME_DEBUG = "app.organicmaps.debug";
    static final String PACKAGE_NAME_BETA = "app.organicmaps.beta";

    private OrganicMapsApi() {
        // utility class
    }

    public static void showPointOnMap(final Activity activity, final double lat, final double lon, final String name) {
        final ArrayList<Point> points = new ArrayList<>(1);
        points.add(new Point(lat, lon, name));
        showPointsOnMap(activity, name, points);
    }

    public static void showPointsOnMap(final Activity activity, final String name, final ArrayList<Point> points) {
        final Intent intent = new MapRequest()
                .setPoints(points)
                .setAppName(name)
                .toIntent();
        sendRequest(activity, intent);
    };

    public static void sendRequest(final Activity caller, final Intent intent) {
        if (canHandleOrganicMapsIntents(caller)) {
            caller.startActivity(intent);
        } else {
            new DownloadDialog(caller).show();
        }
    }

    /**
     * Detects if any handler for OrganicMaps intents is installed on the device
     */
    public static boolean canHandleOrganicMapsIntents(final Context context) {
        final ComponentName c = new MapRequest().toIntent().resolveActivity(context.getPackageManager());
        return c != null;
    }

    /**
     * Detects if one of the specific OrganicMaps packages is installed
     */
    public static boolean isOrganicMapsPackageInstalled(final Context context) {
        final PackageManager pm = context.getPackageManager();
        return (pm.getLaunchIntentForPackage(PACKAGE_NAME_RELEASE) != null
                || pm.getLaunchIntentForPackage(PACKAGE_NAME_BETA) != null
                || pm.getLaunchIntentForPackage(PACKAGE_NAME_DEBUG) != null);
    }
}
