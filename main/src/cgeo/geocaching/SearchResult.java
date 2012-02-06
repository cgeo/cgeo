package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LoadFlags.LoadFlag;
import cgeo.geocaching.enumerations.LoadFlags.SaveFlag;
import cgeo.geocaching.enumerations.StatusCode;

import org.apache.commons.collections.CollectionUtils;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class SearchResult implements Parcelable {

    final private Set<String> geocodes;
    public StatusCode error = null;
    private String url = "";
    public String[] viewstates = null;
    public int totalCnt = 0;

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
            this.totalCnt = searchResult.totalCnt;
        } else {
            this.geocodes = new HashSet<String>();
        }
    }

    public SearchResult(final Set<String> geocodes) {
        if (geocodes == null) {
            this.geocodes = new HashSet<String>();
        } else {
            this.geocodes = new HashSet<String>(geocodes.size());
            this.geocodes.addAll(geocodes);
        }
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
        totalCnt = in.readInt();
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
        out.writeInt(totalCnt);
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
        if (cgBase.isEmpty(viewstates)) {
            return;
        }

        this.viewstates = viewstates;
    }

    public int getTotal() {
        return totalCnt;
    }

    /**
     * @param excludeDisabled
     * @param excludeMine
     * @param cacheType
     * @return
     */
    public SearchResult filterSearchResults(final boolean excludeDisabled, final boolean excludeMine, final CacheType cacheType, final int listId) {

        SearchResult result = new SearchResult(this);
        result.geocodes.clear();

        for (final String geocode : geocodes) {
            cgCache cache = cgeoapplication.getInstance().loadCache(geocode, LoadFlags.LOADCACHEORDB);
            // Is there any reason to exclude the cache from the list?
            final boolean excludeCache = (excludeDisabled && cache.isDisabled()) ||
                    (excludeMine && (cache.isOwn() || cache.isFound())) ||
                    (cacheType != CacheType.ALL && cacheType != cache.getType());
            if (!excludeCache) {
                cache.setListId(listId);
                result.addCache(cache);
            }
        }
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
        return geocodes.add(geocode);
    }

    /** Add the cache geocode to the search and store the cache in the CacheCache */
    public boolean addCache(final cgCache cache) {
        addGeocode(cache.getGeocode());
        return cgeoapplication.getInstance().saveCache(cache, EnumSet.of(SaveFlag.SAVECACHE));
    }

    /** Add the cache geocodes to the search and store them in the CacheCache */
    public void addCaches(final Set<cgCache> caches, final int listId) {
        if (CollectionUtils.isEmpty(caches)) {
            return;
        }

        for (final cgCache cache : caches) {
            cache.setListId(listId);
            addCache(cache);
        }
    }
}
