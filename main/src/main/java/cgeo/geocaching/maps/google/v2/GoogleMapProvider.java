package cgeo.geocaching.maps.google.v2;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.AbstractMapProvider;
import cgeo.geocaching.maps.AbstractMapSource;
import cgeo.geocaching.maps.interfaces.MapItemFactory;
import cgeo.geocaching.maps.interfaces.MapProvider;
import cgeo.geocaching.maps.interfaces.MapSource;

import android.content.res.Resources;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;

public final class GoogleMapProvider extends AbstractMapProvider {

    public static final String GOOGLE_MAP_ID = "GOOGLE_MAP";
    public static final String GOOGLE_SATELLITE_ID = "GOOGLE_SATELLITE";

    private final MapItemFactory mapItemFactory;

    private GoogleMapProvider() {
        final Resources resources = CgeoApplication.getInstance().getResources();

        registerMapSource(new GoogleMapSource(this, resources.getString(R.string.map_source_google_map)));
        registerMapSource(new GoogleSatelliteSource(this, resources.getString(R.string.map_source_google_satellite)));
        registerMapSource(new GoogleTerrainSource(this, resources.getString(R.string.map_source_google_terrain)));

        mapItemFactory = new GoogleMapItemFactory();
    }

    private static class Holder {
        private static final GoogleMapProvider INSTANCE = new GoogleMapProvider();
    }

    public static GoogleMapProvider getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public Class<? extends AppCompatActivity> getMapClass() {
        return GoogleMapActivity.class;
    }

    @Override
    public int getMapViewId() {
        return R.id.map;
    }

    @Override
    public MapItemFactory getMapItemFactory() {
        return mapItemFactory;
    }

    @Override
    public boolean isSameActivity(final MapSource source1, final MapSource source2) {
        return true;
    }

    public abstract static class AbstractGoogleMapSource extends AbstractMapSource {
        public final boolean indoorEnabled;
        public final boolean supportsTheming;
        public final int mapType;

        protected AbstractGoogleMapSource(final MapProvider mapProvider, final String name, final int mapType, final boolean supportsTheming, final boolean indoorEnabled) {
            super(mapProvider, name);
            this.mapType = mapType;
            this.supportsTheming = supportsTheming;
            this.indoorEnabled = indoorEnabled;
        }

    }

    private static final class GoogleMapSource extends AbstractGoogleMapSource {

        GoogleMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, GoogleMap.MAP_TYPE_NORMAL, true, true);
        }

    }

    private static final class GoogleSatelliteSource extends AbstractGoogleMapSource {

        GoogleSatelliteSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, GoogleMap.MAP_TYPE_HYBRID, false, false);
        }

    }

    private static final class GoogleTerrainSource extends AbstractGoogleMapSource {

        GoogleTerrainSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name, GoogleMap.MAP_TYPE_TERRAIN, false, false);
        }

    }

}
