package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import java.util.List;

public class OrGeocacheFilter extends LogicalGeocacheFilter {

    @Override
    public String getId() {
        return "OR";
    }

    @Override
    public Boolean filter(final Geocache cache) {
        if (getChildren().isEmpty()) {
            return true;
        }
        boolean isInconclusive = false;
        for (IGeocacheFilter child : getChildren()) {
            final Boolean childResult = child.filter(cache);
            if (childResult == null) {
                isInconclusive = true;
            } else if (childResult) {
                return true;
            }
        }
        return isInconclusive ? null : false;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (!getChildren().isEmpty()) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.OR);
            for (IGeocacheFilter child : getChildren()) {
                child.addToSql(sqlBuilder);
            }
            sqlBuilder.closeWhere();
        }
    }

    @Override
    public String getUserDisplayableType() {
        return " OR ";
    }

    @Override
    protected IGeocacheFilter simplifyFor(final List<IGeocacheFilter> simplifiedChildren) {
        final OrGeocacheFilter result = new OrGeocacheFilter();
        for (IGeocacheFilter child : simplifiedChildren) {
            if (child == ConstantGeocacheFilter.ALWAYS_TRUE) {
                return ConstantGeocacheFilter.ALWAYS_TRUE;
            }
            if (child instanceof OrGeocacheFilter) {
                result.getChildren().addAll(child.getChildren());
            } else if (child != ConstantGeocacheFilter.ALWAYS_FALSE) {
                result.getChildren().add(child);
            }
        }
        if (result.getChildren().isEmpty()) {
            return ConstantGeocacheFilter.ALWAYS_FALSE;
        }
        if (result.getChildren().size() == 1) {
            return result.getChildren().get(0);
        }
        return result;
    }
}
