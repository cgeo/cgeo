package cgeo.geocaching.brouter.core;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public abstract class Formatter {

    static final String MESSAGES_HEADER = "Longitude\tLatitude\tElevation\tDistance\tCostPerKm\tElevCost\tTurnCost\tNodeCost\tInitialCost\tWayTags\tNodeTags\tTime\tEnergy";

    RoutingContext rc;

    Formatter(final RoutingContext rc) {
        this.rc = rc;
    }

    /**
     * writes the track in gpx-format to a file
     *
     * @param filename the filename to write to
     * @param t        the track to write
     */
    public void write(final String filename, final OsmTrack t) throws Exception {
        final BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
        bw.write(format(t));
        bw.close();
    }

    public OsmTrack read(final String filename) throws Exception {
        return null;
    }

    /**
     * writes the track in a selected output format to a string
     *
     * @param t the track to format
     * @return the formatted string
     */
    public abstract String format(OsmTrack t);


    static String formatILon(final int ilon) {
        return formatPos(ilon - 180000000);
    }

    static String formatILat(final int ilat) {
        return formatPos(ilat - 90000000);
    }

    private static String formatPos(final int p) {
        int pLocal = p;
        final boolean negative = pLocal < 0;
        if (negative) {
            pLocal = -pLocal;
        }
        final char[] ac = new char[12];
        int i = 11;
        while (pLocal != 0 || i > 3) {
            ac[i--] = (char) ('0' + (pLocal % 10));
            pLocal /= 10;
            if (i == 5) {
                ac[i--] = '.';
            }
        }
        if (negative) {
            ac[i--] = '-';
        }
        return new String(ac, i + 1, 11 - i);
    }

    public static String getFormattedTime2(final int s) {
        int seconds = (int) (s + 0.5);
        final int hours = seconds / 3600;
        final int minutes = (seconds - hours * 3600) / 60;
        seconds = seconds - hours * 3600 - minutes * 60;
        String time = "";
        if (hours != 0) {
            time = hours + "h ";
        }
        if (minutes != 0) {
            time = time + minutes + "m ";
        }
        if (seconds != 0) {
            time = time + seconds + "s";
        }
        return time;
    }

    public static String getFormattedEnergy(final int energy) {
        return format1(energy / 3600000.) + "kwh";
    }

    private static String format1(double n) {
        final String s = "" + (long) (n * 10 + 0.5);
        final int len = s.length();
        return s.substring(0, len - 1) + "." + s.charAt(len - 1);
    }


    static final String dateformat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static String getFormattedTime3(final float time) {
        final SimpleDateFormat timestampFormat = new SimpleDateFormat(dateformat, Locale.US);
        timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // yyyy-mm-ddThh:mm:ss.SSSZ
        final Date d = new Date((long) (time * 1000f));
        return timestampFormat.format(d);
    }


}
