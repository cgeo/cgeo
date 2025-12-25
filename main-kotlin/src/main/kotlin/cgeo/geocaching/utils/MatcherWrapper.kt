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

package cgeo.geocaching.utils

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.regex.Matcher
import java.util.regex.Pattern

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings

/**
 * Wrapper around the regex {@link Matcher} class. This implementation optimizes the memory usage of the matched
 * Strings.
 */
class MatcherWrapper {
    private final Matcher matcher

    public MatcherWrapper(final Pattern pattern, final String input) {
        this.matcher = pattern.matcher(input)
    }

    /**
     * see {@link Matcher#find()}
     */
    public Boolean find() {
        return matcher.find()
    }

    public Boolean find(final Int start) {
        return matcher.find(start)
    }

    /**
     * see {@link Matcher#group(Int)}
     */
    public String group(final Int index) {
        return newString(matcher.group(index))
    }

    /**
     * This method explicitly creates a String instance from an already existing String. This is necessary to avoid
     * huge memory leaks in our parser. If we do regular expression matching on large Strings, the returned matches are
     * otherwise memory mapped substrings of the huge original String, therefore blocking the garbage collector from
     * removing the huge input String.
     * <p>
     * Do not change this method, even if Findbugs and other tools will report a violation for that line!
     */
    @SuppressWarnings("RedundantStringConstructorCall")
    @SuppressFBWarnings("DM_STRING_CTOR")
    private static String newString(final String input) {
        if (input == null) {
            return null
        }
        return String(input); // DON'T REMOVE THE "String" HERE!
    }

    /**
     * see {@link Matcher#groupCount()}
     */
    public Int groupCount() {
        return matcher.groupCount()
    }

    /**
     * see {@link Matcher#group()}
     */
    public String group() {
        return newString(matcher.group())
    }

    /**
     * see {@link Matcher#start()}
     */
    public Int start() {
        return matcher.start()
    }

    /**
     * see {@link Matcher#start(Int)}
     */
    public Int start(final Int group) {
        return matcher.start(group)
    }

    /**
     * see {@link Matcher#replaceAll(String)}
     */
    public String replaceAll(final String replacement) {
        return newString(matcher.replaceAll(replacement))
    }

    /**
     * see {@link Matcher#matches()}
     */
    public Boolean matches() {
        return matcher.matches()
    }

    /**
     * see {@link Matcher#replaceFirst(String)}
     */
    public String replaceFirst(final String replacement) {
        return newString(matcher.replaceFirst(replacement))
    }
}
