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
import cgeo.geocaching.databinding.VariableListViewBinding
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.utils.formulas.FormulaUtils
import cgeo.geocaching.utils.formulas.Value
import cgeo.geocaching.utils.formulas.VariableList
import cgeo.geocaching.utils.formulas.VariableMap
import cgeo.geocaching.utils.functions.Action2

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.LayoutRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Consumer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.Collection
import java.util.Comparator
import java.util.HashSet
import java.util.List
import java.util.Objects
import java.util.Set
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils

class VariableListView : LinearLayout() {

    enum class class DisplayType {

        ADVANCED(R.layout.variable_list_item_advanced, (tv, vn) -> {
            tv.setText(vn)
            final Int textSize
            switch (vn.length()) {
                case 1:
                    textSize = 40
                    break
                case 2:
                    textSize = 25
                    break
                default:
                    textSize = 14
                    break
            }
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
        }),
        MINIMALISTIC(R.layout.variable_list_item_minimalistic, null)

        @LayoutRes
        public final Int listItemLayout
        private final Action2<TextView, String> varNameSetter

        DisplayType(@LayoutRes final Int listItemLayout, final Action2<TextView, String> varNameSetter) {
            this.listItemLayout = listItemLayout
            this.varNameSetter = varNameSetter
        }

        Unit setVariableName(final TextView tv, final String text) {
            setText(tv, text, varNameSetter)
        }

        private static Unit setText(final TextView tv, final String text, final Action2<TextView, String> setter) {
            if (tv == null) {
                return
            }
            val txt: String = text == null ? "" : text
            if (setter != null) {
                setter.call(tv, txt)
            } else {
                tv.setText(txt)
            }
        }
    }

    private static val VARNAME_PATTERN: Pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$")

    private VariablesListAdapter adapter

    private static class VariableViewHolder : RecyclerView().ViewHolder {

        private final VariablesListAdapter listAdapter

        private final DisplayType displayType
        private final TextView viewVariableName
        private final TextView viewVariableFormulaText
        private final TextView viewVariableResult
        private final View viewButtonFunction
        private final View viewButtonDelete

        private String varName

        VariableViewHolder(final View rowView, final VariablesListAdapter listAdapter, final DisplayType displayType) {
            super(rowView)
            this.listAdapter = listAdapter
            this.displayType = displayType
            this.viewVariableName = rowView.findViewById(R.id.variable_name)
            this.viewVariableFormulaText = rowView.findViewById(R.id.variable_formula_text)
            this.viewVariableResult = rowView.findViewById(R.id.variable_result)
            this.viewButtonDelete = rowView.findViewById(R.id.variable_delete)
            this.viewButtonFunction = rowView.findViewById(R.id.variable_function)
        }

        private Unit setData(final VariableMap.VariableState variableState) {

            this.varName = variableState == null ? "?" : variableState.getVar()
            val displayVarName: String = VariableList.isVisible(this.varName) ? this.varName : "-"
            this.displayType.setVariableName(this.viewVariableName, displayVarName)

            if (this.viewVariableFormulaText != null) {
                val currentText: String = this.viewVariableFormulaText.getText().toString()
                if (!currentText == (variableState == null ? "" : variableState.getFormulaString())) {
                    this.viewVariableFormulaText.setText(variableState == null || StringUtils.isBlank(variableState.getFormulaString()) ? "" : variableState.getFormulaString())
                }
                if (listAdapter.currentFocusKeep && Objects == (listAdapter.currentFocusVar, this.varName)) {
                    // we come here if listview changed due to added view (due to filter being changed)
                    // This might change focus unwanted to another variable's edit field.
                    // -> Thus here we try to restore previous focus and cursorpos from previous
                    if (!this.viewVariableFormulaText.hasFocus()) {
                        this.viewVariableFormulaText.requestFocus()
                    }
                    if (this.viewVariableFormulaText.getSelectionStart() != listAdapter.currentFocusCursorPos) {
                        ViewUtils.setSelection(this.viewVariableFormulaText, listAdapter.currentFocusCursorPos, listAdapter.currentFocusCursorPos)
                    }
                }
            }

            setResult(variableState)
        }

        @SuppressLint("SetTextI18n")
        public Unit setResult(final VariableMap.VariableState variableState) {

            if (viewVariableResult == null) {
                return
            }
            val isError: Boolean = variableState == null || variableState.getError() != null
            if (isError) {
                val errorText: CharSequence = variableState == null ? "?" : TextUtils.setSpan(variableState.getError(), ForegroundColorSpan(Color.RED))
                if (variableState != null && variableState.getResultAsCharSequence() != null) {
                    viewVariableResult.setText(TextUtils.concat(variableState.getResultAsCharSequence(), " | ", errorText))
                } else {
                    viewVariableResult.setText(errorText)
                }
            } else {
                val result: Value = variableState.getResult()
                viewVariableResult.setText("= " + (result == null ? "?" : result.toUserDisplayableString()))
            }
        }

        public String getVar() {
            return this.varName
        }
    }

    public static class VariablesListAdapter : ManagedListAdapter()<VariableMap.VariableState, VariableViewHolder> {

        private var displayType: DisplayType = DisplayType.ADVANCED
        private var displayColumns: Int = -1

        private VariableList variables
        private var textListeningActive: Boolean = true
        private final RecyclerView recyclerView

        private val visibleVariables: Set<String> = HashSet<>()
        private var filterEnabled: Boolean = false

        private Action2<String, CharSequence> varChangeCallback
        private Consumer<VariableList> changeCallback

        //save and restore focus and cursorpos on view change
        private String currentFocusVar;  // variable whose formula field curently/last had focus
        private Int currentFocusCursorPos; // cursor pos for formula field with current focus
        private Boolean currentFocusKeep; //if false then focus is captured, if true then focus is restored

        private VariablesListAdapter(final RecyclerView recyclerView) {
            super(Config(recyclerView)
                    .setNotifyOnPositionChange(true)
                    .setSupportDragDrop(true))
            this.recyclerView = recyclerView
            setDisplay(DisplayType.ADVANCED, 1)
            setOriginalItemListInsertOrderer((s1, s2) -> TextUtils.COLLATOR.compare(s1.getVar(), s2.getVar()))
        }

        public Unit setChangeCallback(final Consumer<VariableList> changeCallback) {
            this.changeCallback = changeCallback
        }

        public Unit setVarChangeCallback(final Action2<String, CharSequence> varChangeCallback) {
            this.varChangeCallback = varChangeCallback
        }

        public Unit setVariableList(final VariableList variables) {
            this.variables = variables
            val newList: List<VariableMap.VariableState> = ArrayList<>()
            for (String var : this.variables.asList()) {
                newList.add(this.variables.getState(var))
            }
            setItems(newList)
            callCallback()
        }

        public Unit setVisibleVariablesAndDependent(final Collection<String> neededVars) {
            setVisibleVariables(neededVars == null ? null : this.variables.getDependentVariables(neededVars))
        }

        public Unit addVisibleVariables(final Collection<String> newVars) {
            //this is a costly operation
            //Thus check whether and old set contains same elements (e.g. no change)
            if (filterEnabled && (newVars.isEmpty() || this.visibleVariables.containsAll(newVars))) {
                return
            }

            this.visibleVariables.addAll(newVars)

            this.currentFocusKeep = true
            this.setFilter(d -> this.visibleVariables.contains(d.getVar()), true)
            //recyclerview processes notifications not immediately but instead puts actions on ui thread
            //-> put setting back currentFocusKeep on ui thread as well so it is done after this update
            this.recyclerView.post(() -> this.currentFocusKeep = false)

            filterEnabled = true
        }

        public Unit setVisibleVariables(final Collection<String> newVisibleVariables) {

            if (newVisibleVariables == null) {
                if (!filterEnabled) {
                    return
                }
                //disable filter
                setVisibleVariables()
                return
            }

            //this is a costly operation
            //Thus check whether and old set contains same elements (e.g. no change)
            if (filterEnabled && newVisibleVariables.size() == this.visibleVariables.size() && this.visibleVariables.containsAll(newVisibleVariables)) {
                return
            }
            this.visibleVariables.clear()
            this.visibleVariables.addAll(newVisibleVariables)
            this.setFilter(d -> this.visibleVariables.contains(d.getVar()), true)
            filterEnabled = true
        }

        public Unit checkAddVisibleVariables(final Collection<String> vars) {
            if (!vars.isEmpty()) {
                val neededVars: Set<String> = getVariables().getDependentVariables(vars)
                ensureVariables(neededVars)
                addVisibleVariables(neededVars)
            }
        }

        public Unit setVisibleVariables() {
            //disable filter
            this.filterEnabled = Settings.getHideCompletedVariables()
            this.visibleVariables.clear()
            this.setFilter(d -> filterEnabled ? !isVariableComplete(d.getVar()) : true, true)
        }

        private Boolean isVariableComplete(final String var) {
            // return StringUtils.isNotEmpty(this.variables.getState(var).getFormulaString()
            return null != this.variables.getValue(var)
                    && this.variables.getState(var).getState() == VariableMap.State.OK
        }

        public Unit ensureVariables(final Collection<String> variables) {
            for (String v : variables) {
                if (!containsVariable(v)) {
                    addVariable(v, "")
                }
            }
        }

        public VariableList getVariables() {
            return this.variables
        }

        private Unit fillViewHolder(final VariableViewHolder holder, final VariableMap.VariableState data) {
            if (holder == null) {
                return
            }
            textListeningActive = false
            holder.setData(data)
            textListeningActive = true
        }

        override         public VariableViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            val view: View = LayoutInflater.from(parent.getContext()).inflate(displayType.listItemLayout, parent, false)
            val viewHolder: VariableViewHolder = VariableViewHolder(view, this, displayType)
            if (viewHolder.viewButtonDelete != null) {
                viewHolder.viewButtonDelete.setOnClickListener(v -> removeVarAt(viewHolder.getBindingAdapterPosition()))
            }

            if (viewHolder.viewVariableFormulaText != null) {
                viewHolder.viewVariableFormulaText.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
                    if (textListeningActive) {
                        changeFormulaFor(viewHolder.getBindingAdapterPosition(), s.toString())
                        if (this.varChangeCallback != null) {
                            this.varChangeCallback.call(viewHolder.getVar(), s)
                        }
                        this.currentFocusCursorPos = viewHolder.viewVariableFormulaText.getSelectionStart()
                        Log.d("[FOCUS-TL]Focus changed to " + currentFocusVar + ":" + currentFocusCursorPos)
                    }
                }))

                viewHolder.viewVariableFormulaText.setOnFocusChangeListener((v, f) -> {
                    if (!currentFocusKeep) {
                        if (f) {
                            this.currentFocusVar = viewHolder.getVar()
                            this.currentFocusCursorPos = viewHolder.viewVariableFormulaText.getSelectionStart()
                        } else {
                            this.currentFocusVar = null
                        }
                        Log.d("[FOCUS]Focus changed to " + currentFocusVar + ":" + currentFocusCursorPos)
                    }
                })
            }

            if (viewHolder.viewButtonFunction != null) {
                viewHolder.viewButtonFunction.setOnClickListener(d -> FormulaUtils.showSelectFunctionDialog(parent.getContext(), viewHolder.viewVariableFormulaText, newFormula -> changeFormulaFor(viewHolder.getBindingAdapterPosition(), newFormula)))
            }

            if (viewHolder.viewVariableName != null) {
                viewHolder.viewVariableName.setOnClickListener((d ->
                        selectVariableName(viewHolder.getVar(), (o, n) -> changeVarAt(viewHolder.getBindingAdapterPosition(), n))))
            }
            return viewHolder
        }

        override         public Unit onBindViewHolder(final VariableViewHolder holder, final Int position) {
            fillViewHolder(holder, getItem(position))
        }

        private Unit removeVarAt(final Int varPos) {
            val var: String = getItem(varPos).getVar()
            variables.removeVariable(var)
            removeItem(varPos)
            callCallback()
            notifyItemRangeChanged(0, getItemCount())
        }

        private Unit changeVarAt(final Int varPos, final String newVar) {
            final VariableMap.VariableState oldState = getItem(varPos)
            if (Objects == (oldState.getVar(), newVar)) {
                return
            }
            val oldFormula: String = oldState.getFormulaString()
            val oldVarPos: Int = variables.removeVariable(oldState.getVar())
            removeVariable(newVar)

            variables.addVariable(newVar, oldFormula, oldVarPos)
            updateItem(variables.getState(newVar), varPos)
            callCallback()
            recalculateResultsInView()
        }

        public Unit addVariable(final String newVar, final String formula) {
            if (newVar != null) {
                removeVariable(newVar)
            }

            val var: String = variables.addVariable(newVar, formula, TextUtils.getSortedPos(variables.asList(), newVar))
            addItem(TextUtils.getSortedPos(getItems(), VariableMap.VariableState::getVar, newVar), variables.getState(var))
            callCallback()
            notifyItemRangeChanged(0, getItemCount())
        }

        public Boolean containsVariable(final String var) {
            return variables.contains(var)
        }

        public Unit tidyUp(final Collection<String> neededVars) {
            this.variables.tidyUp(neededVars)
            this.setVisibleVariablesAndDependent(neededVars)
            setVariableList(this.variables)
        }

        public Unit clearAllVariables() {
            variables.clear()
            clearList()
            callCallback()
        }

        public Unit sortVariables(final Comparator<String> comparator) {
            if (comparator == null || variables == null) {
                return
            }

            // don't reinitialize from original list, hence, only sortVariables and not sortItems
            variables.sortVariables(comparator)
        }

        public Unit selectVariableName(final String oldName, final Action2<String, String> callback) {

            val oldNameIsInvisible: Boolean = !VariableList.isVisible(oldName)
            val nameToShow: String = oldNameIsInvisible ? "" : oldName
            SimpleDialog.ofContext(recyclerView.getContext()).setTitle(TextParam.id(R.string.variables_varname_dialog_title))
                    .setMessage(TextParam.id(R.string.variables_varname_dialog_message))
                    .input(SimpleDialog.InputOptions()
                        .setInitialValue(nameToShow)
                        .setInputChecker(s -> StringUtils.isBlank(s) || isValidVarName(s))
                        .setAllowedChars("[a-zA-Z0-9]"),
                        t -> {
                            val newNameIsInvisible: Boolean = StringUtils.isBlank(t)
                            if ((oldName != null && oldNameIsInvisible && newNameIsInvisible) || Objects == (oldName, t)) {
                                //nothing to do
                                return
                            }
                            val newName: String = StringUtils.isBlank(t) ? null : t
                            callback.call(oldName, newName)
                        })
        }

        private Boolean isValidVarName(final String varName) {
            return VARNAME_PATTERN.matcher(varName).matches()
        }


        private Unit changeFormulaFor(final Int varPos, final String formula) {
            final VariableMap.VariableState state = getItem(varPos)
            if (state == null) {
                return
            }

            val var: String = state.getVar()
            if (variables.changeVariable(var, formula)) {
                recalculateResultsInView()
            }
        }

        private Unit recalculateResultsInView() {
            for (Int pos = 0; pos < getItemCount(); pos++) {
                val itemHolder: VariableViewHolder = (VariableViewHolder) this.recyclerView.findViewHolderForLayoutPosition(pos)
                if (itemHolder != null) {
                    final VariableMap.VariableState state = variables.getState(itemHolder.getVar())
                    if (state != null) {
                        itemHolder.setResult(state)
                    }
                }
            }
        }

        public Unit removeVariable(final String var) {
            for (Int pos = 0; pos < getItemCount(); pos++) {
                val itemHolder: VariableViewHolder = (VariableViewHolder) this.recyclerView.findViewHolderForLayoutPosition(pos)
                if (itemHolder != null && itemHolder.getVar() == (var)) {
                    removeVarAt(pos)
                    break
                }
            }
        }

        public Unit setDisplay(final DisplayType displayType, final Int displayColumns) {
            val dtUsed: DisplayType = displayType == null ? DisplayType.ADVANCED : displayType
            val dcUsed: Int = Math.max(1, displayColumns)
            if (dtUsed == (this.displayType) && dcUsed == this.displayColumns) {
                return
            }
            this.displayType = dtUsed
            this.displayColumns = dcUsed

            setVarListLayoutManager()

            invalidateView()

        }

        private Unit setVarListLayoutManager() {
            this.recyclerView.setLayoutManager(GridLayoutManager(this.recyclerView.getContext(), this.displayColumns) {
                //we need to set "supportsPredictiveItemAnimations" to "false"
                //to prevent exceptions when "setFilter" is called due to complex series of insert/remove notifications.
                //See e.g. https://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in
                override                 public Boolean supportsPredictiveItemAnimations() {
                    return false
                }

                /** the following overwrite of focus search will always search the next EditText inside same RecyclerView */
                override                 public View onInterceptFocusSearch(final View focused, final Int direction) {
                    val nextView: View = ViewUtils.nextView(focused, v -> v is RecyclerView,
                            v -> v is EditText)
                    if (nextView != null) {
                        return nextView
                    }
                    return super.onInterceptFocusSearch(focused, direction)
                }
            })
        }

        private Unit invalidateView() {
            //make sure the view is completely recreated
            this.recyclerView.getRecycledViewPool().clear()
            this.recyclerView.invalidate()
            //this.recyclerView.setAdapter(this); //seems to be necessary to really force redraw
        }

        public DisplayType getDisplayType() {
            return this.displayType
        }

        public Int getDisplayColumns() {
            return this.displayColumns
        }

        private Unit callCallback() {
            if (changeCallback != null && variables != null) {
                changeCallback.accept(variables)
            }
        }
    }


    public VariableListView(final Context context) {
        super(context)
        init()
    }

    public VariableListView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init()
    }

    public VariableListView(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
        init()
    }

    public VariableListView(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
        init()
    }

    public VariablesListAdapter getAdapter() {
        return adapter
    }

    private Unit init() {
        setOrientation(VERTICAL)
        val ctw: ContextThemeWrapper = ContextThemeWrapper(getContext(), R.style.cgeo)
        inflate(ctw, R.layout.variable_list_view, this)
        val listViewBinding: VariableListViewBinding = VariableListViewBinding.bind(this)
        this.adapter = VariablesListAdapter(listViewBinding.variablesList)
    }

}
