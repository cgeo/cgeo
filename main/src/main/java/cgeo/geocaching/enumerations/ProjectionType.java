package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;
import cgeo.geocaching.location.DistanceUnit;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.EnumValueMapper;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.functions.Func4;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/** Enum listing projection types */
public enum ProjectionType {
    NO_PROJECTION("none", R.string.projection_type_none, 0, R.drawable.ic_menu_mylocation_off, null),
    BEARING("bearing", R.string.projection_type_bearing, R.string.projection_type_bearing_info, R.drawable.arrow_north_east, (base, v1, v2, du) ->
        base == null || v1 == null || v2 == null ? null : base.project(v2, du.toKilometers(v1.floatValue()))),
    OFFSET("offset", R.string.projection_type_offset, R.string.projection_type_offset_info, R.drawable.arrow_top_right, (base, v1, v2, du) ->
        base == null || v1 == null || v2 == null ? null : base.offsetMinuteMillis(v1, v2));



    private static final EnumValueMapper<String, ProjectionType> FIND_BY_ID = new EnumValueMapper<>();

    static {
        FIND_BY_ID.addAll(values(), v -> v.id);
    }

    @NonNull public final String id;
    @StringRes public final int stringId;
    @StringRes public final int userInfoId;
    @DrawableRes public final int markerId;

    private final Func4<Geopoint, Double, Double, DistanceUnit, Geopoint> projection;

    ProjectionType(@NonNull final String id, @StringRes final int stringId, @StringRes final int userInfoId, @DrawableRes final int markerId, final Func4<Geopoint, Double, Double, DistanceUnit, Geopoint> projection) {
        this.id = id;
        this.stringId = stringId;
        this.userInfoId = userInfoId;
        this.markerId = markerId;
        this.projection = projection;
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

    public Geopoint project(final Geopoint base, final Double value1, final Double value2, final DistanceUnit distanceUnit) {
        if (projection == null) {
            return base;
        }
        return projection.call(base, value1, value2, distanceUnit);
    }

    public String getUserDisplayableInfo(final String value1Formula, final String value2Formula, final DistanceUnit distanceUnit) {
        if (userInfoId == 0) {
            return "";
        }
        return LocalizationUtils.getString(userInfoId, value1Formula, value2Formula, (distanceUnit == null ? null : distanceUnit.getId()));
    }

}
