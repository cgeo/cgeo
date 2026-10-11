package cgeo.geocaching.unifiedmap.mapsforge;

import cgeo.geocaching.utils.ColorUtils;

import org.mapsforge.map.layer.renderer.PolylineContainer;
import org.mapsforge.map.rendertheme.ThemeCallbackAdapter;
import org.mapsforge.map.rendertheme.renderinstruction.RenderInstruction;

/**
 * Turns every colour a render theme asks for into its grey equivalent.
 *
 * <p>Only the map itself goes through the theme, so caches, waypoints and routes keep their
 * colours and stand out against it - which is the point of the option.</p>
 *
 * <p>This covers the offline vector maps only. Raster tiles arrive already rendered and never
 * reach a theme; on this renderer they are decoded inside the shared
 * {@code AndroidGraphicFactory}, which offers no per-layer hook to convert them.</p>
 */
public class GrayscaleThemeCallback extends ThemeCallbackAdapter {

    @Override
    public int getColor(final RenderInstruction renderInstruction, final int color) {
        return ColorUtils.toGrayscale(color);
    }

    @Override
    public int getColor(final PolylineContainer way, final int color) {
        return ColorUtils.toGrayscale(color);
    }
}
