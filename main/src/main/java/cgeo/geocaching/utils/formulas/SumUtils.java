package cgeo.geocaching.utils.formulas;

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
                "Both parameters must be of the same type (both strings or both numeric)", 
                start.getType() + " and " + end.getType(), 
                "same type");
        }
    }

    private static Value sumNumericRange(final Value start, final Value end) {
        if (!start.isInteger() || !end.isInteger()) {
            throw new FormulaException(FormulaException.ErrorType.WRONG_TYPE, 
                "Numeric ranges must use integer parameters", 
                start.getType() + " and " + end.getType(), 
                "Integer");
        }
        final long startVal = start.getAsLong();
        final long endVal = end.getAsLong();
        if (startVal > endVal) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Start value must be <= end value: " + startVal + " > " + endVal);
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
                    throw new FormulaException(FormulaException.ErrorType.OTHER, 
                        "Variable " + varName + " is not numeric");
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
        
        throw new FormulaException(FormulaException.ErrorType.OTHER, 
            "Invalid variable range: " + startVar + " to " + endVar);
    }
    
    private static void validateVariableFormat(final String varName) {
        // Multi-character variables must have $ prefix
        // Single-character variables can optionally have $ prefix
        if (varName.length() > 1 && !varName.startsWith("$")) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Multi-character variable '" + varName + "' must have $ prefix (e.g., '$" + varName + "')");
        }
        // If it has $, make sure there's content after it
        if (varName.equals("$")) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Invalid variable name: " + varName);
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
    
    private static void validateSingleLetterRange(final char startChar, final char endChar, 
            final String start, final String end) {
        // Check that both are same case (both upper or both lower)
        if (Character.isUpperCase(startChar) != Character.isUpperCase(endChar)) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Cannot mix uppercase and lowercase in variable range: " + start + " to " + end);
        }
        
        // Check that start <= end
        final char startCharUpper = Character.toUpperCase(startChar);
        final char endCharUpper = Character.toUpperCase(endChar);
        if (startCharUpper > endCharUpper) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Start variable must be <= end variable: " + startChar + " > " + endChar);
        }
    }
    
    private static List<String> expandSingleLetterRange(final String start, final String end) {
        final List<String> variables = new ArrayList<>();
        final char startChar = start.charAt(0);
        final char endChar = end.charAt(0);
        
        validateSingleLetterRange(startChar, endChar, start, end);
        
        // Normalize to uppercase for comparison and iteration
        final char startCharUpper = Character.toUpperCase(startChar);
        final char endCharUpper = Character.toUpperCase(endChar);
        
        // Generate range preserving the original case
        final boolean isUpperCase = Character.isUpperCase(startChar);
        for (char c = startCharUpper; c <= endCharUpper; c++) {
            variables.add(String.valueOf(isUpperCase ? c : Character.toLowerCase(c)));
        }
        return variables;
    }
    
    private static List<String> expandNumericSuffixRange(final String start, final String end) {
        final List<String> variables = new ArrayList<>();
        final String startPrefix = extractPrefix(start);
        final String endPrefix = extractPrefix(end);
        
        if (!startPrefix.equalsIgnoreCase(endPrefix)) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Variable prefixes must match: " + startPrefix + " != " + endPrefix);
        }
        
        final String startSuffix = start.substring(startPrefix.length());
        final String endSuffix = end.substring(endPrefix.length());
        
        final int startNum;
        final int endNum;
        try {
            startNum = Integer.parseInt(startSuffix);
            endNum = Integer.parseInt(endSuffix);
        } catch (final NumberFormatException e) {
            throw new FormulaException(FormulaException.ErrorType.OTHER,
                "Variable suffixes must be numeric: '" + start + "', '" + end + "'");
        }
        
        if (startNum > endNum) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Start value must be <= end value: " + startNum + " > " + endNum);
        }
        
        for (int i = startNum; i <= endNum; i++) {
            variables.add(startPrefix + i);
        }
        return variables;
    }
    
    private static List<String> expandLetterSuffixRange(final String start, final String end) {
        final List<String> variables = new ArrayList<>();
        final String startPrefix = start.substring(0, start.length() - 1);
        final String endPrefix = end.substring(0, end.length() - 1);
        
        if (!startPrefix.equalsIgnoreCase(endPrefix)) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Variable prefixes must match: " + startPrefix + " != " + endPrefix);
        }
        
        final char startChar = Character.toUpperCase(start.charAt(start.length() - 1));
        final char endChar = Character.toUpperCase(end.charAt(end.length() - 1));
        
        if (startChar > endChar) {
            throw new FormulaException(FormulaException.ErrorType.OTHER, 
                "Start variable must be <= end variable: " + startChar + " > " + endChar);
        }
        
        for (char c = startChar; c <= endChar; c++) {
            variables.add(startPrefix + c);
        }
        return variables;
    }
    
    private static String extractPrefix(final String varName) {
        int i = varName.length() - 1;
        while (i >= 0 && Character.isDigit(varName.charAt(i))) {
            i--;
        }
        return varName.substring(0, i + 1);
    }
}
