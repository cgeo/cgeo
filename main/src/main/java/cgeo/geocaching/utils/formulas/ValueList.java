package cgeo.geocaching.utils.formulas;

import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_PARAMETER_COUNT;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE;

import androidx.annotation.NonNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Encapsulates a list of {@link cgeo.geocaching.utils.formulas.Value}'s for handling as parameter lists in {@link Formula}
 */
public class ValueList implements Iterable<Value> {

    private final List<Value> list = new ArrayList<>();

    public static ValueList ofPlain(final Object ... values) {
        final ValueList result = new ValueList();
        for (Object v : values) {
            result.add(Value.of(v));
        }
        return result;
    }

    public static ValueList of(final Value ... values) {
        return new ValueList().add(values);
    }

    public ValueList add(final Value... values) {
        Collections.addAll(list, values);
        return this;
    }

    @NonNull
    @Override
    public Iterator<Value> iterator() {
        return list.iterator();
    }

    @NonNull
    public Value get(final int idx) {
        return isValidIdx(idx) ? list.get(idx) : Value.EMPTY;
    }

    @NonNull
    public String getAsString(final int idx, final String defaultValue) {
        return isValidIdx(idx) ? list.get(idx).getAsString() : defaultValue;
    }

    public BigDecimal getAsDecimal(final int idx) {
        return getAsDecimal(idx, BigDecimal.ZERO);
    }

    public BigDecimal getAsDecimal(final int idx, final BigDecimal defaultValue) {
        return isValidIdx(idx) && list.get(idx).isNumeric() ? list.get(idx).getAsDecimal() : defaultValue;
    }

    private boolean isValidIdx(final int idx) {
        return idx >= 0 && idx < list.size();
    }

    public int size() {
        return list.size();
    }

    public boolean assertCheckType(final int index, final Predicate<Value> test, final String wantedType, final boolean checkOnly) {
        return assertCheckTypes((v, i) -> {
            if (index == i) {
                return test.test(v);
            }
            return true;
        }, i -> wantedType, checkOnly);
    }

    public boolean assertCheckTypes(final BiPredicate<Value, Integer> test, final Function<Integer, String> wantedType, final boolean checkOnly) {
        final int[] idx = new int[]{ 0 };
        int exceptionIdx = -1;
        Value exceptionValue = null;
        final Set<Integer> childsInError = new HashSet<>();
        for (Value v : list) {
            if (v == null || (test != null && !test.test(v, idx[0]))) {
                childsInError.add(idx[0]);
                if (exceptionIdx < 0) {
                    exceptionIdx = idx[0];
                    exceptionValue = v;
                }
            }
            idx[0]++;
        }
        if (!childsInError.isEmpty() && !checkOnly) {
            throw new FormulaException((Throwable) null, childsInError, WRONG_TYPE, wantedType == null ? "--" : wantedType.apply(exceptionIdx), exceptionValue.toUserDisplayableString(), exceptionValue.getType());
        }
        return childsInError.isEmpty();
    }

    public boolean assertCheckCount(final int minCount, final int maxCount, final boolean checkOnly) {
        if ((minCount > 0 && (size() < minCount)) ||
                (maxCount > 0 && (size() > maxCount))) {
            if (checkOnly) {
                return false;
            }
            throw new FormulaException(WRONG_PARAMETER_COUNT, minCount < 0 ? "*" : "" + minCount, maxCount < 0 ? "*" : "" + maxCount, size());
        }
        return true;
    }

}
