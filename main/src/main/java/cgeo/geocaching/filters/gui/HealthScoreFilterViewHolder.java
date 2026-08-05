package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.HealthScoreGeocacheFilter;
import cgeo.geocaching.ui.ContinuousRangeSlider;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class HealthScoreFilterViewHolder extends BaseFilterViewHolder<HealthScoreGeocacheFilter> {

    private ContinuousRangeSlider slider;
    private CheckBox includeUnscored;

    @Override
    public View createView() {
        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        includeUnscored = new CheckBox(getActivity());
        includeUnscored.setText(R.string.cache_filter_health_score_include_unscored);
        final LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.setMargins(0, dpToPixel(5), 0, dpToPixel(5));
        ll.addView(includeUnscored, cbLp);

        slider = new ContinuousRangeSlider(getActivity());
        slider.setScale(-0.5f, 100.5f, f -> {
            if (f <= 0) {
                return "0%";
            }
            if (f >= 100) {
                return "100%";
            }
            return Math.round(f) + "%";
        }, 6, 1);
        slider.setRange(-0.5f, 100.5f);
        final LinearLayout.LayoutParams sliderLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sliderLp.setMargins(0, dpToPixel(5), 0, dpToPixel(5));
        ll.addView(slider, sliderLp);

        return ll;
    }

    @Override
    public void setViewFromFilter(final HealthScoreGeocacheFilter filter) {
        includeUnscored.setChecked(filter.isIncludeUnscored());
        slider.setRange(
                filter.getMinRangeValue() == null ? -0.5f : filter.getMinRangeValue().floatValue(),
                filter.getMaxRangeValue() == null ? 100.5f : filter.getMaxRangeValue().floatValue());
    }

    @Override
    public HealthScoreGeocacheFilter createFilterFromView() {
        final HealthScoreGeocacheFilter filter = createFilter();
        filter.setIncludeUnscored(includeUnscored.isChecked());
        final ImmutablePair<Float, Float> range = slider.getRange();
        filter.setMinMaxRange(range.left, range.right, 0f, 100f, value -> Math.round(value));
        return filter;
    }
}
