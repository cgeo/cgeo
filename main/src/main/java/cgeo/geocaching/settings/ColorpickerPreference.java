package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.ColorPickerUI;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class ColorpickerPreference extends Preference {

    private boolean showOpaquenessSlider = false;   // show opaqueness slider?
    private int originalColor = 0xffff0000;         // remember currently selected color (incl. opaqueness)
    private int defaultColor = 0xffff0000;          // default value (for reset)
    private boolean hasDefaultValue = false;
    private boolean showLineWidthSlider = false;
    private String lineWidthPreferenceKey = null;
    private int originalWidth = 0;
    private int defaultWidth = 0;

    public ColorpickerPreference(final Context context) {
        this(context, null);
    }

    public ColorpickerPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public ColorpickerPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_colorpicker_widget);

        // get additional params, if given
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorpickerPreference, defStyle, 0);
        try {
            showOpaquenessSlider = a.getBoolean(R.styleable.ColorpickerPreference_showOpaquenessSlider, false);
            hasDefaultValue = a.hasValue(R.styleable.ColorpickerPreference_defaultColor);
            if (hasDefaultValue) {
                defaultColor = a.getColor(R.styleable.ColorpickerPreference_defaultColor, defaultColor);
            }
            showLineWidthSlider = a.hasValue(R.styleable.ColorpickerPreference_lineWidthKey);
            if (showLineWidthSlider) {
                lineWidthPreferenceKey = a.getString(R.styleable.ColorpickerPreference_lineWidthKey);
                defaultWidth = a.getInt(R.styleable.ColorpickerPreference_defaultLineWidth, 0);
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        originalColor = getPersistedInt(defaultColor);
        originalWidth = getSharedPreferences().getInt(lineWidthPreferenceKey, defaultWidth);

        final ImageView colorView = (ImageView) holder.findViewById(R.id.colorpicker_item);
        final TextView textView = (TextView) holder.findViewById(R.id.colorpicker_width_text);
        if (colorView != null) {
            ColorPickerUI.setViewColor(colorView, originalColor, false);
        }
        if (textView != null && showLineWidthSlider) {
            textView.setVisibility(View.VISIBLE);
            textView.setText(getContext().getString(R.string.preference_colorpicker_show_width, originalWidth));
        }
        final Preference pref = findPreferenceInHierarchy(getKey());
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                new ColorPickerUI(getContext(), originalColor, originalWidth, hasDefaultValue, defaultColor,  defaultWidth, showOpaquenessSlider, showLineWidthSlider).show(this::setValue);
                return false;
            });
        }
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInt(index, defaultColor);
    }

    public void setValue(final int newColor, final int newWidth) {
        if (callChangeListener(newColor)) {
            if (showLineWidthSlider) {
                originalWidth = newWidth;
                getSharedPreferences().edit().putInt(lineWidthPreferenceKey, newWidth).apply();
            }
            originalColor = newColor;
            persistInt(newColor);
            notifyChanged();
        }
    }

}
