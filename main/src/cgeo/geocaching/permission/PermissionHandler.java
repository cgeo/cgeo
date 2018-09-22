package cgeo.geocaching.permission;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PermissionHandler {

    private static HashMap<PermissionKey, List<PermissionGrantedCallback>> callbackRegistry = new HashMap<>();

    private PermissionHandler() {
        // Utility class should not be instantiated externally
    }

    public static void executeIfLocationPermissionGranted(final Activity activity, final PermissionGrantedCallback callback) {
        final String[] locationPermissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        final PermissionKey pk = new PermissionKey(locationPermissions);

        if (ActivityCompat.checkSelfPermission(activity, locationPermissions[0]) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, locationPermissions[1]) != PackageManager.PERMISSION_GRANTED) {
            if (!callbackRegistry.containsKey(pk)) {
                callbackRegistry.put(pk, new ArrayList<PermissionGrantedCallback>());
                ActivityCompat.requestPermissions(activity, locationPermissions, callback.getRequestCode());
            }

            boolean callbackHasAlreadyBeenRegistered = false;
            for (final PermissionGrantedCallback permissionGrantedCallback : callbackRegistry.get(pk)) {
                if (permissionGrantedCallback.getRequestCode() == callback.getRequestCode()) {
                    callbackHasAlreadyBeenRegistered = true;
                    break;
                }
            }
            if (!callbackHasAlreadyBeenRegistered) {
                callbackRegistry.get(pk).add(callback);
            }
        } else {
            callback.execute();
            executeCallbacksFor(locationPermissions);
        }
    }

    public static void executeCallbacksFor(final String[] permissions) {
        final PermissionKey pk = new PermissionKey(permissions);
        if (callbackRegistry.containsKey(pk)) {
            for (final PermissionGrantedCallback callback : callbackRegistry.get(pk)) {
                callback.execute();
            }
            callbackRegistry.remove(pk);
        }
    }

    public static void askAgainFor(final String[] permissions, final Activity activity) {
        if (callbackRegistry.containsKey(new PermissionKey(permissions))) {
            ActivityCompat.requestPermissions(activity, permissions, 2222);
        } else {
            throw new IllegalArgumentException("No registered callback for permissions " + Arrays.toString(permissions));
        }
    }

    private static class PermissionKey {
        private String[] permissions;

        private PermissionKey(final String[] permissions) {
            this.permissions = permissions;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (final String permission : permissions) {
                hash += permission.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof PermissionKey)) {
                return false;
            }
            final PermissionKey pk = (PermissionKey) o;
            if (pk.permissions.length != permissions.length) {
                return false;
            }

            for (int i = 0; i < pk.permissions.length; i++) {
                for (int j = 0; j < permissions.length; j++) {
                    if (!pk.permissions[i].equals(permissions[j]) && permissions.length == j - 1) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

}

