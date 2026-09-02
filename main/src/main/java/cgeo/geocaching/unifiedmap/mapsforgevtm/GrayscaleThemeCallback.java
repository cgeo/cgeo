package cgeo.geocaching.unifiedmap.mapsforgevtm;

import android.graphics.Color;

import org.oscim.theme.ThemeCallbackAdapter;
import org.oscim.theme.styles.RenderStyle;

/**
 * Turns every colour a render theme asks for into its grey equivalent.
 *
 * <p>Only the map itself goes through the theme, so caches, waypoints and routes keep their
 * colours and stand out against it - which is the point of the option.</p>
 */
public class GrayscaleThemeCallback extends ThemeCallbackAdapter {

    @Override
    public int getColor(final RenderStyle style, final int color) {
        return toGray(color);
    }

    @Override
    public int getColor(final String[] keys, final String[] values, final int color) {
        return toGray(color);
    }

    /** keeps the alpha channel and replaces the colour with its perceived brightness */
    public static int toGray(final int color) {
        final int gray = (int) Math.round(
                0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
        return Color.argb(Color.alpha(color), gray, gray, gray);
    }
}
