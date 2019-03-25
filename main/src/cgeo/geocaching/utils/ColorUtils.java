package cgeo.geocaching.utils;

import android.graphics.Color;

public class ColorUtils {
    private ColorUtils() {
        // utility class
    }

    /**
     * Calculates color luminance according to W3 standard
     * <p>
     * https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef
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
     * See https://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef
     *
     * @param rgb1 first {@link Color} int
     * @param rgb2 second {@link Color} int
     * @return contrast ratio in range from 1 to 21
     */
    public static double getContrastRatio(final int rgb1, final int rgb2) {
        final double lumi1 = getLuminance(Color.red(rgb1), Color.green(rgb1), Color.blue(rgb1)) + 0.05;
        final double lumi2 = getLuminance(Color.red(rgb2), Color.green(rgb2), Color.blue(rgb2)) + 0.05;
        return Math.max(lumi1, lumi2) / Math.min(lumi1, lumi2);
    }
}
