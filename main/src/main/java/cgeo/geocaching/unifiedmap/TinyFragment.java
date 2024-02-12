package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractNavigationBarMapActivity;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.mapsforgevtm.VtmThemes;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Log;

import android.net.Uri;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

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

public class TinyFragment implements XmlRenderThemeMenuCallback {

    private final MapView mapView;
    private IRenderTheme theme;
    private final AbstractNavigationBarMapActivity activity;

    public TinyFragment(final AbstractNavigationBarMapActivity activity, final MapView mapView, final IRenderTheme theme) {
        this.activity = activity;
        this.mapView = mapView;
        this.theme = theme;
//            super(R.layout.fragment_map);
    }

    public void loadMap(final Uri mapUri) {
        try {
            // Tile source
            final MapFileTileSource tileSource = new MapFileTileSource();
            final FileInputStream fis = (FileInputStream) activity.getContentResolver().openInputStream(mapUri);
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

    public void loadTheme(final Uri themeUri) {
        try {
            if (theme != null) {
                theme.dispose();
            }

            // Render theme (themeUri may be null, using default theme then)
            if (themeUri != null) {
                final List<String> xmlThemes = ZipXmlThemeResourceProvider.scanXmlThemes(new ZipInputStream(new BufferedInputStream(activity.getContentResolver().openInputStream(themeUri))));
                if (xmlThemes.isEmpty()) {
                    return;
                }
                final ThemeFile themeFile = new ZipRenderTheme(xmlThemes.get(0), new ZipXmlThemeResourceProvider(new ZipInputStream(new BufferedInputStream(activity.getContentResolver().openInputStream(themeUri)))));
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

    public boolean supportsTileSource(final AbstractTileProvider newSource) {
        return true;
    }

    public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return null;
    }

    public void setCenter(final Geopoint geopoint) {
        final MapPosition pos = mapView.map().getMapPosition();
        pos.setPosition(geopoint.getLatitude(), geopoint.getLongitude());
        mapView.map().setMapPosition(pos);
    }

    public Geopoint getCenter() {
        final MapPosition pos = mapView.map().getMapPosition();
        return new Geopoint(pos.getLatitude(), pos.getLongitude());
    }

    public BoundingBox getBoundingBox() {
        return mapView.map().getBoundingBox(0);
    }

    public Viewport getViewport() {
        final BoundingBox bb = getBoundingBox();
        return new Viewport(new Geopoint(bb.getMinLatitude(), bb.getMinLongitude()), new Geopoint(bb.getMaxLatitude(), bb.getMaxLongitude()));
    }

    public void setDrivingMode(final boolean enabled) {
        mapView.map().viewport().setMapViewCenter(0f, enabled ? 0.5f : 0f);
    }

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

    public int getCurrentZoom() {
        return mapView.map().getMapPosition().getZoomLevel();
    }

    public void setZoom(final int zoomLevel) {
        final MapPosition pos = mapView.map().getMapPosition();
        pos.setZoomLevel(zoomLevel);
        mapView.map().setMapPosition(pos);
    }

    public void zoomInOut(final boolean zoomIn) {
        mapView.map().animator().animateZoom(300, zoomIn ? 2 : 0.5, 0f, 0f);
    }

    public void setMapRotation(final int mapRotation) {
//        super.setMapRotation(mapRotation);
        if (mapRotation == Settings.MAPROTATION_OFF) {
            setBearing(0);
        }

        mapView.map().getEventLayer().enableRotation(mapRotation == Settings.MAPROTATION_MANUAL);
//        repaintRotationIndicator(mapView.map().getMapPosition().bearing);
    }

    public float getCurrentBearing() {
        return AngleUtils.normalize(360 - mapView.map().getMapPosition().bearing); // VTM uses opposite way of calculating bearing compared to GM
    }

    public void setBearing(final float bearing) {
        final float adjustedBearing = AngleUtils.normalize(360 - bearing); // VTM uses opposite way of calculating bearing compared to GM
        final MapPosition pos = mapView.map().getMapPosition();
        pos.setBearing(adjustedBearing);
        final float bearingDelta = Math.abs(AngleUtils.difference(360 - pos.bearing, bearing));
        if (bearingDelta
                > 10.f) {
            mapView.map().animator().animateTo(5000, pos, Easing.Type.QUAD_INOUT, ANIM_ROTATE);
        } else {
            mapView.map().setMapPosition(pos);
        }
    }

    protected void adaptLayoutForActionbar(final boolean actionBarShowing) {
        final View compass = activity.findViewById(R.id.bearingIndicator);
        compass.animate().translationY((actionBarShowing ? activity.findViewById(R.id.actionBarSpacer).getHeight() : 0) + ViewUtils.dpToPixel(25)).start();
    }
}

