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

public class WpThresholdPreference extends Preference {

    private TextView valueView;

    public WpThresholdPreference(Context context) {
        super(context);
        init();
    }

    public WpThresholdPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WpThresholdPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setPersistent(false);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View v = super.onCreateView(parent);

        // get views
        SeekBar seekBar = (SeekBar) v.findViewById(R.id.wp_threshold_seekbar);
        valueView = (TextView) v.findViewById(R.id.wp_threshold_value_view);

        // init seekbar
        seekBar.setMax(Settings.SHOW_WP_THRESHOLD_MAX);

        // set initial value
        int threshold = Settings.getWayPointsThreshold();
        valueView.setText(String.valueOf(threshold));
        seekBar.setProgress(threshold);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    valueView.setText(String.valueOf(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Settings.setShowWaypointsThreshold(seekBar.getProgress());
            }
        });

        return v;
    }

}
