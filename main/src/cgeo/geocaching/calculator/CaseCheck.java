package cgeo.geocaching.calculator;

/**
 * Class used for checking that a value is with in a given range.
 * This is used to check for upper-case an lower-case letters.
 */
public final class CaseCheck {
    private final boolean useUpper;

    public CaseCheck(final boolean upper) {
        useUpper = upper;
    }

    public boolean check(final char ch) {

        boolean returnValue = Character.isLetterOrDigit(ch);
        if (useUpper) {
            returnValue &= Character.isUpperCase(ch);
        } else {
            returnValue &= Character.isLowerCase(ch);
        }

        return returnValue;
    }
}
