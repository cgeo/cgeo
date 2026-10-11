package cgeo.geocaching.unifiedmap.googlemaps;

import cgeo.geocaching.maps.CacheMarker;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class BitmapDescriptorCache {

    /** upper bound for the bitmap-keyed cache. Prevents unbounded growth if marker appearances vary a lot */
    private static final int MAX_BITMAP_ENTRIES = 256;

    /**
     * rely on unique hashcode of CacheMarker
     */
    protected final SparseArray<BitmapDescriptor> cache = new SparseArray<>();

    /**
     * cache keyed by the source bitmap itself (identity based, Bitmap does not override equals/hashCode).
     * Used where only the bitmap is available - creating a descriptor per marker would allocate a full
     * ARGB_8888 copy for every single marker on the map.
     */
    private final Map<Bitmap, BitmapDescriptor> bitmapCache = new LinkedHashMap<Bitmap, BitmapDescriptor>(16, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<Bitmap, BitmapDescriptor> eldest) {
            return size() > MAX_BITMAP_ENTRIES;
        }
    };

    public BitmapDescriptor fromCacheMarker(final CacheMarker d) {
        BitmapDescriptor bd = cache.get(d.hashCode());
        if (bd == null) {
            bd = toBitmapDescriptor(d.getDrawable());
            cache.put(d.hashCode(), bd);
        }
        return bd;
    }

    /** Gets the descriptor for the given bitmap, creating it on first use */
    public BitmapDescriptor fromBitmap(final Bitmap bitmap) {
        BitmapDescriptor bd = bitmapCache.get(bitmap);
        if (bd == null) {
            bd = BitmapDescriptorFactory.fromBitmap(bitmap);
            bitmapCache.put(bitmap, bd);
        }
        return bd;
    }

    public void clear() {
        cache.clear();
        bitmapCache.clear();
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
