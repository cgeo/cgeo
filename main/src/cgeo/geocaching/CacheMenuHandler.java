package cgeo.geocaching;

import cgeo.calendar.ICalendar;
import cgeo.geocaching.apps.cache.navi.NavigationAppFactory;
import cgeo.geocaching.geopoint.GeopointFormatter;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.AbstractUIFactory;
import cgeo.geocaching.utils.ProcessUtils;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Date;

/**
 * Shared menu handling for all activities having menu items related to a cache.
 *
 */
public class CacheMenuHandler extends AbstractUIFactory {

    /**
     * Methods to be implemented by the activity to react to the cache menu selections.
     *
     */
    protected interface ActivityInterface {
        public void navigateTo();

        public void showNavigationMenu();

        public void cachesAround();

    }

    public static boolean onMenuItemSelected(MenuItem item, CacheMenuHandler.ActivityInterface activityInterface, Geocache cache) {
        assert activityInterface instanceof Activity;
        final Activity activity = (Activity) activityInterface;
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
            case R.id.menu_share:
                cache.shareCache(activity, res);
                return true;
            case R.id.menu_calendar:
                addToCalendarWithIntent(activity, cache);
                return true;
            default:
                return false;
        }
    }

    public static void onPrepareOptionsMenu(final Menu menu, final Geocache cache) {
        final boolean hasCoords = cache.getCoords() != null;
        menu.findItem(R.id.menu_default_navigation).setVisible(hasCoords);
        menu.findItem(R.id.menu_navigate).setVisible(hasCoords);
        menu.findItem(R.id.menu_caches_around).setVisible(hasCoords && cache.supportsCachesAround());
        menu.findItem(R.id.menu_calendar).setVisible(cache.canBeAddedToCalendar());
        menu.findItem(R.id.menu_show_in_browser).setVisible(cache.canOpenInBrowser());

        menu.findItem(R.id.menu_default_navigation).setTitle(NavigationAppFactory.getDefaultNavigationApplication().getName());
    }

    public static void addMenuItems(Activity activity, Menu menu, Geocache cache) {
        activity.getMenuInflater().inflate(R.menu.cache_options, menu);
        onPrepareOptionsMenu(menu, cache);
    }

    private static void addToCalendarWithIntent(final Activity activity, final Geocache cache) {
        final boolean calendarAddOnAvailable = ProcessUtils.isIntentAvailable(ICalendar.INTENT, Uri.parse(ICalendar.URI_SCHEME + "://" + ICalendar.URI_HOST));

        if (calendarAddOnAvailable) {
            final Date hiddenDate = cache.getHiddenDate();
            final Parameters params = new Parameters(
                    ICalendar.PARAM_NAME, cache.getName(),
                    ICalendar.PARAM_NOTE, StringUtils.defaultString(cache.getPersonalNote()),
                    ICalendar.PARAM_HIDDEN_DATE, hiddenDate != null ? String.valueOf(hiddenDate.getTime()) : StringUtils.EMPTY,
                    ICalendar.PARAM_URL, StringUtils.defaultString(cache.getUrl()),
                    ICalendar.PARAM_COORDS, cache.getCoords() == null ? "" : cache.getCoords().format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW),
                    ICalendar.PARAM_LOCATION, StringUtils.defaultString(cache.getLocation()),
                    ICalendar.PARAM_SHORT_DESC, StringUtils.defaultString(cache.getShortDescription()),
                    ICalendar.PARAM_START_TIME_MINUTES, StringUtils.defaultString(cache.guessEventTimeMinutes())
                    );

            activity.startActivity(new Intent(ICalendar.INTENT,
                    Uri.parse(ICalendar.URI_SCHEME + "://" + ICalendar.URI_HOST + "?" + params.toString())));
        } else {
            // Inform user the calendar add-on is not installed and let them get it from Google Play
            new AlertDialog.Builder(activity)
                    .setTitle(res.getString(R.string.addon_missing_title))
                    .setMessage(new StringBuilder(res.getString(R.string.helper_calendar_missing))
                            .append(' ')
                            .append(res.getString(R.string.addon_download_prompt))
                            .toString())
                    .setPositiveButton(activity.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            final Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(ICalendar.CALENDAR_ADDON_URI));
                            activity.startActivity(intent);
                        }
                    })
                    .setNegativeButton(activity.getString(android.R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
        }
    }

}
