package cgeo.geocaching.log;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.AbstractUIFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class LoggingUI extends AbstractUIFactory {

    private LoggingUI() {
        // utility class
    }

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
        @NonNull
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

        SpecialLogType(final int stringId) {
            this.stringId = stringId;
        }

        public String getL10n() {
            return res.getString(stringId);
        }
    }

    public static boolean onMenuItemSelected(final MenuItem item, final Activity activity, final Geocache cache, final DialogInterface.OnDismissListener listener) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_log_visit) {
            cache.logVisit(activity);
        } else if (itemId == R.id.menu_log_visit_offline) {
            showOfflineMenu(cache, activity, listener);
        } else {
            return false;
        }
        return true;
    }

    private static void showOfflineMenu(final Geocache cache, final Activity activity, final DialogInterface.OnDismissListener listener) {
        final LogEntry currentLog = DataStore.loadLogOffline(cache.getGeocode());
        final LogType currentLogType = currentLog == null ? null : currentLog.getType();

        final List<LogType> logTypes = cache.getPossibleLogTypes();
        final ArrayList<LogTypeEntry> list = new ArrayList<>();
        for (final LogType logType : logTypes) {
            list.add(new LogTypeEntry(logType, null, logType == currentLogType));
        }
        if (cache.isLogOffline()) {
            list.add(new LogTypeEntry(null, SpecialLogType.CLEAR_LOG, false));
        }
        list.add(new LogTypeEntry(null, SpecialLogType.LOG_CACHE, false));

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.cache_menu_visit_offline);

        final ArrayAdapter<LogTypeEntry> adapter = new ArrayAdapter<>(activity, android.R.layout.select_dialog_item, list);

        builder.setAdapter(adapter, (dialog, item) -> {
            final LogTypeEntry logTypeEntry = adapter.getItem(item);
            if (logTypeEntry.logType == null) {
                switch (logTypeEntry.specialLogType) {
                    case LOG_CACHE:
                        cache.logVisit(activity);
                        break;

                    case CLEAR_LOG:
                        cache.clearOfflineLog();
                        break;
                }
            } else {
                final ReportProblemType reportProblem;
                final LogType logType;
                switch (logTypeEntry.logType) {
                    case NEEDS_MAINTENANCE:
                        logType = LogType.NOTE;
                        reportProblem = ReportProblemType.OTHER;
                        break;
                    case NEEDS_ARCHIVE:
                        logType = LogType.NOTE;
                        reportProblem = ReportProblemType.ARCHIVE;
                        break;
                    default:
                        logType = logTypeEntry.logType;
                        reportProblem = ReportProblemType.NO_PROBLEM;
                }
                cache.logOffline(activity, logType, reportProblem);
            }
            dialog.dismiss();
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnDismissListener(listener);
        alertDialog.show();
    }

    public static void onPrepareOptionsMenu(final Menu menu, final Geocache cache) {
        if (cache == null) {
            return;
        }
        final MenuItem itemLog = menu.findItem(R.id.menu_log_visit);
        itemLog.setVisible(cache.supportsLogging() && !Settings.getLogOffline());

        final MenuItem itemOffline = menu.findItem(R.id.menu_log_visit_offline);
        itemOffline.setVisible(cache.supportsLogging() && Settings.getLogOffline());
    }

    public static void addMenuItems(final Activity activity, final Menu menu, final Geocache cache) {
        activity.getMenuInflater().inflate(R.menu.logging_ui, menu);
        onPrepareOptionsMenu(menu, cache);
    }
}
