package cgeo.geocaching.utils;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Wrapper around the regex {@link Matcher} class. This implementation optimizes the memory usage of the matched
 * Strings.
 *
 */
public class MatcherWrapper {
    private final Matcher matcher;

    public MatcherWrapper(@NonNull final Pattern pattern, @NonNull final String input) {
        this.matcher = pattern.matcher(input);
    }

    /**
     * see {@link Matcher#find()}
     */
    public boolean find() {
        return matcher.find();
    }

    public boolean find(final int start) {
        return matcher.find(start);
    }

    /**
     * see {@link Matcher#group(int)}
     */
    public String group(final int index) {
        return newString(matcher.group(index));
    }

    /**
     * This method explicitly creates a new String instance from an already existing String. This is necessary to avoid
     * huge memory leaks in our parser. If we do regular expression matching on large Strings, the returned matches are
     * otherwise memory mapped substrings of the huge original String, therefore blocking the garbage collector from
     * removing the huge input String.
     * <p>
     * Do not change this method, even if Findbugs and other tools will report a violation for that line!
     *
     */
    @Nullable
    @SuppressFBWarnings("DM_STRING_CTOR")
    private static String newString(final String input) {
        if (input == null) {
            return null;
        }
        return new String(input); // DON'T REMOVE THE "new String" HERE!
    }

    /**
     * see {@link Matcher#groupCount()}
     */
    public int groupCount() {
        return matcher.groupCount();
    }

    /**
     * see {@link Matcher#group()}
     */
    public String group() {
        return newString(matcher.group());
    }

    /**
     * see {@link Matcher#start()}
     */
    public int start() {
        return matcher.start();
    }

    /**
     * see {@link Matcher#replaceAll(String)}
     */
    public String replaceAll(final String replacement) {
        return newString(matcher.replaceAll(replacement));
    }

    /**
     * see {@link Matcher#matches()}
     */
    public boolean matches() {
        return matcher.matches();
    }

    /**
     * see {@link Matcher#replaceFirst(String)}
     */
    public String replaceFirst(final String replacement) {
        return newString(matcher.replaceFirst(replacement));
    }
}
