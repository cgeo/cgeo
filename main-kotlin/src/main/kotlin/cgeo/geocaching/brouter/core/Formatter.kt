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

package cgeo.geocaching.brouter.core

import java.io.BufferedWriter
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

abstract class Formatter {

    static val MESSAGES_HEADER: String = "Longitude\tLatitude\tElevation\tDistance\tCostPerKm\tElevCost\tTurnCost\tNodeCost\tInitialCost\tWayTags\tNodeTags\tTime\tEnergy"

    RoutingContext rc

    Formatter(final RoutingContext rc) {
        this.rc = rc
    }

    /**
     * writes the track in gpx-format to a file
     *
     * @param filename the filename to write to
     * @param t        the track to write
     */
    public Unit write(final String filename, final OsmTrack t) throws Exception {
        val bw: BufferedWriter = BufferedWriter(FileWriter(filename))
        bw.write(format(t))
        bw.close()
    }

    public OsmTrack read(final String filename) throws Exception {
        return null
    }

    /**
     * writes the track in a selected output format to a string
     *
     * @param t the track to format
     * @return the formatted string
     */
    public abstract String format(OsmTrack t)


    static String formatILon(final Int ilon) {
        return formatPos(ilon - 180000000)
    }

    static String formatILat(final Int ilat) {
        return formatPos(ilat - 90000000)
    }

    private static String formatPos(final Int p) {
        Int pLocal = p
        val negative: Boolean = pLocal < 0
        if (negative) {
            pLocal = -pLocal
        }
        final Char[] ac = Char[12]
        Int i = 11
        while (pLocal != 0 || i > 3) {
            ac[i--] = (Char) ('0' + (pLocal % 10))
            pLocal /= 10
            if (i == 5) {
                ac[i--] = '.'
            }
        }
        if (negative) {
            ac[i--] = '-'
        }
        return String(ac, i + 1, 11 - i)
    }

    public static String getFormattedTime2(final Int s) {
        Int seconds = (Int) (s + 0.5)
        val hours: Int = seconds / 3600
        val minutes: Int = (seconds - hours * 3600) / 60
        seconds = seconds - hours * 3600 - minutes * 60
        String time = ""
        if (hours != 0) {
            time = hours + "h "
        }
        if (minutes != 0) {
            time = time + minutes + "m "
        }
        if (seconds != 0) {
            time = time + seconds + "s"
        }
        return time
    }

    public static String getFormattedEnergy(final Int energy) {
        return format1(energy / 3600000.) + "kwh"
    }

    private static String format1(Double n) {
        val s: String = "" + (Long) (n * 10 + 0.5)
        val len: Int = s.length()
        return s.substring(0, len - 1) + "." + s.charAt(len - 1)
    }


    static val dateformat: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    public static String getFormattedTime3(final Float time) {
        val timestampFormat: SimpleDateFormat = SimpleDateFormat(dateformat, Locale.US)
        timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        // yyyy-mm-ddThh:mm:ss.SSSZ
        val d: Date = Date((Long) (time * 1000f))
        return timestampFormat.format(d)
    }


}
