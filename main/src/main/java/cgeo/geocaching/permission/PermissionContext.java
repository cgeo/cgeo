package cgeo.geocaching.permission;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.LocalizationUtils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;

public enum PermissionContext {

    LOCATION(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, R.string.location_data_permission_request_explanation, R.string.location_data_permission_request_explanation_title),
    WRITE_EXTERNAL_STORAGE(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, R.string.external_storage_permission_request_explanation, R.string.external_storage_permission_request_explanation_title),
    SEARCH_USER_IN_CONTACTS(new String[]{Manifest.permission.READ_CONTACTS}, R.string.contacts_read_permission_request_explanation, R.string.contacts_read_permission_request_explanation_title);


    private final String[] permissions;
    @StringRes
    private final int explanationId;
    @StringRes
    private final int explanationTitleId;

    PermissionContext(final String[] permissions, @StringRes final int explanationId, @StringRes final int explanationTitleId) {
        this.permissions = permissions;
        this.explanationId = explanationId;
        this.explanationTitleId = explanationTitleId;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public TextParam getExplanation() {
        return TextParam.id(explanationId);
    }

    public String getExplanationTitle() {
        return LocalizationUtils.getString(explanationTitleId);
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

    public Set<String> getNotGrantedPermissions() {
        final Context ctx = CgeoApplication.getInstance().getApplicationContext();
        final Set<String> result = new HashSet<>();
        for (String permission : getPermissions()) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
                result.add(permission);
            }
        }
        return result;
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
