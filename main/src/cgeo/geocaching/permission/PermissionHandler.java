package cgeo.geocaching.permission;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.util.HashMap;

public class PermissionHandler {

    private static HashMap<Integer, Entry> callbackRegistry = new HashMap<>();

    private static class Entry {
        PermissionGrantedCallback callback;
        String[] permissions;

        public Entry(PermissionGrantedCallback callback, String[] permissions) {
            this.permissions = permissions;
            this.callback = callback;
        }
    }

    public static void executeIfLocationPermissionGranted(Activity activity, int requestCode, PermissionGrantedCallback callback) {
        final String[] locationPermissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ActivityCompat.checkSelfPermission(activity, locationPermissions[0]) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, locationPermissions[1]) != PackageManager.PERMISSION_GRANTED) {
            callbackRegistry.put(requestCode, new Entry(callback, locationPermissions));
            ActivityCompat.requestPermissions(activity, locationPermissions, requestCode);
        }
    }

    public static void executeCallbackFor(int requestCode) {
        if (callbackRegistry.containsKey(requestCode)) {
            callbackRegistry.get(requestCode).callback.execute();
            callbackRegistry.remove(requestCode);
        } else {
            throw new IllegalArgumentException("No registered entry for requestCode " + requestCode);
        }
    }

    public static void askAgainFor(int requestCode, Activity activity) {
        if (callbackRegistry.containsKey(requestCode)) {
            ActivityCompat.requestPermissions(activity, callbackRegistry.get(requestCode).permissions, requestCode);
        } else {
            throw new IllegalArgumentException("No registered entry for requestCode " + requestCode);
        }
    }
}
