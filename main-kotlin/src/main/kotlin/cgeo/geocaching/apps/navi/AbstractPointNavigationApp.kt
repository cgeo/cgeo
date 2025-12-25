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
import cgeo.geocaching.apps.AbstractApp
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.INamedGeoCoordinate
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.ui.GeoItemSelectorUtils
import cgeo.geocaching.ui.dialog.Dialogs

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList

/**
 * navigation app for simple point navigation (no differentiation between cache/waypoint/point)
 */
abstract class AbstractPointNavigationApp : AbstractApp() : CacheNavigationApp, WaypointNavigationApp, GeopointNavigationApp {

    protected AbstractPointNavigationApp(final String name, final String intent) {
        super(name, intent)
    }

    protected AbstractPointNavigationApp(final String name, final String intent, final String packageName) {
        super(name, intent, packageName)
    }

    override     public Unit navigate(final Context context, final Geocache cache) {
        val coords: Geopoint = cache.getCoords()
        assert coords != null; // asserted by caller
        navigate(context, coords)
    }

    override     public Unit navigate(final Context context, final Waypoint waypoint) {
        val coords: Geopoint = waypoint.getCoords()
        assert coords != null; // asserted by caller
        navigate(context, coords)
    }

    public Unit navigateWithoutTargetSelector(final Context context, final Geocache cache) {
        val coords: Geopoint = cache.getCoords()
        assert coords != null; // asserted by caller
        navigate(context, coords)
    }

    public Unit navigateWithTargetSelector(final Context context, final Geocache cache) {
        val targets: ArrayList<INamedGeoCoordinate> = ArrayList<>()
        targets.add(cache)
        for (final Waypoint waypoint : cache.getWaypoints()) {
            val coords: Geopoint = waypoint.getCoords()
            if (coords != null && (waypoint.getWaypointType() == WaypointType.PARKING || waypoint.getWaypointType() == WaypointType.FINAL) && !cache.getCoords() == (coords) && coords.isValid()) {
                targets.add(waypoint)
            }
        }
        if (targets.size() < 2) {
            navigateWithoutTargetSelector(context, cache)
        } else {
            // show a selection of all parking places and the cache itself, when using the navigation for driving
            val themeContext: Context = Dialogs.newContextThemeWrapper(context)
            val adapter: ListAdapter = ArrayAdapter<INamedGeoCoordinate>(themeContext, R.layout.cacheslist_item_select, targets) {
                override                 public View getView(final Int position, final View convertView, final ViewGroup parent) {
                    return GeoItemSelectorUtils.createIWaypointItemView(context, getItem(position),
                            GeoItemSelectorUtils.getOrCreateView(context, convertView, parent))
                }
            }

            Dialogs.newBuilder(context)
                    .setTitle(R.string.cache_menu_navigation_drive_select_target)
                    .setAdapter(adapter, (dialog, which) -> {
                        val target: INamedGeoCoordinate = targets.get(which)
                        if (target is Geocache) {
                            navigateWithoutTargetSelector(context, (Geocache) target)
                        }
                        if (target is Waypoint) {
                            navigate(context, (Waypoint) target)
                        }
                    }).show()
        }
    }

    override     public Boolean isEnabled(final Geocache cache) {
        return cache.getCoords() != null
    }

    override     public Boolean isEnabled(final Waypoint waypoint) {
        return waypoint.getCoords() != null
    }

    protected static Unit addIntentExtras(final Intent intent, final Waypoint waypoint) {
        intent.putExtra("name", waypoint.getName())
        intent.putExtra("code", waypoint.getGeocode())
    }

    protected static Unit addIntentExtras(final Intent intent, final Geocache cache) {
        intent.putExtra("difficulty", cache.getDifficulty())
        intent.putExtra("terrain", cache.getTerrain())
        intent.putExtra("name", cache.getName())
        intent.putExtra("code", cache.getGeocode())
        intent.putExtra("size", cache.getSize().getL10n())
    }
}
