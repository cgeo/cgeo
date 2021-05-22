package cgeo.geocaching.filters.core;

import java.util.List;

/** Base implementation for common (non-logical) geocache filters */
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
        //common filters have no children
    }

    @Override
    public List<IGeocacheFilter> getChildren() {
        //common filters have no children
        return null;
    }

    @Override
    public String toUserDisplayableString(final int level) {
        if (!isFiltering()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder(getType().getUserDisplayableName());
        if (level <= 2) {
            final String userDisplayValue = getUserDisplayableConfig();
            if (userDisplayValue != null) {
                sb.append(":" + userDisplayValue);
            }
        }
        return sb.toString();
    }

    /** To be overwrite potentially by subclasses wishing to provide user-displayable filter config information */
    protected String getUserDisplayableConfig() {
        return null;
    }

}
