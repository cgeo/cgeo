package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/** Enum listing projection types */
public enum ProjectionType {
    NO_PROJECTION("none", R.string.no),
    BEARING_DISTANCE("bearing", R.string.waypoint_bearing);

    private static final EnumValueMapper<String, ProjectionType> FIND_BY_ID = new EnumValueMapper<>();

    static {
        FIND_BY_ID.addAll(values(), v -> v.id);
    }

    @NonNull public final String id;
    @StringRes public final int stringId;

    ProjectionType(@NonNull final String id, @StringRes final int stringId) {
        this.id = id;
        this.stringId = stringId;
    }

    @NonNull
    public String getId() {
        return id;
    }


    /** find by id */
    @NonNull
    public static ProjectionType findById(final String id) {
        return FIND_BY_ID.get(id, ProjectionType.NO_PROJECTION);
    }

    @NonNull
    public final String getL10n() {
        return LocalizationUtils.getString(stringId);
    }

}
