package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.net.Uri;

import androidx.core.util.Pair;

import org.apache.commons.lang3.StringUtils;

public class UserDefinedMapsforgeOnlineSource extends AbstractMapsforgeOnlineTileProvider {
    UserDefinedMapsforgeOnlineSource() {
        super(CgeoApplication.getInstance().getString(R.string.settings_userDefinedTileProvider), Uri.parse(Settings.getUserDefinedTileProviderUri()), "/{Z}/{X}/{Y}.png", 2, 18, new Pair<>(CgeoApplication.getInstance().getString(R.string.settings_userDefinedTileProvider), true));
        final Uri fullUri = Uri.parse(Settings.getUserDefinedTileProviderUri());

        final String mapUri = fullUri.getScheme() + "://" + fullUri.getHost();
        setMapUri(Uri.parse(mapUri));

        String tilePath = fullUri.getPath();
        if (tilePath != null) {
            if (!(tilePath.contains("{X}") && tilePath.contains("{Y}"))) {
                if (!tilePath.endsWith("/")) {
                    tilePath += "/";
                }
                tilePath += "{Z}/{X}/{Y}.png";
            }
            setTilePath(tilePath);
        }
    }

    public static boolean isConfigured() {
        return StringUtils.isNotBlank(Settings.getUserDefinedTileProviderUri());
    }

}
