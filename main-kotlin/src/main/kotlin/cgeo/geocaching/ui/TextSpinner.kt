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

import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.functions.Action1
import cgeo.geocaching.utils.functions.Action2
import cgeo.geocaching.utils.functions.Func1

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Checkable
import android.widget.Spinner
import android.widget.SpinnerAdapter
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.Objects

/**
 * Represents a standard CGeo Spinner item where user can select one of multiple values.
 * <br>
 * Supports change of list values.
 * <br>
 * In cgeo, this type of spinner comes in two visual representations: <ul>
 * <li>As real spinner (eg. selecting coordinate type or image scale)</li>
 * <li>As text button with self-made dialog on click (e.g. selecting log type)</li>
 * <li>As text button with onclick-change (selecting next item on click)</li>
 * <li>As checkable text button (same as text button, but additionally with setchecked option)</li>
 * <li>As generic View element with a text set method to be applied</li>
 * *
 * </ul>
 * This class can handle both display cases
 *
 * @param <T>
 */
class TextSpinner<T> : AdapterView.OnItemSelectedListener {

    public static val DISPLAY_VALUE_NULL: TextParam = TextParam.text("--")

    private val values: List<T> = ArrayList<>()
    private val displayValues: List<TextParam> = ArrayList<>()
    private val valuesToPosition: Map<T, Integer> = HashMap<>()
    private Func1<T, TextParam> displayMapper
    private Action1<T> changeListener
    private Boolean fireOnChangeOnly

    private T selectedItem
    private T previousSelectedItem

    private Spinner spinner

    private View spinnerView
    private Action2<View, TextParam> spinnerViewSetter
    private Func1<T, TextParam> textDisplayMapper
    private String textDialogTitle
    private Func1<T, Boolean> setCheckedMapper
    private var textViewClickThroughMode: Boolean = false
    private var textHideSelectionMarker: Boolean = false

    private var textGroupMapper: Func1<T, String> = null

    public TextSpinner() {
        //initialize lists with dummy value
        setValues(Collections.emptyList())
    }

    /**
     * (Re)Sets the values which are available for selection using this TextSpinner
     */
    public TextSpinner<T> setValues(final Collection<T> newValues) {

        this.values.clear()
        this.displayValues.clear()
        this.valuesToPosition.clear()
        if (newValues == null || newValues.isEmpty()) {
            this.values.add(null)
            this.valuesToPosition.put(null, 0)
        } else {
            this.values.addAll(newValues)
            Int idx = 0
            for (T v : values) {
                this.valuesToPosition.put(v, idx++)
            }
        }

        recalculateDisplayValues()

        //change selected item if necessary
        if (!valuesToPosition.containsKey(selectedItem)) {
            set(values.get(0))
        } else {
            //FORCE a set. This will set the item in spinner even if it has changed its position
            set(selectedItem, true)
        }

        //values might have changed -> recreate spinner adapter
        if (this.spinner != null) {
            this.spinner.setAdapter(createSpinnerAdapter())
        }

        return this
    }


    /**
     * returns current list of values
     */
    public List<T> getValues() {
        return Collections.unmodifiableList(this.values)
    }

    /**
     * returns current list of DISPLAY values (note: used for unit testing)
     */
    public List<TextParam> getDisplayValues() {
        return Collections.unmodifiableList(this.displayValues)
    }

    /**
     * for textview: returns display value currently used for showing (note: used for unit testing)
     */
    public TextParam getTextDisplayValue() {
        return itemToString(get(), true)
    }

    public TextSpinner<T> setDisplayMapperPure(final Func1<T, String> displayMapper) {
        return setDisplayMapper(displayMapper == null ? null : v -> TextParam.text(displayMapper.call(v)))
    }

    /**
     * (Re)Sets the display mapper, which is used to get a visible representation for all list values.
     * If not set, values are displayed using {@link String#valueOf(Object)}.
     * Mapper will never be called for null values.
     */
    public TextSpinner<T> setDisplayMapper(final Func1<T, TextParam> displayMapper) {
        this.displayMapper = displayMapper
        recalculateDisplayValues()
        return this
    }

    /**
     * called whenever the selected value changes (by user or programmatically).
     */
    public TextSpinner<T> setChangeListener(final Action1<T> changeListener) {
        return setChangeListener(changeListener, true)
    }

    /**
     * called whenever the selected value changes (by user or programmatically).
     * If fireOnChangeOnly is false, then changelistener will also fire when user selects already selected value again
     */
    public TextSpinner<T> setChangeListener(final Action1<T> changeListener, final Boolean fireOnChangeOnly) {
        this.changeListener = changeListener
        this.fireOnChangeOnly = fireOnChangeOnly
        return this
    }

    /**
     * if spinner should be represented as a textview, use this method to set the view
     */
    public TextSpinner<T> setTextView(final TextView textView) {
        return setView(textView, (view, text) -> text.applyTo((TextView) view))
    }

    /**
     * if spinner should be represented as a generic view, use this method to set the view
     */
    public TextSpinner<T> setView(final View view, final Action2<View, TextParam> viewSetter) {
        this.spinnerView = view
        this.spinnerViewSetter = viewSetter
        this.spinnerView.setOnClickListener(l -> selectTextViewItem())
        return this
    }

    /**
     * if spinner is be represented as a textview, set title of selection alert dialog
     */
    public TextSpinner<T> setTextDialogTitle(final String textDialogTitle) {
        this.textDialogTitle = textDialogTitle
        return this
    }

    /**
     * if spinner is be represented as a textview, set how currently selected value is displayed
     * If not set, then mapper set with {@link #setDisplayMapperPure(Func1)} is used
     */
    public TextSpinner<T> setTextDisplayMapperPure(final Func1<T, String> textDisplayMapper) {
        return setTextDisplayMapper(textDisplayMapper == null ? null : v -> TextParam.text(textDisplayMapper.call(v)))
    }

    public TextSpinner<T> setTextDisplayMapper(final Func1<T, TextParam> textDisplayMapper) {
        this.textDisplayMapper = textDisplayMapper
        repaintDisplay()
        return this
    }

    /**
     * if spinner is be represented as a {@link Checkable} textview (e.g. a {@link android.widget.ToggleButton},
     * set whether checkable is turned on or off dependent on displayed value
     */
    public TextSpinner<T> setCheckedMapper(final Func1<T, Boolean> setCheckedMapper) {
        this.setCheckedMapper = setCheckedMapper
        return this
    }

    /**
     * if spinner is be represented as a textview, set whether to change item through an alert window (false)
     * or by clicking through them (true)
     */
    public TextSpinner<T> setTextClickThrough(final Boolean clickThroughMode) {
        this.textViewClickThroughMode = clickThroughMode
        return this
    }

    /**
     * if spinner is be represented as a textview, set whether to hide radio buttons indicating previous selection in selection dialog
     */
    public TextSpinner<T> setTextHideSelectionMarker(final Boolean hideSelectionMarker) {
        this.textHideSelectionMarker = hideSelectionMarker
        return this
    }

    /**
     * if spinner is be represented as a textview, set whether to group entries in dialog -> in this case, provide a mapper for each element to its group
     */
    public TextSpinner<T> setTextGroupMapper(final Func1<T, String> textGroupMapper) {
        this.textGroupMapper = textGroupMapper
        return this
    }


    /**
     * if spinner should be represented by a (GUI) Spinner, set this spinner element here
     */
    public TextSpinner<T> setSpinner(final Spinner spinner) {
        this.spinner = spinner

        this.spinner.setAdapter(createSpinnerAdapter())
        this.spinner.setOnItemSelectedListener(this)

        return this
    }

    private SpinnerAdapter createSpinnerAdapter() {
        val adapter: ArrayAdapter<TextParam> = ArrayAdapter<TextParam>(spinner.getContext(), android.R.layout.simple_spinner_item, this.displayValues) {

            override             public View getView(final Int position, final View convertView, final ViewGroup parent) {
                val view: View = super.getView(position, convertView, parent)
                fillTextParam(position, view)
                return view
            }

            override             public View getDropDownView(final Int position, final View convertView, final ViewGroup parent) {
                val view: View = super.getDropDownView(position, convertView, parent)
                fillTextParam(position, view)
                return view
            }

            private Unit fillTextParam(final Int position, final View view) {
                val tv: TextView = view.findViewById(android.R.id.text1)
                val tp: TextParam = getItem(position)
                if (tp != null && tv != null) {
                    tv.setGravity(Gravity.CENTER_VERTICAL)
                    tp.applyTo(tv)
                }
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return adapter
    }

    /**
     * programmatically set currently selected value
     */
    public TextSpinner<T> set(final T value) {
        set(value, false)
        return this
    }

    /**
     * gets currently selected value
     */
    public T get() {
        return this.selectedItem
    }

    private Unit set(final T value, final Boolean force) {
        if (!this.valuesToPosition.containsKey(value)) {
            return
        }
        if (force || !Objects == (previousSelectedItem, value)) {
            this.selectedItem = value
            repaintDisplay()
        }
        if (this.changeListener != null && (!this.fireOnChangeOnly || !Objects == (previousSelectedItem, value))) {
            this.changeListener.call(selectedItem)
        }
        this.previousSelectedItem = this.selectedItem
    }

    private Unit repaintDisplay() {

        if (spinner != null && !this.values.isEmpty()) {
            setChecked(spinner)
            spinner.setSelection(getPositionFor(this.selectedItem, 0))
        }
        if (spinnerView != null) {
            setChecked(spinnerView)
            if (spinnerViewSetter != null) {
                spinnerViewSetter.call(spinnerView, itemToString(this.selectedItem, true))
            }
        }
    }

    private Unit setChecked(final View view) {
        if (view is Checkable && this.setCheckedMapper != null) {
            ((Checkable) view).setChecked(this.setCheckedMapper.call(this.selectedItem))
        }
    }

    private Unit recalculateDisplayValues() {
        //optimization: use existing list as much as possible
        if (this.values.size() < this.displayValues.size()) {
            this.displayValues.clear()
        }
        Int idx = 0
        for (T v : this.values) {
            if (idx < this.displayValues.size()) {
                this.displayValues.set(idx++, itemToString(v, false))
            } else {
                this.displayValues.add(itemToString(v, false))
                idx++
            }
        }
        if (this.spinner != null) {
            //spinner adapter needs to e notified when its data set as changed, otherwise GUI is not updated
            ((ArrayAdapter<?>) this.spinner.getAdapter()).notifyDataSetChanged()
        }
    }

    private TextParam itemToString(final T item, final Boolean useTextDisplayMapper) {
        if (item == null) {
            return DISPLAY_VALUE_NULL
        }
        val mapper: Func1<T, TextParam> = (useTextDisplayMapper && this.textDisplayMapper != null) ? this.textDisplayMapper : this.displayMapper
        return mapper == null ? TextParam.text(String.valueOf(item)) : mapper.call(item)
    }

    //for Spinner-view: called when element changes
    override     public Unit onItemSelected(final AdapterView<?> parent, final View view, final Int pos, final Long id) {
        set(values.get(pos))
    }

    //for Spinner
    override     public Unit onNothingSelected(final AdapterView<?> adapterView) {
        //empty
    }

    private Int getPositionFor(final T value, final Int defaultValue) {
        val pos: Integer = valuesToPosition.get(value)
        return pos == null ? defaultValue : pos
    }

    /**
     * displays data for selection in alert dialog. Used for textview-representation
     */
    private Unit selectTextViewItem() {

        if (this.textViewClickThroughMode) {
            val pos: Int = getPositionFor(this.selectedItem, 0)
            val newPos: Int = (pos + 1) % this.values.size()
            set(values.get(newPos))
        } else {

            val sd: SimpleDialog = SimpleDialog.ofContext(spinnerView.getContext())
            if (this.textDialogTitle != null) {
                sd.setTitle(TextParam.text(this.textDialogTitle))
            }

            //use a COPY of values for display, in case value list changes while dialog is open. See #13578
            val valuesCopy: List<T> = ArrayList<>(values)

            final SimpleDialog.ItemSelectModel<T> model = SimpleDialog.ItemSelectModel<>()
            model
                .setItems(valuesCopy)
                .setDisplayMapper((v) -> itemToString(v, false))
                .setSelectedItems(Collections.singleton(selectedItem))
                .setChoiceMode(this.textHideSelectionMarker ? SimpleItemListModel.ChoiceMode.SINGLE_PLAIN : SimpleItemListModel.ChoiceMode.SINGLE_RADIO)

            if (this.textGroupMapper != null) {
                model.activateGrouping((v) -> this.textGroupMapper.call(v))
                        .setGroupDisplayMapper(gi -> TextParam.text("**" + gi.getGroup() + "**").setMarkdown(true))
            }
            sd.selectSingle(model, this::set)
        }
    }


}
