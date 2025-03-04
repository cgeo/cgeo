package cgeo.geocaching.unifiedmap.mapsforge;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.UnifiedMapActivity;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.MapsforgeV6GeoItemLayer;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.view.InputListener;
import org.oscim.core.BoundingBox;

public class MapsforgeFragment extends AbstractMapFragment implements Observer {

    private MapView mMapView;
    private TileCache tileCache;
    private MapsforgeThemeHelper themeHelper;
    private View mapAttribution;
    private boolean doReapplyTheme = false;

    public MapsforgeFragment() {
        super(R.layout.unifiedmap_mapsforge_fragment);
        AndroidGraphicFactory.createInstance(CgeoApplication.getInstance());
    }

    @Override
    public void onViewCreated(final @NonNull View view, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // set some parameters for Mapsforge library
        // Support for multi-threaded map painting
        Parameters.NUMBER_OF_THREADS = Settings.getMapOsmThreads();
        // Use fast parent tile rendering to increase performance when zooming in
        Parameters.PARENT_TILES_RENDERING = Parameters.ParentTilesRendering.SPEED;

        mMapView = requireView().findViewById(R.id.mapViewMapsforge);
        setMapRotation(Settings.getMapRotation());
        mapAttribution = requireView().findViewById(R.id.map_attribution);

        // create tile cache (taking driving mode into account when calculating tile cache size)
        setDrivingMode(true);
        tileCache = AndroidUtil.createTileCache(requireContext(), "mapcache", mMapView.getModel().displayModel.getTileSize(), 2f, mMapView.getModel().frameBufferModel.getOverdrawFactor());
        setDrivingMode(false);

        themeHelper = new MapsforgeThemeHelper(requireActivity());

        if (position != null) {
            setCenter(position);
        }
        setZoom(zoomLevel);

        mMapView.addInputListener(new InputListener() {
            @Override
            public void onMoveEvent() {
                if (Boolean.TRUE.equals(viewModel.followMyLocation.getValue())) {
                    viewModel.followMyLocation.setValue(false);
                }
            }

            @Override
            public void onZoomEvent() {

            }
        });

        if (onMapReadyTasks != null) {
            onMapReadyTasks.run();
        }
    }

    @Override
    public void onChange() {
        repaintRotationIndicator(getCurrentBearing());
        ((UnifiedMapActivity) requireActivity()).notifyZoomLevel(getCurrentZoom());
    }

    private void startMap() {
        mMapView.getMapScaleBar().setVisible(true);
        mMapView.getMapScaleBar().setDistanceUnitAdapter(mMapView.getMapScaleBar().getDistanceUnitAdapter());
        mMapView.setBuiltInZoomControls(false);
        mMapView.setZoomLevelMax((byte) currentTileProvider.getZoomMax());

        //make room for map attribution icon button
        final int mapAttPx = Math.round(this.getResources().getDisplayMetrics().density * 30);
        mMapView.getMapScaleBar().setMarginHorizontal(mapAttPx);

        if (this.mapAttribution != null) {
            this.mapAttribution.setOnClickListener(v -> displayMapAttribution());
        }
    }

    private void displayMapAttribution() {
        final Pair<String, Boolean> mapAttribution = currentTileProvider.getMapAttribution();
        if (mapAttribution == null || StringUtils.isBlank(mapAttribution.first)) {
            return;
        }

        //create text message
        CharSequence message = HtmlCompat.fromHtml(mapAttribution.first, HtmlCompat.FROM_HTML_MODE_LEGACY);
        if (mapAttribution.second) {
            final SpannableString s = new SpannableString(message);
            ViewUtils.safeAddLinks(s, Linkify.ALL);
            message = s;
        }

        final AlertDialog alertDialog = Dialogs.newBuilder(getContext())
                .setTitle(requireContext().getString(R.string.map_source_attribution_dialog_title))
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, pos) -> dialog.dismiss())
                .create();
        alertDialog.show();

        // Make the URLs in TextView clickable. Must be called after show()
        // Note: we do NOT use the "setView()" option of AlertDialog because this screws up the layout
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }


    // ========================================================================
    // lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        initLayers();
        if (doReapplyTheme) {
            applyTheme(); // @todo: There must be a less resource-intensive way of applying style-changes...
            doReapplyTheme = false;
        }
        mMapView.getLayerManager().getLayers().add(new MapEventsReceiver());
    }

    @Override
    public void onResume() {
        super.onResume();
        // mMapView.onResume();
        mMapView.getModel().mapViewPosition.addObserver(this);
    }

    @Override
    public void onPause() {
        // mMapView.onPause();
        super.onPause();

        mMapView.getModel().mapViewPosition.removeObserver(this);
    }

    @Override
    public void onDestroyView() {
//        themeHelper.disposeTheme();
        mMapView.destroyAll();
        super.onDestroyView();
    }


    // ========================================================================
    // tilesource handling

    public TileCache getTileCache() {
        return tileCache;
    }

    @Override
    public boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource instanceof AbstractMapsforgeTileProvider;
    }

    @Override
    public void prepareForTileSourceChange() {
        ((AbstractMapsforgeTileProvider) currentTileProvider).prepareForTileSourceChange(mMapView);
        super.prepareForTileSourceChange();
    }

    @Override
    public boolean setTileSource(final AbstractTileProvider newSource, final boolean force) {
        final boolean needsUpdate = super.setTileSource(newSource, force);
        if (needsUpdate) {
            ((AbstractMapsforgeTileProvider) currentTileProvider).addTileLayer(this, mMapView);
            startMap();
        }
        return needsUpdate;
    }


    // ========================================================================
    // layer handling

    @Override
    public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return new MapsforgeV6GeoItemLayer(mMapView.getLayerManager(), mMapView.getMapViewProjection());
    }


    // ========================================================================
    // position related methods

    @Override
    public void setCenter(final Geopoint geopoint) {
        mMapView.setCenter(new LatLong(geopoint.getLatitude(), geopoint.getLongitude()));
    }

    @Override
    public Geopoint getCenter() {
        final LatLong pos = mMapView.getModel().mapViewPosition.getMapPosition().latLong;
        return new Geopoint(pos.getLatitude(), pos.getLongitude());
    }

    @Override
    public Viewport getViewport() {
        if (mMapView == null) {
            return null;
        }
        final Model model = mMapView.getModel();
        if (model == null) {
            return null;
        }
        final LatLong center = model.mapViewPosition.getCenter();
        return new Viewport(new Geopoint(center.latitude, center.longitude), getLatitudeSpan(), getLongitudeSpan());
    }

    private double getLatitudeSpan() {
        double span = 0;
        final long mapSize = MercatorProjection.getMapSize(mMapView.getModel().mapViewPosition.getZoomLevel(), mMapView.getModel().displayModel.getTileSize());
        final Point center = MercatorProjection.getPixelAbsolute(mMapView.getModel().mapViewPosition.getCenter(), mapSize);

        if (mMapView.getHeight() > 0) {
            try {
                final LatLong low = mercatorFromPixels(center.x, center.y - mMapView.getHeight() / 2.0, mapSize);
                final LatLong high = mercatorFromPixels(center.x, center.y + mMapView.getHeight() / 2.0, mapSize);
                span = Math.abs(high.latitude - low.latitude);
            } catch (final IllegalArgumentException ex) {
                //should never happen due to handling in "mercatorFromPixels", but leave it here just in case
                Log.w("Exception when calculating longitude span (center:" + center + ", h/w:" + mMapView.getDimension(), ex);
            }
        }
        return span;
    }

    private double getLongitudeSpan() {
        double span = 0;
        final long mapSize = MercatorProjection.getMapSize(mMapView.getModel().mapViewPosition.getZoomLevel(), mMapView.getModel().displayModel.getTileSize());
        final Point center = MercatorProjection.getPixelAbsolute(mMapView.getModel().mapViewPosition.getCenter(), mapSize);

        if (mMapView.getWidth() > 0) {
            try {
                final LatLong low = mercatorFromPixels(center.x - mMapView.getWidth() / 2.0, center.y, mapSize);
                final LatLong high = mercatorFromPixels(center.x + mMapView.getWidth() / 2.0, center.y, mapSize);
                span = Math.abs(high.longitude - low.longitude);
            } catch (final IllegalArgumentException ex) {
                //should never happen due to handling in "mercatorFromPixels", but leave it here just in case
                Log.w("Exception when calculating longitude span (center:" + center + ", h/w:" + mMapView.getDimension(), ex);
            }
        }
        return span;
    }

    /**
     * Calculates projection of pixel to coord.
     * For this method to operate normally, it should 0 <= pixelX <= maxSize and 0 <= pixelY <= mapSize
     * <br>
     * If either pixelX or pixelY is OUT of these bounds, it is assumed that the map displays the WHOLE WORLD
     * (and this displayed whole world map is smaller than the device's display size of the map.)
     * In these cases, lat/lon is projected to the world-border-coordinates (for lat: -85° - 85°, for lon: -180° - 180°)
     */
    private static LatLong mercatorFromPixels(final double pixelX, final double pixelY, final long mapSize) {
        final double normedPixelX = toBounds(pixelX, 0, mapSize);
        final double normedPixelY = toBounds(pixelY, 0, mapSize);
        final LatLong ll = MercatorProjection.fromPixels(normedPixelX, normedPixelY, mapSize);
        final double lon = toBounds(ll.longitude, -180, 180);
        final double lat = toBounds(ll.latitude, -85, 85);
        return new LatLong(lat, lon);
    }

    private static double toBounds(final double value, final double min, final double max) {
        return value < min ? min : Math.min(value, max);
    }

    @Override
    public void setDrivingMode(final boolean enabled) {
        mMapView.setMapViewCenterY(enabled ? 0.75f : 0.5f);
        mMapView.getModel().frameBufferModel.setOverdrawFactor(Math.max(mMapView.getModel().frameBufferModel.getOverdrawFactor(), mMapView.getMapViewCenterY() * 2));
    }


    // ========================================================================
    // zoom, bearing & heading methods

    @Override
    public void zoomToBounds(final Viewport bounds) {
        zoomToBounds(new BoundingBox(bounds.bottomLeft.getLatitudeE6(), bounds.bottomLeft.getLongitudeE6(), bounds.topRight.getLatitudeE6(), bounds.topRight.getLongitudeE6()));
    }

    public void zoomToBounds(final BoundingBox bounds) {
        if (bounds.getLatitudeSpan() == 0 && bounds.getLongitudeSpan() == 0) {
            setCenter(new Geopoint(bounds.getCenterPoint().getLatitude(), bounds.getCenterPoint().getLongitude()));
        } else {
            // add some margin to not cut-off items at the edge
            // Google Maps does this implicitly, so we need to add it here map-specific
            final BoundingBox extendedBounds = bounds.extendMargin(1.1f);
            if (mMapView.getWidth() == 0 || mMapView.getHeight() == 0) {
                //See Bug #14948: w/o map width/height the bounds can't be calculated
                // -> postpone animation to later on UI thread (where map width/height will be set)
                mMapView.post(() -> zoomToBoundsDirect(extendedBounds));
            } else {
                zoomToBoundsDirect(extendedBounds);
            }
        }
    }

    private void zoomToBoundsDirect(final BoundingBox bounds) {
        setCenter(new Geopoint(bounds.getCenterPoint().getLatitude(), bounds.getCenterPoint().getLongitude()));
        final int tileSize = mMapView.getModel().displayModel.getTileSize();
        final byte newZoom = LatLongUtils.zoomForBounds(new Dimension(mMapView.getWidth(), mMapView.getHeight()),
                new org.mapsforge.core.model.BoundingBox(bounds.getMinLatitude(), bounds.getMinLongitude(), bounds.getMaxLatitude(), bounds.getMaxLongitude()), tileSize);
        setZoom(newZoom);
    }

    @Override
    public int getCurrentZoom() {
        return mMapView.getModel().mapViewPosition.getZoomLevel();
    }

    @Override
    public void setZoom(final int zoomLevel) {
        mMapView.setZoomLevel((byte) Math.max(Math.min(zoomLevel, getZoomMax()), getZoomMin()));
    }

    @Override
    public void zoomInOut(final boolean zoomIn) {
        if (zoomIn) {
            mMapView.getModel().mapViewPosition.zoomIn();
        } else {
            mMapView.getModel().mapViewPosition.zoomOut();
        }
    }

    @Override
    public void setMapRotation(final int mapRotation) {
        super.setMapRotation(mapRotation);
        mMapView.getTouchGestureHandler().setRotationEnabled(mapRotation == Settings.MAPROTATION_MANUAL);
    }

    @Override
    public float getCurrentBearing() {
        return AngleUtils.normalize(360 - mMapView.getMapRotation().degrees); // Mapsforge uses opposite way of calculating bearing compared to GM
    }

    @Override
    public void setBearing(final float bearing) {
        final float adjustedBearing = AngleUtils.normalize(360 - bearing); // Mapsforge uses opposite way of calculating bearing compared to GM
        mMapView.rotate(new Rotation(adjustedBearing, mMapView.getWidth() * 0.5f, mMapView.getHeight() * 0.5f));
        mMapView.getLayerManager().redrawLayers();
    }


    // ========================================================================
    // theme & language related methods

    @Override
    public void selectTheme(final Activity activity) {
        themeHelper.selectMapTheme(((AbstractMapsforgeTileProvider) currentTileProvider).getTileLayer(), tileCache);
    }

    @Override
    public void selectThemeOptions(final Activity activity) {
        themeHelper.selectMapThemeOptions();
        doReapplyTheme = true;
    }

    @Override
    public void applyTheme() {
        themeHelper.reapplyMapTheme(((AbstractMapsforgeTileProvider) currentTileProvider).getTileLayer(), tileCache);
    }

    @Override
    public void setPreferredLanguage(final String language) {
        currentTileProvider.setPreferredLanguage(language);
    }


    // ========================================================================
    // additional menu entries


    // ========================================================================
    // Tap handling methods

    class MapEventsReceiver extends Layer {

        @Override
        public boolean onLongPress(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
            onTapCallback(tapLatLong.getLatitudeE6(), tapLatLong.getLongitudeE6(), (int) tapXY.x, (int) tapXY.y, true);
            return true;
        }

        @Override
        public boolean onTap(final LatLong tapLatLong, final Point layerXY, final Point tapXY) {
            onTapCallback(tapLatLong.getLatitudeE6(), tapLatLong.getLongitudeE6(), (int) tapXY.x, (int) tapXY.y, false);
            return true;
        }

        @Override
        public void draw(final org.mapsforge.core.model.BoundingBox boundingBox, final byte zoomLevel, final Canvas canvas, final Point topLeftPoint, final Rotation rotation) {
            // nothing to do
        }
    }

}
