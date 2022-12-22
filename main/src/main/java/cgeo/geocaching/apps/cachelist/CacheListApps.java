package cgeo.geocaching.apps.cachelist;

import java.util.ArrayList;
import java.util.List;

public enum CacheListApps {

    INTERNAL(new InternalCacheListMap()),
    LOCUS_SHOW(new LocusShowCacheListApp()),
    LOCUS_EXPORT(new LocusExportCacheListApp()),
    MAPS_ME(new MapsMeCacheListApp());

    private final CacheListApp app;

    CacheListApps(final CacheListApp app) {
        this.app = app;
    }

    public static List<CacheListApp> getActiveApps() {
        final List<CacheListApp> activeApps = new ArrayList<>();
        for (final CacheListApps appEnum : values()) {
            if (appEnum.app.isInstalled()) {
                activeApps.add(appEnum.app);
            }
        }
        return activeApps;
    }

}

