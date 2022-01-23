package cgeo.geocaching.utils.formulas;

import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Func1;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maintains all functions available in {@link Formula}'s */
public enum FormulaFunction {

    SQRT("sqrt", FunctionGroup.SIMPLE_NUMERIC, 0, "Square Root", null, 0,
        singleValueNumericFunction(Math::sqrt)),
    SIN("sin", FunctionGroup.SIMPLE_NUMERIC, 0, "Sinus", null, 0,
        singleValueNumericFunction(p -> Math.sin(Math.toRadians(p)))),
    COS("cos", FunctionGroup.SIMPLE_NUMERIC, 0, "Cosinus", null, 0,
        singleValueNumericFunction(p -> Math.cos(Math.toRadians(p)))),
    TAN("tan", FunctionGroup.SIMPLE_NUMERIC, 0, "Tangens", null, 0,
        singleValueNumericFunction(p -> Math.tan(Math.toRadians(p)))),
    ABS("abs", FunctionGroup.SIMPLE_NUMERIC, 0, "Absolute Value", null, 0,
        singleValueNumericFunction(Math::abs)),
    ROUND("round", FunctionGroup.SIMPLE_NUMERIC, 0, "Round", null, 0,
        p -> Value.of(FormulaUtils.round(p.getAsDouble(0, -1), p.getAsInt(1, 0)))),

    IF("if", FunctionGroup.COMPLEX_NUMERIC, 0, "If", null, 0,
        minMaxParamFunction(2, -1, FormulaUtils::ifFunction)),

    LENGTH("length", FunctionGroup.SIMPLE_STRING, 0, "String Length", "''", 1,
        singleValueStringFunction(String::length)),
    SUBSTRING(new String[]{"substring", "sub"}, FunctionGroup.SIMPLE_STRING, 0, "Substring", "'';0;1", 1,
        minMaxParamFunction(1, 3, p -> FormulaUtils.substring(p.getAsString(0, ""), p.getAsInt(1, 0), p.getAsInt(2, 1)))),

    ROT13("rot13", FunctionGroup.COMPLEX_STRING, 0, "Rotate characters by 13", "''", 1,
        minMaxParamFunction(1, 1, p -> Value.of(FormulaUtils.rot(p.get(0).getAsString(), 13)))),
    ROT("rot", FunctionGroup.COMPLEX_STRING, 0, "Rotate characters by x", "'';13", 1,
        minMaxParamFunction(1, 2, p -> Value.of(FormulaUtils.rot(p.get(0).getAsString(), p.getAsInt(1, 13))))),
    CHECKSUM(new String[]{"checksum", "cs" }, FunctionGroup.COMPLEX_NUMERIC, 0, "Checksum", null, 0,
        minMaxParamFunction(1, 1,  p -> FormulaUtils.valueChecksum(p.get(0), false))),
    ICHECKSUM(new String[]{"ichecksum", "ics" }, FunctionGroup.COMPLEX_NUMERIC, 0, "Iterative Checksum", null, 0,
        minMaxParamFunction(1, 1,  p -> FormulaUtils.valueChecksum(p.get(0), true))),
    LETTERVALUE(new String[]{"lettervalue", "lv", "wordvalue", "wv", "bww" }, FunctionGroup.COMPLEX_STRING, 0, "Letter Value", "''", 1,
        singleValueStringFunction(FormulaUtils::letterValue)),
    ROMAN("roman", FunctionGroup.COMPLEX_STRING, 0, "Roman", "''", 1,
        singleValueStringFunction(FormulaUtils::roman)),
    VANITY(new String[]{ "vanity", "vanitycode", "vc" }, FunctionGroup.COMPLEX_STRING, 0, "Roman", "''", 1,
          singleValueStringFunction(FormulaUtils::vanity));

    public enum FunctionGroup {
        SIMPLE_NUMERIC(0, "Simple Numeric"),
        COMPLEX_NUMERIC(0, "Complex Numeric"),
        SIMPLE_STRING(0, "Simple String"),
        COMPLEX_STRING(0, "Complex String");

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
    private final Func1<ValueList, Object> function;

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

    FormulaFunction(final String name, final FunctionGroup group, @StringRes final int resId, final String resFallback, final String insertPattern, final int insertIndex, final Func1<ValueList, Object> function) {
        this(new String[]{name}, group, resId, resFallback, insertPattern, insertIndex, function);
    }

    FormulaFunction(final String[] names, final FunctionGroup group, @StringRes final int resId, final String resFallback, final String insertPattern, final int insertIndex, final Func1<ValueList, Object> function) {
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
        final Object result = function.call(params);
        return result instanceof Value ? (Value) result : Value.of(result);
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

    private static Func1<ValueList, Object> singleValueNumericFunction(final Func1<Double, Number> numericFunction) {
        return params -> {
            params.checkCount(1, 1);
            params.checkAllDouble();
            return Value.of(numericFunction.call(params.get(0).getAsDouble()));
        };
    }

    private static Func1<ValueList, Object> minMaxParamFunction(final int min, final int max, final Func1<ValueList, Object> function) {
        return params -> {
            params.checkCount(min, max);
            return function.call(params);
        };
    }

    private static Func1<ValueList, Object> singleValueStringFunction(final Func1<String, Object> stringFunction) {
        return params -> {
            params.checkCount(1, 1);
            return Value.of(stringFunction.call(params.get(0).getAsString()));
        };
    }
}
