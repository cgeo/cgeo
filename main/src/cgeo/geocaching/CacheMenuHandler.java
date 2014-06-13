package cgeo.geocaching;

import cgeo.calendar.CalendarAddon;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.ui.AbstractUIFactory;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Shared menu handling for all activities having menu items related to a cache. <br>
 * TODO: replace by a fragment
 * 
 */
public class CacheMenuHandler extends AbstractUIFactory {

    /**
     * Methods to be implemented by the activity to react to the cache menu selections.
     *
     */
    interface ActivityInterface {
        public void navigateTo();

        public void showNavigationMenu();

        public void cachesAround();

    }

    public static boolean onMenuItemSelected(final MenuItem item, final CacheMenuHandler.ActivityInterface activityInterface, final Geocache cache) {
        assert activityInterface instanceof Activity || activityInterface instanceof Fragment;
        final Activity activity;
        if (activityInterface instanceof Activity) {
            activity = (Activity) activityInterface;
        } else {
            activity = ((Fragment)activityInterface).getActivity();
        }

        switch (item.getItemId()) {
            case R.id.menu_default_navigation:
                activityInterface.navigateTo();
                return true;
            case R.id.menu_navigate:
                activityInterface.showNavigationMenu();
                return true;
            case R.id.menu_caches_around:
                activityInterface.cachesAround();
                return true;
            case R.id.menu_show_in_browser:
                cache.openInBrowser(activity);
                return true;
            case R.id.menu_calendar:
                CalendarAddon.addToCalendarWithIntent(activity, cache);
                return true;
            default:
                return false;
        }
    }

    public static void onPrepareOptionsMenu(final Menu menu, final Geocache cache) {
        if (cache == null) {
            return;
        }
        final boolean hasCoords = cache.getCoords() != null;
        menu.findItem(R.id.menu_default_navigation).setVisible(hasCoords);
        menu.findItem(R.id.menu_navigate).setVisible(hasCoords);
        menu.findItem(R.id.menu_caches_around).setVisible(hasCoords && cache.supportsCachesAround());
        menu.findItem(R.id.menu_calendar).setVisible(cache.canBeAddedToCalendar());
        menu.findItem(R.id.menu_show_in_browser).setVisible(cache.canOpenInBrowser());

        menu.findItem(R.id.menu_default_navigation).setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());

        final MenuItem shareItem = menu.findItem(R.id.menu_share);
        final ShareActionProvider shareActionProvider = (ShareActionProvider)
                MenuItemCompat.getActionProvider(shareItem);
        if(shareActionProvider != null) {
            shareActionProvider.setShareIntent(cache.getShareIntent());
        }

    }

    public static void addMenuItems(final MenuInflater inflater, final Menu menu, final Geocache cache) {
        inflater.inflate(R.menu.cache_options, menu);
        onPrepareOptionsMenu(menu, cache);
    }

    public static void addMenuItems(final Activity activity, final Menu menu, final Geocache cache) {
        addMenuItems(activity.getMenuInflater(), menu, cache);
    }
}
