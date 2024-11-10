package cgeo.geocaching.models;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Comparator;

/** artificial superclass for RouteItem or Track objects */
public class MapSelectableItem {
    public static final Comparator<? super MapSelectableItem> NAME_COMPARATOR = (Comparator<MapSelectableItem>) (left, right) ->
        TextUtils.COLLATOR.compare(left.getSortFilterString(), right.getSortFilterString());
//            left.data instanceof RouteItem ? ((RouteItem) left.data).getIdentifier() : left.getNameContent(),
//            right.data instanceof RouteItem ? ((RouteItem) right.data).getIdentifier() : right.getNameContent());

    private final Object data;
    private final TextParam name;
    private final TextParam description;

    private final ImageParam icon;

    public MapSelectableItem(final RouteItem routeItem) {
        this(routeItem, null, null, null);
    }

    public MapSelectableItem(final Route route) {
        this(route, null, null, null);
    }

    public MapSelectableItem(final Object data, final String name, final String description, final int iconId) {
        this(data, name == null ? null : TextParam.text(name), description == null ? null : TextParam.text(description), iconId >= 0 ? ImageParam.id(iconId) : null);
    }

    private MapSelectableItem(final Object data, final TextParam name, final TextParam description, final ImageParam icon) {
        this.data = data;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    public boolean isRoute() {
        return data instanceof Route;
    }

    public boolean isRouteItem() {
        return data instanceof RouteItem;
    }

    @Nullable
    public RouteItem getRouteItem() {
        return isRouteItem() ? (RouteItem) data : null;
    }

    @Nullable
    public Route getRoute() {
        return isRoute() ? (Route) data : null;
    }

    @NonNull
    public String getSortFilterString() {
        String name = this.name == null ? null : this.name.toString();
        if (isRoute()) {
            //route always on top
            name = LocalizationUtils.getString(R.string.individual_route);
        }
        if (isRouteItem()) {
            name = getRouteItem().getSortFilterString();
        }
        return name == null ? "" : name;
    }

    @Nullable
    public TextParam getName() {
        return name;
    }

    @Nullable
    public TextParam getDescription() {
        return description;
    }

    @Nullable
    public ImageParam getIcon() {
        return icon;
    }
}
