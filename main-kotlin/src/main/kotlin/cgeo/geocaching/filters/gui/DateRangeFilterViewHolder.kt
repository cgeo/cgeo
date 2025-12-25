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
import cgeo.geocaching.filters.core.DateRangeGeocacheFilter
import cgeo.geocaching.ui.ButtonToggleGroup
import cgeo.geocaching.ui.DateRangeSelector
import cgeo.geocaching.ui.ItemRangeSlider
import cgeo.geocaching.ui.ViewUtils.dpToPixel

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import java.util.Arrays

import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.tuple.ImmutablePair


class DateRangeFilterViewHolder<F : DateRangeGeocacheFilter()> : BaseFilterViewHolder()<F> {

    private final Boolean allowRelativeSelection
    private final Integer[] relativeValues
    private final String[] relativeLabels
    private final String[] relativeShortLabels

    private DateRangeSelector dateRangeSelector
    private ButtonToggleGroup absoluteRelative
    private ItemRangeSlider<Integer> relativeSlider

    public DateRangeFilterViewHolder() {
        this(false, null, null, null)
    }
    public DateRangeFilterViewHolder(final Boolean allowRelativeSelection, final Int[] values, final String[] labels, final String[] shortLabels) {
        this.allowRelativeSelection = allowRelativeSelection
        this.relativeValues = ArrayUtils.toObject(values)
        if (this.relativeValues != null && this.relativeValues.length > 1) {
            //mark beginning and end -> those are the "infinity values"
            this.relativeValues[0] = Integer.MIN_VALUE
            this.relativeValues[this.relativeValues.length - 1] = Integer.MAX_VALUE
        }

        this.relativeLabels = labels
        this.relativeShortLabels = shortLabels
    }

    override     public View createView() {
        dateRangeSelector = DateRangeSelector(getActivity())
        if (!allowRelativeSelection) {
            return dateRangeSelector
        }

        //Build a view which allows toggling between absolute and relative date setting
        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        absoluteRelative = ButtonToggleGroup(getActivity())
        absoluteRelative.addButtons(R.string.cache_filter_datefilter_absolute, R.string.cache_filter_datefilter_relative)
        absoluteRelative.addOnButtonCheckedListener((v, i, b) -> {
            val idx: Int = absoluteRelative.getCheckedButtonIndex()
            dateRangeSelector.setVisibility(idx == 0 ? View.VISIBLE : View.GONE)
            relativeSlider.setVisibility(idx == 1 ? View.VISIBLE : View.GONE)
        })

        LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(5))
        ll.addView(absoluteRelative, llp)

        llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(0), 0, dpToPixel(5))
        ll.addView(dateRangeSelector, llp)

        relativeSlider = ItemRangeSlider<>(getActivity())
        relativeSlider.setScale(Arrays.asList(relativeValues), (i, v) -> relativeLabels[i], (i, v) -> relativeShortLabels[i])

        llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(0), 0, dpToPixel(5))
        ll.addView(relativeSlider, llp)

        dateRangeSelector.setVisibility(View.GONE)
        absoluteRelative.setCheckedButtonByIndex(1, true)
        return ll
    }

    override     public Unit setViewFromFilter(final DateRangeGeocacheFilter filter) {
        dateRangeSelector.setMinMaxDate(filter.getDateFilter().getConfiguredMinDate(), filter.getDateFilter().getConfiguredMaxDate())
        if (absoluteRelative != null) {
            absoluteRelative.setCheckedButtonByIndex(filter.getDateFilter().isRelative() ? 1 : 0, true)
            Integer min = filter.getDateFilter().getMinDateOffset()
            if (min == Integer.MIN_VALUE) {
                min = null
            }
            if (min != null && min == relativeValues[relativeValues.length - 2] + 1) {
                min = Integer.MAX_VALUE
            }
            Integer max = filter.getDateFilter().getMaxDateOffset()
            if (max == Integer.MAX_VALUE) {
                max = null
            }
            if (max != null && max == relativeValues[1] - 1) {
                max = Integer.MIN_VALUE
            }
            relativeSlider.setRange(min, max)
        }
    }


    override     public F createFilterFromView() {
        val filter: F = createFilter()
        if (absoluteRelative != null && absoluteRelative.getCheckedButtonIndex() == 1) {
            val range: ImmutablePair<Integer, Integer> = relativeSlider.getRange()
            Int min = range.left
            Int max = range.right
            if (min == max) {
                if (max == Integer.MIN_VALUE) {
                    max = relativeValues[1] - 1
                }
                if (min == Integer.MAX_VALUE) {
                    min = relativeValues[relativeValues.length - 2] + 1
                }
            }
            filter.setRelativeMinMaxDays(min, max)
        } else {
            filter.setMinMaxDate(dateRangeSelector.getMinDate(), dateRangeSelector.getMaxDate())
        }
        return filter
    }


}
