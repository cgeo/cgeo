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
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class VariablesViewPageFragment extends TabbedViewPagerFragment<CachedetailVariablesPageBinding> {

    private static final String INVISIBLE_VAR_PREFIX = "_";
    private static final Pattern VARNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

    public static final Pattern TEXT_SCAN_PATTERN = Pattern.compile(
        "[^a-zA-Z0-9](( *\\( *)*([a-zA-Z][a-zA-Z0-9]{0,2}|[0-9.]{1,10})((( *[()] *)*( *[-+/:*] *)+)( *[()] *)*([a-zA-Z][a-zA-Z0-9]{0,2}|[0-9.]{1,10}))+( *\\) *)*)[^a-zA-Z0-9]");

    private CacheDetailActivity activity;

    private VariablesListAdapter adapter;


    private static class VariableViewHolder extends RecyclerView.ViewHolder {
        private final VariablesListItemBinding binding;
        private String varName;

        VariableViewHolder(final View rowView) {
            super(rowView);
            binding = VariablesListItemBinding.bind(rowView);
        }

        public void setData(final CalculatorMap.CalculatorState calculatorState) {

            this.varName = calculatorState.getVar();
            final String displayVarName = this.varName.startsWith(INVISIBLE_VAR_PREFIX) ? "-" : calculatorState.getVar();
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
        private final TextView addMultiButton;

        private boolean hasUnsavedChanges = false;


        private VariablesListAdapter(final RecyclerView recyclerView, final TextView addMultiButton) {
            super(new Config(recyclerView)
                .setNotifyOnPositionChange(true)
                .setSupportDragDrop(true));
            this.recyclerView = recyclerView;
            this.addMultiButton = addMultiButton;

            if (this.addMultiButton != null) {
                this.addMultiButton.setOnClickListener(v -> {
                    for (int c = getHighestAddMultiChar(); c >= 'A'; c--) {
                        if (!variables.getMap().containsKey("" + (char) c)) {
                            addVariable("" + (char) c, "");
                        }
                    }
                    processStructuralChange();
                });
                processStructuralChange();
            }
        }

        public void setVariables(@NonNull final CacheVariables variables) {
            this.variables = variables;
            this.hasUnsavedChanges = false;
            clearList();
            for (String var : this.variables.getVariables()) {
                addItem(variables.getMap().get(var));
            }
        }

        public boolean hasUnsavedChanges() {
            return hasUnsavedChanges;
        }

        public void resetUnsavedChanges() {
            this.hasUnsavedChanges = false;
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
            variables.getMap().remove(var);
            removeItem(varPos);
            processStructuralChange();
            hasUnsavedChanges = true;
            notifyItemRangeChanged(0, getItemCount());
        }

        private void changeVarAt(final int varPos, final String newVar) {
            final CalculatorMap.CalculatorState oldState = getItem(varPos);
            if (Objects.equals(oldState.getVar(), newVar)) {
                return;
            }
            final String oldFormula = oldState.getFormula();
            variables.getMap().remove(oldState.getVar());

            removeVarIfPresent(newVar);

            variables.getMap().put(newVar, oldFormula);
            updateItem(variables.getMap().get(newVar), varPos);
            processStructuralChange();
            resetResults();
            hasUnsavedChanges = true;
        }

        public void addVariable(final String newVar, final String formula) {
            final String var = newVar == null ? variables.getMap().createNonContainedKey(INVISIBLE_VAR_PREFIX) : newVar;

            if (newVar != null) {
                removeVarIfPresent(var);
            }

            variables.getMap().put(var, formula);
            addItem(0, variables.getMap().get(var));
            processStructuralChange();
            hasUnsavedChanges = true;
            notifyItemRangeChanged(0, getItemCount());
        }

        public void addAllMissing() {
            final Set<String> varsMissing = new HashSet<>(variables.getMap().getVars());
            varsMissing.removeAll(variables.getVariables());
            for (String var : varsMissing) {
                addVariable(var, "");
            }
        }

        public void clearAllVariables() {
            if (!variables.getVariables().isEmpty()) {
                hasUnsavedChanges = true;
            }
            variables.clear();
            clearList();
            processStructuralChange();
        }

        public void sortVariables() {
            sortItems((v1, v2) -> TextUtils.COLLATOR.compare(v1.getVar(), v2.getVar()));
            hasUnsavedChanges = true;
            processStructuralChange();
        }

        public String createNonContainedVariableName(final String prefix) {
            return variables.getMap().createNonContainedKey(prefix);
        }

        public void selectVariableName(final String oldName, final Action2<String, String> callback) {

            final boolean oldNameIsInvisible = oldName == null || oldName.startsWith(INVISIBLE_VAR_PREFIX);
            final String nameToShow = oldNameIsInvisible ? "" : oldName;
            SimpleDialog.ofContext(recyclerView.getContext()).setTitle(TextParam.text("Variable Name")).setMessage(TextParam.text("Enter variable name (may be left empty)"))
                .input(InputType.TYPE_CLASS_TEXT, nameToShow, null, null, s -> StringUtils.isBlank(s) || isValidVarName(s), "[a-zA-Z0-9]", t -> {
                    final boolean newNameIsInvisible = StringUtils.isBlank(t);
                    if ((oldNameIsInvisible && newNameIsInvisible) || Objects.equals(oldName, t)) {
                        //nothing to do
                        return;
                    }
                    final String newName = StringUtils.isBlank(t) ? createNonContainedVariableName(INVISIBLE_VAR_PREFIX) : t;
                    callback.call(oldName, newName);
                });
        }

        private boolean isValidVarName(final String varName) {
            return VARNAME_PATTERN.matcher(varName).matches();
        }


        private void changeFormulaFor(final int varPos, final String formula) {
            final String var = getItem(varPos).getVar();
            variables.getMap().put(var, formula);
            hasUnsavedChanges = true;
            resetResults();
        }

        private void resetResults() {
            for (int pos = 0; pos < getItemCount(); pos++) {
                final VariableViewHolder itemHolder = (VariableViewHolder) this.recyclerView.findViewHolderForLayoutPosition(pos);
                if (itemHolder != null) {
                    final CalculatorMap.CalculatorState state = variables.getMap().get(itemHolder.getVar());
                    if (state != null) {
                        itemHolder.setResult(state);
                    }
                }
            }
        }

        private void removeVarIfPresent(final String var) {
            if (variables.getMap().getVars().contains(var)) {
                for (int pos = 0; pos < getItemCount(); pos++) {
                    final VariableViewHolder itemHolder = (VariableViewHolder) this.recyclerView.findViewHolderForLayoutPosition(pos);
                    if (itemHolder != null && itemHolder.getVar().equals(var)) {
                        removeVarAt(pos);
                        break;
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        private void processStructuralChange() {
            //recalculate variable order
            final List<String> variablesOrder = new ArrayList<>();
            for (CalculatorMap.CalculatorState state : getItems()) {
                variablesOrder.add(state.getVar());
            }
            if (variables != null) {
                variables.setOrder(variablesOrder);
            }

            //rename addMulti button if necessary
            if (addMultiButton != null) {
                addMultiButton.setText("A - " + (char) getHighestAddMultiChar());
            }
        }

        private int getHighestAddMultiChar() {
            if (variables == null || !variables.getVariableSet().contains("A")) {
                return 'E';
            }
            int lowestContChar = 'A';
            while (lowestContChar <= 'Z' && variables.getVariableSet().contains("" + (char) (lowestContChar + 1))) {
                lowestContChar++;
            }
            int newHighestChar = ((lowestContChar - 'A'  + 1) / 5) * 5 + 'A' + 4;
            if (newHighestChar >= 'Y') {
                newHighestChar = 'Z';
            }
            return newHighestChar;
        }
    }


    @Override
    public CachedetailVariablesPageBinding createView(@NonNull final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final CachedetailVariablesPageBinding binding = CachedetailVariablesPageBinding.inflate(inflater, container, false);
        this.adapter = new VariablesListAdapter(binding.variablesList, binding.variablesAddmulti);

        //Experimental warning
        TextParam.text("**Experimental New Feature** \n" +
            "* Expect disruptive changes in the future\n" +
            "* Not all features implemented yet\n" +
            "* No persistence yet\n" +
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
        if (adapter.hasUnsavedChanges()) {
            activity.ensureSaved();
            adapter.variables.persistState();
            adapter.resetUnsavedChanges();
        }
    }

    private void scanCache() {
        final List<String> patterns = new ArrayList<>();
        final Set<String> patternSet = new HashSet<>();
        scanText(activity.getCache().getDescription(), patterns, patternSet);
        scanText(activity.getCache().getHint(), patterns, patternSet);
        for (Waypoint w : activity.getCache().getWaypoints()) {
            scanText(w.getNote(), patterns, patternSet);
        }
        Collections.sort(patterns, TextUtils.COLLATOR::compare);
        SimpleDialog.of(activity).setTitle(TextParam.text("Choose found pattern"))
            .selectMultiple(patterns, (s, i) -> TextParam.text("`" + s + "`").setMarkdown(true), null, set -> {
                for (String s : set) {
                    adapter.addVariable(null, s);
                }
            });
    }

    private static void scanText(final String text, final List<String> result, final Set<String> resultSet) {
        if (text == null) {
            return;
        }
        final Matcher m = TEXT_SCAN_PATTERN.matcher(text);
        while (m.find()) {
            final String found = m.group(1);
            if (!resultSet.contains(found)) {
                result.add(found);
                resultSet.add(found);
            }
        }
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
