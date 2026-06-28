package cgeo.geocaching.models;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

public final class UtmPatternUtils {

    private static final String ZONE_LETTER_REGEX = ".*([C-HJ-NP-X]).*";
    private static final String ALLOWED_FORMULA_CHARS_REGEX = "[^0-9()A-Z+\\-*/.,_ ]";

    private UtmPatternUtils() {
        // utility class
    }

    @NonNull
    public static UtmFields extractFieldsFromPatterns(final String latitudePattern, final String longitudePattern) {
        final String latPattern = StringUtils.defaultString(latitudePattern).toUpperCase();
        final String lonPattern = StringUtils.defaultString(longitudePattern).toUpperCase();
        final String merged = (latPattern + " " + lonPattern).replaceAll("[^A-Z0-9 ]", " ");
        final String[] tokens = merged.trim().split("\\s+");

        String zoneLetter = "";
        String zoneNumber = "";
        String easting = "";
        String northing = "";

        for (final String token : tokens) {
            if (StringUtils.isBlank(token)) {
                continue;
            }
            if (StringUtils.isBlank(zoneLetter) && token.matches(".*[C-HJ-NP-X].*")) {
                zoneLetter = token.replaceAll(ZONE_LETTER_REGEX, "$1");
            }
            if (StringUtils.isBlank(zoneNumber) && token.matches("\\d{1,2}")) {
                zoneNumber = token;
                continue;
            }
            if (StringUtils.isBlank(easting) && token.matches("\\d{5,7}")) {
                easting = token;
                continue;
            }
            if (StringUtils.isBlank(northing) && token.matches("\\d{6,8}")) {
                northing = token;
            }
        }

        if (StringUtils.isBlank(zoneNumber)) {
            zoneNumber = "31";
        }
        if (StringUtils.isBlank(zoneLetter)) {
            zoneLetter = "U";
        }

        return new UtmFields(zoneNumber + zoneLetter, easting, northing);
    }

    @NonNull
    public static UtmPatterns createPatternsFromPlainFields(final String zoneToken, final String eastingToken, final String northingToken, final boolean allowFormulaChars) {
        final String normalizedZone = StringUtils.defaultString(zoneToken).trim().toUpperCase();
        final String zoneNumber = normalizedZone.replaceAll("[^0-9]", "");
        String zoneLetter = normalizedZone.replaceAll(ZONE_LETTER_REGEX, "$1");
        if (!normalizedZone.matches(".*[C-HJ-NP-X].*")) {
            zoneLetter = "";
        }

        final String sanitizedEasting = sanitizeCoordinateToken(eastingToken, allowFormulaChars);
        final String sanitizedNorthing = sanitizeCoordinateToken(northingToken, allowFormulaChars);
        final String northingPadded = StringUtils.leftPad(sanitizedNorthing.replaceAll("\\s+", ""), 7, '0');

        return new UtmPatterns(
                zoneLetter + " " + StringUtils.defaultIfBlank(zoneNumber, "00") + " " + sanitizedEasting,
                "N " + northingPadded.substring(0, 1) + " " + northingPadded.substring(1));
    }

    @NonNull
    private static String sanitizeCoordinateToken(final String token, final boolean allowFormulaChars) {
        final String text = StringUtils.defaultString(token).trim().toUpperCase();
        if (!allowFormulaChars) {
            return text.replaceAll("\\s+", "");
        }
        return text.replaceAll(ALLOWED_FORMULA_CHARS_REGEX, "");
    }

    public static final class UtmFields {
        public final String zone;
        public final String easting;
        public final String northing;

        UtmFields(final String zone, final String easting, final String northing) {
            this.zone = zone;
            this.easting = easting;
            this.northing = northing;
        }
    }

    public static final class UtmPatterns {
        public final String latitudePattern;
        public final String longitudePattern;

        UtmPatterns(final String latitudePattern, final String longitudePattern) {
            this.latitudePattern = latitudePattern;
            this.longitudePattern = longitudePattern;
        }
    }
}
