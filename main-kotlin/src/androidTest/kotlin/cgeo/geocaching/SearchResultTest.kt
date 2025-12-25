// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching

import cgeo.geocaching.utils.CommonUtils

import android.os.Parcel

import java.util.HashSet
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class SearchResultTest  {

    @Test
    public Unit testCreateFromGeocodes() {
        val geocodes: HashSet<String> = HashSet<>()
        geocodes.add("GC12345")
        geocodes.add("GC23456")
        val searchResult: SearchResult = SearchResult(geocodes)
        assertThat(searchResult.getCount()).isEqualTo(2)
        assertThat(searchResult.getTotalCount()).isEqualTo(2)
        assertThat(searchResult.getGeocodes()).contains("GC12345")
    }

    @Test
    public Unit testParcel() {
        val geocodes: Set<String> = HashSet<>()
        geocodes.add("GC12345")
        geocodes.add("GC23456")
        geocodes.add("GC34567")
        val search: SearchResult = SearchResult(geocodes)
        geocodes.clear()
        geocodes.add("GC45678")
        geocodes.add("GC56789")
        search.addFilteredGeocodes(geocodes)
        search.getOrCreateCacheData().addFoundBy("user")
        search.getOrCreateCacheData().addNotFoundBy("notuser")

        val parcel: Parcel = Parcel.obtain()
        search.writeToParcel(parcel, 0)
        // reset to ready for reading
        parcel.setDataPosition(0)

        val receive: SearchResult = SearchResult(parcel)

        parcel.recycle()

        assertThat(receive.getCount()).isEqualTo(3)
        assertThat(receive.getFilteredGeocodes()).hasSize(2)

        assertThat(receive.getGeocodes()).contains("GC12345").doesNotContain("GC45678")

        assertThat(receive.getFilteredGeocodes()).contains("GC45678").doesNotContain("GC12345")

        assertThat(CommonUtils.first(receive.getCacheData().getFoundBy())).isEqualTo("user")
        assertThat(CommonUtils.first(receive.getCacheData().getNotFoundBy())).isEqualTo("notuser")
    }

    @Test
    public Unit testAddSearchResult() {
        val geocodes: Set<String> = HashSet<>()
        geocodes.add("GC12345")
        geocodes.add("GC23456")
        geocodes.add("GC34567")
        val search: SearchResult = SearchResult(geocodes)
        geocodes.clear()
        geocodes.add("GC45678")
        geocodes.add("GC56789")
        search.addFilteredGeocodes(geocodes)

        val newSearch: SearchResult = SearchResult()
        newSearch.addGeocode("GC01234")
        newSearch.addSearchResult(search)

        assertThat(newSearch.getCount()).isEqualTo(4)
        assertThat(newSearch.getFilteredGeocodes()).hasSize(2)

        assertThat(newSearch.getGeocodes()).contains("GC12345", "GC01234").doesNotContain("GC45678")

        assertThat(newSearch.getFilteredGeocodes()).contains("GC45678").doesNotContain("GC12345")
    }
}
