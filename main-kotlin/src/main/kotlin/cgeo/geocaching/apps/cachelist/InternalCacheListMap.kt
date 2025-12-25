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

import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.apps.AbstractApp
import cgeo.geocaching.maps.DefaultMap
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings

import android.app.Activity

import androidx.annotation.NonNull

import java.util.List

class InternalCacheListMap : AbstractApp() : CacheListApp {

    final Class<?> cls

    InternalCacheListMap(final Class<?> cls, final Int name) {
        super(getString(name), null)
        this.cls = cls
    }

    InternalCacheListMap() {
        super(getString(R.string.cache_menu_map), null)
        cls = null
    }

    override     public Boolean isInstalled() {
        return true
    }

    override     public Unit invoke(final List<Geocache> caches, final Activity activity, final SearchResult search) {
        DefaultMap.startActivitySearch(activity, cls != null ? cls : Settings.getMapProvider().getMapClass(), search, null)
    }
}
