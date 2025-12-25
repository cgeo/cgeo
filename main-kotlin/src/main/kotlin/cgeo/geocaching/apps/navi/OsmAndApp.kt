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

import cgeo.geocaching.R
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.utils.ProcessUtils

import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.apache.commons.collections4.CollectionUtils

class OsmAndApp : AbstractPointNavigationApp() {

    private static val PARAM_NAME: String = "name"
    private static val PARAM_LAT: String = "lat"
    private static val PARAM_LON: String = "lon"
    private static val PREFIX: String = "osmand.api://"
    private static val GET_INFO: String = "get_info"
    private static val ADD_MAP_MARKER: String = "add_map_marker"

    protected OsmAndApp() {
        super(getString(R.string.cache_menu_osmand), null)
    }

    override     public Boolean isInstalled() {
        return ProcessUtils.isIntentAvailable(Intent.ACTION_VIEW, Uri.parse(PREFIX + GET_INFO))
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        val coords: Geopoint = cache.getCoords()
        assert coords != null; // guaranteed by super class
        navigate(context, coords, cache.getName())
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        val coords: Geopoint = waypoint.getCoords()
        assert coords != null; // guaranteed by super class
        navigate(context, coords, waypoint.getName())
    }

    override     public Unit navigate(final Context context, final Geopoint coords) {
        navigate(context, coords, context.getString(R.string.osmand_marker_cgeo))
    }

    private static Unit navigate(final Context context, final Geopoint coords, final String markerName) {
        val params: Parameters = Parameters(PARAM_LAT, String.valueOf(coords.getLatitude()),
                PARAM_LON, String.valueOf(coords.getLongitude()),
                PARAM_NAME, markerName == null ? "" : markerName)
        context.startActivity(buildIntent(params))
    }

    private static Intent buildIntent(final Parameters parameters) {
        val stringBuilder: StringBuilder = StringBuilder(PREFIX)
        stringBuilder.append(ADD_MAP_MARKER)
        if (CollectionUtils.isNotEmpty(parameters)) {
            stringBuilder.append('?')
            stringBuilder.append(parameters)
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse(stringBuilder.toString()))
    }
}
