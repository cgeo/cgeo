package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.expressions.IExpression;

public interface IGeocacheFilter extends IExpression<IGeocacheFilter> {

    Boolean filter(Geocache cache);

    GeocacheFilterType getType();

    default void addToSql(final SqlBuilder sqlBuilder) {
        //Filters may be used in all combinations of AND and OR statements
        //Thus in case filters do not provide their own WHERE filter, we MUST provide a statement always evaluating to TRUE
        //Otherwise caches might falsely be filtered out esp. in OR-statements
        //Example:
        //  If user wants to filter caches starting with A OR caches being of type "multi" and second filter would NOT provide any SQL-where
        //  then this MUST falsely result in sst like "SELECT * from caches WHERE name like 'A%' OR 1=1" rather than "SELECT * from caches WHERE name like 'A%'"
        sqlBuilder.addWhereAlwaysInclude();
    }

    default String toUserDisplayableString(final int level) {
        return toString();
    }

}
