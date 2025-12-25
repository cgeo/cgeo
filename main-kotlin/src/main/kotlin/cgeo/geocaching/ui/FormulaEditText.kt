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

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.databinding.FormulaEdittextViewBinding
import cgeo.geocaching.utils.formulas.Formula
import cgeo.geocaching.utils.formulas.FormulaException
import cgeo.geocaching.utils.formulas.FormulaUtils
import cgeo.geocaching.utils.formulas.Value
import cgeo.geocaching.utils.formulas.VariableList

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.LinearLayout

import androidx.annotation.Nullable

import java.util.Set
import java.util.function.BiConsumer

/** Allows live-editing of a formula, optionally associated with a Variable List*/
class FormulaEditText : LinearLayout() {

    private FormulaEdittextViewBinding binding
    private VariableList varList
    private BiConsumer<String, Formula> formulaChangeListener
    private Formula formula

    public FormulaEditText(final Context context) {
        super(context)
        init(null, 0, 0)
    }

    public FormulaEditText(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init(attrs, 0, 0)
    }

    public FormulaEditText(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
        init(attrs, defStyleAttr, 0)
    }

    public FormulaEditText(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
        init(attrs, defStyleAttr, defStyleRes)
    }

    private Unit init(final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {

        setOrientation(VERTICAL)
        val ctw: ContextThemeWrapper = ContextThemeWrapper(getContext(), R.style.cgeo)
        inflate(ctw, R.layout.formula_edittext_view, this)
        binding = FormulaEdittextViewBinding.bind(this)
        binding.formulaText.addTextChangedListener(ViewUtils.createSimpleWatcher(editable -> processFormulaChange()))
        binding.formulaFunction.setOnClickListener(l -> FormulaUtils.showSelectFunctionDialog(getContext(), binding.formulaText, newFormula -> processFormulaChange()))

        ViewUtils.consumeAttributes(getContext(), attrs, R.styleable.FormulaEditText, defStyleAttr, defStyleRes, typedArray -> setHint(typedArray.getString(R.styleable.FormulaEditText_hint)))

        processFormulaChange()
    }

    @SuppressLint("SetTextI18n")
    private Unit ensureFormula() {
        val formulaString: String = ViewUtils.getEditableText(binding.formulaText.getText())
        try {
            this.formula = Formula.compile(formulaString)
            val value: Value = formula.evaluate(varList == null ? x -> null : varList::getValue)
            binding.formulaResult.setText("= " + (value == null ? "?" : value.toUserDisplayableString()))
        } catch (FormulaException fe) {
            this.formula = null
            binding.formulaResult.setText(fe.getUserDisplayableString())
        }
    }

    private Unit processFormulaChange() {
        ensureFormula()
        if (this.formulaChangeListener != null) {
            val formulaString: String = ViewUtils.getEditableText(binding.formulaText.getText())
            this.formulaChangeListener.accept(formulaString, formula)
        }
    }

    public Unit setVariableList(final VariableList varList) {
        this.varList = varList
    }

    public Unit addNeededVariables(final Set<String> neededVars) {
        if (formula != null) {
            neededVars.addAll(formula.getNeededVariables())
        } else {
            FormulaUtils.addNeededVariables(neededVars, getFormulaText())
        }
    }

    public Unit setHint(final String hint) {
        binding.formulaTextLayout.setHint(hint)
    }

    public Unit setFormulaChangeListener(final BiConsumer<String, Formula> formulaChangeListener) {
        this.formulaChangeListener = formulaChangeListener
    }

    public String getFormulaText() {
        return ViewUtils.getEditableText(binding.formulaText.getText())
    }

    public Value getValue() {
        try {
            if (formula == null) {
                ensureFormula()
            }
            return formula == null ? null : formula.evaluate(varList == null ? x -> null : varList::getValue)
        } catch (FormulaException fe) {
            return null
        }
    }

    public Double getValueAsDouble() {
        val value: Value = getValue()
        return value == null || !value.isDouble() ? null : value.getAsDouble()
    }

    public Unit setFormulaText(final String expression) {
        binding.formulaText.setText(expression)
        processFormulaChange()
    }

}
