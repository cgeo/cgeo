package cgeo.geocaching.utils.formulas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The RangeFormulaUtils class provides utility methods for handling ranges in formulas.
 * It includes methods for processing range expressions, extracting variables, and performing operations on ranges.
 *
 * Example usage:
 * <pre>
 *     // Get needed variables for a range
 *     List<Value> constantParams = Arrays.asList(Value.of("1-5"), Value.of("$A-$C"));
 *     Set<String> neededVars = RangeFormulaUtils.getNeededVariablesForRange(constantParams);
 *     // Output: ["$A", "$B", "$C"]
 *
 *     // Perform a range operation
 *     BiFunction<Function<String, Value>, ValueList, Object> rangeOp = RangeFormulaUtils.rangeOperationFunction(BigDecimal.ZERO, BigDecimal::add);
 * </pre>
 */
public class RangeFormulaUtils {

    private RangeFormulaUtils() {
        //no instance
    }

    /**
     * Creates a function to process a range list and apply a custom operation.
     *
     * @param stringFunction The function to apply to the range list.
     * @return A BiFunction that processes the range list.
     */
    public static BiFunction<Function<String, Value>, ValueList, Object> rangeListFunction(final Function<ValueList, Object> stringFunction) {
        return (vars, params) -> {
            final ValueList valueList = getValueList(vars, params);

            return stringFunction.apply(valueList);
        };
    }

    /**
     * Creates a function to perform a range operation starting from a given value.
     *
     * @param startValue    The starting value for the operation.
     * @param rangeOperator The operator to apply to the range values.
     * @return A BiFunction that performs the range operation.
     */
    public static BiFunction<Function<String, Value>, ValueList, Object> rangeOperationFunction(final BigDecimal startValue, final BinaryOperator<BigDecimal> rangeOperator) {
        return rangeListFunction((vars) -> {
            final List<Value> numericValues = new ArrayList<>();
            for (final Value vl : vars) {
                numericValues.add(vl);
            }

            return numericValues.stream().map(Value::getAsDecimal).reduce(startValue, rangeOperator);
        });
    }

    /**
     * Extracts the variables needed for a range from constant parameter values.
     *
     * @param constantParamValues The constant parameter values.
     * @return A set of variable names needed for the range.
     */
    @NonNull
    public static Set<String> getNeededVariablesForRange(final List<Value> constantParamValues) {
        final Set<String> neededVars = new HashSet<>();

        final Set<String> stringParams = constantParamValues.stream().filter(Objects::nonNull).map(Value::getAsString).collect(Collectors.toSet());
        for (final String rangeString : stringParams) {
            final List<String> valuesList = parseValuesFromRange(rangeString).stream().filter(vv -> !vv.isNumeric()).map(Value::getAsString).collect(Collectors.toList());
            neededVars.addAll(valuesList);
        }

        return neededVars;
    }

    @NonNull
    private static List<Value> parseValuesFromRange(final String rangeString) {
        return RangeParser.createFromString(rangeString);
    }

    @Nullable
    private static Value parseSingleValue(final String rangeString) {
        final String varName = RangeParser.parseSingleVariableName(rangeString);
        if (varName != null) {
            return Value.of(varName);
        }
        return null;
    }

    @NonNull
    private static ValueList getValueList(final Function<String, Value> vars, final ValueList params) {
        final List<String> missingVariables = new ArrayList<>();
        final ValueList valueList = new ValueList();

        params.forEach(pv -> RangeFormulaUtils.processValue(vars, pv, valueList, missingVariables));

        if (!missingVariables.isEmpty()) {
            final String varNames = missingVariables.stream().distinct().sorted().collect(Collectors.joining(", "));
            throw new FormulaException(FormulaException.ErrorType.MISSING_VARIABLE_VALUE, varNames);
        }
        return valueList;
    }

    @NonNull
    private static List<Value> getValueRanges(final Value paramValue) {
        final List<Value> rangeValues = new ArrayList<>();
        final String varString = paramValue.getAsString();
        final List<Value> subRangeValues = parseValuesFromRange(varString);
        if (!subRangeValues.isEmpty()) {
            rangeValues.addAll(subRangeValues);
        } else {
            final Value singleVar = parseSingleValue(varString);
            if (singleVar != null) {
                rangeValues.add(singleVar);
            } else {
                throw new FormulaException(FormulaException.ErrorType.INVALID_RANGE, varString);
            }
        }
        return rangeValues;
    }

    private static void processValue(final Function<String, Value> vars, final Value paramValue, final ValueList valueList, final List<String> missingVariables) {
        if (paramValue.isNumeric()) {
            valueList.add(paramValue);
            return;
        }

        final List<Value> rangeValues = getValueRanges(paramValue);
        for (Value rv : rangeValues) {
            if (rv.isNumeric()) {
                valueList.add(rv);
            } else {
                final String varName = rv.getAsString();
                final Value calculatedVar = vars.apply(varName);
                if (calculatedVar != null) {
                    Value.assertType(calculatedVar, Value::isNumeric, "Numeric");
                    valueList.add(calculatedVar);
                } else {
                    missingVariables.add(varName);
                }
            }
        }
    }
}
