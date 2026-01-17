package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import android.util.Pair;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utility methods for the sum() formula function.
 * Handles variable range expansion, numeric range summation, and variable summation.
 */
public final class SumUtils {

    private SumUtils() {
        // Utility class, no instantiation
    }

    /**
     * Implementation of sum function that supports ranges of variables or numeric ranges
     */
    public static Value sum(final ValueList valueList) {
        valueList.assertCheckCount(2, 2, false);
        
        final Value start = valueList.get(0);
        final Value end = valueList.get(1);
        
        validateSumParameters(start, end);
        
        // Handle numeric range: sum(1;5) -> 1+2+3+4+5
        if (start.isNumeric() && end.isNumeric()) {
            return sumNumericRange(start, end);
        }
        
        // This should never be reached in runtime as string parameters are handled at compile time
        // but we provide a fallback error
        throw new FormulaException(FormulaException.ErrorType.OTHER, 
            "sum() with string parameters must be compiled with variable context");
    }

    private static void validateSumParameters(final Value start, final Value end) {
        // Both parameters must be of the same type
        if (start.isNumeric() != end.isNumeric()) {
            throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE, 
                "same type (both numeric or both string)", 
                start.toUserDisplayableString(), 
                start.getType());
        }
    }

    private static Value sumNumericRange(final Value start, final Value end) {
        if (!start.isInteger() || !end.isInteger()) {
            throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE, 
                "Integer", 
                start.toUserDisplayableString(), 
                start.getType());
        }
        final long startVal = start.getAsLong();
        final long endVal = end.getAsLong();
        if (startVal > endVal) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, 
                startVal + " > " + endVal);
        }
        
        // Use arithmetic series formula: n*(start+end)/2 for better performance
        // Use BigInteger to avoid overflow for large ranges
        final BigInteger startBI = BigInteger.valueOf(startVal);
        final BigInteger endBI = BigInteger.valueOf(endVal);
        final BigInteger n = endBI.subtract(startBI).add(BigInteger.ONE);
        final BigInteger sum = n.multiply(startBI.add(endBI)).divide(BigInteger.TWO);
        return Value.of(sum);
    }
    
    /**
     * Helper method to sum a list of variables
     * @param variables List of variable names to sum
     * @param varProvider Function to retrieve variable values
     * @return Pair of sum and list of missing variables (if any)
     */
    public static Pair<BigDecimal, List<String>> sumVariables(
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
    public static List<String> expandVariableRange(final String startVar, final String endVar) {
        // Validate that multi-character variables have $ prefix
        validateVariableFormat(startVar);
        validateVariableFormat(endVar);
        
        // Remove leading $ if present
        final String start = startVar.startsWith("$") ? startVar.substring(1) : startVar;
        final String end = endVar.startsWith("$") ? endVar.substring(1) : endVar;
        
        // Single-letter variable range: A-D
        if (isSingleLetterRange(start, end)) {
            return expandSingleLetterRange(start, end);
        }
        
        // Two-letter or longer variable ranges
        if (start.length() >= 2 && end.length() >= 2) {
            // Check if both have numeric suffix: A1-A5
            if (hasNumericSuffix(start) && hasNumericSuffix(end)) {
                return expandNumericSuffixRange(start, end);
            }
            
            // Check if both have letter suffix: NA-ND
            if (hasLetterSuffix(start) && hasLetterSuffix(end)) {
                return expandLetterSuffixRange(start, end);
            }
        }
        
        throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, 
            startVar + " to " + endVar);
    }
    
    private static void validateVariableFormat(final String varName) {
        // Multi-character variables must have $ prefix
        // Single-character variables can optionally have $ prefix
        if (varName.length() > 1 && !varName.startsWith("$")) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, 
                LocalizationUtils.getString(R.string.formula_error_range_invalid_variable, varName));
        }
        // If it has $, make sure there's content after it
        if (varName.equals("$")) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, 
                varName);
        }
    }
    
    private static boolean isSingleLetterRange(final String start, final String end) {
        return start.length() == 1 && end.length() == 1 && 
               Character.isLetter(start.charAt(0)) && Character.isLetter(end.charAt(0));
    }
    
    private static boolean hasNumericSuffix(final String var) {
        return Character.isDigit(var.charAt(var.length() - 1));
    }
    
    private static boolean hasLetterSuffix(final String var) {
        return Character.isLetter(var.charAt(var.length() - 1));
    }
    
    private static void validateSingleLetterRange(final char startChar, final char endChar) {
        // Check that both are same case (both upper or both lower) and that start <= end
        final boolean isStartUpper = Character.isUpperCase(startChar);
        final boolean isEndUpper = Character.isUpperCase(endChar);
        final char startCharUpper = Character.toUpperCase(startChar);
        final char endCharUpper = Character.toUpperCase(endChar);
        
        if (isStartUpper != isEndUpper || startCharUpper > endCharUpper) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, 
                LocalizationUtils.getString(R.string.formula_error_range_case_mismatch, 
                    String.valueOf(startChar), String.valueOf(endChar)));
        }
    }
    
    private static List<String> expandSingleLetterRange(final String start, final String end) {
        final List<String> variables = new ArrayList<>();
        final char startChar = start.charAt(0);
        final char endChar = end.charAt(0);
        
        validateSingleLetterRange(startChar, endChar);
        
        // Generate range - iterate directly on the characters preserving case
        for (char c = startChar; c <= endChar; c++) {
            variables.add(String.valueOf(c));
        }
        return variables;
    }
    
    private static List<String> expandNumericSuffixRange(final String start, final String end) {
        final String startPrefix = extractPrefix(start);
        final String endPrefix = extractPrefix(end);
        
        validatePrefixMatch(startPrefix, endPrefix);
        
        final String startSuffix = start.substring(startPrefix.length());
        final String endSuffix = end.substring(endPrefix.length());
        
        final int startNum;
        final int endNum;
        try {
            startNum = Integer.parseInt(startSuffix);
            endNum = Integer.parseInt(endSuffix);
        } catch (final NumberFormatException e) {
            throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE,
                "Numeric suffix", 
                start + ", " + end, 
                "String");
        }
        
        if (startNum > endNum) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, 
                startNum + " > " + endNum);
        }
        
        final List<String> variables = new ArrayList<>();
        for (int i = startNum; i <= endNum; i++) {
            variables.add(startPrefix + i);
        }
        return variables;
    }
    
    private static List<String> expandLetterSuffixRange(final String start, final String end) {
        final String startPrefix = start.substring(0, start.length() - 1);
        final String endPrefix = end.substring(0, end.length() - 1);
        
        validatePrefixMatch(startPrefix, endPrefix);
        
        final char startChar = start.charAt(start.length() - 1);
        final char endChar = end.charAt(end.length() - 1);
        
        // Validate same case and range ordering
        validateSingleLetterRange(startChar, endChar);
        
        final List<String> variables = new ArrayList<>();
        for (char c = startChar; c <= endChar; c++) {
            variables.add(startPrefix + c);
        }
        return variables;
    }
    
    private static void validatePrefixMatch(final String startPrefix, final String endPrefix) {
        // Prefixes must match exactly (including case)
        if (!startPrefix.equals(endPrefix)) {
            throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, 
                LocalizationUtils.getString(R.string.formula_error_range_prefix_mismatch, 
                    startPrefix, endPrefix));
        }
    }
    
    private static String extractPrefix(final String varName) {
        int i = varName.length() - 1;
        while (i >= 0 && Character.isDigit(varName.charAt(i))) {
            i--;
        }
        return varName.substring(0, i + 1);
    }
}
