package cgeo.contacts.permission;

import cgeo.contacts.R;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHandler {

    private static final String contactsPermission = Manifest.permission.READ_CONTACTS;

    private PermissionHandler() {
        // Utility class should not be instantiated externally
    }

    /*
     * Check if version is marshmallow and above.
     * Used in deciding to ask runtime permission
     * */
    private static boolean notNeedAskForPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
    }

    public static boolean requestContactsPermission(final Activity activity) {

        final PermissionRequestContext perm = PermissionRequestContext.ContactsActivity;
        return requestPermission(activity, contactsPermission, perm);
    }

    private static boolean requestPermission(final Activity activity, final String permission, final PermissionRequestContext perm) {

        if (notNeedAskForPermission()) {
            return true;
        }

        if (hasPermission(activity, permission)) {
            return true;
        }

        askFor(activity, permission, perm);
        return false;
    }

    private static void askFor(final Activity activity, final String permission, final PermissionRequestContext perm) {
        askFor(activity, new String[] {permission}, perm);
    }

    private static void askFor(final Activity activity, final String[] permissions, final PermissionRequestContext perm) {
        ActivityCompat.requestPermissions(activity, permissions, perm.getRequestCode());
    }

    public static void askAgainFor(final Activity activity, final int requestCode) {
        if (requestCode == PermissionRequestContext.ContactsActivity.getRequestCode()) {
            askAgainForContacts(activity);
        }
    }

    private static void askAgainForContacts(final Activity activity) {
        final boolean currShouldShowStatus = ActivityCompat.shouldShowRequestPermissionRationale(activity, contactsPermission);
        final PermissionRequestContext perm = PermissionRequestContext.ContactsActivity;
        if (currShouldShowStatus) {
            // Show permission explanation dialog...
            new AlertDialog.Builder(activity)
                    .setMessage(perm.getAskAgainResource())
                    .setCancelable(false)
                    .setNeutralButton(R.string.ask_again, (dialog, which) -> askFor(activity, new String[] {contactsPermission}, perm))
                    .create()
                    .show();
        } else {
            // Never ask again selected, or device policy prohibits the app from having that permission.
            // So, disable that feature, or fall back to another situation...
            new AlertDialog.Builder(activity)
                    .setMessage(perm.getDeniedResource())
                    .setCancelable(false)
                    .setNegativeButton(R.string.close, (dialog, which) -> activity.finish())
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

    private static boolean hasPermission(final Activity activity, final String permission) {
        return hasPermission(activity, new String[] {permission});
    }

    private static boolean hasPermission(final Activity activity, final String[] permissions) {
        boolean hasPermission = true;
        for (String permission: permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                hasPermission = false;
            }
        }

        return hasPermission;
    }
}
