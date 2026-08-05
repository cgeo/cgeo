package cgeo.geocaching.utils.builders;

import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.DisplayUtils;
import static cgeo.geocaching.utils.DisplayUtils.SIZE_CACHE_MARKER_DP;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;

import java.util.List;

public class InsetBuilder {

    private final Drawable drawable;
    private final int drawableId;
    private final int gravity;
    private boolean doubleSize = false;
    private float scalingFactor = 1.0f;


    public InsetBuilder(final int drawableId, final int gravity) {
        this(drawableId, gravity, 1.0f);
    }

    public InsetBuilder(final int drawableId, final int gravity, final float scalingFactor) {
        this(drawableId, gravity, scalingFactor, false);
    }

    public InsetBuilder(final int drawableId, final float scalingFactor) {
        this(drawableId, 0, scalingFactor);
    }

    public InsetBuilder(final Drawable drawable) {
        this(drawable, 0);
    }

    public InsetBuilder(final Drawable drawable, final int gravity) {
        this.drawable = drawable;
        this.drawableId = 0;
        this.gravity = gravity;
    }

    public InsetBuilder(final int drawableId, final int gravity, final float scalingFactor, final boolean doubleSize) {
        this.drawableId = drawableId;
        this.drawable = null;
        this.gravity = gravity;
        this.doubleSize = doubleSize;
        this.scalingFactor = scalingFactor;
    }

    public int[] build(final Resources res, final List<Drawable> layers, final int width, final int height, final boolean mutate) {
        Drawable drawableToUse = this.drawable;
        if (drawableToUse == null) {
            drawableToUse = ViewUtils.getDrawable(drawableId, scalingFactor, mutate);
        }
        layers.add(drawableToUse);

        if (Build.VERSION.SDK_INT > 22) {
            // solution for API 23+, returns parameters for setLayerGravity and a flag for doubleSize
            return new int[]{doubleSize ? DisplayUtils.getPxFromDp(res, SIZE_CACHE_MARKER_DP, scalingFactor) : 0, gravity == 0 ? Gravity.CENTER : gravity};
        } else {
            // solution for API < 23, returns parameters for LayerDrawable.setLayerInset
            final int[] insetPadding = {0, 0, 0, 0}; // left, top, right, bottom padding for inset
            int iWidth = drawableToUse.getIntrinsicWidth();
            int iHeight = drawableToUse.getIntrinsicHeight();

            if (doubleSize) {
                iWidth *= 2;
                iHeight *= 2;
            }

            // cannot use Gravity.* constants here, as
            // LEFT/RIGHT contain CENTER_HORIZONTAL as well
            // and TOP/BOTTOM contain CENTER_VERTICAL as well

            // horizontal
            if ((gravity & 2) > 0) { // LEFT
                insetPadding[2] = width - iWidth;
            } else if ((gravity & 4) > 0) { // RIGHT
                insetPadding[0] = width - iWidth;
                insetPadding[2] = width - iWidth - insetPadding[0];
            } else { // CENTER
                insetPadding[0] = (width - iWidth) / 2;
                insetPadding[2] = insetPadding[0];
            }

            // vertical
            if ((gravity & 32) > 0) { // TOP
                insetPadding[3] = height - iHeight;
            } else if ((gravity & 64) > 0) { // BOTTOM
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
