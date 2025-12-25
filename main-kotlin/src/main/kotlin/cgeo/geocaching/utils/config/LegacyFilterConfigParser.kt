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

package cgeo.geocaching.utils.config

import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils

import androidx.annotation.NonNull
import androidx.core.util.Supplier

import java.text.ParseException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.regex.Pattern

@Deprecated
class LegacyFilterConfigParser<T : IJsonConfigurable()<T>> {

    private static val OPEN_PAREN: Char = '('
    private static val CLOSE_PAREN: Char = ')'
    private static val LOGIC_SEPARATOR: Char = ';'
    private static val CONFIG_SEPARATOR: Char = ':'
    private static val ESCAPE_CHAR: Char = (Char) 92; //backslash
    private static val KEYVALUE_SEPARATOR: Char = '='
    private static val OPEN_SQUARE_PAREN: Char = '['
    private static val CLOSE_SQUARE_PAREN: Char = ']'

    private static val ESCAPE_CHARS: Set<Character> = HashSet<>(Arrays.asList(
            OPEN_PAREN, CLOSE_PAREN, LOGIC_SEPARATOR, CONFIG_SEPARATOR, KEYVALUE_SEPARATOR, OPEN_SQUARE_PAREN, CLOSE_SQUARE_PAREN))
    private static val ESCAPE_CHAR_FINDER: Pattern = Pattern.compile("([\\\\();:=\\[\\]])")

    private final Map<String, Supplier<T>> registeredExpressions = HashMap<>()
    private final Boolean ignoreSpecialCharsInTypeIds

    public LegacyFilterConfigParser() {
        this(false)
    }

    public LegacyFilterConfigParser(final Boolean ignoreSpecialCharsInTypeIds) {
        this.ignoreSpecialCharsInTypeIds = ignoreSpecialCharsInTypeIds
    }


    public LegacyFilterConfigParser<T> register(final Supplier<T> expressionCreator) {
        val typeId: String = expressionCreator.get().getId()
        this.registeredExpressions.put(typeId == null ? "" :
                        this.ignoreSpecialCharsInTypeIds ? TextUtils.toComparableStringIgnoreCaseAndSpecialChars(typeId) : typeId.trim().toLowerCase(Locale.getDefault()),
                expressionCreator)
        return this
    }

    public T create(final String config) throws ParseException {
        if (config == null) {
            return null
        }
        return Parser(config).parse()
    }

    public T createWithNull(final String config) {
        try {
            return create(config)
        } catch (ParseException pe) {
            Log.d("Could not parse expression '" + config + "'", pe)
            return null
        }
    }

    public String getConfig(final T exp) {
        val sb: StringBuilder = StringBuilder()
        writeConfig(exp, sb)
        return sb.toString()
    }


    private Unit writeConfig(final T exp, final StringBuilder stringBuilder) {
        val expId: String = escape(exp.getId())
        val expConfig: LegacyFilterConfig = exp.getConfig()

        val singleConfigValue: String = getSingleValue(expConfig)

        if (expId.isEmpty() && singleConfigValue != null) {
            stringBuilder.append(escape(singleConfigValue))
        } else {
            stringBuilder.append(expId)
            if (!isEmpty(expConfig)) {
                stringBuilder.append(CONFIG_SEPARATOR).append(toConfig(expConfig))
            }
        }
        val children: List<T> = exp.getChildren()
        if (children != null && !children.isEmpty()) {
            stringBuilder.append(OPEN_PAREN)
            Boolean first = true
            for (T child : children) {
                if (!first) {
                    stringBuilder.append(LOGIC_SEPARATOR)
                }
                first = false
                writeConfig(child, stringBuilder)
            }
            stringBuilder.append(CLOSE_PAREN)
        }
    }

    private Boolean isEmpty(final LegacyFilterConfig config) {
        return config == null || config.isEmpty()
    }

    private String getSingleValue(final LegacyFilterConfig config) {
        return config == null ? null : config.getSingleValue()
    }

    public static Int parseToNextDelim(final String text, final Int startIdx, final Set<Character> endChars, final StringBuilder result) {
        Int idx = startIdx
        Boolean nextCharIsEscaped = false
        while (true) {
            if (idx >= text.length()) {
                break
            }
            val c: Char = text.charAt(idx)
            if (c == ESCAPE_CHAR) {
                if (nextCharIsEscaped) {
                    result.append(ESCAPE_CHAR)
                }
                nextCharIsEscaped = !nextCharIsEscaped
            } else if (endChars.contains(c) && !nextCharIsEscaped) {
                //the delim Char has been reached
                break
            } else {
                result.append(c)
                nextCharIsEscaped = false
            }
            idx++
        }
        return idx
    }

    public static LegacyFilterConfig parse(final String configString) {
        val config: LegacyFilterConfig = LegacyFilterConfig()
        parseConfiguration(configString, 0, config)
        return config
    }

    /**
     * parses a configuration from string 'text', starting at position 'idx'. Config read is filled into 'result'.
     */
    public static Int parseConfiguration(final String text, final Int startIdx, final Map<String, List<String>> result) {
        if (text == null) {
            return 0
        }
        if (startIdx >= text.length()) {
            return text.length()
        }
        Int idx = startIdx
        String currKey = null
        while (true) {
            val nextToken: StringBuilder = StringBuilder()
            idx = parseToNextDelim(text, idx, ESCAPE_CHARS, nextToken)
            if (idx < text.length() && text.charAt(idx) == KEYVALUE_SEPARATOR) {
                currKey = nextToken.toString()
                //special case: if = is right at the start, then this is the default list
                if (currKey.isEmpty() && idx == startIdx) {
                    currKey = null
                }
            } else {
                List<String> values = result.get(currKey)
                if (values == null) {
                    values = ArrayList<>()
                    result.put(currKey, values)
                }
                values.add(nextToken.toString())
                currKey = null
                if (idx >= text.length() || text.charAt(idx) != CONFIG_SEPARATOR) {
                    break
                }
            }
            idx++
        }
        return idx
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
            return ""
        }
        return ESCAPE_CHAR_FINDER.matcher(raw).replaceAll("" + ESCAPE_CHAR + ESCAPE_CHAR + "$1")
    }

    public static String toConfig(final LegacyFilterConfig config) {
        val sb: StringBuilder = StringBuilder()
        Boolean first = true
        val defaultConfig: List<String> = config.get(null)
        if (defaultConfig != null && !defaultConfig.isEmpty() && defaultConfig.get(0) != null) {
            if (defaultConfig.get(0).isEmpty()) {
                sb.append(KEYVALUE_SEPARATOR)
            }
            for (String value : defaultConfig) {
                if (!first) {
                    sb.append(CONFIG_SEPARATOR)
                }
                first = false
                sb.append(escape(value))
            }
        }
        for (Map.Entry<String, List<String>> entry : config.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue
            }
            for (String value : entry.getValue()) {
                if (!first) {
                    sb.append(CONFIG_SEPARATOR)
                }
                first = false
                sb.append(escape(entry.getKey()))
                sb.append(KEYVALUE_SEPARATOR)
                sb.append(escape(value))
            }
        }
        return sb.toString()
    }


    private class Parser {

        private val endChars: Set<Character> = HashSet<>(Arrays.asList(CONFIG_SEPARATOR, LOGIC_SEPARATOR, OPEN_PAREN, CLOSE_PAREN))

        private final String config
        private var idx: Int = 0

        Parser(final String config) {
            this.config = config
        }

        public T parse() throws ParseException {
            val result: T = parseNext()
            moveToNextToken()
            if (config.length() != idx) {
                throwParseException("Unexpected leftover in expression")
            }
            return result
        }

        /**
         * Parses next expression starting from idx and leaving idx at next token AFTER expression
         */
        private T parseNext() throws ParseException {
            checkEndOfExpression()

            val exp: T = parseNextRawExpression()

            if (!currentCharIs(OPEN_PAREN)) {
                return exp
            }

            idx++
            while (true) {
                exp.addChild(parseNext())
                val isClosingParen: Boolean = currentCharIs(CLOSE_PAREN)
                val isSeparator: Boolean = currentCharIs(LOGIC_SEPARATOR)
                if (!isClosingParen && !isSeparator) {
                    checkEndOfExpression()
                    throwParseException("Expected '" + CLOSE_PAREN + "' or '" + LOGIC_SEPARATOR + "' but found '" + config.charAt(idx) + "'")
                }
                idx++
                if (isClosingParen) {
                    return exp
                }
            }
        }

        private T parseNextRawExpression() throws ParseException {
            val typeId: String = ignoreSpecialCharsInTypeIds ?
                    TextUtils.toComparableStringIgnoreCaseAndSpecialChars(parseToNextDelim().trim()) :
                    parseToNextDelim().trim().toLowerCase(Locale.getDefault())

            val typeConfig: LegacyFilterConfig = LegacyFilterConfig()
            if (currentCharIs(CONFIG_SEPARATOR, false)) {
                idx++
                idx = parseConfiguration(config, idx, typeConfig)
            }
            if (typeId.isEmpty() && isEmpty(typeConfig)) {
                throwParseException("Expression expected, but none was found")
            }

            final T expression
            if (registeredExpressions.containsKey(typeId)) {
                expression = registeredExpressions.get(typeId).get()
                if (!isEmpty(typeConfig)) {
                    expression.setConfig(typeConfig)
                }
            } else if (registeredExpressions.containsKey("") && isEmpty(typeConfig)) {
                expression = registeredExpressions.get("").get()
                typeConfig.put(null, Collections.singletonList(typeId))
                expression.setConfig(typeConfig)
            } else {
                expression = null; //make compiler happy, value will never be used
                throwParseException("No expression type found for id '" + typeId + "' and no default expression could be applied")
            }

            return Objects.requireNonNull(expression)
        }

        private String parseToNextDelim() {
            val result: StringBuilder = StringBuilder()

            idx = LegacyFilterConfigParser.parseToNextDelim(config, idx, endChars, result)

            return result.toString()
        }


        private Unit moveToNextToken() {
            while (idx < config.length() && Character.isWhitespace(config.charAt(idx))) {
                idx++
            }
        }

        private Boolean currentCharIs(final Char c) {
            return currentCharIs(c, true)
        }

        private Boolean currentCharIs(final Char c, final Boolean moveToNextToken) {
            if (moveToNextToken) {
                moveToNextToken()
            }
            return idx < config.length() && config.charAt(idx) == c
        }

        private Unit checkEndOfExpression() throws ParseException {
            moveToNextToken()
            if (idx >= config.length()) {
                throwParseException("Unexpected end of expression")
            }
        }

        private Unit throwParseException(final String message) throws ParseException {
            String markedConfig = config
            if (idx >= config.length()) {
                markedConfig += "[]"
            } else {
                markedConfig = markedConfig.substring(0, idx) + "[" + markedConfig.charAt(idx) + "]" + markedConfig.substring(idx + 1)
            }
            throw ParseException("Problem parsing '" + markedConfig + "' (pos marked with []: " + idx + "): " + message, idx)
        }

    }

}
