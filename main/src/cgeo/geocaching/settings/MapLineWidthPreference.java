package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

class MapLineWidthPreference extends AbstractSeekbarPreference {

    private int startValue = 0;

    MapLineWidthPreference(final Context context) {
        this(context, null);
    }

    MapLineWidthPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    MapLineWidthPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
        setPersistent(true);
        setLayoutResource(R.layout.preference_seekbar);
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        startValue = restoreValue ? getPersistedInt(getContext().getResources().getInteger(R.integer.default_linewidth)) : (Integer) defaultValue;
    }

    @Override
    protected void saveSetting(final int progress) {
        if (callChangeListener(progress)) {
            persistInt(progress);
            notifyChanged();
        }
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInt(index, getContext().getResources().getInteger(R.integer.default_linewidth));
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        configure(0, 50, startValue, null, false);
        return super.onCreateView(parent);
    }
}
