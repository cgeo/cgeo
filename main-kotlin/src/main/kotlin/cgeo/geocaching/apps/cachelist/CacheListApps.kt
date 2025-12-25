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

package cgeo.geocaching.apps.cachelist

import java.util.ArrayList
import java.util.List

enum class class CacheListApps {

    INTERNAL(InternalCacheListMap()),
    LOCUS_SHOW(LocusShowCacheListApp()),
    LOCUS_EXPORT(LocusExportCacheListApp()),
    MAPS_ME(MapsMeCacheListApp())

    private final CacheListApp app

    CacheListApps(final CacheListApp app) {
        this.app = app
    }

    public static List<CacheListApp> getActiveApps() {
        val activeApps: List<CacheListApp> = ArrayList<>()
        for (final CacheListApps appEnum : values()) {
            if (appEnum.app.isInstalled()) {
                activeApps.add(appEnum.app)
            }
        }
        return activeApps
    }

}

