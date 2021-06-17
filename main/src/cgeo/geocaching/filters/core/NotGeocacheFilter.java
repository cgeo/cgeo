package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.LocalizationUtils;

public class NotGeocacheFilter extends AndGeocacheFilter {

    @Override
    public String getId() {
        return "NOT";
    }

    @Override
    public Boolean filter(final Geocache cache) {
        final Boolean superResult = super.filter(cache);
        return superResult == null ? null : !superResult;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        sqlBuilder.openWhere(SqlBuilder.WhereType.NOT);
        super.addToSql(sqlBuilder);
        sqlBuilder.closeWhere();
    }

    @Override
    public String toUserDisplayableString(final int level) {
        return LocalizationUtils.getString(R.string.cache_filter_userdisplay_not) + "[" + super.toUserDisplayableString(level) + "]";
    }
}
