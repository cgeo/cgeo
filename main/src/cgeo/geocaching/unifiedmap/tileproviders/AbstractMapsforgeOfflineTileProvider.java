package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.unifiedmap.LayerHelper;
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

    MapFileTileSource tileSource;

    AbstractMapsforgeOfflineTileProvider(final String name, final Uri uri, final int zoomMin, final int zoomMax) {
        super(name, uri, zoomMin, zoomMax);
        supportsThemes = true;
        supportsThemeOptions = true; // rule of thumb, not all themes support options
    }

    @Override
    public void addTileLayer(final Map map) {
        tileSource = new MapFileTileSource();
        tileSource.setPreferredLanguage(Settings.getMapLanguage());
        tileSource.setMapFileInputStream((FileInputStream) ContentStorage.get().openForRead(mapUri));
        final VectorTileLayer tileLayer = (VectorTileLayer) MAP_MAPSFORGE.setBaseMap(tileSource);
        MAP_MAPSFORGE.addLayer(LayerHelper.ZINDEX_BUILDINGS, new BuildingLayer(map, tileLayer));
        MAP_MAPSFORGE.addLayer(LayerHelper.ZINDEX_LABELS, new LabelLayer(map, tileLayer));
        MAP_MAPSFORGE.applyTheme();

        final MapInfo info = tileSource.getMapInfo();
        if (info != null) {
            supportsLanguages = StringUtils.isNotBlank(info.languagesPreference);
            if (supportsLanguages) {
                TileProviderFactory.setLanguages(info.languagesPreference.split(","));
            }
            parseZoomLevel(info.zoomLevel);
            if (!info.boundingBox.contains(map.getMapPosition().getGeoPoint())) {
                MAP_MAPSFORGE.zoomToBounds(info.boundingBox);
            }
        }
    }

    @Override
    public void setPreferredLanguage(final String language) {
        tileSource.setPreferredLanguage(language);
    }
}
