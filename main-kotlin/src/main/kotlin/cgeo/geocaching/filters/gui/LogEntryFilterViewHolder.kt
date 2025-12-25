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

package cgeo.geocaching.filters.gui

import cgeo.geocaching.R
import cgeo.geocaching.filters.core.LogEntryGeocacheFilter
import cgeo.geocaching.ui.ButtonToggleGroup
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.ViewUtils.dpToPixel

import android.util.Pair
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout

class LogEntryFilterViewHolder : BaseFilterViewHolder()<LogEntryGeocacheFilter> {

    private ButtonToggleGroup inverseFoundBy
    private EditText foundByText
    private EditText logText


    override     public View createView() {

        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        inverseFoundBy = ButtonToggleGroup(getActivity())
        inverseFoundBy.addButtons(R.string.cache_filter_include, R.string.cache_filter_exclude)

        LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(5))
        ll.addView(inverseFoundBy, llp)

        val foundByField: Pair<View, EditText> = ViewUtils.createTextField(getActivity(), null, TextParam.id(R.string.cache_filter_log_entry_foundby), null, -1, 1, 1)
        foundByText = foundByField.second
        llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(0), 0, dpToPixel(5))
        ll.addView(foundByField.first, llp)

        val logTextField: Pair<View, EditText> = ViewUtils.createTextField(getActivity(), null, TextParam.id(R.string.cache_filter_log_entry_logtext), null, -1, 1, 1)
        logText = logTextField.second
        llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(0), 0, dpToPixel(20))
        ll.addView(logTextField.first, llp)

        return ll
    }

    override     public Unit setViewFromFilter(final LogEntryGeocacheFilter filter) {
        foundByText.setText(filter.getFoundByUser())
        logText.setText(filter.getLogText())
        inverseFoundBy.setCheckedButtonByIndex(filter.isInverse() ? 1 : 0, true)
    }

    override     public LogEntryGeocacheFilter createFilterFromView() {
        val filter: LogEntryGeocacheFilter = createFilter()
        filter.setFoundByUser(foundByText.getText().toString())
        filter.setLogText(logText.getText().toString())
        filter.setInverse(inverseFoundBy.getCheckedButtonIndex() == 1)
        return filter
    }

}
