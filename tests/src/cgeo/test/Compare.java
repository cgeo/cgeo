package cgeo.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;
import cgeo.geocaching.enumerations.LogType;

public abstract class Compare {

    public static void assertCompareCaches(ICache expected, Geocache actual, boolean all) {
        assertNotNull(actual);
        assertEquals(expected.getGeocode(), actual.getGeocode());
        assertTrue(expected.getType() == actual.getType());
        assertEquals(expected.getOwnerDisplayName(), actual.getOwnerDisplayName());
        assertEquals(expected.getDifficulty(), actual.getDifficulty());
        assertEquals(expected.getTerrain(), actual.getTerrain());
        assertEquals(expected.isDisabled(), actual.isDisabled());
        assertEquals(expected.isArchived(), actual.isArchived());
        assertEquals(expected.getSize(), actual.getSize());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getGuid(), actual.getGuid());
        assertTrue(expected.getFavoritePoints() <= actual.getFavoritePoints());
        assertEquals(expected.getHiddenDate().toString(), actual.getHiddenDate().toString());
        assertEquals(expected.isPremiumMembersOnly(), actual.isPremiumMembersOnly());

        if (all) {
            assertEquals(expected.getCoords(), actual.getCoords());
            assertTrue(actual.isReliableLatLon());
            assertEquals(expected.isOwner(), actual.isOwner());
            assertEquals(expected.getOwnerUserId(), actual.getOwnerUserId());
            assertEquals(expected.getHint(), actual.getHint());
            assertTrue(actual.getDescription().startsWith(expected.getDescription()));
            assertEquals(expected.getShortDescription(), actual.getShortDescription());
            assertEquals(expected.getCacheId(), actual.getCacheId());
            assertEquals(expected.getLocation(), actual.getLocation());
            assertEquals(expected.isFound(), actual.isFound());
            assertEquals(expected.isFavorite(), actual.isFavorite());
            assertEquals(expected.isWatchlist(), actual.isWatchlist());

            for (String attribute : expected.getAttributes()) {
                assertTrue(actual.getAttributes().contains(attribute));
            }
            for (LogType logType : expected.getLogCounts().keySet()) {
                assertTrue(actual.getLogCounts().get(logType) >= expected.getLogCounts().get(logType));
            }

            // The inventories can differ too often, therefore we don't compare them. Also, the personal note
            // cannot be expected to match with different tester accounts.

            final int actualSpoilersSize = null != actual.getSpoilers() ? actual.getSpoilers().size() : 0;
            final int expectedSpoilersSize = null != expected.getSpoilers() ? expected.getSpoilers().size() : 0;
            assertEquals(expectedSpoilersSize, actualSpoilersSize);
        }
    }

}
