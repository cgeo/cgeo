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

package cgeo.geocaching.log

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.models.Geocache

import android.view.View
import android.view.View.OnClickListener

class EditOfflineLogListener : OnClickListener {

    private final Geocache cache
    private final CacheDetailActivity activity

    EditOfflineLogListener(final Geocache cache, final CacheDetailActivity activity) {
        this.cache = cache
        this.activity = activity
    }

    override     public Unit onClick(final View v) {
        activity.setNeedsRefresh()
        cache.logVisit(activity)
    }

}
