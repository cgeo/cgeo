package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

public class AndGeocacheFilter extends LogicalGeocacheFilter {

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


}
