package cgeo.geocaching;

import android.os.Parcel;
import android.test.AndroidTestCase;

import java.util.HashSet;
import java.util.Set;

public class SearchResultTest extends AndroidTestCase {
    public static void testCreateFromGeocodes() {
        final HashSet<String> geocodes = new HashSet<String>();
        geocodes.add("GC12345");
        geocodes.add("GC23456");
        final SearchResult searchResult = new SearchResult(geocodes);
        assertEquals(2, searchResult.getCount());
        assertEquals(2, searchResult.getTotal());
        assertTrue(searchResult.getGeocodes().contains("GC12345"));
    }

    public static void testParcel() {
        final Set<String> geocodes = new HashSet<String>();
        geocodes.add("GC12345");
        geocodes.add("GC23456");
        geocodes.add("GC34567");
        final SearchResult search = new SearchResult(geocodes);
        geocodes.clear();
        geocodes.add("GC45678");
        geocodes.add("GC56789");
        search.addFilteredGeocodes(geocodes);

        Parcel parcel = Parcel.obtain();
        search.writeToParcel(parcel, 0);
        // reset to ready for reading
        parcel.setDataPosition(0);

        final SearchResult receive = new SearchResult(parcel);

        parcel.recycle();

        assertEquals(3, receive.getCount());
        assertEquals(2, receive.getFilteredGeocodes().size());

        assertTrue(receive.getGeocodes().contains("GC12345"));
        assertFalse(receive.getGeocodes().contains("GC45678"));

        assertFalse(receive.getFilteredGeocodes().contains("GC12345"));
        assertTrue(receive.getFilteredGeocodes().contains("GC45678"));
    }

    public static void testAddSearchResult() {
        final Set<String> geocodes = new HashSet<String>();
        geocodes.add("GC12345");
        geocodes.add("GC23456");
        geocodes.add("GC34567");
        final SearchResult search = new SearchResult(geocodes);
        geocodes.clear();
        geocodes.add("GC45678");
        geocodes.add("GC56789");
        search.addFilteredGeocodes(geocodes);

        final SearchResult newSearch = new SearchResult();
        newSearch.addGeocode("GC01234");
        newSearch.addSearchResult(search);

        assertEquals(4, newSearch.getCount());
        assertEquals(2, newSearch.getFilteredGeocodes().size());

        assertTrue(newSearch.getGeocodes().contains("GC12345"));
        assertTrue(newSearch.getGeocodes().contains("GC01234"));
        assertFalse(newSearch.getGeocodes().contains("GC45678"));

        assertFalse(newSearch.getFilteredGeocodes().contains("GC12345"));
        assertTrue(newSearch.getFilteredGeocodes().contains("GC45678"));
    }
}
