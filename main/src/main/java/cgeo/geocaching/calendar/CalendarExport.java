package cgeo.geocaching.calendar;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.Log;

public class CalendarExport {

    private static final class CalendarInfo {
        final long id;
        final String displayName;
        final String accountName;

        CalendarInfo(final long id, final String displayName, final String accountName) {
            this.id = id;
            this.displayName = displayName;
            this.accountName = accountName;
        }

        @NonNull
        @Override
        public String toString() {
            return displayName + " (" + accountName + ")";
        }
    }

    private CalendarExport() {
        // utility class
    }

    private static List<CalendarInfo> getWritableCalendars(final ContentResolver cr) {
        final List<CalendarInfo> calendars = new ArrayList<>();
        final String[] projection = new String[]{
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME
        };
        final String selection = CalendarContract.Calendars.VISIBLE + " = 1 AND " + CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= " + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
        try (Cursor cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null)) {
            if (cursor != null) {
                final int idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID);
                final int nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME);
                final int accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME);
                while (cursor.moveToNext()) {
                    calendars.add(new CalendarInfo(
                            cursor.getLong(idIdx),
                            cursor.getString(nameIdx),
                            cursor.getString(accountIdx)
                    ));
                }
            }
        } catch (final Exception e) {
            Log.e("Error getting calendars", e);
        }
        return calendars;
    }

    public static boolean eventExists(final ContentResolver cr, final long calendarId, final String geocode) {
        final String[] projection = new String[]{CalendarContract.Events._ID};
        final String selection = CalendarContract.Events.CALENDAR_ID + " = ? AND " + CalendarContract.Events.TITLE + " LIKE ?";
        final String[] selectionArgs = new String[]{String.valueOf(calendarId), "%" + geocode + "%"};
        try (Cursor cursor = cr.query(CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, null)) {
            return cursor != null && cursor.getCount() > 0;
        } catch (final Exception e) {
            Log.e("Error checking event existence", e);
        }
        return false;
    }

    public static void exportToCalendar(final Context context, final Collection<Geocache> caches) {
        final ContentResolver cr = context.getContentResolver();
        final List<CalendarInfo> calendars = getWritableCalendars(cr);

        if (calendars.isEmpty()) {
            ViewUtils.showToast(context, R.string.export_events_fail);
            return;
        }

        if (calendars.size() == 1 || !(context instanceof Activity)) {
            exportToCalendarInternal(context, caches, calendars.get(0).id);
        } else {
            final SimpleDialog.ItemSelectModel<CalendarInfo> model = new SimpleDialog.ItemSelectModel<>();
            model.setItems(calendars)
                    .setDisplayMapper(cal -> TextParam.text(cal.toString()))
                    .setSelectedItems(calendars.subList(0, 1));

            SimpleDialog.of((Activity) context).setTitle(R.string.menu_export_events_to_calendar)
                    .selectSingle(model, cal -> exportToCalendarInternal(context, caches, cal.id));
        }
    }

    private static void exportToCalendarInternal(final Context context, final Collection<Geocache> caches, final long calendarId) {
        final ContentResolver cr = context.getContentResolver();
        int successCount = 0;
        for (Geocache cache : caches) {
            if (eventExists(cr, calendarId, cache.getGeocode())) {
                continue;
            }

            final Date hiddenDate = cache.getHiddenDate();
            if (hiddenDate == null) {
                continue;
            }

            final CalendarEntry entry = new CalendarEntry(cache, hiddenDate);
            final ContentValues values = entry.getContentValues(calendarId);
            try {
                if (cr.insert(CalendarContract.Events.CONTENT_URI, values) != null) {
                    successCount++;
                } else {
                    Log.w("Failed to insert event (insert returned null): " + cache.getGeocode());
                }
            } catch (final Exception e) {
                Log.e("Error inserting event: " + cache.getGeocode(), e);
            }
        }

        if (successCount > 0) {
            ViewUtils.showToast(context, context.getString(R.string.export_events_success) + " (" + successCount + ")");
        } else {
            ViewUtils.showToast(context, R.string.export_events_success);
        }
    }
}
