package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.R;
import cgeo.geocaching.unifiedmap.tiles.TileUrlShare;
import cgeo.geocaching.unifiedmap.tiles.TileUrlUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/** The stored form of a user-defined tile provider, as kept in the preferences. */
public class PrefUserDefinedTileProvider {
    /**
     * Key of a tile provider read from the legacy single-Uri format of the setting.
     * Deliberately "null", as the tile provider id derived from it must stay identical to
     * the one used before multiple providers were supported (which appended the Uri's last
     * path segment, always null for user-defined providers) - otherwise an upgrading user
     * would lose their selected map source.
     */
    public static final String LEGACY_KEY = "null";

    private final @NonNull String key;
    private final String name;
    private final String uri;

    @JsonCreator
    public PrefUserDefinedTileProvider(@JsonProperty("key") final String key, @JsonProperty("name") final String name, @JsonProperty("uri") final String uri) {
        this.key = key;
        this.name = name;
        this.uri = uri;
    }

    @NonNull
    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    /**
     * Name to display for this provider: the name the user gave it, the host of its uri, or a
     * generic fallback. Not part of the stored form - it is derived at read time, so renaming the
     * fallback or fixing the host extraction takes effect without rewriting anybody's settings.
     */
    @JsonIgnore
    public String getDisplayName() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        }
        final String host = TileUrlUtils.host(uri);
        return StringUtils.isNotBlank(host) ? host : LocalizationUtils.getString(R.string.settings_userDefinedTileProvider);
    }

    /** a provider can only be registered if it has a uri with a host */
    @JsonIgnore
    public boolean isConfigured() {
        return StringUtils.isNotBlank(TileUrlUtils.host(uri));
    }

    /** the form to hand to somebody else, e.g. in a chat message: {@code name;uri} */
    @NonNull
    public String toShareableText() {
        return TileUrlShare.format(StringUtils.defaultString(name), StringUtils.defaultString(uri));
    }

    /**
     * Reads a provider back from text somebody shared.
     *
     * <p>Gets a key of its own, as it describes a provider this installation has not seen before.
     * Callers that only want the name and the uri - the paste handler in the edit dialog - simply
     * ignore it.</p>
     *
     * @return the provider, or {@code null} if the text holds no usable uri
     */
    @Nullable
    public static PrefUserDefinedTileProvider fromShareableText(@Nullable final String text) {
        final String[] shared = TileUrlShare.parse(text);
        return shared == null ? null : new PrefUserDefinedTileProvider(UUID.randomUUID().toString(), shared[0], shared[1]);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PrefUserDefinedTileProvider p = (PrefUserDefinedTileProvider) o;
        return p.getKey().equals(this.getKey());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return StringUtils.isNotBlank(name) ? name : StringUtils.defaultString(uri);
    }
}
