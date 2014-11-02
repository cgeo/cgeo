package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.LocalStorage;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;
import cgeo.geocaching.utils.RxUtils;

import junit.framework.TestCase;

import android.test.suitebuilder.annotation.Suppress;

import java.io.File;

@Suppress
public class StaticMapsProviderTest extends TestCase {

    public static void testDownloadStaticMaps() {
        final double lat = 52.354176d;
        final double lon = 9.745685d;
        String geocode = "GCTEST1";

        boolean backupStore = Settings.isStoreOfflineMaps();
        boolean backupStoreWP = Settings.isStoreOfflineWpMaps();
        TestSettings.setStoreOfflineMaps(true);
        TestSettings.setStoreOfflineWpMaps(true);
        try {
            Geopoint gp = new Geopoint(lat + 0.25d, lon + 0.25d);
            Geocache cache = new Geocache();
            cache.setGeocode(geocode);
            cache.setCoords(gp);
            cache.setCacheId(String.valueOf(1));

            Waypoint theFinal = new Waypoint("Final", WaypointType.FINAL, false);
            Geopoint finalGp = new Geopoint(lat + 0.25d + 1, lon + 0.25d + 1);
            theFinal.setCoords(finalGp);
            theFinal.setId(1);
            cache.addOrChangeWaypoint(theFinal, false);

            Waypoint trailhead = new Waypoint("Trail head", WaypointType.TRAILHEAD, false);
            Geopoint trailheadGp = new Geopoint(lat + 0.25d + 2, lon + 0.25d + 2);
            trailhead.setCoords(trailheadGp);
            trailhead.setId(2);
            cache.addOrChangeWaypoint(trailhead, false);

            // make sure we don't have stale downloads
            deleteCacheDirectory(geocode);
            assertThat(StaticMapsProvider.hasStaticMap(cache)).isFalse();
            assertThat(StaticMapsProvider.hasStaticMapForWaypoint(geocode, theFinal)).isFalse();
            assertThat(StaticMapsProvider.hasStaticMapForWaypoint(geocode, trailhead)).isFalse();

            // download
            RxUtils.waitForCompletion(StaticMapsProvider.downloadMaps(cache));

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
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

    private static void deleteCacheDirectory(String geocode) {
        File cacheDir = LocalStorage.getStorageDir(geocode);
        LocalStorage.deleteDirectory(cacheDir);
    }

}
