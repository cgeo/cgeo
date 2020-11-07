package cgeo.geocaching;

import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.apps.navi.NavigationSelectionActionProvider;
import cgeo.geocaching.calendar.CalendarAdder;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.AbstractUIFactory;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;

/**
 * Shared menu handling for all activities having menu items related to a cache. <br>
 * TODO: replace by a fragment
 *
 */
public final class CacheMenuHandler extends AbstractUIFactory {

    private CacheMenuHandler() {
        // utility class
    }

    /**
     * Methods to be implemented by the activity to react to the cache menu selections.
     *
     */
    interface ActivityInterface {
        void navigateTo();

        void showNavigationMenu();

        void cachesAround();

    }

    public static boolean onMenuItemSelected(final MenuItem item, @NonNull final CacheMenuHandler.ActivityInterface activityInterface, final Geocache cache) {
        final Activity activity;
        if (activityInterface instanceof Activity) {
            activity = (Activity) activityInterface;
        } else {
            activity = ((Fragment) activityInterface).getActivity();
        }

        final int menuItem = item.getItemId();
        if (menuItem == R.id.menu_default_navigation) {
            activityInterface.navigateTo();
            return true;
        } else if (menuItem == R.id.menu_navigate) {
            final NavigationSelectionActionProvider navigationProvider = (NavigationSelectionActionProvider) MenuItemCompat.getActionProvider(item);
            if (navigationProvider == null) {
                activityInterface.showNavigationMenu();
                return true;
            }
            return false;
        } else if (menuItem == R.id.menu_caches_around) {
            activityInterface.cachesAround();
            return true;
        } else if (menuItem == R.id.menu_show_in_browser) {
            cache.openInBrowser(activity);
            return true;
        } else if (menuItem == R.id.menu_share) {
            /* If the share menu is a shareActionProvider do nothing and let the share ActionProvider do the work */
            final ShareActionProvider shareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(item);
            if (shareActionProvider == null) {
                cache.shareCache(activity, res);
                return true;
            }
            return false;
        } else if (menuItem == R.id.menu_calendar) {
            CalendarAdder.addToCalendar(activity, cache);
            return true;
        }
        return false;
    }

    public static void onPrepareOptionsMenu(final Menu menu, final Geocache cache) {
        if (cache == null) {
            return;
        }
        final boolean hasCoords = cache.getCoords() != null;
        menu.findItem(R.id.menu_default_navigation).setVisible(hasCoords);
        menu.findItem(R.id.menu_navigate).setVisible(hasCoords);
        menu.findItem(R.id.menu_delete).setVisible(cache.isOffline());
        menu.findItem(R.id.menu_share).setVisible(!InternalConnector.getInstance().canHandle(cache.getGeocode()));
        menu.findItem(R.id.menu_caches_around).setVisible(hasCoords && cache.supportsCachesAround());
        menu.findItem(R.id.menu_calendar).setVisible(cache.canBeAddedToCalendar());
        menu.findItem(R.id.menu_log_visit).setVisible(cache.supportsLogging() && !Settings.getLogOffline());
        menu.findItem(R.id.menu_log_visit_offline).setVisible(cache.supportsLogging() && Settings.getLogOffline());

        menu.findItem(R.id.menu_default_navigation).setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());

        final MenuItem shareItem = menu.findItem(R.id.menu_share);
        final ShareActionProvider shareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);
        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(cache.getShareIntent());
        }

    }

    public static void addMenuItems(final MenuInflater inflater, final Menu menu, final Geocache cache) {
        inflater.inflate(R.menu.cache_options, menu);
        onPrepareOptionsMenu(menu, cache);
    }

    public static void addMenuItems(final Activity activity, final Menu menu, final Geocache cache) {
        addMenuItems(activity.getMenuInflater(), menu, cache);
        // some connectors don't support URL - we don't need "open in browser" for those caches
        menu.findItem(R.id.menu_show_in_browser).setVisible(cache != null && cache.getCgeoUrl() != null);
    }
}
