package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class GeocacheFilterContext implements Parcelable {

    public enum FilterType {
        LIVE(R.string.cache_filter_contexttype_live_title),
        OFFLINE(R.string.cache_filter_contexttype_offline_title),
        TRANSIENT(R.string.cache_filter_contexttype_transient_title);

        @StringRes
        public final int titleId;

        FilterType(@StringRes final int titleId) {
            this.titleId = titleId;
        }

    }

    private FilterType type;
    private GeocacheFilter filter;
    private GeocacheFilter previousFilter = null;

    public GeocacheFilterContext(final FilterType type) {
        setType(type);
    }

    @NonNull
    public FilterType getType() {
        return type;
    }

    public void setType(final FilterType type) {
        this.type = type == null ? FilterType.TRANSIENT : type;
        if (type == FilterType.TRANSIENT && filter == null) {
            filter = GeocacheFilter.createEmpty();
        }
        if (type != FilterType.TRANSIENT) {
            filter = null;
        }
    }

    @NonNull
    public GeocacheFilter get() {
        if (type == FilterType.TRANSIENT) {
            return filter;
        }
        return getForType(type);
    }

    public void set(final GeocacheFilter filter) {
        if (filter != null) {
            final GeocacheFilter currentFilter = get();
            final boolean sameFilter = currentFilter.filtersSame(filter) && currentFilter.getName().equals(filter.getName());
            if (currentFilter.isFiltering() && !sameFilter) {
                previousFilter = currentFilter;
            }
        }

        if (type == FilterType.TRANSIENT) {
            this.filter = filter == null ? GeocacheFilter.createEmpty() : filter;
        } else if (filter != null) {
            Settings.setCacheFilterConfig(type.name(), filter.toConfig());
        }
    }

    public GeocacheFilter getPreviousFilter() {
        return previousFilter;
    }

    @NonNull
    public static GeocacheFilter getForType(final FilterType type) {
        if (type == FilterType.TRANSIENT) {
            return GeocacheFilter.createEmpty();
        }
        return GeocacheFilter.createFromConfig(Settings.getCacheFilterConfig(type.name()));
    }

    public static final Creator<GeocacheFilterContext> CREATOR = new Creator<GeocacheFilterContext>() {
        @Override
        public GeocacheFilterContext createFromParcel(final Parcel in) {
            final FilterType type = (FilterType) in.readSerializable();
            final String filterConfig = in.readString();
            final String previousFilterConfig = in.readString();

            final GeocacheFilterContext ctx = new GeocacheFilterContext(type);
            if (filterConfig != null) {
                ctx.set(GeocacheFilter.createFromConfig(filterConfig));
            }
            if (previousFilterConfig != null) {
                ctx.previousFilter = GeocacheFilter.createFromConfig(previousFilterConfig);
            }
            return ctx;
        }

        @Override
        public GeocacheFilterContext[] newArray(final int size) {
            return new GeocacheFilterContext[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeSerializable(type);
        dest.writeString(filter == null ? null : filter.toConfig());
        dest.writeString(previousFilter == null ? null : previousFilter.toConfig());
    }
}
