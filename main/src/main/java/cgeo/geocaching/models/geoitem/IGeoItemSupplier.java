package cgeo.geocaching.models.geoitem;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;

public interface IGeoItemSupplier {

    boolean isHidden();

    void setHidden(boolean isHidden);

    boolean hasData();

    //Collection<GeoPrimitive> getGeoData();

    default Viewport getViewport() {
        return getItem().getViewport();
    }

    default Geopoint getCenter() {
        return getViewport().getCenter();
    }

    GeoItem getItem();

}
