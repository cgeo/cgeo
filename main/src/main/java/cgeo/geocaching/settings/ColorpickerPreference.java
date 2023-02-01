package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.ColorPickerUI;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class ColorpickerPreference extends Preference {

    private int color = 0xffff0000;                 // currently selected color (incl. opaqueness)
    private boolean showOpaquenessSlider = false;   // show opaqueness slider?
    private int originalColor = 0xffff0000;         // remember color on instantiating or ok-ing the dialog
    private int defaultColor = 0xffff0000;          // default value (for reset)
    private boolean hasDefaultValue = false;

    public ColorpickerPreference(final Context context) {
        this(context, null);
    }

    public ColorpickerPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public ColorpickerPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_colorpicker_item);

        // get additional params, if given
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorpickerPreference, defStyle, 0);
        try {
            showOpaquenessSlider = a.getBoolean(R.styleable.ColorpickerPreference_showOpaquenessSlider, false);
            hasDefaultValue = a.hasValue(R.styleable.ColorpickerPreference_defaultColor);
            if (hasDefaultValue) {
                defaultColor = a.getColor(R.styleable.ColorpickerPreference_defaultColor, defaultColor);
                color = defaultColor;
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final ImageView colorView = (ImageView) holder.findViewById(R.id.colorpicker_item);
        if (colorView != null) {
            ColorPickerUI.setViewColor(colorView, color, false);
        }
        final Preference pref = findPreferenceInHierarchy(getKey());
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                new ColorPickerUI(getContext(), originalColor, hasDefaultValue, defaultColor, showOpaquenessSlider).show(this::setValue);
                return false;
            });
        }
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInt(index, defaultColor);
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        color = restoreValue ? getPersistedInt(defaultColor) : (Integer) defaultValue;
        setValue(color);
    }

    public void setValue(final int value) {
        if (callChangeListener(value)) {
            originalColor = color;
            color = value;
            persistInt(value);
            notifyChanged();
        }
    }

}
