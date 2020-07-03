package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.routing.Routing;
import cgeo.geocaching.maps.routing.RoutingMode;
import cgeo.geocaching.settings.Settings;

import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

public class BRouterUtils {

    private BRouterUtils() {
        // utility class
    }

    public static void onPrepareOptionsMenu(@NonNull final Menu menu) {
        menu.findItem(R.id.submenu_routing).setVisible(Routing.isAvailable());
        switch (Settings.getRoutingMode()) {
            case STRAIGHT:
                menu.findItem(R.id.menu_routing_straight).setChecked(true);
                break;
            case WALK:
                menu.findItem(R.id.menu_routing_walk).setChecked(true);
                break;
            case BIKE:
                menu.findItem(R.id.menu_routing_bike).setChecked(true);
                break;
            case CAR:
                menu.findItem(R.id.menu_routing_car).setChecked(true);
                break;
            default:
                break;
        }
    }


    public static boolean onOptionsItemSelected(@NonNull final MenuItem item, final Runnable routingModeChanged) {
        switch (item.getItemId()) {
            case R.id.menu_routing_straight: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.STRAIGHT);
                routingModeChanged.run();
                return true;
            }
            case R.id.menu_routing_walk: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.WALK);
                routingModeChanged.run();
                return true;
            }
            case R.id.menu_routing_bike: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.BIKE);
                routingModeChanged.run();
                return true;
            }
            case R.id.menu_routing_car: {
                item.setChecked(true);
                Settings.setRoutingMode(RoutingMode.CAR);
                routingModeChanged.run();
                return true;
            }
            default:
                return false;
        }
    }
}
