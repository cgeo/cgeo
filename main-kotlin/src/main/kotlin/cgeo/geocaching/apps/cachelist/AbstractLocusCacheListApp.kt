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
import cgeo.geocaching.apps.AbstractLocusApp
import cgeo.geocaching.models.Geocache

import android.app.Activity
import android.content.Intent

import androidx.annotation.NonNull

import java.util.List

import org.apache.commons.collections4.CollectionUtils

abstract class AbstractLocusCacheListApp : AbstractLocusApp() : CacheListApp {

    private final Boolean export

    AbstractLocusCacheListApp(final Boolean export) {
        super(getString(export ? R.string.caches_map_locus_export : R.string.caches_map_locus), Intent.ACTION_VIEW)
        this.export = export
    }

    /**
     * show caches in Locus
     *
     * @see AbstractLocusApp#showInLocus
     */
    override     public Unit invoke(final List<Geocache> cacheList, final Activity activity, final SearchResult search) {
        if (CollectionUtils.isEmpty(cacheList)) {
            return
        }

        showInLocus(cacheList, false, export, activity)
    }

}
