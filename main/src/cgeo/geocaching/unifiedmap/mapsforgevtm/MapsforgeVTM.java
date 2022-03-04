package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.R;
import cgeo.geocaching.unifiedmap.AbstractUnifiedMap;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;

import androidx.appcompat.app.AppCompatActivity;

import org.oscim.android.MapView;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.ImperialUnitAdapter;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.theme.IRenderTheme;

public class MapsforgeVTM extends AbstractUnifiedMap {

    private IRenderTheme mTheme;
    private MapView mMapView;
    private Map mMap;

    @Override
    public void init(final AppCompatActivity activity) {
        activity.setContentView(R.layout.unifiedmap_mapsforgevtm);
        mMapView = activity.findViewById(R.id.mapViewVTM);
        mMap = mMapView.map();
    }

    private void startMap() {
        final DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mMap);
        mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH);
        mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
        mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);
        mapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT);

        final MapScaleBarLayer mapScaleBarLayer = new MapScaleBarLayer(mMap, mapScaleBar);
        final BitmapRenderer renderer = mapScaleBarLayer.getRenderer();
        renderer.setPosition(GLViewport.Position.BOTTOM_LEFT);
        renderer.setOffset(5 * CanvasAdapter.getScale(), 0);
        mMap.layers().add(mapScaleBarLayer);
    }

    @Override
    public void setTileSource(final AbstractTileProvider newSource) {
        super.setTileSource(newSource);
        // mMap.layers().clear();
        ((AbstractMapsforgeTileProvider) newSource).addTileLayer(mMap);
        startMap();
    }

    @Override
    public void applyTheme() {
        if (mTheme != null) {
            mTheme.dispose();
        }

        /*
        // zip based theme
        try {
            final List<String> xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new BufferedInputStream(ContentStorage.get().openForRead(PersistableFolder.OFFLINE_MAP_THEMES.getFolder(), "Elevate.zip"))));
            StringBuilder sb = new StringBuilder();
            for (String s : xmlThemes) {
                sb.append('"').append(s).append("\" ");
            }
            Log.e("found themes: " + sb.toString());
        } catch (IOException e) {
            Log.e(e.getMessage());
        }


        ThemeFile theme = null;
        try {
            theme = new ZipRenderTheme("Elevate.xml", new ZipXmlThemeResourceProvider(new ZipInputStream(new BufferedInputStream(ContentStorage.get().openForRead(PersistableFolder.OFFLINE_MAP_THEMES.getFolder(), "Elevate.zip")))));
        } catch (Exception ignore) {
            Log.e(ignore.getMessage());
        }
        mTheme = mMap.setTheme(theme);
        */

        mTheme = mMap.setTheme(VtmThemes.OSMARENDER);

    }

    @Override
    public void zoomToBounds(final BoundingBox bounds) {
        final MapPosition pos = new MapPosition();
        pos.setByBoundingBox(bounds, Tile.SIZE * 4, Tile.SIZE * 4);
        mMap.setMapPosition(pos);
    }

    public int getCurrentZoom() {
        return mMap.getMapPosition().getZoomLevel();
    };

    public void setZoom(final int zoomLevel) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setZoomLevel(zoomLevel);
        mMap.setMapPosition(pos);
    };

    // Lifecycle methods

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        mTheme.dispose();
        super.onDestroy();
    }
}
