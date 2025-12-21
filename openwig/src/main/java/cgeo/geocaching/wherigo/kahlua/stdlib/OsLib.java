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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction;
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame;
import cgeo.geocaching.wherigo.kahlua.vm.LuaState;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable;
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl;

public class OsLib implements JavaFunction {
    private static final int DATE = 0;
    private static final int DIFFTIME = 1;
    private static final int TIME = 2;

    private static final int NUM_FUNCS = 3;

    private static String[] funcnames;
    private static OsLib[] funcs;

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

    private static TimeZone tzone = TimeZone.getDefault();

    public static final int TIME_DIVIDEND = 1000; // number to divide by for converting from milliseconds.
    public static final double TIME_DIVIDEND_INVERTED = 1.0 / TIME_DIVIDEND; // number to divide by for converting from milliseconds.
    private static final int MILLIS_PER_DAY = TIME_DIVIDEND * 60 * 60 * 24;
    private static final int MILLIS_PER_WEEK = MILLIS_PER_DAY * 7;

    private int methodId;

    private static String[] shortDayNames = new String[] {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private static String[] longDayNames = new String[] {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private static String[] shortMonthNames = new String[] {
        "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
    };

    private static String[] longMonthNames = new String[] {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    static {
        funcnames = new String[NUM_FUNCS];
        funcnames[DATE] = "date";
        funcnames[DIFFTIME] = "difftime";
        funcnames[TIME] = "time";

        funcs = new OsLib[NUM_FUNCS];
        for (int i = 0; i < NUM_FUNCS; i++) {
            funcs[i] = new OsLib(i);
        }
    }

    public static void register(LuaState state) {
        LuaTable os = new LuaTableImpl();
        state.getEnvironment().rawset("os", os);

        for (int i = 0; i < NUM_FUNCS; i++) {
            os.rawset(funcnames[i], funcs[i]);
        }
    }

    public static void setTimeZone (TimeZone tz) {
        tzone = tz;
    }

    private OsLib(int methodId) {
        this.methodId = methodId;
    }

    public int call(LuaCallFrame cf, int nargs) {
        return switch (methodId) {
            case DATE -> date(cf, nargs);
            case DIFFTIME -> difftime(cf);
            case TIME -> time(cf, nargs);
            default -> throw new IllegalStateException("Undefined method called on os.");
        };
    }

    private int time(LuaCallFrame cf, int nargs) {
        if (nargs == 0) {
            double t = (double) System.currentTimeMillis() * TIME_DIVIDEND_INVERTED;
            cf.push(LuaState.toDouble(t));
        } else {
            LuaTable table = (LuaTable) BaseLib.getArg(cf, 1, BaseLib.TYPE_TABLE, "time");
            double t = (double) getDateFromTable(table).getTime() * TIME_DIVIDEND_INVERTED;
            cf.push(LuaState.toDouble(t));
        }
        return 1;
    }

    private int difftime(LuaCallFrame cf) {
        double t2 = BaseLib.rawTonumber(cf.get(0)).doubleValue();
        double t1 = BaseLib.rawTonumber(cf.get(1)).doubleValue();
        cf.push(LuaState.toDouble(t2-t1));
        return 1;
    }

    private int date(LuaCallFrame cf, int nargs) {
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

    public static Object getdate(String format) {
        return getdate(format, Calendar.getInstance().getTime().getTime());
    }

    public static Object getdate(String format, long time) {
        //boolean universalTime = format.startsWith("!");
        Calendar calendar = null;
        int si = 0;
        if (format.charAt(si) == '!') { // UTC?
            calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            si++;  // skip '!'
        } else {
            calendar = Calendar.getInstance(tzone); // TODO: user-defined timezone
        }
        calendar.setTime(new Date(time));

        if (calendar == null) { // invalid calendar?
            return null;
        } else if (format.substring(si, 2 + si).equals(TABLE_FORMAT)) {
            return getTableFromDate(calendar);
        } else {
            return formatTime(format.substring(si), calendar);
        }
    }

    public static String formatTime(String format, Calendar cal) {

        StringBuffer buffer = new StringBuffer();
        for (int stringIndex = 0; stringIndex < format.length(); stringIndex ++) {
            if (format.charAt(stringIndex) != '%' || stringIndex + 1 == format.length()) { // no conversion specifier?
                buffer.append(format.charAt(stringIndex));
            } else {
                ++stringIndex;
                buffer.append(strftime(format.charAt(stringIndex), cal));
            }
        }
        return buffer.toString();
    }

    private static String strftime(char format, Calendar cal) {
        return switch (format) {
            case 'a' -> shortDayNames[cal.get(Calendar.DAY_OF_WEEK) - 1];
            case 'A' -> longDayNames[cal.get(Calendar.DAY_OF_WEEK) - 1];
            case 'b' -> shortMonthNames[cal.get(Calendar.MONTH)];
            case 'B' -> longMonthNames[cal.get(Calendar.MONTH)];
            case 'c' -> cal.getTime().toString();
            case 'C' -> Integer.toString(cal.get(Calendar.YEAR) / 100);
            case 'd' -> Integer.toString(cal.get(Calendar.DAY_OF_MONTH));
            case 'D' -> formatTime("%m/%d/%y", cal);
            case 'e' -> cal.get(Calendar.DAY_OF_MONTH) < 10 ?
                    " " + strftime('d', cal) : strftime('d', cal);
            case 'h' -> strftime('b', cal);
            case 'H' -> Integer.toString(cal.get(Calendar.HOUR_OF_DAY));
            case 'I' -> Integer.toString(cal.get(Calendar.HOUR));
            case 'j' -> Integer.toString(getDayOfYear(cal));
            case 'm' -> Integer.toString(cal.get(Calendar.MONTH) + 1);
            case 'M' -> Integer.toString(cal.get(Calendar.MINUTE));
            case 'n' -> "\n";
            case 'p' -> cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            case 'r' -> formatTime("%I:%M:%S %p", cal);
            case 'R' -> formatTime("%H:%M", cal);
            case 'S' -> Integer.toString(cal.get(Calendar.SECOND));
            case 'U' -> Integer.toString(getWeekOfYear(cal, true, false));
            case 'V' -> Integer.toString(getWeekOfYear(cal, false, true));
            case 'w' -> Integer.toString(cal.get(Calendar.DAY_OF_WEEK) - 1);
            case 'W' -> Integer.toString(getWeekOfYear(cal, false, false));
            /* commented out until we have a way to define locale and get locale formats working
            case 'x':
                String str = Integer.toString(cal.get(Calendar.YEAR));
                return Integer.toString(cal.get(Calendar.MONTH)) + "/" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)) +
                        "/" + str.substring(2, str.length());
            case 'X': return Integer.toString(cal.get(Calendar.HOUR_OF_DAY)) + ":" + Integer.toString(cal.get(Calendar.MINUTE)) +
                        ":" + Integer.toString(cal.get(Calendar.SECOND));
            */
            case 'y' -> Integer.toString(cal.get(Calendar.YEAR) % 100);
            case 'Y' -> Integer.toString(cal.get(Calendar.YEAR));
            case 'Z' -> cal.getTimeZone().getID();
            default -> null; // bad input format.
        };
    }

    public static LuaTable getTableFromDate(Calendar c) {
        LuaTable time = new LuaTableImpl();
        time.rawset(YEAR, LuaState.toDouble(c.get(Calendar.YEAR)));
        time.rawset(MONTH, LuaState.toDouble(c.get(Calendar.MONTH)+1));
        time.rawset(DAY, LuaState.toDouble(c.get(Calendar.DAY_OF_MONTH)));
        time.rawset(HOUR, LuaState.toDouble(c.get(Calendar.HOUR_OF_DAY)));
        time.rawset(MIN, LuaState.toDouble(c.get(Calendar.MINUTE)));
        time.rawset(SEC, LuaState.toDouble(c.get(Calendar.SECOND)));
        time.rawset(WDAY, LuaState.toDouble(c.get(Calendar.DAY_OF_WEEK)));
        time.rawset(YDAY, LuaState.toDouble(getDayOfYear(c)));
        time.rawset(MILLISECOND, LuaState.toDouble(c.get(Calendar.MILLISECOND)));
        //time.rawset(ISDST, null);
        return time;
    }

    /**
     * converts the relevant fields in the given luatable to a Date object.
     * @param time LuaTable with entries for year month and day, and optionally hour/min/sec
     * @return a date object representing the date frim the luatable.
     */
    public static Date getDateFromTable(LuaTable time) {
        Calendar c = Calendar.getInstance(tzone);
        c.set(Calendar.YEAR,(int)LuaState.fromDouble(time.rawget(YEAR)));
        c.set(Calendar.MONTH,(int)LuaState.fromDouble(time.rawget(MONTH))-1);
        c.set(Calendar.DAY_OF_MONTH,(int)LuaState.fromDouble(time.rawget(DAY)));
        Object hour = time.rawget(HOUR);
        Object minute = time.rawget(MIN);
        Object seconds = time.rawget(SEC);
        Object milliseconds = time.rawget(MILLISECOND);
        //Object isDst = time.rawget(ISDST);
        if (hour != null) {
            c.set(Calendar.HOUR_OF_DAY,(int)LuaState.fromDouble(hour));
        } else {
            c.set(Calendar.HOUR_OF_DAY, 0);
        }
        if (minute != null) {
            c.set(Calendar.MINUTE,(int)LuaState.fromDouble(minute));
        } else {
            c.set(Calendar.MINUTE, 0);
        }
        if (seconds != null) {
            c.set(Calendar.SECOND,(int)LuaState.fromDouble(seconds));
        } else {
            c.set(Calendar.SECOND, 0);
        }
        if (milliseconds != null) {
            c.set(Calendar.MILLISECOND, (int)LuaState.fromDouble(milliseconds));
        } else {
            c.set(Calendar.MILLISECOND, 0);
        }
        // TODO: daylight savings support(is it possible?)
        return c.getTime();
    }

    public static int getDayOfYear(Calendar c) {
        Calendar c2 = Calendar.getInstance(c.getTimeZone());
        c2.setTime(c.getTime());
        c2.set(Calendar.MONTH, Calendar.JANUARY);
        c2.set(Calendar.DAY_OF_MONTH, 1);
        long diff = c.getTime().getTime() - c2.getTime().getTime();

        return (int)Math.ceil((double)diff / MILLIS_PER_DAY);
    }

    public static int getWeekOfYear(Calendar c, boolean weekStartsSunday, boolean jan1midweek) {
        Calendar c2 = Calendar.getInstance(c.getTimeZone());
        c2.setTime(c.getTime());
        c2.set(Calendar.MONTH, Calendar.JANUARY);
        c2.set(Calendar.DAY_OF_MONTH, 1);
        int dayOfWeek = c2.get(Calendar.DAY_OF_WEEK);
        if (weekStartsSunday && dayOfWeek != Calendar.SUNDAY) {
            c2.set(Calendar.DAY_OF_MONTH,(7 - dayOfWeek) + 1);
        } else if (dayOfWeek != Calendar.MONDAY) {
            c2.set(Calendar.DAY_OF_MONTH,(7 - dayOfWeek + 1) + 1);
        }
        long diff = c.getTime().getTime() - c2.getTime().getTime();

        int w = (int)(diff / MILLIS_PER_WEEK);

        if (jan1midweek && 7-dayOfWeek >= 4)
            w++;

        return w;
    }
}
