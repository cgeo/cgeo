package cgeo.geocaching.permission;

public abstract class PermissionGrantedCallback {
    private PermissionRequestContext request;

    protected PermissionGrantedCallback(final PermissionRequestContext request) {
        this.request = request;
    }

    protected abstract void execute();

    public int getRequestCode() {
        return request.getRequestCode();
    }
}
