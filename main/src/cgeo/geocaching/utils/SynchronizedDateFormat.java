package cgeo.geocaching.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SynchronizedDateFormat {
    private final SimpleDateFormat format;

    public SynchronizedDateFormat(final String pattern, final Locale locale) {
        format = new SimpleDateFormat(pattern, locale);
    }

    public SynchronizedDateFormat(final String pattern, final TimeZone timeZone, final Locale locale) {
        format = new SimpleDateFormat(pattern, locale);
        format.setTimeZone(timeZone);
    }

    public synchronized Date parse(final String input) throws ParseException {
        return format.parse(input);
    }

    public synchronized String format(final Date date) {
        return format.format(date);
    }

}
