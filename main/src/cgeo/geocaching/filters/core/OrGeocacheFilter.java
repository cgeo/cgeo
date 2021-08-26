package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

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
        return " ^ ";
    }
}
