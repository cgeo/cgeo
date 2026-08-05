package cgeo.geocaching.unifiedmap.layers.mbtiles;

/* based upon work of (c) 2023 Christian Pesch, https://github.com/cpesch */

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapsforge.core.model.BoundingBox;

public class MBTilesFile {

    protected final SQLiteDatabase database;

    private Map<String, String> metadata = null;

    private static final String SELECT_METADATA = "SELECT name, value FROM metadata";
    private static final String SELECT_TILES = "SELECT tile_data FROM tiles WHERE zoom_level=%s AND tile_column=%s AND tile_row=%s ORDER BY zoom_level DESC LIMIT 1";
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("png", "jpg", "jpeg");

    public MBTilesFile(final File file) {
        this.database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY);

        final String format = getFormat();
        if (format == null) {
            throw new IllegalArgumentException("'metadata.format' field was not found. Is this an MBTiles database?");
        }
        if (!SUPPORTED_FORMATS.contains(format)) {
            throw new IllegalArgumentException(String.format("Unsupported 'metadata.format: %s'. Supported format(s) are: %s", format, SUPPORTED_FORMATS));
        }
    }

    public void close() {
        try {
            metadata = null;
            this.database.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public BoundingBox getBoundingBox() {
        final String bounds = getMetadata().get("bounds");
        if (bounds == null) {
            return null;
        }
        final String[] split = bounds.split(",");
        if (split.length != 4) {
            return null;
        }
        return new BoundingBox(Double.parseDouble(split[1]), Double.parseDouble(split[0]), Double.parseDouble(split[3]), Double.parseDouble(split[2]));
    }

    private String getFormat() {
        return getMetadata().get("format");
    }

    private Integer getMetadata(final String name) {
        final String value = getMetadata().get(name);
        return value != null ? Integer.parseInt(value) : null;
    }

    private Map<String, String> getMetadata() {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
            try (Cursor cursor = this.database.rawQuery(SELECT_METADATA, null)) {
                while (cursor.moveToNext()) {
                    final String key = cursor.getString(0);
                    final String value = cursor.getString(1);
                    this.metadata.put(key, value);
                }
            }
        }
        return this.metadata;
    }

    private Integer getZoomLevel(final String name, final String function) {
        if (getMetadata(name) == null) {
            final String result = getSingleValue("SELECT " + function + "(zoom_level) AS value FROM tiles");
            if (result != null) {
                getMetadata().put(name, result);
            }
        }
        return getMetadata(name);
    }

    public Integer getZoomLevelMax() {
        return getZoomLevel("maxzoom", "MAX");
    }

    public int getZoomLevelMin() {
        return getZoomLevel("minzoom", "MIN");
    }

    private String getSingleValue(final String query) {
        try (Cursor cursor = database.rawQuery(query, null)) {
            if (cursor.moveToNext()) {
                final int i = cursor.getColumnIndex("value");
                return cursor.getString(i);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Converts a zoom level to a scale factor.
     *
     * @param zoomLevel the zoom level to convert.
     * @return the corresponding scale factor.
     */
    private double zoomLevelToScale(final byte zoomLevel) {
        return 1 << zoomLevel;
    }

    /**
     * Converts a tile Y number at a certain zoom level to TMS notation.
     *
     * @param tileY     the tile Y number that should be converted.
     * @param zoomLevel the zoom level at which the number should be converted.
     * @return the TMS value of the tile Y number.
     */
    private long tileYToTMS(final long tileY, final byte zoomLevel) {
        return (long) (zoomLevelToScale(zoomLevel) - tileY - 1);
    }

    public InputStream getTileAsBytes(final int tileX, final int tileY, final byte zoomLevel) {
        final long tmsTileY = tileYToTMS(tileY, zoomLevel);
        try (Cursor cursor = database.rawQuery(String.format(SELECT_TILES, zoomLevel, tileX, tmsTileY), null)) {
            if (cursor.moveToNext()) {
                final int i = cursor.getColumnIndex("tile_data");
                return i < 0 ? null : new ByteArrayInputStream(cursor.getBlob(i));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

}
