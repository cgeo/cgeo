package cgeo.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.widget.Toast;

public final class CalendarActivity extends Activity {
    static final String LOG_TAG = "cgeo.calendar";
    private Uri uri;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            uri = getIntent().getData();
            if (uri == null) {
                finish();
                return;
            }
            final CalendarEntry entry = new CalendarEntry(uri);
            if (!entry.isValid()) {
                return;
            }
            if (Compatibility.isLevel14()) {
                new AddEntryLevel14(entry, this).addEntryToCalendar();
                finish();
            } else {
                selectCalendarForAdding(entry);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            finish();
        }
    }

    /**
     * Adds the cache to the Android-calendar if it is an event.
     *
     * @param entry
     */
    private void selectCalendarForAdding(final CalendarEntry entry) {
        final String[] projection = new String[] { "_id", "displayName" };
        final Uri calendarProvider = Compatibility.getCalendarProviderURI();

        final Cursor cursor = managedQuery(calendarProvider, projection, "selected=1", null, null);

        if (cursor == null || cursor.getCount() <= 0) {
            showToast(R.string.event_fail);
            finish();
            return;
        }

        final SparseArray<String> calendars = new SparseArray<String>();
        cursor.moveToFirst();

        final int indexId = cursor.getColumnIndex("_id");
        final int indexName = cursor.getColumnIndex("displayName");

        do {
            final String idString = cursor.getString(indexId);
            if (idString != null) {
                try {
                    int id = Integer.parseInt(idString);
                    final String calName = cursor.getString(indexName);

                    if (id > 0 && calName != null) {
                        calendars.put(id, calName);
                    }
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "CalendarActivity.selectCalendarForAdding", e);
                }
            }
        } while (cursor.moveToNext());

        if (calendars.size() == 0) {
            showToast(R.string.event_fail);
            finish();
            return;
        }

        final String[] items = new String[calendars.size()];
        for (int i = 0; i < calendars.size(); i++) {
            items[i] = calendars.valueAt(i);
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.calendars);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                final int calendarId = calendars.keyAt(item);
                new AddEntry(entry, CalendarActivity.this, calendarId).addEntryToCalendar();
                finish();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.create().show();
    }

    public final void showToast(final int res) {
        final String text = getResources().getString(res);
        final Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);

        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
        toast.show();
    }
}