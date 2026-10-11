package cgeo.geocaching.unifiedmap.tileproviders;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.apache.commons.lang3.StringUtils;

public class UserDefinedMapsforgeOnlineSource extends AbstractMapsforgeOnlineTileProvider {
    @NonNull private final String key;

    UserDefinedMapsforgeOnlineSource(final PrefUserDefinedTileProvider provider) {
        super(provider.getDisplayName(), Uri.parse(provider.getUri()), "/{Z}/{X}/{Y}.png", 2, 18, new Pair<>(provider.getDisplayName(), true));
        this.key = provider.getKey();
        final Uri fullUri = Uri.parse(provider.getUri());

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
            final String query = fullUri.getQuery();
            setTilePath(tilePath + (StringUtils.isNotBlank(query) ? "?" + query : ""));
        }
    }

    /**
     * Deriving the id from the map Uri (as the superclass does) is not sufficient here,
     * as several user-defined providers may share the same host.
     */
    @Override
    @NonNull
    public String getId() {
        return getClass().getName() + ":" + key;
    }

}
