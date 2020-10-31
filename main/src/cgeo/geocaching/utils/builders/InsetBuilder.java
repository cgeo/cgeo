package cgeo.geocaching.utils.builders;

import cgeo.geocaching.compatibility.Compatibility;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.util.List;

public class InsetBuilder {
    private static final int[] FULLSIZE = {0, 0, 0, 0};

    public enum VERTICAL { TOP, CENTER, BOTTOM }

    public enum HORIZONTAL { LEFT, CENTER, RIGHT }

    private Drawable drawable = null;
    private int id;
    private VERTICAL vPos;
    private HORIZONTAL hPos;
    private boolean doubleSize = false;

    public InsetBuilder(final int id, final VERTICAL vPos, final HORIZONTAL hPos) {
        this.id = id;
        this.vPos = vPos;
        this.hPos = hPos;
    }

    public InsetBuilder(final int id) {
        this.id = id;
    }

    public InsetBuilder(final Drawable drawable) {
        this.drawable = drawable;
    }

    public InsetBuilder(final int id, final VERTICAL vPos, final HORIZONTAL hPos, final boolean doubleSize) {
        this.id = id;
        this.vPos = vPos;
        this.hPos = hPos;
        this.doubleSize = doubleSize;
    }

    public int[] build(final Resources res, final List<Drawable> layers, final int width, final int height) {
        if (drawable == null) {
            drawable = Compatibility.getDrawable(res, id);
        }

        layers.add(drawable);

        if (hPos == null || vPos == null) {
            return FULLSIZE;
        }

        return insetHelper(width, height, drawable, vPos, hPos);
    }

    private int[] insetHelper(final int width, final int height, final Drawable b, final VERTICAL vPos, final HORIZONTAL hPos) {
        final int[] insetPadding = {0, 0, 0, 0}; // left, top, right, bottom padding for inset
        int iWidth = b.getIntrinsicWidth();
        int iHeight = b.getIntrinsicHeight();

        if (doubleSize) {
            iWidth *= 2;
            iHeight *= 2;
        }

        // vertical offset from bottom:
        final int vDelta = height / 10;

        // horizontal
        if (hPos == HORIZONTAL.CENTER) {
            insetPadding[0] = (width - iWidth) / 2;
        } else if (hPos == HORIZONTAL.RIGHT) {
            insetPadding[0] = width - iWidth;
        }
        insetPadding[2] = width - iWidth - insetPadding[0];

        // vertical
        if (vPos == VERTICAL.CENTER) {
            insetPadding[1] = Math.max((height - iHeight) / 2 - vDelta, 0);
        } else if (vPos == VERTICAL.BOTTOM) {
            insetPadding[1] = Math.max(height - iHeight - vDelta, 0);
        }
        insetPadding[3] = height - iHeight - insetPadding[1];

        return insetPadding;
    }
}
