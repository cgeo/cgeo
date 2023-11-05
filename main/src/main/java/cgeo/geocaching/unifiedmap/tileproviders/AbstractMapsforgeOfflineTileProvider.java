package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeVtmFragment;

import android.net.Uri;

import androidx.core.util.Pair;

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
        super(name, uri, zoomMin, zoomMax, new Pair<>("", false));
        supportsThemes = true;
        supportsThemeOptions = true; // rule of thumb, not all themes support options
    }

    @Override
    public void addTileLayer(final MapsforgeVtmFragment fragment, final Map map) {
        tileSource = new MapFileTileSource();
        tileSource.setPreferredLanguage(Settings.getMapLanguage());
        tileSource.setMapFileInputStream((FileInputStream) ContentStorage.get().openForRead(mapUri));
        final VectorTileLayer tileLayer = (VectorTileLayer) fragment.setBaseMap(tileSource);
        fragment.addLayer(LayerHelper.ZINDEX_BUILDINGS, new BuildingLayer(map, tileLayer));
        fragment.addLayer(LayerHelper.ZINDEX_LABELS, new LabelLayer(map, tileLayer));
        fragment.applyTheme();

        final MapInfo info = tileSource.getMapInfo();
        if (info != null) {
            supportsLanguages = StringUtils.isNotBlank(info.languagesPreference);
            if (supportsLanguages) {
                TileProviderFactory.setLanguages(info.languagesPreference.split(","));
            }
            parseZoomLevel(info.zoomLevel);
            if (!info.boundingBox.contains(map.getMapPosition().getGeoPoint())) {
                fragment.zoomToBounds(info.boundingBox);
            }
            // map attribution
            if (StringUtils.isNotBlank(info.comment)) {
                setMapAttribution(new Pair<>(info.comment, true));
            } else if (StringUtils.isNotBlank(info.createdBy)) {
                setMapAttribution(new Pair<>(info.createdBy, true));
            }
        }
    }

    @Override
    public void setPreferredLanguage(final String language) {
        tileSource.setPreferredLanguage(language);
    }
}
