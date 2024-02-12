package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.mapsforgevtm.VtmThemes;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.Log;
import static cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider.isValidMapFile;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.oscim.android.MapView;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.oscim.theme.ZipRenderTheme;
import org.oscim.theme.ZipXmlThemeResourceProvider;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MapInfo;
import org.oscim.utils.animation.Easing;
import static org.oscim.map.Animator.ANIM_ROTATE;

public class TinyFragment extends AbstractMapFragment implements XmlRenderThemeMenuCallback {

    private MapView mapView;
    private IRenderTheme theme;

    public TinyFragment() {
        super(R.layout.fragment_map);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = new MapView(getActivity());
        final RelativeLayout relativeLayout = view.findViewById(R.id.mapView);
        relativeLayout.addView(mapView);

        // Open map
        List<ImmutablePair<String, Uri>> offlineMaps =
                CollectionStream.of(ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true))
                        .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith("germany (oam).map") && isValidMapFile(fi.uri))
                        .map(fi -> new ImmutablePair<>(StringUtils.capitalize(StringUtils.substringBeforeLast(fi.name, ".")), fi.uri)).toList();
        if (offlineMaps.size() != 1) {
            offlineMaps =
                    CollectionStream.of(ContentStorage.get().list(PersistableFolder.OFFLINE_MAPS, true))
                            .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith("berlin (oam).map") && isValidMapFile(fi.uri))
                            .map(fi -> new ImmutablePair<>(StringUtils.capitalize(StringUtils.substringBeforeLast(fi.name, ".")), fi.uri)).toList();
        }
        loadMap(offlineMaps.get(0).right); // "germany (oam).map" or "berlin (oam).map", whatever is found first

        // try to load Elevate.zip theme; use built-in VTM.DEFAULT otherwise
        List<ImmutablePair<String, Uri>> themes =
                CollectionStream.of(ContentStorage.get().list(PersistableFolder.OFFLINE_MAP_THEMES, true))
                        .filter(fi -> !fi.isDirectory && fi.name.toLowerCase(Locale.getDefault()).endsWith("elevate.zip"))
                        .map(fi -> new ImmutablePair<>(StringUtils.capitalize(StringUtils.substringBeforeLast(fi.name, ".")), fi.uri)).toList();
        loadTheme(themes.size() > 0 ? themes.get(0).right : null);
    }

    private void loadMap(final Uri mapUri) {
        try {
            // Tile source
            final MapFileTileSource tileSource = new MapFileTileSource();
            final FileInputStream fis = (FileInputStream) getActivity().getContentResolver().openInputStream(mapUri);
            tileSource.setMapFileInputStream(fis);

            // Vector layer
            final VectorTileLayer tileLayer = mapView.map().setBaseMap(tileSource);

            // Building layer
            mapView.map().layers().add(new BuildingLayer(mapView.map(), tileLayer));

            // Label layer
            mapView.map().layers().add(new LabelLayer(mapView.map(), tileLayer));

            if (theme == null) {
                theme = mapView.map().setTheme(VtmThemes.DEFAULT);
            }

            // Scale bar
            final MapScaleBar mapScaleBar = new DefaultMapScaleBar(mapView.map());
            final MapScaleBarLayer mapScaleBarLayer = new MapScaleBarLayer(mapView.map(), mapScaleBar);
            mapScaleBarLayer.getRenderer().setPosition(GLViewport.Position.BOTTOM_LEFT);
            mapScaleBarLayer.getRenderer().setOffset(5 * CanvasAdapter.getScale(), 0);
            mapView.map().layers().add(mapScaleBarLayer);

            // initial position
            final MapInfo info = tileSource.getMapInfo();
            if (!info.boundingBox.contains(mapView.map().getMapPosition().getGeoPoint())) {
                final MapPosition pos = new MapPosition();
                pos.setByBoundingBox(info.boundingBox, Tile.SIZE * 4, Tile.SIZE * 4);
                mapView.map().setMapPosition(pos);
            }

        } catch (Exception e) {
            Log.e("loadMap: " + e.getMessage());
        }
    }

    private void loadTheme(final Uri themeUri) {
        try {
            if (theme != null) {
                theme.dispose();
            }

            // Render theme (themeUri may be null, using default theme then)
            if (themeUri != null) {
                final List<String> xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new BufferedInputStream(getActivity().getContentResolver().openInputStream(themeUri))));
                if (xmlThemes.isEmpty()) {
                    return;
                }
                final ThemeFile themeFile = new ZipRenderTheme(xmlThemes.get(0), new ZipXmlThemeResourceProvider(new ZipInputStream(new BufferedInputStream(getActivity().getContentResolver().openInputStream(themeUri)))));
                theme = mapView.map().setTheme(themeFile);
            } else {
                theme = mapView.map().setTheme(VtmThemes.DEFAULT);
            }

        } catch (Exception e) {
            Log.e("loadTheme: " + e.getMessage());
        }
    }

    @Override
    public Set<String> getCategories(final XmlRenderThemeStyleMenu menu) {
        // ignore theme settings for now
        return new HashSet<>();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        if (theme != null) {
            theme.dispose();
            theme = null;
        }
        super.onDestroyView();
    }

    @Override
    public boolean supportsTileSource(final AbstractTileProvider newSource) {
        return true;
    }

    @Override
    public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return null;
    }

    @Override
    public void setCenter(final Geopoint geopoint) {
        final MapPosition pos = mapView.map().getMapPosition();
        pos.setPosition(geopoint.getLatitude(), geopoint.getLongitude());
        mapView.map().setMapPosition(pos);
    }

    @Override
    public Geopoint getCenter() {
        final MapPosition pos = mapView.map().getMapPosition();
        return new Geopoint(pos.getLatitude(), pos.getLongitude());
    }

    @Override
    public BoundingBox getBoundingBox() {
        return mapView.map().getBoundingBox(0);
    }

    @Override
    public void zoomToBounds(final Viewport bounds) {
        zoomToBounds(new BoundingBox(bounds.bottomLeft.getLatitudeE6(), bounds.bottomLeft.getLongitudeE6(), bounds.topRight.getLatitudeE6(), bounds.topRight.getLongitudeE6()));
    }

    public void zoomToBounds(final BoundingBox bounds) {
        if (bounds.getLatitudeSpan() == 0 && bounds.getLongitudeSpan() == 0) {
            mapView.map().animator().animateTo(new GeoPoint(bounds.getMaxLatitude(), bounds.getMaxLongitude()));
        } else {
            // add some margin to not cut-off items at the edge
            // Google Maps does this implicitly, so we need to add it here map-specific
            final BoundingBox extendedBounds = bounds.extendMargin(1.1f);
            if (mapView.map().getWidth() == 0 || mapView.map().getHeight() == 0) {
                //See Bug #14948: w/o map width/height the bounds can't be calculated
                // -> postpone animation to later on UI thread (where map width/height will be set)
                mapView.map().post(() -> zoomToBoundsDirect(extendedBounds));
            } else {
                zoomToBoundsDirect(extendedBounds);
            }
        }
    }

    private void zoomToBoundsDirect(final BoundingBox bounds) {
        final MapPosition mp = mapView.map().getMapPosition();
        mp.setByBoundingBox(bounds, mapView.getWidth(), mapView.getHeight());
        mapView.map().setMapPosition(mp);
    }

    @Override
    public int getCurrentZoom() {
        return mapView.map().getMapPosition().getZoomLevel();
    }

    @Override
    public void setZoom(final int zoomLevel) {
        final MapPosition pos = mapView.map().getMapPosition();
        pos.setZoomLevel(zoomLevel);
        mapView.map().setMapPosition(pos);
    }

    @Override
    public void zoomInOut(final boolean zoomIn) {
        mapView.map().animator().animateZoom(300, zoomIn ? 2 : 0.5, 0f, 0f);
    }

    @Override
    public float getCurrentBearing() {
        return AngleUtils.normalize(360 - mapView.map().getMapPosition().bearing); // VTM uses opposite way of calculating bearing compared to GM
    }

    @Override
    public void setBearing(final float bearing) {
        final float adjustedBearing = AngleUtils.normalize(360 - bearing); // VTM uses opposite way of calculating bearing compared to GM
        final MapPosition pos = mapView.map().getMapPosition();
        pos.setBearing(adjustedBearing);
        final float bearingDelta = Math.abs(AngleUtils.difference(360 - pos.bearing, bearing));
        if (bearingDelta > 10.f) {
            mapView.map().animator().animateTo(5000, pos, Easing.Type.QUAD_INOUT, ANIM_ROTATE);
        } else {
            mapView.map().setMapPosition(pos);
        }
    }

}
