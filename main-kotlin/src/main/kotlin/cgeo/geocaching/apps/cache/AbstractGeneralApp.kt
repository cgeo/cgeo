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

package cgeo.geocaching.apps.cache

import cgeo.geocaching.apps.AbstractApp
import cgeo.geocaching.apps.navi.CacheNavigationApp
import cgeo.geocaching.models.Geocache

import android.content.Context
import android.content.Intent

import androidx.annotation.NonNull

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

abstract class AbstractGeneralApp : AbstractApp() : CacheNavigationApp {

    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    protected AbstractGeneralApp(final String name, final String packageName) {
        super(name, null, packageName)
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        val intent: Intent = getLaunchIntent()
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            context.startActivity(intent)
        }
    }
}
