package cgeo.test;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

public abstract class Compare {

    public static void assertCompareCaches(Geocache expected, Geocache actual, boolean all) {
        final String geocode = expected.getGeocode();
        final String cacheStr = "Cache " + geocode + ": ";
        assertThat(actual).isNotNull();
        assertThat(actual.getGeocode()).as(cacheStr + "geocode").isEqualTo(expected.getGeocode());
        assertThat(actual.getType()).as(cacheStr + "type").isEqualTo(expected.getType());
        assertThat(actual.getOwnerDisplayName()).as(cacheStr + "OwnerDisplayName").isEqualTo(expected.getOwnerDisplayName());
        assertThat(actual.getDifficulty()).as(cacheStr + "difficulty").isEqualTo(expected.getDifficulty());
        assertThat(actual.getTerrain()).as(cacheStr + "terrain").isEqualTo(expected.getTerrain());
        assertThat(actual.isDisabled()).as(cacheStr + "disabled").isEqualTo(expected.isDisabled());
        assertThat(actual.isArchived()).as(cacheStr + "archived").isEqualTo(expected.isArchived());
        assertThat(actual.getSize()).overridingErrorMessage(cacheStr + "expected size", expected.getSize()).isEqualTo(expected.getSize());
        assertThat(actual.getName()).as(cacheStr + "name").isEqualTo(expected.getName());
        assertThat(actual.getGuid()).as(cacheStr + "guid").isEqualTo(expected.getGuid());
        assertThat(actual.getFavoritePoints()).as(cacheStr + "fav points").isGreaterThanOrEqualTo(expected.getFavoritePoints());
        final Date hiddenDate = actual.getHiddenDate();
        assertThat(hiddenDate).isNotNull();
        assert hiddenDate != null; // silence the eclipse compiler in the next line
        assertThat(hiddenDate).as(cacheStr + " hidden date").isEqualTo(expected.getHiddenDate());
        assertThat(actual.isPremiumMembersOnly()).as(cacheStr + "premium only").isEqualTo(expected.isPremiumMembersOnly());

        if (all) {
            assertThat(actual.getCoords()).as(cacheStr + "coords").isEqualTo(expected.getCoords());
            assertThat(actual.isReliableLatLon()).as(cacheStr + "reliable latlon").isTrue();
            assertThat(actual.isOwner()).as(cacheStr + "owning status").isEqualTo(expected.isOwner());
            assertThat(actual.getOwnerUserId()).as(cacheStr + "owner user id").isEqualTo(expected.getOwnerUserId());
            assertThat(StringUtils.equals(expected.getHint(), actual.getHint()) || StringUtils.equals(expected.getHint(), CryptUtils.rot13(actual.getHint()))).isTrue();
            assertThat(actual.getDescription()).as("description").startsWith(expected.getDescription());
            assertThat(actual.getShortDescription()).as(cacheStr + "short description").isEqualTo(expected.getShortDescription());
            assertThat(actual.getCacheId()).as(cacheStr + "cache id").isEqualTo(expected.getCacheId());
            assertThat(actual.getLocation()).as(cacheStr + "location").isEqualTo(expected.getLocation());
            assertThat(actual.isFound()).as(cacheStr + "found status").isEqualTo(expected.isFound());
            assertThat(actual.isFavorite()).as(cacheStr + "favorite status").isEqualTo(expected.isFavorite());
            assertThat(actual.isOnWatchlist()).as(cacheStr + "watchlist status").isEqualTo(expected.isOnWatchlist());

            for (String attribute : expected.getAttributes()) {
                assertThat(actual.getAttributes()).as("attributes of " + actual.getGeocode()).contains(attribute);
            }
            for (LogType logType : expected.getLogCounts().keySet()) {
                assertThat(actual.getLogCounts().get(logType)).as("logcount of " + geocode + " for type " + logType.toString()).isGreaterThanOrEqualTo(expected.getLogCounts().get(logType));
            }

            // The inventories can differ too often, therefore we don't compare them. Also, the personal note
            // cannot be expected to match with different tester accounts.

            final int actualSpoilersSize = null != actual.getSpoilers() ? actual.getSpoilers().size() : 0;
            final int expectedSpoilersSize = null != expected.getSpoilers() ? expected.getSpoilers().size() : 0;
            assertThat(actualSpoilersSize).as(cacheStr + "spoiler count").isEqualTo(expectedSpoilersSize);
        }
    }

}
