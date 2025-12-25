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

package cgeo.geocaching.utils

import cgeo.geocaching.CgeoApplication

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatDelegate

class ColorUtils {
    private ColorUtils() {
        // utility class
    }

    /**
     * Calculates color luminance according to W3 standard
     * <p>
     * <a href="https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef">...</a>
     */
    private static Double getLuminance(final Int r, final Int g, final Int b) {
        final Double[] components = {r / 255.0, g / 255.0, b / 255.0}
        for (Int i = 0; i < components.length; i++) {
            if (components[i] <= 0.03928) {
                components[i] /= 12.92
            } else {
                components[i] = Math.pow((components[i] + 0.055) / 1.055, 2.4)
            }
        }
        return components[0] * 0.2126 + components[1] * 0.7152 + components[2] * 0.0722
    }

    /**
     * Calculates contrast ratio between two {@link Color} Int according to W3
     * <p>
     * See <a href="https://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef">...</a>
     *
     * @param rgb1 first {@link Color} Int
     * @param rgb2 second {@link Color} Int
     * @return contrast ratio in range from 1 to 21
     */
    public static Double getContrastRatio(@ColorInt final Int rgb1, @ColorInt final Int rgb2) {
        val lumi1: Double = getLuminance(Color.red(rgb1), Color.green(rgb1), Color.blue(rgb1)) + 0.05
        val lumi2: Double = getLuminance(Color.red(rgb2), Color.green(rgb2), Color.blue(rgb2)) + 0.05
        return Math.max(lumi1, lumi2) / Math.min(lumi1, lumi2)
    }

    public static String colorToString(@ColorInt final Int color) {
        return "R:" + Color.red(color) + ";G:" + Color.green(color) + ";B:" + Color.blue(color) + ";A:" + Color.alpha(color)
    }

    @ColorInt
    public static Int colorFromResource(@ColorRes final Int colorRes) {
        return getThemedContext().getResources().getColor(colorRes)
    }

    public static Context getThemedContext() {
        val ctx: Context = CgeoApplication.getInstance()
        val res: Resources = ctx.getResources()
        val configuration: Configuration = Configuration(ctx.getResources().getConfiguration())
        val nightNode: Int = AppCompatDelegate.getDefaultNightMode()
        if (nightNode == AppCompatDelegate.MODE_NIGHT_NO) {
            configuration.uiMode = Configuration.UI_MODE_NIGHT_NO | (res.getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
        } else if (nightNode == AppCompatDelegate.MODE_NIGHT_YES) {
            configuration.uiMode = Configuration.UI_MODE_NIGHT_YES | (res.getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
        } else {
            configuration.uiMode = res.getConfiguration().uiMode
        }
        return ctx.createConfigurationContext(configuration)
    }

    public static Float[] getHslValues(@ColorInt final Int colorInt) {
        val red: Int = Color.red(colorInt)
        val green: Int = Color.green(colorInt)
        val blue: Int = Color.blue(colorInt)

        final Float[] hsl = Float[3]
        androidx.core.graphics.ColorUtils.RGBToHSL(red, green, blue, hsl)
        return hsl
    }

    public static Int getColorFromHslValues(final Float[] hslValues) {
        return androidx.core.graphics.ColorUtils.HSLToColor(hslValues)

    }
}
