package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.Collection;
import java.util.List;

public class AndGeocacheFilter extends LogicalGeocacheFilter {

    public static AndGeocacheFilter create(final Collection<? extends IGeocacheFilter> children) {
        final AndGeocacheFilter andFilter = new AndGeocacheFilter();
        andFilter.setChildren(children);
        return andFilter;
    }

    public static AndGeocacheFilter create(final IGeocacheFilter... children) {
        return AndGeocacheFilter.create(List.of(children));
    }

    @Override
    public String getId() {
        return "AND";
    }

    @Override
    public Boolean filter(final Geocache cache) {
        boolean isInconclusive = false;
        for (IGeocacheFilter child : getChildren()) {
            final Boolean childResult = child.filter(cache);
            if (childResult == null) {
                isInconclusive = true;
            } else if (!childResult) {
                return false;
            }
        }
        return isInconclusive ? null : true;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!getChildren().isEmpty()) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.AND);
            for (IGeocacheFilter child : getChildren()) {
                child.addToSql(sqlBuilder);
            }
            sqlBuilder.closeWhere();
        }

    }

    @Override
    public String getUserDisplayableType() {
        return ", ";
    }

    @Override
    protected IGeocacheFilter simplifyFor(final List<IGeocacheFilter> simplifiedChildren) {
        final AndGeocacheFilter result = new AndGeocacheFilter();
        for (IGeocacheFilter child : simplifiedChildren) {
            if (child == ConstantGeocacheFilter.ALWAYS_FALSE) {
                return ConstantGeocacheFilter.ALWAYS_FALSE;
            }
            if (child instanceof AndGeocacheFilter && !(child instanceof NotGeocacheFilter)) {
                result.getChildren().addAll(child.getChildren());
            } else if (child != ConstantGeocacheFilter.ALWAYS_TRUE) {
                result.getChildren().add(child);
            }
        }
        if (result.getChildren().isEmpty()) {
            return ConstantGeocacheFilter.ALWAYS_TRUE;
        }
        if (result.getChildren().size() == 1) {
            return result.getChildren().get(0);
        }
        return result;
    }


}
