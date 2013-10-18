package cgeo.test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.ICache;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

public abstract class Compare {

    public static void assertCompareCaches(ICache expected, Geocache actual, boolean all) {
        String geocode = expected.getGeocode();
        assertNotNull("Cache " + geocode + " is missing", actual);
        assertEquals("Cache " + geocode + ": geocode wrong", expected.getGeocode(), actual.getGeocode());
        assertTrue("Cache " + geocode + ": type wrong", expected.getType() == actual.getType());
        assertEquals("Cache " + geocode + ": OwnerDisplayName wrong", expected.getOwnerDisplayName(), actual.getOwnerDisplayName());
        assertEquals("Cache " + geocode + ": difficulty wrong", expected.getDifficulty(), actual.getDifficulty());
        assertEquals("Cache " + geocode + ": terrain wrong", expected.getTerrain(), actual.getTerrain());
        assertEquals("Cache " + geocode + ": disabled wrong", expected.isDisabled(), actual.isDisabled());
        assertEquals("Cache " + geocode + ": archived wrong", expected.isArchived(), actual.isArchived());
        assertEquals("Cache " + geocode + ": size wrong", expected.getSize(), actual.getSize());
        assertEquals("Cache " + geocode + ": name wrong", expected.getName(), actual.getName());
        assertEquals("Cache " + geocode + ": guid wrong", expected.getGuid(), actual.getGuid());
        assertTrue("Cache " + geocode + ": fav points wrong", expected.getFavoritePoints() <= actual.getFavoritePoints());
        assertEquals("Cache " + geocode + ": hidden date wrong", expected.getHiddenDate().toString(), actual.getHiddenDate().toString());
        assertEquals("Cache " + geocode + ": premium only wrong", expected.isPremiumMembersOnly(), actual.isPremiumMembersOnly());

        if (all) {
            assertEquals("Cache " + geocode + ": coords wrong", expected.getCoords(), actual.getCoords());
            assertTrue("Cache " + geocode + ": reliable latlon wrong", actual.isReliableLatLon());
            assertEquals("Cache " + geocode + ": owning status wrong", expected.isOwner(), actual.isOwner());
            assertEquals("Cache " + geocode + ": owner user id wrong", expected.getOwnerUserId(), actual.getOwnerUserId());
            assertTrue("Cache " + geocode + ": hint wrong", StringUtils.equals(expected.getHint(), actual.getHint()) || StringUtils.equals(expected.getHint(), CryptUtils.rot13(actual.getHint())));
            assertTrue("Cache " + geocode + ": description wrong", actual.getDescription().startsWith(expected.getDescription()));
            assertEquals("Cache " + geocode + ": short description wrong", expected.getShortDescription(), actual.getShortDescription());
            assertEquals("Cache " + geocode + ": cache id wrong", expected.getCacheId(), actual.getCacheId());
            assertEquals("Cache " + geocode + ": location wrong", expected.getLocation(), actual.getLocation());
            assertEquals("Cache " + geocode + ": found status wrong", expected.isFound(), actual.isFound());
            assertEquals("Cache " + geocode + ": favorite status wrong", expected.isFavorite(), actual.isFavorite());
            assertEquals("Cache " + geocode + ": watchlist status wrong", expected.isOnWatchlist(), actual.isOnWatchlist());

            for (String attribute : expected.getAttributes()) {
                assertTrue("Expected attribute '" + attribute + "' not found in " + actual.getGeocode(), actual.getAttributes().contains(attribute));
            }
            for (LogType logType : expected.getLogCounts().keySet()) {
                assertTrue("Cache " + geocode + ": logcount for type " + logType.toString() + " wrong", actual.getLogCounts().get(logType) >= expected.getLogCounts().get(logType));
            }

            // The inventories can differ too often, therefore we don't compare them. Also, the personal note
            // cannot be expected to match with different tester accounts.

            final int actualSpoilersSize = null != actual.getSpoilers() ? actual.getSpoilers().size() : 0;
            final int expectedSpoilersSize = null != expected.getSpoilers() ? expected.getSpoilers().size() : 0;
            assertEquals("Cache " + geocode + ": spoiler count wrong", expectedSpoilersSize, actualSpoilersSize);
        }
    }

}
