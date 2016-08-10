package cgeo.geocaching.maps;


import android.graphics.drawable.Drawable;


public class CacheMarker {

    private int hashCode;
    protected final Drawable drawable;

    public CacheMarker(final int hashCode, final Drawable drawable) {
        this.hashCode = hashCode;
        this.drawable = drawable;
    }

    /**
     * fallback contructor
     * @param drawable
     */
    public CacheMarker(final Drawable drawable) {
        this(0, drawable);
    }

    public Drawable getDrawable() {
        return drawable;
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

