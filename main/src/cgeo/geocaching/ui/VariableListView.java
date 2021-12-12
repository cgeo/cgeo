package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.databinding.VariableListViewBinding;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.formulas.FormulaFunction;
import cgeo.geocaching.utils.formulas.Value;
import cgeo.geocaching.utils.formulas.VariableList;
import cgeo.geocaching.utils.formulas.VariableMap;
import cgeo.geocaching.utils.functions.Action2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
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
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

        private final DisplayType displayType;
        private final TextView viewVariableName;
        private final TextView viewVariableFormulaText;
        private final TextView viewVariableResult;
        private final View viewButtonFunction;
        private final View viewButtonDelete;


        //private final VariableListItemBinding binding;
        private String varName;

        VariableViewHolder(final View rowView, final DisplayType displayType) {
            super(rowView);
            this.displayType = displayType;
            this.viewVariableName = rowView.findViewById(R.id.variable_name);
            this.viewVariableFormulaText = rowView.findViewById(R.id.variable_formula_text);
            this.viewVariableResult = rowView.findViewById(R.id.variable_result);
            this.viewButtonDelete = rowView.findViewById(R.id.variable_delete);
            this.viewButtonFunction = rowView.findViewById(R.id.variable_function);
            //binding = VariableListItemBinding.bind(rowView);
        }

        private void setData(final VariableMap.VariableState variableState) {

            this.varName = variableState.getVar();
            final String displayVarName = VariableList.isVisible(this.varName) ? this.varName : "-";
            this.displayType.setVariableName(this.viewVariableName, displayVarName);

            if (this.viewVariableFormulaText != null) {
                this.viewVariableFormulaText.setText(variableState.getFormulaString());
            }

            setResult(variableState);
        }

        @SuppressLint("SetTextI18n")
        public void setResult(final VariableMap.VariableState variableState) {

            if (viewVariableResult == null) {
                return;
            }
            final boolean isError = variableState.getError() != null;
            if (isError) {
                viewVariableResult.setText(variableState.getError());
                viewVariableResult.setTextColor(Color.RED);
            } else {
                final Value result = variableState.getResult();
                viewVariableResult.setText("= " + (result == null ? "?" : result.toUserDisplayableString()));
                viewVariableResult.setTextColor(ContextCompat.getColor(viewVariableResult.getContext(), R.color.colorText));
            }
        }

        public String getVar() {
            return this.varName;
        }
    }

    public static final class VariablesListAdapter extends ManagedListAdapter<VariableMap.VariableState, VariableViewHolder> {

        private DisplayType displayType = DisplayType.ADVANCED;
        private int displayColumns = 1;

        private VariableList variables;
        private boolean textListeningActive = true;
        private final RecyclerView recyclerView;

        private Action2<String, CharSequence> varChangeCallback;
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

        public void setVarChangeCallback(final Action2<String, CharSequence> varChangeCallback) {
            this.varChangeCallback = varChangeCallback;
        }

        public void setVariableList(@NonNull final VariableList variables) {
            this.variables = variables;
            clearList();
            for (String var : this.variables.asList()) {
                addItem(this.variables.getState(var));
            }
            callCallback();
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
            final VariableViewHolder viewHolder = new VariableViewHolder(view, displayType);
            if (viewHolder.viewButtonDelete != null) {
                viewHolder.viewButtonDelete.setOnClickListener(v -> removeVarAt(viewHolder.getBindingAdapterPosition()));
            }

            if (viewHolder.viewVariableFormulaText != null) {
                viewHolder.viewVariableFormulaText.addTextChangedListener(ViewUtils.createSimpleWatcher(s ->  {
                    if (textListeningActive) {
                        changeFormulaFor(viewHolder.getBindingAdapterPosition(), s.toString());
                        if (this.varChangeCallback != null) {
                            this.varChangeCallback.call(viewHolder.getVar(), s);
                        }
                    }
                }));
            }

            if (viewHolder.viewButtonFunction != null) {
                viewHolder.viewButtonFunction.setOnClickListener(d -> {
                    final List<FormulaFunction> functions = FormulaFunction.valuesAsUserDisplaySortedList();
                    SimpleDialog.ofContext(parent.getContext()).setTitle(TextParam.text("Choose function"))
                        .selectSingleGrouped(functions, (f, i) -> getFunctionDisplayString(f), -1, true, (f, i) -> f.getGroup(), VariablesListAdapter::getFunctionGroupDisplayString, (f, i) -> {
                            final String newFormula = f.getFunctionInsertString();
                            if (viewHolder.viewVariableFormulaText != null) {
                                viewHolder.viewVariableFormulaText.setText(newFormula);
                                changeFormulaFor(viewHolder.getBindingAdapterPosition(), newFormula);
                                if (viewHolder.viewVariableFormulaText instanceof EditText) {
                                    ((EditText) viewHolder.viewVariableFormulaText).setSelection(f.getFunctionInsertCursorPosition());
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
            return TextParam.text(f.getUserDisplayableString());
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
            variables.removeVariable(oldState.getVar());
            removeVariable(newVar);

            variables.addVariable(newVar, oldFormula, varPos);
            updateItem(variables.getState(newVar), varPos);
            callCallback();
            recalculateResultsInView();
        }

        public void addVariable(final String newVar, final String formula) {
            if (newVar != null) {
                removeVariable(newVar);
            }
            final String var = variables.addVariable(newVar, formula, 0);
            addItem(0, variables.getState(var));
            callCallback();
            notifyItemRangeChanged(0, getItemCount());
        }

        public boolean containsVariable(final String var) {
            return variables.contains(var);
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

            if (this.displayColumns == 1) {
                this.recyclerView.setLayoutManager(new LinearLayoutManager(this.recyclerView.getContext()));
            } else {
                this.recyclerView.setLayoutManager(new GridLayoutManager(this.recyclerView.getContext(), this.displayColumns));
            }

            //make sure the view is completely recreated
            this.recyclerView.getRecycledViewPool().clear();
            this.recyclerView.invalidate();
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
