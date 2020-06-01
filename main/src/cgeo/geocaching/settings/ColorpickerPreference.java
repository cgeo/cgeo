package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.DisplayUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

public class ColorpickerPreference extends DialogPreference {

    private int colorScheme = 0;                    // currently selected color scheme
    private int color = 0xffff0000;                 // currently selected color (incl. opaqueness)
    private boolean showOpaquenessSlider = false;   // show opaqueness slider?

    private GridLayout colorSchemeGrid = null;
    private GridLayout colorGrid = null;
    private TextView opaquenessValue = null;
    private SeekBar opaquenessSlider = null;

    private int iconSize = 0;
    private final DisplayMetrics dm = DisplayUtils.getDisplayMetrics();
    private final float radius = DisplayUtils.getDisplayDensity() * 2.0f;
    private final float[] radii = { radius, radius, radius, radius, radius, radius, radius, radius };
    private final int strokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);

    public ColorpickerPreference(final Context context) {
        this(context, null);
    }

    public ColorpickerPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public ColorpickerPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_colorpicker_item);
        setDialogLayoutResource(R.layout.preference_colorpicker);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        // set icon size dynamically, based on screen dimensions
        iconSize = Math.max(50, Math.min(dm.widthPixels, dm.heightPixels) / 10);

        // get additional params, if given
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ColorpickerPreference, defStyle, 0);
        try {
            showOpaquenessSlider = a.getBoolean(R.styleable.ColorpickerPreference_showOpaquenessSlider, false);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);
        final ImageView colorView = view.findViewById(R.id.colorpicker_item);
        if (colorView != null) {
            setViewColor(colorView, color, false);
        }
    }

    @Override
    protected void onBindDialogView(final View v) {
        super.onBindDialogView(v);
        if (null != v) {
            colorSchemeGrid = v.findViewById(R.id.colorpicker_basegrid);
            colorSchemeGrid.setColumnCount(6);

            colorGrid = v.findViewById(R.id.colorpicker_colorgrid);
            colorGrid.setColumnCount(6);

            int opaqueness = getOpaqueness();
            opaquenessSlider = v.findViewById(R.id.colorpicker_opaqueness_slider);
            opaquenessValue = v.findViewById(R.id.colorpicker_opaqueness_value);

            if (showOpaquenessSlider) {
                v.findViewById(R.id.colorpicker_opaqueness_items).setVisibility(View.VISIBLE);
                opaquenessSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                        selectOpaqueness(progress);
                    }

                    @Override
                    public void onStartTrackingTouch(final SeekBar seekBar) {
                        // nothing to do
                    }

                    @Override
                    public void onStopTrackingTouch(final SeekBar seekBar) {
                        // nothing to do
                    }
                });
            } else {
                // without opaqueness slider don't use fully transparent colors
                if (opaqueness == 0) {
                    opaqueness = 0xff;
                }
            }

            opaquenessSlider.setProgress(opaqueness);
            selectOpaqueness(opaqueness);

        } else {
            throw new IllegalStateException("onBindDialogView without view?");
        }
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getInt(index, 0xffff0000);
    }

    @Override
    protected void onSetInitialValue(final boolean restoreValue, final Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
    }

    public void setValue(final int value) {
        if (callChangeListener(value)) {
            color = value;
            colorScheme = getColorScheme();
            if (null != opaquenessSlider) {
                opaquenessSlider.setProgress(getOpaqueness());
            }
            persistInt(value);
            notifyChanged();
        }
    }

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            setValue(color);
        }
    }

    private void initColorSchemeGrid() {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        colorSchemeGrid.removeAllViews();
        final int opaqueness = color & 0xff000000;
        for (int r = 0; r < 6; r++) {
            final int newColor = opaqueness + ((r * 51) << 16);
            colorSchemeGrid.addView(getIcon(inflater, colorSchemeGrid, r, newColor, r == colorScheme, this::selectColorScheme));
        }
    }

    private void initColorGrid(final int red) {
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        colorGrid.removeAllViews();
        final int opaqueness = color & 0xff000000;
        for (int green = 0; green < 6; green++) {
            for (int blue = 0; blue < 6; blue++) {
                final int newColor = opaqueness + (((red * 51) << 16) + ((green * 51) << 8) + (blue * 51));
                colorGrid.addView(getIcon(inflater, colorGrid, newColor, newColor, newColor == color, this::selectColor));
            }
        }
    }

    private View getIcon(final LayoutInflater inflater, final GridLayout grid, final int id, final int newColor, final boolean selected, final View.OnClickListener onClickListener) {
        final View itemView = inflater.inflate(R.layout.preference_colorpicker_item, grid, false);
        itemView.findViewById(R.id.colorpicker_item).setLayoutParams(new FrameLayout.LayoutParams(iconSize, iconSize));
        setViewColor(itemView.findViewById(R.id.colorpicker_item), newColor, selected);
        itemView.setId(id);
        itemView.setClickable(true);
        itemView.setFocusable(true);
        itemView.setOnClickListener(onClickListener);
        return itemView;
    }

    private void setViewColor(final ImageView imageView, final int color, final boolean selected) {
        final Drawable current = imageView.getDrawable();
        final GradientDrawable drawable;
        if (current instanceof GradientDrawable) {
            drawable = (GradientDrawable) current;
        } else {
            drawable = new GradientDrawable();
            drawable.setShape(selected ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE);
        }
        drawable.setCornerRadii(radii);
        drawable.setColor(color);
        drawable.setStroke(strokeWidth, Color.rgb(Color.red(color) * 224 / 256, Color.green(color) * 224 / 256, Color.blue(color) * 224 / 256));
        imageView.setImageDrawable(drawable);
    }

    private int getColorScheme() {
        return ((color & 0xffffff) >> 16) / 51;
    }

    private int getOpaqueness() {
        return (color >> 24) & 0xff;
    }

    private void selectColor(final View button) {
        color = button.getId();
        initColorGrid(colorScheme);
    }

    private void selectColorScheme(final View button) {
        colorScheme = button.getId();
        initColorSchemeGrid();
        initColorGrid(colorScheme);
    }

    private void selectOpaqueness(final int opaqueness) {
        opaquenessValue.setText(String.format(Locale.getDefault(), "%d", opaqueness));
        color = ((opaqueness & 0xff) << 24) + (color & 0xffffff);
        colorScheme = getColorScheme();
        initColorSchemeGrid();
        initColorGrid(colorScheme);
    }

}
