package cgeo.geocaching.location;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** A list of geoobjects */
public class GeoObjectList implements IGeoDataProvider, Parcelable {

    private final List<GeoObject> objects = new ArrayList<>();
    private final List<GeoObject> objectsReadOnly = Collections.unmodifiableList(objects);
    private String name;
    private boolean isHidden;

    private Viewport viewport;

    public GeoObjectList() {
        //default constructur
    }

    public void setId(final String name) {
        this.name = name;
    }

    public String getId() {
        return name;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(final boolean isHidden) {
        this.isHidden = isHidden;
    }

    @Override
    public boolean hasData() {
        return !objects.isEmpty();
    }

    public void addAll(final Collection<GeoObject> objects) {
        invalidateMetadata();
        this.objects.addAll(objects);
    }

    public void add(final GeoObject ... objects) {
        invalidateMetadata();
        if (objects == null || objects.length == 0) {
            return;
        }
        if (objects.length == 1) {
            this.objects.add(objects[0]);
        } else {
            this.objects.addAll(Arrays.asList(objects));
        }
    }

    @Override
    public List<GeoObject> getGeoData() {
        return objectsReadOnly;
    }

    @Override
    public Viewport getViewport() {
        if (viewport == null) {
            recalculateMetadata();
        }
        return viewport;
    }

    public Geopoint getCenter() {
        if (viewport == null) {
            recalculateMetadata();
        }
        return viewport == null ? null : viewport.getCenter();
    }

    public List<GeoObject> getGeodata() {
        return this.objectsReadOnly;
    }

    private void recalculateMetadata() {
        final Viewport.ContainingViewportBuilder cvb = new Viewport.ContainingViewportBuilder();
        for (GeoObject go : this.objects) {
            cvb.add(go.points);
        }
        this.viewport = cvb.getViewport();
    }

    private void invalidateMetadata() {
        this.viewport = null;
    }

    // Parcelable implementation

    protected GeoObjectList(final Parcel in) {
        name = in.readString();
        isHidden = in.readByte() != 0;
        in.readList(objects, GeoObject.class.getClassLoader());
    }

    public static final Creator<GeoObjectList> CREATOR = new Creator<GeoObjectList>() {
        @Override
        public GeoObjectList createFromParcel(final Parcel in) {
            return new GeoObjectList(in);
        }

        @Override
        public GeoObjectList[] newArray(final int size) {
            return new GeoObjectList[size];
        }
    };



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(name);
        dest.writeByte((byte) (isHidden ? 1 : 0));
        dest.writeList(objects);
    }

}
