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

    @Override
    public String toUserDisplayableString(final int level) {
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
