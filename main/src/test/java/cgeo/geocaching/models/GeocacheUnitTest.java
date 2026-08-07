package cgeo.geocaching.models;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.location.Geopoint;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeocacheUnitTest {

    @Test
    public void testGatherMissingFromNameIsGeocode() {
        // Test for issue #17797: When importing route GPX, cache name should not be overwritten with geocode
        final Geocache existing = new Geocache();
        existing.setGeocode("GC12345");
        existing.setName("My Awesome Cache");
        existing.setDetailed(true);
        existing.setCoords(new Geopoint(40.0, 8.0));
        existing.setType(CacheType.TRADITIONAL);

        // Simulate a cache imported from a route GPX where the name field contains the geocode
        final Geocache imported = new Geocache();
        imported.setGeocode("GC12345");
        imported.setName("GC12345"); // Name is set to geocode (the problem)
        imported.setCoords(new Geopoint(40.0, 8.0));

        imported.gatherMissingFrom(existing);

        // The name should be taken from the existing cache, not kept as the geocode
        assertThat(imported.getName()).as("name should not be geocode").isEqualTo("My Awesome Cache");
    }

    @Test
    public void testGatherMissingFromNameIsGeocodeButDifferentCase() {
        // Test case insensitive comparison
        final Geocache existing = new Geocache();
        existing.setGeocode("GC12345");
        existing.setName("Another Great Cache");
        existing.setDetailed(true);

        final Geocache imported = new Geocache();
        imported.setGeocode("GC12345");
        imported.setName("gc12345"); // lowercase geocode

        imported.gatherMissingFrom(existing);

        assertThat(imported.getName()).as("name should be case-insensitive").isEqualTo("Another Great Cache");
    }

    @Test
    public void testGatherMissingFromNameIsProperName() {
        // Test that a proper name is not overwritten
        final Geocache existing = new Geocache();
        existing.setGeocode("GC12345");
        existing.setName("Old Cache Name");
        existing.setDetailed(true);

        final Geocache imported = new Geocache();
        imported.setGeocode("GC12345");
        imported.setName("New Cache Name"); // Proper name, not geocode

        imported.gatherMissingFrom(existing);

        // The new name should be kept as it's a proper name, not a geocode
        assertThat(imported.getName()).as("proper name should be kept").isEqualTo("New Cache Name");
    }

    @Test
    public void testGatherMissingFromBlankName() {
        // Test that blank names are still handled correctly
        final Geocache existing = new Geocache();
        existing.setGeocode("GC12345");
        existing.setName("Existing Cache Name");
        existing.setDetailed(true);

        final Geocache imported = new Geocache();
        imported.setGeocode("GC12345");
        imported.setName(""); // Blank name

        imported.gatherMissingFrom(existing);

        assertThat(imported.getName()).as("blank name should use existing").isEqualTo("Existing Cache Name");
    }
}
