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
import cgeo.geocaching.filters.core.FavoritesGeocacheFilter
import cgeo.geocaching.ui.ButtonToggleGroup
import cgeo.geocaching.ui.ContinuousRangeSlider
import cgeo.geocaching.ui.ViewUtils.dpToPixel

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import org.apache.commons.lang3.tuple.ImmutablePair

class FavoritesFilterViewHolder : BaseFilterViewHolder()<FavoritesGeocacheFilter> {

    private ContinuousRangeSlider slider
    private ButtonToggleGroup percentage

    private var maxValue: Float = 1000
    private var granularity: Float = 1


    override     public View createView() {

        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        percentage = ButtonToggleGroup(getActivity())
        percentage.addButtons(R.string.cache_filter_favorites_absolute, R.string.cache_filter_favorites_percentage)

        LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5))
        ll.addView(percentage, llp)
        percentage.addOnButtonCheckedListener((v, i, b) -> resetSliderScale())

        slider = ContinuousRangeSlider(getActivity())
        resetSliderScale()
        llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5))
        ll.addView(slider, llp)

        return ll
    }

    private Unit resetSliderScale() {
        if (percentage.getCheckedButtonIndex() == 0) {
            maxValue = 1000
            granularity = 1
            slider.setScale(-0.2f, 1000.2f, f -> {
                maxValue = 1000
                if (f <= 0) {
                    return "0"
                }
                if (f > 1000) {
                    return ">1000"
                }
                return "" + Math.round(f)
            }, 6, 1)
            slider.setRange(-0.2f, 1000.2f)
        } else {
            maxValue = 1
            granularity = 100
            slider.setScale(-0.002f, 1.002f, f -> {
                if (f <= 0) {
                    return "0%"
                }
                if (f >= 1) {
                    return "100%"
                }
                return Math.round(f * 100) + "%"
            }, 6, 100)
            slider.setRange(-0.002f, 1.002f)
        }
    }


    override     public Unit setViewFromFilter(final FavoritesGeocacheFilter filter) {
        percentage.setCheckedButtonByIndex(filter.isPercentage() ? 1 : 0, true)
        resetSliderScale()
        slider.setRange(filter.getMinRangeValue() == null ? -10f : filter.getMinRangeValue(), filter.getMaxRangeValue() == null ? 1500f : filter.getMaxRangeValue())
    }

    override     public FavoritesGeocacheFilter createFilterFromView() {
        val filter: FavoritesGeocacheFilter = createFilter()
        filter.setPercentage(percentage.getCheckedButtonIndex() == 1)
        val range: ImmutablePair<Float, Float> = slider.getRange()
        filter.setMinMaxRange(range.left, range.right , 0f, maxValue, value -> Math.round(value * granularity) / granularity)
        return filter
    }

}
