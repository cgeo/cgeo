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

package cgeo.geocaching.apps.navi

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint

import android.content.Context
import android.content.Intent

import androidx.annotation.NonNull

abstract class AbstractRadarApp : AbstractPointNavigationApp() {

    protected static val RADAR_EXTRA_LONGITUDE: String = "longitude"
    protected static val RADAR_EXTRA_LATITUDE: String = "latitude"

    private final String intentAction

    protected AbstractRadarApp(final String name, final String intent, final String packageName) {
        super(name, intent, packageName)
        this.intentAction = intent
    }

    private Intent createIntent(final Geopoint point) {
        val intent: Intent = Intent(intentAction)
        addCoordinates(intent, point)
        return intent
    }

    override     public Unit navigate(final Context context, final Geopoint point) {
        context.startActivity(createIntent(point))
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        val intent: Intent = createIntent(cache.getCoords())
        addIntentExtras(intent, cache)
        context.startActivity(intent)
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        val intent: Intent = createIntent(waypoint.getCoords())
        addIntentExtras(intent, waypoint)
        context.startActivity(intent)
    }

    protected abstract Unit addCoordinates(Intent intent, Geopoint point)
}
