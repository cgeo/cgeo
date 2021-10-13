package cgeo.geocaching;

import cgeo.geocaching.activity.TabbedViewPagerFragment;
import cgeo.geocaching.databinding.CachedetailVariablesPageBinding;
import cgeo.geocaching.databinding.VariablesListItemBinding;
import cgeo.geocaching.models.CacheVariables;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.ShareUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.calc.CalculatorFunction;
import cgeo.geocaching.utils.calc.CalculatorMap;
import cgeo.geocaching.utils.calc.CalculatorUtils;
import cgeo.geocaching.utils.calc.Value;
import cgeo.geocaching.utils.functions.Action2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class VariablesViewPageFragment extends TabbedViewPagerFragment<CachedetailVariablesPageBinding> {

    private static final Pattern VARNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

    private CacheDetailActivity activity;

    private VariablesListAdapter adapter;

    private int previousVarSize = -1;


    private static class VariableViewHolder extends RecyclerView.ViewHolder {
        private final VariablesListItemBinding binding;
        private String varName;

        VariableViewHolder(final View rowView) {
            super(rowView);
            binding = VariablesListItemBinding.bind(rowView);
        }

        public void setData(final CalculatorMap.CalculatorState calculatorState) {

            this.varName = calculatorState.getVar();
            final String displayVarName = CacheVariables.isVisible(this.varName) ? this.varName : "-";
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

    private static final class VariablesListAdapter extends ManagedListAdapter<CalculatorMap.CalculatorState, VariableViewHolder> {

        private CacheVariables variables;
        private boolean textListeningActive = true;
        private final RecyclerView recyclerView;
        private final TextView addNextChar;

        private final Consumer<CacheVariables> changeCallback;



        private VariablesListAdapter(final RecyclerView recyclerView, final TextView addMultiButton, final Consumer<CacheVariables> changeCallback) {
            super(new Config(recyclerView)
                .setNotifyOnPositionChange(true)
                .setSupportDragDrop(true));
            this.recyclerView = recyclerView;
            this.addNextChar = addMultiButton;
            this.changeCallback = changeCallback;

            if (this.addNextChar != null) {
                this.addNextChar.setOnClickListener(v -> {
                    final int nac = getNextAddChar();
                    if (nac > 0) {
                        addVariable("" + (char) nac, "");
                    }
                    processStructuralChangeInView();
                });
                processStructuralChangeInView();
            }
        }

        public void setVariables(@NonNull final CacheVariables variables) {
            this.variables = variables;
            clearList();
            for (String var : this.variables.getVariableList()) {
                addItem(this.variables.getState(var));
            }
            processStructuralChangeInView();
        }

        private void fillViewHolder(final VariableViewHolder holder, final CalculatorMap.CalculatorState data) {
            if (holder == null) {
                return;
            }
            textListeningActive = false;
            holder.setData(data);
            textListeningActive = true;
        }

        @NonNull
        @Override
        public VariableViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.variables_list_item, parent, false);
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
                    .selectSingle(functions, (f, i) -> getFunctionDisplayString(f), -1, false, (f, i) -> {
                        final String newFormula = f.getFunctionInsertString();
                        final EditText editText = viewHolder.binding.variableFormula.getEditText();
                        editText.setText(newFormula);
                        changeFormulaFor(viewHolder.getBindingAdapterPosition(), newFormula);
                        editText.setSelection(f.getFunctionInsertCursorPosition());
                        editText.requestFocus();
                        //trigger show soft keyboard
                        editText.postDelayed(
                            () -> {
                                final InputMethodManager keyboard = (InputMethodManager) parent.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                if (keyboard != null) {
                                    keyboard.showSoftInput(editText, 0);
                                }
                            }
                            , 200);
                    });
            });

            viewHolder.binding.variableName.setOnClickListener((d ->
                selectVariableName(viewHolder.getVar(), (o, n) -> changeVarAt(viewHolder.getBindingAdapterPosition(), n))));

            return viewHolder;
        }

        private static TextParam getFunctionDisplayString(final CalculatorFunction f) {
            return
                TextParam.text("**" + f.getUserDisplayableString() + "** (`" + StringUtils.join(f.getNames(), ",") + "`)")
                    .setMarkdown(true);
        }

        @Override
        public void onBindViewHolder(@NonNull final VariableViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
        }

        private void removeVarAt(final int varPos) {
            final String var = getItem(varPos).getVar();
            variables.removeVariable(var);
            removeItem(varPos);
            processStructuralChangeInView();
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
            processStructuralChangeInView();
            recalculateResultsInView();
        }

        public void addVariable(final String newVar, final String formula) {
            if (newVar != null) {
                removeVarFromViewIfPresent(newVar);
            }
            final String var = variables.addVariable(newVar, formula, 0);
            addItem(0, variables.getState(var));
            processStructuralChangeInView();
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
            processStructuralChangeInView();
        }

        public void sortVariables() {
            sortItems((v1, v2) -> TextUtils.COLLATOR.compare(v1.getVar(), v2.getVar()));
            variables.sortVariables(TextUtils.COLLATOR::compare);
            processStructuralChangeInView();
        }

        public void selectVariableName(final String oldName, final Action2<String, String> callback) {

            final boolean oldNameIsInvisible = !CacheVariables.isVisible(oldName);
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

        @SuppressLint("SetTextI18n")
        private void processStructuralChangeInView() {

            //rename addMulti button if necessary
            if (addNextChar != null) {
                final int nac = getNextAddChar();

                addNextChar.setText(nac < 0 ? "-" : "" + (char) getNextAddChar());
            }

            if (changeCallback != null && variables != null) {
                changeCallback.accept(variables);
            }

        }

        private int getNextAddChar() {
            if (variables == null || !variables.getVariableSet().contains("A")) {
                return 'A';
            }
            int lowestContChar = 'A';
            while (lowestContChar < 'Z' && variables.getVariableSet().contains("" + (char) (lowestContChar + 1))) {
                lowestContChar++;
            }
            return lowestContChar == 'Z' ? -1 : lowestContChar + 1;
        }
    }


    @Override
    public CachedetailVariablesPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final CachedetailVariablesPageBinding binding = CachedetailVariablesPageBinding.inflate(inflater, container, false);
        this.adapter = new VariablesListAdapter(binding.variablesList, binding.variablesAddnextchar, v -> {
            if (activity != null && (previousVarSize < 0 || previousVarSize != v.getVariableList().size())) {
                activity.reinitializePage(-1); //this just reinits the title bar, not the variable tab content
                previousVarSize = v.getVariableList().size();
            }
        });

        //Experimental warning
        TextParam.text("**Experimental New Feature** \n" +
            "* Expect disruptive changes in the future\n" +
            "* No internationalization yet\n" +
            "* GUI/UX not finalized\n" +
            "* Not used by other parts of c:geo yet")
            .setMarkdown(true).applyTo(binding.variablesExperimentalWarning);

        binding.variablesAdd.setOnClickListener(d ->
            adapter.selectVariableName(null, (o, n) -> adapter.addVariable(n, "")));
        binding.variablesAddscan.setOnClickListener(d -> scanCache());
        binding.variablesAddmissing.setOnClickListener(d -> adapter.addAllMissing());

        binding.variablesSort.setOnClickListener(d -> adapter.sortVariables());
        binding.variablesClear.setOnClickListener(d -> adapter.clearAllVariables());

        binding.variablesInfo.setOnClickListener(d -> ShareUtils.openUrl(
            this.getContext(), "https://github.com/cgeo/cgeo/wiki/Calculator-Formula-Syntax", false));

        return binding;
    }

    @Override
    public long getPageId() {
        return CacheDetailActivity.Page.VARIABLES.id;
    }

    @Override
    public void setContent() {

        checkUnsavedChanges();

        // retrieve activity and cache - if either if this is null, something is really wrong...
        this.activity = (CacheDetailActivity) getActivity();
        if (activity == null) {
            return;
        }
        final Geocache cache = activity.getCache();
        if (cache == null) {
            return;
        }
        adapter.setVariables(cache.getVariables());
        binding.getRoot().setVisibility(View.VISIBLE);
    }

    private void checkUnsavedChanges() {
        if (adapter.variables != null && adapter.variables.hasUnsavedChanges()) {
            activity.ensureSaved();
            adapter.variables.saveState();
        }
    }

    private void scanCache() {
        final List<String> toScan = new ArrayList<>();
        toScan.add(activity.getCache().getDescription());
        toScan.add(activity.getCache().getHint());
        for (Waypoint w : activity.getCache().getWaypoints()) {
            toScan.add(w.getNote());
        }
        final List<String> existingFormulas = new ArrayList<>();
        for (CalculatorMap.CalculatorState state : adapter.getItems()) {
            if (state != null && !StringUtils.isBlank(state.getFormula())) {
                existingFormulas.add(state.getFormula());
            }
        }
        final List<String> patterns = CalculatorUtils.scanForFormulas(toScan, existingFormulas);
        SimpleDialog.of(activity).setTitle(TextParam.text("Choose found pattern"))
            .selectMultiple(patterns, (s, i) -> TextParam.text("`" + s + "`").setMarkdown(true), null, set -> {
                for (String s : set) {
                    adapter.addVariable(null, s);
                }
            });
    }

    @Override
    public void onPause() {
        super.onPause();
        //called e.g. when user swipes away from variables tab
        checkUnsavedChanges();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //called e.g. when user closes cache detail view
        checkUnsavedChanges();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //called e.g. when user closes cache detail view (after "onDestroy" is called)
        checkUnsavedChanges();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //called e.g. when user closes cache detail view (after "onDestroyView" is called)
        checkUnsavedChanges();
    }
}
