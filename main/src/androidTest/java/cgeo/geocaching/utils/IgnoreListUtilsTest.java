package cgeo.geocaching.utils;

import cgeo.geocaching.connector.gc.GCMemberState;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;

import java.util.Collections;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class IgnoreListUtilsTest {

    private static Geocache createGCCache() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GC1234");
        cache.setType(CacheType.TRADITIONAL);
        return cache;
    }

    @Test
    public void testAnySupportsIgnoreList() {
        assertThat(IgnoreListUtils.anySupportsIgnoreList(Collections.singletonList(createGCCache()))).isTrue();
    }

    @Test
    public void testAnySupportsIgnoringRequiresPremiumAndNotAlreadyIgnored() {
        final GCMemberState originalStatus = Settings.getGCMemberStatus();
        try {
            final Geocache cache = createGCCache();

            Settings.setGCMemberStatus(GCMemberState.BASIC);
            assertThat(IgnoreListUtils.anySupportsIgnoring(Collections.singletonList(cache))).isFalse();

            Settings.setGCMemberStatus(GCMemberState.PREMIUM);
            assertThat(IgnoreListUtils.anySupportsIgnoring(Collections.singletonList(cache))).isTrue();

            // already on the local ignore list -> no longer offered to be (re-)ignored
            cache.getLists().add(StoredList.IGNORE_LIST_ID);
            assertThat(IgnoreListUtils.anySupportsIgnoring(Collections.singletonList(cache))).isFalse();
        } finally {
            Settings.setGCMemberStatus(originalStatus);
        }
    }

    @Test
    public void testAnySupportsUnignoringRequiresPremiumAndBeingOnIgnoreList() {
        final GCMemberState originalStatus = Settings.getGCMemberStatus();
        try {
            final Geocache cache = createGCCache();
            Settings.setGCMemberStatus(GCMemberState.PREMIUM);

            // not (yet) on the local ignore list -> nothing to un-ignore
            assertThat(IgnoreListUtils.anySupportsUnignoring(Collections.singletonList(cache))).isFalse();

            cache.getLists().add(StoredList.IGNORE_LIST_ID);
            assertThat(IgnoreListUtils.anySupportsUnignoring(Collections.singletonList(cache))).isTrue();

            Settings.setGCMemberStatus(GCMemberState.BASIC);
            assertThat(IgnoreListUtils.anySupportsUnignoring(Collections.singletonList(cache))).isFalse();
        } finally {
            Settings.setGCMemberStatus(originalStatus);
        }
    }
}
