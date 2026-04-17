package cgeo.geocaching.filters.core;

import cgeo.geocaching.utils.EnumValueMapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Supplier;

/**
 * Type of logical filter to create
 */
public enum LogicalFilterType {
    AND("AND", AndGeocacheFilter::new),
    OR("OR", OrGeocacheFilter::new),
    NOT("NOT", NotGeocacheFilter::new);

    private final String typeId;
    private final Supplier<LogicalGeocacheFilter> supplier;

    private static final EnumValueMapper<String, LogicalFilterType> TYPEID_TO_TYPE = new EnumValueMapper<>();

    static {
        for (LogicalFilterType type : values()) {
            TYPEID_TO_TYPE.add(type, type.typeId);
        }
    }

    LogicalFilterType(@NonNull final String typeId, final Supplier<LogicalGeocacheFilter> supplier) {
        this.typeId = typeId;
        this.supplier = supplier;
    }

    @Nullable
    public static LogicalFilterType getByTypeId(@NonNull final String typeId) {
        return TYPEID_TO_TYPE.get(typeId, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends LogicalGeocacheFilter> T create() {
        return (T) supplier.get();
    }

    @Nullable
    public static LogicalFilterType getLogicalFilterType(@NonNull final IGeocacheFilter filter) {
        if (filter instanceof NotGeocacheFilter) {
            return LogicalFilterType.NOT;
        } else if (filter instanceof AndGeocacheFilter) {
            return LogicalFilterType.AND;
        } else if (filter instanceof OrGeocacheFilter) {
            return LogicalFilterType.OR;
        }
        return null;
    }
}
