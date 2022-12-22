package cgeo.geocaching.utils.formulas;

import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_PARAMETER_COUNT;
import static cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulates a list of {@link cgeo.geocaching.utils.formulas.Value}'s for handling as parameter lists in {@link Formula}
 */
public class ValueList implements Iterable<Value> {

    private final List<Value> list = new ArrayList<>();

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
        return isValidIdx(idx) && list.get(idx).isString() ? list.get(idx).getAsString() : defaultValue;
    }

    public double getAsDouble(final int idx) {
        return getAsDouble(idx, 0);
    }

    public double getAsDouble(final int idx, final double defaultValue) {
        return isValidIdx(idx) && list.get(idx).isDouble() ? list.get(idx).getAsDouble() : defaultValue;
    }

    public long getAsInt(final int idx, final int defaultValue) {
        return isValidIdx(idx) && list.get(idx).isInteger() ? list.get(idx).getAsInt() : defaultValue;
    }

    private boolean isValidIdx(final int idx) {
        return idx >= 0 && idx < list.size();
    }

    public int size() {
        return list.size();
    }

    public void checkAllDouble() {
        for (Value v : list) {
            if (!v.isDouble()) {
                throw new FormulaException(WRONG_TYPE, "Number", v.toUserDisplayableString(), v.getType());
            }
        }
    }

    public void checkCount(final int minCount, final int maxCount) {
        if ((minCount > 0 && (size() < minCount)) ||
                (maxCount > 0 && (size() > maxCount))) {
            throw new FormulaException(WRONG_PARAMETER_COUNT, minCount < 0 ? "*" : "" + minCount, maxCount < 0 ? "*" : "" + maxCount, size());
        }

    }

}
