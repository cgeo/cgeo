package cgeo.test;

import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.utils.CryptUtils;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.Nullable;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public abstract class Compare {

    public static void assertCompareCaches(final Geocache expected, @Nullable final Geocache actual, final boolean all) {
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
        assertThat(hiddenDate).as(cacheStr + " hidden date").isEqualTo(expected.getHiddenDate());
        assertThat(actual.isPremiumMembersOnly()).as(cacheStr + "premium only").isEqualTo(expected.isPremiumMembersOnly());

        if (all) {
            assertThat(actual.getCoords()).as(cacheStr + "coords").isEqualTo(expected.getCoords());
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

            for (final String attribute : expected.getAttributes()) {
                assertThat(actual.getAttributes()).as("attributes of " + actual.getGeocode()).contains(attribute);
            }
            for (final LogType logType : expected.getLogCounts().keySet()) {
                assertThat(actual.getLogCounts().get(logType)).as("logcount of " + geocode + " for type " + logType.toString()).isGreaterThanOrEqualTo(expected.getLogCounts().get(logType));
            }
            for (final Waypoint expectedWpt : expected.getWaypoints()) {
                final Waypoint actualWpt = actual.getWaypointByPrefix(expectedWpt.getPrefix());
                assertThat(actualWpt).as("waypoint " + expectedWpt.getPrefix() + " of " + geocode).isNotNull();
                assertThat(actualWpt.getLookup()).as("waypoint lookup " + expectedWpt.getPrefix() + " of " + geocode).isEqualTo(expectedWpt.getLookup());
                assertThat(actualWpt.getCoords()).as("waypoint coords " + expectedWpt.getPrefix() + " of " + geocode).isEqualTo(expectedWpt.getCoords());
                assertThat(actualWpt.getName()).as("waypoint name " + expectedWpt.getPrefix() + " of " + geocode).isEqualTo(expectedWpt.getName());
                assertThat(TextUtils.stripHtml(actualWpt.getNote())).as("waypoint note " + expectedWpt.getPrefix() + " of " + geocode).isEqualTo(expectedWpt.getNote());
                assertThat(actualWpt.getWaypointType()).as("waypoint type " + expectedWpt.getPrefix() + " of " + geocode).isEqualTo(expectedWpt.getWaypointType());
            }

            // The inventories can differ too often, therefore we don't compare them. Also, the personal note
            // cannot be expected to match with different tester accounts.

            assertThat(actual.getSpoilers()).as(cacheStr + " spoilers").hasSameSizeAs(expected.getSpoilers());
        }
    }

}
