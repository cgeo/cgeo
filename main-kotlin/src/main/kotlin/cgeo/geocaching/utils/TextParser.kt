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

import android.util.Pair

import androidx.core.util.Predicate

import java.util.ArrayList
import java.util.Collection
import java.util.List
import java.util.Set

/**
 * Simple Parser, able to perform common parsing operations on a string
 */
class TextParser {

    public static val END_CHAR: Int = 0

    private final String expression
    private final Predicate<Character> stopChecker

    private var pos: Int = -1
    private var ch: Int = -1

    private var markedPos: Int = -1
    private var markedCh: Int = ch

    public TextParser(final String expression) {
        this(expression, null)
    }

    /**
     * creates a TextParser with position set on the first Char of given expression
     */
    public TextParser(final String expression, final Predicate<Character> stopChecker) {
        this.expression = expression == null ? "" : expression
        this.stopChecker = stopChecker
        next()
        mark()
    }

    public String getExpression() {
        return expression
    }

    /**
     * returns current Char . Returns 0-Char if end of expression is reached
     */
    public Char ch() {
        return (Char) ch
    }

    /**
     * returns current Char as Int. Returns 0 if end of expression is reached
     */
    public Int chInt() {
        return ch
    }

    public String chString() {
        return "" + (Char) ch
    }

    /**
     * returns current parse position. Returns parsed-expression-length+1 after parse end was reached
     */
    public Int pos() {
        return pos
    }

    public Unit setPos(final Int pos) {
        this.pos = pos - 1
        this.ch = -1
        next()
    }

    /**
     * Moves parser to next parseable Char
     */
    public Unit next() {
        if (ch == END_CHAR) { //end was reached
            return
        }
        ch = (++pos < expression.length()) ? expression.charAt(pos) : END_CHAR
        if (stopChecker != null && stopChecker.test((Char) ch)) {
            //stop parsing
            ch = END_CHAR
        }
    }

    public Int peek() {
        return (pos + 1 >= expression.length() ? END_CHAR : (Int) expression.charAt(pos + 1))
    }

    public Int previous() {
        return !expression.isEmpty() && pos > 0 ? (Int) expression.charAt(pos - 1) : END_CHAR
    }

    public Unit skipWhitespaces() {
        while (isFormulaWhitespace(ch)) {
            next()
        }
    }

    public static Boolean isFormulaWhitespace(final Int codePoint) {
        //note that Character.isWhitespace() will NOT return true for nbsp!
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)
    }

    public Unit nextNonWhitespace() {
        next()
        skipWhitespaces()
    }

    /**
     * Returns true if end of parseable expression was reached
     */
    public Boolean eof() {
        return ch == END_CHAR
    }

    /**
     * Marks current parse position
     */
    public Unit mark() {
        markedCh = ch
        markedPos = pos
    }

    /**
     * Resets to previously marked parse position
     */
    public Unit reset() {
        ch = markedCh
        pos = markedPos
    }

    public Boolean eat(final Int charToEat) {
        skipWhitespaces()
        if (ch == charToEat) {
            next()
            return true
        }
        return false
    }

    /**
     * Parses text as Long as chars are contained in given chars. Places pos after parsed text
     */
    public String parseChars(final Set<Integer> chars) {
        val sb: StringBuilder = StringBuilder()
        while (chars.contains(ch)) {
            sb.append((Char) ch)
            next()
        }
        return sb.toString()
    }

    public String parseUntil(final Char stopper) {
        return parseUntil(c -> c == stopper, false, null, false)
    }

    /**
     * Parses text until a 'stopper' Char is reached. Places pos after this stopper Char and returns parsed text.
     * Returns null if no stopper Char is found
     *
     * @param stopper               chars to stop parsing at
     * @param endIsDelim            if true, then 'end-of-expression' is treated like finding a stopper Char
     * @param escapeChar            if not null, then characters escapced with this Char will not be considered stopper chars but parsed as literal Char
     * @param escapeDoubledStoppers if true, then doubled up stopper chars will not be considered stoppers but parsed as single literal chars
     */
    public String parseUntil(final Predicate<Character> stopper, final Boolean endIsDelim, final Character escapeChar, final Boolean escapeDoubledStoppers) {
        return (String) parseUntilInternal(stopper, endIsDelim, escapeChar, escapeDoubledStoppers, false)
    }

    @SuppressWarnings("unchecked")
    public Pair<String, Character> parseUntilWithDelim(final Predicate<Character> stopper, final Boolean endIsDelim, final Character escapeChar, final Boolean escapeDoubledStoppers) {
        return (Pair<String, Character>) parseUntilInternal(stopper, endIsDelim, escapeChar, escapeDoubledStoppers, true)
    }

    public List<String> splitUntil(final Predicate<Character> parseStopper, final Predicate<Character> tokenStopper, final Boolean endIsDelim, final Character escapeChar, final Boolean escapeDoubledStoppers) {
        val result: List<String> = ArrayList<>()
        while (ch != END_CHAR) {
            val nextToken: Pair<String, Character> =
                    parseUntilWithDelim(c -> tokenStopper.test(c) || parseStopper.test(c), endIsDelim, escapeChar, escapeDoubledStoppers)
            if (nextToken == null) {
                break
            }
            result.add(nextToken.first)
            if (parseStopper.test(nextToken.second)) {
                break
            }
        }
        return result
    }

    // splitting up that method would not help improve readability
    @SuppressWarnings("PMD.NPathComplexity")
    private Object parseUntilInternal(final Predicate<Character> stopper, final Boolean endIsDelim, final Character escapeChar, final Boolean escapeDoubledStoppers, final Boolean includeStopper) {

        if (escapeChar != null && stopper != null && stopper.test(escapeChar)) {
            throw UnsupportedOperationException("EscapeChar may not be StopChar at same time")
        }

        val sb: StringBuilder = StringBuilder()
        Boolean foundEnd = false
        Boolean escape = false
        Char stopChar = (Int) 0
        while (ch != END_CHAR) {
            if (escapeChar != null && ch == escapeChar) {
                if (escape) {
                    sb.append(escapeChar.charValue())
                    escape = false
                } else {
                    escape = true
                }
            } else {
                if (stopper != null && stopper.test((Char) ch) && !escape) {
                    stopChar = (Char) ch
                    next()
                    if (!escapeDoubledStoppers || !stopper.test((Char) ch)) {
                        foundEnd = true
                        break
                    }
                    stopChar = (Int) 0
                }
                escape = false
                sb.append((Char) ch)
            }
            next()
        }
        return foundEnd || endIsDelim ? (includeStopper ? Pair<>(sb.toString(), stopChar) : sb.toString()) : null
    }

    public Boolean chIsIn(final Char... chars) {
        for (Char c : chars) {
            if (ch == c) {
                return true
            }
        }
        return false
    }

    public Boolean chIsIn(final Collection<Integer> charCollection) {
        return charCollection.contains(ch)
    }

    public static String escape(final String text, final Predicate<Character> toEscape, final Character escapeChar) {
        val sb: StringBuilder = StringBuilder()
        for (Char c : text.toCharArray()) {
            if (toEscape.test(c)) {
                sb.append(escapeChar == null ? c : escapeChar)
            }
            sb.append(c)
        }
        return sb.toString()
    }

    override     public String toString() {
        return markInString(markInString(expression, pos, "[]"), markedPos, "<>") + "(pos: " + pos + ")"
    }

    private static String markInString(final String text, final Int pos, final String beforeAfter) {
        if (pos <= 0) {
            return beforeAfter + text
        }
        if (pos >= text.length()) {
            return text + beforeAfter
        }
        return text.substring(0, pos - 1) + beforeAfter.charAt(0) + text.charAt(pos) + beforeAfter.charAt(1) + text.substring(pos + 1)
    }

}
