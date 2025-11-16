package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.appcompat.app.AppCompatDelegate;

public class ColorUtils {
    private ColorUtils() {
        // utility class
    }

    /**
     * Calculates color luminance according to W3 standard
     * <p>
     * <a href="https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef">...</a>
     */
    private static double getLuminance(final int r, final int g, final int b) {
        final double[] components = {r / 255.0, g / 255.0, b / 255.0};
        for (int i = 0; i < components.length; i++) {
            if (components[i] <= 0.03928) {
                components[i] /= 12.92;
            } else {
                components[i] = Math.pow((components[i] + 0.055) / 1.055, 2.4);
            }
        }
        return components[0] * 0.2126 + components[1] * 0.7152 + components[2] * 0.0722;
    }

    /**
     * Calculates contrast ratio between two {@link Color} int according to W3
     * <p>
     * See <a href="https://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef">...</a>
     *
     * @param rgb1 first {@link Color} int
     * @param rgb2 second {@link Color} int
     * @return contrast ratio in range from 1 to 21
     */
    public static double getContrastRatio(@ColorInt final int rgb1, @ColorInt final int rgb2) {
        final double lumi1 = getLuminance(Color.red(rgb1), Color.green(rgb1), Color.blue(rgb1)) + 0.05;
        final double lumi2 = getLuminance(Color.red(rgb2), Color.green(rgb2), Color.blue(rgb2)) + 0.05;
        return Math.max(lumi1, lumi2) / Math.min(lumi1, lumi2);
    }

    public static String colorToString(@ColorInt final int color) {
        return "R:" + Color.red(color) + ";G:" + Color.green(color) + ";B:" + Color.blue(color) + ";A:" + Color.alpha(color);
    }

    @ColorInt
    public static int colorFromResource(@ColorRes final int colorRes) {
        return getThemedContext().getResources().getColor(colorRes);
    }

    public static Context getThemedContext() {
        final Context ctx = CgeoApplication.getInstance();
        final Resources res = ctx.getResources();
        final Configuration configuration = new Configuration(ctx.getResources().getConfiguration());
        final int nightNode = AppCompatDelegate.getDefaultNightMode();
        if (nightNode == AppCompatDelegate.MODE_NIGHT_NO) {
            configuration.uiMode = Configuration.UI_MODE_NIGHT_NO | (res.getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        } else if (nightNode == AppCompatDelegate.MODE_NIGHT_YES) {
            configuration.uiMode = Configuration.UI_MODE_NIGHT_YES | (res.getConfiguration().uiMode & ~Configuration.UI_MODE_NIGHT_MASK);
        } else {
            configuration.uiMode = res.getConfiguration().uiMode;
        }
        return ctx.createConfigurationContext(configuration);
    }

    public static float[] getHslValues(@ColorInt final int colorInt) {
        final int red = Color.red(colorInt);
        final int green = Color.green(colorInt);
        final int blue = Color.blue(colorInt);

        final float[] hsl = new float[3];
        androidx.core.graphics.ColorUtils.RGBToHSL(red, green, blue, hsl);
        return hsl;
    }

    public static int getColorFromHslValues(final float[] hslValues) {
        return androidx.core.graphics.ColorUtils.HSLToColor(hslValues);

    }
}
