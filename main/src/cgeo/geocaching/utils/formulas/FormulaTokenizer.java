package cgeo.geocaching.utils.formulas;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class FormulaTokenizer {

    public enum Token { ID, NUMERIC, TEXT, SYMBOL, PAREN_OPEN, PAREN_CLOSE, PAREN_SEPARATOR, END }

    private static final TokenData TOKENDATA_END = new TokenData(Token.END, null);
    private static final char ESCAPE_CHAR = (char) 92; //backslash


    private static final Set<Character> SYMBOL_CHARS = new HashSet<>(Arrays.asList(
        '-', '+', '*', '/', '%', '&', '_', '#', '!', '|', '$', ':', '$'));

    private static final Set<Character> NUMERIC_CHARS = new HashSet<>();
    private static final Set<Character> ID_CHARS = new HashSet<>();
    private static final Set<Character> ID_START_CHARS = new HashSet<>();

    private static final Map<Token, Set<Token>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        for (char c = 'a' ; c <= 'z' ; c++) {
            ID_CHARS.add(c);
            ID_START_CHARS.add(c);
        }
        for (char c = 'A' ; c <= 'Z' ; c++) {
            ID_CHARS.add(c);
            ID_START_CHARS.add(c);
        }
        for (char c = '0' ; c <= '9' ; c++) {
            ID_CHARS.add(c);
            NUMERIC_CHARS.add(c);
        }

        ALLOWED_TRANSITIONS.put(null, new HashSet<>(Arrays.asList(Token.ID, Token.TEXT, Token.NUMERIC, Token.PAREN_OPEN, Token.SYMBOL)));
        ALLOWED_TRANSITIONS.put(Token.ID, new HashSet<>(Arrays.asList(Token.PAREN_OPEN, Token.PAREN_SEPARATOR, Token.PAREN_CLOSE, Token.SYMBOL, Token.ID, Token.END)));
        ALLOWED_TRANSITIONS.put(Token.TEXT, new HashSet<>(Arrays.asList(Token.ID, Token.SYMBOL, Token.PAREN_SEPARATOR, Token.PAREN_CLOSE, Token.END)));
        ALLOWED_TRANSITIONS.put(Token.NUMERIC, new HashSet<>(Arrays.asList(Token.ID, Token.SYMBOL, Token.PAREN_SEPARATOR, Token.PAREN_CLOSE, Token.END)));
        ALLOWED_TRANSITIONS.put(Token.SYMBOL, new HashSet<>(Arrays.asList(Token.ID, Token.NUMERIC, Token.TEXT, Token.PAREN_OPEN, Token.PAREN_SEPARATOR, Token.PAREN_CLOSE)));
        ALLOWED_TRANSITIONS.put(Token.PAREN_OPEN, new HashSet<>(Arrays.asList(Token.PAREN_OPEN, Token.ID, Token.SYMBOL, Token.NUMERIC, Token.TEXT)));
        ALLOWED_TRANSITIONS.put(Token.PAREN_SEPARATOR, ALLOWED_TRANSITIONS.get(Token.PAREN_OPEN));
        ALLOWED_TRANSITIONS.put(Token.PAREN_CLOSE, new HashSet<>(Arrays.asList(Token.END, Token.SYMBOL, Token.ID, Token.PAREN_SEPARATOR, Token.PAREN_CLOSE)));
    }

    private final String formula;
    private int idx;
    private TokenData currTokenData = null;

    public static class TokenData {
        public final Token token;
        public final String text;

        public TokenData(final Token token, final String text) {
            this.token = token;
            this.text = text;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof TokenData)) {
                return false;
            }
            final TokenData other = (TokenData) o;
            return other.token == this.token && Objects.equals(other.text, this.text);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.token) + Objects.hashCode(this.text);
        }

        @NonNull
        @Override
        public String toString() {
            return this.token + "[" + this.text + "]";
        }
    }

    public String getFormula() {
        return formula;
    }

    public int getParseIndex() {
        return idx;
    }

    public TokenData getCurrentToken() {
        return currTokenData;
    }

    public FormulaTokenizer(final String formula) {
        this.formula = formula == null ? "" : formula;
        this.idx = 0;
    }

    public TokenData parseNextToken() throws FormulaParseException {
        final TokenData data = parseNextTokenInternal();
        checkTransition(currTokenData, data);
        currTokenData = data;
        return data;
    }

    private TokenData parseNextTokenInternal() throws FormulaParseException {
        if (!moveToNextToken()) {
            return TOKENDATA_END;
        }
        final char c = currChar();
        if (ID_START_CHARS.contains(c)) {
            // parse as ID
            return new TokenData(Token.ID, parseContained(ID_CHARS));
        }
        if (startsNumeric(c)) {
            return parseNextNumeric(c);
        }
        if (SYMBOL_CHARS.contains(c)) {
            // parse as Symbol
            return new TokenData(Token.SYMBOL, parseContained(SYMBOL_CHARS));
        }
        if (c == '(') {
            nextChar();
            return new TokenData(Token.PAREN_OPEN, "(");
        }
        if (c == ')') {
            nextChar();
            return new TokenData(Token.PAREN_CLOSE, ")");
        }
        if (c == ';') {
            nextChar();
            return new TokenData(Token.PAREN_SEPARATOR, ";");
        }
        if (c == '"' || c == '\'') {
            return parseNextText(c);
        }
        throwParseException("Unknown token starting with '" + c + "'");
        return TOKENDATA_END;
    }

    private boolean startsNumeric(final char c) {
        return NUMERIC_CHARS.contains(c) ||
            (c == '-' && NUMERIC_CHARS.contains(peekNextChar()) && isAllowedTransition(currTokenData, Token.NUMERIC));
    }

    @NotNull
    private TokenData parseNextText(final char c) throws FormulaParseException {
        char tc = nextChar();
        final StringBuilder text = new StringBuilder();
        boolean nextCharIsEscaped = false;
        while (true) {
            if (tc == '\0') {
                break;
            }
            if (tc == ESCAPE_CHAR) {
                if (nextCharIsEscaped) {
                    text.append(ESCAPE_CHAR);
                }
                nextCharIsEscaped = !nextCharIsEscaped;
            } else if (tc == c && !nextCharIsEscaped) {
                //the delim char has been reached
                break;
            } else {
                text.append(tc);
                nextCharIsEscaped = false;
            }
            tc = nextChar();
        }
        if (tc != c) {
            throwParseException("Unclosed string literal '" + text + "'");
        }
        nextChar();
        return new TokenData(Token.TEXT, text.toString());
    }

    @NotNull
    private TokenData parseNextNumeric(final char c) {
        //parse as Number
        final StringBuilder text = new StringBuilder("" + c);
        nextChar();
        parseContained(NUMERIC_CHARS, text);
        if (currChar() == '.') {
            text.append('.');
            nextChar();
            parseContained(NUMERIC_CHARS, text);
        }
        return new TokenData(Token.NUMERIC, text.toString());
    }

    private String parseContained(final Set<Character> contained) {
        return parseContained(contained, new StringBuilder());
    }

    private String parseContained(final Set<Character> contained, final StringBuilder text) {

        char c = currChar();
        while (contained.contains(c)) {
            text.append(c);
            c = nextChar();
        }
        return text.toString();
    }

    private char currChar() {
        return idx < formula.length() ? formula.charAt(idx) : '\0';
    }

    private char nextChar() {
        if (idx < formula.length()) {
            idx++;
        }
        return currChar();
    }

    private char peekNextChar() {
        if (idx + 1 < formula.length()) {
            return formula.charAt(idx + 1);
        }
        return '\0';
    }

    private boolean moveToNextToken() {
        while (idx < formula.length() && Character.isWhitespace(formula.charAt(idx))) {
            idx++;
        }
        return idx < formula.length();
    }

    public void throwParseException(final String message) throws FormulaParseException {
        throw new FormulaParseException(this, message);
    }

    private void checkTransition(final TokenData from, final TokenData to) throws FormulaParseException {
        if (!isAllowedTransition(from, to == null ? Token.END : to.token)) {
            throwParseException("Transition not allowed: " + from + " -> " + to);
        }
    }

    public boolean isAllowedTransition(final TokenData from, final Token toToken) {
        return Objects.requireNonNull(ALLOWED_TRANSITIONS.get(from == null ? null : from.token)).contains(toToken);
    }

}
