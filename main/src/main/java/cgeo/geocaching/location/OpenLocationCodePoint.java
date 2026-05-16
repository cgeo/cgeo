package cgeo.geocaching.location;

import cgeo.geocaching.utils.MatcherWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents an Open Location Code (OLC, Plus Code).
 */
public class OpenLocationCodePoint {

    private static final String CODE_ALPHABET = "23456789CFGHJMPQRVWX";

    private static final char SEPARATOR = '+';
    private static final int SEPARATOR_POSITION = 8;
    private static final int DEFAULT_CODE_LENGTH = 10;
    private static final int PAIR_CODE_LENGTH = 10;

    private static final double[] PAIR_RESOLUTIONS = new double[]{20.0d, 1.0d, 0.05d, 0.0025d, 0.000125d};
    private static final int GRID_ROWS = 5;
    private static final int GRID_COLUMNS = 4;

        //                                                         (1)            (2)
        static final Pattern PATTERN_OLC = Pattern.compile(
            "(^|[^23456789CFGHJMPQRVWX])([23456789CFGHJMPQRVWX]{2,8}\\+[23456789CFGHJMPQRVWX]{2,7})(?=$|[^23456789CFGHJMPQRVWX])",
            Pattern.CASE_INSENSITIVE
        );

    private final String code;
    private final Geopoint center;
    private final double latitudeResolutionDeg;
    private final double longitudeResolutionDeg;

    public OpenLocationCodePoint(final String codeText) {
        this(codeText, null);
    }

    public OpenLocationCodePoint(final String codeText, @Nullable final Geopoint referencePoint) {
        final MatcherWrapper matcher = new MatcherWrapper(PATTERN_OLC, codeText);
        if (!matcher.find()) {
            throw new ParseException("Unable to recognize OLC format in String '" + codeText + "'");
        }

        final String normalizedCode = normalizeCode(matcher.group(2));
        this.code = isShortCode(normalizedCode)
                ? recoverNearestFullCode(normalizedCode, referencePoint, codeText)
                : normalizeFullCode(normalizedCode);
        final DecodedCode decodedCode = decode(this.code);
        this.center = decodedCode.center;
        this.latitudeResolutionDeg = decodedCode.latitudeResolutionDeg;
        this.longitudeResolutionDeg = decodedCode.longitudeResolutionDeg;
    }

    private OpenLocationCodePoint(final String code, final Geopoint center,
                                  final double latitudeResolutionDeg, final double longitudeResolutionDeg) {
        this.code = code;
        this.center = center;
        this.latitudeResolutionDeg = latitudeResolutionDeg;
        this.longitudeResolutionDeg = longitudeResolutionDeg;
    }

    @NonNull
    public static OpenLocationCodePoint latLong2OLC(final Geopoint geopoint) {
        final String code = encode(geopoint.getLatitude(), geopoint.getLongitude(), DEFAULT_CODE_LENGTH);
        final DecodedCode decodedCode = decode(code);
        return new OpenLocationCodePoint(code, decodedCode.center, decodedCode.latitudeResolutionDeg, decodedCode.longitudeResolutionDeg);
    }

    @NonNull
    public Geopoint toLatLong() {
        return center;
    }

    public double getLatitudeResolutionDeg() {
        return latitudeResolutionDeg;
    }

    public double getLongitudeResolutionDeg() {
        return longitudeResolutionDeg;
    }

    @Override
    @NonNull
    public String toString() {
        return code;
    }

    @NonNull
    private static String encode(final double latitude, final double longitude, final int codeLength) {
        final int effectiveLength = Math.max(2, codeLength);
        final StringBuilder codeBuilder = new StringBuilder();

        double lat = clipLatitude(latitude);
        double lon = normalizeLongitude(longitude);

        if (lat == 90.0d) {
            lat = Math.nextDown(lat);
        }

        lat += 90.0d;
        lon += 180.0d;

        final int pairLength = Math.min(effectiveLength, PAIR_CODE_LENGTH);
        for (int i = 0; i < pairLength / 2; i++) {
            final double placeValue = PAIR_RESOLUTIONS[i];
            final int latDigit = (int) Math.floor(lat / placeValue);
            final int lonDigit = (int) Math.floor(lon / placeValue);

            codeBuilder.append(CODE_ALPHABET.charAt(latDigit));
            codeBuilder.append(CODE_ALPHABET.charAt(lonDigit));

            lat -= latDigit * placeValue;
            lon -= lonDigit * placeValue;
        }

        double latPlaceValue = PAIR_RESOLUTIONS[PAIR_RESOLUTIONS.length - 1];
        double lonPlaceValue = PAIR_RESOLUTIONS[PAIR_RESOLUTIONS.length - 1];

        for (int i = PAIR_CODE_LENGTH; i < effectiveLength; i++) {
            latPlaceValue /= GRID_ROWS;
            lonPlaceValue /= GRID_COLUMNS;

            int row = (int) Math.floor(lat / latPlaceValue);
            int col = (int) Math.floor(lon / lonPlaceValue);

            if (row >= GRID_ROWS) {
                row = GRID_ROWS - 1;
            }
            if (col >= GRID_COLUMNS) {
                col = GRID_COLUMNS - 1;
            }

            final int index = row * GRID_COLUMNS + col;
            codeBuilder.append(CODE_ALPHABET.charAt(index));

            lat -= row * latPlaceValue;
            lon -= col * lonPlaceValue;
        }

        codeBuilder.insert(SEPARATOR_POSITION, SEPARATOR);
        return codeBuilder.toString();
    }

    @NonNull
    private static DecodedCode decode(final String codeText) {
        final String normalized = normalizeFullCode(codeText);
        final String code = normalized.replace(String.valueOf(SEPARATOR), "");

        final int pairLength = Math.min(code.length(), PAIR_CODE_LENGTH);
        double latitude = -90.0d;
        double longitude = -180.0d;

        for (int i = 0; i < pairLength / 2; i++) {
            latitude += getCodeIndex(code.charAt(i * 2)) * PAIR_RESOLUTIONS[i];
            longitude += getCodeIndex(code.charAt(i * 2 + 1)) * PAIR_RESOLUTIONS[i];
        }

        double latPlaceValue = PAIR_RESOLUTIONS[pairLength / 2 - 1];
        double lonPlaceValue = PAIR_RESOLUTIONS[pairLength / 2 - 1];

        for (int i = pairLength; i < code.length(); i++) {
            final int index = getCodeIndex(code.charAt(i));
            final int row = index / GRID_COLUMNS;
            final int col = index % GRID_COLUMNS;

            latPlaceValue /= GRID_ROWS;
            lonPlaceValue /= GRID_COLUMNS;

            latitude += row * latPlaceValue;
            longitude += col * lonPlaceValue;
        }

        return new DecodedCode(
            new Geopoint(latitude + latPlaceValue / 2.0d, longitude + lonPlaceValue / 2.0d),
            latPlaceValue,
            lonPlaceValue
        );
    }

    @NonNull
    private static String normalizeFullCode(final String codeText) {
        final String trimmed = normalizeCode(codeText);
        final int separatorIndex = trimmed.indexOf(SEPARATOR);

        if (separatorIndex != SEPARATOR_POSITION || separatorIndex != trimmed.lastIndexOf(SEPARATOR)) {
            throw new ParseException("Invalid OLC separator position in String '" + codeText + "'");
        }

        final int suffixLength = trimmed.length() - separatorIndex - 1;
        if (suffixLength < 2) {
            throw new ParseException("OLC requires at least two characters after separator in String '" + codeText + "'");
        }

        final String code = trimmed.replace(String.valueOf(SEPARATOR), "");
        if (code.length() < PAIR_CODE_LENGTH) {
            throw new ParseException("OLC full code must contain at least 10 characters in String '" + codeText + "'");
        }

        for (int i = 0; i < code.length(); i++) {
            if (getCodeIndex(code.charAt(i)) < 0) {
                throw new ParseException("Invalid OLC character in String '" + codeText + "'");
            }
        }

        return trimmed;
    }

    @NonNull
    private static String normalizeCode(final String codeText) {
        return codeText.trim().toUpperCase(Locale.US).replace(" ", "");
    }

    private static boolean isShortCode(final String codeText) {
        final int separatorIndex = codeText.indexOf(SEPARATOR);
        return separatorIndex >= 0 && separatorIndex < SEPARATOR_POSITION;
    }

    @NonNull
    private static String normalizeShortCode(final String codeText, final String originalCodeText) {
        final String trimmed = normalizeCode(codeText);
        final int separatorIndex = trimmed.indexOf(SEPARATOR);

        if (separatorIndex < 2 || separatorIndex >= SEPARATOR_POSITION || separatorIndex % 2 != 0 ||
                separatorIndex != trimmed.lastIndexOf(SEPARATOR)) {
            throw new ParseException("Invalid short OLC separator position in String '" + originalCodeText + "'");
        }

        final int suffixLength = trimmed.length() - separatorIndex - 1;
        if (suffixLength < 2 || suffixLength > 7) {
            throw new ParseException("Short OLC requires 2-7 characters after separator in String '" + originalCodeText + "'");
        }

        final String code = trimmed.replace(String.valueOf(SEPARATOR), "");
        for (int i = 0; i < code.length(); i++) {
            if (getCodeIndex(code.charAt(i)) < 0) {
                throw new ParseException("Invalid OLC character in String '" + originalCodeText + "'");
            }
        }

        return trimmed;
    }

    @NonNull
    private static String recoverNearestFullCode(@NonNull final String shortCodeText,
                                                 @Nullable final Geopoint referencePoint,
                                                 @NonNull final String originalCodeText) {
        if (referencePoint == null) {
            throw new ParseException("Short OLC requires a reference location in String '" + originalCodeText + "'");
        }

        final String shortCode = normalizeShortCode(shortCodeText, originalCodeText);
        final int separatorIndex = shortCode.indexOf(SEPARATOR);
        final int paddingLength = SEPARATOR_POSITION - separatorIndex;
        final int recoveredCodeLength = shortCode.replace(String.valueOf(SEPARATOR), "").length() + paddingLength;

        final String referenceCode = encode(referencePoint.getLatitude(), referencePoint.getLongitude(), DEFAULT_CODE_LENGTH);
        final String recoveredCandidate = referenceCode.substring(0, paddingLength) + shortCode;
        final DecodedCode recoveredCode = decode(recoveredCandidate);

        final double resolution = Math.pow(20.0d, 2.0d - (paddingLength / 2.0d));
        final double areaToEdge = resolution / 2.0d;

        double recoveredLatitude = recoveredCode.center.getLatitude();
        if (referencePoint.getLatitude() + areaToEdge < recoveredLatitude && recoveredLatitude - resolution >= -90.0d) {
            recoveredLatitude -= resolution;
        } else if (referencePoint.getLatitude() - areaToEdge > recoveredLatitude && recoveredLatitude + resolution <= 90.0d) {
            recoveredLatitude += resolution;
        }

        double recoveredLongitude = recoveredCode.center.getLongitude();
        if (referencePoint.getLongitude() + areaToEdge < recoveredLongitude) {
            recoveredLongitude -= resolution;
        } else if (referencePoint.getLongitude() - areaToEdge > recoveredLongitude) {
            recoveredLongitude += resolution;
        }

        return encode(clipLatitude(recoveredLatitude), normalizeLongitude(recoveredLongitude), recoveredCodeLength);
    }

    private static int getCodeIndex(final char character) {
        return CODE_ALPHABET.indexOf(character);
    }

    private static double clipLatitude(final double latitude) {
        return Math.max(-90.0d, Math.min(90.0d, latitude));
    }

    private static double normalizeLongitude(final double longitude) {
        double normalized = longitude;
        while (normalized < -180.0d) {
            normalized += 360.0d;
        }
        while (normalized >= 180.0d) {
            normalized -= 360.0d;
        }
        return normalized;
    }

    public static class ParseException extends NumberFormatException {
        private static final long serialVersionUID = 1L;

        ParseException(final String message) {
            super(message);
        }
    }

    private static class DecodedCode {
        private final Geopoint center;
        private final double latitudeResolutionDeg;
        private final double longitudeResolutionDeg;

        DecodedCode(final Geopoint center, final double latitudeResolutionDeg, final double longitudeResolutionDeg) {
            this.center = center;
            this.latitudeResolutionDeg = latitudeResolutionDeg;
            this.longitudeResolutionDeg = longitudeResolutionDeg;
        }
    }
}
