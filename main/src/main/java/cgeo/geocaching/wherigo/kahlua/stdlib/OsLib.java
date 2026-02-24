/*
Copyright(c) 2008 Kevin Lundberg <kevinrlundberg@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files(the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
--
--
File initially copied to c:geo from https://github.com/cgeo/openWIG in April 2025.
Release 1.1.0 / 4386a025b88aac759e1e67cb27bcc50692d61d9a, Base Package se.krka.kahlua.stdlib
*/

package cgeo.geocaching.wherigo.kahlua.stdlib;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;

public enum OsLib implements JavaFunction {

    DATE,
    DIFFTIME,
    TIME;

    private static final String TABLE_FORMAT = "*t";
    private static final String DEFAULT_FORMAT = "%c";

    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String HOUR = "hour";
    private static final String MIN = "min";
    private static final String SEC = "sec";
    private static final String WDAY = "wday";
    private static final String YDAY = "yday";
    private static final Object MILLISECOND = "milli";
    //private static final String ISDST = "isdst";

    private static ZoneId tzone = ZoneId.systemDefault();

    public static final int TIME_DIVIDEND = 1000; // number to divide by for converting from milliseconds.
    public static final double TIME_DIVIDEND_INVERTED = 1.0 / TIME_DIVIDEND; // number to divide by for converting from milliseconds.

    public static void register(final LuaState state) {
        final LuaTable os = new LuaTableImpl();
        state.getEnvironment().rawset("os", os);
        for (final OsLib f : values()) {
            os.rawset(f.name().toLowerCase(Locale.ROOT), f);
        }
    }

    public static void setTimeZone(final ZoneId tz) {
        tzone = tz;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public int call(final LuaCallFrame cf, final int nargs) {
        switch (this) {
            case DATE: return date(cf, nargs);
            case DIFFTIME: return difftime(cf);
            case TIME: return time(cf, nargs);
            default: throw new IllegalStateException("Undefined method called on os.");
        }
    }

    private int time(final LuaCallFrame cf, final int nargs) {
        if (nargs == 0) {
            cf.push(LuaState.toDouble((double) System.currentTimeMillis() * TIME_DIVIDEND_INVERTED));
        } else {
            final LuaTable table = BaseLib.getArg(cf, 1, LuaType.TABLE, "time");
            cf.push(LuaState.toDouble((double) getDateFromTable(table) * TIME_DIVIDEND_INVERTED));
        }
        return 1;
    }

    private int difftime(final LuaCallFrame cf) {
        double t2 = BaseLib.rawTonumber(cf.get(0)).doubleValue();
        double t1 = BaseLib.rawTonumber(cf.get(1)).doubleValue();
        cf.push(LuaState.toDouble(t2 - t1));
        return 1;
    }

    private int date(final LuaCallFrame cf, final int nargs) {
        if (nargs == 0) {
            return cf.push(getdate(DEFAULT_FORMAT));
        } else {
            String format = BaseLib.rawTostring(cf.get(0));
            if (nargs == 1) {
                return cf.push(getdate(format));
            } else {
                Double rawTonumber = BaseLib.rawTonumber(cf.get(1));
                long time = (long) (rawTonumber.doubleValue() * TIME_DIVIDEND);
                return cf.push(getdate(format, time));
            }
        }
    }

    public static Object getdate(final String format) {
        return getdate(format, System.currentTimeMillis());
    }

    public static Object getdate(final String format, final long time) {
        int si = 0;
        final ZoneId zone;
        if (format.charAt(si) == '!') { // UTC?
            zone = ZoneId.of("UTC");
            si++;  // skip '!'
        } else {
            zone = tzone;
        }
        final ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), zone);

        if (format.substring(si, 2 + si).equals(TABLE_FORMAT)) {
            return getTableFromDate(zdt);
        } else {
            return formatTime(format.substring(si), zdt);
        }
    }

    public static String formatTime(final String format, final ZonedDateTime zdt) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            if (format.charAt(i) != '%' || i + 1 == format.length()) { // no conversion specifier?
                buffer.append(format.charAt(i));
            } else {
                ++i;
                buffer.append(strftime(format.charAt(i), zdt));
            }
        }
        return buffer.toString();
    }

    private static String strftime(final char format, final ZonedDateTime zdt) {
        // day-of-week array index: ISO Mon=1..Sun=7 → Sun=0..Sat=6
        final int dowIndex = zdt.getDayOfWeek().getValue() % 7;
        switch (format) {
            case 'a': return zdt.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ROOT);
            case 'A': return zdt.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ROOT);
            case 'b': return zdt.getMonth().getDisplayName(TextStyle.SHORT, Locale.ROOT);
            case 'B': return zdt.getMonth().getDisplayName(TextStyle.FULL, Locale.ROOT);
            case 'c': return zdt.toString();
            case 'C': return Integer.toString(zdt.getYear() / 100);
            case 'd': return Integer.toString(zdt.getDayOfMonth());
            case 'D': return formatTime("%m/%d/%y", zdt);
            case 'e': return zdt.getDayOfMonth() < 10 ? " " + zdt.getDayOfMonth() : Integer.toString(zdt.getDayOfMonth());
            case 'h': return strftime('b', zdt);
            case 'H': return Integer.toString(zdt.getHour());
            case 'I': return Integer.toString(zdt.getHour() % 12);
            case 'j': return Integer.toString(zdt.getDayOfYear());
            case 'm': return Integer.toString(zdt.getMonthValue());
            case 'M': return Integer.toString(zdt.getMinute());
            case 'n': return "\n";
            case 'p': return zdt.getHour() < 12 ? "AM" : "PM";
            case 'r': return formatTime("%I:%M:%S %p", zdt);
            case 'R': return formatTime("%H:%M", zdt);
            case 'S': return Integer.toString(zdt.getSecond());
            case 'U': return Integer.toString(zdt.get(WeekFields.SUNDAY_START.weekOfYear()));
            case 'V': return Integer.toString(zdt.get(WeekFields.ISO.weekOfWeekBasedYear()));
            case 'w': return Integer.toString(dowIndex);
            case 'W': return Integer.toString(zdt.get(WeekFields.of(DayOfWeek.MONDAY, 1).weekOfYear()));
            case 'y': return Integer.toString(zdt.getYear() % 100);
            case 'Y': return Integer.toString(zdt.getYear());
            case 'Z': return zdt.getZone().getId();
            default: return null; // bad input format.
        }
    }

    public static LuaTable getTableFromDate(final ZonedDateTime zdt) {
        final LuaTable time = new LuaTableImpl();
        time.rawset(YEAR, LuaState.toDouble(zdt.getYear()));
        time.rawset(MONTH, LuaState.toDouble(zdt.getMonthValue()));
        time.rawset(DAY, LuaState.toDouble(zdt.getDayOfMonth()));
        time.rawset(HOUR, LuaState.toDouble(zdt.getHour()));
        time.rawset(MIN, LuaState.toDouble(zdt.getMinute()));
        time.rawset(SEC, LuaState.toDouble(zdt.getSecond()));
        time.rawset(WDAY, LuaState.toDouble(zdt.getDayOfWeek().getValue() % 7 + 1));
        time.rawset(YDAY, LuaState.toDouble(zdt.getDayOfYear()));
        time.rawset(MILLISECOND, LuaState.toDouble(zdt.getNano() / 1_000_000));
        return time;
    }

    /**
     * Converts the relevant fields in the given LuaTable to epoch milliseconds.
     * @param time LuaTable with entries for year month and day, and optionally hour/min/sec
     * @return epoch milliseconds representing the date from the LuaTable.
     */
    public static long getDateFromTable(final LuaTable time) {
        final int year = (int) LuaState.fromDouble(time.rawget(YEAR));
        final int month = (int) LuaState.fromDouble(time.rawget(MONTH));
        final int day = (int) LuaState.fromDouble(time.rawget(DAY));
        final Object hourObj = time.rawget(HOUR);
        final Object minObj = time.rawget(MIN);
        final Object secObj = time.rawget(SEC);
        final Object milliObj = time.rawget(MILLISECOND);
        final int hour = hourObj != null ? (int) LuaState.fromDouble(hourObj) : 0;
        final int min = minObj != null ? (int) LuaState.fromDouble(minObj) : 0;
        final int sec = secObj != null ? (int) LuaState.fromDouble(secObj) : 0;
        final int milli = milliObj != null ? (int) LuaState.fromDouble(milliObj) : 0;
        return ZonedDateTime.of(year, month, day, hour, min, sec, milli * 1_000_000, tzone)
                .toInstant().toEpochMilli();
    }
}

