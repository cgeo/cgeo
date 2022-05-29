package cgeo.geocaching.maps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class CacheMarker {

    private final int hashCode;
    protected final Drawable drawable;
    protected final Bitmap bitmap;

    public CacheMarker(final int hashCode, final Drawable drawable) {
        this.hashCode = hashCode;
        this.drawable = drawable;

        // prepare bitmap from drawable (used as map markers)
        bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
    }

    /**
     * fallback contructor
     *
     * @param drawable
     */
    public CacheMarker(final Drawable drawable) {
        this(0, drawable);
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CacheMarker that = (CacheMarker) o;

        if (hashCode == 0) {
            return this.drawable.equals(that.drawable);
        } else {
            return hashCode == that.hashCode;
        }
    }

    @Override
    public int hashCode() {
        return hashCode == 0 ? drawable.hashCode() : hashCode;
    }
}

