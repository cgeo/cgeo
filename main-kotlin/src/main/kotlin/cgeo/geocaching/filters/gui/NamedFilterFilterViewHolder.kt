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
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.NamedFilterGeocacheFilter
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.TextSpinner
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.TextUtils

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

import java.util.ArrayList
import java.util.List


class NamedFilterFilterViewHolder : BaseFilterViewHolder()<NamedFilterGeocacheFilter> {

    private val selectSpinner: TextSpinner<GeocacheFilter> = TextSpinner<>()

    override     public View createView() {
        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        val button: Button = ViewUtils.createButton(getActivity(), ll, TextParam.text(""))
        button.setMinimumWidth(ViewUtils.dpToPixel(200))
        selectSpinner.setTextView(button)

        val namedFilters: List<GeocacheFilter> = ArrayList<>(GeocacheFilter.Storage.getStoredFilters())
        TextUtils.sortListLocaleAware(namedFilters, GeocacheFilter::getName)
        namedFilters.add(0, null)
        selectSpinner
                .setValues(namedFilters)
                .set(null)
                .setDisplayMapperPure(f ->
                        f == null ? LocalizationUtils.getString(R.string.cache_filter_userdisplay_none) : f.getName())
        final LinearLayout.LayoutParams llParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llParams.bottomMargin = ViewUtils.dpToPixel(10)
        ll.addView(button, llParams)

        return ll
    }

    override     public Unit setViewFromFilter(final NamedFilterGeocacheFilter filter) {
        selectSpinner.set(filter.getNamedFilter())
    }

    override     public NamedFilterGeocacheFilter createFilterFromView() {
        val filter: NamedFilterGeocacheFilter = createFilter()
        filter.setNamedFilter(selectSpinner.get())
        return filter
    }

}
