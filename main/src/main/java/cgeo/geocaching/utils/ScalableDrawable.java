package cgeo.geocaching.utils;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

// based on https://stackoverflow.com/questions/4609456/set-drawable-size-programmatically code by ceph3us
// this class wraps a drawable and forwards drawn canvas on its wrapped instance by using its defined bounds

public class ScalableDrawable extends Drawable {

    private final Drawable mDrawable;
    protected Drawable getDrawable() {
        return mDrawable;
    }

    public ScalableDrawable(final Drawable drawable) {
        super();
        mDrawable = drawable;
    }

    public ScalableDrawable(final Drawable drawable, final float factor) {
        this(drawable);
        final int width = (int) (drawable.getIntrinsicWidth() * factor);
        final int height = (int) (drawable.getIntrinsicHeight() * factor);
        setBounds(0, 0, width, height);
    }

    @Override
    public void setBounds(final int left, final int top, final int right, final int bottom) {
        //update bounds to get correctly
        super.setBounds(left, top, right, bottom);
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setBounds(left, top, right, bottom);
        }
    }

    @Override
    public void setAlpha(final int alpha) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(final ColorFilter colorFilter) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        final Drawable drawable = getDrawable();
        return drawable != null ? drawable.getOpacity() : PixelFormat.UNKNOWN;
    }

    @Override
    public void draw(@NonNull final Canvas canvas) {
        final Drawable drawable = getDrawable();
        if (drawable != null) {
            drawable.draw(canvas);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        final Drawable drawable = getDrawable();
        return drawable != null ? drawable.getBounds().width() : 0;
    }

    @Override
    public int getIntrinsicHeight() {
        final Drawable drawable = getDrawable();
        return drawable != null ? drawable.getBounds().height() : 0;
    }

}
