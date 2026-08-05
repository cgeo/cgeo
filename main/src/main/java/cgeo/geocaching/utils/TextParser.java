package cgeo.geocaching.utils;

import android.util.Pair;

import androidx.core.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Simple Parser, able to perform common parsing operations on a string
 */
public final class TextParser {

    public static final int END_CHAR = 0;

    private final String expression;
    private final Predicate<Character> stopChecker;

    private int pos = -1;
    private int ch = -1;

    private int markedPos = -1;
    private int markedCh = ch;

    public TextParser(final String expression) {
        this(expression, null);
    }

    /**
     * creates a new TextParser with position set on the first char of given expression
     */
    public TextParser(final String expression, final Predicate<Character> stopChecker) {
        this.expression = expression == null ? "" : expression;
        this.stopChecker = stopChecker;
        next();
        mark();
    }

    public String getExpression() {
        return expression;
    }

    /**
     * returns current char . Returns 0-char if end of expression is reached
     */
    public char ch() {
        return (char) ch;
    }

    /**
     * returns current char as int. Returns 0 if end of expression is reached
     */
    public int chInt() {
        return ch;
    }

    public String chString() {
        return "" + (char) ch;
    }

    /**
     * returns current parse position. Returns parsed-expression-length+1 after parse end was reached
     */
    public int pos() {
        return pos;
    }

    public void setPos(final int pos) {
        this.pos = pos - 1;
        this.ch = -1;
        next();
    }

    /**
     * Moves parser to next parseable char
     */
    public void next() {
        if (ch == END_CHAR) { //end was reached
            return;
        }
        ch = (++pos < expression.length()) ? expression.charAt(pos) : END_CHAR;
        if (stopChecker != null && stopChecker.test((char) ch)) {
            //stop parsing
            ch = END_CHAR;
        }
    }

    public int peek() {
        return (pos + 1 >= expression.length() ? END_CHAR : (int) expression.charAt(pos + 1));
    }

    public int previous() {
        return !expression.isEmpty() && pos > 0 ? (int) expression.charAt(pos - 1) : END_CHAR;
    }

    public void skipWhitespaces() {
        while (isFormulaWhitespace(ch)) {
            next();
        }
    }

    public static boolean isFormulaWhitespace(final int codePoint) {
        //note that Character.isWhitespace() will NOT return true for nbsp!
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    public void nextNonWhitespace() {
        next();
        skipWhitespaces();
    }

    /**
     * Returns true if end of parseable expression was reached
     */
    public boolean eof() {
        return ch == END_CHAR;
    }

    /**
     * Marks current parse position
     */
    public void mark() {
        markedCh = ch;
        markedPos = pos;
    }

    /**
     * Resets to previously marked parse position
     */
    public void reset() {
        ch = markedCh;
        pos = markedPos;
    }

    public boolean eat(final int charToEat) {
        skipWhitespaces();
        if (ch == charToEat) {
            next();
            return true;
        }
        return false;
    }

    /**
     * Parses text as long as chars are contained in given chars. Places pos after parsed text
     */
    public String parseChars(final Set<Integer> chars) {
        final StringBuilder sb = new StringBuilder();
        while (chars.contains(ch)) {
            sb.append((char) ch);
            next();
        }
        return sb.toString();
    }

    public String parseUntil(final char stopper) {
        return parseUntil(c -> c == stopper, false, null, false);
    }

    /**
     * Parses text until a 'stopper' char is reached. Places pos after this stopper char and returns parsed text.
     * Returns null if no stopper char is found
     *
     * @param stopper               chars to stop parsing at
     * @param endIsDelim            if true, then 'end-of-expression' is treated like finding a stopper char
     * @param escapeChar            if not null, then characters escapced with this char will not be considered stopper chars but parsed as literal char
     * @param escapeDoubledStoppers if true, then doubled up stopper chars will not be considered stoppers but parsed as single literal chars
     */
    public String parseUntil(final Predicate<Character> stopper, final boolean endIsDelim, final Character escapeChar, final boolean escapeDoubledStoppers) {
        return (String) parseUntilInternal(stopper, endIsDelim, escapeChar, escapeDoubledStoppers, false);
    }

    @SuppressWarnings("unchecked")
    public Pair<String, Character> parseUntilWithDelim(final Predicate<Character> stopper, final boolean endIsDelim, final Character escapeChar, final boolean escapeDoubledStoppers) {
        return (Pair<String, Character>) parseUntilInternal(stopper, endIsDelim, escapeChar, escapeDoubledStoppers, true);
    }

    public List<String> splitUntil(final Predicate<Character> parseStopper, final Predicate<Character> tokenStopper, final boolean endIsDelim, final Character escapeChar, final boolean escapeDoubledStoppers) {
        final List<String> result = new ArrayList<>();
        while (ch != END_CHAR) {
            final Pair<String, Character> nextToken =
                    parseUntilWithDelim(c -> tokenStopper.test(c) || parseStopper.test(c), endIsDelim, escapeChar, escapeDoubledStoppers);
            if (nextToken == null) {
                break;
            }
            result.add(nextToken.first);
            if (parseStopper.test(nextToken.second)) {
                break;
            }
        }
        return result;
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings("PMD.NPathComplexity")
    private Object parseUntilInternal(final Predicate<Character> stopper, final boolean endIsDelim, final Character escapeChar, final boolean escapeDoubledStoppers, final boolean includeStopper) {

        if (escapeChar != null && stopper != null && stopper.test(escapeChar)) {
            throw new UnsupportedOperationException("EscapeChar may not be StopChar at same time");
        }

        final StringBuilder sb = new StringBuilder();
        boolean foundEnd = false;
        boolean escape = false;
        char stopChar = (int) 0;
        while (ch != END_CHAR) {
            if (escapeChar != null && ch == escapeChar) {
                if (escape) {
                    sb.append(escapeChar.charValue());
                    escape = false;
                } else {
                    escape = true;
                }
            } else {
                if (stopper != null && stopper.test((char) ch) && !escape) {
                    stopChar = (char) ch;
                    next();
                    if (!escapeDoubledStoppers || !stopper.test((char) ch)) {
                        foundEnd = true;
                        break;
                    }
                    stopChar = (int) 0;
                }
                escape = false;
                sb.append((char) ch);
            }
            next();
        }
        return foundEnd || endIsDelim ? (includeStopper ? new Pair<>(sb.toString(), stopChar) : sb.toString()) : null;
    }

    public boolean chIsIn(final char... chars) {
        for (char c : chars) {
            if (ch == c) {
                return true;
            }
        }
        return false;
    }

    public boolean chIsIn(final Collection<Integer> charCollection) {
        return charCollection.contains(ch);
    }

    public static String escape(final String text, final Predicate<Character> toEscape, final Character escapeChar) {
        final StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (toEscape.test(c)) {
                sb.append(escapeChar == null ? c : escapeChar);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return markInString(markInString(expression, pos, "[]"), markedPos, "<>") + "(pos: " + pos + ")";
    }

    private static String markInString(final String text, final int pos, final String beforeAfter) {
        if (pos <= 0) {
            return beforeAfter + text;
        }
        if (pos >= text.length()) {
            return text + beforeAfter;
        }
        return text.substring(0, pos - 1) + beforeAfter.charAt(0) + text.charAt(pos) + beforeAfter.charAt(1) + text.substring(pos + 1);
    }

}
