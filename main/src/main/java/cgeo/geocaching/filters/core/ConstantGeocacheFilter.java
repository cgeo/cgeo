package cgeo.geocaching.filters.core;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConstantGeocacheFilter extends BaseGeocacheFilter {

    public static final ConstantGeocacheFilter ALWAYS_TRUE = new ConstantGeocacheFilter(true);
    public static final ConstantGeocacheFilter ALWAYS_FALSE = new ConstantGeocacheFilter(false);
    public static final ConstantGeocacheFilter ALWAYS_NULL = new ConstantGeocacheFilter(null);


    private final Boolean value;

    private ConstantGeocacheFilter(final Boolean value) {
        this.value = value;
    }

    @Override
    public String getId() {
        return "CONSTANT";
    }

    @Nullable
    @Override
    public ObjectNode getJsonConfig() {
        return null;
    }

    @Override
    public void setJsonConfig(@NonNull final ObjectNode node) {
        //empty on purpose
    }

    @Override
    public Boolean filter(final Geocache cache) {
        return value;
    }

    @Override
    public void addToSql(final SqlBuilder sqlBuilder) {
        if (Boolean.FALSE.equals(value)) {
            sqlBuilder.openWhere(SqlBuilder.WhereType.NOT);
        }
        sqlBuilder.addWhereTrue();
        if (Boolean.FALSE.equals(value)) {
            sqlBuilder.closeWhere();
        }
    }

    @Override
    public boolean isFiltering() {
        return Boolean.FALSE.equals(value);
    }

    @Override
    public String toUserDisplayableString(final int level) {
        return "" + value;
    }
}
