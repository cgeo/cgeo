package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.location.IConversion;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.util.Locale;

import butterknife.ButterKnife;

public class BrouterThresholdPreference extends Preference {

    private TextView valueView;

    public BrouterThresholdPreference(final Context context) {
        super(context);
        init();
    }

    public BrouterThresholdPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BrouterThresholdPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setPersistent(false);
    }

    private String getValueString(final int progress) {
        return Settings.useImperialUnits()
            ? String.format(Locale.US, "%.1f mi", progress / IConversion.MILES_TO_KILOMETER)
            : String.valueOf(progress) + " km"
        ;
    }

    private boolean atLeastOne(final SeekBar seekBar, final int progress) {
        if (progress < 1) {
            seekBar.setProgress(1);
            return false;
        }
        return true;
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        final View v = super.onCreateView(parent);

        // get views
        final SeekBar seekBar = ButterKnife.findById(v, R.id.brouter_threshold_seekbar);
        valueView = ButterKnife.findById(v, R.id.brouter_threshold_value_view);

        // init seekbar
        seekBar.setMax(Settings.BROUTER_THRESHOLD_MAX);

        // set initial value
        final int threshold = Settings.getBrouterThreshold();
        valueView.setText(getValueString(threshold));
        seekBar.setProgress(threshold);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                if (fromUser && atLeastOne(seekBar, progress)) {
                    valueView.setText(getValueString(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                // abstract method needs to be implemented, but is not used here
            }
            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                if (atLeastOne(seekBar, seekBar.getProgress())) {
                    Settings.setBrouterThreshold(seekBar.getProgress());
                }
            }
        });

        return v;
    }
}
