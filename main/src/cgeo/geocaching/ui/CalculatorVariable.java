package cgeo.geocaching.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.List;

import cgeo.geocaching.R;

import static cgeo.geocaching.models.CalcState.ERROR_CHAR;
import static cgeo.geocaching.models.CalcState.ERROR_STRING;

/**
 * Class used to display a variable with an equation, such as:
 *  X = a^2 = b^2
 */
public class CalculatorVariable extends LinearLayout {

    private final VariableData variableData;

    // Views used to display the name and expression of the variable.
    // Note that the actual name and expression used are stored in the variableData 'struct'
    // and these views are only used to display that information
    private final TextView name;
    private final EditText expression;

    // Variables used to save unnecessary re-computing.
    private double cachedValue;
    private boolean cacheDirty;

    // Data used to capture the state of this Variable such that it can be restored again later.
    public static class VariableData implements Serializable, JSONAble {
        private final char name;
        private String expression; // Note, we have to use a String rather than an Editable as Editable's can't be serialized

        public VariableData(final char name) {
            this.name = name;
            expression = "";
        }

        private VariableData(final JSONObject json) {
            name = (char) json.optInt("name", ERROR_CHAR);
            expression = json.optString("expression", ERROR_STRING);
        }

        public char getName() {
            return name;
        }

        public JSONObject toJSON() throws JSONException {
            final JSONObject returnValue = new JSONObject();

            returnValue.put("name", name);
            returnValue.put("expression", expression);

            return returnValue;
        }
    }

    public static class VariableDataFactory implements JSONAbleFactory {
        @Override
        public JSONAble fromJSON(final JSONObject json) {
            return new VariableData(json);
        }
    }

    public CalculatorVariable(final Context context, final VariableData variableData, final String hintText, final TextWatcher textWatcher) {
        super(context);
        this.variableData = variableData;
        cacheDirty = true;
        setLayoutParams(new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)));

        final int variableSpacingGap = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        setPadding(0, variableSpacingGap, 0, variableSpacingGap);

        name = new TextView(context);
        name.setWidth(getResources().getDimensionPixelSize(R.dimen.equation_name_width));
        name.setGravity(Gravity.RIGHT);
        name.setTextSize(22);
        name.setTypeface(name.getTypeface(), Typeface.BOLD);
        name.setText(variableData.name + " = ");

        expression = new EditText(context);
        expression.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        expression.setMaxLines(Integer.MAX_VALUE);
        expression.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        expression.setHint(hintText);
        expression.setText(variableData.expression);

        // Note two 'TextWatchers' are added.  An internal one to update the Variable itself
        // and another external watcher to propagate the changes more widely.
        expression.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                // Intentionally left empty
            }

            @Override
            // Only use afterTextChanged
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                // Intentionally left empty
            }

            @Override
            public void afterTextChanged(final Editable s) {
                setCacheDirty();
                CalculatorVariable.this.variableData.expression = expression.getText().toString();
            }
        });
        expression.addTextChangedListener(textWatcher);

        addView(name);
        addView(expression);
    }

    @Override
    public void setNextFocusDownId(final int nextFocusDownId) {
        super.setNextFocusDownId(nextFocusDownId);

        expression.setNextFocusDownId(nextFocusDownId);
    }

    public VariableData getData() {
        return variableData;
    }

    private boolean isCacheDirty() {
        return cacheDirty;
    }

    public void setCacheDirty() {
        setCacheDirty(true);
    }

    private void setCacheDirty(final boolean cacheDirty) {
        this.cacheDirty = cacheDirty;
    }

    // Method to returned the cached values as a String
    // This is used to display the result in the UI.
    private String getCachedString() {
        final String returnValue;

        if (variableData.expression == null || variableData.expression.length() == 0) {
            returnValue = getContext().getString(R.string.empty_equation_result);
        } else if (Double.isNaN(getCachedValue())) {
            returnValue = getContext().getString(R.string.equation_error_result);
        } else {
            returnValue = String.valueOf((int) getCachedValue());
        }

        return returnValue;
    }

    // Method to returned the cached values as a Double
    // This is used to compute the value of another variable.
    private double getCachedValue() {
        return cachedValue;
    }

    private void setCachedValue(final double cachedValue) {
        this.cachedValue = cachedValue;
        setCacheDirty(false);

        // Make the name colour grey if value is invalid
        final int nameColour = Double.isNaN(getCachedValue()) ? ContextCompat.getColor(getContext(), R.color.text_hint_dark) : Color.WHITE;
        name.setTextColor(nameColour);
    }

    public char getName() {
        return variableData.name;
    }

    public String getExpression() {
        return variableData.expression;
    }

    public int getExpressionId() {
        return expression.getId();
    }

    public void setExpressionId(final int id) {
        expression.setId(id);
    }

    public String evaluateString(final List<CalculatorVariable> dependantVariables) {
        if (isCacheDirty()) {
            evaluateDouble(dependantVariables);
        }

        return getCachedString();
    }

    private double evaluateDouble(final List<CalculatorVariable> dependantVariables) {
        if (isCacheDirty()) {
            String expression = getExpression();

            if (dependantVariables != null) {
                for (final CalculatorVariable depVar : dependantVariables) {
                    expression = expression.replace(String.valueOf(depVar.getName()), "(" + depVar.evaluateDouble(null) + ")");
                }
            }

            try {
                setCachedValue(cgeo.geocaching.utils.CalculationUtils.eval(expression));
            } catch (final Exception e) {
                setCachedValue(Double.NaN);
            }
        }

        return getCachedValue();
    }

}
