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
import cgeo.geocaching.filters.core.LogsCountGeocacheFilter
import cgeo.geocaching.log.LogType
import cgeo.geocaching.ui.ContinuousRangeSlider
import cgeo.geocaching.ui.TextSpinner
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.log.LogType.UNKNOWN
import cgeo.geocaching.ui.ViewUtils.dpToPixel

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import java.util.Arrays

import org.apache.commons.lang3.tuple.ImmutablePair

class LogsCountFilterViewHolder : BaseFilterViewHolder()<LogsCountGeocacheFilter> {

    private ContinuousRangeSlider slider
    private val selectSpinner: TextSpinner<LogType> = TextSpinner<>()


    override     public View createView() {

        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        val spinnerView: TextView = ViewUtils.createTextSpinnerView(getActivity(), ll)

        selectSpinner.setTextView(spinnerView)
        selectSpinner
                .setDisplayMapperPure(v -> v == UNKNOWN ? getActivity().getString(R.string.all_types_short) : v.getL10n())
                .setValues(Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, UNKNOWN))
                .set(LogType.FOUND_IT)

        LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5))
        ll.addView(spinnerView, llp)

        slider = ContinuousRangeSlider(getActivity())
        resetSliderScale()
        llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5))
        ll.addView(slider, llp)

        return ll
    }

    private Unit resetSliderScale() {
        slider.setScale(0f, 1000.2f, f -> {
            if (f <= 0) {
                return "0"
            }
            if (f > 1000) {
                return ">1000"
            }
            return "" + Math.round(f)
        }, 6, 1)
        slider.setRange(0f, 1000.2f)
    }


    override     public Unit setViewFromFilter(final LogsCountGeocacheFilter filter) {
        selectSpinner.set(filter.getLogType() == null ? UNKNOWN : filter.getLogType())
        slider.setRange(filter.getMinRangeValue() == null ? -10f : filter.getMinRangeValue(), filter.getMaxRangeValue() == null ? 1500f : filter.getMaxRangeValue())
    }

    override     public LogsCountGeocacheFilter createFilterFromView() {
        val filter: LogsCountGeocacheFilter = createFilter()
        val range: ImmutablePair<Float, Float> = slider.getRange()
        filter.setMinMaxRange(range.left, range.right , 0f, 1000f, Math::round)

        filter.setLogType(selectSpinner.get() == UNKNOWN ? null : selectSpinner.get())
        return filter
    }

}
