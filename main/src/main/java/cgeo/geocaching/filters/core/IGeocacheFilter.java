package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.config.IJsonConfigurable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Function;

/**
 * Base interface to be implemented by all Geocache-Filters
 */
public interface IGeocacheFilter extends IJsonConfigurable<IGeocacheFilter> {

    /**
     * Decides whether a geocache passes this filter (returns true) or not (returns false).
     * If this filter is inconclusive whether cache passes filter or nor (e.g. because data is missing in cache)
     * methodshall return null
     *
     * @param cache cache to filter
     * @return filter result
     */
    @Nullable
    Boolean filter(Geocache cache);

    /**
     * Gets the type of this geocache-filter
     */
    GeocacheFilterType getType();

    /**
     * If this filter is configured in a way that it will actually perform a filtering, this method shall return true
     */
    boolean isFiltering();

    /**
     * Tries to simplify filter tree and returns a simplified version if possible. May return self.
     * @param criterion if application to this or any child node returns "true" or "false",
     *   then this (sub-)tree is considered to always return true/false. If null then subtree is more complex
     * @return possibly simplified filter tree
     */
    @NonNull
    IGeocacheFilter simplify(@NonNull Function<IGeocacheFilter, Boolean> criterion);

    /**
     * For efficient selection of geocaches from DB passing this filter, filter classes shall implement this method
     */
    default void addToSql(final SqlBuilder sqlBuilder) {
        //Filters may be used in all combinations of AND and OR statements
        //Thus in case filters do not provide their own WHERE filter, we MUST provide a statement always evaluating to TRUE (resp. returning lines)
        //Otherwise caches might falsely be filtered out esp. in OR-statements
        //Example:
        //  If user wants to filter caches starting with A OR caches being of type "multi" and second filter would NOT provide any SQL-where
        //  then this MUST falsely result in sst like "SELECT * from caches WHERE name like 'A%' OR 1=1" rather than "SELECT * from caches WHERE name like 'A%'"
        sqlBuilder.addWhereAlwaysInclude();
    }

    /**
     * The way this filter is displayed to the user in a textual fashion. Might return null to signal that filter shall not displayed to user
     */
    @Nullable
    default String toUserDisplayableString(final int level) {
        return toString();
    }

}
