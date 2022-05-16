package cgeo.geocaching.ui.renderer;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.ColorUtils;

import android.content.res.Resources;
import android.graphics.Color;

import net.nightwhistler.htmlspanner.ContrastPatcher;
import net.nightwhistler.htmlspanner.style.Style;

public class ColorNormalizer implements ContrastPatcher {

    /**
     * Minimal contrast ratio. If description:background contrast ratio is less than this value
     * for some string, the background color will be inverted in order to make it more readable.
     *
     * @see <a href="https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html">W3 Minimum Contrast</a>
     **/
    private static final float CONTRAST_THRESHOLD = 3.5f;

    private final Resources res;


    ColorNormalizer(final Resources res) {
        this.res = res;
    }

    @Override
    public Integer patchBackgroundColor(final Style style) {
        if (checkIsContrastProblematic(style) && !shouldInvertFontColor(style)) {
            return getBackgroundColor(style) ^ 0x00ffffff;
        }
        return style.getBackgroundColor();
    }

    @Override
    public Integer patchFontColor(final Style style) {
        if (checkIsContrastProblematic(style) && shouldInvertFontColor(style)) {
            return getFontColor(style) ^ 0x00ffffff;
        }
        return style.getColor();
    }

    private boolean checkIsContrastProblematic(final Style style) {

        final int backgroundColor = getBackgroundColor(style);
        final int fontColor = getFontColor(style);

        // If  background and foreground color are explicitly specified in the HTML,
        // this seems to be intended by the Owner even if the has bad contrast.
        if (style.getColor() != null && style.getBackgroundColor() != null) {
            return false;
        }

        return ColorUtils.getContrastRatio(fontColor, backgroundColor) < CONTRAST_THRESHOLD;
    }

    /**
     * Returns true, if the foreground color should be inverted, otherwise false
     */
    private boolean shouldInvertFontColor(final Style style) {
        return style.getColor() == null || style.getColor() == Color.BLACK || style.getColor() == Color.WHITE;
    }

    private int getBackgroundColor(final Style style) {
        return style.getBackgroundColor() != null ? style.getBackgroundColor() : res.getColor(R.color.colorBackground);
    }

    private int getFontColor(final Style style) {
        return style.getColor() != null ? style.getColor() : res.getColor(R.color.colorText);
    }
}
