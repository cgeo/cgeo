package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.downloader.CompanionFileUtils;
import cgeo.geocaching.maps.mapsforge.v6.layers.HillShadingLayerHelper;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeFragment;
import cgeo.geocaching.utils.Log;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.labels.LabelLayer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileException;
import org.mapsforge.map.reader.header.MapFileInfo;
import org.mapsforge.map.view.MapView;

public class AbstractMapsforgeOfflineTileProvider extends AbstractMapsforgeTileProvider {

    private MapFile mapFile = null;
    private final String displayName;

    AbstractMapsforgeOfflineTileProvider(final String name, final Uri uri, final int zoomMin, final int zoomMax) {
        super(name, uri, zoomMin, zoomMax, new Pair<>("", false));
        supportsThemes = true;
        supportsThemeOptions = true; // rule of thumb, not all themes support options
        supportsHillshading = true;
        supportsBackgroundMaps = true;
        displayName = CompanionFileUtils.getDisplaynameForMap(uri);
    }

    @Override
    public void addTileLayer(final MapsforgeFragment fragment, final MapView map) {
        final InputStream mapStream = ContentStorage.get().openForRead(mapUri, true);
        if (mapStream != null) {
            try {
                mapFile = new MapFile((FileInputStream) mapStream, 0, Settings.getMapLanguage());
            } catch (MapFileException mfe) {
                Log.e("Problem opening map file '" + mapUri + "'", mfe);
            }
        }
        if (mapFile != null) {
            final MapFileInfo info = mapFile.getMapFileInfo();
            if (info != null) {
                supportsLanguages = StringUtils.isNotBlank(info.languagesPreference);
                if (supportsLanguages) {
                    TileProviderFactory.setLanguages(info.languagesPreference.split(","));
                }
                zoomMin = info.zoomLevelMin;
                zoomMax = info.zoomLevelMax;
                // map attribution
                if (StringUtils.isNotBlank(info.comment)) {
                    setMapAttribution(new Pair<>(info.comment, true));
                } else if (StringUtils.isNotBlank(info.createdBy)) {
                    setMapAttribution(new Pair<>(info.createdBy, true));
                }
            }
            createTileLayerAndLabelStore(fragment, map, mapFile);
        }
    }

    protected void createTileLayerAndLabelStore(final MapsforgeFragment fragment, final MapView map, final MapDataStore mapDataStore) {
        // create layers for tiles and labels
        tileLayer = new TileRendererLayer(fragment.getTileCache(), mapDataStore, map.getModel().mapViewPosition, false, false, true, AndroidGraphicFactory.INSTANCE, HillShadingLayerHelper.getHillsRenderConfig());

        tileLayer.setCacheTileMargin(1);
        tileLayer.setCacheZoomMinus(1);
        tileLayer.setCacheZoomPlus(2);
        map.getLayerManager().getLayers().add(tileLayer);

        fragment.applyTheme();

        final LabelLayer labelLayer = new LabelLayer(AndroidGraphicFactory.INSTANCE, ((TileRendererLayer) tileLayer).getLabelStore());
        map.getLayerManager().getLayers().add(labelLayer);
    }

    @Override
    public void setPreferredLanguage(final String language) {
        if (mapFile != null) {
            // @todo: mapFile.setPreferredLanguage(language);
        } else {
            Log.w("AbstractMapsforgeOfflineTileProvider.setPreferredLanguage: tilesource is null");
        }
    }

    @Override
    public String getDisplayName(@Nullable final String defaultDisplayName) {
        return StringUtils.isNotBlank(displayName) ? displayName : defaultDisplayName;
    }

}
