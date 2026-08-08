package cgeo.geocaching.pebble;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.files.GPXTrackOrRouteImporter;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Route;
import cgeo.geocaching.models.RouteSegment;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;
import cgeo.geocaching.storage.extension.Trackfiles;
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeThemeHelper;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapLineUtils;

import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileException;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes;
import org.mapsforge.map.rendertheme.rule.RenderThemeFuture;

/**
 * Renders an offline Mapsforge map tile around the current location and converts
 * it to a Pebble 8-bit color frame.
 */
public class MapsforgePebbleRenderer implements PebbleMapService.PebbleMapRenderer, XmlRenderThemeMenuCallback {

    private static final String LOG = "MapsforgePebbleRenderer";
    private static final int TILE_SIZE = 256;
    private static final int TILES_SIDE = 3;
    private static final int BUFFER_SIZE = TILES_SIDE * TILE_SIZE;

    private final Context context;
    private final MapDataStore mapDataStore;
    private final DisplayModel displayModel;
    private RenderThemeFuture renderThemeFuture;
    private String currentThemeId;
    private final TileCache tileCache;
    private List<RouteSegment[]> trackSegments = new ArrayList<>();
    private List<Integer> trackColors = new ArrayList<>();
    private List<Integer> trackWidths = new ArrayList<>();
    private String trackStateKey = "";
    private final DatabaseRenderer databaseRenderer;
    private final boolean hasMap;

    public MapsforgePebbleRenderer(final Context context, final List<Uri> mapUris) {
        this.context = context.getApplicationContext();
        if (AndroidGraphicFactory.INSTANCE == null) {
            AndroidGraphicFactory.createInstance(this.context);
        }

        this.displayModel = new DisplayModel();
        this.displayModel.setFixedTileSize(TILE_SIZE);
        this.tileCache = AndroidUtil.createTileCache(this.context, "pebble", TILE_SIZE, 1f, 1.25);
        loadTheme();

        final MapDataStore tempMap = openMapFiles(mapUris);
        this.mapDataStore = tempMap;
        this.hasMap = tempMap != null;
        if (hasMap) {
            this.databaseRenderer = new DatabaseRenderer(mapDataStore, AndroidGraphicFactory.INSTANCE, tileCache, null, false, false, null);
        } else {
            this.databaseRenderer = null;
        }
    }

    private void loadTheme() {
        final String selectedTheme = Settings.getSelectedMapRenderTheme();
        if (selectedTheme == null ? currentThemeId == null : selectedTheme.equals(currentThemeId)) {
            return;
        }
        currentThemeId = selectedTheme;
        final XmlRenderTheme xmlRenderTheme = MapsforgeThemeHelper.getSelectedRenderTheme(this);
        this.renderThemeFuture = new RenderThemeFuture(AndroidGraphicFactory.INSTANCE, xmlRenderTheme != null ? xmlRenderTheme : MapsforgeThemes.OSMARENDER, this.displayModel);
        this.renderThemeFuture.run();
    }

    private MapDataStore openMapFiles(final List<Uri> mapUris) {
        Log.w(LOG + " openMapFiles: " + mapUris);
        if (mapUris == null || mapUris.isEmpty()) {
            return null;
        }
        final MultiMapDataStore multi = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        int opened = 0;
        for (final Uri mapUri : mapUris) {
            final MapDataStore store = openMapFile(mapUri);
            if (store != null) {
                multi.addMapDataStore(store, false, false);
                opened++;
            }
        }
        Log.w(LOG + " openMapFiles opened " + opened + "/" + mapUris.size());
        return opened == 0 ? null : multi;
    }

    private MapDataStore openMapFile(final Uri mapUri) {
        Log.w(LOG + " openMapFile: " + mapUri);
        if (mapUri == null) {
            return null;
        }
        final InputStream is = ContentStorage.get().openForRead(mapUri, true);
        if (!(is instanceof FileInputStream)) {
            Log.w(LOG + " cannot open offline map: " + mapUri);
            return null;
        }
        try {
            final MapFile map = new MapFile((FileInputStream) is, 0, Settings.getMapLanguage());
            Log.w(LOG + " map opened");
            return map;
        } catch (final MapFileException e) {
            Log.w(LOG + " failed to open map file: " + mapUri, e);
            return null;
        }
    }

    @Override
    public byte[] render(final double latitude, final double longitude, final int zoom) {
        Log.w(LOG + " render lat=" + latitude + " lon=" + longitude + " zoom=" + zoom);
        loadTheme();
        if (!hasMap || databaseRenderer == null || renderThemeFuture == null) {
            Log.w(LOG + " render skipped: hasMap=" + hasMap + " renderer=" + databaseRenderer + " theme=" + renderThemeFuture);
            return createBlackFrame();
        }

        final byte z = (byte) zoom;
        final long mapSize = MercatorProjection.getMapSize(z, TILE_SIZE);
        final LatLong center = new LatLong(latitude, longitude);
        final Point centerPixel = MercatorProjection.getPixelAbsolute(center, mapSize);

        int tileX = MercatorProjection.longitudeToTileX(longitude, z);
        int tileY = MercatorProjection.latitudeToTileY(latitude, z);
        final int maxTile = Tile.getMaxTileNumber(z);

        tileX = Math.max(1, Math.min(tileX, maxTile - 1));
        tileY = Math.max(1, Math.min(tileY, maxTile - 1));

        final Tile topLeft = new Tile(tileX - 1, tileY - 1, z, TILE_SIZE);
        final Point topLeftOrigin = topLeft.getOrigin();

        final Bitmap big = Bitmap.createBitmap(BUFFER_SIZE, BUFFER_SIZE, Bitmap.Config.ARGB_8888);
        final Canvas bigCanvas = new Canvas(big);

        final List<TileBitmap> renderedTiles = new ArrayList<>();
        for (int dy = 0; dy < TILES_SIDE; dy++) {
            for (int dx = 0; dx < TILES_SIDE; dx++) {
                final Tile tile = new Tile(tileX - 1 + dx, tileY - 1 + dy, z, TILE_SIZE);
                final RendererJob job = new RendererJob(tile, mapDataStore, renderThemeFuture, displayModel, 1f, false, false);
                final TileBitmap tb;
                try {
                    tb = databaseRenderer.executeJob(job);
                } catch (final Exception e) {
                    Log.w(LOG + " executeJob failed for tile " + tile, e);
                    continue;
                }
                if (tb == null) {
                    continue;
                }
                renderedTiles.add(tb);
                final Bitmap androidBmp = AndroidGraphicFactory.getBitmap(tb);
                if (androidBmp == null) {
                    continue;
                }
                final Point origin = tile.getOrigin();
                bigCanvas.drawBitmap(androidBmp,
                        (float) (origin.x - topLeftOrigin.x),
                        (float) (origin.y - topLeftOrigin.y),
                        null);
            }
        }

        final int left = (int) (centerPixel.x - topLeftOrigin.x - (double) PebbleMapConstants.MAP_WIDTH / 2);
        final int top = (int) (centerPixel.y - topLeftOrigin.y - (double) PebbleMapConstants.MAP_HEIGHT / 2);
        if (left < 0 || top < 0 || left + PebbleMapConstants.MAP_WIDTH > big.getWidth() || top + PebbleMapConstants.MAP_HEIGHT > big.getHeight()) {
            big.recycle();
            return createBlackFrame();
        }

        final Bitmap out = Bitmap.createBitmap(PebbleMapConstants.MAP_WIDTH, PebbleMapConstants.MAP_HEIGHT, Bitmap.Config.ARGB_8888);
        final Canvas outCanvas = new Canvas(out);
        final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        outCanvas.drawBitmap(big,
                new Rect(left, top, left + PebbleMapConstants.MAP_WIDTH, top + PebbleMapConstants.MAP_HEIGHT),
                new Rect(0, 0, PebbleMapConstants.MAP_WIDTH, PebbleMapConstants.MAP_HEIGHT),
                paint);

        final Paint redPaint = new Paint();
        redPaint.setColor(Color.RED);
        redPaint.setStyle(Paint.Style.FILL);
        outCanvas.drawCircle(PebbleMapConstants.MAP_WIDTH / 2f, PebbleMapConstants.MAP_HEIGHT / 2f, 5f, redPaint);

        loadTracks();
        drawTracks(outCanvas, mapSize, centerPixel);

        final byte[] pebble = PebbleMapConverter.toColor8(out, PebbleMapConstants.MAP_WIDTH, PebbleMapConstants.MAP_HEIGHT);
        Log.w(LOG + " rendered " + pebble.length + " bytes");
        out.recycle();
        big.recycle();
        return pebble;
    }

    private byte[] createBlackFrame() {
        return new byte[PebbleMapConstants.MAP_WIDTH * PebbleMapConstants.MAP_HEIGHT];
    }

    private void loadTracks() {
        final List<Trackfiles> trackfiles = Trackfiles.getTrackfiles();
        final StringBuilder newState = new StringBuilder();
        if (trackfiles != null) {
            for (final Trackfiles tf : trackfiles) {
                newState.append(tf.getFilename()).append(':').append(tf.isHidden() ? 1 : 0).append(':').append(tf.getColor()).append(';');
            }
        }
        if (newState.toString().equals(trackStateKey)) {
            return;
        }
        trackStateKey = newState.toString();

        final List<RouteSegment[]> newSegments = new ArrayList<>();
        final List<Integer> newColors = new ArrayList<>();
        final List<Integer> newWidths = new ArrayList<>();
        if (trackfiles != null) {
            for (final Trackfiles tf : trackfiles) {
                if (tf.isHidden()) {
                    continue;
                }
                final IGeoItemSupplier value = GPXTrackOrRouteImporter.doInBackground(context, Trackfiles.getUriFromKey(tf.getFilename()));
                if (value instanceof Route) {
                    newSegments.add(((Route) value).getSegments());
                    newColors.add(tf.getColor());
                    newWidths.add(tf.getWidth());
                }
            }
        }
        trackSegments = newSegments;
        trackColors = newColors;
        trackWidths = newWidths;
    }

    private void drawTracks(final Canvas canvas, final long mapSize, final Point centerPixel) {
        if (trackSegments == null || trackSegments.isEmpty()) {
            return;
        }
        final Paint trackPaint = new Paint();
        trackPaint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < trackSegments.size(); i++) {
            trackPaint.setStrokeWidth(MapLineUtils.getWidthFromRaw(trackWidths.get(i), true));
            trackPaint.setColor(trackColors.get(i));
            for (final RouteSegment segment : trackSegments.get(i)) {
                final ArrayList<Geopoint> points = segment.getPoints();
                if (points == null || points.size() < 2) {
                    continue;
                }
                final LatLong first = new LatLong(points.get(0).getLatitude(), points.get(0).getLongitude());
                final Point firstPixel = MercatorProjection.getPixelAbsolute(first, mapSize);
                float prevX = (float) (firstPixel.x - centerPixel.x + (double) PebbleMapConstants.MAP_WIDTH / 2.0);
                float prevY = (float) (firstPixel.y - centerPixel.y + (double) PebbleMapConstants.MAP_HEIGHT / 2.0);
                for (int p = 1; p < points.size(); p++) {
                    final LatLong latLong = new LatLong(points.get(p).getLatitude(), points.get(p).getLongitude());
                    final Point pixel = MercatorProjection.getPixelAbsolute(latLong, mapSize);
                    final float x = (float) (pixel.x - centerPixel.x + (double) PebbleMapConstants.MAP_WIDTH / 2.0);
                    final float y = (float) (pixel.y - centerPixel.y + (double) PebbleMapConstants.MAP_HEIGHT / 2.0);
                    canvas.drawLine(prevX, prevY, x, y, trackPaint);
                    prevX = x;
                    prevY = y;
                }
            }
        }
    }

    @Override
    public Set<String> getCategories(final XmlRenderThemeStyleMenu menu) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String id = prefs.getString(menu.getId(), menu.getDefaultValue());
        final XmlRenderThemeStyleLayer baseLayer = menu.getLayer(id);
        if (baseLayer == null) {
            Log.w(LOG + " invalid style " + id);
            return null;
        }
        final Set<String> result = baseLayer.getCategories();
        for (final XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
            if (prefs.getBoolean(overlay.getId(), overlay.isEnabled())) {
                result.addAll(overlay.getCategories());
            }
        }
        return result;
    }
}
