package cgeo.geocaching.calculator;


import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.formulas.Formula;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.List;




/**
 * Through out this implementation:
 *
 * 'Equations' are used to represent 'Variables' that appear in the description of the cache coordinated themselves.
 *             As in "N 42° 127.ABC".  In this example 'A', 'B' and 'C' are all 'equations'.
 *             All 'equations' must have a CAPITAL-LETTER name.
 *
 * 'FreeVariables' are used to represent 'Variables' that appear in the 'expression' of an equation
 *                 As in "X = a^2 + b^2".  In this example 'a' and 'b' are both 'freeVariables'.
 *                 All 'freeVariables' must have a lower-case name.
 */
public final class CalcStateEvaluator {

    /** Flag values used to designate that no AutoChar has been set */
    public static final char EMPTY_CHAR = '-';

    // TODO: as parameter
    private static final String PLACE_HOLDER = "~";

    private static final char[] BRACKET_OPENINGS = {'(', '[', '{'};
    private static final char[] BRACKET_CLOSINGS = {')', ']', '}'};


    private final List<VariableData> equations;
    private final List<VariableData> freeVariables;
    private final Context context;

    public CalcStateEvaluator(final List<VariableData> equations, final List<VariableData> freeVariables, final Context context) {
        this.equations = equations;
        this.freeVariables = freeVariables;
        this.context = context;
    }

    /**
     * Evaluates a Geopoint from given latitude and longitude - formula.
     * Replace 'equation' variables with their computed values: 42° AB.CDE' -> 42° 12.345'
     *
     * @param latValue The string for the latitude
     * @param lonValue The string for the latitude
     * @return parsed Geopoint, null, if no valid coordinates can be evaluated
     */
    @Nullable
    public Geopoint evaluate(final String latValue, final String lonValue) {
        try {
            final String latString = evaluate(latValue);
            final String lonString = evaluate(lonValue);
            final Geopoint gp = new Geopoint(latString, lonString);
            return gp;
        } catch (Geopoint.GeopointException ex) {
            // nothing
        }

        return null;
    }

    /**
     * Replace 'equation' variables with their computed values: 42° AB.CDE' -> 42° 12.345'
     *
     * @param values The string to perform the substitutions on
     * @return String with the substitutions performed
     */
    public  String evaluate(final String values) {
        String returnValue = "";

        if (values.length() > 0) {
            final char first = values.charAt(0);
            String substitutionString;

            // Trim of the leading hemisphere character if it exists.
            if (first == 'N' || first == 'S' || first == 'E' || first == 'W') {
                returnValue = returnValue.concat(String.valueOf(first));
                substitutionString = values.substring(1);
            } else {
                substitutionString = values;
            }

            // Perform the substitutions on the remainder of the string.
            for (final VariableData equ : equations) {
                substitutionString = substitutionString.replace(String.valueOf(equ.getName()), equ.evaluateString(freeVariables, context));
            }

            // If the string contains matching brackets evaluate the enclosed expression (for use in PLANE format)
            substitutionString = evaluateBrackets(substitutionString);

            // Recombine the hemisphere and substituted string.
            returnValue = returnValue.concat(substitutionString);
        }

        // Remove placeholder characters.
        returnValue = returnValue.replaceAll(PLACE_HOLDER, "");

        // Break up connecting underscores
        while (returnValue.contains("__")) {
            returnValue = returnValue.replace("__", "_ _");
        }

        return returnValue;
    }


    private static String evaluateBrackets(final String original) {
        String returnValue = original;
        int openIndex;
        int closeIndex;

        try {
            for (int bracketIndex = 0; bracketIndex < BRACKET_OPENINGS.length; bracketIndex++) {
                for (int returnValueIndex = 0; returnValueIndex < returnValue.length(); returnValueIndex++) {
                    char ch = returnValue.charAt(returnValueIndex);

                    if (ch == BRACKET_OPENINGS[bracketIndex]) {
                        int nestedBrackerCount = 1;
                        openIndex = returnValueIndex;
                        closeIndex = returnValueIndex;

                        while (nestedBrackerCount > 0 && closeIndex < returnValue.length() - 1) {
                            closeIndex++;
                            ch = returnValue.charAt(closeIndex);

                            if (ch == BRACKET_OPENINGS[bracketIndex]) {
                                nestedBrackerCount++;
                            } else if (ch == BRACKET_CLOSINGS[bracketIndex]) {
                                nestedBrackerCount--;
                            }
                        }

                        if (nestedBrackerCount == 0) {
                            String result = "";

                            if (closeIndex > openIndex + 1) {
                                final int resInt = (int) (Formula.eval(returnValue.substring(openIndex + 1, closeIndex)));
                                result = String.valueOf(resInt);
                            }

                            returnValue = returnValue.substring(0, openIndex) + result + returnValue.substring(closeIndex + 1);
                        } else {
                            // Reached end without finding enough closing brackets
                            throw new IllegalArgumentException("Unmatched opening bracket '" + returnValue.charAt(openIndex) + "' at index " + openIndex + " of \"" + returnValue + "\"/");
                        }
                    } else if (ch == BRACKET_CLOSINGS[bracketIndex]) {
                        // Negative nested bracket count.
                        throw new IllegalArgumentException("Unmatched closing bracket '" + ch + "' at index " + returnValueIndex + " of \"" + returnValue + "\"/");
                    }
                }
            }
        } catch (final Exception e) {
            // section can't be evaluated
            returnValue = original;
        }

        return returnValue;
    }
}
