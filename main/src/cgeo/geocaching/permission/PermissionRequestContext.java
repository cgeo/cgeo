package cgeo.geocaching.permission;

public enum PermissionRequestContext {

    MainActivityOnCreate(1111),
    MainActivityOnResume(1112),
    TrackableActivity(2222),
    CacheDetailActivity(3333),
    CacheListActivity(4444);

    private final int requestCode;

    PermissionRequestContext(int requestCode) {
        this.requestCode = requestCode;
    }

    public int getRequestCode() {
        return requestCode;
    }
}
