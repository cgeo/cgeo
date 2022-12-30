package cgeo.geocaching.permission;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public enum PermissionContext {

    SEARCH_USER_IN_CONTACTS(new String[]{Manifest.permission.READ_CONTACTS}, R.string.contacts_permission_request_explanation);

    private final String[] permissions;
    @StringRes
    private final int explanationId;

    PermissionContext(final String[] permissions, @StringRes final int explanationId) {
        this.permissions = permissions;
        this.explanationId = explanationId;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public TextParam getExplanation() {
        return TextParam.id(explanationId);
    }

    public boolean hasAllPermissions() {
        final Context ctx = CgeoApplication.getInstance().getApplicationContext();
        for (String permission : getPermissions()) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public boolean shouldShowRequestPermissionRationale(@NonNull final Activity activity) {
        final Context ctx = CgeoApplication.getInstance().getApplicationContext();
        for (String permission : getPermissions()) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

}
