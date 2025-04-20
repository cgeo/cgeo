package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.INamedGeoCoordinate;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * navigation app for simple point navigation (no differentiation between cache/waypoint/point)
 */
abstract class AbstractPointNavigationApp extends AbstractApp implements CacheNavigationApp, WaypointNavigationApp, GeopointNavigationApp {

    protected AbstractPointNavigationApp(@NonNull final String name, @Nullable final String intent) {
        super(name, intent);
    }

    protected AbstractPointNavigationApp(@NonNull final String name, @Nullable final String intent, @Nullable final String packageName) {
        super(name, intent, packageName);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        final Geopoint coords = cache.getCoords();
        assert coords != null; // asserted by caller
        navigate(context, coords);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        final Geopoint coords = waypoint.getCoords();
        assert coords != null; // asserted by caller
        navigate(context, coords);
    }

    public void navigateWithoutTargetSelector(@NonNull final Context context, @NonNull final Geocache cache) {
        final Geopoint coords = cache.getCoords();
        assert coords != null; // asserted by caller
        navigate(context, coords);
    }

    public void navigateWithTargetSelector(@NonNull final Context context, @NonNull final Geocache cache) {
        final ArrayList<INamedGeoCoordinate> targets = new ArrayList<>();
        targets.add(cache);
        for (final Waypoint waypoint : cache.getWaypoints()) {
            final Geopoint coords = waypoint.getCoords();
            if (coords != null && (waypoint.getWaypointType() == WaypointType.PARKING || waypoint.getWaypointType() == WaypointType.FINAL) && !cache.getCoords().equals(coords) && coords.isValid()) {
                targets.add(waypoint);
            }
        }
        if (targets.size() < 2) {
            navigateWithoutTargetSelector(context, cache);
        } else {
            // show a selection of all parking places and the cache itself, when using the navigation for driving
            final Context themeContext = Dialogs.newContextThemeWrapper(context);
            final ListAdapter adapter = new ArrayAdapter<INamedGeoCoordinate>(themeContext, R.layout.cacheslist_item_select, targets) {
                @NonNull
                @Override
                public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                    return GeoItemSelectorUtils.createIWaypointItemView(context, getItem(position),
                            GeoItemSelectorUtils.getOrCreateView(context, convertView, parent));
                }
            };

            Dialogs.newBuilder(context)
                    .setTitle(R.string.cache_menu_navigation_drive_select_target)
                    .setAdapter(adapter, (dialog, which) -> {
                        final INamedGeoCoordinate target = targets.get(which);
                        if (target instanceof Geocache) {
                            navigateWithoutTargetSelector(context, (Geocache) target);
                        }
                        if (target instanceof Waypoint) {
                            navigate(context, (Waypoint) target);
                        }
                    }).show();
        }
    }

    @Override
    public boolean isEnabled(@NonNull final Geocache cache) {
        return cache.getCoords() != null;
    }

    @Override
    public boolean isEnabled(@NonNull final Waypoint waypoint) {
        return waypoint.getCoords() != null;
    }

    protected static void addIntentExtras(@NonNull final Intent intent, @NonNull final Waypoint waypoint) {
        intent.putExtra("name", waypoint.getName());
        intent.putExtra("code", waypoint.getGeocode());
    }

    protected static void addIntentExtras(@NonNull final Intent intent, @NonNull final Geocache cache) {
        intent.putExtra("difficulty", cache.getDifficulty());
        intent.putExtra("terrain", cache.getTerrain());
        intent.putExtra("name", cache.getName());
        intent.putExtra("code", cache.getGeocode());
        intent.putExtra("size", cache.getSize().getL10n());
    }
}
