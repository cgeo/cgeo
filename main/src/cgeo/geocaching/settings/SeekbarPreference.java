package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.util.Consumer;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.util.Locale;

public class SeekbarPreference extends Preference {

    private TextView valueView;
    protected int minProgress = 0;
    protected int maxProgress = 100;
    protected int startProgress = 0;
    protected final int defaultValue = 10;
    protected boolean hasDecimals = false;
    protected String label = "";
    protected Context context = null;

    public SeekbarPreference(final Context context) {
        this(context, null);
    }

    public SeekbarPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public SeekbarPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        setPersistent(true);
        setLayoutResource(R.layout.preference_seekbar);

        // analyze given parameters
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekbarPreference);
        minProgress = valueToProgress(a.getInt(R.styleable.SeekbarPreference_min, minProgress));
        maxProgress = valueToProgress(a.getInt(R.styleable.SeekbarPreference_max, maxProgress));
        final String temp = a.getString(R.styleable.SeekbarPreference_label);
        if (null != temp) {
            label = temp;
        }
        hasDecimals = a.getBoolean(R.styleable.SeekbarPreference_hasDecimals, useDecimals());
        a.recycle();

        init();
    }

    // naming convention:
    // progress     - the internal value of the android widget (0 to max) - always int
    // value        - actual value (may differ from "progress" if non linear or other boundaries) - always int
    // shownValue   - displayed value (may differ from "value", if conversions are involved) - in:float out:float or int converted to string

    protected int valueToProgressHelper(final int value) {
        return value;
    }

    protected int valueToProgress(final int value) {
        return valueToProgressHelper(value);
    }

    protected int progressToValue(final int progress) {
        return progress;
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
        return valueToShownValue(progressToValue(progress)) + getUnitString();
    }

    /**
     * Get unit-label for progress value.
     *
     * @return string for the unit label
     */
    protected String getUnitString() {
        return "";
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

        // init seekbar
        seekBar.setMax(maxProgress);

        // set initial value
        final int threshold = startProgress;
        valueView.setText(getValueString(threshold));
        seekBar.setProgress(threshold);

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
            final String title = String.format(context.getString(R.string.number_input_title), valueToShownValue(progressToValue(minProgress)), valueToShownValue(progressToValue(maxProgress)));
            final String defaultValue = valueToShownValue(progressToValue(seekBar.getProgress()));
            int inputType = InputType.TYPE_CLASS_NUMBER;
            if (useDecimals()) {
                inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
            }
            final Consumer<String> listener = input -> {
                int newValue;
                try {
                    newValue = valueToProgress(shownValueToValue(Float.parseFloat(input)));
                    if (newValue > maxProgress) {
                        newValue = maxProgress;
                        Toast.makeText(context, R.string.number_input_err_boundarymax, Toast.LENGTH_SHORT).show();
                    }
                    if (newValue < minProgress) {
                        newValue = minProgress;
                        Toast.makeText(context, R.string.number_input_err_boundarymin, Toast.LENGTH_SHORT).show();
                    }
                    seekBar.setProgress(newValue);
                    saveSetting(seekBar.getProgress());
                    valueView.setText(getValueString(newValue));
                } catch (NumberFormatException e) {
                    Toast.makeText(context, R.string.number_input_err_format, Toast.LENGTH_SHORT).show();
                }
            };
            SimpleDialog.of((Activity) context).setTitle(TextParam.text(title)).input(inputType, defaultValue, null, getUnitString(), listener);
        });
    }
}
