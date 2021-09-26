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
        singleValueNumericFunction(p -> Math.sqrt(p.doubleValue()))),
    SIN("sin", 0, "Sinus", null, 0,
        singleValueNumericFunction(p -> Math.sin(Math.toRadians(p.doubleValue())))),
    COS("cos", 0, "Cosinus", null, 0,
        singleValueNumericFunction(p -> Math.cos(Math.toRadians(p.doubleValue())))),
    TAN("tan", 0, "Tangens", null, 0,
        singleValueNumericFunction(p -> Math.tan(Math.toRadians(p.doubleValue())))),
    ABS("abs", 0, "Absolute Value", null, 0,
        singleValueNumericFunction(p -> Math.round(p.doubleValue()))),
    ROUND("round", 0, "Round", null, 0,
        singleValueNumericFunction(p -> Math.round(p.doubleValue()))),

    RANDOM("random", 0, "Random Integer Number", null, 0,
        numericFunction(CalculatorUtils::random)),

    LENGTH("length", 0, "String Length", "''", 1,
        singleValueStringFunction(String::length)),
    ROT13("rot13", 0, "Rotate String characters by 13", "''", 1,
        singleValueStringFunction(s -> CalculatorUtils.rot(s, 13))),
    ROT("rot", 0, "Rotate String characters by parameter", "'';13", 1,
        minMaxParamFunction(1, 2, p -> CalculatorUtils.rot(paramAsString(p[0]), paramAsInt(p, 1, 13)))),
    CHECKSUM(new String[]{"checksum", "cs" }, 0, "Checksum", null, 0,
        singleValueNumericFunction(p -> CalculatorUtils.checksum(p, false))),
    ICHECKSUM(new String[]{"ichecksum", "ics" }, 0, "Iterative Checksum", null, 0,
        singleValueNumericFunction(p -> CalculatorUtils.checksum(p, true))),
    LETTERVALUE(new String[]{"lettervalue", "lv" }, 0, "Letter Value", "''", 1,
        singleValueStringFunction(CalculatorUtils::letterValue));


    private final String[] names;
    private final Func1<Object[], Object> function;

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

    CalculatorFunction(final String name, @StringRes final int resId, final String resFallback, final String insertPattern, final int insertIndex, final Func1<Object[], Object> function) {
        this(new String[]{name}, resId, resFallback, insertPattern, insertIndex, function);
    }

    CalculatorFunction(final String[] names, @StringRes final int resId, final String resFallback, final String insertPattern, final int insertIndex, final Func1<Object[], Object> function) {
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

    public Object execute(final Object[] params) throws CalculatorException {
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

    private static Func1<Object[], Object> numericFunction(final Func1<Number[], Object> numericFunction) {
        return params -> numericFunction.call(CalculatorUtils.toNumericArray(params));
    }

    private static Func1<Object[], Object> singleValueNumericFunction(final Func1<Number, Object> numericFunction) {
        return params -> {
            CalculatorUtils.checkParameters(params, 1, 1);
            return numericFunction.call(CalculatorUtils.toNumericArray(params)[0]);
        };
    }

    private static Func1<Object[], Object> minMaxParamFunction(final int min, final int max, final Func1<Object[], Object> function) {
        return params -> {
            CalculatorUtils.checkParameters(params, min, max);
            return function.call(params);
        };
    }

    private static Func1<Object[], Object> singleValueStringFunction(final Func1<String, Object> stringFunction) {
        return params -> {
            CalculatorUtils.checkParameters(params, 1, 1);
            return stringFunction.call(paramAsString(params[0]));
        };
    }

    private static String paramAsString(final Object param) {
        return param.toString();
    }

    private static int paramAsInt(final Object[] params, final int idx, final int defaultValue) {
        if (params == null || idx >= params.length || params[idx] == null) {
            return defaultValue;
        }
        if (params[idx] instanceof Number) {
            return ((Number) params[idx]).intValue();
        }
        return defaultValue;
    }
}
