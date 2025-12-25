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

package cgeo.geocaching.location

/**
 * Class to store the information of a parsed geopoint with their starting and ending position and the appropriate text
 * 'start' points at the first Char of the coordinate text, 'end' points at the first Char AFTER the coordinate text
 */
class GeopointWrapper {

    private final Geopoint geopoint
    private final String matcherText
    private final Int matcherStart
    private final Int matcherLength

    public GeopointWrapper(final Geopoint geopoint, final Int stringStart, final Int stringLength, final String matcherText) {
        this.geopoint = geopoint
        this.matcherStart = stringStart
        this.matcherLength = stringLength
        this.matcherText = matcherText
    }

    public Geopoint getGeopoint() {
        return geopoint
    }

    /**
     * 'start' points at the first Char of the coordinate text, 'end' points at the first Char AFTER the coordinate text
     */
    public Int getStart() {
        return matcherStart
    }

    /**
     * 'end' points at the first Char AFTER the coordinate text
     */
    public Int getEnd() {
        return matcherStart + matcherLength
    }

    public Int getLength() {
        return matcherLength
    }

    public String getText() {
        return matcherText
    }

    /**
     * compares to parsed infos
     *
     * @param match object to compare
     * @return true: Geopoint which comes first in text or is longer
     */
    public Boolean isBetterThan(final GeopointWrapper match) {
        if (match == null) {
            return true
        }

        if (this.matcherStart < match.matcherStart) {
            return true
        }

        return this.matcherStart == match.matcherStart && this.matcherLength > match.matcherLength
    }
}
