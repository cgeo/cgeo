package cgeo.geocaching.filters.core;

import java.util.List;

public abstract class BaseGeocacheFilter implements IGeocacheFilter {

    private GeocacheFilterType type;

    public void setType(final GeocacheFilterType type) {
        this.type = type;
    }

    public GeocacheFilterType getType() {
        return type;
    }

    public String getId() {
        return this.type == null ? "" : this.type.getTypeId();
    }

    @Override
    public void addChild(final IGeocacheFilter child) {
        //no children

    }

    @Override
    public List<IGeocacheFilter> getChildren() {
        //no children
        return null;
    }
}
