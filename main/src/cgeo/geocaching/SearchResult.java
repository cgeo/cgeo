package cgeo.geocaching;

import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.gcvote.GCVote;

import org.apache.commons.lang3.StringUtils;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class SearchResult implements Parcelable {

    final private Set<String> geocodes;
    private StatusCode error = null;
    private String url = "";
    public String[] viewstates = null;
    private int totalCnt = 0;

    final public static Parcelable.Creator<SearchResult> CREATOR = new Parcelable.Creator<SearchResult>() {
        public SearchResult createFromParcel(Parcel in) {
            return new SearchResult(in);
        }

        public SearchResult[] newArray(int size) {
            return new SearchResult[size];
        }
    };

    public SearchResult() {
        this((Set<String>) null);
    }

    public SearchResult(SearchResult searchResult) {
        if (searchResult != null) {
            this.geocodes = new HashSet<String>(searchResult.geocodes);
            this.error = searchResult.error;
            this.url = searchResult.url;
            this.viewstates = searchResult.viewstates;
            this.setTotal(searchResult.getTotal());
        } else {
            this.geocodes = new HashSet<String>();
        }
    }

    public SearchResult(final Set<String> geocodes, final int total) {
        if (geocodes == null) {
            this.geocodes = new HashSet<String>();
        } else {
            this.geocodes = new HashSet<String>(geocodes.size());
            this.geocodes.addAll(geocodes);
        }
        this.setTotal(total);
    }

    public SearchResult(final Set<String> geocodes) {
        this(geocodes, geocodes == null ? 0 : geocodes.size());
    }

    public SearchResult(final Parcel in) {
        ArrayList<String> list = new ArrayList<String>();
        in.readStringList(list);
        geocodes = new HashSet<String>(list);
        error = (StatusCode) in.readSerializable();
        url = in.readString();
        final int length = in.readInt();
        if (length >= 0) {
            viewstates = new String[length];
            in.readStringArray(viewstates);
        }
        setTotal(in.readInt());
    }

    public SearchResult(cgCache cache) {
        this();
        addCache(cache);
    }

    public SearchResult(Collection<cgCache> caches) {
        this();
        for (cgCache cache : caches) {
            addCache(cache);
        }
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags) {
        out.writeStringArray(geocodes.toArray(new String[geocodes.size()]));
        out.writeSerializable(error);
        out.writeString(url);
        if (viewstates == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(viewstates.length);
            out.writeStringArray(viewstates);
        }
        out.writeInt(getTotal());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Set<String> getGeocodes() {
        return Collections.unmodifiableSet(geocodes);
    }

    public int getCount() {
        return geocodes.size();
    }

    public StatusCode getError() {
        return error;
    }

    public void setError(final StatusCode error) {
        this.error = error;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String[] getViewstates() {
        return viewstates;
    }

    public void setViewstates(String[] viewstates) {
        if (Login.isEmpty(viewstates)) {
            return;
        }

        this.viewstates = viewstates;
    }

    public int getTotal() {
        return totalCnt;
    }

    public void setTotal(int totalCnt) {
        this.totalCnt = totalCnt;
    }

    /**
     * @param excludeDisabled
     * @param excludeMine
     * @param cacheType
     * @return
     */
    public SearchResult filterSearchResults(final boolean excludeDisabled, final boolean excludeMine, final CacheType cacheType) {

        SearchResult result = new SearchResult(this);
        result.geocodes.clear();
        final ArrayList<cgCache> cachesForVote = new ArrayList<cgCache>();

        final Set<cgCache> caches = cgeoapplication.getInstance().loadCaches(geocodes, LoadFlags.LOAD_CACHE_OR_DB);
        for (cgCache cache : caches) {
            // Is there any reason to exclude the cache from the list?
            final boolean excludeCache = (excludeDisabled && cache.isDisabled()) ||
                    (excludeMine && (cache.isOwn() || cache.isFound())) ||
                    (cacheType != CacheType.ALL && cacheType != cache.getType());
            if (!excludeCache) {
                result.addCache(cache);
                cachesForVote.add(cache);
            }
        }
        GCVote.loadRatings(cachesForVote);
        return result;
    }

    public cgCache getFirstCacheFromResult(final EnumSet<LoadFlag> loadFlags) {
        if (geocodes != null && geocodes.size() >= 1) {
            return cgeoapplication.getInstance().loadCache((String) geocodes.toArray()[0], loadFlags);
        }
        return null;
    }

    public Set<cgCache> getCachesFromSearchResult(final EnumSet<LoadFlag> loadFlags) {
        return cgeoapplication.getInstance().loadCaches(geocodes, loadFlags);
    }

    /** Add the geocode to the search. No cache is loaded into the CacheCache */
    public boolean addGeocode(final String geocode) {
        if (StringUtils.isBlank(geocode)) {
            throw new IllegalArgumentException("geocode must not be blank");
        }
        return geocodes.add(geocode);
    }

    /** Add the geocodes to the search. No caches are loaded into the CacheCache */
    public boolean addGeocodes(Set<String> geocodes) {
        return this.geocodes.addAll(geocodes);
    }

    /** Add the cache geocode to the search and store the cache in the CacheCache */
    public boolean addCache(final cgCache cache) {
        addGeocode(cache.getGeocode());
        return cgeoapplication.getInstance().saveCache(cache, EnumSet.of(SaveFlag.SAVE_CACHE));
    }

    public boolean isEmpty() {
        return geocodes.isEmpty();
    }

}
