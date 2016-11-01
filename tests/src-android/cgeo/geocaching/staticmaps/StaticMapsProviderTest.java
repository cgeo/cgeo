package cgeo.geocaching.staticmaps;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;
import cgeo.geocaching.storage.LocalStorage;
import cgeo.geocaching.utils.FileUtils;

import android.test.suitebuilder.annotation.Suppress;

import java.io.File;

import junit.framework.TestCase;

@Suppress
public class StaticMapsProviderTest extends TestCase {

    public static void testDownloadStaticMaps() {
        final double lat = 52.354176d;
        final double lon = 9.745685d;
        final String geocode = "GCTEST1";

        final boolean backupStore = Settings.isStoreOfflineMaps();
        final boolean backupStoreWP = Settings.isStoreOfflineWpMaps();
        TestSettings.setStoreOfflineMaps(true);
        TestSettings.setStoreOfflineWpMaps(true);
        try {
            final Geopoint gp = new Geopoint(lat + 0.25d, lon + 0.25d);
            final Geocache cache = new Geocache();
            cache.setGeocode(geocode);
            cache.setCoords(gp);
            cache.setCacheId(String.valueOf(1));

            final Waypoint theFinal = new Waypoint("Final", WaypointType.FINAL, false);
            final Geopoint finalGp = new Geopoint(lat + 0.25d + 1, lon + 0.25d + 1);
            theFinal.setCoords(finalGp);
            theFinal.setId(1);
            cache.addOrChangeWaypoint(theFinal, false);

            final Waypoint trailhead = new Waypoint("Trail head", WaypointType.TRAILHEAD, false);
            final Geopoint trailheadGp = new Geopoint(lat + 0.25d + 2, lon + 0.25d + 2);
            trailhead.setCoords(trailheadGp);
            trailhead.setId(2);
            cache.addOrChangeWaypoint(trailhead, false);

            // make sure we don't have stale downloads
            deleteCacheDirectory(geocode);
            assertThat(StaticMapsProvider.hasStaticMap(cache)).isFalse();
            assertThat(StaticMapsProvider.hasStaticMapForWaypoint(geocode, theFinal)).isFalse();
            assertThat(StaticMapsProvider.hasStaticMapForWaypoint(geocode, trailhead)).isFalse();

            // download
            StaticMapsProvider.downloadMaps(cache).blockingAwait();

            try {
                Thread.sleep(10000);
            } catch (final InterruptedException e) {
                fail();
            }

            // check download
            assertThat(StaticMapsProvider.hasStaticMap(cache)).isTrue();
            assertThat(StaticMapsProvider.hasStaticMapForWaypoint(geocode, theFinal)).isTrue();
            assertThat(StaticMapsProvider.hasStaticMapForWaypoint(geocode, trailhead)).isTrue();

            // waypoint static maps hashcode dependent
            trailhead.setCoords(new Geopoint(lat + 0.24d + 2, lon + 0.25d + 2));
            assertThat(StaticMapsProvider.hasStaticMapForWaypoint(geocode, trailhead)).isFalse();
        } finally {
            TestSettings.setStoreOfflineWpMaps(backupStoreWP);
            TestSettings.setStoreOfflineMaps(backupStore);
            deleteCacheDirectory(geocode);
        }
    }

    private static void deleteCacheDirectory(final String geocode) {
        final File cacheDir = LocalStorage.getStorageDir(geocode);
        FileUtils.deleteDirectory(cacheDir);
    }

}
