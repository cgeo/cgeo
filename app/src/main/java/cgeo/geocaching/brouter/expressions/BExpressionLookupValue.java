/**
 * A lookup value with optional aliases
 * <p>
 * toString just gives the primary value,
 * equals just compares against primary value
 * matches() also compares aliases
 *
 * @author ab
 */
package cgeo.geocaching.brouter.expressions;

import java.util.ArrayList;

final class BExpressionLookupValue {
    public String value;
    public ArrayList<String> aliases;

    BExpressionLookupValue(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public void addAlias(final String alias) {
        if (aliases == null) {
            aliases = new ArrayList<>();
        }
        aliases.add(alias);
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof String) {
            final String v = (String) o;
            return value.equals(v);
        }
        if (o instanceof BExpressionLookupValue) {
            final BExpressionLookupValue v = (BExpressionLookupValue) o;

            return value.equals(v.value);
        }
        return false;
    }

    public boolean matches(final String s) {
        if (value.equals(s)) {
            return true;
        }
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias.equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }

}
