package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.databinding.VariableListViewBinding;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.formulas.FormulaFunction;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.formulas.VariableList;
import cgeo.geocaching.utils.formulas.VariableMap;
import cgeo.geocaching.utils.functions.Action2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class VariableListView extends LinearLayout {

    public enum DisplayType {

        ADVANCED(R.layout.variable_list_item_advanced, (tv, vn) -> {
            tv.setText(vn);
            final int textSize;
            switch (vn.length()) {
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
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        }),
        MINIMALISTIC(R.layout.variable_list_item_minimalistic, null);

        @LayoutRes
        public final int listItemLayout;
        private final Action2<TextView, String> varNameSetter;

        DisplayType(@LayoutRes final int listItemLayout, final Action2<TextView, String> varNameSetter) {
            this.listItemLayout = listItemLayout;
            this.varNameSetter = varNameSetter;
        }

        void setVariableName(final TextView tv, final String text) {
            setText(tv, text, varNameSetter);
        }

        private static void setText(final TextView tv, final String text, final Action2<TextView, String> setter) {
            if (tv == null) {
                return;
            }
            final String txt = text == null ? "" : text;
            if (setter != null) {
                setter.call(tv, txt);
            } else {
                tv.setText(txt);
            }
        }
    }

    private static final Pattern VARNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

    private VariablesListAdapter adapter;

    private static class VariableViewHolder extends RecyclerView.ViewHolder {

        private final VariablesListAdapter listAdapter;

        private final DisplayType displayType;
        private final TextView viewVariableName;
        private final TextView viewVariableFormulaText;
        private final TextView viewVariableResult;
        private final View viewButtonFunction;
        private final View viewButtonDelete;

        private String varName;

        VariableViewHolder(final View rowView, final VariablesListAdapter listAdapter, final DisplayType displayType) {
            super(rowView);
            this.listAdapter = listAdapter;
            this.displayType = displayType;
            this.viewVariableName = rowView.findViewById(R.id.variable_name);
            this.viewVariableFormulaText = rowView.findViewById(R.id.variable_formula_text);
            this.viewVariableResult = rowView.findViewById(R.id.variable_result);
            this.viewButtonDelete = rowView.findViewById(R.id.variable_delete);
            this.viewButtonFunction = rowView.findViewById(R.id.variable_function);
        }

        private void setData(final VariableMap.VariableState variableState) {

            this.varName = variableState == null ? "?" : variableState.getVar();
            final String displayVarName = VariableList.isVisible(this.varName) ? this.varName : "-";
            this.displayType.setVariableName(this.viewVariableName, displayVarName);

            if (this.viewVariableFormulaText != null) {
                final String currentText = this.viewVariableFormulaText.getText().toString();
                if (!currentText.equals(variableState == null ? "" : variableState.getFormulaString())) {
                    this.viewVariableFormulaText.setText(variableState == null || StringUtils.isBlank(variableState.getFormulaString()) ? "" : variableState.getFormulaString());
                }
                if (listAdapter.currentFocusKeep && Objects.equals(listAdapter.currentFocusVar, this.varName)) {
                    // we come here if listview changed due to added view (due to filter being changed)
                    // This might change focus unwanted to another variable's edit field.
                    // -> Thus here we try to restore previous focus and cursorpos from previous
                    if (!this.viewVariableFormulaText.hasFocus()) {
                        this.viewVariableFormulaText.requestFocus();
                    }
                    if (this.viewVariableFormulaText.getSelectionStart() != listAdapter.currentFocusCursorPos) {
                        ViewUtils.setSelection(this.viewVariableFormulaText, listAdapter.currentFocusCursorPos, listAdapter.currentFocusCursorPos);
                    }
                }
            }

            setResult(variableState);
        }

        @SuppressLint("SetTextI18n")
        public void setResult(final VariableMap.VariableState variableState) {

            if (viewVariableResult == null) {
                return;
            }
            final boolean isError = variableState == null || variableState.getError() != null;
            if (isError) {
                final CharSequence errorText = variableState == null ? "?" : TextUtils.setSpan(variableState.getError(), new ForegroundColorSpan(Color.RED));
                if (variableState != null && variableState.getResultAsCharSequence() != null) {
                    viewVariableResult.setText(TextUtils.concat(variableState.getResultAsCharSequence(), " | ", errorText));
                } else {
                    viewVariableResult.setText(errorText);
                }
            } else {
                final Value result = variableState.getResult();
                viewVariableResult.setText("= " + (result == null ? "?" : result.toUserDisplayableString()));
            }
        }

        public String getVar() {
            return this.varName;
        }
    }

    public static final class VariablesListAdapter extends ManagedListAdapter<VariableMap.VariableState, VariableViewHolder> {

        private DisplayType displayType = DisplayType.ADVANCED;
        private int displayColumns = -1;

        private VariableList variables;
        private boolean textListeningActive = true;
        private final RecyclerView recyclerView;

        private final Set<String> visibleVariables = new HashSet<>();
        private boolean filterEnabled = false;

        private Action2<String, CharSequence> varChangeCallback;
        private Consumer<VariableList> changeCallback;

        //save and restore focus and cursorpos on view change
        private String currentFocusVar;  // variable whose formula field curently/last had focus
        private int currentFocusCursorPos; // cursor pos for formula field with current focus
        private boolean currentFocusKeep; //if false then focus is captured, if true then focus is restored

        private VariablesListAdapter(final RecyclerView recyclerView) {
            super(new Config(recyclerView)
                    .setNotifyOnPositionChange(true)
                    .setSupportDragDrop(true));
            this.recyclerView = recyclerView;
            setDisplay(DisplayType.ADVANCED, 1);
            setOriginalItemListInsertOrderer((s1, s2) -> TextUtils.COLLATOR.compare(s1.getVar(), s2.getVar()));
        }

        public void setChangeCallback(final Consumer<VariableList> changeCallback) {
            this.changeCallback = changeCallback;
        }

        public void setVarChangeCallback(final Action2<String, CharSequence> varChangeCallback) {
            this.varChangeCallback = varChangeCallback;
        }

        public void setVariableList(@NonNull final VariableList variables) {
            this.variables = variables;
            final List<VariableMap.VariableState> newList = new ArrayList<>();
            for (String var : this.variables.asList()) {
                newList.add(this.variables.getState(var));
            }
            setItems(newList);
            callCallback();
        }

        public void setVisibleVariablesAndDependent(final Collection<String> neededVars) {
            setVisibleVariables(neededVars == null ? null : this.variables.getDependentVariables(neededVars));
        }

        public void addVisibleVariables(final Collection<String> newVars) {
            //this is a costly operation
            //Thus check whether new and old set contains same elements (e.g. no change)
            if (filterEnabled && (newVars.isEmpty() || this.visibleVariables.containsAll(newVars))) {
                return;
            }

            this.visibleVariables.addAll(newVars);

            this.currentFocusKeep = true;
            this.setFilter(d -> this.visibleVariables.contains(d.getVar()), true);
            //recyclerview processes notifications not immediately but instead puts actions on ui thread
            //-> put setting back currentFocusKeep on ui thread as well so it is done after this update
            this.recyclerView.post(() -> this.currentFocusKeep = false);

            filterEnabled = true;
        }

        public void setVisibleVariables(@Nullable final Collection<String> newVisibleVariables) {

            if (newVisibleVariables == null) {
                if (!filterEnabled) {
                    return;
                }
                //disable filter
                filterEnabled = false;
                this.visibleVariables.clear();
                this.setFilter(d -> true, true);

            }
            //this is a costly operation
            //Thus check whether new and old set contains same elements (e.g. no change)
            if (filterEnabled && newVisibleVariables.size() == this.visibleVariables.size() && this.visibleVariables.containsAll(newVisibleVariables)) {
                return;
            }
            this.visibleVariables.clear();
            this.visibleVariables.addAll(newVisibleVariables);
            this.setFilter(d -> this.visibleVariables.contains(d.getVar()), true);
            filterEnabled = true;
        }

        public void ensureVariables(final Collection<String> variables) {
            for (String v : variables) {
                if (!containsVariable(v)) {
                    addVariable(v, "");
                }
            }
        }

        public VariableList getVariables() {
            return this.variables;
        }

        private void fillViewHolder(final VariableViewHolder holder, final VariableMap.VariableState data) {
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
            final View view = LayoutInflater.from(parent.getContext()).inflate(displayType.listItemLayout, parent, false);
            final VariableViewHolder viewHolder = new VariableViewHolder(view, this, displayType);
            if (viewHolder.viewButtonDelete != null) {
                viewHolder.viewButtonDelete.setOnClickListener(v -> removeVarAt(viewHolder.getBindingAdapterPosition()));
            }

            if (viewHolder.viewVariableFormulaText != null) {
                viewHolder.viewVariableFormulaText.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
                    if (textListeningActive) {
                        changeFormulaFor(viewHolder.getBindingAdapterPosition(), s.toString());
                        if (this.varChangeCallback != null) {
                            this.varChangeCallback.call(viewHolder.getVar(), s);
                        }
                        this.currentFocusCursorPos = viewHolder.viewVariableFormulaText.getSelectionStart();
                        Log.d("[FOCUS-TL]Focus changed to " + currentFocusVar + ":" + currentFocusCursorPos);
                    }
                }));

                viewHolder.viewVariableFormulaText.setOnFocusChangeListener((v, f) -> {
                    if (!currentFocusKeep) {
                        if (f) {
                            this.currentFocusVar = viewHolder.getVar();
                            this.currentFocusCursorPos = viewHolder.viewVariableFormulaText.getSelectionStart();
                        } else {
                            this.currentFocusVar = null;
                        }
                        Log.d("[FOCUS]Focus changed to " + currentFocusVar + ":" + currentFocusCursorPos);
                    }
                });
            }

            if (viewHolder.viewButtonFunction != null) {
                viewHolder.viewButtonFunction.setOnClickListener(d -> {
                    final List<FormulaFunction> functions = FormulaFunction.valuesAsUserDisplaySortedList();
                    SimpleDialog.ofContext(parent.getContext()).setTitle(TextParam.id(R.string.formula_choose_function))
                            .selectSingleGrouped(functions, (f, i) -> getFunctionDisplayString(f), -1, SimpleDialog.SingleChoiceMode.SHOW_RADIO, (f, i) -> f.getGroup(), VariablesListAdapter::getFunctionGroupDisplayString, (f, i) -> {
                                if (viewHolder.viewVariableFormulaText != null) {
                                    final String current = viewHolder.viewVariableFormulaText.getText().toString();
                                    final int currentPos = viewHolder.viewVariableFormulaText.getSelectionStart();

                                    final String function = f.getFunctionInsertString();
                                    final int functionPos = f.getFunctionInsertCursorPosition();

                                    final String newFormula = current.substring(0, currentPos) + function + current.substring(currentPos);
                                    final int newPos = currentPos + functionPos;

                                    viewHolder.viewVariableFormulaText.setText(newFormula);
                                    changeFormulaFor(viewHolder.getBindingAdapterPosition(), newFormula);
                                    if (viewHolder.viewVariableFormulaText instanceof EditText) {
                                        ((EditText) viewHolder.viewVariableFormulaText).setSelection(newPos);
                                    }
                                    Keyboard.show(parent.getContext(), viewHolder.viewVariableFormulaText);
                                }
                            });
                });
            }

            if (viewHolder.viewVariableName != null) {
                viewHolder.viewVariableName.setOnClickListener((d ->
                        selectVariableName(viewHolder.getVar(), (o, n) -> changeVarAt(viewHolder.getBindingAdapterPosition(), n))));
            }
            return viewHolder;
        }

        private static TextParam getFunctionDisplayString(final FormulaFunction f) {
            //find the shortest abbrevation
            String fAbbr = f.getMainName();
            for (String name : f.getNames()) {
                if (name.length() < fAbbr.length()) {
                    fAbbr = name;
                }
            }
            return TextParam.text(f.getUserDisplayableString() + " (" + fAbbr + ")");
        }

        private static TextParam getFunctionGroupDisplayString(final FormulaFunction.FunctionGroup g) {
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
            final VariableMap.VariableState oldState = getItem(varPos);
            if (Objects.equals(oldState.getVar(), newVar)) {
                return;
            }
            final String oldFormula = oldState.getFormulaString();
            final int oldVarPos = variables.removeVariable(oldState.getVar());
            removeVariable(newVar);

            variables.addVariable(newVar, oldFormula, oldVarPos);
            updateItem(variables.getState(newVar), varPos);
            callCallback();
            recalculateResultsInView();
        }

        public void addVariable(final String newVar, final String formula) {
            if (newVar != null) {
                removeVariable(newVar);
            }

            final String var = variables.addVariable(newVar, formula, TextUtils.getSortedPos(variables.asList(), newVar));
            addItem(TextUtils.getSortedPos(getItems(), VariableMap.VariableState::getVar, newVar), variables.getState(var));
            callCallback();
            notifyItemRangeChanged(0, getItemCount());
        }

        public boolean containsVariable(final String var) {
            return variables.contains(var);
        }

        public void tidyUp(final Collection<String> neededVars) {
            this.variables.tidyUp(neededVars);
            this.setVisibleVariablesAndDependent(neededVars);
            setVariableList(this.variables);

        }

        public void clearAllVariables() {
            variables.clear();
            clearList();
            callCallback();
        }

        public void sortVariables(final Comparator<String> comparator) {
            if (comparator == null) {
                return;
            }
            sortItems((v1, v2) -> comparator.compare(v1.getVar(), v2.getVar()));
            if (variables != null) {
                variables.sortVariables(comparator);
            }
            callCallback();
        }

        public void selectVariableName(final String oldName, final Action2<String, String> callback) {

            final boolean oldNameIsInvisible = !VariableList.isVisible(oldName);
            final String nameToShow = oldNameIsInvisible ? "" : oldName;
            SimpleDialog.ofContext(recyclerView.getContext()).setTitle(TextParam.id(R.string.variables_varname_dialog_title))
                    .setMessage(TextParam.id(R.string.variables_varname_dialog_message))
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
            final VariableMap.VariableState state = getItem(varPos);
            if (state == null) {
                return;
            }

            final String var = state.getVar();
            if (variables.changeVariable(var, formula)) {
                recalculateResultsInView();
            }
        }

        private void recalculateResultsInView() {
            for (int pos = 0; pos < getItemCount(); pos++) {
                final VariableViewHolder itemHolder = (VariableViewHolder) this.recyclerView.findViewHolderForLayoutPosition(pos);
                if (itemHolder != null) {
                    final VariableMap.VariableState state = variables.getState(itemHolder.getVar());
                    if (state != null) {
                        itemHolder.setResult(state);
                    }
                }
            }
        }

        public void removeVariable(final String var) {
            for (int pos = 0; pos < getItemCount(); pos++) {
                final VariableViewHolder itemHolder = (VariableViewHolder) this.recyclerView.findViewHolderForLayoutPosition(pos);
                if (itemHolder != null && itemHolder.getVar().equals(var)) {
                    removeVarAt(pos);
                    break;
                }
            }
        }

        public void setDisplay(final DisplayType displayType, final int displayColumns) {
            final DisplayType dtUsed = displayType == null ? DisplayType.ADVANCED : displayType;
            final int dcUsed = Math.max(1, displayColumns);
            if (dtUsed.equals(this.displayType) && dcUsed == this.displayColumns) {
                return;
            }
            this.displayType = dtUsed;
            this.displayColumns = dcUsed;

            setVarListLayoutManager();

            invalidateView();

        }

        private void setVarListLayoutManager() {
            this.recyclerView.setLayoutManager(new GridLayoutManager(this.recyclerView.getContext(), this.displayColumns) {
                //we need to set "supportsPredictiveItemAnimations" to "false"
                //to prevent exceptions when "setFilter" is called due to complex series of insert/remove notifications.
                //See e.g. https://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in
                @Override
                public boolean supportsPredictiveItemAnimations() {
                    return false;
                }

                /** the following overwrite of focus search will always search the next EditText inside same RecyclerView */
                @Nullable
                @Override
                public View onInterceptFocusSearch(@NonNull final View focused, final int direction) {
                    final View nextView = ViewUtils.nextView(focused, v -> v instanceof RecyclerView,
                            v -> v instanceof EditText);
                    if (nextView != null) {
                        return nextView;
                    }
                    return super.onInterceptFocusSearch(focused, direction);
                }
            });
        }

        private void invalidateView() {
            //make sure the view is completely recreated
            this.recyclerView.getRecycledViewPool().clear();
            this.recyclerView.invalidate();
            //this.recyclerView.setAdapter(this); //seems to be necessary to really force redraw
        }

        public DisplayType getDisplayType() {
            return this.displayType;
        }

        public int getDisplayColumns() {
            return this.displayColumns;
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
