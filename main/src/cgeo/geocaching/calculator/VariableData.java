package cgeo.geocaching.calculator;

import static cgeo.geocaching.models.CalcState.ERROR_CHAR;
import static cgeo.geocaching.models.CalcState.ERROR_STRING;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Data used to capture the state of this Variable such that it can be restored again later
 */
public final class VariableData implements JSONAble {
    private final char name;
    /**
     * Note, we have to use a String rather than an Editable as Editable's can't be serialized
     */
    private String expression;

    public VariableData(final char name) {
        this.name = name;
        expression = "";
    }

    public VariableData(final char name, final String expression) {
        this.name = name;
        this.expression = expression;
    }

    protected VariableData(final JSONObject json) {
        name = (char) json.optInt("name", ERROR_CHAR);
        expression = json.optString("expression", ERROR_STRING);
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
