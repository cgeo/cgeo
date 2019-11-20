package cgeo.contacts.permission;

import cgeo.contacts.R;

public enum PermissionRequestContext {

    ContactsActivity(1114, R.string.contacts_permission_request_explanation, R.string.contacts_permission_request_denied);

    private final int requestCode;
    private final int askAgainResource;
    private final int deniedResource;

    PermissionRequestContext(final int requestCode, final int askAgainResource, final int deniedResource) {
        this.requestCode = requestCode;
        this.askAgainResource = askAgainResource;
        this.deniedResource = deniedResource;
    }

    public int getRequestCode() {
        return requestCode;
    }

    public int getAskAgainResource() {
        return askAgainResource;
    }

    public int getDeniedResource() {
        return deniedResource;
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
