package cgeo.geocaching.test;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;

import androidx.core.util.Consumer;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** Sets up a (temporary) cache in Cgeo for testing purposes */
public class CgeoTemporaryCacheRule implements TestRule {

    private static final AtomicInteger cacheIdSupplier = new AtomicInteger(0);

    private Geocache cache = null;
    private final Consumer<Geocache> modifier;

    public CgeoTemporaryCacheRule() {
        this(null);
    }

    public CgeoTemporaryCacheRule(final Consumer<Geocache> modifier) {
        this.modifier = modifier;
    }


    public Geocache getCache() {
        return cache;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                try {
                    createCache();
                    base.evaluate();
                } finally {
                    deleteCache();
                }
            }
        };
    }

    private void createCache() {
        this.cache = CgeoTestUtils.createTestCache();
        this.cache.setGeocode("TEST" + cacheIdSupplier.incrementAndGet());
        if (modifier != null) {
            modifier.accept(this.cache);
        }
        DataStore.saveCache(this.cache, Collections.singleton(LoadFlags.SaveFlag.CACHE));
    }

    private void deleteCache() {
        CgeoTestUtils.removeCacheCompletely(this.cache.getGeocode());
    }


}
