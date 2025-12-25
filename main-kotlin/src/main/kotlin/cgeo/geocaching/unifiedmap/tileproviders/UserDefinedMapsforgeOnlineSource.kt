// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.unifiedmap.tileproviders

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings

import android.net.Uri

import androidx.core.util.Pair

import org.apache.commons.lang3.StringUtils

class UserDefinedMapsforgeOnlineSource : AbstractMapsforgeOnlineTileProvider() {
    UserDefinedMapsforgeOnlineSource() {
        super(CgeoApplication.getInstance().getString(R.string.settings_userDefinedTileProvider), Uri.parse(Settings.getUserDefinedTileProviderUri()), "/{Z}/{X}/{Y}.png", 2, 18, Pair<>(CgeoApplication.getInstance().getString(R.string.settings_userDefinedTileProvider), true))
        val fullUri: Uri = Uri.parse(Settings.getUserDefinedTileProviderUri())

        val mapUri: String = fullUri.getScheme() + "://" + fullUri.getHost()
        setMapUri(Uri.parse(mapUri))

        String tilePath = fullUri.getPath()
        if (tilePath != null) {
            if (!(tilePath.contains("{X}") && tilePath.contains("{Y}"))) {
                if (!tilePath.endsWith("/")) {
                    tilePath += "/"
                }
                tilePath += "{Z}/{X}/{Y}.png"
            }
            setTilePath(tilePath)
        }
    }

    public static Boolean isConfigured() {
        val uri: String = Settings.getUserDefinedTileProviderUri()
        return StringUtils.isNotBlank(uri) && StringUtils.isNotBlank(Uri.parse(uri).getHost())
    }

}
