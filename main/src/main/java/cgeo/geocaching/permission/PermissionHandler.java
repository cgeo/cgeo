package cgeo.geocaching.permission;

import cgeo.geocaching.storage.LocalStorage;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class PermissionHandler {

    private static final HashMap<PermissionKey, List<PermissionGrantedCallback>> callbackRegistry = new HashMap<>();

    private PermissionHandler() {
        // Utility class should not be instantiated externally
    }

    public static void executeIfLocationPermissionGranted(final Activity activity, final PermissionGrantedCallback callback) {
        final String[] locationPermissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        final PermissionKey pk = new PermissionKey(locationPermissions);

//        if (ContextCompat.checkSelfPermission(activity, locationPermissions[0]) != PackageManager.PERMISSION_GRANTED) {
//            if (!callbackRegistry.containsKey(pk)) {
//                callbackRegistry.put(pk, new ArrayList<>());
//                ActivityCompat.requestPermissions(activity, locationPermissions, callback.getRequestCode());
//            }
//
//            boolean callbackHasAlreadyBeenRegistered = false;
//            for (final PermissionGrantedCallback permissionGrantedCallback : callbackRegistry.get(pk)) {
//                if (permissionGrantedCallback.getRequestCode() == callback.getRequestCode()) {
//                    callbackHasAlreadyBeenRegistered = true;
//                    break;
//                }
//            }
//            if (!callbackHasAlreadyBeenRegistered) {
//                callbackRegistry.get(pk).add(callback);
//            }
//        } else {
            callback.execute();
            executeCallbacksFor(locationPermissions);
//        }
    }

    public static void requestStoragePermission(final Activity activity, final PermissionGrantedCallback requestContext) {
        final String[] storagePermission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
//        final PermissionKey pk = new PermissionKey(storagePermission);
//
//        if (ContextCompat.checkSelfPermission(activity, storagePermission[0]) != PackageManager.PERMISSION_GRANTED) {
//            if (!callbackRegistry.containsKey(pk)) {
//                callbackRegistry.put(pk, new ArrayList<>());
//                ActivityCompat.requestPermissions(activity, storagePermission, requestContext.getRequestCode());
//            }
//
//            boolean callbackHasAlreadyBeenRegistered = false;
//            for (final PermissionGrantedCallback permissionGrantedCallback : callbackRegistry.get(pk)) {
//                if (permissionGrantedCallback.getRequestCode() == requestContext.getRequestCode()) {
//                    callbackHasAlreadyBeenRegistered = true;
//                    break;
//                }
//            }
//            if (!callbackHasAlreadyBeenRegistered) {
//                callbackRegistry.get(pk).add(requestContext);
//
//                final PermissionGrantedCallback pgc = new PermissionGrantedCallback(requestContext.getContext()) {
//                    @Override
//                    protected void execute() {
//                        LocalStorage.resetExternalPublicCgeoDirectory();
//                    }
//                };
//                callbackRegistry.get(pk).add(pgc);
//            }
//        } else {
            requestContext.execute();
            executeCallbacksFor(storagePermission);
//        }
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

    public static void askAgainFor(final String[] permissions, final Activity activity, final PermissionRequestContext perm) {
        if (callbackRegistry.containsKey(new PermissionKey(permissions))) {
            ActivityCompat.requestPermissions(activity, permissions, perm.getRequestCode());
        } else {
            throw new IllegalArgumentException("No registered callback for permissions " + Arrays.toString(permissions));
        }
    }

    private static class PermissionKey {
        private final String[] permissions;

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
