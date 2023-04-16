package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;

import java.util.Collection;

public interface IGeoItemSupplier {

    String getId();

    boolean isHidden();

    void setHidden(boolean isHidden);

    boolean hasData();

    Collection<GeoPrimitive> getGeoData();

    default Viewport getViewport() {
        return getItem().getViewport();
    }

    default Geopoint getCenter() {
        return getViewport().getCenter();
    }

    GeoItem getItem();

}
