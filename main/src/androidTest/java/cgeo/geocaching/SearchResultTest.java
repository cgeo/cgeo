package cgeo.geocaching;

import android.os.Parcel;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class SearchResultTest  {

    @Test
    public void testCreateFromGeocodes() {
        final HashSet<String> geocodes = new HashSet<>();
        geocodes.add("GC12345");
        geocodes.add("GC23456");
        final SearchResult searchResult = new SearchResult(geocodes);
        assertThat(searchResult.getCount()).isEqualTo(2);
        assertThat(searchResult.getTotalCount()).isEqualTo(2);
        assertThat(searchResult.getGeocodes()).contains("GC12345");
    }

    @Test
    public void testParcel() {
        final Set<String> geocodes = new HashSet<>();
        geocodes.add("GC12345");
        geocodes.add("GC23456");
        geocodes.add("GC34567");
        final SearchResult search = new SearchResult(geocodes);
        geocodes.clear();
        geocodes.add("GC45678");
        geocodes.add("GC56789");
        search.addFilteredGeocodes(geocodes);
        search.setFinder("test");

        final Parcel parcel = Parcel.obtain();
        search.writeToParcel(parcel, 0);
        // reset to ready for reading
        parcel.setDataPosition(0);

        final SearchResult receive = new SearchResult(parcel);

        parcel.recycle();

        assertThat(receive.getCount()).isEqualTo(3);
        assertThat(receive.getFilteredGeocodes()).hasSize(2);

        assertThat(receive.getGeocodes()).contains("GC12345").doesNotContain("GC45678");

        assertThat(receive.getFilteredGeocodes()).contains("GC45678").doesNotContain("GC12345");

        assertThat(receive.getFinder()).isEqualTo("test");
    }

    @Test
    public void testAddSearchResult() {
        final Set<String> geocodes = new HashSet<>();
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

        assertThat(newSearch.getCount()).isEqualTo(4);
        assertThat(newSearch.getFilteredGeocodes()).hasSize(2);

        assertThat(newSearch.getGeocodes()).contains("GC12345", "GC01234").doesNotContain("GC45678");

        assertThat(newSearch.getFilteredGeocodes()).contains("GC45678").doesNotContain("GC12345");
    }
}
