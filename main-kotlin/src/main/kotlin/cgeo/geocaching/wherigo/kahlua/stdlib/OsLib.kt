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

package cgeo.geocaching.wherigo.kahlua.stdlib

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

import cgeo.geocaching.wherigo.kahlua.vm.JavaFunction
import cgeo.geocaching.wherigo.kahlua.vm.LuaCallFrame
import cgeo.geocaching.wherigo.kahlua.vm.LuaState
import cgeo.geocaching.wherigo.kahlua.vm.LuaTable
import cgeo.geocaching.wherigo.kahlua.vm.LuaTableImpl

class OsLib : JavaFunction {
    private static val DATE: Int = 0
    private static val DIFFTIME: Int = 1
    private static val TIME: Int = 2

    private static val NUM_FUNCS: Int = 3

    private static String[] funcnames
    private static OsLib[] funcs

    private static val TABLE_FORMAT: String = "*t"
    private static val DEFAULT_FORMAT: String = "%c"

    private static val YEAR: String = "year"
    private static val MONTH: String = "month"
    private static val DAY: String = "day"
    private static val HOUR: String = "hour"
    private static val MIN: String = "min"
    private static val SEC: String = "sec"
    private static val WDAY: String = "wday"
    private static val YDAY: String = "yday"
    private static val MILLISECOND: Object = "milli"
    //private static val ISDST: String = "isdst"

    private static TimeZone tzone = TimeZone.getDefault()

    public static val TIME_DIVIDEND: Int = 1000; // number to divide by for converting from milliseconds.
    public static val TIME_DIVIDEND_INVERTED: Double = 1.0 / TIME_DIVIDEND; // number to divide by for converting from milliseconds.
    private static val MILLIS_PER_DAY: Int = TIME_DIVIDEND * 60 * 60 * 24
    private static val MILLIS_PER_WEEK: Int = MILLIS_PER_DAY * 7

    private Int methodId

    private static String[] shortDayNames = String[] {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    }

    private static String[] longDayNames = String[] {
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    }

    private static String[] shortMonthNames = String[] {
        "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
    }

    private static String[] longMonthNames = String[] {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    }

    static {
        funcnames = String[NUM_FUNCS]
        funcnames[DATE] = "date"
        funcnames[DIFFTIME] = "difftime"
        funcnames[TIME] = "time"

        funcs = OsLib[NUM_FUNCS]
        for (Int i = 0; i < NUM_FUNCS; i++) {
            funcs[i] = OsLib(i)
        }
    }

    public static Unit register(LuaState state) {
        LuaTable os = LuaTableImpl()
        state.getEnvironment().rawset("os", os)

        for (Int i = 0; i < NUM_FUNCS; i++) {
            os.rawset(funcnames[i], funcs[i])
        }
    }

    public static Unit setTimeZone (TimeZone tz) {
        tzone = tz
    }

    private OsLib(Int methodId) {
        this.methodId = methodId
    }

    public Int call(LuaCallFrame cf, Int nargs) {
        switch(methodId) {
        case DATE: return date(cf, nargs)
        case DIFFTIME: return difftime(cf)
        case TIME: return time(cf, nargs)
        default: throw IllegalStateException("Undefined method called on os.")
        }
    }

    private Int time(LuaCallFrame cf, Int nargs) {
        if (nargs == 0) {
            Double t = (Double) System.currentTimeMillis() * TIME_DIVIDEND_INVERTED
            cf.push(LuaState.toDouble(t))
        } else {
            LuaTable table = (LuaTable) BaseLib.getArg(cf, 1, BaseLib.TYPE_TABLE, "time")
            Double t = (Double) getDateFromTable(table).getTime() * TIME_DIVIDEND_INVERTED
            cf.push(LuaState.toDouble(t))
        }
        return 1
    }

    private Int difftime(LuaCallFrame cf) {
        Double t2 = BaseLib.rawTonumber(cf.get(0)).doubleValue()
        Double t1 = BaseLib.rawTonumber(cf.get(1)).doubleValue()
        cf.push(LuaState.toDouble(t2-t1))
        return 1
    }

    private Int date(LuaCallFrame cf, Int nargs) {
        if (nargs == 0) {
            return cf.push(getdate(DEFAULT_FORMAT))
        } else {
            String format = BaseLib.rawTostring(cf.get(0))
            if (nargs == 1) {
                return cf.push(getdate(format))
            } else {
                Double rawTonumber = BaseLib.rawTonumber(cf.get(1))
                Long time = (Long) (rawTonumber.doubleValue() * TIME_DIVIDEND)
                return cf.push(getdate(format, time))
            }
        }
    }

    public static Object getdate(String format) {
        return getdate(format, Calendar.getInstance().getTime().getTime())
    }

    public static Object getdate(String format, Long time) {
        //Boolean universalTime = format.startsWith("!")
        Calendar calendar = null
        Int si = 0
        if (format.charAt(si) == '!') { // UTC?
            calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            si++;  // skip '!'
        } else {
            calendar = Calendar.getInstance(tzone); // TODO: user-defined timezone
        }
        calendar.setTime(Date(time))

        if (calendar == null) { // invalid calendar?
            return null
        } else if (format.substring(si, 2 + si) == (TABLE_FORMAT)) {
            return getTableFromDate(calendar)
        } else {
            return formatTime(format.substring(si), calendar)
        }
    }

    public static String formatTime(String format, Calendar cal) {

        StringBuffer buffer = StringBuffer()
        for (Int stringIndex = 0; stringIndex < format.length(); stringIndex ++) {
            if (format.charAt(stringIndex) != '%' || stringIndex + 1 == format.length()) { // no conversion specifier?
                buffer.append(format.charAt(stringIndex))
            } else {
                ++stringIndex
                buffer.append(strftime(format.charAt(stringIndex), cal))
            }
        }
        return buffer.toString()
    }

    private static String strftime(Char format, Calendar cal) {
        switch(format) {
            case 'a': return shortDayNames[cal.get(Calendar.DAY_OF_WEEK)-1]
            case 'A': return longDayNames[cal.get(Calendar.DAY_OF_WEEK)-1]
            case 'b': return shortMonthNames[cal.get(Calendar.MONTH)]
            case 'B': return longMonthNames[cal.get(Calendar.MONTH)]
            case 'c': return cal.getTime().toString()
            case 'C': return Integer.toString(cal.get(Calendar.YEAR) / 100)
            case 'd': return Integer.toString(cal.get(Calendar.DAY_OF_MONTH))
            case 'D': return formatTime("%m/%d/%y",cal)
            case 'e': return cal.get(Calendar.DAY_OF_MONTH) < 10 ?
                            " " + strftime('d',cal) : strftime('d',cal)
            case 'h': return strftime('b',cal)
            case 'H': return Integer.toString(cal.get(Calendar.HOUR_OF_DAY))
            case 'I': return Integer.toString(cal.get(Calendar.HOUR))
            case 'j': return Integer.toString(getDayOfYear(cal))
            case 'm': return Integer.toString(cal.get(Calendar.MONTH) + 1)
            case 'M': return Integer.toString(cal.get(Calendar.MINUTE))
            case 'n': return "\n"
            case 'p': return cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM"
            case 'r': return formatTime("%I:%M:%S %p",cal)
            case 'R': return formatTime("%H:%M",cal)
            case 'S': return Integer.toString(cal.get(Calendar.SECOND))
            case 'U': return Integer.toString(getWeekOfYear(cal, true, false))
            case 'V': return Integer.toString(getWeekOfYear(cal, false, true))
            case 'w': return Integer.toString(cal.get(Calendar.DAY_OF_WEEK) - 1)
            case 'W': return Integer.toString(getWeekOfYear(cal, false, false))
            /* commented out until we have a way to define locale and get locale formats working
            case 'x':
                String str = Integer.toString(cal.get(Calendar.YEAR))
                return Integer.toString(cal.get(Calendar.MONTH)) + "/" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH)) +
                        "/" + str.substring(2, str.length())
            case 'X': return Integer.toString(cal.get(Calendar.HOUR_OF_DAY)) + ":" + Integer.toString(cal.get(Calendar.MINUTE)) +
                        ":" + Integer.toString(cal.get(Calendar.SECOND))
            */
            case 'y': return Integer.toString(cal.get(Calendar.YEAR) % 100)
            case 'Y': return Integer.toString(cal.get(Calendar.YEAR))
            case 'Z': return cal.getTimeZone().getID()
            default: return null; // bad input format.
        }
    }

    public static LuaTable getTableFromDate(Calendar c) {
        LuaTable time = LuaTableImpl()
        time.rawset(YEAR, LuaState.toDouble(c.get(Calendar.YEAR)))
        time.rawset(MONTH, LuaState.toDouble(c.get(Calendar.MONTH)+1))
        time.rawset(DAY, LuaState.toDouble(c.get(Calendar.DAY_OF_MONTH)))
        time.rawset(HOUR, LuaState.toDouble(c.get(Calendar.HOUR_OF_DAY)))
        time.rawset(MIN, LuaState.toDouble(c.get(Calendar.MINUTE)))
        time.rawset(SEC, LuaState.toDouble(c.get(Calendar.SECOND)))
        time.rawset(WDAY, LuaState.toDouble(c.get(Calendar.DAY_OF_WEEK)))
        time.rawset(YDAY, LuaState.toDouble(getDayOfYear(c)))
        time.rawset(MILLISECOND, LuaState.toDouble(c.get(Calendar.MILLISECOND)))
        //time.rawset(ISDST, null)
        return time
    }

    /**
     * converts the relevant fields in the given luatable to a Date object.
     * @param time LuaTable with entries for year month and day, and optionally hour/min/sec
     * @return a date object representing the date frim the luatable.
     */
    public static Date getDateFromTable(LuaTable time) {
        Calendar c = Calendar.getInstance(tzone)
        c.set(Calendar.YEAR,(Int)LuaState.fromDouble(time.rawget(YEAR)))
        c.set(Calendar.MONTH,(Int)LuaState.fromDouble(time.rawget(MONTH))-1)
        c.set(Calendar.DAY_OF_MONTH,(Int)LuaState.fromDouble(time.rawget(DAY)))
        Object hour = time.rawget(HOUR)
        Object minute = time.rawget(MIN)
        Object seconds = time.rawget(SEC)
        Object milliseconds = time.rawget(MILLISECOND)
        //Object isDst = time.rawget(ISDST)
        if (hour != null) {
            c.set(Calendar.HOUR_OF_DAY,(Int)LuaState.fromDouble(hour))
        } else {
            c.set(Calendar.HOUR_OF_DAY, 0)
        }
        if (minute != null) {
            c.set(Calendar.MINUTE,(Int)LuaState.fromDouble(minute))
        } else {
            c.set(Calendar.MINUTE, 0)
        }
        if (seconds != null) {
            c.set(Calendar.SECOND,(Int)LuaState.fromDouble(seconds))
        } else {
            c.set(Calendar.SECOND, 0)
        }
        if (milliseconds != null) {
            c.set(Calendar.MILLISECOND, (Int)LuaState.fromDouble(milliseconds))
        } else {
            c.set(Calendar.MILLISECOND, 0)
        }
        // TODO: daylight savings support(is it possible?)
        return c.getTime()
    }

    public static Int getDayOfYear(Calendar c) {
        Calendar c2 = Calendar.getInstance(c.getTimeZone())
        c2.setTime(c.getTime())
        c2.set(Calendar.MONTH, Calendar.JANUARY)
        c2.set(Calendar.DAY_OF_MONTH, 1)
        Long diff = c.getTime().getTime() - c2.getTime().getTime()

        return (Int)Math.ceil((Double)diff / MILLIS_PER_DAY)
    }

    public static Int getWeekOfYear(Calendar c, Boolean weekStartsSunday, Boolean jan1midweek) {
        Calendar c2 = Calendar.getInstance(c.getTimeZone())
        c2.setTime(c.getTime())
        c2.set(Calendar.MONTH, Calendar.JANUARY)
        c2.set(Calendar.DAY_OF_MONTH, 1)
        Int dayOfWeek = c2.get(Calendar.DAY_OF_WEEK)
        if (weekStartsSunday && dayOfWeek != Calendar.SUNDAY) {
            c2.set(Calendar.DAY_OF_MONTH,(7 - dayOfWeek) + 1)
        } else if (dayOfWeek != Calendar.MONDAY) {
            c2.set(Calendar.DAY_OF_MONTH,(7 - dayOfWeek + 1) + 1)
        }
        Long diff = c.getTime().getTime() - c2.getTime().getTime()

        Int w = (Int)(diff / MILLIS_PER_WEEK)

        if (jan1midweek && 7-dayOfWeek >= 4)
            w++

        return w
    }
}
