package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.calculator.CalculationUtils;
import cgeo.geocaching.calculator.VariableData;
import cgeo.geocaching.settings.Settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;

import java.util.List;

/**
 * Class used to display a variable with an equation, such as: X = a^2 = b^2
 */
public class CalculatorVariable extends LinearLayout {

    /** actual name and expression used */
    private final VariableData variableData;

    /** view to display the name of the variable */
    private final TextView name;

    /** view to display the expression of the variable */
    private final EditText expression;

    /** cached result of the expression */
    private double cachedValue;

    /** indicates if recomputation needs to be done */
    private boolean cacheDirty;


    @SuppressLint("SetTextI18n")
    public CalculatorVariable(final Context context, final VariableData variableData, final String hintText, final TextWatcher textWatcher, final InputFilter[] filter) {
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
        name.setText(variableData.getName() + " = ");

        expression = new EditText(context);
        expression.setLayoutParams(new LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        expression.setMaxLines(Integer.MAX_VALUE);
        expression.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        expression.setHint(hintText);
        expression.setText(variableData.getExpression());

        // Note two 'TextWatchers' are added.  An internal one to update the Variable itself
        // and another external watcher to propagate the changes more widely.
        expression.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                // Intentionally left empty
            }

            /**
             * Only use afterTextChanged
             */
            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                // Intentionally left empty
            }

            @Override
            public void afterTextChanged(final Editable s) {
                setCacheDirty();
                CalculatorVariable.this.variableData.expression = s.toString();
            }
        });
        expression.addTextChangedListener(textWatcher);
        expression.setFilters(filter);

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

    /**
     * This is used to display the result in the UI
     *
     * @return cached values as a String
     */
    private String getCachedString() {
        final String returnValue;

        if (variableData.getExpression() == null || variableData.getExpression().length() == 0) {
            returnValue = getContext().getString(R.string.empty_equation_result);
        } else if (Double.isNaN(getCachedValue())) {
            returnValue = getContext().getString(R.string.equation_error_result);
        } else {
            returnValue = String.valueOf((int) getCachedValue());
        }

        return returnValue;
    }

    /**
     * This is used to compute the value of another variable
     *
     * @return cached values as a Double
     */
    private double getCachedValue() {
        return cachedValue;
    }

    private void setCachedValue(final double cachedValue) {
        this.cachedValue = cachedValue;
        setCacheDirty(false);

        final boolean lightSkin = Settings.isLightSkin();
        final int validColour = ContextCompat.getColor(getContext(), lightSkin ? R.color.text_light : R.color.text_dark);
        final int invalidColour = ContextCompat.getColor(getContext(), lightSkin ? R.color.text_hint_light : R.color.text_hint_dark);

        // Make the name colour grey if value is invalid
        name.setTextColor(Double.isNaN(getCachedValue()) ? invalidColour : validColour);
    }

    public char getName() {
        return variableData.getName();
    }

    public String getExpression() {
        return variableData.getExpression();
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
                setCachedValue(new CalculationUtils(expression).eval());
            } catch (final Exception e) {
                setCachedValue(Double.NaN);
            }
        }

        return getCachedValue();
    }
}
