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

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.maps.CacheMarker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.SparseArray

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class BitmapDescriptorCache {

    /**
     * rely on unique hashcode of CacheMarker
     */
    protected val cache: SparseArray<BitmapDescriptor> = SparseArray<>()

    public BitmapDescriptor fromCacheMarker(final CacheMarker d) {
        BitmapDescriptor bd = cache.get(d.hashCode())
        if (bd == null) {
            bd = toBitmapDescriptor(d.getDrawable())
            cache.put(d.hashCode(), bd)
        }
        return bd
    }

    public static BitmapDescriptor toBitmapDescriptor(final Drawable d) {
        val canvas: Canvas = Canvas()
        val width: Int = d.getIntrinsicWidth()
        val height: Int = d.getIntrinsicHeight()
        val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        d.setBounds(0, 0, width, height)
        d.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

}
