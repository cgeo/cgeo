package cgeo.geocaching.filters.core;

import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.function.Function;

/**
 * Base implementation for common (non-logical) geocache filters
 */
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
                sb.append(": ").append(userDisplayValue);
            }
        }
        return sb.toString();
    }

    /**
     * To be overwrite potentially by subclasses wishing to provide user-displayable filter config information
     */
    protected String getUserDisplayableConfig() {
        return null;
    }

    /**
     * Helper method to read boolean config values as flags
     */
    protected static boolean checkBooleanFlag(final String expectedFlag, final String value) {
        return TextUtils.isEqualIgnoreCaseAndSpecialChars(value, expectedFlag);
    }

    @NonNull
    @Override
    public IGeocacheFilter simplify(@NonNull final Function<IGeocacheFilter, Boolean> criterion) {
        final Boolean crit = criterion.apply(this);
        if (crit == null) {
            return isFiltering() ? this : ConstantGeocacheFilter.ALWAYS_TRUE;
        }
        return crit ? ConstantGeocacheFilter.ALWAYS_TRUE : ConstantGeocacheFilter.ALWAYS_FALSE;
    }
}
