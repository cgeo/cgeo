package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.storage.ContentStorage;
import static cgeo.geocaching.unifiedmap.tileproviders.TileProviderFactory.MAP_MAPSFORGE;

import android.net.Uri;

import java.io.FileInputStream;

import org.apache.commons.lang3.StringUtils;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MapInfo;

class AbstractMapsforgeOfflineTileProvider extends AbstractMapsforgeTileProvider {

    AbstractMapsforgeOfflineTileProvider(final String name, final Uri uri, final int zoomMin, final int zoomMax) {
        super(name, uri, zoomMin, zoomMax);
        supportsThemes = true;
    }

    @Override
    public void addTileLayer(final Map map) {
        final MapFileTileSource tileSource = new MapFileTileSource();
        tileSource.setMapFileInputStream((FileInputStream) ContentStorage.get().openForRead(mapUri));
        final VectorTileLayer tileLayer = (VectorTileLayer) MAP_MAPSFORGE.setBaseMap(tileSource);
        MAP_MAPSFORGE.addLayer(new BuildingLayer(map, tileLayer));
        MAP_MAPSFORGE.addLayer(new LabelLayer(map, tileLayer));
        MAP_MAPSFORGE.applyTheme();

        final MapInfo info = tileSource.getMapInfo();
        supportsLanguages = StringUtils.isNotBlank(info.languagesPreference);
        if (!info.boundingBox.contains(map.getMapPosition().getGeoPoint())) {
            MAP_MAPSFORGE.zoomToBounds(info.boundingBox);
        }
    }

}
