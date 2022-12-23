package cgeo.geocaching.utils.expressions;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class ExpressionParser<T extends IExpression<T>> {

    private static final char OPEN_PAREN = '(';
    private static final char CLOSE_PAREN = ')';
    private static final char LOGIC_SEPARATOR = ';';
    private static final char CONFIG_SEPARATOR = ':';
    private static final char ESCAPE_CHAR = (char) 92; //backslash
    private static final char KEYVALUE_SEPARATOR = '=';
    private static final char OPEN_SQUARE_PAREN = '[';
    private static final char CLOSE_SQUARE_PAREN = ']';

    private static final Set<Character> ESCAPE_CHARS = new HashSet<>(Arrays.asList(
            OPEN_PAREN, CLOSE_PAREN, LOGIC_SEPARATOR, CONFIG_SEPARATOR, KEYVALUE_SEPARATOR, OPEN_SQUARE_PAREN, CLOSE_SQUARE_PAREN));
    private static final Pattern ESCAPE_CHAR_FINDER = Pattern.compile("([\\\\();:=\\[\\]])");

    private final Map<String, Supplier<T>> registeredExpressions = new HashMap<>();
    private final boolean ignoreSpecialCharsInTypeIds;

    public ExpressionParser() {
        this(false);
    }

    public ExpressionParser(final boolean ignoreSpecialCharsInTypeIds) {
        this.ignoreSpecialCharsInTypeIds = ignoreSpecialCharsInTypeIds;
    }


    public ExpressionParser<T> register(final Supplier<T> expressionCreator) {
        final String typeId = expressionCreator.get().getId();
        this.registeredExpressions.put(typeId == null ? "" :
                        this.ignoreSpecialCharsInTypeIds ? TextUtils.toComparableStringIgnoreCaseAndSpecialChars(typeId) : typeId.trim().toLowerCase(Locale.getDefault()),
                expressionCreator);
        return this;
    }

    public T create(final String config) throws ParseException {
        if (config == null) {
            return null;
        }
        return new Parser(config).parse();
    }

    public T createWithNull(final String config) {
        try {
            return create(config);
        } catch (ParseException pe) {
            Log.d("Could not parse expression '" + config + "'", pe);
            return null;
        }
    }

    public String getConfig(@NonNull final T exp) {
        final StringBuilder sb = new StringBuilder();
        writeConfig(exp, sb);
        return sb.toString();
    }


    private void writeConfig(final T exp, final StringBuilder stringBuilder) {
        final String expId = escape(exp.getId());
        final ExpressionConfig expConfig = exp.getConfig();

        final String singleConfigValue = getSingleValue(expConfig);

        if (expId.isEmpty() && singleConfigValue != null) {
            stringBuilder.append(escape(singleConfigValue));
        } else {
            stringBuilder.append(expId);
            if (!isEmpty(expConfig)) {
                stringBuilder.append(CONFIG_SEPARATOR).append(toConfig(expConfig));
            }
        }
        final List<T> children = exp.getChildren();
        if (children != null && !children.isEmpty()) {
            stringBuilder.append(OPEN_PAREN);
            boolean first = true;
            for (T child : children) {
                if (!first) {
                    stringBuilder.append(LOGIC_SEPARATOR);
                }
                first = false;
                writeConfig(child, stringBuilder);
            }
            stringBuilder.append(CLOSE_PAREN);
        }
    }

    private boolean isEmpty(final ExpressionConfig config) {
        return config == null || config.isEmpty();
    }

    private String getSingleValue(final ExpressionConfig config) {
        return config == null ? null : config.getSingleValue();
    }

    public static int parseToNextDelim(final String text, final int startIdx, final Set<Character> endChars, final StringBuilder result) {
        int idx = startIdx;
        boolean nextCharIsEscaped = false;
        while (true) {
            if (idx >= text.length()) {
                break;
            }
            final char c = text.charAt(idx);
            if (c == ESCAPE_CHAR) {
                if (nextCharIsEscaped) {
                    result.append(ESCAPE_CHAR);
                }
                nextCharIsEscaped = !nextCharIsEscaped;
            } else if (endChars.contains(c) && !nextCharIsEscaped) {
                //the delim char has been reached
                break;
            } else {
                result.append(c);
                nextCharIsEscaped = false;
            }
            idx++;
        }
        return idx;
    }

    /**
     * parses a configuration from string 'text', starting at position 'idx'. Config read is filled into 'result'.
     * Method will ALWAYS fill in at least an empty List for key 'null' into result
     */
    public static int parseConfiguration(final String text, final int startIdx, @NonNull final Map<String, List<String>> result) {
        result.put(null, new ArrayList<>());
        if (text == null) {
            return 0;
        }
        if (startIdx >= text.length()) {
            return text.length();
        }
        int idx = startIdx;
        String currKey = null;
        while (true) {
            final StringBuilder nextToken = new StringBuilder();
            idx = parseToNextDelim(text, idx, ESCAPE_CHARS, nextToken);
            if (idx < text.length() && text.charAt(idx) == KEYVALUE_SEPARATOR) {
                currKey = nextToken.toString();
            } else {
                List<String> values = result.get(currKey);
                if (values == null) {
                    values = new ArrayList<>();
                    result.put(currKey, values);
                }
                values.add(nextToken.toString());
                currKey = null;
                if (idx >= text.length() || text.charAt(idx) != CONFIG_SEPARATOR) {
                    break;
                }
            }
            idx++;
        }
        return idx;
    }

    /**
     * Escapes all characters with a backslash (\) which could have a special meaning in context of expressions.
     * Those are: ()=:[]; and the backslash \ itself
     *
     * @param raw string to escape
     * @return escaped string
     */
    public static String escape(final String raw) {
        if (raw == null) {
            return "";
        }
        return ESCAPE_CHAR_FINDER.matcher(raw).replaceAll("" + ESCAPE_CHAR + ESCAPE_CHAR + "$1");
    }

    public static String toConfig(final ExpressionConfig config) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (config.get(null) != null) {
            for (String value : config.get(null)) {
                if (!first) {
                    sb.append(CONFIG_SEPARATOR);
                }
                first = false;
                sb.append(escape(value));
            }
        }
        for (Map.Entry<String, List<String>> entry : config.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (!first) {
                    sb.append(CONFIG_SEPARATOR);
                }
                first = false;
                sb.append(escape(entry.getKey()));
                sb.append(KEYVALUE_SEPARATOR);
                sb.append(escape(value));
            }
        }
        return sb.toString();
    }


    private class Parser {

        private final Set<Character> endChars = new HashSet<>(Arrays.asList(CONFIG_SEPARATOR, LOGIC_SEPARATOR, OPEN_PAREN, CLOSE_PAREN));

        private final String config;
        private int idx = 0;

        Parser(final String config) {
            this.config = config;
        }

        @NonNull
        public T parse() throws ParseException {
            final T result = parseNext();
            moveToNextToken();
            if (config.length() != idx) {
                throwParseException("Unexpected leftover in expression");
            }
            return result;
        }

        /**
         * Parses next expression starting from idx and leaving idx at next token AFTER expression
         */
        private T parseNext() throws ParseException {
            checkEndOfExpression();

            final T exp = parseNextRawExpression();

            if (!currentCharIs(OPEN_PAREN)) {
                return exp;
            }

            idx++;
            while (true) {
                exp.addChild(parseNext());
                final boolean isClosingParen = currentCharIs(CLOSE_PAREN);
                final boolean isSeparator = currentCharIs(LOGIC_SEPARATOR);
                if (!isClosingParen && !isSeparator) {
                    checkEndOfExpression();
                    throwParseException("Expected '" + CLOSE_PAREN + "' or '" + LOGIC_SEPARATOR + "' but found '" + config.charAt(idx) + "'");
                }
                idx++;
                if (isClosingParen) {
                    return exp;
                }
            }
        }

        @NonNull
        private T parseNextRawExpression() throws ParseException {
            final String typeId = ignoreSpecialCharsInTypeIds ?
                    TextUtils.toComparableStringIgnoreCaseAndSpecialChars(parseToNextDelim().trim()) :
                    parseToNextDelim().trim().toLowerCase(Locale.getDefault());

            final ExpressionConfig typeConfig = new ExpressionConfig();
            if (currentCharIs(CONFIG_SEPARATOR, false)) {
                idx++;
                idx = parseConfiguration(config, idx, typeConfig);
            }
            if (typeId.isEmpty() && isEmpty(typeConfig)) {
                throwParseException("Expression expected, but none was found");
            }

            final T expression;
            if (registeredExpressions.containsKey(typeId)) {
                expression = registeredExpressions.get(typeId).get();
                if (!isEmpty(typeConfig)) {
                    expression.setConfig(typeConfig);
                }
            } else if (registeredExpressions.containsKey("") && isEmpty(typeConfig)) {
                expression = registeredExpressions.get("").get();
                typeConfig.put(null, Collections.singletonList(typeId));
                expression.setConfig(typeConfig);
            } else {
                expression = null; //make compiler happy, value will never be used
                throwParseException("No expression type found for id '" + typeId + "' and no default expression could be applied");
            }

            return Objects.requireNonNull(expression);
        }

        private String parseToNextDelim() {
            final StringBuilder result = new StringBuilder();

            idx = ExpressionParser.parseToNextDelim(config, idx, endChars, result);

            return result.toString();
        }


        private void moveToNextToken() {
            while (idx < config.length() && Character.isWhitespace(config.charAt(idx))) {
                idx++;
            }
        }

        private boolean currentCharIs(final char c) {
            return currentCharIs(c, true);
        }

        private boolean currentCharIs(final char c, final boolean moveToNextToken) {
            if (moveToNextToken) {
                moveToNextToken();
            }
            return idx < config.length() && config.charAt(idx) == c;
        }

        private void checkEndOfExpression() throws ParseException {
            moveToNextToken();
            if (idx >= config.length()) {
                throwParseException("Unexpected end of expression");
            }
        }

        private void throwParseException(final String message) throws ParseException {
            String markedConfig = config;
            if (idx >= config.length()) {
                markedConfig += "[]";
            } else {
                markedConfig = markedConfig.substring(0, idx) + "[" + markedConfig.charAt(idx) + "]" + markedConfig.substring(idx + 1);
            }
            throw new ParseException("Problem parsing '" + markedConfig + "' (pos marked with []: " + idx + "): " + message, idx);
        }

    }

}
