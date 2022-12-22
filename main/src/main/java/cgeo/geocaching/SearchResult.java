package cgeo.geocaching;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class SearchResult implements Parcelable {

    public static final String CON_LEFT_TO_FETCH = "con_lefttofetch";
    public static final String CON_URL = "con_url";
    public static final String CON_ERROR = "con_error";

    private final Set<String> geocodes = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> filteredGeocodes = Collections.synchronizedSet(new HashSet<>());

    private String finder = null;

    //A bundle of bundles where connectors can store specific context values
    private final Bundle connectorContext = new Bundle();


    public static final Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
        @Override
        public SearchResult createFromParcel(final Parcel in) {
            return new SearchResult(in);
        }

        @Override
        public SearchResult[] newArray(final int size) {
            return new SearchResult[size];
        }
    };

    /**
     * Build a new empty search result.
     */
    public SearchResult() {
        this(new HashSet<String>());
    }

    /**
     * Build a new empty search result with an error status.
     */
    public SearchResult(@NonNull final IConnector con, @NonNull final StatusCode statusCode) {
        this();
        setError(con, statusCode);
    }

    /**
     * Copy a search result, for example to apply different filters on it.
     *
     * @param searchResult the original search result, which cannot be null
     */
    public SearchResult(final SearchResult searchResult) {
        geocodes.clear();
        geocodes.addAll(searchResult.geocodes);
        filteredGeocodes.clear();
        filteredGeocodes.addAll(searchResult.filteredGeocodes);
        finder = searchResult.finder;
        connectorContext.putAll(searchResult.connectorContext);
    }

    /**
     * Build a search result from an existing collection of geocodes.
     *
     * @param geocodes   a non-null collection of geocodes
     * @param totalCount the total number of caches matching that search on geocaching.com (as we always get only the next 20
     *                   from a web page)
     */
    public SearchResult(final IConnector con, final Collection<String> geocodes, final int totalCount) {
        this.geocodes.clear();
        this.geocodes.addAll(geocodes);
        this.filteredGeocodes.clear();
        this.setLeftToFetch(con, Math.max(0, totalCount - geocodes.size()));
    }

    /**
     * Build a search result from an existing collection of geocodes.
     *
     * @param geocodes a non-null set of geocodes
     */
    public SearchResult(final Set<String> geocodes) {
        this(null, geocodes, geocodes.size());
    }

    @VisibleForTesting
    SearchResult(final Parcel in) {
        final ArrayList<String> list = new ArrayList<>();
        in.readStringList(list);
        geocodes.addAll(list);
        final ArrayList<String> filteredList = new ArrayList<>();
        in.readStringList(filteredList);
        filteredGeocodes.addAll(filteredList);
        finder = in.readString();
        connectorContext.putAll(in.readBundle(getClass().getClassLoader()));
    }

    /**
     * Build a search result designating a single cache.
     *
     * @param cache the cache to include
     */

    public SearchResult(final Geocache cache) {
        this(Collections.singletonList(cache));
    }

    /**
     * Build a search result from a collection of caches.
     *
     * @param caches the non-null collection of caches to include
     */
    public SearchResult(@NonNull final Collection<Geocache> caches) {
        this();
        addAndPutInCache(caches);
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeStringArray(geocodes.toArray(new String[0]));
        out.writeStringArray(filteredGeocodes.toArray(new String[0]));
        out.writeString(finder);
        out.writeBundle(connectorContext);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public Set<String> getGeocodes() {
        return Collections.unmodifiableSet(geocodes);
    }

    public int getCount() {
        return geocodes.size();
    }

    @NonNull
    public StatusCode getError() {
        final List<StatusCode> l = getAllFromContext(b -> StatusCode.values()[b.getInt(CON_ERROR)]);
        for (StatusCode sc : l) {
            if (!StatusCode.NO_ERROR.equals(sc)) {
                return sc;
            }
        }
        return StatusCode.NO_ERROR;
    }

    public void setError(final IConnector con, @NonNull final StatusCode error) {
        setToContext(con, b -> b.putInt(CON_ERROR, error.ordinal()));
    }

    public String getUrl() {
        return StringUtils.join("/", getAllFromContext(bb -> bb.getString(CON_URL)));
    }

    public void setUrl(final IConnector con, final String url) {
        setToContext(con, b -> b.putString(CON_URL, url));
    }

    public int getTotalCount() {
        //this returns the total ACHIEVABLE count by adding the returned total count differences to the
        return reduceToContext(bb -> bb.getInt(CON_LEFT_TO_FETCH, 0), getCount(), (i1, i2) -> i1 + i2);
    }

    public void setLeftToFetch(final IConnector con, final int leftToFetch) {
        setToContext(con, b -> b.putInt(CON_LEFT_TO_FETCH, leftToFetch));
    }

    public Bundle getConnectorContext(@Nullable final IConnector con) {
        return getConnectorContext(con == null ? "null" : con.getName());
    }

    private Bundle getConnectorContext(@NonNull final String conKey) {
        Bundle b;
        synchronized (this.connectorContext) {
            b = this.connectorContext.getBundle(conKey);
            if (b == null) {
                b = new Bundle();
                this.connectorContext.putBundle(conKey, b);
            }
        }
        return b;
    }

    public <T> T getFromContext(@Nullable final IConnector con, final Func1<Bundle, T> getter) {
        synchronized (this.connectorContext) {
            final Bundle b = getConnectorContext(con);
            return getter.call(b);
        }
    }

    private <T> T reduceToContext(final Func1<Bundle, T> getter, final T initial, final Func2<T, T, T> reducer) {
        T result = initial;
        for (T v : getAllFromContext(getter)) {
            result = reducer.call(result, v);
        }
        return result;
    }

    private <T> List<T> getAllFromContext(final Func1<Bundle, T> getter) {
        final List<T> result = new ArrayList<>();
        synchronized (this.connectorContext) {
            for (String key : this.connectorContext.keySet()) {
                final Bundle b = this.connectorContext.getBundle(key);
                final T value = getter.call(b);
                if (value != null) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    public void setToContext(@NonNull final IConnector con, final Consumer<Bundle> setter) {
        synchronized (this.connectorContext) {
            setter.accept(getConnectorContext(con));
        }
    }

    @VisibleForTesting
    @Nullable
    String getFinder() {
        return this.finder;
    }

    public void setFinder(@NonNull final String finder) {
        this.finder = finder;
    }

    @WorkerThread
    public SearchResult putInCacheAndLoadRating() {
        final SearchResult result = new SearchResult(this);
        result.geocodes.clear();
        final Set<Geocache> caches = DataStore.loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB);

        final List<Geocache> includedCaches = new ArrayList<>(caches);
        result.addAndPutInCache(includedCaches);

        GCVote.loadRatings(includedCaches);
        return result;
    }

    @Nullable
    public Geocache getFirstCacheFromResult(final EnumSet<LoadFlag> loadFlags) {
        return CollectionUtils.isNotEmpty(geocodes) ? DataStore.loadCache(geocodes.iterator().next(), loadFlags) : null;
    }

    public Set<Geocache> getCachesFromSearchResult(final EnumSet<LoadFlag> loadFlags) {
        return DataStore.loadCaches(geocodes, loadFlags);
    }

    /**
     * Add the geocode to the search. No cache is loaded into the CacheCache
     */
    public boolean addGeocode(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            throw new IllegalArgumentException("geocode must not be blank");
        }
        return geocodes.add(geocode);
    }

    /**
     * Add the geocodes to the search. No caches are loaded into the CacheCache
     */
    public boolean addGeocodes(final Set<String> geocodes) {
        return this.geocodes.addAll(geocodes);
    }

    /**
     * Add the cache geocode to the search and store the cache in the CacheCache
     */
    public void addAndPutInCache(@NonNull final Collection<Geocache> caches) {
        for (final Geocache geocache : caches) {
            addGeocode(geocache.getGeocode());
            geocache.setSearchFinder(finder);
        }
        DataStore.saveCaches(caches, EnumSet.of(SaveFlag.CACHE));
    }

    public boolean isEmpty() {
        return geocodes.isEmpty();
    }

    public boolean hasUnsavedCaches() {
        for (final String geocode : getGeocodes()) {
            if (!DataStore.isOffline(geocode, null)) {
                return true;
            }
        }
        return false;
    }

    public void addFilteredGeocodes(final Set<String> cachedMissingFromSearch) {
        filteredGeocodes.addAll(cachedMissingFromSearch);
    }

    public Set<String> getFilteredGeocodes() {
        return Collections.unmodifiableSet(filteredGeocodes);
    }

    public void addSearchResult(final SearchResult other) {
        if (other == null) {
            return;
        }
        addGeocodes(other.geocodes);
        addFilteredGeocodes(other.filteredGeocodes);
        finder = other.finder;
        for (Geocache cache : DataStore.loadCaches(other.geocodes, LoadFlags.LOAD_CACHE_ONLY)) {
            cache.setSearchFinder(this.finder);
        }


        synchronized (this.connectorContext) {
            for (String keyOther : other.connectorContext.keySet()) {
                getConnectorContext(keyOther).putAll(other.connectorContext.getBundle(keyOther));
            }
        }
    }

    public static <C extends IConnector> SearchResult parallelCombineActive(
            final Collection<C> connectors, final Function<C, SearchResult> func) {
        return parallelCombineActive(null, connectors, func);
    }

    /**
     * execute the given connector request in parallel on all active connectors
     *
     * @param initial    optional initial SearchResult. If given, new results are added to this, otherwise a new one is created
     * @param connectors connectors to be considered in request
     * @param func       connector request
     */
    @WorkerThread
    public static <C extends IConnector> SearchResult parallelCombineActive(
            @Nullable final SearchResult initial, final Collection<C> connectors, final Function<C, SearchResult> func) {
        return Observable.fromIterable(connectors).flatMapMaybe((Function<C, Maybe<SearchResult>>) connector -> {
            if (!connector.isActive()) {
                return Maybe.empty();
            }
            return Maybe.fromCallable(() -> {
                try {
                    return func.apply(connector);
                } catch (final Throwable t) {
                    Log.w("parallelCombineActive: swallowing error from connector " + connector, t);
                    return null;
                }
            }).subscribeOn(AndroidRxUtils.networkScheduler);
        }).reduce(initial == null ? new SearchResult() : initial, (searchResult, searchResult2) -> {
            searchResult.addSearchResult(searchResult2);
            return searchResult;
        }).blockingGet();
    }

}
