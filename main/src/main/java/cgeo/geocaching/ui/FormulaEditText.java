package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.FormulaEdittextViewBinding;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.formulas.Formula;
import cgeo.geocaching.utils.formulas.FormulaException;
import cgeo.geocaching.utils.formulas.FormulaUtils;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.formulas.VariableList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

/** Allows live-editing of a formula, optionally associated with a Variable List*/
public class FormulaEditText extends LinearLayout {

    private FormulaEdittextViewBinding binding;
    private VariableList varList;
    private BiConsumer<String, Formula> formulaChangeListener;

    public FormulaEditText(final Context context) {
        super(context);
        init();
    }

    public FormulaEditText(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FormulaEditText(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FormulaEditText(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        final ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.formula_edittext_view, this);
        binding = FormulaEdittextViewBinding.bind(this);
        binding.formulaText.addTextChangedListener(ViewUtils.createSimpleWatcher(editable -> processFormulaChange()));
        binding.formulaFunction.setOnClickListener(l -> {
            FormulaUtils.showSelectFunctionDialog(getContext(), binding.formulaText, newFormula -> processFormulaChange());
        });
    }

    @SuppressLint("SetTextI18n")
    private void processFormulaChange() {
        final String formulaString = binding.formulaText.getText().toString();
        Formula formula = null;
        try {
            formula = Formula.compile(formulaString);
            final Value value = formula.evaluate(varList == null ? x -> null : varList::getValue);
            binding.formulaResult.setText("= " + (value == null ? "?" : value.toUserDisplayableString()));
        } catch (FormulaException fe) {
            CharSequence error = "";
            if (!StringUtils.isBlank(fe.getUserDisplayableString())) {
                error = TextUtils.setSpan(" | " + fe.getUserDisplayableString(), new ForegroundColorSpan(Color.RED));
            }
            binding.formulaResult.setText(TextUtils.concat(fe.getExpressionFormatted(), error));
        }
        if (this.formulaChangeListener != null) {
            this.formulaChangeListener.accept(formulaString, formula);
        }
    }

    public void setVariableList(final VariableList varList) {
        this.varList = varList;
    }

    public void setLabel(final String label) {
        binding.formulaTextLayout.setHint(label);
    }

    public void setFormulaChangeListener(final BiConsumer<String, Formula> formulaChangeListener) {
        this.formulaChangeListener = formulaChangeListener;
    }

    public String getFormulaText() {
        return binding.formulaText.getText().toString();
    }

    public Formula getFormula() {
        return Formula.compile(getFormulaText());
    }

    public void setFormulaText(final String expression) {
        binding.formulaText.setText(expression);
        processFormulaChange();
    }

}
