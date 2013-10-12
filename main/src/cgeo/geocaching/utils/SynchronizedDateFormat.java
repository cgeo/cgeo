package cgeo.geocaching.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SynchronizedDateFormat {
    private final SimpleDateFormat format;

    public SynchronizedDateFormat(final String pattern, final Locale locale) {
        format = new SimpleDateFormat(pattern, locale);
    }

    public synchronized Date parse(final String input) throws ParseException {
        return format.parse(input);
    }
}
