// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SynchronizedDateFormat {
    private final SimpleDateFormat format

    public SynchronizedDateFormat(final String pattern, final Locale locale) {
        format = SimpleDateFormat(pattern, locale)
    }

    public SynchronizedDateFormat(final String pattern, final TimeZone timeZone, final Locale locale) {
        format = SimpleDateFormat(pattern, locale)
        format.setTimeZone(timeZone)
    }

    public synchronized Date parse(final String input) throws ParseException {
        return format.parse(input)
    }

    public synchronized String format(final Date date) {
        return format.format(date)
    }

}
