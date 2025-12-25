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
import cgeo.geocaching.filters.core.LogicalGeocacheFilter

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.google.android.material.button.MaterialButton
import org.apache.commons.lang3.StringUtils

class LogicalFilterViewHolder : BaseFilterViewHolder()<LogicalGeocacheFilter> {

    private LogicalGeocacheFilter filter
    private TextView textView

    override     public View createView() {
        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        val inflater: LayoutInflater = LayoutInflater.from(getActivity())

        val infoLine: View = inflater.inflate(R.layout.checkbox_item, ll, false)
        ((ImageView) infoLine.findViewById(R.id.item_icon)).setImageResource(R.drawable.ic_menu_filter)
        infoLine.findViewById(R.id.item_checkbox).setVisibility(View.GONE)
        textView = infoLine.findViewById(R.id.item_text)
        ll.addView(infoLine)

        val modify: MaterialButton = (MaterialButton) inflater.inflate(R.layout.button_view, ll, false)
        modify.setText(R.string.cache_filter_nested_filter_modify)
        modify.setOnClickListener(v -> ((GeocacheFilterActivity) getActivity()).selectNestedFilter(this))
        ll.addView(modify)

        // restore state
        setViewFromFilter(createFilterFromView())

        return ll
    }

    override     public Unit setViewFromFilter(final LogicalGeocacheFilter filter) {
        this.filter = filter != null ? filter : createFilter()
        val text: String = this.filter.toUserDisplayableString(0)
        textView.setText(StringUtils.isNotBlank(text) ? text : getActivity().getString(R.string.cache_filter_nested_filter_empty_filterconfic))
    }

    override     public LogicalGeocacheFilter createFilterFromView() {
        return filter
    }

}
