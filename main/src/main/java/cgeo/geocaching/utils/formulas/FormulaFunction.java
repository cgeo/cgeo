package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.OTHER;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Maintains all functions available in {@link Formula}'s
 */
public enum FormulaFunction {

    SQRT(new String[]{"sqrt"}, FunctionGroup.SIMPLE_NUMERIC, R.string.formula_function_sqrt, "Square Root", null, 0,
            singleValueDoubleFunction(Math::sqrt)),
    SIN(new String[]{"sin"}, FunctionGroup.SIMPLE_NUMERIC, R.string.formula_function_sin, "Sinus", null, 0,
            singleValueDoubleFunction(p -> Math.sin(Math.toRadians(p)))),
    COS(new String[]{"cos"}, FunctionGroup.SIMPLE_NUMERIC, R.string.formula_function_cos, "Cosinus", null, 0,
            singleValueDoubleFunction(p -> Math.cos(Math.toRadians(p)))),
    TAN(new String[]{"tan"}, FunctionGroup.SIMPLE_NUMERIC, R.string.formula_function_tan, "Tangens", null, 0,
            singleValueDoubleFunction(p -> Math.tan(Math.toRadians(p)))),
    ABS(new String[]{"abs"}, FunctionGroup.SIMPLE_NUMERIC, R.string.formula_function_abs, "Absolute Value", null, 0,
            singleValueNumericFunction(v -> v.getAsDecimal().abs())),
    ROUND(new String[]{"round", "rd"}, FunctionGroup.SIMPLE_NUMERIC, R.string.formula_function_round, "Round", null, 0,
            valueList -> FormulaUtils.truncRound(valueList, false)),
    TRUNC(new String[]{"trunc", "tr", "floor", "fl"}, FunctionGroup.SIMPLE_NUMERIC, R.string.formula_function_trunc, "Trunc", null, 0,
            valueList -> FormulaUtils.truncRound(valueList, true)),
    IF(new String[]{"if"}, FunctionGroup.COMPLEX_NUMERIC, R.string.formula_function_if, "If", null, 0,
            FormulaUtils::ifFunction),
    LENGTH(new String[]{"length", "len"}, FunctionGroup.SIMPLE_STRING, R.string.formula_function_length, "String Length", "''", 1,
            singleValueStringFunction(String::length)),
    SUBSTRING(new String[]{"substring", "substr", "sub"}, FunctionGroup.SIMPLE_STRING, R.string.formula_function_substring, "Substring", "'';0;1", 1,
            vl -> FormulaUtils.substring(false, vl)),
    CHARS(new String[]{"chars", "ch"}, FunctionGroup.SIMPLE_STRING, R.string.formula_function_chars, "Select Chars", "'';1;2", 1,
        FormulaUtils::selectChars),

    ROT13(new String[]{"rot13"}, FunctionGroup.COMPLEX_STRING, R.string.formula_function_rot13, "Rotate characters by 13", "''", 1,
            valueList -> FormulaUtils.rot(valueList, true)),
    ROT(new String[]{"rot"}, FunctionGroup.COMPLEX_STRING, R.string.formula_function_rot, "Rotate characters by x", "'';13", 1,
            valueList -> FormulaUtils.rot(valueList, false)),
    CHECKSUM(new String[]{"checksum", "cs"}, FunctionGroup.COMPLEX_NUMERIC, R.string.formula_function_checksum, "Checksum", null, 0,
            valueList -> FormulaUtils.checksum(valueList, false)),
    ICHECKSUM(new String[]{"ichecksum", "ics"}, FunctionGroup.COMPLEX_NUMERIC, R.string.formula_function_ichecksum, "Iterative Checksum", null, 0,
            valueList -> FormulaUtils.checksum(valueList, true)),
    LETTERVALUE(new String[]{"lettervalue", "lv", "wordvalue", "wv", "bww"}, FunctionGroup.COMPLEX_STRING, R.string.formula_function_lettervalue, "Letter Value", "''", 1,
            singleValueStringFunction(FormulaUtils::letterValue)),
    ROMAN(new String[]{"roman"}, FunctionGroup.COMPLEX_STRING, R.string.formula_function_roman, "Roman", "''", 1,
            singleValueStringFunction(FormulaUtils::roman)),
    VANITY(new String[]{"vanity", "vanitycode", "vc"}, FunctionGroup.COMPLEX_STRING, R.string.formula_function_vanity, "Vanity", "''", 1,
            singleValueStringFunction(FormulaUtils::vanity));

    public enum FunctionGroup {
        SIMPLE_NUMERIC(R.string.formula_function_group_simplenumeric, "Simple Numeric"),
        COMPLEX_NUMERIC(R.string.formula_function_group_complexnumeric, "Complex Numeric"),
        SIMPLE_STRING(R.string.formula_function_group_simplestring, "Simple String"),
        COMPLEX_STRING(R.string.formula_function_group_complexstring, "Complex String");

        @StringRes
        private final int resId;
        private final String resFallback;

        FunctionGroup(@StringRes final int resId, final String resFallback) {
            this.resId = resId;
            this.resFallback = resFallback;
        }

        public String getUserDisplayableString() {
            return LocalizationUtils.getStringWithFallback(this.resId, this.resFallback);
        }
    }


    private final String[] names;
    private final Function<ValueList, Object> function;

    private final FunctionGroup group;

    @StringRes
    private final int resId;
    private final String resFallback;

    private final String insertPattern;
    private final int insertIndex;

    private static final Map<String, FormulaFunction> FUNC_BY_NAME = new HashMap<>();

    static {
        for (final FormulaFunction cf : values()) {
            for (String name : cf.names) {
                FUNC_BY_NAME.put(name, cf);
            }
        }
    }


    FormulaFunction(final String[] names, final FunctionGroup group, @StringRes final int resId, final String resFallback, final String insertPattern, final int insertIndex, final Function<ValueList, Object> function) {
        this.names = names;
        this.group = group;
        this.function = function;
        this.resId = resId;
        this.resFallback = resFallback;
        this.insertPattern = insertPattern;
        this.insertIndex = insertIndex;
    }

    public String[] getNames() {
        return this.names;
    }

    public String getMainName() {
        return this.names[0];
    }

    public String getUserDisplayableString() {
        return LocalizationUtils.getStringWithFallback(this.resId, this.resFallback);
    }

    public String getFunctionInsertString() {
        return getMainName() + "(" + (insertPattern == null ? "" : insertPattern) + ")";
    }

    public int getFunctionInsertCursorPosition() {
        return getMainName().length() + 1 + Math.max(insertIndex, 0);
    }

    public FunctionGroup getGroup() {
        return group;
    }

    public Value execute(final ValueList params) throws FormulaException {
        try {
            final Object result = function.apply(params);
            return result instanceof Value ? (Value) result : Value.of(result);
        } catch (FormulaException fe) {
            throw fe;
        } catch (RuntimeException re) {
            throw new FormulaException(re, null, OTHER, re.getMessage());
        }
    }

    @Nullable
    public static FormulaFunction findByName(final String name) {
        return name == null ? null : FUNC_BY_NAME.get(name);
    }

    public static List<FormulaFunction> valuesAsUserDisplaySortedList() {
        final List<FormulaFunction> list = new ArrayList<>(Arrays.asList(values()));
        Collections.sort(list, (f1, f2) -> TextUtils.COLLATOR.compare(f1.getUserDisplayableString(), f2.getUserDisplayableString()));
        return list;
    }

    private static Function<ValueList, Object> singleValueNumericFunction(final Function<Value, Number> numericFunction) {
        return params -> {
            params.assertCheckCount(1, 1, false);
            params.assertCheckTypes((v, i) -> v.isNumeric(), i -> "Numeric", false);
            try {
                return Value.of(numericFunction.apply(params.get(0)));
            } catch (ArithmeticException ae) {
                throw new FormulaException(FormulaException.ErrorType.NUMERIC_OVERFLOW);
            }
        };
    }

    private static Function<ValueList, Object> singleValueDoubleFunction(final Function<Double, Number> numericFunction) {
        return params -> {
            params.assertCheckCount(1, 1, false);
            params.assertCheckTypes((v, i) -> v.isDouble(), i -> "Numeric double", false);
            try {
                return Value.of(numericFunction.apply(params.get(0).getAsDouble()));
            } catch (ArithmeticException ae) {
                throw new FormulaException(FormulaException.ErrorType.NUMERIC_OVERFLOW);
            }
        };
    }

    private static Function<ValueList, Object> singleValueStringFunction(final Function<String, Object> stringFunction) {
        return params -> {
            params.assertCheckCount(1, 1, false);
            return Value.of(stringFunction.apply(params.get(0).getAsString()));
        };
    }
}
