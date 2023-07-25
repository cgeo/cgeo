package app.organicmaps.api;

/* added by c:geo project as temporary workaround until OrganicMaps API is completed */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import java.util.ArrayList;

public class OrganicMapsApi {

    static final String PACKAGE_NAME = "app.organicmaps";

    private OrganicMapsApi() {
        // utility class
    }

    public static void showPointOnMap(final Activity activity, final double lat, final double lon, final String name) {
        final ArrayList<Point> points = new ArrayList<>(1);
        points.add(new Point(lat, lon, name, String.valueOf(name.hashCode())));
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
        if (isOrganicMapsInstalled(caller)) {
            // Match activity for intent
            final ActivityInfo aInfo = caller.getPackageManager().resolveActivity(intent, 0).activityInfo;
            intent.setClassName(aInfo.packageName, aInfo.name);
            caller.startActivity(intent);
        } else {
            new DownloadDialog(caller).show();
        }
    }

    /**
     * Detects if any version (Lite, Pro) of OrganicMaps is installed on the device
     */
    public static boolean isOrganicMapsInstalled(final Context context) {
        final Intent i = context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
        return i != null;
    }

}
