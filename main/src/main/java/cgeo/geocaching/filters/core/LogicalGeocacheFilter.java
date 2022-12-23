package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.expressions.ExpressionConfig;

import java.util.ArrayList;
import java.util.List;

public abstract class LogicalGeocacheFilter extends BaseGeocacheFilter {

    private final List<IGeocacheFilter> children = new ArrayList<>();

    LogicalGeocacheFilter() {
        setType(GeocacheFilterType.LOGICAL_FILTER_GROUP);
    }

    @Override
    public void setConfig(final ExpressionConfig config) {
        //Logical filter has no config
    }

    @Override
    public ExpressionConfig getConfig() {
        //Logical filter has no config
        return null;
    }

    @Override
    public void addChild(final IGeocacheFilter child) {
        children.add(child);
    }

    @Override
    public List<IGeocacheFilter> getChildren() {
        return children;
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

}
