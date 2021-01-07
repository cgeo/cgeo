package cgeo.geocaching.calculator;

import java.util.ArrayList;
import java.util.Arrays;
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
public final class CoordinatesCalculateUtils {

    /** Flag values used to designate that no AutoChar has been set */
    public static final char EMPTY_CHAR = '-';

    private CoordinatesCalculateUtils() {
        // Do not instantiate
    }

    /**
     * Find if a variable exists in the supplied list with the given name
     *
     * @param name name to search for
     * @param list list of variables
     * @return first occurrence of the variable if it can found, 'null' otherwise
     */
     private static VariableData getVariable(final char name, final List<VariableData> list, final boolean remove) {
        for (final VariableData equ : list) {
            if (equ.getName() == name) {
                if (remove) {
                    list.remove(equ);
                }
                return equ;
            }
        }

        return null;
    }

    /**
     * Find if variable data exists in the supplied list with the given name
     *
     * @param name name to search for
     * @return first occurrence of the data if it can found, 'null' otherwise
     */
    private static VariableData findAndRemoveData(final char name, final List<VariableData> list) {
        for (final VariableData var : list) {
            if (var.getName() == name) {
                list.remove(var);
                return var;
            }
        }

        return null;
    }

    public static List<VariableData> sortVariables(final List<VariableData> variables,
                                                   final List<VariableData> varBank,
                                                   final String variableNames,
                                                   final CaseCheck theCase) {
        final List<VariableData> returnList = new ArrayList<>();

        final char[] sortedVariables = variableNames.toCharArray();
        Arrays.sort(sortedVariables);

        for (final char ch : sortedVariables) {
            if (theCase.check(ch)) {
                if (getVariable(ch, returnList, false) != null) {
                    continue;
                }

                VariableData thisEquation = getVariable(ch, variables, true);
                if (thisEquation == null) {
                    thisEquation = findAndRemoveData(ch, varBank);

                    if (thisEquation == null) {
                        thisEquation = new VariableData(ch);
                    }
                }

                returnList.add(thisEquation);
            }
        }

        // Add all the left over equations to the variable bank.
        for (final VariableData var : variables) {
            varBank.add(var);
        }

        return returnList;
    }
}
