package cgeo.geocaching.pebble;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

/**
 * Converts an Android Bitmap to a Pebble full screen color or 1-bit frame.
 */
public final class PebbleMapConverter {

    private PebbleMapConverter() {
        // utility class
    }

    public static byte[] to1Bit(final Bitmap source, final int targetWidth, final int targetHeight) {
        if (source == null || targetWidth <= 0 || targetHeight <= 0) {
            return new byte[0];
        }
        final Bitmap canvasBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawColor(Color.WHITE);

        final float scale = Math.max((float) targetWidth / source.getWidth(),
                                     (float) targetHeight / source.getHeight());
        final int newWidth = Math.round(source.getWidth() * scale);
        final int newHeight = Math.round(source.getHeight() * scale);
        final int x = (targetWidth - newWidth) / 2;
        final int y = (targetHeight - newHeight) / 2;
        canvas.drawBitmap(source,
                new Rect(0, 0, source.getWidth(), source.getHeight()),
                new Rect(x, y, x + newWidth, y + newHeight),
                null);

        final int bytesPerRow = (targetWidth + 7) / 8;
        final byte[] out = new byte[targetHeight * bytesPerRow];
        for (int py = 0; py < targetHeight; py++) {
            for (int xByte = 0; xByte < bytesPerRow; xByte++) {
                byte b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    final int px = xByte * 8 + bit;
                    if (px < targetWidth) {
                        final int p = canvasBitmap.getPixel(px, py);
                        if (Color.luminance(p) < 0.5f) {
                            b |= (1 << (7 - bit));
                        }
                    }
                }
                out[py * bytesPerRow + xByte] = b;
            }
        }
        canvasBitmap.recycle();
        return out;
    }

    private static final int[][] BAYER = {
            { 0,  8,  2, 10},
            {12,  4, 14,  6},
            { 3, 11,  1,  9},
            {15,  7, 13,  5}
    };

    public static byte[] toColor8(final Bitmap source, final int targetWidth, final int targetHeight) {
        if (source == null || targetWidth <= 0 || targetHeight <= 0) {
            return new byte[0];
        }
        final Bitmap canvasBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(canvasBitmap);
        canvas.drawColor(Color.BLACK);

        final float scale = Math.max((float) targetWidth / source.getWidth(),
                                     (float) targetHeight / source.getHeight());
        final int newWidth = Math.round(source.getWidth() * scale);
        final int newHeight = Math.round(source.getHeight() * scale);
        final int x = (targetWidth - newWidth) / 2;
        final int y = (targetHeight - newHeight) / 2;
        canvas.drawBitmap(source,
                new Rect(0, 0, source.getWidth(), source.getHeight()),
                new Rect(x, y, x + newWidth, y + newHeight),
                null);

        final byte[] out = new byte[targetHeight * targetWidth];
        for (int py = 0; py < targetHeight; py++) {
            for (int px = 0; px < targetWidth; px++) {
                final int p = canvasBitmap.getPixel(px, py);
                final int dither = ((BAYER[py & 3][px & 3] - 8) * 85) / 16;
                final int a = 0b11;
                final int r = quantizeChannel((Color.red(p) & 0xFF) + dither);
                final int g = quantizeChannel((Color.green(p) & 0xFF) + dither);
                final int b = quantizeChannel((Color.blue(p) & 0xFF) + dither);
                out[py * targetWidth + px] = (byte) ((a << 6) | (r << 4) | (g << 2) | b);
            }
        }
        canvasBitmap.recycle();
        return out;
    }

    private static int quantizeChannel(final int value) {
        final int v = Math.max(0, Math.min(255, value));
        return Math.min(3, (v + 42) / 85);
    }
}
