package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.settings.Settings.PrefCustomMap;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.apache.commons.lang3.StringUtils;

import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

public class CustomMapsforgeVTMOnlineSource extends AbstractMapsforgeVTMOnlineTileProvider {

    private final String customMapId;

    CustomMapsforgeVTMOnlineSource(final PrefCustomMap customMap) {
        super(customMap.getName(), Uri.parse(CustomMapUrl.getBaseUrl(customMap.getUrl())),
                StringUtils.defaultString(CustomMapUrl.getTilePath(customMap.getUrl())), MIN_ZOOM_LEVEL, 20,
                new Pair<>(customMap.getName(), true), false);
        customMapId = customMap.getId();
        supportsHillshading = true;
    }

    public static String getId(final String customMapId) {
        return CustomMapsforgeVTMOnlineSource.class.getName() + ":" + customMapId;
    }

    @Override
    @NonNull
    public String getId() {
        return getId(customMapId);
    }
}
