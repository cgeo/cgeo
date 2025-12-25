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
import cgeo.geocaching.ui.ColorPickerUI

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

class ColorpickerPreference : Preference() {

    private var showOpaquenessSlider: Boolean = false;   // show opaqueness slider?
    private var originalColor: Int = 0xffff0000;         // remember currently selected color (incl. opaqueness)
    private var defaultColor: Int = 0xffff0000;          // default value (for reset)
    private var hasDefaultValue: Boolean = false
    private var showLineWidthSlider: Boolean = false
    private var lineWidthPreferenceKey: String = null
    private var originalWidth: Int = 0
    private var defaultWidth: Int = 0

    public ColorpickerPreference(final Context context) {
        this(context, null)
    }

    public ColorpickerPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle)
    }

    public ColorpickerPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        setWidgetLayoutResource(R.layout.preference_colorpicker_widget)

        // get additional params, if given
        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorpickerPreference, defStyle, 0)
        try {
            showOpaquenessSlider = a.getBoolean(R.styleable.ColorpickerPreference_showOpaquenessSlider, false)
            hasDefaultValue = a.hasValue(R.styleable.ColorpickerPreference_defaultColor)
            if (hasDefaultValue) {
                defaultColor = a.getColor(R.styleable.ColorpickerPreference_defaultColor, defaultColor)
            }
            showLineWidthSlider = a.hasValue(R.styleable.ColorpickerPreference_lineWidthKey)
            if (showLineWidthSlider) {
                lineWidthPreferenceKey = a.getString(R.styleable.ColorpickerPreference_lineWidthKey)
                defaultWidth = a.getInt(R.styleable.ColorpickerPreference_defaultLineWidth, 0)
            }
        } finally {
            a.recycle()
        }
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)
        holder.setDividerAllowedAbove(false)

        originalColor = getPersistedInt(defaultColor)
        originalWidth = getSharedPreferences().getInt(lineWidthPreferenceKey, defaultWidth)

        val colorView: ImageView = (ImageView) holder.findViewById(R.id.colorpicker_item)
        val textView: TextView = (TextView) holder.findViewById(R.id.colorpicker_width_text)
        if (colorView != null) {
            ColorPickerUI.setViewColor(colorView, originalColor, false)
        }
        if (textView != null && showLineWidthSlider) {
            textView.setVisibility(View.VISIBLE)
            textView.setText(getContext().getString(R.string.preference_colorpicker_show_width, originalWidth))
        }
        val pref: Preference = findPreferenceInHierarchy(getKey())
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                ColorPickerUI(getContext(), originalColor, originalWidth, hasDefaultValue, defaultColor,  defaultWidth, showOpaquenessSlider, showLineWidthSlider).show(this::setValue)
                return false
            })
        }
    }

    override     protected Object onGetDefaultValue(final TypedArray a, final Int index) {
        return a.getInt(index, defaultColor)
    }

    public Unit setValue(final Int newColor, final Int newWidth) {
        if (callChangeListener(newColor)) {
            if (showLineWidthSlider) {
                originalWidth = newWidth
                getSharedPreferences().edit().putInt(lineWidthPreferenceKey, newWidth).apply()
            }
            originalColor = newColor
            persistInt(newColor)
            notifyChanged()
        }
    }

}
