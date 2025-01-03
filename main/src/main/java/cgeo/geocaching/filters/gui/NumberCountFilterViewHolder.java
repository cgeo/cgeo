package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.NumberRangeGeocacheFilter;
import cgeo.geocaching.ui.ContinuousRangeSlider;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class NumberCountFilterViewHolder<F extends NumberRangeGeocacheFilter<Integer>> extends BaseFilterViewHolder<F> {

    private ContinuousRangeSlider slider;

    private Integer minValue = 0;
    private Integer maxValue = 1000;

    NumberCountFilterViewHolder(final Integer minValue, final Integer maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public View createView() {

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5));

        slider = new ContinuousRangeSlider(getActivity());
        resetSliderScale();
        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5));
        ll.addView(slider, llp);

        return ll;
    }

    private void resetSliderScale() {
        final float minScaleValue = minValue - 0.2f;
        final float maxScaleValue = maxValue + 0.2f;
        slider.setScale(minScaleValue, maxScaleValue, f -> {
            if (f <= minValue) {
                return "" + Math.round(minValue);
            }
            if (f > maxValue) {
                return ">" + Math.round(maxValue);
            }
            return "" + Math.round(f);
        }, 6, 1);
        slider.setRange(minScaleValue, maxScaleValue);

    }


    @Override
    public void setViewFromFilter(final F filter) {
        resetSliderScale();
        slider.setRange(filter.getMinRangeValue() == null ? -10f : filter.getMinRangeValue(), filter.getMaxRangeValue() == null ? 1500f : filter.getMaxRangeValue());
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final ImmutablePair<Float, Float> range = slider.getRange();
        filter.setMinMaxRange(range.left, range.right, minValue.floatValue(), maxValue.floatValue(), value -> Math.round(value));
        return filter;
    }
}
