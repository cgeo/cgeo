package cgeo.geocaching.maps.mapsforge;

import cgeo.geocaching.Settings;

import org.mapsforge.android.maps.mapgenerator.tiledownloader.TileDownloader;
import org.mapsforge.core.Tile;

import android.util.Log;

public class EniroTileDownloader extends TileDownloader {

    public enum EniroType {
        map(".png", (byte) 17),
        aerial(".jpeg", (byte) 20);

        public String extension;
        public byte maxZoomLevel;

        private EniroType(String ext, byte maxZoom) {
            extension = ext;
            maxZoomLevel = maxZoom;
        }
    }

    // http://map.eniro.com/geowebcache/service/tms1.0.0/map/[z]/[x]/[y].png

    final private StringBuilder stringBuilder;
    private EniroType _type;

    public EniroTileDownloader(final EniroType type) {
        super();
        stringBuilder = new StringBuilder(150);
        _type = type;
    }

    public void setType(EniroType newType) {
        _type = newType;
    }

    @Override
    public byte getZoomLevelMax() {
        return _type.maxZoomLevel;
    }

    @Override
    public String getHostName() {
        return "map.eniro.com";
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public String getTilePath(Tile arg0) {
        stringBuilder.setLength(0);
        stringBuilder.append("/geowebcache/service/tms1.0.0/");
        stringBuilder.append(_type.name());
        stringBuilder.append("/");
        stringBuilder.append(arg0.zoomLevel);
        stringBuilder.append("/");
        stringBuilder.append(arg0.tileX);
        stringBuilder.append("/");
        stringBuilder.append((int) (Math.pow(2, arg0.zoomLevel)) - 1 - arg0.tileY);
        stringBuilder.append(_type.extension);

        Log.d(Settings.tag, "Tile path: " + stringBuilder.toString());

        return stringBuilder.toString();
    }

}
