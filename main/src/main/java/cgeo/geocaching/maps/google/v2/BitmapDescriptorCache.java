package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.maps.CacheMarker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class BitmapDescriptorCache {

    /**
     * rely on unique hashcode of CacheMarker
     */
    protected final SparseArray<BitmapDescriptor> cache = new SparseArray<>();

    public BitmapDescriptor fromCacheMarker(final CacheMarker d) {
        BitmapDescriptor bd = cache.get(d.hashCode());
        if (bd == null) {
            bd = toBitmapDescriptor(d.getDrawable());
            cache.put(d.hashCode(), bd);
        }
        return bd;
    }

    public static BitmapDescriptor toBitmapDescriptor(final Drawable d) {
        final Canvas canvas = new Canvas();
        final int width = d.getIntrinsicWidth();
        final int height = d.getIntrinsicHeight();
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        d.setBounds(0, 0, width, height);
        d.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

}
