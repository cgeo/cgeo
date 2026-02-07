package cgeo.geocaching.utils.formulas;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The RangeParser class is responsible for parsing range expressions and creating a list of values.
 * It supports both numeric ranges (e.g., "1-5") and variable ranges (e.g., "$A-$D", "$nA1-$nA12").
 * <p>
 * Example usage:
 * <pre>
 *     // Parse a numeric range
 *     List<Value> numericRange = RangeParser.createFromString("1-5");
 *     // Output: [1, 2, 3, 4, 5]
 *
 *     // Parse a variable range
 *     List<Value> variableRange = RangeParser.createFromString("$A-$D");
 *     // Output: [$A, $B, $C, $D]
 * </pre>
 */
class RangeParser {


    private RangeParser() {
        //no instance
    }

    static class VariableComponents {
        final String prefix; // prefix before char part (can be empty)
        final Character charPart; // Character portion, if it is a letter range (null for numeric-only variables)
        final String numericPart; // Numeric portion, if it is a numeric range
        final boolean hasNumeric;

        private VariableComponents(@NonNull final String prefix, @Nullable final Character letterPart, @NonNull final String numericPart, final boolean hasNumeric) {
            this.prefix = prefix;
            this.charPart = letterPart;
            this.numericPart = numericPart;
            this.hasNumeric = hasNumeric;
        }
        
        public static VariableComponents parseVariableComponents(final String var) {
            String remaining = var;

            final boolean hasDollar = remaining.startsWith("$");
            if (hasDollar) {
                remaining = remaining.substring(1);
            }

            // Separate letters from digits
            final StringBuilder letters = new StringBuilder();
            final StringBuilder digits = new StringBuilder();

            for (final char ch : remaining.toCharArray()) {
                if (Character.isLetter(ch)) {
                    if (digits.length() == 0) {
                        letters.append(ch);
                    } else {
                        // Letters after digits - invalid
                        return null;
                    }
                } else if (Character.isDigit(ch)) {
                    digits.append(ch);
                }
            }

            final String letterString = letters.toString();
            // Variable must have at least one letter
            if (letterString.isEmpty()) {
                return null;
            }

            // Multi-letter-variable must start with a $
            if (!hasDollar && letterString.length() > 1) {
                return null;
            }
            return VariableComponents.createVarComponents(letterString, digits.toString());
        }

        public String getName() {
            final StringBuilder nameBuilder = new StringBuilder(prefix);
            if (charPart != null) {
                nameBuilder.append(charPart);
            }
            if (hasNumeric) {
                nameBuilder.append(numericPart);
            }
            return nameBuilder.toString();
        }

        @Nullable

        private static boolean areCompatible(final VariableComponents start, final VariableComponents end) {
            // Must have same (multi-letter) prefix
            if (!start.prefix.equals(end.prefix)) {
                return false;
            }

            // If one has numeric part, both must have numeric part
            if (start.hasNumeric != end.hasNumeric) {
                return false;
            }

            // For letter-only variables
            if (start.hasNumeric) {
                return true;
            }

            // Must be single letter with same case, or same multi-letter prefix
            if (start.charPart != null) {
                final char startChar = start.charPart;
                final char endChar = end.charPart;
                // Must be same case
                return (Character.isUpperCase(startChar) && Character.isUpperCase(endChar)) ||
                        (Character.isLowerCase(startChar) && Character.isLowerCase(endChar));
            }

            // Multi-letter variables cannot be ranged
            return false;
        }

        private static int calculateRangeSize(final VariableComponents start, final VariableComponents end) {
            if (start.hasNumeric) {
                // Numeric range
                final int startNum = Integer.parseInt(start.numericPart);
                final int endNum = Integer.parseInt(end.numericPart);
                return (endNum - startNum) + 1;
            } else if (start.charPart != null) {
                // Single letter range
                return (end.charPart - start.charPart) + 1;
            }
            return 0;
        }

        private static String incrementVariable(final VariableComponents baseVar, final int offset, final String paddingFormat) {
            final StringBuilder varName = new StringBuilder(baseVar.prefix);
            if (offset == 0) {
                if (baseVar.hasNumeric) {
                    varName.append(baseVar.numericPart);
                } else if (baseVar.charPart != null) {
                    varName.append(baseVar.charPart);
                }
                return varName.toString();
            }

            if (baseVar.hasNumeric) {
                // Increment numeric part
                final int baseNum = Integer.parseInt(baseVar.numericPart);
                final int newNum = baseNum + offset;

                // Use the provided padding format if available
                // Fallback to base value padding (defensive programming, normally paddingFormat is set)
                final String format;
                if (paddingFormat != null) {
                    format = paddingFormat;
                } else if (baseVar.numericPart.startsWith("0") && baseVar.numericPart.length() > 1) {
                    format = "%0" + baseVar.numericPart.length() + "d";
                } else {
                    format = "%d";
                }

                varName.append(String.format(format, newNum));
            } else if (baseVar.charPart != null) {
                // Increment letter
                final char newChar = (char) (baseVar.charPart + offset);
                varName.append(newChar);
            }
            return varName.toString();
        }

        @Nullable
        private static VariableComponents createVarComponents(@NonNull final String letterString, @NonNull final String digitString) {
            if (letterString.isEmpty() && digitString.isEmpty()) {
                return null;
            }
            Character letterChar = null;
            String prefixString = letterString;
            final boolean hasNumeric = !digitString.isEmpty();

            if (!hasNumeric) {
                final int letterLastPos = letterString.length() - 1;
                letterChar = letterString.charAt(letterLastPos);
                prefixString = letterString.substring(0, letterLastPos);
            }

            return new VariableComponents(prefixString, letterChar, digitString, hasNumeric);
        }
    }

    // Pattern for matching a variable name
    // Matches: A, Z, a, z, $A, $ABC, $nA1, $Na01, etc.
    // Single letter (with optional $): [A-Za-z] or $[A-Za-z]
    // Multi-char (requires $): $[A-Za-z][A-Za-z0-9]+
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
            "\\$?[A-Za-z][A-Za-z0-9]*"
    );

    // Pattern for matching a numeric value (including negative numbers in parentheses)
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "\\(-?\\d+\\)|-?\\d+"
    );

    // Pattern for the entire range expression
    // Matches variable-variable or numeric-numeric ranges
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^(\\(-?\\d+\\)|-?\\d+|\\$?[A-Za-z][A-Za-z0-9]*)-(\\(-?\\d+\\)|-?\\d+|\\$?[A-Za-z][A-Za-z0-9]*)$"
    );

    /**
     * Parses a range string and creates a list of values.
     *
     * @param rangeString The range string to parse (e.g., "1-5", "$A-$D").
     * @return A list of values representing the range.
     * @throws FormulaException If the range string is invalid.
     */
    @NonNull
    protected static List<Value> createFromString(final String rangeString) throws FormulaException {
        if (rangeString == null || rangeString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        final Pair<String, String> range = RangeParser.matchRangePattern(rangeString.trim());
        if (null == range) {
            return Collections.emptyList();
        }

        final String start = range.first;
        final String end = range.second;

        // Check if both are valid numeric patterns
        final Integer startNum = RangeParser.matchNumericPattern(start);
        final Integer endNum = RangeParser.matchNumericPattern(end);
        if (startNum != null && endNum != null) {
            return parseNumericRange(startNum, endNum);
        } else {
            // Check if both are valid variable patterns
            final VariableComponents startComponent = RangeParser.matchVariablePattern(start);
            final VariableComponents endComponent = RangeParser.matchVariablePattern(end);
            if (startComponent != null && endComponent != null) {
                return parseVariableRange(startComponent, endComponent);
            }
        }
        throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, rangeString);
    }

    /**
     * Parses a numeric range and generates a list of values.
     *
     * @param startNum The starting number of the range.
     * @param endNum   The ending number of the range.
     * @return A list of numeric values within the range.
     */
    @NonNull
    private static List<Value> parseNumericRange(final int startNum, final int endNum) {
        try {
            if (startNum > endNum) {
                throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, startNum + "-" + endNum);
            }
            final int size = endNum - startNum + 1;

            // Pre-calculate all values
            final List<Value> values = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                values.add(Value.of(startNum + i));
            }

            return values;
        } catch (NumberFormatException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Parses a variable range and generates a list of values.
     *
     * @param startComp The starting variable components.
     * @param endComp   The ending variable components.
     * @return A list of variable values within the range.
     * @throws FormulaException If the range is invalid.
     */
    @NonNull
    private static List<Value> parseVariableRange(@NonNull final VariableComponents startComp, @NonNull final VariableComponents endComp) throws FormulaException {
        // Variables must have compatible structure for ranging
        if (!VariableComponents.areCompatible(startComp, endComp)) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, startComp.getName() + "-" + endComp.getName());
        }

        final int size = VariableComponents.calculateRangeSize(startComp, endComp);

        if (size <= 0) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, startComp.getName() + "-" + endComp.getName());
        }

        // Calculate padding format for variable ranges with numeric suffixes
        final String paddingFormat = startComp.hasNumeric ?
                calculateVariablePadding(startComp, endComp) : null;

        // Pre-calculate all values
        final List<Value> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(Value.of(VariableComponents.incrementVariable(startComp, i, paddingFormat)));
        }

        return values;
    }

    /**
     * Parses a single variable name and returns its canonical form.
     *
     * @param var The variable name to parse.
     * @return The canonical form of the variable name, or null if invalid.
     */
    @Nullable
    public static String parseSingleVariableName(final String var) {
        final VariableComponents components = VariableComponents.parseVariableComponents(var);
        if (components != null) {
            return components.getName();
        }
        return null;
    }

    @Nullable
    private static Pair<String, String> matchRangePattern(final String numericString) {
        final Matcher rangeMatcher = RANGE_PATTERN.matcher(numericString);
        if (!rangeMatcher.matches()) {
            return null;
        }
        return new Pair<>(rangeMatcher.group(1), rangeMatcher.group(2));
    }

    @Nullable
    private static VariableComponents matchVariablePattern(final String variableString) {
        if (!VARIABLE_PATTERN.matcher(variableString).matches()) {
            return null;
        }
        // Extract variable components
        return VariableComponents.parseVariableComponents(variableString);
    }

    @Nullable
    private static Integer matchNumericPattern(final String numericString) {
        if (!NUMERIC_PATTERN.matcher(numericString).matches()) {
            return null;
        }
        try {
            String clean = numericString.trim();
            if (clean.startsWith("(") && clean.endsWith(")")) {
                clean = clean.substring(1, clean.length() - 1);
            }
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            // Handle potential overflow or invalid format
            return null;
        }
    }

    @NonNull
    private static String calculateVariablePadding(final VariableComponents startComp, final VariableComponents endComp) {
        // Determine the maximum length needed for padding
        final int maxLen = Math.max(startComp.numericPart.length(), endComp.numericPart.length());

        // Check if either has leading zeros
        if ((startComp.numericPart.length() > 1 && startComp.numericPart.startsWith("0")) ||
                (endComp.numericPart.length() > 1 && endComp.numericPart.startsWith("0"))) {
            return "%0" + maxLen + "d";
        }
        return "%d";
    }
}
