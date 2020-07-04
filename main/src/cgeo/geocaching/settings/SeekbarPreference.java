package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class SeekbarPreference extends Preference {

    private TextView valueView;
    protected int minProgress = 0;
    protected int maxProgress = 100;
    protected int startProgress = 0;
    protected int defaultValue = 10;
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
        hasDecimals = a.getBoolean(R.styleable.SeekbarPreference_hasDecimals, hasDecimals);
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
        return hasDecimals ? String.format(Locale.getDefault(), "%.2f", (float) value) : String.valueOf(value);
    }

    protected int shownValueToValue(final float shownValue) {
        return Math.round(shownValue);
    }

    protected void init() {
        // init method gets called once by the constructor,
        // so override and put initialization stuff in there (if needed)
    }

    protected String getValueString(final int progress) {
        return valueToShownValue(progressToValue(progress));
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
        startProgress = valueToProgress(restoreValue ? getPersistedInt(defValue) : defValue);
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return valueToProgress(a.getInt(index, defaultValue));
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        final View v = super.onCreateView(parent);

        // get views
        final SeekBar seekBar = v.findViewById(R.id.preference_seekbar);
        valueView = v.findViewById(R.id.preference_seekbar_value_view);

        // init seekbar
        seekBar.setMax(maxProgress);

        // set initial value
        final int threshold = startProgress;
        valueView.setText(getValueString(threshold));
        seekBar.setProgress(threshold);

        // set label (if given)
        if (null != label && !label.isEmpty()) {
            final TextView labelView = v.findViewById(R.id.preference_seekbar_label_view);
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
            final EditText editText = new EditText(context);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL | (hasDecimals ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
            editText.setText(valueToShownValue(progressToValue(seekBar.getProgress())));

            new AlertDialog.Builder(context)
                .setTitle(String.format(context.getString(R.string.number_input_title), valueToShownValue(progressToValue(minProgress)), valueToShownValue(progressToValue(maxProgress))))
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    int newValue;
                    try {
                        newValue = valueToProgress(shownValueToValue(Float.parseFloat(editText.getText().toString())));
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
                })
                .setNegativeButton(android.R.string.cancel, (dialog, whichButton) -> { })
                .show()
            ;
        });

        return v;
    }
}
