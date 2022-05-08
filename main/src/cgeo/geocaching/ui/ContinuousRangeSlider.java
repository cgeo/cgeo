package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.ContinuousrangesliderViewBinding;
import cgeo.geocaching.utils.functions.Func1;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

/** View displays and maintains a slider where user can select single or range of items from a sorted item list. */
public class ContinuousRangeSlider extends LinearLayout {

    private float min = 0f;
    private float max = 100f;
    private float minSelected = min;
    private float maxSelected = max;

    private Func1<Float, String> labelMapper;
    private int axisLabelCount = 10;

    private ContinuousrangesliderViewBinding binding;

    public ContinuousRangeSlider(final Context context) {
        super(context);
        init();
    }

    public ContinuousRangeSlider(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContinuousRangeSlider(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ContinuousRangeSlider(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setLabelMapper(final Func1<Float, String> labelMapper) {
        this.labelMapper = labelMapper;
        redesign();
    }


    private void init() {
        setOrientation(VERTICAL);
        final ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.continuousrangeslider_view, this);
        binding = ContinuousrangesliderViewBinding.bind(this);
        setScale(0f, 100f, null, 11);
    }

    public void setScale(final float min, final float max, final Func1<Float, String> labelMapper, final int axisLabelCount) {
        this.min = min;
        this.max = max;
        this.minSelected = Math.max(this.min, this.minSelected);
        this.maxSelected = Math.min(this.max, this.maxSelected);
        this.labelMapper = labelMapper;
        this.axisLabelCount = axisLabelCount;

        redesign();
    }

    private void redesign() {


        binding.sliderInternal.setValueFrom(min);
        binding.sliderInternal.setValueTo(max);
        binding.sliderInternal.setLabelFormatter(f -> {
            final String label = getLabel(f);
            return label == null ? "" : label;
        });

        final LayoutParams sliderLp = new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        sliderLp.weight = axisLabelCount * 2 - 2;
        binding.sliderInternal.setLayoutParams(sliderLp);
        if (ViewUtils.isDebugLayout()) {
            binding.sliderInternal.setBackgroundResource(R.drawable.mark_red);
        }

        buildScaleLegend(axisLabelCount);

        setRangeAll();
    }

    private void buildScaleLegend(final int axisLabelCount) {
        //build legend
        final LinearLayout legendgroup = binding.sliderLegendAnchor;
        legendgroup.removeAllViews();

        if (axisLabelCount > 1) {
            final List<Float> axisLabels = new ArrayList<>();
            for (int i = 0; i < axisLabelCount; i++) {
                axisLabels.add((max - min) * i / (axisLabelCount - 1) + min);
            }

            ViewUtils.createHorizontallyDistributedText(getContext(), legendgroup, axisLabels, (idx, item) -> getLabel(item));
        }
    }

    public void setRangeAll() {
        setRange(min, max);
    }

    public void setRange(final float minSelected, final float maxSelected) {
        final float minValue = Math.max(minSelected, min);
        final float maxValue = Math.min(maxSelected, max);
        binding.sliderInternal.setValues(minValue, maxValue);
    }

    public ImmutablePair<Float, Float> getRange() {
        return new ImmutablePair<>(binding.sliderInternal.getValues().get(0), binding.sliderInternal.getValues().get(1));
    }

    private String getLabel(final float value) {
        if (labelMapper != null) {
            return labelMapper.call(value);
        }
        return String.valueOf(value);
    }

}
