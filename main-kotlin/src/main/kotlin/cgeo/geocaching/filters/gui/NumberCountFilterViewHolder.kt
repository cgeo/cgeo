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

import cgeo.geocaching.filters.core.NumberRangeGeocacheFilter
import cgeo.geocaching.ui.ContinuousRangeSlider
import cgeo.geocaching.ui.ViewUtils.dpToPixel

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import org.apache.commons.lang3.tuple.ImmutablePair

class NumberCountFilterViewHolder<F : NumberRangeGeocacheFilter()<Integer>> : BaseFilterViewHolder()<F> {

    private ContinuousRangeSlider slider

    private var minValue: Integer = 0
    private var maxValue: Integer = 1000

    NumberCountFilterViewHolder(final Integer minValue, final Integer maxValue) {
        this.minValue = minValue
        this.maxValue = maxValue
    }

    override     public View createView() {

        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5))

        slider = ContinuousRangeSlider(getActivity())
        resetSliderScale()
        llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5))
        ll.addView(slider, llp)

        return ll
    }

    private Unit resetSliderScale() {
        val minScaleValue: Float = minValue - 0.2f
        val maxScaleValue: Float = maxValue + 0.2f
        slider.setScale(minScaleValue, maxScaleValue, f -> {
            if (f <= minValue) {
                return "" + Math.round(minValue)
            }
            if (f > maxValue) {
                return ">" + Math.round(maxValue)
            }
            return "" + Math.round(f)
        }, 6, 1)
        slider.setRange(minScaleValue, maxScaleValue)

    }


    override     public Unit setViewFromFilter(final F filter) {
        resetSliderScale()
        slider.setRange(filter.getMinRangeValue() == null ? -10f : filter.getMinRangeValue(), filter.getMaxRangeValue() == null ? 1500f : filter.getMaxRangeValue())
    }

    override     public F createFilterFromView() {
        val filter: F = createFilter()
        val range: ImmutablePair<Float, Float> = slider.getRange()
        filter.setMinMaxRange(range.left, range.right, minValue.floatValue(), maxValue.floatValue(), Math::round)
        return filter
    }
}
