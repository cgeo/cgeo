package cgeo.geocaching.permission;

import cgeo.geocaching.R;

public enum PermissionRequestContext {

    MainActivityOnCreate(1111, R.string.location_permission_request_explanation),
    MainActivityOnResume(1112, R.string.location_permission_request_explanation),
    MainActivityStorage(1113, R.string.storage_permission_request_explanation),
    // placeholder, see contacts addon
    // ContactsActivity(1114, R.string.contacts_permission_request_explanation, R.string.contacts_permission_request_denied);
    ReceiveMapFileActivity(1115, R.string.storage_permission_request_explanation),
    TrackableActivity(2221, R.string.location_permission_request_explanation),
    CacheDetailActivity(2222, R.string.location_permission_request_explanation),
    CacheListActivity(2223, R.string.location_permission_request_explanation),
    CGeoMap(2224, R.string.location_permission_request_explanation),
    NewMap(2225, R.string.location_permission_request_explanation),
    EditWaypointActivity(2226, R.string.location_permission_request_explanation),
    CompassActivity(2227, R.string.location_permission_request_explanation),
    AbstractDialogFragment(2228, R.string.location_permission_request_explanation),
    NavigateAnyPointActivity(2229, R.string.location_permission_request_explanation),
    LogCacheActivity(2230, R.string.location_permission_request_explanation),
    InstallWizardActivity(3001, R.string.location_permission_request_explanation);

    private final int requestCode;
    private final int askAgainResource;

    PermissionRequestContext(final int requestCode, final int askAgainResource) {
        this.requestCode = requestCode;
        this.askAgainResource = askAgainResource;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public int getAskAgainResource() {
        return askAgainResource;
    }

    public static PermissionRequestContext fromRequestCode(final int requestCode) {
        for (final PermissionRequestContext perm : PermissionRequestContext.values()) {
            if (perm.requestCode == requestCode) {
                return perm;
            }
        }
        return null;
        //throw new IndexOutOfBoundsException("Unknown request code " + requestCode);
    }
}
