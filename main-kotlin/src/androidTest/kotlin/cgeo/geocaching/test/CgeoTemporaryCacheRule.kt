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

package cgeo.geocaching.test

import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore

import androidx.core.util.Consumer

import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Sets up a (temporary) cache in Cgeo for testing purposes */
class CgeoTemporaryCacheRule : TestRule {

    private static val cacheIdSupplier: AtomicInteger = AtomicInteger(0)

    private var cache: Geocache = null
    private final Consumer<Geocache> modifier

    public CgeoTemporaryCacheRule() {
        this(null)
    }

    public CgeoTemporaryCacheRule(final Consumer<Geocache> modifier) {
        this.modifier = modifier
    }


    public Geocache getCache() {
        return cache
    }

    override     public Statement apply(final Statement base, final Description description) {
        return Statement() {
            override             public Unit evaluate() throws Throwable {

                try {
                    createCache()
                    base.evaluate()
                } finally {
                    deleteCache()
                }
            }
        }
    }

    private Unit createCache() {
        this.cache = CgeoTestUtils.createTestCache()
        this.cache.setGeocode("TEST" + cacheIdSupplier.incrementAndGet())
        if (modifier != null) {
            modifier.accept(this.cache)
        }
        DataStore.saveCache(this.cache, Collections.singleton(LoadFlags.SaveFlag.CACHE))
    }

    private Unit deleteCache() {
        CgeoTestUtils.removeCacheCompletely(this.cache.getGeocode())
    }


}
