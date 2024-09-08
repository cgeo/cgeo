package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.net.Uri;

import androidx.core.util.Pair;

import org.apache.commons.lang3.StringUtils;
import static org.oscim.map.Viewport.MIN_ZOOM_LEVEL;

public class UserDefinedMapsforgeVTMOnlineSource extends AbstractMapsforgeVTMOnlineTileProvider {
    UserDefinedMapsforgeVTMOnlineSource() {
        super(CgeoApplication.getInstance().getString(R.string.settings_userDefinedTileProvider), Uri.parse(Settings.getUserDefinedTileProviderUri()), "/{Z}/{X}/{Y}.png", MIN_ZOOM_LEVEL, 18, new Pair<>(CgeoApplication.getInstance().getString(R.string.settings_userDefinedTileProvider), true));
    }

    public static boolean isConfigured() {
        return StringUtils.isNotBlank(Settings.getUserDefinedTileProviderUri());
    }
}
