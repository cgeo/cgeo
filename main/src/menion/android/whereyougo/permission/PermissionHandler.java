package menion.android.whereyougo.permission;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cgeo.geocaching.R;
import menion.android.whereyougo.utils.Logger;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import static menion.android.whereyougo.preferences.Locale.getString;

public class PermissionHandler {

    private static final String TAG = "PermissionHandler";

    private PermissionHandler() {
        // Utility class should not be instantiated externally
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void checkPermissions(final Activity activity) {

        final String[] permissions = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        String[] koPermissions = checkKoPermissions(activity, permissions);
        if (koPermissions.length > 0) {
            askFor(activity, koPermissions);
        }
    }

    public static String[] checkKoPermissions(final Activity activity, final String[] permissions) {
    List<String> listKoPermissions = new ArrayList<>();
        for (String permission: permissions) {
            if (checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                listKoPermissions.add(permission);
            }
        }
        return listKoPermissions.toArray(new String[0]);
    }

    /*
     * Check if version is marshmallow and above.
     * Used in deciding to ask runtime permission
     * */
    public static boolean needAskForPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static void askAgainFor(final Activity activity, String[] permissions) {
        int permissionsDenied = 0;
        HashMap<String, Integer> permissionClasses = new HashMap<>();
        for (String permission:permissions) {
            permissionClasses.put(getPermissionClass(permission), 1);
            final boolean currShouldShowStatus = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
            if (!currShouldShowStatus) {
                permissionsDenied++;
            }
        }

        String permissionsMessage = "";
        for (String permissionClass: permissionClasses.keySet()) {
            permissionsMessage = permissionsMessage.concat((permissionsMessage.isEmpty() ? "" : "\n\n")).concat(getPermissionMessage(permissionClass));
        }
        permissionsMessage = permissionsMessage.concat("\n\n").concat(getString(R.string.permission_conclusion));

        if (permissionsDenied == 0) {
            // Show permission explanation dialog...
            new AlertDialog.Builder(activity)
                    .setMessage(permissionsMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ask_again, (dialog, which) -> askFor(activity, permissions))
                    .create()
                    .show();
        } else {
            // Never ask again selected, or device policy prohibits the app from having that permission.
            // So, disable that feature, or fall back to another situation...
            new AlertDialog.Builder(activity)
                    .setMessage(permissionsMessage)
                    .setCancelable(false)
                    .setNegativeButton(R.string.close_app, (dialog, which) -> activity.finish())
                    .setPositiveButton(R.string.goto_app_details, (dialog, which) -> {
                                final Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", activity.getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent);
                                activity.finish();
                            }
                    )
                    .create()
                    .show();
        }
    }

    private static void askFor(final Activity activity, final String[] permissions) {
        ActivityCompat.requestPermissions(activity, permissions, 0);
    }

    private static String getPermissionClass(final String permission) {
        if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) || permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return "LOCATION";
        } else if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE) || permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return "STORAGE";
        }

        Logger.w(TAG, "getPermissionClass(), unknown permission: " + permission);
        return "";
    }

    private static String getPermissionMessage(final String permissionClass) {
        if (permissionClass.equals("LOCATION")) {
            return getString(R.string.location_permission_request_explanation);
        } else if (permissionClass.equals("STORAGE")) {
            return getString(R.string.storage_permission_request_explanation);
        }

        Logger.w(TAG, "getPermissionMessage(), unknown permission class: " + permissionClass);
        return "";
    }
}
