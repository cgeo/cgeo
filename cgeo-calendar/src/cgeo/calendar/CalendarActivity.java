package cgeo.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class CalendarActivity extends Activity {
    private static final String LOG_TAG = "cgeo.calendar";
    private String shortDesc;
    private String hiddenDate;
    private String url;
    private String personalNote;
    private String name;
    private String location;
    private String coords;
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
            shortDesc = getParameter(ICalendar.PARAM_SHORT_DESC);
            hiddenDate = getParameter(ICalendar.PARAM_HIDDEN_DATE);
            url = getParameter(ICalendar.PARAM_URL);
            personalNote = getParameter(ICalendar.PARAM_NOTE);
            name = getParameter(ICalendar.PARAM_NAME);
            location = getParameter(ICalendar.PARAM_LOCATION);
            coords = getParameter(ICalendar.PARAM_COORDS);
            if (name.length() > 0 && hiddenDate.length() > 0) {
                selectCalendarForAdding();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            finish();
            return;
        }
    }

    private String getParameter(final String paramKey) {
        try {
            final String param = uri.getQueryParameter(paramKey);
            if (param == null) {
                return "";
            }
            return URLDecoder.decode(param, "UTF-8").trim();
        } catch (UnsupportedEncodingException e) {
        }
        return "";
    }

    /**
     * Adds the cache to the Android-calendar if it is an event.
     */
    private void selectCalendarForAdding() {
        final String[] projection = new String[] { "_id", "displayName" };
        final Uri calendarProvider = Compatibility.getCalendarProviderURI();

        // TODO: Handle missing provider
        final Cursor cursor = managedQuery(calendarProvider, projection, "selected=1", null, null);

        final Map<Integer, String> calendars = new HashMap<Integer, String>();
        if (cursor != null) {
            if (cursor.getCount() > 0) {
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
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        if (calendars.isEmpty()) {
            return;
        }

        final CharSequence[] items = calendars.values().toArray(new CharSequence[calendars.size()]);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.calendars);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                final Integer[] keys = calendars.keySet().toArray(new Integer[calendars.size()]);
                final Integer calendarId = keys[item];
                addToCalendar(calendarId);
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

    /**
     * @param calendars
     *
     * @param index
     *            The selected calendar
     */
    private void addToCalendar(Integer calendarId) {
        try {
            final Uri calendarProvider = Compatibility.getCalenderEventsProviderURI();

            // date
            final Date eventDate = new Date(Long.parseLong(hiddenDate));
            eventDate.setHours(0);
            eventDate.setMinutes(0);
            eventDate.setSeconds(0);

            // description
            final StringBuilder description = new StringBuilder();
            description.append(url);
            if (shortDesc.length() > 0) {
                // remove images in short description
                final Spanned spanned = Html.fromHtml(shortDesc, null, null);
                String text = spanned.toString();
                final ImageSpan[] spans = spanned.getSpans(0, spanned.length(), ImageSpan.class);
                for (int i = spans.length - 1; i >= 0; i--) {
                    text = text.substring(0, spanned.getSpanStart(spans[i])) + text.substring(spanned.getSpanEnd(spans[i]));
                }
                if (text.length() > 0) {
                    description.append("\n\n");
                    description.append(text);
                }
            }

            if (personalNote.length() > 0) {
                description.append("\n\n").append(Html.fromHtml(personalNote).toString());
            }

            // location
            final StringBuilder locBuffer = new StringBuilder();
            if (coords.length() > 0) {
                locBuffer.append(coords);
            }
            if (location.length() > 0) {
                boolean addParentheses = false;
                if (locBuffer.length() > 0) {
                    addParentheses = true;
                    locBuffer.append(" (");
                }

                locBuffer.append(Html.fromHtml(location).toString());
                if (addParentheses) {
                    locBuffer.append(')');
                }
            }

            // values
            final ContentValues event = new ContentValues();
            event.put("calendar_id", calendarId);
            event.put("dtstart", eventDate.getTime() + 43200000); // noon
            event.put("dtend", eventDate.getTime() + 43200000 + 3600000); // + one hour
            event.put("eventTimezone", "UTC");
            event.put("title", Html.fromHtml(name).toString());
            event.put("description", description.toString());

            if (locBuffer.length() > 0) {
                event.put("eventLocation", locBuffer.toString());
            }
            event.put("allDay", 1);
            event.put("hasAlarm", 0);

            getContentResolver().insert(calendarProvider, event);

            showToast(getResources().getString(R.string.event_success));
        } catch (Exception e) {
            showToast(getResources().getString(R.string.event_fail));

            Log.e(LOG_TAG, "CalendarActivity.addToCalendarFn: " + e.toString());
        }
    }

    public final void showToast(final String text) {
        final Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);

        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 100);
        toast.show();
    }

}