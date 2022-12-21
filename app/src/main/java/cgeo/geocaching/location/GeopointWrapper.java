package cgeo.geocaching.location;

/**
 * Class to store the information of a parsed geopoint with their starting and ending position and the appropriate text
 * 'start' points at the first char of the coordinate text, 'end' points at the first char AFTER the coordinate text
 */
public class GeopointWrapper {

    private final Geopoint geopoint;
    private final String matcherText;
    private final int matcherStart;
    private final int matcherLength;

    public GeopointWrapper(final Geopoint geopoint, final int stringStart, final int stringLength, final String matcherText) {
        this.geopoint = geopoint;
        this.matcherStart = stringStart;
        this.matcherLength = stringLength;
        this.matcherText = matcherText;
    }

    public Geopoint getGeopoint() {
        return geopoint;
    }

    /**
     * 'start' points at the first char of the coordinate text, 'end' points at the first char AFTER the coordinate text
     */
    public int getStart() {
        return matcherStart;
    }

    /**
     * 'end' points at the first char AFTER the coordinate text
     */
    public int getEnd() {
        return matcherStart + matcherLength;
    }

    public int getLength() {
        return matcherLength;
    }

    public String getText() {
        return matcherText;
    }

    /**
     * compares to parsed infos
     *
     * @param match object to compare
     * @return true: Geopoint which comes first in text or is longer
     */
    public Boolean isBetterThan(final GeopointWrapper match) {
        if (match == null) {
            return true;
        }

        if (this.matcherStart < match.matcherStart) {
            return true;
        }

        if (this.matcherStart == match.matcherStart && this.matcherLength > match.matcherLength) {
            return true;
        }

        return false;
    }
}
