package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.SeekbarBinding;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.functions.Action1;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Naming conventions:
 * - progress     - the internal value of the android widget (0 to max) - always int
 * - value        - actual value (may differ from "progress" if non linear or other boundaries) - always int
 * - shownValue   - displayed value (may differ from "value", if conversions are involved) - in:float out:float or int converted to string
 * This applies to derived values (like minProgress etc.) as well.
 * <br />
 * Parameters for using in XML preferences:
 * - min          - minimum allowed value
 * - max          - maximum allowed value
 * - stepSize     - value will be rounded down to nearest stepSize
 * - logScaling   - use logarithmic scaling for seekbar display
 */
public class SeekbarUI extends LinearLayout {

    private SeekbarBinding binding;
    private TextView valueView;
    protected int minValue = 0;
    protected int maxValue = 100;
    protected int minProgress = minValue;
    protected int maxProgress = minProgress;
    protected String minValueDescription;
    protected String maxValueDescription;
    protected int stepSize = 0;
    protected int startProgress = 0;
    public static final int defaultValue = 10;
    protected boolean hasDecimals = false;
    protected boolean useLogScaling = false;
    protected String unitValue = "";
    private ValueProgressMapper valueProgressMapper = null;
    private Action1<Integer> saveProgressListener = null;

    public interface ValueProgressMapper {
        int valueToProgress(int value);

        int progressToValue(int progress);
    }

    public static class FactorizeValueMapper implements ValueProgressMapper {
        private final int factor;

        public FactorizeValueMapper(final int factor) {
            this.factor = factor;
        }

        @Override
        public int valueToProgress(final int value) {
            return value / factor;
        }

        @Override
        public int progressToValue(final int progress) {
            return progress * factor;
        }
    }

    public SeekbarUI(final Context context) {
        this(context, null);
    }

    public SeekbarUI(final Context context, final @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public SeekbarUI(final Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SeekbarUI(final Context context, final @Nullable AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        createView();
        init();
    }

    private void createView() {
        setOrientation(VERTICAL);
        final ContextThemeWrapper ctw = new ContextThemeWrapper(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.seekbar, this);
        binding = SeekbarBinding.bind(this);
    }

    public void init() {
        // get views
        final SeekBar seekBar = binding.preferenceSeekbar;
        valueView = binding.preferenceSeekbarValueView;

        // init seekbar
        seekBar.setMax(maxProgress);
        seekBar.setProgress(startProgress);
        valueView.setText(getValueString(startProgress));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                if (fromUser && atLeastMin(seekBar, progress)) {
                    valueView.setText(getValueString(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                // abstract method needs to be implemented, but is not used here
            }

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if (atLeastMin(seekBar, seekBar.getProgress())) {
                    if (saveProgressListener != null) {
                        saveProgressListener.call(seekBar.getProgress());
                    }
                }
            }
        });

        valueView.setOnClickListener(v2 -> {
            final String currentValue = valueToShownValue(progressToValue(seekBar.getProgress()));
            int inputType = InputType.TYPE_CLASS_NUMBER;
            if (getHasDecimals()) {
                inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            }
            SimpleDialog.ofContext(getContext()).setTitle(TextParam.id(R.string.number_input_title, valueToShownValue(minValue), valueToShownValue(maxValue)))
                    .input(new SimpleDialog.InputOptions()
                            .setInputType(inputType)
                            .setInitialValue(currentValue)
                            .setSuffix(getUnitString()), input -> {
                        try {
                            final int newValue = (int) Dialogs.checkInputRange(getContext(), shownValueToValue(Float.parseFloat(input)), minValue, maxValue);
                            final int newProgress = valueToProgress(newValue);
                            seekBar.setProgress(newProgress);
                            if (saveProgressListener != null) {
                                saveProgressListener.call(seekBar.getProgress());
                            }
                            valueView.setText(getValueString(newProgress));
                        } catch (NumberFormatException e) {
                            ViewUtils.showShortToast(getContext(), R.string.number_input_err_format);
                        }
                    });
        });

    }

    // ------------------------------------------------------------------------

    protected int valueToProgressHelper(final int value) {
        return useLogScaling
                ? (int) Math.sqrt((long) (value - minValue) * (maxValue - minValue))
                : value - minValue;
    }

    public int valueToProgress(final int value) {
        return valueProgressMapper != null ? valueProgressMapper.valueToProgress(value) : valueToProgressHelper(value);
    }

    public int progressToValue(final int progress) {
        if (valueProgressMapper != null) {
            return valueProgressMapper.progressToValue(progress);
        }
        final int value = useLogScaling
                ? (int) Math.round((minValue + (double) Math.pow(progress, 2) / (maxValue - minValue)))
                : progress + minValue;
        return stepSize > 0 ? progress == maxProgress ? maxValue : ((int) Math.round((double) value / stepSize)) * stepSize : value;
    }

    protected String valueToShownValue(final int value) {
        return getHasDecimals() ? String.format(Locale.US, "%.2f", (float) value) : String.valueOf(value);
    }

    protected int shownValueToValue(final float shownValue) {
        return Math.round(shownValue);
    }

    public String getValueString(final int progress) {
        if (progress == minProgress && minValueDescription != null) {
            return minValueDescription;
        }
        if (progress == maxProgress && maxValueDescription != null) {
            return maxValueDescription;
        }
        return valueToShownValue(progressToValue(progress)) + getUnitString();
    }

    /**
     * Get unit-label for progress value.
     *
     * @return string for the unit label
     */
    protected String getUnitString() {
        return unitValue;
    }

    private boolean atLeastMin(final SeekBar seekBar, final int progress) {
        if (progress < minProgress) {
            seekBar.setProgress(minProgress);
            return false;
        }
        return true;
    }

    public int getCurrentProgress() {
        return binding.preferenceSeekbar.getProgress();
    }

    // ------------------------------------------------------------------------
    // getter / setter methods (mainly for interfacing with preferences)
    // all of those may not be called anymore after init() has been called!

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(final int minValue) {
        this.minValue = minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(final int maxValue) {
        this.maxValue = maxValue;
    }

    public int getMinProgress() {
        return minProgress;
    }

    public void setMinProgress(final int minProgress) {
        this.minProgress = minProgress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setMaxProgress(final int maxProgress) {
        this.maxProgress = maxProgress;
    }

    public void setMinValueDescription(final String minValueDescription) {
        this.minValueDescription = minValueDescription;
    }

    public void setMaxValueDescription(final String maxValueDescription) {
        this.maxValueDescription = maxValueDescription;
    }

    public void setStepSize(final int stepSize) {
        this.stepSize = stepSize;
    }

    public void setStartProgress(final int startProgress) {
        this.startProgress = startProgress;
    }

    public void setUnitValue(final String unitValue) {
        this.unitValue = unitValue;
    }

    public void setUseLogScaling(final boolean useLogScaling) {
        this.useLogScaling = useLogScaling;
    }

    /** can be overriden! so always use getHasDecimals instead of local member */
    public boolean getHasDecimals() {
        return hasDecimals;
    }

    public void setHasDecimals(final boolean hasDecimals) {
        this.hasDecimals = hasDecimals;
    }

    public ValueProgressMapper getValueProgressMapper() {
        return valueProgressMapper;
    }

    public void setValueProgressMapper(final ValueProgressMapper valueProgressMapper) {
        this.valueProgressMapper = valueProgressMapper;
    }

    public void setSaveProgressListener(final Action1<Integer> saveProgressListener) {
        this.saveProgressListener = saveProgressListener;
    }

    /** read any non-default attributes here */
    public void loadAdditionalAttributes(final Context context, final TypedArray attrs, final int defStyle) {
        // nothing to do per default
    }

}
