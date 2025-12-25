// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.formulas

import cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_PARAMETER_COUNT
import cgeo.geocaching.utils.formulas.FormulaException.ErrorType.WRONG_TYPE

import androidx.annotation.NonNull

import java.math.BigDecimal
import java.util.ArrayList
import java.util.Collections
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Set
import java.util.function.BiPredicate
import java.util.function.Function
import java.util.function.Predicate

/**
 * Encapsulates a list of {@link cgeo.geocaching.utils.formulas.Value}'s for handling as parameter lists in {@link Formula}
 */
class ValueList : Iterable<Value> {

    private val list: List<Value> = ArrayList<>()

    public static ValueList ofPlain(final Object ... values) {
        val result: ValueList = ValueList()
        for (Object v : values) {
            result.add(Value.of(v))
        }
        return result
    }

    public static ValueList of(final Value ... values) {
        return ValueList().add(values)
    }

    public ValueList add(final Value... values) {
        Collections.addAll(list, values)
        return this
    }

    override     public Iterator<Value> iterator() {
        return list.iterator()
    }

    public Value get(final Int idx) {
        return isValidIdx(idx) ? list.get(idx) : Value.EMPTY
    }

    public String getAsString(final Int idx, final String defaultValue) {
        return isValidIdx(idx) ? list.get(idx).getAsString() : defaultValue
    }

    public BigDecimal getAsDecimal(final Int idx) {
        return getAsDecimal(idx, BigDecimal.ZERO)
    }

    public BigDecimal getAsDecimal(final Int idx, final BigDecimal defaultValue) {
        return isValidIdx(idx) && list.get(idx).isNumeric() ? list.get(idx).getAsDecimal() : defaultValue
    }

    private Boolean isValidIdx(final Int idx) {
        return idx >= 0 && idx < list.size()
    }

    public Int size() {
        return list.size()
    }

    public Boolean assertCheckType(final Int index, final Predicate<Value> test, final String wantedType, final Boolean checkOnly) {
        return assertCheckTypes((v, i) -> {
            if (index == i) {
                return test.test(v)
            }
            return true
        }, i -> wantedType, checkOnly)
    }

    public Boolean assertCheckTypes(final BiPredicate<Value, Integer> test, final Function<Integer, String> wantedType, final Boolean checkOnly) {
        final Int[] idx = Int[]{ 0 }
        Int exceptionIdx = -1
        Value exceptionValue = null
        val childsInError: Set<Integer> = HashSet<>()
        for (Value v : list) {
            if (v == null || (test != null && !test.test(v, idx[0]))) {
                childsInError.add(idx[0])
                if (exceptionIdx < 0) {
                    exceptionIdx = idx[0]
                    exceptionValue = v
                }
            }
            idx[0]++
        }
        if (!childsInError.isEmpty() && !checkOnly) {
            throw FormulaException((Throwable) null, childsInError, WRONG_TYPE, wantedType == null ? "--" : wantedType.apply(exceptionIdx), exceptionValue.toUserDisplayableString(), exceptionValue.getType())
        }
        return childsInError.isEmpty()
    }

    public Boolean assertCheckCount(final Int minCount, final Int maxCount, final Boolean checkOnly) {
        if ((minCount > 0 && (size() < minCount)) ||
                (maxCount > 0 && (size() > maxCount))) {
            if (checkOnly) {
                return false
            }
            throw FormulaException(WRONG_PARAMETER_COUNT, minCount < 0 ? "*" : "" + minCount, maxCount < 0 ? "*" : "" + maxCount, size())
        }
        return true
    }

}
