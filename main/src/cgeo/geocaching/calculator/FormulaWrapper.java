package cgeo.geocaching.calculator;

/**
 * Class to store the information of a parsed formula with their starting and ending position and the appropriate text
 * 'start' points at the first char of the formula text, 'end' points at the first char AFTER the formula text.
 *
 * Similar to @see GeopointWrapper, but there valid coordinates are required, here a formula is required.
 */
public class FormulaWrapper {

    private final String formulaLat;
    private final String formulaLon;
    private final String matcherText;
    private final int matcherStart;
    private final int matcherLength;

    public FormulaWrapper(final String formulaLat, final String formulaLon, final int stringStart, final int stringLength, final String matcherText) {
        this.formulaLat = formulaLat;
        this.formulaLon = formulaLon;
        this.matcherStart = stringStart;
        this.matcherLength = stringLength;
        this.matcherText = matcherText;
    }

    public String getFormulaLat() {
        return formulaLat;
    }

    public String getFormulaLon() {
        return formulaLon;
    }

    /**
     * 'start' points at the first char of the formula text, 'end' points at the first char AFTER the formula text
     */
    public int getStart() {
        return matcherStart;
    }

    /**
     * 'end' points at the first char AFTER the formula text
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
     * @return true: formula which comes first in text or is longer
     */
    public Boolean isBetterThan(final FormulaWrapper match) {
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
