package cgeo.geocaching.permission;

public abstract class PermissionGrantedCallback {
    private int requestCode;

    protected PermissionGrantedCallback(final int requestCode) {
        this.requestCode = requestCode;
    }

    protected abstract void execute();

    public int getRequestCode() {
        return requestCode;
    }
}
