package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.app.AlertDialog;
import android.content.Context;
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

public abstract class AbstractSeekbarPreference extends Preference {

    private TextView valueView;
    private int minValue = 0;
    private int maxValue = 100;
    private int startValue = 0;
    private String labelValue = "";
    private Context context = null;
    protected boolean hasDecimals = false;

    public AbstractSeekbarPreference(final Context context) {
        super(context);
        this.context = context;
        init();
    }

    public AbstractSeekbarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public AbstractSeekbarPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
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
        return hasDecimals ? String.format("%.2f", (float) value) : String.valueOf(value);
    }

    protected int shownValueToValue(final float shownValue) {
        return Math.round(shownValue);
    }

    protected void init() {
        setPersistent(false);
    }

    protected String getValueString(final int progress) {
        return valueToShownValue(progressToValue(progress));
    }

    private boolean atLeastMin(final SeekBar seekBar, final int progress) {
        if (progress < minValue) {
            seekBar.setProgress(minValue);
            return false;
        }
        return true;
    }

    // sets boundaries and initial value for seekbar
    protected void configure(final int minValue, final int maxValue, final int startValue, final String labelValue, final boolean hasDecimals) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.startValue = startValue;
        this.labelValue = labelValue;
        this.hasDecimals = hasDecimals;
    }

    // method to store the final value
    protected abstract void saveSetting(int progress);

    @Override
    protected View onCreateView(final ViewGroup parent) {
        final View v = super.onCreateView(parent);

        // get views
        final SeekBar seekBar = v.findViewById(R.id.preference_seekbar);
        valueView = v.findViewById(R.id.preference_seekbar_value_view);

        // init seekbar
        seekBar.setMax(maxValue);

        // set initial value
        final int threshold = startValue;
        valueView.setText(getValueString(threshold));
        seekBar.setProgress(threshold);

        // set label (if given)
        if (null != labelValue && !labelValue.isEmpty()) {
            final TextView labelView = v.findViewById(R.id.preference_seekbar_label_view);
            labelView.setVisibility(View.VISIBLE);
            labelView.setText(labelValue);
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
                .setTitle(String.format(context.getString(R.string.number_input_title), valueToShownValue(progressToValue(minValue)), valueToShownValue(progressToValue(maxValue))))
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, whichButton) -> {
                    int newValue;
                    try {
                        newValue = valueToProgress(shownValueToValue(Float.parseFloat(editText.getText().toString())));
                        if (newValue > maxValue) {
                            newValue = maxValue;
                            Toast.makeText(context, R.string.number_input_err_boundarymax, Toast.LENGTH_SHORT).show();
                        }
                        if (newValue < minValue) {
                            newValue = minValue;
                            Toast.makeText(context, R.string.number_input_err_boundarymin, Toast.LENGTH_SHORT).show();
                        }
                        seekBar.setProgress(newValue);
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
