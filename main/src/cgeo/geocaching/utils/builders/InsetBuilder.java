package cgeo.geocaching.utils.builders;

import cgeo.geocaching.utils.DisplayUtils;
import static cgeo.geocaching.utils.DisplayUtils.SIZE_CACHE_MARKER_DP;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;

import androidx.core.content.res.ResourcesCompat;

import java.util.List;

public class InsetBuilder {
    private static final int[] FULLSIZE = {0, 0, 0, 0};

    private static int markerSizeUnscaled = 0;

    private Drawable drawable = null;
    private int id;
    private int pos;
    private boolean doubleSize = false;

    public InsetBuilder(final int id, final int pos) {
        this.id = id;
        this.pos = pos;
    }

    public InsetBuilder(final int id) {
        this.id = id;
    }

    public InsetBuilder(final Drawable drawable) {
        this.drawable = drawable;
    }

    public InsetBuilder(final Drawable drawable, final int pos) {
        this.drawable = drawable;
        this.pos = pos;
    }

    public InsetBuilder(final int id, final int pos, final boolean doubleSize) {
        this.id = id;
        this.pos = pos;
        this.doubleSize = doubleSize;
    }

    public int[] build(final Resources res, final List<Drawable> layers, final int width, final int height) {
        if (drawable == null) {
            drawable = ResourcesCompat.getDrawable(res, id, null);
        }
        layers.add(drawable);

        if (Build.VERSION.SDK_INT > 22) {
            // solution for API 23+, returns parameters for setLayerGravity and a flag for doubleSize
            if (markerSizeUnscaled == 0) {
                markerSizeUnscaled = DisplayUtils.getPxFromDp(res, SIZE_CACHE_MARKER_DP, 1.0f);
            }
            return new int[]{doubleSize ? markerSizeUnscaled : 0, pos == 0 ? Gravity.CENTER : pos};
        } else {
            // solution for API < 23, returns parameters for LayerDrawable.setLayerInset
            final int[] insetPadding = {0, 0, 0, 0}; // left, top, right, bottom padding for inset
            int iWidth = drawable.getIntrinsicWidth();
            int iHeight = drawable.getIntrinsicHeight();

            if (doubleSize) {
                iWidth *= 2;
                iHeight *= 2;
            }

            // cannot use Gravity.* constants here, as
            // LEFT/RIGHT contain CENTER_HORIZONTAL as well
            // and TOP/BOTTOM contain CENTER_VERTICAL as well

            // horizontal
            if ((pos & 2) > 0) { // LEFT
                insetPadding[2] = width - iWidth;
            } else if ((pos & 4) > 0) { // RIGHT
                insetPadding[0] = width - iWidth;
                insetPadding[2] = width - iWidth - insetPadding[0];
            } else { // CENTER
                insetPadding[0] = (width - iWidth) / 2;
                insetPadding[2] = insetPadding[0];
            }

            // vertical
            if ((pos & 32) > 0) { // TOP
                insetPadding[3] = height - iHeight;
            } else if ((pos & 64) > 0) { // BOTTOM
                insetPadding[1] = Math.max(height - iHeight, 0);
                insetPadding[3] = height - iHeight - insetPadding[1];
            } else { // CENTER
                insetPadding[1] = Math.max((height - iHeight) / 2, 0);
                insetPadding[3] = insetPadding[1];
            }

            return insetPadding;
        }
    }
}
