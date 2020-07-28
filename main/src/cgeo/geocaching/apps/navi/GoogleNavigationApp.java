package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.Log;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

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
                if (waypoint.getWaypointType() == WaypointType.PARKING && !cache.getCoords().equals(waypoint.getCoords())) {
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
            final LayoutInflater inflater = LayoutInflater.from(context);
            final ListAdapter adapter = new ArrayAdapter<IWaypoint>(context, R.layout.cacheslist_item_select, targets) {
                @Override
                public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {

                    final View view = convertView == null ? inflater.inflate(R.layout.cacheslist_item_select, parent, false) : convertView;
                    final TextView tv = (TextView) view.findViewById(R.id.text);

                    final IWaypoint item = getItem(position);
                    tv.setText(item.getName());

                    final int icon = item instanceof Waypoint ? item.getWaypointType().markerId : ((Geocache) item).getType().markerId;
                    tv.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);

                    final TextView infoView = (TextView) view.findViewById(R.id.info);
                    if (item instanceof Waypoint) {
                        infoView.setText(((Waypoint) item).getNote());
                    } else {
                        infoView.setText(item.getGeocode());
                    }

                    return view;
                }
            };

            new AlertDialog.Builder(context).setTitle(R.string.cache_menu_navigation_drive_select_target).setAdapter(adapter, (dialog, which) -> {
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
