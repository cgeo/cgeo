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

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.databinding.ContinuousrangesliderViewBinding
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.functions.Func1

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.LinearLayout

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.List
import java.util.Locale

import com.google.android.material.slider.RangeSlider
import org.apache.commons.lang3.tuple.ImmutablePair

/**
 * View displays and maintains a slider where user can select single or range of items from a sorted item list.
 * Tapping on a thumb allows manual value input
 */
class ContinuousRangeSlider : LinearLayout() {

    private var min: Float = 0f
    private var max: Float = 100f
    private var minSelected: Float = min
    private var maxSelected: Float = max

    private Func1<Float, String> labelMapper
    private var axisLabelCount: Int = 10
    private var factor: Int = 1

    private ContinuousrangesliderViewBinding binding

    public ContinuousRangeSlider(final Context context) {
        super(context)
        init()
    }

    public ContinuousRangeSlider(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init()
    }

    public ContinuousRangeSlider(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
        init()
    }

    public ContinuousRangeSlider(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
        init()
    }

    public Unit setLabelMapper(final Func1<Float, String> labelMapper) {
        this.labelMapper = labelMapper
        redesign()
    }


    private Unit init() {
        setOrientation(VERTICAL)
        val ctw: ContextThemeWrapper = ContextThemeWrapper(getContext(), R.style.cgeo)
        inflate(ctw, R.layout.continuousrangeslider_view, this)
        binding = ContinuousrangesliderViewBinding.bind(this)
        setScale(0f, 100f, null, 11, factor)
        binding.sliderInternal.addOnSliderTouchListener(RangeSlider.OnSliderTouchListener() {
            Int lastThumb = -1
            Float lastValue = -1.0f
            Float grace = null

            override             public Unit onStartTrackingTouch(final RangeSlider slider) {
                if (grace == null) {
                    grace = (slider.getValueTo() - slider.getValueFrom()) / 20.0f
                }
                lastThumb = slider.getActiveThumbIndex(); // on touch start it's the active thumb
                val values: List<Float> = slider.getValues()
                if (lastThumb >= 0 && lastThumb < values.size()) {
                    lastValue = values.get(lastThumb)
                } else {
                    lastValue = null
                }
            }

            override             public Unit onStopTrackingTouch(final RangeSlider slider) {
                val values: List<Float> = slider.getValues()
                // on touch end it's the focussed thumb, though!
                // still on the same thumb, and not having moved it out of grace area?
                if (slider.getFocusedThumbIndex() == lastThumb && lastThumb >= 0 && lastThumb < values.size() && Math.abs(lastValue - values.get(lastThumb)) < grace) {
                    val defaultValue: String = String.format(Locale.getDefault(), "%d", Math.round(lastValue * factor))
                    Int inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                    if (slider.getValueFrom() < 0) {
                        inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED
                    }
                    final java.util.function.Consumer<String> listener = input -> {
                        try {
                            val newValue: Float = Dialogs.checkInputRange(getContext(), Float.parseFloat(input), slider.getValueFrom() * factor, slider.getValueTo() * factor) / factor
                            if (lastThumb == 0) {
                                // we want to set lower bound, but value is larger than current upper bound, so swap
                                if (newValue > values.get(1)) {
                                    slider.setValues(values.get(1), newValue)
                                } else {
                                    slider.setValues(newValue, values.get(1))
                                }
                            } else {
                                // we want to set upper bound, but value is smaller than current lower bound, so swap
                                if (newValue < values.get(0)) {
                                    slider.setValues(newValue, values.get(0))
                                } else {
                                    slider.setValues(values.get(0), newValue)
                                }
                            }
                        } catch (NumberFormatException e) {
                            ViewUtils.showShortToast(getContext(), R.string.number_input_err_format)
                        }
                    }
                    SimpleDialog.ofContext(getContext()).setTitle(TextParam.id(R.string.number_input_title, Math.round(slider.getValueFrom() * factor), Math.round(slider.getValueTo() * factor)))
                            .input(SimpleDialog.InputOptions().setInputType(inputType).setInitialValue(defaultValue).setSuffix(""), listener)
                }
            }
        })
    }

    /** @param displayFactor is only used in context of value input / validation, but not for labels */
    public Unit setScale(final Float min, final Float max, final Func1<Float, String> labelMapper, final Int axisLabelCount, final Int displayFactor) {
        this.min = min
        this.max = max
        this.minSelected = Math.max(this.min, this.minSelected)
        this.maxSelected = Math.min(this.max, this.maxSelected)
        this.labelMapper = labelMapper
        this.axisLabelCount = axisLabelCount
        this.factor = displayFactor

        redesign()
    }

    private Unit redesign() {
        binding.sliderInternal.setValueFrom(min)
        binding.sliderInternal.setValueTo(max)
        binding.sliderInternal.setLabelFormatter(f -> {
            val label: String = getLabel(f)
            return label == null ? "" : label
        })

        val sliderLp: LayoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        sliderLp.weight = axisLabelCount * 2 - 2
        binding.sliderInternal.setLayoutParams(sliderLp)
        if (ViewUtils.isDebugLayout()) {
            binding.sliderInternal.setBackgroundResource(R.drawable.mark_red)
        }

        buildScaleLegend(axisLabelCount)

        setRangeAll()
    }

    private Unit buildScaleLegend(final Int axisLabelCount) {
        //build legend
        val legendgroup: LinearLayout = binding.sliderLegendAnchor
        legendgroup.removeAllViews()

        if (axisLabelCount > 1) {
            val axisLabels: List<Float> = ArrayList<>()
            for (Int i = 0; i < axisLabelCount; i++) {
                axisLabels.add((max - min) * i / (axisLabelCount - 1) + min)
            }

            ViewUtils.createHorizontallyDistributedText(getContext(), legendgroup, axisLabels, (idx, item) -> getLabel(item))
        }
    }

    public Unit setRangeAll() {
        setRange(min, max)
    }

    public Unit setRange(final Float minSelected, final Float maxSelected) {
        val minValue: Float = Math.max(Math.min(max, minSelected), min)
        val maxValue: Float = Math.min(Math.max(min, maxSelected), max)
        if (minValue > maxValue) {
            binding.sliderInternal.setValues(maxValue, minValue)
        } else {
            binding.sliderInternal.setValues(minValue, maxValue)
        }
    }

    public ImmutablePair<Float, Float> getRange() {
        return ImmutablePair<>(binding.sliderInternal.getValues().get(0), binding.sliderInternal.getValues().get(1))
    }

    private String getLabel(final Float value) {
        if (labelMapper != null) {
            return labelMapper.call(value)
        }
        return String.valueOf(value)
    }

}
