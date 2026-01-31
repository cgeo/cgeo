package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.TextUtils;

import android.text.SpannableStringBuilder;
import android.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for the implementation of the sum() formula function.
 * <p>
 * SumUtils provides static helper methods to support the sum() function in formulas, including:
 * <ul>
 *   <li>Summing numeric ranges (e.g. sum(1;5) → 1+2+3+4+5)</li>
 *   <li>Summing variable ranges (e.g. sum(A;D) → A+B+C+D)</li>
 *   <li>Expanding variable ranges for dependency tracking and evaluation</li>
 *   <li>Validating and formatting variable and numeric ranges</li>
 * </ul>
 * The class is used internally by the formula parser and evaluator to handle sum operations with both
 * numeric and variable arguments, including error handling for invalid ranges or types.
 * <p>
 * parseSumFunction is used for compile-time parsing of sum() functions with variable range parameters,
 * creating appropriate FormulaNode instances for evaluation.
 * <p>
 * Instantiation is not intended (private constructor).
 */
final class SumUtils {

    private SumUtils() {
        // Utility class, no instantiation
    }

    /**
     * Implementation of sum function that supports ranges of variables or numeric ranges
     */
    static Value sum(final ValueList valueList) {
        valueList.assertCheckCount(2, 2, false);

        final Value start = valueList.get(0);
        final Value end = valueList.get(1);

        validateSumParameters(start, end);

        // Handle numeric range: sum(1;5) -> 1+2+3+4+5
        return Value.of(sumNumericRange(start.getAsInteger(), end.getAsInteger()));
    }

    private static void validateSumParameters(final Value start, final Value end) {
        // Both parameters must be integer
        if (!start.isInteger() || !end.isInteger()) {
            if (!start.isInteger() && !end.isInteger()) {
                throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE,
                        "Integer",
                        start.toUserDisplayableString() + ", " + end.toUserDisplayableString(),
                        start.getType());
            } else if (!start.isInteger()) {
                throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE,
                        "Integer",
                        start.toUserDisplayableString(),
                        start.getType());
            } else {
                throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE,
                        "Integer",
                        end.toUserDisplayableString(),
                        end.getType());
            }
        }
    }

    private static BigInteger sumNumericRange(final BigInteger start, final BigInteger end) {
        if (start.compareTo(end) > 0) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE,
                    start + " > " + end);
        }

        // Use arithmetic series formula: (end-start+1)*(start+end)/2 for better performance
        // Use BigInteger to avoid overflow for large ranges
        final BigInteger n = end.subtract(start).add(BigInteger.ONE);
        final BigInteger sum = n.multiply(start.add(end)).divide(BigInteger.valueOf(2));
        return sum;
    }
    
    /**
     * Helper method to sum a list of variables
     * @param variables List of variable names to sum
     * @param varProvider Function to retrieve variable values
     * @return Pair of sum and list of missing variables (if any)
     */
    static Pair<BigDecimal, List<String>> sumVariables(
            final List<String> variables,
            final Function<String, Value> varProvider) {
        BigDecimal sum = BigDecimal.ZERO;
        final List<String> missingVars = new ArrayList<>();
        
        for (final String varName : variables) {
            final Value value = varProvider.apply(varName);
            if (value == null) {
                missingVars.add(varName);
            } else {
                if (!value.isNumeric()) {
                    throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE, 
                        "Numeric", 
                        varName, 
                        value.getType());
                }
                sum = sum.add(value.getAsDecimal());
            }
        }
        
        return new Pair<>(sum, missingVars);
    }
    
    /**
     * Generate list of variable names in a range for dependency tracking
     * This is called at compile time to determine which variables are needed
     */
    static List<String> expandVariableRange(final String startVar, final String endVar) {
        // Validate that multi-character variables have $ prefix
        validateVariableFormat(startVar);
        validateVariableFormat(endVar);

        // Remove leading $ if present
        final String start = startVar.startsWith("$") ? startVar.substring(1) : startVar;
        final String end = endVar.startsWith("$") ? endVar.substring(1) : endVar;

        // Two-letter or longer variable ranges
        // Check if both have numeric suffix: A1-A5
        if (hasNumericSuffix(start) && hasNumericSuffix(end)) {
            return expandNumericSuffixRange(start, end);
        }

        // Check if both have letter suffix: NA-ND
        if (hasLetterSuffix(start) && hasLetterSuffix(end)) {
            return expandLetterSuffixRange(start, end);
        }

        throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE,
                startVar + " - " + endVar);
    }
    
    private static void validateVariableFormat(final String varName) {
        // Multi-character variables must have $ prefix
        // Single-character variables can optionally have $ prefix
        if ((varName.length() > 1 && !varName.startsWith("$")) || varName.equals("$")) {
            throw new FormulaException(FormulaException.ErrorType.UNEXPECTED_TOKEN, 
                LocalizationUtils.getString(R.string.formula_varname));
        }
    }

    private static boolean hasNumericSuffix(final String var) {
        return !var.isEmpty() && Character.isDigit(var.charAt(var.length() - 1));
    }

    private static boolean hasLetterSuffix(final String var) {
        return !var.isEmpty() && Character.isLetter(var.charAt(var.length() - 1));
    }

    private static void validateLetterRange(final char startChar, final char endChar) {
        // Check that both are same case (both upper or both lower) and that start <= end
        final boolean isStartUpper = Character.isUpperCase(startChar);
        final boolean isEndUpper = Character.isUpperCase(endChar);

        if (isStartUpper != isEndUpper) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE,
                    startChar + " != " + endChar);
        } else if (startChar > endChar) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE,
                    startChar + " > " + endChar);
        }
    }

    private static List<String> expandNumericSuffixRange(final String start, final String end) {
        final String startPrefix = extractPrefix(start);
        final String endPrefix = extractPrefix(end);
        validatePrefixMatch(startPrefix, endPrefix);

        final String startSuffix = start.substring(startPrefix.length());
        final String endSuffix = end.substring(endPrefix.length());

        final int startNum = parseNumericSuffix(startSuffix, start, end);
        final int endNum = parseNumericSuffix(endSuffix, start, end);

        validateNumericRange(startNum, endNum, startSuffix, endSuffix);

        final String formatPattern = "%0" + startSuffix.length() + "d";

        final List<String> variables = new ArrayList<>(endNum - startNum + 1);
        for (int i = startNum; i <= endNum; i++) {
            variables.add(startPrefix + String.format(formatPattern, i));
        }
        return variables;
    }

    private static int parseNumericSuffix(final String suffix, final String start, final String end) {
        try {
            return Integer.parseInt(suffix);
        } catch (final NumberFormatException e) {
            throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE,
                    "Numeric suffix", start + ", " + end, "String");
        }
    }

    private static void validateNumericRange(final int startNum, final int endNum,
                                             final String startSuffix, final String endSuffix) {
        if (startNum > endNum) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE,
                    startNum + " > " + endNum);
        }

        final int startLen = startSuffix.length();
        final int endLen = endSuffix.length();

        final boolean hasDifferentLength = startLen != endLen;
        final boolean hasLeadingZero = startSuffix.startsWith("0") || endSuffix.startsWith("0");

        if (startLen > endLen || (hasLeadingZero && hasDifferentLength)) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE,
                    startSuffix + " != " + endSuffix);
        }
    }

    private static List<String> expandLetterSuffixRange(final String start, final String end) {
        final int startPrefixLen = start.length() - 1;
        final int endPrefixLen = end.length() - 1;
        final String startPrefix = start.substring(0, startPrefixLen);
        final String endPrefix = end.substring(0, endPrefixLen);
        
        validatePrefixMatch(startPrefix, endPrefix);

        final char startChar = start.charAt(startPrefixLen);
        final char endChar = end.charAt(endPrefixLen);
        
        // Validate same case and range ordering
        validateLetterRange(startChar, endChar);

        final List<String> variables = new ArrayList<>(endChar - startChar + 1);
        for (char c = startChar; c <= endChar; c++) {
            variables.add(startPrefix + c);
        }
        return variables;
    }
    
    private static void validatePrefixMatch(final String startPrefix, final String endPrefix) {
        // Prefixes must match exactly (including case)
        if (!startPrefix.equals(endPrefix)) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE,
                    startPrefix + " != " + endPrefix);
        }
    }

    private static String extractPrefix(final String varName) {
        for (int i = varName.length() - 1; i >= 0; i--) {
            if (!Character.isDigit(varName.charAt(i))) {
                return varName.substring(0, i + 1);
            }
        }
        return "";
    }

    /**
     * Parses a sum function with possible variable range parameters.
     * Returns a FormulaNode for a variable range sum, or null for standard handling.
     */
    static FormulaNode parseSumFunction(final List<FormulaNode> params) {
        if (params == null || params.size() != 2) {
            return null;
        }

        // Try to extract string literals from parameters
        final String startVar = extractStringLiteral(params.get(0));
        final String endVar = extractStringLiteral(params.get(1));

        // If both parameters are string literals, expand to variable range
        if (startVar != null && endVar != null) {
            return createVariableRangeSumNode(startVar, endVar);
        }

        // Otherwise, return null for standard handling
        return null;
    }

    private static FormulaNode createVariableRangeSumNode(final String startVar, final String endVar) {
        try {
            final List<String> variables = expandVariableRange(startVar, endVar);
            // Create a sum node that references all variables in the range
            return new FormulaNode(Formula.VAR_RANGE_ID, null,
                    (objs, vars, ri) -> {
                        final Pair<BigDecimal, List<String>> result =
                                sumVariables(variables, vars);
                        if (!result.second.isEmpty()) {
                            Collections.sort(result.second);
                            throw new FormulaException(FormulaException.ErrorType.MISSING_VARIABLE_VALUE,
                                    StringUtils.join(result.second, ", "));
                        }
                        return Value.of(result.first);
                    },
                    (objs, vars, ri, error) -> formatVariableRangeSumDisplay(variables, vars),
                    result -> result.addAll(variables)); // Add all variables as dependencies
        } catch (FormulaException fe) {
            // If expansion fails, re-throw the exception to fail at compile time
            throw fe;
        }
    }

    private static CharSequence formatVariableRangeSumDisplay(final List<String> variables, final Function<String, Value> vars) {
        final SpannableStringBuilder sb = new SpannableStringBuilder(FormulaFunction.SUMRANGE.getMainName());
        sb.append("(");
        boolean first = true;
        for (final String varName : variables) {
            if (!first) {
                sb.append("+");
            }
            first = false;
            final Value value = vars.apply(varName);
            if (value == null) {
                sb.append(TextUtils.setSpan("?" + varName, FormulaError.createErrorSpan()));
            } else {
                sb.append(value.getAsString());
            }
        }
        sb.append(")");
        return sb;
    }

    private static String extractStringLiteral(final FormulaNode node) {
        // Check if this is a string-literal node
        if (node == null || !Formula.STRING_LITERAL_ID.equals(node.getId())) {
            return null;
        }

        try {
            final Value val = node.eval(x -> null, -1);
            return val.getAsString();
        } catch (final Exception e) {
            return null;
        }
    }
}
