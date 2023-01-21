package cgeo.geocaching.location;

import java.util.Collection;

public interface IGeoDataProvider {

    String getId();

    boolean isHidden();

    void setHidden(boolean isHidden);

    boolean hasData();

    Collection<GeoObject> getGeoData();

    Viewport getViewport();

    default Geopoint getCenter() {
        return getViewport().getCenter();
    }

}
