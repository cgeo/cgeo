package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.FormulaEdittextViewBinding;
import cgeo.geocaching.utils.formulas.Formula;
import cgeo.geocaching.utils.formulas.FormulaException;
import cgeo.geocaching.utils.formulas.FormulaUtils;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.formulas.VariableList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.function.BiConsumer;

/** Allows live-editing of a formula, optionally associated with a Variable List*/
public class FormulaEditText extends LinearLayout {

    private FormulaEdittextViewBinding binding;
    private VariableList varList;
    private BiConsumer<String, Formula> formulaChangeListener;
    private Formula formula;

    public FormulaEditText(final Context context) {
        super(context);
        init(null, 0, 0);
    }

    public FormulaEditText(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public FormulaEditText(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    public FormulaEditText(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init(final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {

        setOrientation(VERTICAL);
        final ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.formula_edittext_view, this);
        binding = FormulaEdittextViewBinding.bind(this);
        binding.formulaText.addTextChangedListener(ViewUtils.createSimpleWatcher(editable -> processFormulaChange()));
        binding.formulaFunction.setOnClickListener(l -> {
            FormulaUtils.showSelectFunctionDialog(getContext(), binding.formulaText, newFormula -> processFormulaChange());
        });

        ViewUtils.consumeAttributes(getContext(), attrs, R.styleable.FormulaEditText, defStyleAttr, defStyleRes, typedArray -> {
            setHint(typedArray.getString(R.styleable.FormulaEditText_hint));
        });

        processFormulaChange();
    }

    @SuppressLint("SetTextI18n")
    private void ensureFormula() {
        final String formulaString = binding.formulaText.getText().toString();
        try {
            this.formula = Formula.compile(formulaString);
            final Value value = formula.evaluate(varList == null ? x -> null : varList::getValue);
            binding.formulaResult.setText("= " + (value == null ? "?" : value.toUserDisplayableString()));
        } catch (FormulaException fe) {
            this.formula = null;
            binding.formulaResult.setText(fe.getUserDisplayableString());
        }
    }

    private void processFormulaChange() {
        ensureFormula();
        if (this.formulaChangeListener != null) {
            final String formulaString = binding.formulaText.getText().toString();
            this.formulaChangeListener.accept(formulaString, formula);
        }
    }

    public void setVariableList(final VariableList varList) {
        this.varList = varList;
    }

    public void setHint(final String hint) {
        binding.formulaTextLayout.setHint(hint);
    }

    public void setFormulaChangeListener(final BiConsumer<String, Formula> formulaChangeListener) {
        this.formulaChangeListener = formulaChangeListener;
    }

    public String getFormulaText() {
        return binding.formulaText.getText().toString();
    }

    public Value getValue() {
        try {
            if (formula == null) {
                ensureFormula();
            }
            return formula == null ? null : formula.evaluate(varList == null ? x -> null : varList::getValue);
        } catch (FormulaException fe) {
            return null;
        }
    }

    public Double getValueAsDouble() {
        final Value value = getValue();
        return value == null || !value.isDouble() ? null : value.getAsDouble();
    }

    public void setFormulaText(final String expression) {
        binding.formulaText.setText(expression);
        processFormulaChange();
    }

}
