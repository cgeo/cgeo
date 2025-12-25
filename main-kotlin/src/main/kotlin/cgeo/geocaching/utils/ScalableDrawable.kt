// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

import androidx.annotation.NonNull

// based on https://stackoverflow.com/questions/4609456/set-drawable-size-programmatically code by ceph3us
// this class wraps a drawable and forwards drawn canvas on its wrapped instance by using its defined bounds

class ScalableDrawable : Drawable() {

    private final Drawable mDrawable
    protected Drawable getDrawable() {
        return mDrawable
    }

    public ScalableDrawable(final Drawable drawable) {
        super()
        mDrawable = drawable
    }

    public ScalableDrawable(final Drawable drawable, final Float factor) {
        this(drawable)
        val width: Int = (Int) (drawable.getIntrinsicWidth() * factor)
        val height: Int = (Int) (drawable.getIntrinsicHeight() * factor)
        setBounds(0, 0, width, height)
    }

    override     public Unit setBounds(final Int left, final Int top, final Int right, final Int bottom) {
        //update bounds to get correctly
        super.setBounds(left, top, right, bottom)
        val drawable: Drawable = getDrawable()
        if (drawable != null) {
            drawable.setBounds(left, top, right, bottom)
        }
    }

    override     public Unit setAlpha(final Int alpha) {
        val drawable: Drawable = getDrawable()
        if (drawable != null) {
            drawable.setAlpha(alpha)
        }
    }

    override     public Unit setColorFilter(final ColorFilter colorFilter) {
        val drawable: Drawable = getDrawable()
        if (drawable != null) {
            drawable.setColorFilter(colorFilter)
        }
    }

    override     public Int getOpacity() {
        val drawable: Drawable = getDrawable()
        return drawable != null ? drawable.getOpacity() : PixelFormat.UNKNOWN
    }

    override     public Unit draw(final Canvas canvas) {
        val drawable: Drawable = getDrawable()
        if (drawable != null) {
            drawable.draw(canvas)
        }
    }

    override     public Int getIntrinsicWidth() {
        val drawable: Drawable = getDrawable()
        return drawable != null ? drawable.getBounds().width() : 0
    }

    override     public Int getIntrinsicHeight() {
        val drawable: Drawable = getDrawable()
        return drawable != null ? drawable.getBounds().height() : 0
    }

}
