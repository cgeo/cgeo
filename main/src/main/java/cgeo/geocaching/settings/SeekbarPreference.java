package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.util.Locale;

public class SeekbarPreference extends Preference {

    /**
     * Naming conventions:
     * - progress     - the internal value of the android widget (0 to max) - always int
     * - value        - actual value (may differ from "progress" if non linear or other boundaries) - always int
     * - shownValue   - displayed value (may differ from "value", if conversions are involved) - in:float out:float or int converted to string
     * This applies to derived values (like minProgress etc.) as well.
     *
     * Parameters for using in XML preferences:
     * - min          - minimum allowed value
     * - max          - maximum allowed value
     * - stepSize     - value will be rounded down to nearest stepSize
     * - logScaling   - use logarithmic scaling for seekbar display
      */

    private TextView valueView;
    protected int minValue = 0;
    protected int maxValue = 100;
    protected int minProgress = minValue;
    protected int maxProgress = minProgress;
    protected String minValueDescription;
    protected String maxValueDescription;
    protected int stepSize = 0;
    protected int startProgress = 0;
    protected final int defaultValue = 10;
    protected boolean hasDecimals = false;
    protected boolean useLogScaling = false;
    protected String label = "";
    protected String unitValue = "";
    protected Context context = null;
    private ValueProgressMapper valueProgressMapper = null;

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

    public SeekbarPreference(final Context context) {
        this(context, null);
    }

    public SeekbarPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public SeekbarPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        initInternal(attrs);
    }

    /**
     * for programmatic creation
     */
    public SeekbarPreference(final Context context, final int min, final int max, final String label, final String unitValue, final ValueProgressMapper valueProgressMapper) {
        super(context, null, android.R.attr.preferenceStyle);
        this.context = context;
        this.minValue = min;
        this.maxValue = max;
        this.label = label == null ? "" : label;
        this.unitValue = unitValue == null ? "" : unitValue;
        this.valueProgressMapper = valueProgressMapper;
        initInternal(null);
    }

    private void initInternal(final AttributeSet attrs) {
        setPersistent(true);
        setLayoutResource(R.layout.preference_seekbar);

        // analyze given parameters
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekbarPreference);
        useLogScaling = a.getBoolean(R.styleable.SeekbarPreference_logScaling, useLogScaling);
        minValue = a.getInt(R.styleable.SeekbarPreference_min, minValue);
        maxValue = a.getInt(R.styleable.SeekbarPreference_max, maxValue);
        minProgress = valueToProgress(minValue);
        maxProgress = valueToProgress(maxValue);
        minValueDescription = a.getString(R.styleable.SeekbarPreference_minValueDescription);
        maxValueDescription = a.getString(R.styleable.SeekbarPreference_maxValueDescription);
        stepSize = a.getInt(R.styleable.SeekbarPreference_stepSize, stepSize);
        final String temp = a.getString(R.styleable.SeekbarPreference_label);
        if (null != temp) {
            label = temp;
        }
        hasDecimals = a.getBoolean(R.styleable.SeekbarPreference_hasDecimals, useDecimals());
        a.recycle();

        init();
    }

    protected int valueToProgressHelper(final int value) {
        return useLogScaling
                ? (int) Math.sqrt((long) (value - minValue) * (maxValue - minValue))
                : value - minValue;
    }

    protected int valueToProgress(final int value) {
        return valueProgressMapper != null ? valueProgressMapper.valueToProgress(value) : valueToProgressHelper(value);
    }

    protected int progressToValue(final int progress) {
        if (valueProgressMapper != null) {
            return valueProgressMapper.progressToValue(progress);
        }
        final int value = useLogScaling
                ? (int) Math.round((minValue + (double) Math.pow(progress, 2) / (maxValue - minValue)))
                : progress + minValue;
        return stepSize > 0 ? progress == maxProgress ? maxValue : ((int) Math.round((double) value / stepSize)) * stepSize : value;
    }

    protected String valueToShownValue(final int value) {
        return useDecimals() ? String.format(Locale.US, "%.2f", (float) value) : String.valueOf(value);
    }

    protected int shownValueToValue(final float shownValue) {
        return Math.round(shownValue);
    }

    protected void init() {
        // init method gets called once by the constructor,
        // so override and put initialization stuff in there (if needed)
    }

    protected String getValueString(final int progress) {
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

    /**
     * hasDecimals is parameter from SeekbarPreference, but can be overwritten.
     * So use useDecimals instead of member
     *
     * @return hasDecimals
     */
    protected boolean useDecimals() {
        return hasDecimals;
    }

    private boolean atLeastMin(final SeekBar seekBar, final int progress) {
        if (progress < minProgress) {
            seekBar.setProgress(minProgress);
            return false;
        }
        return true;
    }

    protected void saveSetting(final int progress) {
        if (callChangeListener(progress)) {
            persistInt(progressToValue(progress));
            notifyChanged();
            // workaround for Android 7, as onCreateView() gets called unexpectedly after saveSetting(),
            // and the the old startValue would be used, leading to a jumping slider
            startProgress = progress;
        }
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        final int defValue = null != defaultValue ? (Integer) defaultValue : this.defaultValue;
        final int defaultProgress = valueToProgress(restoreValue ? getPersistedInt(defValue) : defValue);
        startProgress = defaultProgress < minProgress ? minProgress : Math.min(defaultProgress, maxProgress);
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInt(index, defaultValue);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        // get views
        final SeekBar seekBar = (SeekBar) holder.findViewById(R.id.preference_seekbar);
        valueView = (TextView) holder.findViewById(R.id.preference_seekbar_value_view);
        holder.setDividerAllowedAbove(false);

        // init seekbar
        seekBar.setMax(maxProgress);
        seekBar.setProgress(startProgress);
        valueView.setText(getValueString(startProgress));

        // set label (if given)
        if (null != label && !label.isEmpty()) {
            final TextView labelView = (TextView) holder.findViewById(R.id.preference_seekbar_label_view);
            labelView.setVisibility(View.VISIBLE);
            labelView.setText(label);
        }

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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
                    saveSetting(seekBar.getProgress());
                }
            }
        });

        valueView.setOnClickListener(v2 -> {
            final String currentValue = valueToShownValue(progressToValue(seekBar.getProgress()));
            int inputType = InputType.TYPE_CLASS_NUMBER;
            if (useDecimals()) {
                inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            }
            SimpleDialog.ofContext(context).setTitle(TextParam.id(R.string.number_input_title, valueToShownValue(minValue), valueToShownValue(maxValue)))
                    .input(new SimpleDialog.InputOptions()
                            .setInputType(inputType)
                            .setInitialValue(currentValue)
                            .setSuffix(getUnitString()), input -> {
                try {
                    final int newValue = (int) Dialogs.checkInputRange(getContext(), shownValueToValue(Float.parseFloat(input)), minValue, maxValue);
                    final int newProgress = valueToProgress(newValue);
                    seekBar.setProgress(newProgress);
                    saveSetting(seekBar.getProgress());
                    valueView.setText(getValueString(newProgress));
                } catch (NumberFormatException e) {
                    Toast.makeText(context, R.string.number_input_err_format, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /** apply mapping to change notifications */
    @Override
    public boolean callChangeListener(final Object newValue) {
        final OnPreferenceChangeListener opcl = getOnPreferenceChangeListener();
        return opcl == null || opcl.onPreferenceChange(this, progressToValue((int) newValue));
    }

}
