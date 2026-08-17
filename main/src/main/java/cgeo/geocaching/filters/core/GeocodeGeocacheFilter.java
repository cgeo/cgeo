package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;

public class GeocodeGeocacheFilter extends StringGeocacheFilter {

    public static GeocodeGeocacheFilter create(final String text) {
        return GeocodeGeocacheFilter.create(text, false, StringFilter.StringFilterType.CONTAINS);
    }

    public static GeocodeGeocacheFilter create(final String text, final boolean matchCase, final StringFilter.StringFilterType stringFilterType) {
        // there is no GeocacheFilterType for geocode, so we cannot use the generic StringGeocacheFilter.create() method here
        //  return StringGeocacheFilter.create(GeocacheFilterType.GEOCODE, text, matchCase, filterType);
        final GeocodeGeocacheFilter geocodeFilter = new GeocodeGeocacheFilter();
        final StringFilter sf = geocodeFilter.getStringFilter();
        sf.setTextValue(text);
        sf.setMatchCase(matchCase);
        sf.setFilterType(stringFilterType);
        return geocodeFilter;
    }

    public String getValue(final Geocache cache) {
        return cache.getGeocode();
    }

    protected String getSqlColumnName() {
        return "geocode";
    }

}
