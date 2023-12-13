package cgeo.geocaching.location;

import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;

import android.os.Parcel;
import android.os.Parcelable;

/** A list of geoobjects */
public class GeoItemHolder implements IGeoItemSupplier, Parcelable {

    private final GeoItem item;

    private boolean isHidden;

    public GeoItemHolder(final GeoItem item) {
        this.item = item;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(final boolean isHidden) {
        this.isHidden = isHidden;
    }

    @Override
    public boolean hasData() {
        return item != null && item.isValid();
    }

    @Override
    public Viewport getViewport() {
        return item == null ? null : item.getViewport();
    }

    public Geopoint getCenter() {
        return item == null ? null : item.getCenter();
    }

    @Override
    public GeoItem getItem() {
        return item;
    }

    // Parcelable implementation

    protected GeoItemHolder(final Parcel in) {
        isHidden = in.readByte() != 0;
        item = in.readParcelable(GeoItem.class.getClassLoader());
    }

    public static final Creator<GeoItemHolder> CREATOR = new Creator<GeoItemHolder>() {
        @Override
        public GeoItemHolder createFromParcel(final Parcel in) {
            return new GeoItemHolder(in);
        }

        @Override
        public GeoItemHolder[] newArray(final int size) {
            return new GeoItemHolder[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeByte((byte) (isHidden ? 1 : 0));
        dest.writeParcelable(item, flags);
    }

}
