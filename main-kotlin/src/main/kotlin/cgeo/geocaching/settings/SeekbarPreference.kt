// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.settings

import cgeo.geocaching.R
import cgeo.geocaching.ui.SeekbarUI

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

import org.apache.commons.lang3.StringUtils

class SeekbarPreference : Preference() {

    /**
     * for naming conventions see SeekBarUI
     * parameters for using in XML preferences: min, max, stepSize, logScaling, ui
     * (ui must be the canonical name of a class providing UI for this preference,
     * either SeekbarUI or a class derived from it)
     */

    private static val DEFAULT_UNSET: Int = -1
    private var restoreStoredValue: Boolean = true

    protected var context: Context = null
    private var seekbarUI: SeekbarUI = null

    // temporary place holders for SeekbarUI variables
    private var tempMinValue: Int = 0
    private var tempMaxValue: Int = 100
    private var tempDefaultValue: Int = DEFAULT_UNSET
    private var tempMinValueDescription: String = ""
    private var tempMaxValueDescription: String = ""
    private var tempStepSize: Int = 0
    private var tempHasDecimals: Boolean = false
    private var tempUseLogScaling: Boolean = false
    private var tempUnitValue: String = ""
    private var tempTemplate: String = ""
    private SeekbarUI.ValueProgressMapper tempValueProgressMapper = null
    private var tempAttributes: TypedArray = null

    public SeekbarPreference(final Context context) {
        this(context, null)
    }

    public SeekbarPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle)
    }

    public SeekbarPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        this.context = context
        initInternal(attrs)
    }

    /**
     * for programmatic creation
     */
    public SeekbarPreference(final Context context, final Int min, final Int max, final String unitValue, final SeekbarUI.ValueProgressMapper valueProgressMapper) {
        super(context, null, android.R.attr.preferenceStyle)
        this.context = context
        tempMinValue = min
        tempMaxValue = max
        tempUnitValue = unitValue == null ? "" : unitValue
        tempValueProgressMapper = valueProgressMapper
        initInternal(null)
    }

    private Unit initInternal(final AttributeSet attrs) {
        setPersistent(true)
        setLayoutResource(R.layout.preference_generic_extendable)

        // analyze given parameters
        tempAttributes = context.obtainStyledAttributes(attrs, R.styleable.SeekbarPreference)

        tempUseLogScaling = tempAttributes.getBoolean(R.styleable.SeekbarPreference_logScaling, tempUseLogScaling)
        tempMinValue = tempAttributes.getInt(R.styleable.SeekbarPreference_min, tempMinValue)
        tempMaxValue = tempAttributes.getInt(R.styleable.SeekbarPreference_max, tempMaxValue)
        tempMinValueDescription = tempAttributes.getString(R.styleable.SeekbarPreference_minValueDescription)
        tempMaxValueDescription = tempAttributes.getString(R.styleable.SeekbarPreference_maxValueDescription)
        tempStepSize = tempAttributes.getInt(R.styleable.SeekbarPreference_stepSize, tempStepSize)
        tempTemplate = tempAttributes.getString(R.styleable.SeekbarPreference_ui)
        tempHasDecimals = tempAttributes.getBoolean(R.styleable.SeekbarPreference_hasDecimals, tempHasDecimals)

        // recycle for tempAttributes is done in onBindViewHolder intentionally

        init()
    }

    /** @noinspection EmptyMethod*/
    protected Unit init() {
        // init method gets called once by the constructor,
        // so override and put initialization stuff in there (if needed)
    }

    protected Unit saveSetting(final Int progress) {
        if (callChangeListener(progress)) {
            persistInt(seekbarUI.progressToValue(progress))
            notifyChanged()
            // workaround for Android 7, as onCreateView() gets called unexpectedly after saveSetting(),
            // and the the old startValue would be used, leading to a jumping slider
            seekbarUI.setStartProgress(progress)
        }
    }

    override     protected Unit onSetInitialValue(final Boolean restoreValue, final Object defaultValue) {
        if (seekbarUI != null) {
            setInitialValue(restoreValue, defaultValue == null ? SeekbarUI.defaultValue : (Int) defaultValue)
            return
        }
        restoreStoredValue = restoreValue
        if (defaultValue != null) {
            tempDefaultValue = (Integer) defaultValue
        }
    }

    private Unit setInitialValue(final Boolean restoreValue, final Int defaultValue) {
        val defaultProgress: Int = seekbarUI.valueToProgress(restoreValue ? getPersistedInt(defaultValue) : defaultValue)
        seekbarUI.setStartProgress(defaultProgress < seekbarUI.getMinProgress() ? seekbarUI.getMinProgress() : Math.min(defaultProgress, seekbarUI.getMaxProgress()))
    }

    override     protected Object onGetDefaultValue(final TypedArray a, final Int index) {
        return a.getInt(index, SeekbarUI.defaultValue)
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)

        val hasTitle: Boolean = StringUtils.isNotBlank(((TextView) holder.findViewById(android.R.id.title)).getText())
        val hasSummary: Boolean = StringUtils.isNotBlank(((TextView) holder.findViewById(android.R.id.summary)).getText())
        if (!hasTitle) {
            holder.findViewById(android.R.id.title).setVisibility(View.GONE)
        }
        if (!hasSummary) {
            holder.findViewById(android.R.id.summary).setVisibility(View.GONE)
        }

        if (seekbarUI == null) {
            if (StringUtils.isBlank(tempTemplate)) {
                tempTemplate = SeekbarUI.class.getCanonicalName()
            }
            try {
                val c: Class = Class.forName(tempTemplate)
                final Class[] types = {Context.class}
                val constructor: Constructor = c.getConstructor(types)
                final Object[] parameters = {getContext()}
                seekbarUI = (SeekbarUI) constructor.newInstance(parameters)
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException |
                     NoSuchMethodException |
                     InvocationTargetException e) {
                throw RuntimeException(e)
            }

            if (tempAttributes != null) {
                seekbarUI.loadAdditionalAttributes(getContext(), tempAttributes, android.R.attr.preferenceStyle)
                tempAttributes.recycle()
                tempAttributes = null
            }

            seekbarUI.setMinValue(tempMinValue)
            seekbarUI.setMaxValue(tempMaxValue)
            seekbarUI.setMinProgress(seekbarUI.valueToProgress(tempMinValue))
            seekbarUI.setMaxProgress(seekbarUI.valueToProgress(tempMaxValue))
            seekbarUI.setMinValueDescription(tempMinValueDescription)
            seekbarUI.setMaxValueDescription(tempMaxValueDescription)
            seekbarUI.setStepSize(tempStepSize)
            seekbarUI.setHasDecimals(tempHasDecimals)
            seekbarUI.setUseLogScaling(tempUseLogScaling)
            seekbarUI.setUnitValue(tempUnitValue)
            seekbarUI.setValueProgressMapper(tempValueProgressMapper)
            seekbarUI.setSaveProgressListener(this::saveSetting)
            setInitialValue(restoreStoredValue, tempDefaultValue)
            seekbarUI.init()
        }
        setExtraView(holder)
    }

    private Unit setExtraView(final PreferenceViewHolder holder) {
        //if seekbar is still assigned to another group/viewholder, remove it from there. See #16635
        if (seekbarUI.getParent() is ViewGroup) {
            ((ViewGroup) seekbarUI.getParent()).removeView(seekbarUI)
        }

        val widget: FrameLayout = (FrameLayout) holder.findViewById(R.id.widget_extra)
        widget.removeAllViews()
        widget.addView(seekbarUI)
        widget.setVisibility(View.VISIBLE)
    }

    /** apply mapping to change notifications */
    override     public Boolean callChangeListener(final Object newValue) {
        val opcl: OnPreferenceChangeListener = getOnPreferenceChangeListener()
        return opcl == null || opcl.onPreferenceChange(this, seekbarUI.progressToValue((Int) newValue))
    }

}
