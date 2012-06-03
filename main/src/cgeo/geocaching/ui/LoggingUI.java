package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.activity.IAbstractActivity;
import cgeo.geocaching.enumerations.LogType;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class LoggingUI extends AbstractUIFactory {
    public static class LogTypeEntry {
        private final LogType logType;

        public LogTypeEntry(final LogType logType) {
            this.logType = logType;
        }

        @Override
        public String toString() {
            if (logType == null) {
                return res.getString(R.string.cache_menu_visit);
            }
            return logType.getL10n();
        }
    }

    private static final int MENU_ICON_LOG_VISIT = R.drawable.ic_menu_edit;
    private static final int MENU_LOG_VISIT = 100;
    private static final int MENU_LOG_VISIT_OFFLINE = 101;

    public static void addMenuItems(final Menu menu, final cgCache cache) {
        if (cache == null) {
            return;
        }
        if (!cache.supportsLogging()) {
            return;
        }
        if (Settings.getLogOffline()) {
            menu.add(0, MENU_LOG_VISIT_OFFLINE, 0, res.getString(R.string.cache_menu_visit_offline)).setIcon(MENU_ICON_LOG_VISIT);
        }
        else {
            menu.add(0, MENU_LOG_VISIT, 0, res.getString(R.string.cache_menu_visit)).setIcon(MENU_ICON_LOG_VISIT);
        }
    }

    public static boolean onMenuItemSelected(final MenuItem item, IAbstractActivity activity, cgCache cache) {
        switch (item.getItemId()) {
            case MENU_LOG_VISIT:
                cache.logVisit(activity);
                return true;
            case MENU_LOG_VISIT_OFFLINE:
                showOfflineMenu(cache, (Activity) activity);
                return true;
            default:
                return false;
        }
    }

    private static void showOfflineMenu(final cgCache cache, final Activity activity) {
        final List<LogType> logTypes = cache.getPossibleLogTypes();
        final ArrayList<LogTypeEntry> list = new ArrayList<LogTypeEntry>();
        for (LogType logType : logTypes) {
            list.add(new LogTypeEntry(logType));
        }
        // online logging
        list.add(new LogTypeEntry(null));

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cache_menu_visit_offline);

        final ArrayAdapter<LogTypeEntry> adapter = new ArrayAdapter<LogTypeEntry>(activity, android.R.layout.select_dialog_item, list);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                final LogTypeEntry logTypeEntry = adapter.getItem(item);
                if (logTypeEntry.logType != null) {
                    cache.logOffline(activity, logTypeEntry.logType);
                } else {
                    cache.logVisit((IAbstractActivity) activity);
                }
            }
        });

        builder.create().show();

    }

    public static void onPrepareOptionsMenu(Menu menu) {
        final MenuItem item = menu.findItem(MENU_LOG_VISIT);
        if (item != null) {
            item.setEnabled(Settings.isLogin());
        }
    }
}
