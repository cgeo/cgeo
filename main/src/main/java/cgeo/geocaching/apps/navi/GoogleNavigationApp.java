package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.ArrayList;

abstract class GoogleNavigationApp extends AbstractPointNavigationApp {

    private final String mode;

    private GoogleNavigationApp(@StringRes final int nameResourceId, final String mode) {
        super(getString(nameResourceId), null);
        this.mode = mode;
    }

    @Override
    public boolean isInstalled() {
        return true;
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("google.navigation:ll=" + coords.getLatitude() + ","
                            + coords.getLongitude() + "&mode=" + mode)));

        } catch (final Exception e) {
            Log.i("GoogleNavigationApp.navigate: No navigation application available.", e);
        }
    }

    static class GoogleNavigationWalkingApp extends GoogleNavigationApp {
        GoogleNavigationWalkingApp() {
            super(R.string.cache_menu_navigation_walk, "w");
        }
    }

    static class GoogleNavigationTransitApp extends GoogleNavigationApp {
        GoogleNavigationTransitApp() {
            super(R.string.cache_menu_navigation_transit, "r");
        }
    }

    static class GoogleNavigationDrivingApp extends GoogleNavigationApp {
        GoogleNavigationDrivingApp() {
            super(R.string.cache_menu_navigation_drive, "d");
        }

        @Override
        public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
            final ArrayList<IWaypoint> targets = new ArrayList<>();
            targets.add(cache);
            for (final Waypoint waypoint : cache.getWaypoints()) {
                if ((waypoint.getWaypointType() == WaypointType.PARKING || waypoint.getWaypointType() == WaypointType.FINAL) && !cache.getCoords().equals(waypoint.getCoords())) {
                    targets.add(waypoint);
                }
            }
            if (targets.size() > 1) {
                selectDriveTarget(context, targets);
            } else {
                super.navigate(context, cache);
            }
        }

        /**
         * show a selection of all parking places and the cache itself, when using the navigation for driving
         */
        private void selectDriveTarget(final Context context, final ArrayList<IWaypoint> targets) {
            final Context themeContext = Dialogs.newContextThemeWrapper(context);
            final ListAdapter adapter = new ArrayAdapter<IWaypoint>(themeContext, R.layout.cacheslist_item_select, targets) {
                @Override
                public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                    return GeoItemSelectorUtils.createIWaypointItemView(context, getItem(position),
                            GeoItemSelectorUtils.getOrCreateView(context, convertView, parent));
                }
            };

            Dialogs.newBuilder(context)
                    .setTitle(R.string.cache_menu_navigation_drive_select_target)
                    .setAdapter(adapter, (dialog, which) -> {
                        final IWaypoint target = targets.get(which);
                        if (target instanceof Geocache) {
                            GoogleNavigationDrivingApp.super.navigate(context, (Geocache) target);
                        }
                        if (target instanceof Waypoint) {
                            navigate(context, (Waypoint) target);
                        }
                    }).show();
        }
    }

    static class GoogleNavigationBikeApp extends GoogleNavigationApp {
        GoogleNavigationBikeApp() {
            super(R.string.cache_menu_navigation_bike, "b");
        }
    }
}
