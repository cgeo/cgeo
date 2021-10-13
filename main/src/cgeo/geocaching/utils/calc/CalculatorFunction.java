package cgeo.geocaching.utils.calc;

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

public enum CalculatorFunction {

    SQRT("sqrt", 0, "Square Root", null, 0,
        singleValueNumericFunction(Math::sqrt)),
    SIN("sin", 0, "Sinus", null, 0,
        singleValueNumericFunction(p -> Math.sin(Math.toRadians(p)))),
    COS("cos", 0, "Cosinus", null, 0,
        singleValueNumericFunction(p -> Math.cos(Math.toRadians(p)))),
    TAN("tan", 0, "Tangens", null, 0,
        singleValueNumericFunction(p -> Math.tan(Math.toRadians(p)))),
    ABS("abs", 0, "Absolute Value", null, 0,
        singleValueNumericFunction(Math::round)),
    ROUND("round", 0, "Round", null, 0,
        singleValueNumericFunction(Math::round)),

    RANDOM("random", 0, "Random Integer Number", null, 0, p -> Value.of(CalculatorUtils.random(p.getAsInt(0, -1), p.getAsInt(1, -1)))),

    LENGTH("length", 0, "String Length", "''", 1,
        singleValueStringFunction(String::length)),
    ROT13("rot13", 0, "Rotate String characters by 13", "''", 1,
        minMaxParamFunction(1, 1, p -> Value.of(CalculatorUtils.rot(p.get(0).getAsString(), 13)))),
    ROT("rot", 0, "Rotate String characters by parameter", "'';13", 1,
        minMaxParamFunction(1, 2, p -> Value.of(CalculatorUtils.rot(p.get(0).getAsString(), p.getAsInt(1, 13))))),
    CHECKSUM(new String[]{"checksum", "cs" }, 0, "Checksum", null, 0,
        singleValueNumericFunction(p -> CalculatorUtils.checksum(p.intValue(), false))),
    ICHECKSUM(new String[]{"ichecksum", "ics" }, 0, "Iterative Checksum", null, 0,
        singleValueNumericFunction(p -> CalculatorUtils.checksum(p.intValue(), true))),
    LETTERVALUE(new String[]{"lettervalue", "lv" }, 0, "Letter Value", "''", 1,
        singleValueStringFunction(CalculatorUtils::letterValue));


    private final String[] names;
    private final Func1<ValueList, Value> function;

    @StringRes
    private final int resId;
    private final String resFallback;

    private final String insertPattern;
    private final int insertIndex;

    private static final Map<String, CalculatorFunction> FUNC_BY_NAME = new HashMap<>();

    static {
        for (final CalculatorFunction cf : values()) {
            for (String name : cf.names) {
                FUNC_BY_NAME.put(name, cf);
            }
        }
    }

    CalculatorFunction(final String name, @StringRes final int resId, final String resFallback, final String insertPattern, final int insertIndex, final Func1<ValueList, Value> function) {
        this(new String[]{name}, resId, resFallback, insertPattern, insertIndex, function);
    }

    CalculatorFunction(final String[] names, @StringRes final int resId, final String resFallback, final String insertPattern, final int insertIndex, final Func1<ValueList, Value> function) {
        this.names = names;
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

    public Value execute(final ValueList params) throws CalculatorException {
        return function.call(params);
    }

    @Nullable
    public static CalculatorFunction findByName(final String name) {
        return name == null ? null : FUNC_BY_NAME.get(name);
    }

    public static List<CalculatorFunction> valuesAsUserDisplaySortedList() {
        final List<CalculatorFunction> list = new ArrayList<>(Arrays.asList(values()));
        Collections.sort(list, (f1, f2) -> TextUtils.COLLATOR.compare(f1.getUserDisplayableString(), f2.getUserDisplayableString()));
        return list;
    }

    private static Func1<ValueList, Value> singleValueNumericFunction(final Func1<Double, Number> numericFunction) {
        return params -> {
            params.checkCount(1, 1);
            params.checkAllDouble();
            return Value.of(numericFunction.call(params.get(0).getAsDouble()));
        };
    }

    private static Func1<ValueList, Value> minMaxParamFunction(final int min, final int max, final Func1<ValueList, Value> function) {
        return params -> {
            params.checkCount(min, max);
            return function.call(params);
        };
    }

    private static Func1<ValueList, Value> singleValueStringFunction(final Func1<String, Object> stringFunction) {
        return params -> {
            params.checkCount(1, 1);
            return Value.of(stringFunction.call(params.get(0).getAsString()));
        };
    }
}
