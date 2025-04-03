package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.MapMode;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.OFFLINE;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class UnifiedMapType implements Parcelable {

    public static final String BUNDLE_MAPTYPE = "maptype";

    public enum UnifiedMapTypeType {
        UMTT_PlainMap(MapMode.LIVE),          // open map (from bottom navigation)
        UMTT_Viewport(MapMode.LIVE),          // open map, shows and scales to a given Viewport
        UMTT_TargetGeocode(MapMode.SINGLE),     // set cache or waypoint as target
        UMTT_TargetCoords(MapMode.COORDS),      // set coords as target
        UMTT_List(MapMode.LIST),              // display list contents
        UMTT_SearchResult(MapMode.LIST);       // show and scale to searchresult
        // to be extended

        public final MapMode compatibilityMapMode;

        UnifiedMapTypeType(final MapMode compatibilityMapMode) {
            this.compatibilityMapMode = compatibilityMapMode;
        }
    }

    public UnifiedMapTypeType type;
    public String target = null;
    public Geopoint coords = null;
    public SearchResult searchResult = null;
    public String title = null;
    public int fromList = 0;
    public int waypointId = 0;
    public GeocacheFilterContext filterContext = new GeocacheFilterContext(LIVE);
    public boolean followMyLocation = false;
    public Viewport viewport;
    // reminder: add additional fields to parcelable methods below

    /** default UnifiedMapType is PlainMap with no further data */
    public UnifiedMapType() {
        type = UnifiedMapTypeType.UMTT_PlainMap;
        followMyLocation = Settings.getFollowMyLocation();
    }

    /** open map and scale to a given viewport */
    public UnifiedMapType(final Viewport viewport) {
        type = UnifiedMapTypeType.UMTT_Viewport;
        this.viewport = viewport;
    }

    /** open map and scale to a given viewport */
    public UnifiedMapType(final Viewport viewport, final String title) {
        type = UnifiedMapTypeType.UMTT_Viewport;
        this.viewport = viewport;
        this.title = title;
    }

    /** set geocode as target */
    public UnifiedMapType(final String geocode) {
        type = UnifiedMapTypeType.UMTT_TargetGeocode;
        target = geocode;
    }

    /** set waypoint as target */
    public UnifiedMapType(@NonNull final Waypoint waypoint) {
        this(waypoint.getGeocode());
        waypointId = waypoint.getId();
    }

    /** set coords as target */
    public UnifiedMapType(final Geopoint coords) {
        type = UnifiedMapTypeType.UMTT_TargetCoords;
        this.coords = coords;
    }

    /** show and scale to list content */
    public UnifiedMapType(final int fromList) {
        this(fromList, null);
    }

    /** show and scale to list content with filter applied */
    public UnifiedMapType(final int fromList, final GeocacheFilterContext filterContext) {
        type = UnifiedMapTypeType.UMTT_List;
        this.filterContext = filterContext != null ? filterContext : new GeocacheFilterContext(OFFLINE);
        this.fromList = fromList;
    }

    /** show and scale to search result */
    public UnifiedMapType(final SearchResult searchResult, final String title) {
        type = UnifiedMapTypeType.UMTT_SearchResult;
        this.searchResult = searchResult;
        this.title = title;
    }

    /** show and scale to search result with marked coordinates */
    public UnifiedMapType(final SearchResult searchResult, final String title, final Geopoint coords) {
        type = UnifiedMapTypeType.UMTT_SearchResult;
        this.searchResult = searchResult;
        this.title = title;
        this.coords = coords;
    }

    /** get launch intent */
    public Intent getLaunchMapIntent(final Context fromActivity) {
        final Intent intent = new Intent(fromActivity, UnifiedMapActivity.class);
        intent.putExtra(BUNDLE_MAPTYPE, this);
        return intent;
    }

    /** launch fresh map with current settings */
    public void launchMap(final Context fromActivity) {
        fromActivity.startActivity(getLaunchMapIntent(fromActivity));
    }

    public boolean enableLiveMap() {
        return type == UnifiedMapTypeType.UMTT_PlainMap || type == UnifiedMapTypeType.UMTT_TargetCoords;
    }

    public boolean isSingleCacheView() {
        return type == UnifiedMapTypeType.UMTT_TargetCoords || type == UnifiedMapTypeType.UMTT_TargetGeocode;
    }
    // ========================================================================
    // parcelable methods

    UnifiedMapType(final Parcel in) {
        type = UnifiedMapTypeType.values()[in.readInt()];
        target = in.readString();
        coords = in.readParcelable(Geopoint.class.getClassLoader());
        searchResult = in.readParcelable(SearchResult.class.getClassLoader());
        title = in.readString();
        fromList = in.readInt();
        filterContext = in.readParcelable(GeocacheFilterContext.class.getClassLoader());
        followMyLocation = (in.readInt() > 0); // readBoolean available from SDK 29 on
        waypointId = in.readInt();
        viewport = in.readParcelable(Viewport.class.getClassLoader());
        // ...
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(type.ordinal());
        dest.writeString(target);
        dest.writeParcelable(coords, 0);
        dest.writeParcelable(searchResult, 0);
        dest.writeString(title);
        dest.writeInt(fromList);
        dest.writeParcelable(filterContext, 0);
        dest.writeInt(followMyLocation ? 1 : 0);
        dest.writeInt(waypointId);
        dest.writeParcelable(viewport, flags);
        // ...
    }

    public static final Parcelable.Creator<UnifiedMapType> CREATOR = new Parcelable.Creator<UnifiedMapType>() {
        @Override
        public UnifiedMapType createFromParcel(final Parcel in) {
            return new UnifiedMapType(in);
        }

        @Override
        public UnifiedMapType[] newArray(final int size) {
            return new UnifiedMapType[size];
        }
    };

}
