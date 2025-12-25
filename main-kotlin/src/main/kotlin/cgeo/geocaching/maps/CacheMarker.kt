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

package cgeo.geocaching.maps

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable

class CacheMarker {

    private final Int hashCode
    protected final Drawable drawable
    protected final Bitmap bitmap

    public CacheMarker(final Int hashCode, final Drawable drawable) {
        this.hashCode = hashCode
        this.drawable = drawable

        // prepare bitmap from drawable (used as map markers)
        bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight())
        drawable.draw(canvas)
    }

    public Drawable getDrawable() {
        return drawable
    }

    public Bitmap getBitmap() {
        return bitmap
    }

    override     public Boolean equals(final Object o) {
        if (this == o) {
            return true
        }
        if (o == null || getClass() != o.getClass()) {
            return false
        }

        val that: CacheMarker = (CacheMarker) o

        if (hashCode == 0) {
            return this.drawable == (that.drawable)
        } else {
            return hashCode == that.hashCode
        }
    }

    override     public Int hashCode() {
        return hashCode == 0 ? drawable.hashCode() : hashCode
    }
}

