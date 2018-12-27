package cgeo.geocaching.permission;

import cgeo.geocaching.R;

public enum PermissionRequestContext {

    MainActivityOnCreate(1111, R.string.location_permission_request_explanation),
    MainActivityOnResume(1112, R.string.location_permission_request_explanation),
    MainActivityStorage(1113, R.string.storage_permission_request_explanation),
    TrackableActivity(2222, R.string.location_permission_request_explanation),
    CacheDetailActivity(3333, R.string.location_permission_request_explanation),
    CacheListActivity(4444, R.string.location_permission_request_explanation);

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

        throw new IndexOutOfBoundsException("Unknown request code " + requestCode);
    }
}
