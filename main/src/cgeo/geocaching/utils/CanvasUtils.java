package cgeo.geocaching.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;

import java.util.ArrayList;

/**
 * Utility class to enhance the {@link Canvas}.
 */
public class CanvasUtils {

    private CanvasUtils() {
        // utility
    }

    /**
     * Render a path by actually drawing line segments instead. When using a real {@link Path}, then it is first painted
     * into a temporary bitmap by the CPU, before being rendered by the GPU. That simply fails for large paths,
     * therefore we use a synthesis from line segments, since lines are drawn entirely by the GPU.
     */
    public static void drawPath(final ArrayList<Point> pixelPoints, final Canvas canvas, final Paint paint) {
        final float[] pointData = new float[(pixelPoints.size() - 1) * 4];

        for (int i = 1; i < pixelPoints.size(); i++) {
            final Point last = pixelPoints.get(i - 1);
            final Point current = pixelPoints.get(i);

            final int index = (i - 1) * 4;
            // start point
            pointData[index] = last.x;
            pointData[index + 1] = last.y;

            // end point
            pointData[index + 2] = current.x;
            pointData[index + 3] = current.y;
        }

        canvas.drawLines(pointData, paint);
    }

}
