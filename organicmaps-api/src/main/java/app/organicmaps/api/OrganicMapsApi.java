package app.organicmaps.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

public class OrganicMapsApi {

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
        if (isOrganicMapsInstalled(caller)) {
            caller.startActivity(intent);
        } else {
            new DownloadDialog(caller).show();
        }
    }

    /**
     * Detects if any version of OrganicMaps is installed on the device
     */
    public static boolean isOrganicMapsInstalled(final Context context) {
        final ComponentName c = new MapRequest().toIntent().resolveActivity(context.getPackageManager());
        return c != null;
    }

}
