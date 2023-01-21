package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.ContinuousrangesliderViewBinding;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.functions.Func1;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.android.material.slider.RangeSlider;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * View displays and maintains a slider where user can select single or range of items from a sorted item list.
 * Tapping on a thumb allows manual value input
 */
public class ContinuousRangeSlider extends LinearLayout {

    private float min = 0f;
    private float max = 100f;
    private float minSelected = min;
    private float maxSelected = max;

    private Func1<Float, String> labelMapper;
    private int axisLabelCount = 10;
    private int factor = 1;

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
        setScale(0f, 100f, null, 11, factor);
        binding.sliderInternal.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            int lastThumb = -1;
            Float lastValue = -1.0f;
            Float grace = null;

            @Override
            public void onStartTrackingTouch(final @NonNull RangeSlider slider) {
                if (grace == null) {
                    grace = (slider.getValueTo() - slider.getValueFrom()) / 20.0f;
                }
                lastThumb = slider.getActiveThumbIndex(); // on touch start it's the active thumb
                final List<Float> values = slider.getValues();
                if (lastThumb >= 0 && lastThumb < values.size()) {
                    lastValue = values.get(lastThumb);
                } else {
                    lastValue = null;
                }
            }

            @Override
            public void onStopTrackingTouch(final @NonNull RangeSlider slider) {
                final List<Float> values = slider.getValues();
                // on touch end it's the focussed thumb, though!
                // still on the same thumb, and not having moved it out of grace area?
                if (slider.getFocusedThumbIndex() == lastThumb && lastThumb >= 0 && lastThumb < values.size() && Math.abs(lastValue - values.get(lastThumb)) < grace) {
                    final String defaultValue = String.format(Locale.getDefault(), "%d", Math.round(lastValue * factor));
                    int inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
                    if (slider.getValueFrom() < 0) {
                        inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED;
                    }
                    final Consumer<String> listener = input -> {
                        try {
                            final float newValue = SimpleDialog.checkInputRange(getContext(), Float.parseFloat(input), slider.getValueFrom() * factor, slider.getValueTo() * factor) / factor;
                            if (lastThumb == 0) {
                                // we want to set lower bound, but new value is larger than current upper bound, so swap
                                if (newValue > values.get(1)) {
                                    slider.setValues(values.get(1), newValue);
                                } else {
                                    slider.setValues(newValue, values.get(1));
                                }
                            } else {
                                // we want to set upper bound, but new value is smaller than current lower bound, so swap
                                if (newValue < values.get(0)) {
                                    slider.setValues(newValue, values.get(0));
                                } else {
                                    slider.setValues(values.get(0), newValue);
                                }
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), R.string.number_input_err_format, Toast.LENGTH_SHORT).show();
                        }
                    };
                    SimpleDialog.ofContext(getContext()).setTitle(TextParam.id(R.string.number_input_title, Math.round(slider.getValueFrom() * factor), Math.round(slider.getValueTo() * factor))).input(inputType, defaultValue, null, "", listener);
                }
            }
        });
    }

    /** @param displayFactor is only used in context of value input / validation, but not for labels */
    public void setScale(final float min, final float max, final Func1<Float, String> labelMapper, final int axisLabelCount, final int displayFactor) {
        this.min = min;
        this.max = max;
        this.minSelected = Math.max(this.min, this.minSelected);
        this.maxSelected = Math.min(this.max, this.maxSelected);
        this.labelMapper = labelMapper;
        this.axisLabelCount = axisLabelCount;
        this.factor = displayFactor;

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
        final float minValue = Math.max(Math.min(max, minSelected), min);
        final float maxValue = Math.min(Math.max(min, maxSelected), max);
        if (minValue > maxValue) {
            binding.sliderInternal.setValues(maxValue, minValue);
        } else {
            binding.sliderInternal.setValues(minValue, maxValue);
        }
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
