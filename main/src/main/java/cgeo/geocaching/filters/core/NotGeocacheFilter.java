package cgeo.geocaching.filters.core;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.LocalizationUtils;

import java.util.List;

public class NotGeocacheFilter extends AndGeocacheFilter {

    public static NotGeocacheFilter create(final List<? extends IGeocacheFilter> children) {
        final NotGeocacheFilter notFilter = new NotGeocacheFilter();
        notFilter.setChildren(children);
        return notFilter;
    }

    public static NotGeocacheFilter create(final IGeocacheFilter... children) {
        return NotGeocacheFilter.create(List.of(children));
    }

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

    @Override
    protected IGeocacheFilter simplifyFor(final List<IGeocacheFilter> simplifiedChildren) {
        //special case: double-NOT
        if (simplifiedChildren.size() == 1 && simplifiedChildren.get(0) instanceof NotGeocacheFilter) {
            if (simplifiedChildren.get(0).getChildren().size() == 1) {
                return simplifiedChildren.get(0).getChildren().get(0);
            } else {
                return AndGeocacheFilter.create(simplifiedChildren.get(0).getChildren());
            }
        }

        //else: optimize inner-and
        final NotGeocacheFilter result = NotGeocacheFilter.create();
        for (IGeocacheFilter child : simplifiedChildren) {
            if (child == ConstantGeocacheFilter.ALWAYS_FALSE) {
                return ConstantGeocacheFilter.ALWAYS_TRUE;
            }
            if (child instanceof AndGeocacheFilter && !(child instanceof NotGeocacheFilter)) {
                result.getChildren().addAll(child.getChildren());
            } else if (child != ConstantGeocacheFilter.ALWAYS_TRUE) {
                result.getChildren().add(child);
            }
        }
        if (result.getChildren().isEmpty()) {
            return ConstantGeocacheFilter.ALWAYS_FALSE;
        }
        return result;
    }

}
