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
import cgeo.geocaching.databinding.SeekbarBinding
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.functions.Action1

import android.content.Context
import android.content.res.TypedArray
import android.text.InputType
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

import androidx.annotation.Nullable

import java.util.Locale

/**
 * Naming conventions:
 * - progress     - the internal value of the android widget (0 to max) - always Int
 * - value        - actual value (may differ from "progress" if non linear or other boundaries) - always Int
 * - shownValue   - displayed value (may differ from "value", if conversions are involved) - in:Float out:Float or Int converted to string
 * This applies to derived values (like minProgress etc.) as well.
 * <br />
 * Parameters for using in XML preferences:
 * - min          - minimum allowed value
 * - max          - maximum allowed value
 * - stepSize     - value will be rounded down to nearest stepSize
 * - logScaling   - use logarithmic scaling for seekbar display
 */
class SeekbarUI : LinearLayout() {

    private SeekbarBinding binding
    private TextView valueView
    protected var minValue: Int = 0
    protected var maxValue: Int = 100
    protected var minProgress: Int = minValue
    protected var maxProgress: Int = minProgress
    protected String minValueDescription
    protected String maxValueDescription
    protected var stepSize: Int = 0
    protected var startProgress: Int = 0
    public static val defaultValue: Int = 10
    protected var hasDecimals: Boolean = false
    protected var useLogScaling: Boolean = false
    protected var unitValue: String = ""
    private var valueProgressMapper: ValueProgressMapper = null
    private var saveProgressListener: Action1<Integer> = null

    interface ValueProgressMapper {
        Int valueToProgress(Int value)

        Int progressToValue(Int progress)
    }

    public static class FactorizeValueMapper : ValueProgressMapper {
        private final Int factor

        public FactorizeValueMapper(final Int factor) {
            this.factor = factor
        }

        override         public Int valueToProgress(final Int value) {
            return value / factor
        }

        override         public Int progressToValue(final Int progress) {
            return progress * factor
        }
    }

    public SeekbarUI(final Context context) {
        this(context, null)
    }

    public SeekbarUI(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle)
    }

    public SeekbarUI(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0)
    }

    public SeekbarUI(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
        createView()
        init()
    }

    private Unit createView() {
        setOrientation(VERTICAL)
        val ctw: ContextThemeWrapper = ContextThemeWrapper(getContext(), R.style.cgeo)
        inflate(ctw, R.layout.seekbar, this)
        binding = SeekbarBinding.bind(this)
    }

    public Unit init() {
        // get views
        val seekBar: SeekBar = binding.preferenceSeekbar
        valueView = binding.preferenceSeekbarValueView

        // init seekbar
        seekBar.setMax(maxProgress)
        seekBar.setProgress(startProgress)
        valueView.setText(getValueString(startProgress))

        seekBar.setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener() {
            override             public Unit onProgressChanged(final SeekBar seekBar, final Int progress, final Boolean fromUser) {
                if (fromUser && atLeastMin(seekBar, progress)) {
                    valueView.setText(getValueString(progress))
                }
            }

            override             public Unit onStartTrackingTouch(final SeekBar seekBar) {
                // abstract method needs to be implemented, but is not used here
            }

            override             public Unit onStopTrackingTouch(final SeekBar seekBar) {
                if (atLeastMin(seekBar, seekBar.getProgress())) {
                    if (saveProgressListener != null) {
                        saveProgressListener.call(seekBar.getProgress())
                    }
                }
            }
        })

        valueView.setOnClickListener(v2 -> {
            val currentValue: String = valueToShownValue(progressToValue(seekBar.getProgress()))
            Int inputType = InputType.TYPE_CLASS_NUMBER
            if (getHasDecimals()) {
                inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL
            }
            SimpleDialog.ofContext(getContext()).setTitle(TextParam.id(R.string.number_input_title, valueToShownValue(minValue), valueToShownValue(maxValue)))
                    .input(SimpleDialog.InputOptions()
                            .setInputType(inputType)
                            .setInitialValue(currentValue)
                            .setSuffix(getUnitString()), input -> {
                        try {
                            val newValue: Int = (Int) Dialogs.checkInputRange(getContext(), shownValueToValue(Float.parseFloat(input)), minValue, maxValue)
                            val newProgress: Int = valueToProgress(newValue)
                            seekBar.setProgress(newProgress)
                            if (saveProgressListener != null) {
                                saveProgressListener.call(seekBar.getProgress())
                            }
                            valueView.setText(getValueString(newProgress))
                        } catch (NumberFormatException e) {
                            ViewUtils.showShortToast(getContext(), R.string.number_input_err_format)
                        }
                    })
        })

    }

    // ------------------------------------------------------------------------

    protected Int valueToProgressHelper(final Int value) {
        return useLogScaling
                ? (Int) Math.sqrt((Long) (value - minValue) * (maxValue - minValue))
                : value - minValue
    }

    public Int valueToProgress(final Int value) {
        return valueProgressMapper != null ? valueProgressMapper.valueToProgress(value) : valueToProgressHelper(value)
    }

    public Int progressToValue(final Int progress) {
        if (valueProgressMapper != null) {
            return valueProgressMapper.progressToValue(progress)
        }
        val value: Int = useLogScaling
                ? (Int) Math.round((minValue + (Double) Math.pow(progress, 2) / (maxValue - minValue)))
                : progress + minValue
        return stepSize > 0 ? progress == maxProgress ? maxValue : ((Int) Math.round((Double) value / stepSize)) * stepSize : value
    }

    protected String valueToShownValue(final Int value) {
        return getHasDecimals() ? String.format(Locale.US, "%.2f", (Float) value) : String.valueOf(value)
    }

    protected Int shownValueToValue(final Float shownValue) {
        return Math.round(shownValue)
    }

    public String getValueString(final Int progress) {
        if (progress == minProgress && minValueDescription != null) {
            return minValueDescription
        }
        if (progress == maxProgress && maxValueDescription != null) {
            return maxValueDescription
        }
        return valueToShownValue(progressToValue(progress)) + getUnitString()
    }

    /**
     * Get unit-label for progress value.
     *
     * @return string for the unit label
     */
    protected String getUnitString() {
        return unitValue
    }

    private Boolean atLeastMin(final SeekBar seekBar, final Int progress) {
        if (progress < minProgress) {
            seekBar.setProgress(minProgress)
            return false
        }
        return true
    }

    public Int getCurrentProgress() {
        return binding.preferenceSeekbar.getProgress()
    }

    // ------------------------------------------------------------------------
    // getter / setter methods (mainly for interfacing with preferences)
    // all of those may not be called anymore after init() has been called!

    public Int getMinValue() {
        return minValue
    }

    public Unit setMinValue(final Int minValue) {
        this.minValue = minValue
    }

    public Int getMaxValue() {
        return maxValue
    }

    public Unit setMaxValue(final Int maxValue) {
        this.maxValue = maxValue
    }

    public Int getMinProgress() {
        return minProgress
    }

    public Unit setMinProgress(final Int minProgress) {
        this.minProgress = minProgress
    }

    public Int getMaxProgress() {
        return maxProgress
    }

    public Unit setMaxProgress(final Int maxProgress) {
        this.maxProgress = maxProgress
    }

    public Unit setMinValueDescription(final String minValueDescription) {
        this.minValueDescription = minValueDescription
    }

    public Unit setMaxValueDescription(final String maxValueDescription) {
        this.maxValueDescription = maxValueDescription
    }

    public Unit setStepSize(final Int stepSize) {
        this.stepSize = stepSize
    }

    public Unit setStartProgress(final Int startProgress) {
        this.startProgress = startProgress
    }

    public Unit setUnitValue(final String unitValue) {
        this.unitValue = unitValue
    }

    public Unit setUseLogScaling(final Boolean useLogScaling) {
        this.useLogScaling = useLogScaling
    }

    /** can be overriden! so always use getHasDecimals instead of local member */
    public Boolean getHasDecimals() {
        return hasDecimals
    }

    public Unit setHasDecimals(final Boolean hasDecimals) {
        this.hasDecimals = hasDecimals
    }

    public ValueProgressMapper getValueProgressMapper() {
        return valueProgressMapper
    }

    public Unit setValueProgressMapper(final ValueProgressMapper valueProgressMapper) {
        this.valueProgressMapper = valueProgressMapper
    }

    public Unit setSaveProgressListener(final Action1<Integer> saveProgressListener) {
        this.saveProgressListener = saveProgressListener
    }

    /** read any non-default attributes here */
    public Unit loadAdditionalAttributes(final Context context, final TypedArray attrs, final Int defStyle) {
        // nothing to do per default
    }

}
