package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.models.geoitem.GeoItem;
import cgeo.geocaching.models.geoitem.GeoItemUtils;
import cgeo.geocaching.models.geoitem.GeoStyle;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;
import cgeo.geocaching.unifiedmap.geoitemlayer.GeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;

public class RouteLayer {

    private final GeoItemLayer<String> geoItemLayer = new GeoItemLayer<>("route");

    public void init(final IProviderGeoItemLayer<?> provider) {
        geoItemLayer.setProvider(provider, 0);
    }

    public void destroy() {
        geoItemLayer.destroy();
    }

    public void updateRoute(final String key, final IGeoItemSupplier r, final int color, final int width) {

        final GeoStyle defaultStyle = GeoStyle.builder().setStrokeColor(color).setStrokeWidth((float) width).build();
        final GeoItem geoItem = GeoItemUtils.applyDefaultStyle(r.getItem(), defaultStyle);
        geoItemLayer.put(key, geoItem);
    }

    public void removeRoute(final String key) {
        geoItemLayer.remove(key);
    }

    public void setHidden(final String key, final boolean isHidden) {
        geoItemLayer.setVisibility(key, !isHidden);
    }

}
