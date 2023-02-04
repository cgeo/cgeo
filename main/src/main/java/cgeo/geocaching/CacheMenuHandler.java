package cgeo.geocaching;

import cgeo.geocaching.activity.INavigationSource;
import cgeo.geocaching.apps.navi.NavigationAppFactory;
import cgeo.geocaching.apps.navi.NavigationSelectionActionProvider;
import cgeo.geocaching.calendar.CalendarAdder;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.BookmarkUtils;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.AbstractUIFactory;
import cgeo.geocaching.ui.NavigationActionProvider;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;

import java.util.Collections;

/**
 * Shared menu handling for all activities having menu items related to a cache. <br>
 * TODO: replace by a fragment
 */
public final class CacheMenuHandler extends AbstractUIFactory {

    private CacheMenuHandler() {
        // utility class
    }

    /**
     * Methods to be implemented by the activity to react to the cache menu selections.
     */
    interface ActivityInterface {
        void navigateTo();

        void showNavigationMenu();

        void cachesAround();

    }

    // Note for parameter "cache": this can be null if menu is clicked before cache was loaded in CacheDetailsActivity
    public static boolean onMenuItemSelected(final MenuItem item, @NonNull final CacheMenuHandler.ActivityInterface activityInterface, @Nullable final Geocache cache, @Nullable final Runnable notifyDataSetChanged, final boolean fromPopup) {
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
            if (navigationProvider == null || fromPopup) {
                activityInterface.showNavigationMenu();
                return true;
            }
            return false;
        } else if (menuItem == R.id.menu_caches_around || menuItem == R.id.menu_caches_around_from_popup) {
            activityInterface.cachesAround();
            return true;
        } else if (menuItem == R.id.menu_upload_bookmarklist) {
            BookmarkUtils.askAndUploadCachesToBookmarkList(activity, Collections.singletonList(cache));
            return true;
        } else if (menuItem == R.id.menu_show_in_browser) {
            if (cache != null) {
                cache.openInBrowser(activity);
            }
            return true;
        } else if (menuItem == R.id.menu_log_in_browser) {
            if (cache != null) {
                cache.openCreateNewLogInBrowser(activity);
            }
            return true;
        } else if (menuItem == R.id.menu_share || menuItem == R.id.menu_share_from_popup) {
            if (cache != null && activity != null) {
                cache.shareCache(activity, res);
            }
            return true;
        } else if (menuItem == R.id.menu_calendar) {
            if (cache != null && activity != null) {
                CalendarAdder.addToCalendar(activity, cache);
            }
            return true;
        } else if (menuItem == R.id.menu_set_found) {
            if (cache != null) {
                setFoundState(activity, cache, true, false, notifyDataSetChanged);
            }
        } else if (menuItem == R.id.menu_set_DNF) {
            if (cache != null) {
                setFoundState(activity, cache, false, true, notifyDataSetChanged);
            }
        } else if (menuItem == R.id.menu_reset_foundstate && cache != null) {
            setFoundState(activity, cache, false, false, notifyDataSetChanged);
        }
        return false;
    }

    private static void setFoundState(final Activity activity, final Geocache cache, final boolean foundState, final boolean dnfState, @Nullable final Runnable notifyDataSetChanged) {
        cache.setFound(foundState);
        cache.setDNF(dnfState);
        if (!cache.isOffline()) {
            // store to default list if not yet stored
            cache.setLists(Collections.singleton(StoredList.STANDARD_LIST_ID));
        }
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
        Toast.makeText(activity, R.string.cache_foundstate_updated, Toast.LENGTH_SHORT).show();
        if (notifyDataSetChanged != null) {
            notifyDataSetChanged.run();
        }
    }

    public static void onPrepareOptionsMenu(final Menu menu, final Geocache cache, final boolean fromPopup) {
        if (cache == null) {
            return;
        }
        final boolean hasCoords = cache.getCoords() != null;
        // top level menu items
        menu.findItem(R.id.menu_default_navigation).setVisible(hasCoords);
        menu.findItem(R.id.menu_default_navigation).setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());
        menu.findItem(R.id.menu_navigate).setVisible(hasCoords);
        if (!fromPopup) {
            NavigationSelectionActionProvider.initialize(menu.findItem(R.id.menu_navigate), cache);
        }
        menu.findItem(R.id.menu_log_visit).setVisible(cache.supportsLogging() && !Settings.getLogOffline());
        menu.findItem(R.id.menu_log_in_browser).setVisible(cache.supportsLoggingOnline());
        menu.findItem(R.id.menu_log_visit_offline).setVisible(cache.supportsLogging() && Settings.getLogOffline());
        menu.findItem(R.id.menu_set_found).setVisible(cache.supportsSettingFoundState() && !cache.isFound());
        menu.findItem(R.id.menu_set_DNF).setVisible(cache.supportsSettingFoundState() && !cache.isDNF());
        menu.findItem(R.id.menu_reset_foundstate).setVisible(cache.supportsSettingFoundState() && (cache.isFound() || cache.isDNF()));
        // some connectors don't support URL - we don't need "open in browser" for those caches
        menu.findItem(R.id.menu_show_in_browser).setVisible(cache.getUrl() != null);
        // submenu share / export
        menu.findItem(fromPopup ? R.id.menu_share_from_popup : R.id.menu_share).setVisible(!InternalConnector.getInstance().canHandle(cache.getGeocode()));
        // submenu advanced
        menu.findItem(R.id.menu_calendar).setVisible(cache.canBeAddedToCalendar());
        menu.findItem(fromPopup ? R.id.menu_caches_around_from_popup : R.id.menu_caches_around).setVisible(hasCoords);
        menu.findItem(R.id.menu_upload_bookmarklist).setVisible(Settings.isGCConnectorActive() && Settings.isGCPremiumMember() && ConnectorFactory.getConnector(cache) instanceof GCConnector);
    }

    public static void addMenuItems(final MenuInflater inflater, final Menu menu, final Geocache cache, final boolean fromPopup) {
        inflater.inflate(R.menu.cache_options, menu);
        onPrepareOptionsMenu(menu, cache, fromPopup);
    }

    public static void addMenuItems(final Activity activity, final Menu menu, final Geocache cache) {
        addMenuItems(activity.getMenuInflater(), menu, cache, false);
    }

    public static void initDefaultNavigationMenuItem(final Menu menu, final INavigationSource navigationSource) {
        final MenuItem defaultNavigationMenuItem = menu.findItem(R.id.menu_default_navigation);
        final NavigationActionProvider defaultNavAction = (NavigationActionProvider) MenuItemCompat.getActionProvider(defaultNavigationMenuItem);
        if (defaultNavAction != null) {
            defaultNavAction.setNavigationSource(navigationSource);
        }
    }
}
