// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.permission

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.utils.LocalizationUtils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager

import androidx.annotation.NonNull
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import java.util.HashSet
import java.util.Set

enum class class PermissionContext {

    LOCATION(String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, R.string.permission_location_explanation, R.string.permission_location_explanation_title),
    SEARCH_USER_IN_CONTACTS(String[]{Manifest.permission.READ_CONTACTS}, R.string.permission_contacts_read_explanation, R.string.permission_contacts_read_explanation_title),
    LEGACY_WRITE_EXTERNAL_STORAGE(String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, R.string.permission_legacy_write_external_storage_explanation, R.string.permission_legacy_write_external_storage_explanation_title),
    @TargetApi(33) // permission will only be requested on API 33+ devices (see InstallWizardActivity)
    NOTIFICATIONS(String[]{Manifest.permission.POST_NOTIFICATIONS}, R.string.permission_post_notifications_explanation, R.string.permission_post_notifications_explanation_title)

    private final String[] permissions
    @StringRes
    private final Int explanationId
    @StringRes
    private final Int explanationTitleId

    PermissionContext(final String[] permissions, @StringRes final Int explanationId, @StringRes final Int explanationTitleId) {
        this.permissions = permissions
        this.explanationId = explanationId
        this.explanationTitleId = explanationTitleId
    }

    public String[] getPermissions() {
        return permissions
    }

    public TextParam getExplanation() {
        return TextParam.id(explanationId)
    }

    public String getExplanationTitle() {
        return LocalizationUtils.getString(explanationTitleId)
    }

    public Boolean hasAllPermissions() {
        val ctx: Context = CgeoApplication.getInstance().getApplicationContext()
        for (String permission : getPermissions()) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    public Set<String> getNotGrantedPermissions() {
        val ctx: Context = CgeoApplication.getInstance().getApplicationContext()
        val result: Set<String> = HashSet<>()
        for (String permission : getPermissions()) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED) {
                result.add(permission)
            }
        }
        return result
    }

    public Boolean shouldShowRequestPermissionRationale(final Activity activity) {
        val ctx: Context = CgeoApplication.getInstance().getApplicationContext()
        for (String permission : getPermissions()) {
            if (ContextCompat.checkSelfPermission(ctx, permission) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true
            }
        }
        return false
    }

}
