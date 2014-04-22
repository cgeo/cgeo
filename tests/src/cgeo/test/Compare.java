package cgeo.test;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

public abstract class Compare {

    public static void assertCompareCaches(ICache expected, Geocache actual, boolean all) {
        String geocode = expected.getGeocode();
        assertThat(actual).isNotNull();
        assertThat(expected.getGeocode()).as("Cache " + geocode + ": geocode").isEqualTo(actual.getGeocode());
        assertThat(expected.getType()).as("Cache " + geocode + ": type").isEqualTo(actual.getType());
        assertThat(expected.getOwnerDisplayName()).as("Cache " + geocode + ": OwnerDisplayName").isEqualTo(actual.getOwnerDisplayName());
        assertThat(expected.getDifficulty()).as("Cache " + geocode + ": difficulty").isEqualTo(actual.getDifficulty());
        assertThat(expected.getTerrain()).as("Cache " + geocode + ": terrain").isEqualTo(actual.getTerrain());
        assertThat(expected.isDisabled()).as("Cache " + geocode + ": disabled").isEqualTo(actual.isDisabled());
        assertThat(expected.isArchived()).as("Cache " + geocode + ": archived").isEqualTo(actual.isArchived());
        assertThat(actual.getSize()).overridingErrorMessage("Cache " + geocode + ": expected size", expected.getSize()).isEqualTo(expected.getSize());
        assertThat(expected.getName()).as("Cache " + geocode + ": name").isEqualTo(actual.getName());
        assertThat(expected.getGuid()).as("Cache " + geocode + ": guid").isEqualTo(actual.getGuid());
        assertThat(expected.getFavoritePoints()).as("Cache " + geocode + ": fav points").isLessThanOrEqualTo(actual.getFavoritePoints());
        final Date hiddenDate = actual.getHiddenDate();
        assertThat(hiddenDate).isNotNull();
        assert hiddenDate != null; // silence the eclipse compiler in the next line
        assertThat(expected.getHiddenDate().toString()).as("Cache " + geocode + ": hidden date").isEqualTo(hiddenDate.toString());
        assertThat(expected.isPremiumMembersOnly()).as("Cache " + geocode + ": premium only").isEqualTo(actual.isPremiumMembersOnly());

        if (all) {
            assertThat(expected.getCoords()).as("Cache " + geocode + ": coords").isEqualTo(actual.getCoords());
            assertThat(actual.isReliableLatLon()).as("Cache " + geocode + ": reliable latlon").isTrue();
            assertThat(expected.isOwner()).as("Cache " + geocode + ": owning status").isEqualTo(actual.isOwner());
            assertThat(expected.getOwnerUserId()).as("Cache " + geocode + ": owner user id").isEqualTo(actual.getOwnerUserId());
            assertThat(StringUtils.equals(expected.getHint(), actual.getHint()) || StringUtils.equals(expected.getHint(), CryptUtils.rot13(actual.getHint()))).isTrue();
            assertThat(actual.getDescription()).as("description").startsWith(expected.getDescription());
            assertThat(expected.getShortDescription()).as("Cache " + geocode + ": short description").isEqualTo(actual.getShortDescription());
            assertThat(expected.getCacheId()).as("Cache " + geocode + ": cache id").isEqualTo(actual.getCacheId());
            assertThat(expected.getLocation()).as("Cache " + geocode + ": location").isEqualTo(actual.getLocation());
            assertThat(expected.isFound()).as("Cache " + geocode + ": found status").isEqualTo(actual.isFound());
            assertThat(expected.isFavorite()).as("Cache " + geocode + ": favorite status").isEqualTo(actual.isFavorite());
            assertThat(expected.isOnWatchlist()).as("Cache " + geocode + ": watchlist status").isEqualTo(actual.isOnWatchlist());

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
            assertThat(expectedSpoilersSize).as("Cache " + geocode + ": spoiler count").isEqualTo(actualSpoilersSize);
        }
    }

}
