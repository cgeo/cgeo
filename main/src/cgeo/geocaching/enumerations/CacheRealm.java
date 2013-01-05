package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

public enum CacheRealm {

    GC("gc", "geocaching.com", R.drawable.marker, R.drawable.marker_disabled),
    OC("oc", "OpenCaching Network", R.drawable.marker_oc, R.drawable.marker_disabled_oc),
    OTHER("other", "Other", R.drawable.marker_other, R.drawable.marker_disabled_other);

    public final String id;
    public final String name;
    public final int markerId;
    public final int markerDisabledId;

    CacheRealm(String id, String name, int markerId, int markerDisabledId) {
        this.id = id;
        this.name = name;
        this.markerId = markerId;
        this.markerDisabledId = markerDisabledId;
    }
}
