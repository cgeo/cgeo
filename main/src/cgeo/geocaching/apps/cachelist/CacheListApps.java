package cgeo.geocaching.apps.cachelist;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.NewMap;

import java.util.ArrayList;
import java.util.List;

public enum CacheListApps {

    INTERNAL(new InternalCacheListMap()),
    INTERNAL_NEW(new InternalCacheListMap(NewMap.class, R.string.cache_menu_mfbeta)),
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

