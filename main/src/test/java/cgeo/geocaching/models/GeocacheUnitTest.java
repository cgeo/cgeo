package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.test.mock.ConfigurableMockedCache;

import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocacheUnitTest {

    @Test
    public void testGetPossibleLogTypes() {
        final Geocache gcCache = new Geocache();
        gcCache.setGeocode("GC123");
        gcCache.setType(CacheType.TRADITIONAL);
        assertThat(gcCache.getPossibleLogTypes()).as("GC cache possible log-types").contains(LogType.DIDNT_FIND_IT);
        assertThat(gcCache.getPossibleLogTypes()).as("GC cache possible log-types").contains(LogType.NOTE);
        assertThat(gcCache.getPossibleLogTypes()).as("GC cache possible log-types").doesNotContain(LogType.NEEDS_MAINTENANCE);
        assertThat(gcCache.getPossibleLogTypes()).as("GC cache possible log-types").doesNotContain(LogType.WEBCAM_PHOTO_TAKEN);
        gcCache.setType(CacheType.WEBCAM);
        assertThat(gcCache.getPossibleLogTypes()).as("GC cache possible webcam log-types").contains(LogType.WEBCAM_PHOTO_TAKEN);
        assertThat(gcCache.getPossibleLogTypes()).as("GC cache possible webcam log-types").doesNotContain(LogType.NEEDS_MAINTENANCE);

        final Geocache ocCache = new Geocache();
        ocCache.setGeocode("OC1234");
        ocCache.setType(CacheType.TRADITIONAL);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible log-types").contains(LogType.DIDNT_FIND_IT);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible log-types").contains(LogType.NOTE);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible log-types").doesNotContain(LogType.NEEDS_MAINTENANCE);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible log-types").doesNotContain(LogType.WEBCAM_PHOTO_TAKEN);

        ocCache.setType(CacheType.WEBCAM);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible webcam log-types").doesNotContain(LogType.WEBCAM_PHOTO_TAKEN);
        assertThat(ocCache.getPossibleLogTypes()).as("OC cache possible webcam log-types").doesNotContain(LogType.NEEDS_MAINTENANCE);
    }

    @Test
    public void testGetPossibleOfflineLogTypes() {
        final Geocache gcCache = new Geocache();
        gcCache.setGeocode("GC123");
        gcCache.setType(CacheType.TRADITIONAL);
        final List<LogType> gcOnlineLogTypes = gcCache.getPossibleLogTypes();
        final List<LogType> gcOfflineLogTypes = gcCache.getPossibleOfflineLogTypes();
        assertThat(gcOfflineLogTypes.containsAll(gcOnlineLogTypes)).isTrue();
        assertThat(gcOfflineLogTypes).as("GC cache offline-possible log-types").contains(LogType.NEEDS_MAINTENANCE);
        assertThat(gcOfflineLogTypes).as("GC cache offline-possible log-types").contains(LogType.NEEDS_ARCHIVE);

        final Geocache ocCache = new Geocache();
        ocCache.setGeocode("OC1234");
        ocCache.setType(CacheType.TRADITIONAL);
        final List<LogType> ocOnlineLogTypes = ocCache.getPossibleLogTypes();
        final List<LogType> ocOfflineLogTypes = ocCache.getPossibleOfflineLogTypes();
        // OC has no log type which is not possible for offline logging, so both lists must be identical
        assertThat(ocOfflineLogTypes.containsAll(ocOnlineLogTypes)).isTrue();
        assertThat(ocOnlineLogTypes.containsAll(ocOfflineLogTypes)).isTrue();
    }

    @Test
    public void testGetPossibleOwnerLogTypes() {
        final ConfigurableMockedCache gcCache = new ConfigurableMockedCache("GC123");
        gcCache.setIsOwner(true);
        gcCache.setDisabled(true);
        final List<LogType> gcOnlineLogTypes = gcCache.getPossibleLogTypes();
        assertThat(gcOnlineLogTypes).as("GC cache online-possible log-types").contains(LogType.ARCHIVE);
        assertThat(gcOnlineLogTypes).as("GC cache online-possible log-types").contains(LogType.OWNER_MAINTENANCE);
        assertThat(gcOnlineLogTypes).as("GC cache online-possible log-types").contains(LogType.ENABLE_LISTING);

        final ConfigurableMockedCache ocCache = new ConfigurableMockedCache("OC123");
        ocCache.setIsOwner(true);
        final List<LogType> ocOnlineLogTypes = ocCache.getPossibleLogTypes();
        assertThat(ocOnlineLogTypes).as("OC cache online-possible log-types").contains(LogType.ARCHIVE);
        assertThat(ocOnlineLogTypes).as("OC cache online-possible log-types").contains(LogType.OWNER_MAINTENANCE);
        assertThat(ocOnlineLogTypes).as("OC cache online-possible log-types").contains(LogType.TEMP_DISABLE_LISTING);
    }
}
