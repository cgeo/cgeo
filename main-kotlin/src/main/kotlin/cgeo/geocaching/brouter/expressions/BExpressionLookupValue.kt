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

/**
 * A lookup value with optional aliases
 * <p>
 * toString just gives the primary value,
 * equals just compares against primary value
 * matches() also compares aliases
 *
 * @author ab
 */
package cgeo.geocaching.brouter.expressions

import java.util.ArrayList
import java.util.List

class BExpressionLookupValue {
    public String value
    public List<String> aliases

    BExpressionLookupValue(final String value) {
        this.value = value
    }

    override     public String toString() {
        return value
    }

    public Unit addAlias(final String alias) {
        if (aliases == null) {
            aliases = ArrayList<>()
        }
        aliases.add(alias)
    }

    override     public Boolean equals(final Object o) {
        if (o is String) {
            val v: String = (String) o
            return value == (v)
        }
        if (o is BExpressionLookupValue) {
            val v: BExpressionLookupValue = (BExpressionLookupValue) o

            return value == (v.value)
        }
        return false
    }

    public Boolean matches(final String s) {
        if (value == (s)) {
            return true
        }
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias == (s)) {
                    return true
                }
            }
        }
        return false
    }

}
