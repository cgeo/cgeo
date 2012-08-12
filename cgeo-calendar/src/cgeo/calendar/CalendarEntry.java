package cgeo.calendar;

import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.text.style.ImageSpan;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

class CalendarEntry {

    private String shortDesc;
    private String hiddenDate;
    private String url;
    private String personalNote;
    private String name;
    private String location;
    private String coords;
    private int startTimeMinutes = -1;
    private Uri uri;

    public CalendarEntry(final Uri uri) {
        this.uri = uri;
        this.shortDesc = getParameter(ICalendar.PARAM_SHORT_DESC);
        this.hiddenDate = getParameter(ICalendar.PARAM_HIDDEN_DATE);
        this.url = getParameter(ICalendar.PARAM_URL);
        this.personalNote = getParameter(ICalendar.PARAM_NOTE);
        this.name = getParameter(ICalendar.PARAM_NAME);
        location = getParameter(ICalendar.PARAM_LOCATION);
        coords = getParameter(ICalendar.PARAM_COORDS);
        final String startTime = getParameter(ICalendar.PARAM_START_TIME_MINUTES);
        if (startTime.length() > 0) {
            try {
                this.startTimeMinutes = Integer.valueOf(startTime);
            } catch (NumberFormatException e) {
            }
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

    public boolean isValid() {
        return (getName().length() > 0 && getHiddenDate().length() > 0);
    }

    public String getHiddenDate() {
        return hiddenDate;
    }

    public String getUrl() {
        return url;
    }

    public String getPersonalNote() {
        return personalNote;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    /**
     * @return location string with coordinates and location
     */
    protected String parseLocation() {
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

        return locBuffer.toString();
    }

    /**
     * @return <code>Date</code> based on hidden date. Time is set to 00:00:00.
     */
    protected Date parseDate() {
        try {
            final Date eventDate = new Date(Long.parseLong(getHiddenDate()));
            eventDate.setHours(0);
            eventDate.setMinutes(0);
            eventDate.setSeconds(0);

            return eventDate;
        } catch (NumberFormatException e) {
            // cannot happen normally, but static code analysis does not know
        }
        return null;
    }

    /**
     * @return description string with images removed and personal note included
     */
    protected String parseDescription() {
        final StringBuilder description = new StringBuilder();
        description.append(getUrl());
        if (getShortDesc().length() > 0) {
            // remove images in short description
            final Spanned spanned = Html.fromHtml(getShortDesc(), null, null);
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

        if (getPersonalNote().length() > 0) {
            description.append("\n\n").append(Html.fromHtml(getPersonalNote()).toString());
        }

        return description.toString();
    }

    public String getName() {
        return name;
    }

    public int getStartTimeMinutes() {
        return startTimeMinutes;
    }

}
