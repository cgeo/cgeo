package cgeo.geocaching.maps.google;

import cgeo.geocaching.R;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.maps.AbstractMapProvider;
import cgeo.geocaching.maps.AbstractMapSource;
import cgeo.geocaching.maps.MapProviderFactory;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

import com.google.android.maps.MapActivity;

import android.content.res.Resources;

import java.util.HashMap;
import java.util.Map;

public final class GoogleMapProvider extends AbstractMapProvider {

    private final static int MAP = 1;
    private final static int SATELLITE = 2;

    private final Map<Integer, MapSource> mapSources;

    private final MapItemFactory mapItemFactory;

    public GoogleMapProvider(final int baseid) {
        final Resources resources = cgeoapplication.getInstance().getResources();

        mapSources = new HashMap<Integer, MapSource>();
        mapSources.put(baseid + MAP, new GoogleMapSource(this, resources.getString(R.string.map_source_google_map)));
        mapSources.put(baseid + SATELLITE, new GoogleMapSatelliteSource(this, resources.getString(R.string.map_source_google_satellite)));

        mapItemFactory = new GoogleMapItemFactory();
    }

    @Override
    public Map<Integer, MapSource> getMapSources() {
        return mapSources;
    }

    public static boolean isSatelliteSource(final int sourceId) {
        final MapSource mapSource = MapProviderFactory.getMapSource(sourceId);
        return mapSource != null && mapSource instanceof GoogleMapSatelliteSource;
    }

    @Override
    public Class<? extends MapActivity> getMapClass() {
        return GoogleMapActivity.class;
    }

    @Override
    public int getMapViewId() {
        return R.id.map;
    }

    @Override
    public int getMapLayoutId() {
        return R.layout.map_google;
    }

    @Override
    public MapItemFactory getMapItemFactory() {
        return mapItemFactory;
    }

    @Override
    public boolean isSameActivity(int sourceId1, int sourceId2) {
        return true;
    }

    private static class GoogleMapSource extends AbstractMapSource {

        public GoogleMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name);
        }

    }

    private static final class GoogleMapSatelliteSource extends GoogleMapSource {

        public GoogleMapSatelliteSource(MapProvider mapProvider, String name) {
            super(mapProvider, name);
        }

    }

}
