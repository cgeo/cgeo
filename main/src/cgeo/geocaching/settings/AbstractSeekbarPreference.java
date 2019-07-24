package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import butterknife.ButterKnife;

public abstract class AbstractSeekbarPreference extends Preference {

    private TextView valueView;
    private int minValue = 0;
    private int maxValue = 100;
    private int startValue = 0;
    private String labelValue = "";

    public AbstractSeekbarPreference(final Context context) {
        super(context);
        init();
    }

    public AbstractSeekbarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AbstractSeekbarPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    protected void init() {
        setPersistent(false);
    }

    protected String getValueString(final int progress) {
        return String.valueOf(progress);
    }

    private boolean atLeastMin(final SeekBar seekBar, final int progress) {
        if (progress < minValue) {
            seekBar.setProgress(minValue);
            return false;
        }
        return true;
    }

    // sets boundaries and initial value for seekbar
    protected void configure(final int minValue, final int maxValue, final int startValue, final String labelValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.startValue = startValue;
        this.labelValue = labelValue;
    }

    // method to store the final value
    protected abstract void saveSetting(int progress);

    @Override
    protected View onCreateView(final ViewGroup parent) {
        final View v = super.onCreateView(parent);

        // get views
        final SeekBar seekBar = ButterKnife.findById(v, R.id.preference_seekbar);
        valueView = ButterKnife.findById(v, R.id.preference_seekbar_value_view);

        // init seekbar
        seekBar.setMax(maxValue);

        // set initial value
        final int threshold = startValue;
        valueView.setText(getValueString(threshold));
        seekBar.setProgress(threshold);

        // set label (if given)
        if (null != labelValue && !labelValue.isEmpty()) {
            final TextView labelView = ButterKnife.findById(v, R.id.preference_seekbar_label_view);
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

        return v;
    }
}
