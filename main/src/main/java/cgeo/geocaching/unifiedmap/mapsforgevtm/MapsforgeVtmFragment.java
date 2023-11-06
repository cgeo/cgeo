package cgeo.geocaching.unifiedmap.mapsforgevtm;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.UnifiedMapActivity;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.MapsforgeVtmGeoItemLayer;
import cgeo.geocaching.unifiedmap.mapsforgevtm.legend.RenderThemeLegend;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeTileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.GroupedList;
import cgeo.geocaching.utils.ImageUtils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.oscim.android.MapView;
import org.oscim.backend.CanvasAdapter;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.ImperialUnitAdapter;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.tiling.TileSource;

public class MapsforgeVtmFragment extends AbstractMapFragment {
    private Map mMap;
    private GroupedList<Layer> mMapLayers;
    protected TileLayer baseMap;
    protected final ArrayList<Layer> layers = new ArrayList<>();
    protected MapsforgeThemeHelper themeHelper;
    protected Map.UpdateListener mapUpdateListener;
    private View mapAttribution;

    private final Bitmap rotationIndicator = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.bearing_indicator, null));
    private final int rotationWidth = rotationIndicator.getWidth();
    private final int rotationHeight = rotationIndicator.getHeight();


    public MapsforgeVtmFragment() {
        super(R.layout.unifiedmap_mapsforgevtm_fragment);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final MapView mMapView = requireView().findViewById(R.id.mapViewVTM);
        mMap = mMapView.map();
        mMapLayers = new GroupedList<>(mMap.layers(), 4);
        setMapRotation(Settings.getMapRotation());
        requireView().findViewById(R.id.map_zoomin).setOnClickListener(v -> zoomInOut(true));
        requireView().findViewById(R.id.map_zoomout).setOnClickListener(v -> zoomInOut(false));
        mapAttribution = requireView().findViewById(R.id.map_attribution);
        themeHelper = new MapsforgeThemeHelper(requireActivity());
        if (position != null) {
            setCenter(position);
        }
        setZoom(zoomLevel);
        mapUpdateListener = (event, mapPosition) -> {
            if (event == Map.ROTATE_EVENT) {
                repaintRotationIndicator(mapPosition.bearing);
            }
            if (event == Map.MOVE_EVENT) {
                if (Boolean.TRUE.equals(viewModel.followMyLocation.getValue())) {
                    viewModel.followMyLocation.setValue(false);
                }
                viewModel.mapCenter.setValue(new Geopoint(mapPosition.getLatitude(), mapPosition.getLongitude()));
            }
//            if (event == Map.POSITION_EVENT || event == Map.ROTATE_EVENT) {
//                activityMapChangeListener.call(new UnifiedMapPosition(mapPosition.getLatitude(), mapPosition.getLongitude(), mapPosition.zoomLevel, mapPosition.bearing));
//            }
        };
        mMap.events.bind(mapUpdateListener);

        if (onMapReadyTasks != null) {
            onMapReadyTasks.run();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        initLayers();
        applyTheme(); // @todo: There must be a less resource-intensive way of applying style-changes...
//        mMapView.onResume(); needed? probably not, as the view receives the normal lifecycle
        mMapLayers.add(new MapsforgeVtmFragment.MapEventsReceiver(mMap));
    }

    @Override
    public void onDestroy() {
//        mMapView.onDestroy(); needed? probably not, as the view receives the normal lifecycle
        themeHelper.disposeTheme();
        super.onDestroy();
    }

    @Override
    public boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource instanceof AbstractMapsforgeTileProvider;
    }

    @Override
    public void setTileSource(final AbstractTileProvider newSource) {
        super.setTileSource(newSource);
        ((AbstractMapsforgeTileProvider) currentTileProvider).addTileLayer(this, mMap);
        startMap();
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
        renderer.setOffset(30 * CanvasAdapter.getScale(), 0); // make room for attribution
        addLayer(LayerHelper.ZINDEX_SCALEBAR, mapScaleBarLayer);

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
                .setTitle(getContext().getString(R.string.map_source_attribution_dialog_title))
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, pos) -> dialog.dismiss())
                .create();
        alertDialog.show();

        // Make the URLs in TextView clickable. Must be called after show()
        // Note: we do NOT use the "setView()" option of AlertDialog because this screws up the layout
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    protected void repaintRotationIndicator(final float bearing) {
        final ImageView compassrose = requireView().findViewById(R.id.bearingIndicator);
        if (bearing == 0.0f) {
            compassrose.setImageBitmap(null);
        } else {
            final ActionBar actionBar = ((UnifiedMapActivity) requireActivity()).getSupportActionBar();
            final boolean actionBarIsShowing = actionBar != null && actionBar.isShowing();
            adaptLayoutForActionbar(actionBarIsShowing);

            final Matrix matrix = new Matrix();
            matrix.setRotate(bearing, rotationWidth / 2.0f, rotationHeight / 2.0f);
            compassrose.setImageBitmap(Bitmap.createBitmap(rotationIndicator, 0, 0, rotationWidth, rotationHeight, matrix, true));
            compassrose.setOnClickListener(v -> {
                setBearing(0.0f);
                repaintRotationIndicator(0.0f);
            });
        }

    }

    @Override
    public void prepareForTileSourceChange() {
        // remove layers from currently displayed Mapsforge map
        removeBaseMap();
        synchronized (layers) {
            for (Layer layer : layers) {
                layer.setEnabled(false);
                try {
                    mMapLayers.remove(layer);
                } catch (IndexOutOfBoundsException ignore) {
                    // ignored
                }
            }
            layers.clear();
        }
        mMap.clearMap();
        super.prepareForTileSourceChange();
    }

    @Override
    public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return new MapsforgeVtmGeoItemLayer(mMap, mMapLayers);
    }

    @Override
    public void setCenter(final Geopoint geopoint) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setPosition(geopoint.getLatitude(), geopoint.getLongitude());
        mMap.setMapPosition(pos);
    }

    @Override
    public Geopoint getCenter() {
        final MapPosition pos = mMap.getMapPosition();
        return new Geopoint(pos.getLatitude(), pos.getLongitude());
    }

    @Override
    public BoundingBox getBoundingBox() {
        return mMap.getBoundingBox(0);
    }

    @Override
    public void zoomToBounds(final Viewport bounds) {
        zoomToBounds(new BoundingBox(bounds.bottomLeft.getLatitudeE6(), bounds.bottomLeft.getLongitudeE6(), bounds.topRight.getLatitudeE6(), bounds.topRight.getLongitudeE6()));
    }

    public void zoomToBounds(final BoundingBox bounds) {
        mMap.animator().animateTo(bounds);
    }

    @Override
    public int getCurrentZoom() {
        return mMap.getMapPosition().getZoomLevel();
    }

    @Override
    public void setZoom(final int zoomLevel) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setZoomLevel(zoomLevel);
        mMap.setMapPosition(pos);
    }

    private void zoomInOut(final boolean zoomIn) {
        mMap.animator().animateZoom(300, zoomIn ? 2 : 0.5, 0f, 0f);
    }

    @Override
    public void setMapRotation(final int mapRotation) {
        mMap.getEventLayer().enableRotation(mapRotation != Settings.MAPROTATION_OFF);
        super.setMapRotation(mapRotation);
    }

    @Override
    public float getCurrentBearing() {
        return mMap.getMapPosition().bearing;
    }

    @Override
    public void setBearing(final float bearing) {
        final MapPosition pos = mMap.getMapPosition();
        pos.setBearing(bearing);
        mMap.setMapPosition(pos);
    }

    /**
     * call this instead of VTM.setBaseMap so that we can keep track of baseMap set by tile provider
     */
    public synchronized TileLayer setBaseMap(final TileSource tileSource) {
        removeBaseMap();

        final VectorTileLayer l = new OsmTileLayer(mMap);
        l.setTileSource(tileSource);
        addLayerToGroup(l, LayerHelper.ZINDEX_BASEMAP);
        baseMap = l;
        return baseMap;
    }

    private void removeBaseMap() {
        if (baseMap != null) {
            mMapLayers.removeGroup(LayerHelper.ZINDEX_BASEMAP);
        }
        baseMap = null;
    }

    /**
     * call this instead of VTM.layers().add so that we can keep track of layers added by the tile provider
     */
    public synchronized void addLayer(final int index, final Layer layer) {
        layers.add(layer);
        mMapLayers.addToGroup(layer, index);
    }

    public synchronized void addLayerToGroup(final Layer layer, final int groupIndex) {
        mMapLayers.addToGroup(layer, groupIndex);
    }

    public synchronized void clearGroup(final int groupIndex) {
        mMapLayers.removeGroup(groupIndex);
    }

    // ========================================================================
    // theme & language related methods

    @Override
    public void selectTheme(final Activity activity) {
        themeHelper.selectMapTheme(activity, mMap, currentTileProvider);
    }

    @Override
    public void selectThemeOptions(final Activity activity) {
        themeHelper.selectMapThemeOptions(activity, currentTileProvider);
    }

    @Override
    public void applyTheme() {
        themeHelper.reapplyMapTheme(mMap, currentTileProvider);
    }

    @Override
    public void setPreferredLanguage(final String language) {
        currentTileProvider.setPreferredLanguage(language);
        mMap.clearMap();
    }

    // ========================================================================
    // additional menu entries

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.menu_theme_legend) {
            RenderThemeLegend.showLegend(requireActivity(), themeHelper);
            return true;
        }
        return false;
    }



//    // ========================================================================
//    // Tap handling methods
//
    class MapEventsReceiver extends Layer implements GestureListener {

        MapEventsReceiver(final Map map) {
            super(map);
        }

        @Override
        public boolean onGesture(final Gesture g, final MotionEvent e) {
            if (g instanceof Gesture.Tap) {
                final GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                onTapCallback(p.latitudeE6, p.longitudeE6, (int) e.getX(), (int) e.getY(), false);
                return true;
            } else if (g instanceof Gesture.LongPress) {
                final GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
                onTapCallback(p.latitudeE6, p.longitudeE6, (int) e.getX(), (int) e.getY(), true);
                return true;
            }
            return false;
        }
    }

    @Override
    protected void adaptLayoutForActionbar(final boolean actionBarShowing) {
        final View compass = requireView().findViewById(R.id.bearingIndicator);
        compass.animate().translationY((actionBarShowing ? requireActivity().findViewById(R.id.actionBarSpacer).getHeight() : 0) + ViewUtils.dpToPixel(25)).start();
    }
}
