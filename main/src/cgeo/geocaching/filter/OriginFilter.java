package cgeo.geocaching.filter;

import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.TextUtils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OriginFilter extends AbstractFilter {

    private final IConnector connector;

    public static final Creator<OriginFilter> CREATOR = new Parcelable.Creator<OriginFilter>() {

        @Override
        public OriginFilter createFromParcel(final Parcel in) {
            return new OriginFilter(in);
        }

        @Override
        public OriginFilter[] newArray(final int size) {
            return new OriginFilter[size];
        }
    };

    public OriginFilter(@NonNull final IConnector connector) {
        super(connector.getName());
        this.connector = connector;
    }

    protected OriginFilter(final Parcel in) {
        super(in);
        connector = ConnectorFactory.getConnectorByName(in.readString());
    }

    @Override
    public final boolean accepts(@NonNull final Geocache cache) {
        return ConnectorFactory.getConnector(cache).getName().equalsIgnoreCase(connector.getName());
    }

    public static final class Factory implements IFilterFactory {

        @Override
        @NonNull
        public List<IFilter> getFilters() {
            final List<IFilter> filters = new ArrayList<>();
            for (final IConnector connector : ConnectorFactory.getConnectors()) {
                filters.add(new OriginFilter(connector));
            }

            // sort connectors by name
            Collections.sort(filters, new Comparator<IFilter>() {

                @Override
                public int compare(final IFilter lhs, final IFilter rhs) {
                    return TextUtils.COLLATOR.compare(lhs.getName(), rhs.getName());
                }
            });

            return filters;
        }

    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(connector.getName()); // Do not parcel the full Connector Object
    }
}
