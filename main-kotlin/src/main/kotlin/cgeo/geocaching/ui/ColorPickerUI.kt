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

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.DisplayUtils
import cgeo.geocaching.utils.Log

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.core.util.Consumer

import java.util.Locale

class ColorPickerUI {

    private var colorScheme: Int = 0;                    // currently selected color scheme
    private var color: Int = 0xffff0000;                 // currently selected color (incl. opaqueness)
    private var originalColor: Int = 0xffff0000;         // remember color on instantiating or ok-ing the dialog
    private var originalWidth: Int = 0;                  // remember width on instantiating or ok-ing the dialog
    private var defaultColor: Int = 0xffff0000;          // default value (for reset)
    private var defaultWidth: Int = 0;                   // default value (for reset)
    private var hasDefaultValue: Boolean = false
    private var showOpaquenessSlider: Boolean = false

    private var showWidthSlider: Boolean = false

    private var colorSchemeGrid: GridLayout = null
    private var colorGrid: GridLayout = null
    private var opaquenessValue: TextView = null
    private var opaquenessSlider: SeekBar = null
    private var widthValue: TextView = null
    private var widthSlider: SeekBar = null
    private final Context context

    private var iconSize: Int = 0
    private static final DisplayMetrics dm
    private static final Float[] radii
    private static final Int strokeWidth

    static {
        dm = DisplayUtils.getDisplayMetrics()
        val radius: Float = DisplayUtils.getDisplayDensity() * 2.0f
        radii = Float[]{radius, radius, radius, radius, radius, radius, radius, radius}
        strokeWidth = (Int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm)
    }

    /** fires up ColorPicker UI */
    public ColorPickerUI(final Context context, final Int originalColor, final Int originalWidth, final Boolean hasDefaultValue, final Int defaultColor, final Int defaultWidth, final Boolean showOpaquenessSlider, final Boolean showWidthSlider) {
        this.context = context
        color = originalColor
        this.originalColor = originalColor
        this.originalWidth = originalWidth
        this.hasDefaultValue = hasDefaultValue
        this.defaultColor = defaultColor
        this.defaultWidth = defaultWidth
        this.showOpaquenessSlider = showOpaquenessSlider
        this.showWidthSlider = showWidthSlider
        Log.i("color=" + (color & 0xffffff) + ", scheme=" + getColorScheme() + ", opaqueness=" + getOpaqueness())

        // set icon size dynamically, based on screen dimensions
        iconSize = Math.max(50, Math.min(ColorPickerUI.dm.widthPixels, dm.heightPixels) / 10)
    }

    public Unit show(final ColorPickerResult valueCallback) {
        val v: View = LayoutInflater.from(context).inflate(R.layout.preference_colorpicker, null)
        final AlertDialog.Builder builder = Dialogs.newBuilder(context)
        builder.setView(v)
        builder.setPositiveButton(android.R.string.ok, (dialog1, which) -> {
            valueCallback.setColor(color, widthSlider.getProgress())
            if (null != opaquenessSlider) {
                opaquenessSlider.setProgress(getOpaqueness())
            }
        })
        builder.setNegativeButton(android.R.string.cancel, ((dialog1, which) -> color = originalColor))
        if (hasDefaultValue) {
            builder.setNeutralButton(R.string.reset_to_default, null)
        }

        val dialog: AlertDialog = builder.show()
        if (hasDefaultValue) {
            // override onClick listener to prevent closing dialog on pressing the "default" button
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v2 -> {
                color = defaultColor
                widthSlider.setProgress(defaultWidth)
                val opaqueness: Int = getOpaqueness()
                if (null != opaquenessSlider) {
                    opaquenessSlider.setProgress(opaqueness)
                }
                selectOpaqueness(opaqueness)
            })
        }

        // initialize actual view

        colorSchemeGrid = v.findViewById(R.id.colorpicker_basegrid)
        colorSchemeGrid.setColumnCount(6)

        colorGrid = v.findViewById(R.id.colorpicker_colorgrid)
        colorGrid.setColumnCount(6)

        Int opaqueness = getOpaqueness()
        opaquenessSlider = v.findViewById(R.id.colorpicker_opaqueness_slider)
        opaquenessValue = v.findViewById(R.id.colorpicker_opaqueness_value)

        if (showOpaquenessSlider) {
            v.findViewById(R.id.colorpicker_opaqueness_items).setVisibility(View.VISIBLE)
            configureSlider(opaquenessSlider, opaquenessValue, true, this::selectOpaqueness)
        } else {
            // without opaqueness slider don't use fully transparent colors
            if (opaqueness == 0) {
                opaqueness = 0xff
            }
        }

        opaquenessSlider.setProgress(opaqueness)
        selectOpaqueness(opaqueness)

        widthSlider = v.findViewById(R.id.colorpicker_width_slider)
        widthValue = v.findViewById(R.id.colorpicker_width_value)

        if (showWidthSlider) {
            v.findViewById(R.id.colorpicker_width_items).setVisibility(View.VISIBLE)
            configureSlider(widthSlider, widthValue, false, this::selectWidth)
        }

        widthSlider.setProgress(originalWidth)
        selectWidth(originalWidth)
    }

    private Unit configureSlider(final SeekBar slider, final TextView value, final Boolean inputAsPercent, final Consumer<Integer> progressChangedCallback) {
        val valueToShownValue: Float = inputAsPercent ? 100f / 255f : 1f

        slider.setOnSeekBarChangeListener(SeekBar.OnSeekBarChangeListener() {
            override             public Unit onProgressChanged(final SeekBar seekBar, final Int progress, final Boolean fromUser) {
                progressChangedCallback.accept(progress)
            }

            override             public Unit onStartTrackingTouch(final SeekBar seekBar) {
                // nothing to do
            }

            override             public Unit onStopTrackingTouch(final SeekBar seekBar) {
                // nothing to do
            }
        })

        value.setOnClickListener(v2 -> {
            val editText: EditText = EditText(context)
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL)
            editText.setText(String.valueOf((Int) Math.round(slider.getProgress() * valueToShownValue)))

            val min: Int = 0
            val max: Int = (Int) Math.round(slider.getMax() * valueToShownValue)

            Dialogs.newBuilder(context)
                    .setTitle(String.format(context.getString(R.string.number_input_title), "" + min, "" + max))
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, (dialog2, whichButton) -> {
                        Int newValue
                        try {
                            newValue = Integer.parseInt(editText.getText().toString())
                            if (newValue > max) {
                                newValue = max
                                ViewUtils.showShortToast(context, R.string.number_input_err_boundarymax)
                            }
                            if (newValue < min) {
                                newValue = min
                                ViewUtils.showShortToast(context, R.string.number_input_err_boundarymin)
                            }
                            slider.setProgress((Int) Math.round(newValue / valueToShownValue))
                            progressChangedCallback.accept(slider.getProgress())
                        } catch (NumberFormatException e) {
                            ViewUtils.showShortToast(context, R.string.number_input_err_format)
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog2, whichButton) -> {
                    })
                    .show()
            
        })
    }

    private Unit initColorSchemeGrid() {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        colorSchemeGrid.removeAllViews()
        val opaqueness: Int = color & 0xff000000
        for (Int r = 0; r < 6; r++) {
            val newColor: Int = opaqueness + ((r * 51) << 16)
            colorSchemeGrid.addView(getIcon(inflater, colorSchemeGrid, r, newColor, r == colorScheme, this::selectColorScheme))
        }
    }

    private Unit initColorGrid(final Int red) {
        val inflater: LayoutInflater = LayoutInflater.from(context)
        colorGrid.removeAllViews()
        val opaqueness: Int = color & 0xff000000
        for (Int green = 0; green < 6; green++) {
            for (Int blue = 0; blue < 6; blue++) {
                val newColor: Int = opaqueness + (((red * 51) << 16) + ((green * 51) << 8) + (blue * 51))
                colorGrid.addView(getIcon(inflater, colorGrid, newColor, newColor, newColor == color, this::selectColor))
            }
        }
    }

    private View getIcon(final LayoutInflater inflater, final GridLayout grid, final Int id, final Int newColor, final Boolean selected, final View.OnClickListener onClickListener) {
        val itemView: View = inflater.inflate(R.layout.preference_colorpicker_item, grid, false)
        itemView.findViewById(R.id.colorpicker_item).setLayoutParams(FrameLayout.LayoutParams(iconSize, iconSize))
        setViewColor(itemView.findViewById(R.id.colorpicker_item), newColor, selected)
        itemView.setId(id)
        itemView.setClickable(true)
        itemView.setFocusable(true)
        itemView.setOnClickListener(onClickListener)
        return itemView
    }

    public static Drawable getViewImage(final Drawable current, final Int color, final Boolean selected) {
        final GradientDrawable drawable
        if (current is GradientDrawable) {
            drawable = (GradientDrawable) current
        } else {
            drawable = GradientDrawable()
            drawable.setShape(selected ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE)
        }
        drawable.setCornerRadii(radii)
        drawable.setColor(color)
        drawable.setStroke(strokeWidth, Color.rgb(Color.red(color) * 224 / 256, Color.green(color) * 224 / 256, Color.blue(color) * 224 / 256))
        return drawable
    }

    public static Unit setViewColor(final ImageView imageView, final Int color, final Boolean selected) {
        imageView.setImageDrawable(getViewImage(imageView.getDrawable(), color, selected))
    }

    private Int getColorScheme() {
        return ((color & 0xffffff) >> 16) / 51
    }

    private Int getOpaqueness() {
        return (color >> 24) & 0xff
    }

    private Unit selectColor(final View button) {
        color = button.getId()
        initColorGrid(colorScheme)
    }

    private Unit selectColorScheme(final View button) {
        colorScheme = button.getId()
        initColorSchemeGrid()
        initColorGrid(colorScheme)
    }

    private Unit selectOpaqueness(final Int opaqueness) {
        opaquenessValue.setText(String.format(Locale.getDefault(), "%d%%", opaqueness * 100 / 255)); // format as percentage value
        color = ((opaqueness & 0xff) << 24) + (color & 0xffffff)
        colorScheme = getColorScheme()
        initColorSchemeGrid()
        initColorGrid(colorScheme)
    }

    private Unit selectWidth(final Int width) {
        widthValue.setText(String.format(Locale.getDefault(), "%d", width))
    }

    interface ColorPickerResult {
        Unit setColor(Int color, Int width)
    }

}
