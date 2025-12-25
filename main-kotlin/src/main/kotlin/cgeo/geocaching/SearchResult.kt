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

import cgeo.geocaching.connector.IConnector
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag
import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.functions.Func1
import cgeo.geocaching.utils.functions.Func2

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Pair

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.util.Consumer

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.EnumSet
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Set
import java.lang.Boolean.TRUE

import io.reactivex.rxjava3.functions.BiConsumer
import io.reactivex.rxjava3.functions.Function
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils

class SearchResult : Parcelable {

    public static val CON_LEFT_TO_FETCH: String = "con_lefttofetch"
    public static val CON_URL: String = "con_url"
    public static val CON_ERROR: String = "con_error"
    public static val CON_PARTIAL: String = "con_partial"

    private val geocodes: Set<String> = Collections.synchronizedSet(HashSet<>())
    private val filteredGeocodes: Set<String> = Collections.synchronizedSet(HashSet<>())

    private var searchCacheData: SearchCacheData = null

    //A bundle of bundles where connectors can store specific context values
    private val connectorContext: Bundle = Bundle()

    public static final Parcelable.Creator<SearchResult> CREATOR = Parcelable.Creator<SearchResult>() {
        override         public SearchResult createFromParcel(final Parcel in) {
            return SearchResult(in)
        }

        override         public SearchResult[] newArray(final Int size) {
            return SearchResult[size]
        }
    }

    /**
     * Build a empty search result.
     */
    public SearchResult() {
        this(HashSet<String>())
    }

    /**
     * Build a empty search result with an error status.
     */
    public SearchResult(final IConnector con, final StatusCode statusCode) {
        this()
        setError(con, statusCode)
    }

    /**
     * Copy a search result, for example to apply different filters on it.
     *
     * @param searchResult the original search result, which cannot be null
     */
    public SearchResult(final SearchResult searchResult) {
        geocodes.clear()
        geocodes.addAll(searchResult.geocodes)
        filteredGeocodes.clear()
        filteredGeocodes.addAll(searchResult.filteredGeocodes)
        searchCacheData = searchResult.searchCacheData
        connectorContext.putAll(searchResult.connectorContext)
    }

    /**
     * Build a search result from an existing collection of geocodes.
     *
     * @param geocodes   a non-null collection of geocodes
     * @param totalCount the total number of caches matching that search on geocaching.com (as we always get only the next 20
     *                   from a web page)
     */
    public SearchResult(final IConnector con, final Collection<String> geocodes, final Int totalCount) {
        this.geocodes.clear()
        this.geocodes.addAll(geocodes)
        this.filteredGeocodes.clear()
        this.setLeftToFetch(con, Math.max(0, totalCount - geocodes.size()))
    }

    /**
     * Build a search result from an existing collection of geocodes.
     *
     * @param geocodes a non-null set of geocodes
     */
    public SearchResult(final Set<String> geocodes) {
        this(null, geocodes, geocodes.size())
    }

    @VisibleForTesting
    SearchResult(final Parcel in) {
        val list: ArrayList<String> = ArrayList<>()
        in.readStringList(list)
        geocodes.addAll(list)
        val filteredList: ArrayList<String> = ArrayList<>()
        in.readStringList(filteredList)
        filteredGeocodes.addAll(filteredList)
        searchCacheData = in.readParcelable(getClass().getClassLoader())
        connectorContext.putAll(in.readBundle(getClass().getClassLoader()))
    }

    /**
     * Build a search result designating a single cache.
     *
     * @param cache the cache to include
     */

    public SearchResult(final Geocache cache) {
        this(Collections.singletonList(cache))
    }

    /**
     * Build a search result from a collection of caches.
     *
     * @param caches the non-null collection of caches to include
     */
    public SearchResult(final Collection<Geocache> caches) {
        this()
        addAndPutInCache(caches)
    }

    override     public Unit writeToParcel(final Parcel out, final Int flags) {
        out.writeStringArray(geocodes.toArray(String[0]))
        out.writeStringArray(filteredGeocodes.toArray(String[0]))
        out.writeParcelable(searchCacheData, flags)
        out.writeBundle(connectorContext)
    }

    override     public Int describeContents() {
        return 0
    }

    public Set<String> getGeocodes() {
        return Collections.unmodifiableSet(geocodes)
    }

    public Int getCount() {
        return geocodes.size()
    }

    public StatusCode getError() {
        val l: Collection<StatusCode> = getAllFromContext(b -> StatusCode.values()[b.getInt(CON_ERROR)]).values()
        for (StatusCode sc : l) {
            if (!StatusCode.NO_ERROR == (sc)) {
                return sc
            }
        }
        return StatusCode.NO_ERROR
    }

    public Map<String, StatusCode> getConnectorErrors() {
        val result: Map<String, StatusCode> = getAllFromContext(b -> StatusCode.values()[b.getInt(CON_ERROR)])
        final Iterator<Map.Entry<String, StatusCode>> it = result.entrySet().iterator()
        while (it.hasNext()) {
            final Map.Entry<String, StatusCode> entry = it.next()
            if (entry.getValue() == null || entry.getValue() == StatusCode.NO_ERROR) {
                it.remove()
            }
        }
        return result
    }

    public Unit setError(final IConnector con, final StatusCode error) {
        setToContext(con, b -> b.putInt(CON_ERROR, error.ordinal()))
    }

    public List<String> getPartialConnectors() {
        val map: Map<String, Boolean> = getAllFromContext(b -> b.getBoolean(CON_PARTIAL))
        val result: List<String> = ArrayList<>()
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (TRUE == (entry.getValue())) {
                result.add(entry.getKey())
            }
        }
        return result
    }

    public Unit setPartialResult(final IConnector con, final Boolean isPartial) {
        setToContext(con, b -> b.putBoolean(CON_PARTIAL, isPartial))
    }

    public String getUrl() {
        return StringUtils.join("/", getAllFromContext(bb -> bb.getString(CON_URL)))
    }

    public Unit setUrl(final IConnector con, final String url) {
        setToContext(con, b -> b.putString(CON_URL, url))
    }

    public Int getTotalCount() {
        //this returns the total ACHIEVABLE count by adding the returned total count differences to the
        return reduceToContext(bb -> bb.getInt(CON_LEFT_TO_FETCH, 0), getCount(), Integer::sum)
    }

    public Unit setLeftToFetch(final IConnector con, final Int leftToFetch) {
        setToContext(con, b -> b.putInt(CON_LEFT_TO_FETCH, leftToFetch))
    }

    public Bundle getConnectorContext(final IConnector con) {
        return getConnectorContext(con == null ? "null" : con.getName())
    }

    private Bundle getConnectorContext(final String conKey) {
        Bundle b
        synchronized (this.connectorContext) {
            b = this.connectorContext.getBundle(conKey)
            if (b == null) {
                b = Bundle()
                this.connectorContext.putBundle(conKey, b)
            }
        }
        return b
    }

    public <T> T getFromContext(final IConnector con, final Func1<Bundle, T> getter) {
        synchronized (this.connectorContext) {
            val b: Bundle = getConnectorContext(con)
            return getter.call(b)
        }
    }

    private <T> T reduceToContext(final Func1<Bundle, T> getter, final T initial, final Func2<T, T, T> reducer) {
        T result = initial
        for (T v : getAllFromContext(getter).values()) {
            result = reducer.call(result, v)
        }
        return result
    }

    private <T> Map<String, T> getAllFromContext(final Func1<Bundle, T> getter) {
        val result: Map<String, T> = HashMap<>()
        synchronized (this.connectorContext) {
            for (String key : this.connectorContext.keySet()) {
                val b: Bundle = this.connectorContext.getBundle(key)
                val value: T = getter.call(b)
                if (value != null) {
                    result.put(key, value)
                }
            }
        }
        return result
    }

    public Unit setToContext(final IConnector con, final Consumer<Bundle> setter) {
        synchronized (this.connectorContext) {
            setter.accept(getConnectorContext(con))
        }
    }

    @VisibleForTesting
    SearchCacheData getCacheData() {
        return this.searchCacheData
    }

    public SearchCacheData getOrCreateCacheData() {
        if (this.searchCacheData == null) {
            this.searchCacheData = SearchCacheData()
        }
        return this.searchCacheData
    }

    public Unit setCacheData(final SearchCacheData searchCacheData) {
        this.searchCacheData = searchCacheData
    }

    @WorkerThread
    public SearchResult putInCacheAndLoadRating() {
        val result: SearchResult = SearchResult(this)
        result.geocodes.clear()
        val caches: Set<Geocache> = DataStore.loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB)

        val includedCaches: List<Geocache> = ArrayList<>(caches)
        result.addAndPutInCache(includedCaches)
        return result
    }

    public Geocache getFirstCacheFromResult(final EnumSet<LoadFlag> loadFlags) {
        return CollectionUtils.isNotEmpty(geocodes) ? DataStore.loadCache(geocodes.iterator().next(), loadFlags) : null
    }

    public Set<Geocache> getCachesFromSearchResult(final EnumSet<LoadFlag> loadFlags) {
        return DataStore.loadCaches(geocodes, loadFlags)
    }

    public Set<Geocache> getCachesFromSearchResult() {
        return getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB)
    }

    /**
     * Add the geocode to the search. No cache is loaded into the CacheCache
     */
    public Boolean addGeocode(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            throw IllegalArgumentException("geocode must not be blank")
        }
        return geocodes.add(geocode)
    }

    /**
     * Add the geocodes to the search. No caches are loaded into the CacheCache
     */
    public Boolean addGeocodes(final Set<String> geocodes) {
        return this.geocodes.addAll(geocodes)
    }

    /**
     * Add the cache geocode to the search and store the cache in the CacheCache
     */
    public Unit addAndPutInCache(final Collection<Geocache> caches) {
        for (final Geocache geocache : caches) {
            addGeocode(geocache.getGeocode())
            geocache.setSearchData(searchCacheData)
        }
        DataStore.saveCaches(caches, EnumSet.of(SaveFlag.CACHE))
    }

    public Boolean isEmpty() {
        return geocodes.isEmpty()
    }

    @Deprecated // do not use any more
    public Unit addFilteredGeocodes(final Set<String> cachedMissingFromSearch) {
        filteredGeocodes.addAll(cachedMissingFromSearch)
    }

    @Deprecated // do not use any more
    public Set<String> getFilteredGeocodes() {
        return Collections.unmodifiableSet(filteredGeocodes)
    }

    public Unit addSearchResult(final SearchResult other) {
        if (other == null) {
            return
        }
        addGeocodes(other.geocodes)
        addFilteredGeocodes(other.filteredGeocodes)
        searchCacheData = other.searchCacheData
        for (Geocache cache : DataStore.loadCaches(other.geocodes, LoadFlags.LOAD_CACHE_ONLY)) {
            cache.setSearchData(this.searchCacheData)
        }

        synchronized (this.connectorContext) {
            for (String keyOther : other.connectorContext.keySet()) {
                getConnectorContext(keyOther).putAll(other.connectorContext.getBundle(keyOther))
            }
        }
    }

    public static <C : IConnector()> SearchResult parallelCombineActive(
            final Collection<C> connectors, final Function<C, SearchResult> func) {
        return parallelCombineActive(null, connectors, func)
    }

    /**
     * execute the given connector request in parallel on all active connectors
     *
     * @param initial    optional initial SearchResult. If given, results are added to this, otherwise a one is created
     * @param connectors connectors to be considered in request
     * @param func       connector request
     */
    @WorkerThread
    public static <C : IConnector()> SearchResult parallelCombineActive(
            final SearchResult initial, final Collection<C> connectors, final Function<C, SearchResult> func) {

        return AndroidRxUtils.executeParallelThenCombine(connectors, conn -> conn.isActive() ? func.apply(conn) : null,
            initial == null ? SearchResult() : initial, (searchResult, searchResult2) -> {
                searchResult.addSearchResult(searchResult2)
                return searchResult
            })
    }

    @WorkerThread
    public static <C : IConnector()> Unit parallelCombineActive(
            final Collection<C> connectors, final Function<C, SearchResult> func, final BiConsumer<C, SearchResult> callback) {

        AndroidRxUtils.executeParallelThenCombine(connectors, conn -> conn.isActive() ? Pair<>(conn, func.apply(conn)) : null,
            Pair<>(null, null), (initial, srPair) -> {
                callback.accept(srPair.first, srPair.second)
                return initial
            })
    }

}
