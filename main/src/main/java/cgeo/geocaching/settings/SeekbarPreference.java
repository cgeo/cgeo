package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.SeekbarUI;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.StringUtils;

public class SeekbarPreference extends Preference {

    /**
     * for naming conventions see SeekBarUI
     * parameters for using in XML preferences: min, max, stepSize, logScaling, ui
     * (ui must be the canonical name of a class providing UI for this preference,
     * either SeekbarUI or a class derived from it)
     */

    private static final int DEFAULT_UNSET = -1;
    private boolean restoreStoredValue = true;

    protected Context context = null;
    private SeekbarUI seekbarUI = null;

    // temporary place holders for SeekbarUI variables
    private int tempMinValue = 0;
    private int tempMaxValue = 100;
    private int tempDefaultValue = DEFAULT_UNSET;
    private String tempMinValueDescription = "";
    private String tempMaxValueDescription = "";
    private int tempStepSize = 0;
    private boolean tempHasDecimals = false;
    private boolean tempUseLogScaling = false;
    private String tempUnitValue = "";
    private String tempTemplate = "";
    private SeekbarUI.ValueProgressMapper tempValueProgressMapper = null;
    private TypedArray tempAttributes = null;

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
    public SeekbarPreference(final Context context, final int min, final int max, final String unitValue, final SeekbarUI.ValueProgressMapper valueProgressMapper) {
        super(context, null, android.R.attr.preferenceStyle);
        this.context = context;
        tempMinValue = min;
        tempMaxValue = max;
        tempUnitValue = unitValue == null ? "" : unitValue;
        tempValueProgressMapper = valueProgressMapper;
        initInternal(null);
    }

    private void initInternal(final AttributeSet attrs) {
        setPersistent(true);
        setLayoutResource(R.layout.preference_generic_extendable);

        // analyze given parameters
        tempAttributes = context.obtainStyledAttributes(attrs, R.styleable.SeekbarPreference);

        tempUseLogScaling = tempAttributes.getBoolean(R.styleable.SeekbarPreference_logScaling, tempUseLogScaling);
        tempMinValue = tempAttributes.getInt(R.styleable.SeekbarPreference_min, tempMinValue);
        tempMaxValue = tempAttributes.getInt(R.styleable.SeekbarPreference_max, tempMaxValue);
        tempMinValueDescription = tempAttributes.getString(R.styleable.SeekbarPreference_minValueDescription);
        tempMaxValueDescription = tempAttributes.getString(R.styleable.SeekbarPreference_maxValueDescription);
        tempStepSize = tempAttributes.getInt(R.styleable.SeekbarPreference_stepSize, tempStepSize);
        tempTemplate = tempAttributes.getString(R.styleable.SeekbarPreference_ui);
        tempHasDecimals = tempAttributes.getBoolean(R.styleable.SeekbarPreference_hasDecimals, tempHasDecimals);

        // recycle for tempAttributes is done in onBindViewHolder intentionally

        init();
    }

    /** @noinspection EmptyMethod*/
    protected void init() {
        // init method gets called once by the constructor,
        // so override and put initialization stuff in there (if needed)
    }

    protected void saveSetting(final int progress) {
        if (callChangeListener(progress)) {
            persistInt(seekbarUI.progressToValue(progress));
            notifyChanged();
            // workaround for Android 7, as onCreateView() gets called unexpectedly after saveSetting(),
            // and the the old startValue would be used, leading to a jumping slider
            seekbarUI.setStartProgress(progress);
        }
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        if (seekbarUI != null) {
            setInitialValue(restoreValue, defaultValue == null ? SeekbarUI.defaultValue : (int) defaultValue);
            return;
        }
        restoreStoredValue = restoreValue;
        if (defaultValue != null) {
            tempDefaultValue = (Integer) defaultValue;
        }
    }

    private void setInitialValue(final boolean restoreValue, final int defaultValue) {
        final int defaultProgress = seekbarUI.valueToProgress(restoreValue ? getPersistedInt(defaultValue) : defaultValue);
        seekbarUI.setStartProgress(defaultProgress < seekbarUI.getMinProgress() ? seekbarUI.getMinProgress() : Math.min(defaultProgress, seekbarUI.getMaxProgress()));
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInt(index, SeekbarUI.defaultValue);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final boolean hasTitle = StringUtils.isNotBlank(((TextView) holder.findViewById(android.R.id.title)).getText());
        final boolean hasSummary = StringUtils.isNotBlank(((TextView) holder.findViewById(android.R.id.summary)).getText());
        if (!hasTitle) {
            holder.findViewById(android.R.id.title).setVisibility(View.GONE);
        }
        if (!hasSummary) {
            holder.findViewById(android.R.id.summary).setVisibility(View.GONE);
        }

        if (seekbarUI == null) {
            if (StringUtils.isBlank(tempTemplate)) {
                tempTemplate = SeekbarUI.class.getCanonicalName();
            }
            try {
                final Class c = Class.forName(tempTemplate);
                final Class[] types = {Context.class};
                final Constructor constructor = c.getConstructor(types);
                final Object[] parameters = {getContext()};
                seekbarUI = (SeekbarUI) constructor.newInstance(parameters);
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException |
                     NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            if (tempAttributes != null) {
                seekbarUI.loadAdditionalAttributes(getContext(), tempAttributes, android.R.attr.preferenceStyle);
                tempAttributes.recycle();
                tempAttributes = null;
            }

            seekbarUI.setMinValue(tempMinValue);
            seekbarUI.setMaxValue(tempMaxValue);
            seekbarUI.setMinProgress(seekbarUI.valueToProgress(tempMinValue));
            seekbarUI.setMaxProgress(seekbarUI.valueToProgress(tempMaxValue));
            seekbarUI.setMinValueDescription(tempMinValueDescription);
            seekbarUI.setMaxValueDescription(tempMaxValueDescription);
            seekbarUI.setStepSize(tempStepSize);
            seekbarUI.setHasDecimals(tempHasDecimals);
            seekbarUI.setUseLogScaling(tempUseLogScaling);
            seekbarUI.setUnitValue(tempUnitValue);
            seekbarUI.setValueProgressMapper(tempValueProgressMapper);
            seekbarUI.setSaveProgressListener(this::saveSetting);
            setInitialValue(restoreStoredValue, tempDefaultValue);
            seekbarUI.init();
        }
        setExtraView(holder);
    }

    private void setExtraView(final PreferenceViewHolder holder) {
        //if seekbar is still assigned to another group/viewholder, remove it from there. See #16635
        if (seekbarUI.getParent() instanceof ViewGroup) {
            ((ViewGroup) seekbarUI.getParent()).removeView(seekbarUI);
        }

        final FrameLayout widget = (FrameLayout) holder.findViewById(R.id.widget_extra);
        widget.removeAllViews();
        widget.addView(seekbarUI);
        widget.setVisibility(View.VISIBLE);
    }

    /** apply mapping to change notifications */
    @Override
    public boolean callChangeListener(final Object newValue) {
        final OnPreferenceChangeListener opcl = getOnPreferenceChangeListener();
        return opcl == null || opcl.onPreferenceChange(this, seekbarUI.progressToValue((int) newValue));
    }

}
