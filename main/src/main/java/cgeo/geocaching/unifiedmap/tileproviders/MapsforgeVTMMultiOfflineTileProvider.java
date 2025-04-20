package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.unifiedmap.LayerHelper;
import cgeo.geocaching.unifiedmap.mapsforgevtm.MapsforgeVtmFragment;

import android.net.Uri;

import androidx.core.util.Pair;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.oscim.core.BoundingBox;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MapInfo;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;

class MapsforgeVTMMultiOfflineTileProvider extends AbstractMapsforgeVTMOfflineTileProvider {

    private final List<ImmutablePair<String, Uri>> maps;

    MapsforgeVTMMultiOfflineTileProvider(final List<ImmutablePair<String, Uri>> maps) {
        super(CgeoApplication.getInstance().getString(R.string.map_source_osm_offline_combined), Uri.parse(""), 999, 0);
        this.maps = maps;
    }

    @Override
    public void addTileLayer(final MapsforgeVtmFragment fragment, final Map map) {
        // collect metadata first: languages, zoom level range and bounding boxes
        final ArrayList<String> languages = new ArrayList<>();
        final StringBuilder mapAttribution = new StringBuilder();
        BoundingBox boundingBox = null;
        for (ImmutablePair<String, Uri> data : maps) {
            final MapFileTileSource source = new MapFileTileSource();
            source.setMapFileInputStream((FileInputStream) ContentStorage.get().openForRead(data.right));
            source.open();
            final MapInfo info = source.getMapInfo();
            source.close();
            if (info != null) {
                checkLanguage(languages, info.languagesPreference);
                parseZoomLevel(info.zoomLevel);
                boundingBox = boundingBox == null ? info.boundingBox : boundingBox.extendBoundingBox(info.boundingBox);

                // map attribution
                final String temp = StringUtils.isNotBlank(info.comment) ? info.comment : StringUtils.isNotBlank(info.createdBy) ? info.createdBy : "";
                mapAttribution.append("<p><b>").append(data.left).append(":</b>");
                if (StringUtils.isNotBlank(temp)) {
                    mapAttribution.append("<br />").append(temp);
                }
                mapAttribution.append("</p>");
            }
        }

        // now prepare combined map
        tileSource = new MultiMapFileTileSource();
        tileSource.setPreferredLanguage(Settings.getMapLanguage());
        for (ImmutablePair<String, Uri> data : maps) {
            final MapFileTileSource mapFileTileSource = new MapFileTileSource();
            mapFileTileSource.setMapFileInputStream((FileInputStream) ContentStorage.get().openForRead(data.right));
            ((MultiMapFileTileSource) tileSource).add(mapFileTileSource);
        }
        supportsLanguages = !languages.isEmpty();
        if (supportsLanguages) {
            TileProviderFactory.setLanguages(languages.toArray(new String[]{}));
        }
        setMapAttribution(new Pair<>(mapAttribution.toString(), true));

        final VectorTileLayer tileLayer = (VectorTileLayer) fragment.setBaseMap((MultiMapFileTileSource) tileSource);
        fragment.addLayer(LayerHelper.ZINDEX_BUILDINGS, new BuildingLayer(map, tileLayer));
        fragment.addLayer(LayerHelper.ZINDEX_LABELS, new LabelLayer(map, tileLayer));
        fragment.applyTheme();
    }

    private void checkLanguage(final ArrayList<String> languages, final String languagesPreference) {
        if (StringUtils.isNotBlank(languagesPreference)) {
            for (String language : languagesPreference.split(",")) {
                boolean found = false;
                for (String comp : languages) {
                    if (StringUtils.equals(comp, language)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    languages.add(language);
                }
            }
        }
    }

}
