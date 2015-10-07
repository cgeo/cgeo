package cgeo.geocaching.settings;

import butterknife.ButterKnife;

import cgeo.geocaching.R;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class WpThresholdPreference extends Preference {

    private TextView valueView;

    public WpThresholdPreference(final Context context) {
        super(context);
        init();
    }

    public WpThresholdPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WpThresholdPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setPersistent(false);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        final View v = super.onCreateView(parent);

        // get views
        final SeekBar seekBar = ButterKnife.findById(v, R.id.wp_threshold_seekbar);
        valueView = ButterKnife.findById(v, R.id.wp_threshold_value_view);

        // init seekbar
        seekBar.setMax(Settings.SHOW_WP_THRESHOLD_MAX);

        // set initial value
        final int threshold = Settings.getWayPointsThreshold();
        valueView.setText(String.valueOf(threshold));
        seekBar.setProgress(threshold);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                if (fromUser) {
                    valueView.setText(String.valueOf(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                Settings.setShowWaypointsThreshold(seekBar.getProgress());
            }
        });

        return v;
    }

}
