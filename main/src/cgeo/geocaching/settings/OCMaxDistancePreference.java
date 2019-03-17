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

public class OCMaxDistancePreference extends Preference {

    private int seekbarLength;
    private TextView valueView;

    public OCMaxDistancePreference(final Context context) {
        super(context);
        init();
    }

    public OCMaxDistancePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OCMaxDistancePreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setPersistent(false);
    }

    private int km2valueHelper(final double distance) {
        return (int) Math.round(25.0 * Math.log10(distance));
    }

    private int km2value(final double distance) {
        final int length = km2valueHelper(distance);
        if (length < 0) {
            return 0;
        }
        if (length > seekbarLength) {
            return seekbarLength;
        }
        return length;
    }

    private double value2km(final int value) {
        final double distance = Math.pow(10, Double.valueOf(value) / 25.0);
        if (distance < 0.0) {
            return 0.0;
        }
        if (distance > Settings.OCMAXDISTANCE_MAX) {
            return Settings.OCMAXDISTANCE_MAX;
        }
        return distance;
    }

    private String getValueString(final int progress) {
        final boolean useImperialUnits = Settings.useImperialUnits();
        final double distance = useImperialUnits ? value2km(progress) / IConversion.MILES_TO_KILOMETER : value2km(progress);
        return (distance >= 1000.0 ? String.format(Locale.US, "%d", Math.round(distance)) : String.format(Locale.US, "%.1f", distance))
                + (useImperialUnits ? " mi" : " km");
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        final View v = super.onCreateView(parent);

        // get views
        final SeekBar seekBar = ButterKnife.findById(v, R.id.preference_seekbar);
        valueView = ButterKnife.findById(v, R.id.preference_value_view);

        // init seekbar
        seekbarLength = km2valueHelper(Settings.OCMAXDISTANCE_MAX);
        seekBar.setMax(seekbarLength);

        // set initial value
        final int threshold = km2value(Settings.getOCmaxDistance());
        valueView.setText(getValueString(threshold));
        seekBar.setProgress(threshold);

        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                if (fromUser) {
                    valueView.setText(getValueString(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(final SeekBar seekBar) {
                // abstract method needs to be implemented, but is not used here
            }
            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                Settings.setOCmaxDistance((int) Math.round(value2km(seekBar.getProgress())));
            }
        });

        return v;
    }
}
