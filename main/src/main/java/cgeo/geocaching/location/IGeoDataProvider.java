package cgeo.geocaching.location;

import cgeo.geocaching.models.geoitem.GeoPrimitive;

import java.util.Collection;

public interface IGeoDataProvider {

    String getId();

    boolean isHidden();

    void setHidden(boolean isHidden);

    boolean hasData();

    Collection<GeoPrimitive> getGeoData();

    Viewport getViewport();

    default Geopoint getCenter() {
        return getViewport().getCenter();
    }

}
