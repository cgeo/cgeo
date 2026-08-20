package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.settings.Settings.PrefCustomMap;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.apache.commons.lang3.StringUtils;

public class CustomMapsforgeOnlineSource extends AbstractMapsforgeOnlineTileProvider {

    private final String customMapId;

    CustomMapsforgeOnlineSource(final PrefCustomMap customMap) {
        super(customMap.getName(), Uri.parse(CustomMapUrl.getBaseUrl(customMap.getUrl())),
                StringUtils.defaultString(CustomMapUrl.getTilePath(customMap.getUrl())), 0, 20,
                new Pair<>(customMap.getName(), true), false);
        customMapId = customMap.getId();
    }

    public static String getId(final String customMapId) {
        return CustomMapsforgeOnlineSource.class.getName() + ":" + customMapId;
    }

    @Override
    @NonNull
    public String getId() {
        return getId(customMapId);
    }
}
