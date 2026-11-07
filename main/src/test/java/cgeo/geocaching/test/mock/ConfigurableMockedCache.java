package cgeo.geocaching.test.mock;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

/**
 * A configurable mock cache for use in unit tests.
 * <p>
 * Unlike {@code MockedCache} (which reads HTML files and depends on Android instrumentation),
 * this class extends {@link Geocache} directly and can be used in pure JUnit tests.
 * <p>
 * The {@link #isOwner()} method is overridden to return a configurable value instead of
 * delegating to the connector (which would require Android framework initialization).
 * All other properties can be configured using the existing setters from {@link Geocache}.
 */
public class ConfigurableMockedCache extends Geocache {

    private boolean isOwner;

    /**
     * Creates a minimal configurable cache with sensible defaults.
     *
     * @param geocode the geocode (e.g. "GC12345")
     */
    public ConfigurableMockedCache(final String geocode) {
        setGeocode(geocode);
        setType(CacheType.TRADITIONAL);
        setName("Test Cache");
        setSize(CacheSize.REGULAR);
        setDifficulty(1.0f);
        setTerrain(1.0f);
    }

    // --- isOwner override (not settable via Geocache setter) ---

    @Override
    public boolean isOwner() {
        return isOwner;
    }

    public void setIsOwner(final boolean isOwner) {
        this.isOwner = isOwner;
    }
}

