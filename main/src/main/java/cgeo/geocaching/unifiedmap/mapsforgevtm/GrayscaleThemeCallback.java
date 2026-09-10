package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.utils.ColorUtils;

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
        return ColorUtils.toGrayscale(color);
    }

    @Override
    public int getColor(final String[] keys, final String[] values, final int color) {
        return ColorUtils.toGrayscale(color);
    }
}
