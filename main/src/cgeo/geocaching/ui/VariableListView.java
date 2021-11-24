package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.databinding.VariableListItemBinding;
import cgeo.geocaching.databinding.VariableListViewBinding;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.calc.CalculatorFunction;
import cgeo.geocaching.utils.calc.CalculatorMap;
import cgeo.geocaching.utils.calc.Value;
import cgeo.geocaching.utils.calc.VariableList;
import cgeo.geocaching.utils.functions.Action2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class VariableListView extends LinearLayout {

    private static final Pattern VARNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

    private VariablesListAdapter adapter;

    private static class VariableViewHolder extends RecyclerView.ViewHolder {
        private final VariableListItemBinding binding;
        private String varName;

        VariableViewHolder(final View rowView) {
            super(rowView);
            binding = VariableListItemBinding.bind(rowView);
        }

        private void setData(final CalculatorMap.CalculatorState calculatorState, final boolean isAdvanced) {

            this.varName = calculatorState.getVar();
            final String displayVarName = VariableList.isVisible(this.varName) ? this.varName : "-";
            this.binding.variableName.setText(displayVarName);
            final int textSize;
            switch (displayVarName.length()) {
                case 1:
                    textSize = 40;
                    break;
                case 2:
                    textSize = 25;
                    break;
                default:
                    textSize = 14;
                    break;
            }
            this.binding.variableName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);

            final EditText formula = Objects.requireNonNull(this.binding.variableFormula.getEditText());
            formula.setText(calculatorState.getFormula());

            setResult(calculatorState);

            this.binding.variableDelete.setVisibility(isAdvanced ? VISIBLE : GONE);
            this.binding.variableResult.setVisibility(isAdvanced ? VISIBLE : GONE);
            this.binding.variableFunction.setVisibility(isAdvanced ? VISIBLE : GONE);
        }

        @SuppressLint("SetTextI18n")
        public void setResult(final CalculatorMap.CalculatorState calculatorState) {

            final boolean isError = calculatorState.getError() != null;
            if (isError) {
                this.binding.variableResult.setText(calculatorState.getError());
                this.binding.variableResult.setTextColor(Color.RED);
            } else {
                final Value result = calculatorState.getResult();
                this.binding.variableResult.setText("= " + (result == null ? "?" : result.toUserDisplayableString()));
                this.binding.variableResult.setTextColor(ContextCompat.getColor(this.binding.getRoot().getContext(), R.color.colorText));
            }
        }

        public String getVar() {
            return this.varName;
        }

        public String getFormula() {
            return Objects.requireNonNull(this.binding.variableFormula.getEditText()).getText().toString();
        }
    }

    public static final class VariablesListAdapter extends ManagedListAdapter<CalculatorMap.CalculatorState, VariableViewHolder> {

        private boolean isAdvancedView = true;
        private VariableList variables;
        private boolean textListeningActive = true;
        private final RecyclerView recyclerView;

        private Consumer<VariableList> changeCallback;

        private VariablesListAdapter(final RecyclerView recyclerView) {
            super(new Config(recyclerView)
                .setNotifyOnPositionChange(true)
                .setSupportDragDrop(true));
            this.recyclerView = recyclerView;
        }

        public void setChangeCallback(final Consumer<VariableList> changeCallback) {
            this.changeCallback = changeCallback;
        }

        public void setVariableList(@NonNull final VariableList variables) {
            this.variables = variables;
            clearList();
            for (String var : this.variables.getVariableList()) {
                addItem(this.variables.getState(var));
            }
            callCallback();
        }

        public VariableList getVariableList() {
            return this.variables;
        }

        private void fillViewHolder(final VariableViewHolder holder, final CalculatorMap.CalculatorState data) {
            if (holder == null) {
                return;
            }
            textListeningActive = false;
            holder.setData(data, this.isAdvancedView);
            textListeningActive = true;
        }

        @NonNull
        @Override
        public VariableViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.variable_list_item, parent, false);
            final VariableViewHolder viewHolder = new VariableViewHolder(view);
            viewHolder.binding.variableDelete.setOnClickListener(v -> removeVarAt(viewHolder.getBindingAdapterPosition()));

            Objects.requireNonNull(viewHolder.binding.variableFormula.getEditText()).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                    //empty on purpose
                }

                @Override
                public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                    //empty on purpose
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    if (textListeningActive) {
                        changeFormulaFor(viewHolder.getBindingAdapterPosition(), s.toString());
                    }
                }
            });

            viewHolder.binding.variableFunction.setOnClickListener(d -> {
                final List<CalculatorFunction> functions = CalculatorFunction.valuesAsUserDisplaySortedList();
                SimpleDialog.ofContext(parent.getContext()).setTitle(TextParam.text("Choose function"))
                    .selectSingleGrouped(functions, (f, i) -> getFunctionDisplayString(f), -1, true, (f, i) -> f.getGroup(), VariablesListAdapter::getFunctionGroupDisplayString, (f, i) -> {
                        final String newFormula = f.getFunctionInsertString();
                        final EditText editText = viewHolder.binding.variableFormula.getEditText();
                        editText.setText(newFormula);
                        changeFormulaFor(viewHolder.getBindingAdapterPosition(), newFormula);
                        editText.setSelection(f.getFunctionInsertCursorPosition());
                        Keyboard.show(parent.getContext(), editText);
                    });
            });

            viewHolder.binding.variableName.setOnClickListener((d ->
                selectVariableName(viewHolder.getVar(), (o, n) -> changeVarAt(viewHolder.getBindingAdapterPosition(), n))));

            return viewHolder;
        }

        private static TextParam getFunctionDisplayString(final CalculatorFunction f) {
            return TextParam.text(f.getUserDisplayableString());
        }

        private static TextParam getFunctionGroupDisplayString(final CalculatorFunction.CalculatorGroup g) {
            return
                TextParam.text("**" + g.getUserDisplayableString() + "**").setMarkdown(true);
        }

        @Override
        public void onBindViewHolder(@NonNull final VariableViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
        }

        private void removeVarAt(final int varPos) {
            final String var = getItem(varPos).getVar();
            variables.removeVariable(var);
            removeItem(varPos);
            callCallback();
            notifyItemRangeChanged(0, getItemCount());
        }

        private void changeVarAt(final int varPos, final String newVar) {
            final CalculatorMap.CalculatorState oldState = getItem(varPos);
            if (Objects.equals(oldState.getVar(), newVar)) {
                return;
            }
            final String oldFormula = oldState.getFormula();
            variables.removeVariable(oldState.getVar());
            removeVarFromViewIfPresent(newVar);

            variables.addVariable(newVar, oldFormula, varPos);
            updateItem(variables.getState(newVar), varPos);
            callCallback();
            recalculateResultsInView();
        }

        public void addVariable(final String newVar, final String formula) {
            if (newVar != null) {
                removeVarFromViewIfPresent(newVar);
            }
            final String var = variables.addVariable(newVar, formula, 0);
            addItem(0, variables.getState(var));
            callCallback();
            notifyItemRangeChanged(0, getItemCount());
        }

        public void addAllMissing() {
            final Collection<String> varsMissing = variables.getAllMissingVars();
            for (String var : varsMissing) {
                addVariable(var, "");
            }
        }

        public void clearAllVariables() {
            variables.clear();
            clearList();
            callCallback();
        }

        public void sortVariables() {
            sortItems((v1, v2) -> TextUtils.COLLATOR.compare(v1.getVar(), v2.getVar()));
            variables.sortVariables(TextUtils.COLLATOR::compare);
            callCallback();
        }

        public void selectVariableName(final String oldName, final Action2<String, String> callback) {

            final boolean oldNameIsInvisible = !VariableList.isVisible(oldName);
            final String nameToShow = oldNameIsInvisible ? "" : oldName;
            SimpleDialog.ofContext(recyclerView.getContext()).setTitle(TextParam.text("Variable Name")).setMessage(TextParam.text("Enter variable name (may be left empty)"))
                .input(InputType.TYPE_CLASS_TEXT, nameToShow, null, null, s -> StringUtils.isBlank(s) || isValidVarName(s), "[a-zA-Z0-9]", t -> {
                    final boolean newNameIsInvisible = StringUtils.isBlank(t);
                    if ((oldName != null && oldNameIsInvisible && newNameIsInvisible) || Objects.equals(oldName, t)) {
                        //nothing to do
                        return;
                    }
                    final String newName = StringUtils.isBlank(t) ? null : t;
                    callback.call(oldName, newName);
                });
        }

        private boolean isValidVarName(final String varName) {
            return VARNAME_PATTERN.matcher(varName).matches();
        }


        private void changeFormulaFor(final int varPos, final String formula) {
            final String var = getItem(varPos).getVar();
            if (variables.changeVariable(var, formula)) {
                recalculateResultsInView();
            }
        }

        private void recalculateResultsInView() {
            for (int pos = 0; pos < getItemCount(); pos++) {
                final VariableViewHolder itemHolder = (VariableViewHolder) this.recyclerView.findViewHolderForLayoutPosition(pos);
                if (itemHolder != null) {
                    final CalculatorMap.CalculatorState state = variables.getState(itemHolder.getVar());
                    if (state != null) {
                        itemHolder.setResult(state);
                    }
                }
            }
        }

        private void removeVarFromViewIfPresent(final String var) {
            for (int pos = 0; pos < getItemCount(); pos++) {
                final VariableViewHolder itemHolder = (VariableViewHolder) this.recyclerView.findViewHolderForLayoutPosition(pos);
                if (itemHolder != null && itemHolder.getVar().equals(var)) {
                    removeVarAt(pos);
                    break;
                }
            }
        }

        public void setAdvancedView(final boolean isAdvancedView) {
            if (isAdvancedView == this.isAdvancedView) {
                return;
            }
            this.isAdvancedView = isAdvancedView;
            notifyItemRangeChanged(0, this.getItemCount());
        }

        public boolean getAdvancedView() {
            return this.isAdvancedView;
        }

        private void callCallback() {
            if (changeCallback != null && variables != null) {
                changeCallback.accept(variables);
            }
        }
    }


    public VariableListView(final Context context) {
        super(context);
        init();
    }

    public VariableListView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VariableListView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public VariableListView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public VariablesListAdapter getAdapter() {
        return adapter;
    }

    private void init() {
        setOrientation(VERTICAL);
        final ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.variable_list_view, this);
        final VariableListViewBinding listViewBinding = VariableListViewBinding.bind(this);
        this.adapter = new VariablesListAdapter(listViewBinding.variablesList);
    }

}
