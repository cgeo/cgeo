package cgeo.geocaching.calculator;


import cgeo.geocaching.R;
import cgeo.geocaching.utils.formulas.Formula;
import static cgeo.geocaching.models.CalcState.ERROR_CHAR;
import static cgeo.geocaching.models.CalcState.ERROR_STRING;

import android.content.Context;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Data used to capture the state of this Variable such that it can be restored again later
 */
public final class VariableData implements JSONAble, Serializable {
    private final char name;
    /**
     * Note, we have to use a String rather than an Editable as Editable's can't be serialized
     */
    private String expression;

    /** cached result of the expression */
    private double cachedValue;

    /** indicates if recomputation needs to be done */
    private boolean cacheDirty;

    public VariableData(final char name) {
        this.name = name;
        expression = "";
        cacheDirty = true;
    }

    public VariableData(final char name, final String expression) {
        this.name = name;
        this.expression = expression;
        cacheDirty = true;
    }

    protected VariableData(final JSONObject json) {
        name = (char) json.optInt("name", ERROR_CHAR);
        expression = json.optString("expression", ERROR_STRING);
        cacheDirty = true;
    }

    public char getName() {
        return name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(final String newExpression) {
        expression = newExpression;
    }

    public String evaluateString(final List<VariableData> dependantVariables, final Context context) {
        if (isCacheDirty()) {
            evaluateDouble(dependantVariables);
        }

        return getCachedString(context);
    }

    /**
     * This is used to display the result in the UI
     *
     * @return cached values as a String
     */
    private String getCachedString(final Context context) {
        String returnValue = "";
        returnValue += getName();

        if (getExpression() == null || getExpression().length() == 0) {
            if (null != context) {
                returnValue = context.getString(R.string.empty_equation_result);
            }
        } else if (Double.isNaN(getCachedValue())) {
            if (null != context) {
                returnValue = context.getString(R.string.equation_error_result);
            }
        } else {
            returnValue = String.valueOf((int) getCachedValue());
        }

        return returnValue;
    }

    private double evaluateDouble(final List<VariableData> dependantVariables) {
        if (isCacheDirty()) {
            String expression = getExpression();

            if (dependantVariables != null) {
                for (final VariableData depVar : dependantVariables) {
                    expression = expression.replace(String.valueOf(depVar.getName()), "(" + depVar.evaluateDouble(null) + ")");
                }
            }

            try {
                setCachedValue(Formula.eval(expression));
            } catch (final Exception e) {
                setCachedValue(Double.NaN);
            }
        }

        return getCachedValue();
    }

    public boolean isCacheDirty() {
        return cacheDirty;
    }


    public void setCacheDirty(final boolean cacheDirty) {
        this.cacheDirty = cacheDirty;
    }

    /**
     * This is used to compute the value of another variable
     *
     * @return cached values as a Double
     */
    public double getCachedValue() {
        return cachedValue;
    }

    public void setCachedValue(final double cachedValue) {
        this.cachedValue = cachedValue;
        setCacheDirty(false);
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        final JSONObject returnValue = new JSONObject();

        returnValue.put("name", name);
        returnValue.put("expression", expression);

        return returnValue;
    }

    public void switchToLowerCase() {
        this.expression = this.expression.toLowerCase(Locale.US);
    }
}
