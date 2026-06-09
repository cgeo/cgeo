package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class LogicalGeocacheFilter extends BaseGeocacheFilter {

    private final List<IGeocacheFilter> children = new ArrayList<>();

    LogicalGeocacheFilter() {
        setType(GeocacheFilterType.LOGICAL_FILTER_GROUP);
    }

    public ObjectNode getJsonConfig() {
        return null;
    }

    public void setJsonConfig(@NonNull final ObjectNode config) {
        //empty on purpose
    }


    @Override
    public void addChild(@NonNull final IGeocacheFilter child) {
        children.add(child);
    }

    @Override
    public List<IGeocacheFilter> getChildren() {
        return children;
    }

    protected void setChildren(final Collection<? extends IGeocacheFilter> children) {
        this.children.addAll(children);
    }

    @Override
    public String toUserDisplayableString(final int level) {
        final int filteringChildrenCnt = getFilteringChildrenCount();
        if (filteringChildrenCnt == 0) {
            return null;
        }

        if (filteringChildrenCnt > 3 || level >= 2) {
            return LocalizationUtils.getString(R.string.cache_filter_userdisplay_complex);
        }
        final String typeString = getUserDisplayableType();
        final StringBuilder sb = new StringBuilder();
        final boolean needParentheses = level > 0 && filteringChildrenCnt > 1;
        if (needParentheses) {
            sb.append("(");
        }
        boolean first = true;
        for (IGeocacheFilter child : getChildren()) {
            if (!child.isFiltering()) {
                continue;
            }
            if (!first) {
                sb.append(typeString);
            }
            first = false;
            sb.append(child.toUserDisplayableString(level + 1));
        }
        if (needParentheses) {
            sb.append(")");
        }

        return sb.toString();
    }

    protected abstract String getUserDisplayableType();

    private int getFilteringChildrenCount() {
        int cnt = 0;
        for (IGeocacheFilter child : getChildren()) {
            if (child.isFiltering()) {
                cnt++;
            }
        }
        return cnt;
    }

    @Override
    public boolean isFiltering() {
        return getFilteringChildrenCount() > 0;
    }

    @NonNull
    @Override
    public IGeocacheFilter simplify(@NonNull final Function<IGeocacheFilter, Boolean> criterion) {
        final Boolean crit = criterion.apply(this);
        if (crit == null) {
            if (getChildren().isEmpty()) {
                return this;
            }
            final List<IGeocacheFilter> simplifiedChildren = new ArrayList<>();
            for (IGeocacheFilter child : getChildren()) {
                simplifiedChildren.add(child.simplify(criterion));
            }
            return simplifyFor(simplifiedChildren);
        }
        return crit ? ConstantGeocacheFilter.ALWAYS_TRUE : ConstantGeocacheFilter.ALWAYS_FALSE;
    }

    protected abstract IGeocacheFilter simplifyFor(List<IGeocacheFilter> simplifiedChildren);

}
