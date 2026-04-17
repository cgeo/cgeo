package cgeo.geocaching.location;

import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a Military Grid Reference System (MGRS) coordinate.
 */
public class MGRSPoint {

    private static final String LATITUDE_BANDS = "CDEFGHJKLMNPQRSTUVWX";
    private static final String COLUMN_SET_1 = "ABCDEFGH";
    private static final String COLUMN_SET_2 = "JKLMNPQR";
    private static final String COLUMN_SET_3 = "STUVWXYZ";
    private static final String ROW_SET_ODD = "ABCDEFGHJKLMNPQRSTUV";
    private static final String ROW_SET_EVEN = "FGHJKLMNPQRSTUVABCDE";

    private static final double HUNDRED_K = 100000.0d;
    private static final double NORTHING_CYCLE = 2000000.0d;
    private static final int DEFAULT_PRECISION = 5;

        //                                                (1)   (2)      (3)          (4)           (5)               (6)              (7)
        static final Pattern PATTERN_MGRS = Pattern.compile(
            "(^|\\s)(\\d{1,2})([C-HJ-NP-X])\\s*([A-HJ-NP-Z])\\s*([A-HJ-NP-V])"
                + "(?:\\s*(\\d{1,5})\\s+(\\d{1,5})|\\s*([\\d]{2,10}))?(?=$|\\s|[.,;:])",
            Pattern.CASE_INSENSITIVE
        );

    private final int zoneNumber;
    private final char zoneLetter;
    private final char columnLetter;
    private final char rowLetter;
    private final double easting;
    private final double northing;
    private final int precision;

    public MGRSPoint(final String mgrsString) {
        final MatcherWrapper matcher = new MatcherWrapper(PATTERN_MGRS, mgrsString);
        if (!matcher.find()) {
            throw new ParseException("Unable to recognize MGRS format in String '" + mgrsString + "'");
        }

        final int parsedZoneNumber = parseZoneNumber(matcher.group(2), mgrsString);
        final char parsedZoneLetter = parseZoneLetter(matcher.group(3), mgrsString);
        final char parsedColumnLetter = Character.toUpperCase(matcher.group(4).charAt(0));
        final char parsedRowLetter = Character.toUpperCase(matcher.group(5).charAt(0));

        final String columnSet = getColumnSet(parsedZoneNumber);
        if (columnSet.indexOf(parsedColumnLetter) < 0) {
            throw new ParseException("Invalid MGRS easting letter for zone in String '" + mgrsString + "'");
        }

        final String rowSet = getRowSet(parsedZoneNumber);
        if (rowSet.indexOf(parsedRowLetter) < 0) {
            throw new ParseException("Invalid MGRS northing letter for zone in String '" + mgrsString + "'");
        }

        final ParsedPrecision parsedPrecision = parsePrecision(
                matcher.group(6),
                matcher.group(7),
                matcher.group(8),
                mgrsString
        );

        final int columnIndex = columnSet.indexOf(parsedColumnLetter);
        final int rowIndex = rowSet.indexOf(parsedRowLetter);
        final double cellSize = Math.pow(10.0d, 5 - parsedPrecision.precision);

        final double parsedEasting = (columnIndex + 1) * HUNDRED_K + parsedPrecision.eastingValue * cellSize;

        double parsedNorthing = rowIndex * HUNDRED_K + parsedPrecision.northingValue * cellSize;
        final double minNorthing = getMinNorthing(parsedZoneNumber, parsedZoneLetter);
        while (parsedNorthing < minNorthing) {
            parsedNorthing += NORTHING_CYCLE;
        }

        this.zoneNumber = parsedZoneNumber;
        this.zoneLetter = parsedZoneLetter;
        this.columnLetter = parsedColumnLetter;
        this.rowLetter = parsedRowLetter;
        this.easting = parsedEasting;
        this.northing = parsedNorthing;
        this.precision = parsedPrecision.precision;
    }

    private MGRSPoint(final int zoneNumber, final char zoneLetter, final char columnLetter, final char rowLetter,
                      final double easting, final double northing, final int precision) {
        this.zoneNumber = zoneNumber;
        this.zoneLetter = zoneLetter;
        this.columnLetter = columnLetter;
        this.rowLetter = rowLetter;
        this.easting = easting;
        this.northing = northing;
        this.precision = precision;
    }

    @NonNull
    public static MGRSPoint latLong2MGRS(final Geopoint geopoint) {
        final UTMPoint utmPoint = UTMPoint.latLong2UTM(geopoint);

        final int zoneNumber = utmPoint.getZoneNumber();
        final char zoneLetter = utmPoint.getZoneLetter();

        final double easting = Math.floor(utmPoint.getEasting());
        final double northing = Math.floor(utmPoint.getNorthing());

        final String columnSet = getColumnSet(zoneNumber);
        int columnNumber = (int) Math.floor(easting / HUNDRED_K);
        if (columnNumber < 1) {
            columnNumber = 1;
        } else if (columnNumber > columnSet.length()) {
            columnNumber = columnSet.length();
        }
        final char columnLetter = columnSet.charAt(columnNumber - 1);

        final String rowSet = getRowSet(zoneNumber);
        final int rowIndex = (int) Math.floor(northing / HUNDRED_K) % rowSet.length();
        final char rowLetter = rowSet.charAt(rowIndex);

        return new MGRSPoint(zoneNumber, zoneLetter, columnLetter, rowLetter, easting, northing, DEFAULT_PRECISION);
    }

    @NonNull
    public Geopoint toLatLong() {
        final double cellSize = Math.pow(10.0d, 5 - precision);
        final double centerEasting = easting + cellSize / 2.0d;
        final double centerNorthing = northing + cellSize / 2.0d;
        return new UTMPoint(zoneNumber, zoneLetter, centerEasting, centerNorthing).toLatLong();
    }

    @Override
    @NonNull
    public String toString() {
        if (precision == 0) {
            return String.format(Locale.getDefault(), "%d%c %c%c", zoneNumber, zoneLetter, columnLetter, rowLetter);
        }

        final long eastingRemainder = Math.floorMod((long) Math.floor(easting), 100000L);
        final long northingRemainder = Math.floorMod((long) Math.floor(northing), 100000L);
        final long divisor = (long) Math.pow(10.0d, 5 - precision);

        final long eastingValue = eastingRemainder / divisor;
        final long northingValue = northingRemainder / divisor;

        final String digitFormat = "%0" + precision + "d";
        return String.format(Locale.getDefault(), "%d%c %c%c " + digitFormat + " " + digitFormat,
                zoneNumber, zoneLetter, columnLetter, rowLetter, eastingValue, northingValue);
    }

    public int getZoneNumber() {
        return zoneNumber;
    }

    public char getZoneLetter() {
        return zoneLetter;
    }

    public double getEasting() {
        return easting;
    }

    public double getNorthing() {
        return northing;
    }

    public int getPrecision() {
        return precision;
    }

    private static int parseZoneNumber(final String zoneNumberText, final String source) {
        try {
            final int value = Integer.parseInt(zoneNumberText);
            if (value < 1 || value > 60) {
                throw new ParseException("Zone number out of range in String '" + source + "'");
            }
            return value;
        } catch (final NumberFormatException ignored) {
            throw new ParseException("Cannot parse MGRS zone number in String '" + source + "'");
        }
    }

    private static char parseZoneLetter(final String zoneLetterText, final String source) {
        final char value = Character.toUpperCase(zoneLetterText.charAt(0));
        if (LATITUDE_BANDS.indexOf(value) < 0) {
            throw new ParseException("Invalid MGRS latitude band in String '" + source + "'");
        }
        return value;
    }

    private static ParsedPrecision parsePrecision(final String splitEasting, final String splitNorthing,
                                                  final String compactDigits, final String source) {

        if (StringUtils.isNotBlank(compactDigits)) {
            final String digits = compactDigits.trim();
            if (digits.length() % 2 != 0 || digits.length() > 10) {
                throw new ParseException("Invalid MGRS precision in String '" + source + "'");
            }
            final int precision = digits.length() / 2;
            final int eastingValue = Integer.parseInt(digits.substring(0, precision));
            final int northingValue = Integer.parseInt(digits.substring(precision));
            return new ParsedPrecision(precision, eastingValue, northingValue);
        }

        if (StringUtils.isNotBlank(splitEasting) || StringUtils.isNotBlank(splitNorthing)) {
            if (StringUtils.isBlank(splitEasting) || StringUtils.isBlank(splitNorthing)) {
                throw new ParseException("Incomplete MGRS precision in String '" + source + "'");
            }

            final String eastingDigits = splitEasting.trim();
            final String northingDigits = splitNorthing.trim();
            if (eastingDigits.length() != northingDigits.length() || eastingDigits.length() > 5) {
                throw new ParseException("Invalid MGRS precision in String '" + source + "'");
            }

            return new ParsedPrecision(eastingDigits.length(), Integer.parseInt(eastingDigits), Integer.parseInt(northingDigits));
        }

        return new ParsedPrecision(0, 0, 0);
    }

    private static String getColumnSet(final int zoneNumber) {
        switch (zoneNumber % 3) {
            case 1:
                return COLUMN_SET_1;
            case 2:
                return COLUMN_SET_2;
            default:
                return COLUMN_SET_3;
        }
    }

    private static String getRowSet(final int zoneNumber) {
        return (zoneNumber % 2 == 0) ? ROW_SET_EVEN : ROW_SET_ODD;
    }

    private static double getMinNorthing(final int zoneNumber, final char zoneLetter) {
        final double minLatitude = getBandMinimumLatitude(zoneLetter);
        final double zoneCenterLongitude = zoneNumber * 6.0d - 183.0d;
        return UTMPoint.latLong2UTM(new Geopoint(minLatitude, zoneCenterLongitude)).getNorthing();
    }

    private static double getBandMinimumLatitude(final char zoneLetter) {
        switch (zoneLetter) {
            case 'C':
                return -80.0d;
            case 'D':
                return -72.0d;
            case 'E':
                return -64.0d;
            case 'F':
                return -56.0d;
            case 'G':
                return -48.0d;
            case 'H':
                return -40.0d;
            case 'J':
                return -32.0d;
            case 'K':
                return -24.0d;
            case 'L':
                return -16.0d;
            case 'M':
                return -8.0d;
            case 'N':
                return 0.0d;
            case 'P':
                return 8.0d;
            case 'Q':
                return 16.0d;
            case 'R':
                return 24.0d;
            case 'S':
                return 32.0d;
            case 'T':
                return 40.0d;
            case 'U':
                return 48.0d;
            case 'V':
                return 56.0d;
            case 'W':
                return 64.0d;
            case 'X':
                return 72.0d;
            default:
                throw new ParseException("Invalid MGRS latitude band");
        }
    }

    private static class ParsedPrecision {
        private final int precision;
        private final int eastingValue;
        private final int northingValue;

        ParsedPrecision(final int precision, final int eastingValue, final int northingValue) {
            this.precision = precision;
            this.eastingValue = eastingValue;
            this.northingValue = northingValue;
        }
    }

    public static class ParseException extends NumberFormatException {
        private static final long serialVersionUID = 1L;

        ParseException(final String message) {
            super(message);
        }
    }
}
