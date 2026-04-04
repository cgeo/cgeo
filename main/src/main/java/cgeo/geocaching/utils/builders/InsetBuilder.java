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

    public int[] build(final Resources res, final List<Drawable> layers, final boolean mutate) {
        Drawable drawableToUse = this.drawable;
        if (drawableToUse == null) {
            drawableToUse = ViewUtils.getDrawable(drawableId, scalingFactor, mutate);
        }
        layers.add(drawableToUse);
        return new int[]{doubleSize ? DisplayUtils.getPxFromDp(res, SIZE_CACHE_MARKER_DP, scalingFactor) : 0, gravity == 0 ? Gravity.CENTER : gravity};
    }
}
