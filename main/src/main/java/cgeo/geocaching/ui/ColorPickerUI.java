package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.DisplayUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import java.util.Locale;

public class ColorPickerUI {

    private int colorScheme = 0;                    // currently selected color scheme
    private int color = 0xffff0000;                 // currently selected color (incl. opaqueness)
    private int originalColor = 0xffff0000;         // remember color on instantiating or ok-ing the dialog
    private int originalWidth = 0;                  // remember width on instantiating or ok-ing the dialog
    private int defaultColor = 0xffff0000;          // default value (for reset)
    private int defaultWidth = 0;                   // default value (for reset)
    private boolean hasDefaultValue = false;
    private boolean showOpaquenessSlider = false;

    private boolean showWidthSlider = false;

    private GridLayout colorSchemeGrid = null;
    private GridLayout colorGrid = null;
    private TextView opaquenessValue = null;
    private SeekBar opaquenessSlider = null;
    private TextView widthValue = null;
    private SeekBar widthSlider = null;
    private final Context context;

    private int iconSize = 0;
    private static final DisplayMetrics dm;
    private static final float[] radii;
    private static final int strokeWidth;

    static {
        dm = DisplayUtils.getDisplayMetrics();
        final float radius = DisplayUtils.getDisplayDensity() * 2.0f;
        radii = new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
        strokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm);
    }

    /** fires up ColorPicker UI */
    public ColorPickerUI(final Context context, final int originalColor, final int originalWidth, final boolean hasDefaultValue, final int defaultColor, final int defaultWidth, final boolean showOpaquenessSlider, final boolean showWidthSlider) {
        this.context = context;
        color = originalColor;
        this.originalColor = originalColor;
        this.originalWidth = originalWidth;
        this.hasDefaultValue = hasDefaultValue;
        this.defaultColor = defaultColor;
        this.defaultWidth = defaultWidth;
        this.showOpaquenessSlider = showOpaquenessSlider;
        this.showWidthSlider = showWidthSlider;
        Log.i("color=" + (color & 0xffffff) + ", scheme=" + getColorScheme() + ", opaqueness=" + getOpaqueness());

        // set icon size dynamically, based on screen dimensions
        iconSize = Math.max(50, Math.min(ColorPickerUI.dm.widthPixels, dm.heightPixels) / 10);
    }

    public void show(final ColorPickerResult valueCallback) {
        final View v = LayoutInflater.from(context).inflate(R.layout.preference_colorpicker, null);
        final AlertDialog.Builder builder = Dialogs.newBuilder(context);
        builder.setView(v);
        builder.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
            valueCallback.setColor(color, widthSlider.getProgress());
            if (null != opaquenessSlider) {
                opaquenessSlider.setProgress(getOpaqueness());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, ((dialog1, which) -> color = originalColor));
        if (hasDefaultValue) {
            builder.setNeutralButton(R.string.reset_to_default, null);
        }

        final AlertDialog dialog = builder.show();
        if (hasDefaultValue) {
            // override onClick listener to prevent closing dialog on pressing the "default" button
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v2 -> {
                color = defaultColor;
                widthSlider.setProgress(defaultWidth);
                final int opaqueness = getOpaqueness();
                if (null != opaquenessSlider) {
                    opaquenessSlider.setProgress(opaqueness);
                }
                selectOpaqueness(opaqueness);
            });
        }

        // initialize actual view

        colorSchemeGrid = v.findViewById(R.id.colorpicker_basegrid);
        colorSchemeGrid.setColumnCount(6);

        colorGrid = v.findViewById(R.id.colorpicker_colorgrid);
        colorGrid.setColumnCount(6);

        int opaqueness = getOpaqueness();
        opaquenessSlider = v.findViewById(R.id.colorpicker_opaqueness_slider);
        opaquenessValue = v.findViewById(R.id.colorpicker_opaqueness_value);

        if (showOpaquenessSlider) {
            v.findViewById(R.id.colorpicker_opaqueness_items).setVisibility(View.VISIBLE);
            configureSlider(opaquenessSlider, opaquenessValue, this::selectOpaqueness);
        } else {
            // without opaqueness slider don't use fully transparent colors
            if (opaqueness == 0) {
                opaqueness = 0xff;
            }
        }

        opaquenessSlider.setProgress(opaqueness);
        selectOpaqueness(opaqueness);

        widthSlider = v.findViewById(R.id.colorpicker_width_slider);
        widthValue = v.findViewById(R.id.colorpicker_width_value);

        if (showWidthSlider) {
            v.findViewById(R.id.colorpicker_width_items).setVisibility(View.VISIBLE);
            configureSlider(widthSlider, widthValue, this::selectWidth);
        }

        widthSlider.setProgress(originalWidth);
        selectWidth(originalWidth);
    }

    private void configureSlider(final SeekBar slider, final TextView value, final Consumer<Integer> progressChangedCallback) {
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(final SeekBar seekBar, final int progress, final boolean fromUser) {
                progressChangedCallback.accept(progress);
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

        value.setOnClickListener(v2 -> {
            final EditText editText = new EditText(context);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
            editText.setText(String.valueOf(slider.getProgress()));

            final int min = 0;
            final int max = slider.getMax();

            Dialogs.newBuilder(context)
                    .setTitle(String.format(context.getString(R.string.number_input_title), "" + min, "" + max))
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, (dialog2, whichButton) -> {
                        int newValue;
                        try {
                            newValue = Integer.parseInt(editText.getText().toString());
                            if (newValue > max) {
                                newValue = max;
                                Toast.makeText(context, R.string.number_input_err_boundarymax, Toast.LENGTH_SHORT).show();
                            }
                            if (newValue < min) {
                                newValue = min;
                                Toast.makeText(context, R.string.number_input_err_boundarymin, Toast.LENGTH_SHORT).show();
                            }
                            slider.setProgress(newValue);
                            progressChangedCallback.accept(slider.getProgress());
                        } catch (NumberFormatException e) {
                            Toast.makeText(context, R.string.number_input_err_format, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog2, whichButton) -> {
                    })
                    .show()
            ;
        });
    }

    private void initColorSchemeGrid() {
        final LayoutInflater inflater = LayoutInflater.from(context);
        colorSchemeGrid.removeAllViews();
        final int opaqueness = color & 0xff000000;
        for (int r = 0; r < 6; r++) {
            final int newColor = opaqueness + ((r * 51) << 16);
            colorSchemeGrid.addView(getIcon(inflater, colorSchemeGrid, r, newColor, r == colorScheme, this::selectColorScheme));
        }
    }

    private void initColorGrid(final int red) {
        final LayoutInflater inflater = LayoutInflater.from(context);
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

    public static void setViewColor(final ImageView imageView, final int color, final boolean selected) {
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
        opaquenessValue.setText(String.format(Locale.getDefault(), "%d%%", opaqueness * 100 / 255)); // format as percentage value
        color = ((opaqueness & 0xff) << 24) + (color & 0xffffff);
        colorScheme = getColorScheme();
        initColorSchemeGrid();
        initColorGrid(colorScheme);
    }

    private void selectWidth(final int width) {
        widthValue.setText(String.format(Locale.getDefault(), "%d", width));
    }

    public interface ColorPickerResult {
        void setColor(int color, int width);
    }

}
