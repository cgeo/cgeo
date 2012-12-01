package cgeo.geocaching.ui;

import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgData;
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
        private final SpecialLogType specialLogType;
        private final boolean isActive;

        public LogTypeEntry(final LogType logType, final SpecialLogType specialLogType, final boolean isActive) {
            this.logType = logType;
            this.specialLogType = specialLogType;
            this.isActive = isActive;
        }

        @Override
        public String toString() {
            if (logType == null) {
                return specialLogType.getL10n();
            }

            String text = logType.getL10n();

            if (isActive) {
                text += " ✔";
            }

            return text;
        }
    }

    private enum SpecialLogType {
        LOG_CACHE(R.string.cache_menu_visit),
        CLEAR_LOG(R.string.log_clear);

        private final int stringId;

        private SpecialLogType(final int stringId) {
            this.stringId = stringId;
        }

        public final String getL10n() {
            return res.getString(stringId);
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
        final LogEntry currentLog = cgData.loadLogOffline(cache.getGeocode());
        final LogType currentLogType = currentLog == null ? null : currentLog.type;

        final List<LogType> logTypes = cache.getPossibleLogTypes();
        final ArrayList<LogTypeEntry> list = new ArrayList<LogTypeEntry>();
        for (LogType logType : logTypes) {
            list.add(new LogTypeEntry(logType, null, logType == currentLogType));
        }
        if (cache.isLogOffline()) {
            list.add(new LogTypeEntry(null, SpecialLogType.CLEAR_LOG, false));
        }
        list.add(new LogTypeEntry(null, SpecialLogType.LOG_CACHE, false));

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cache_menu_visit_offline);

        final ArrayAdapter<LogTypeEntry> adapter = new ArrayAdapter<LogTypeEntry>(activity, android.R.layout.select_dialog_item, list);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                final LogTypeEntry logTypeEntry = adapter.getItem(item);
                if (logTypeEntry.logType == null) {
                    switch (logTypeEntry.specialLogType) {
                        case LOG_CACHE:
                            cache.logVisit((IAbstractActivity) activity);
                            break;

                        case CLEAR_LOG:
                            cgData.clearLogOffline(cache.getGeocode());
                    }
                } else {
                    cache.logOffline(activity, logTypeEntry.logType);
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
