package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.LogsCountGeocacheFilter;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.ui.ContinuousRangeSlider;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.ViewUtils;
import static cgeo.geocaching.log.LogType.UNKNOWN;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class LogsCountFilterViewHolder extends BaseFilterViewHolder<LogsCountGeocacheFilter> {

    private ContinuousRangeSlider slider;
    private final TextSpinner<LogType> selectSpinner = new TextSpinner<>();


    @Override
    public View createView() {

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        final TextView spinnerView = ViewUtils.createTextSpinnerView(getActivity(), ll);

        selectSpinner.setTextView(spinnerView);
        selectSpinner
                .setDisplayMapper(v -> v == UNKNOWN ? getActivity().getString(R.string.all_types_short) : v.getL10n())
                .setValues(Arrays.asList(LogType.FOUND_IT, LogType.DIDNT_FIND_IT, UNKNOWN))
                .set(LogType.FOUND_IT);

        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5));
        ll.addView(spinnerView, llp);

        slider = new ContinuousRangeSlider(getActivity());
        resetSliderScale();
        llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(5), 0, dpToPixel(5));
        ll.addView(slider, llp);

        return ll;
    }

    private void resetSliderScale() {
        slider.setScale(0f, 1000.2f, f -> {
            if (f <= 0) {
                return "0";
            }
            if (f > 1000) {
                return ">1000";
            }
            return "" + Math.round(f);
        }, 6, 1);
        slider.setRange(0f, 1000.2f);
    }


    @Override
    public void setViewFromFilter(final LogsCountGeocacheFilter filter) {
        selectSpinner.set(filter.getLogType() == null ? UNKNOWN : filter.getLogType());
        slider.setRange(filter.getMinRangeValue() == null ? -10f : filter.getMinRangeValue(), filter.getMaxRangeValue() == null ? 1500f : filter.getMaxRangeValue());
    }

    @Override
    public LogsCountGeocacheFilter createFilterFromView() {
        final LogsCountGeocacheFilter filter = createFilter();
        final ImmutablePair<Float, Float> range = slider.getRange();
        filter.setMinMaxRange(range.left, range.right , 0f, 1000f, value -> Math.round(value));

        filter.setLogType(selectSpinner.get() == UNKNOWN ? null : selectSpinner.get());
        return filter;
    }

}
