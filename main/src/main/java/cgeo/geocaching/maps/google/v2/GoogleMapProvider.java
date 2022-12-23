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

public final class GoogleMapProvider extends AbstractMapProvider {

    public static final String GOOGLE_MAP_ID = "GOOGLE_MAP";
    public static final String GOOGLE_SATELLITE_ID = "GOOGLE_SATELLITE";

    private final MapItemFactory mapItemFactory;

    private GoogleMapProvider() {
        final Resources resources = CgeoApplication.getInstance().getResources();

        registerMapSource(new GoogleMapSource(this, resources.getString(R.string.map_source_google_map)));
        registerMapSource(new GoogleSatelliteSource(this, resources.getString(R.string.map_source_google_satellite)));

        mapItemFactory = new GoogleMapItemFactory();
    }

    private static class Holder {
        private static final GoogleMapProvider INSTANCE = new GoogleMapProvider();
    }

    public static GoogleMapProvider getInstance() {
        return Holder.INSTANCE;
    }

    public static boolean isSatelliteSource(final MapSource mapSource) {
        return mapSource instanceof GoogleSatelliteSource;
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

    private abstract static class AbstractGoogleMapSource extends AbstractMapSource {

        protected AbstractGoogleMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name);
        }

    }

    private static final class GoogleMapSource extends AbstractGoogleMapSource {

        GoogleMapSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name);
        }

    }

    private static final class GoogleSatelliteSource extends AbstractGoogleMapSource {

        GoogleSatelliteSource(final MapProvider mapProvider, final String name) {
            super(mapProvider, name);
        }

    }

}
