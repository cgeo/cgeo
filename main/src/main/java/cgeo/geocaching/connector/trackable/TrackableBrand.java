package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public enum TrackableBrand {
    TRAVELBUG(1, R.drawable.trackable_travelbug, R.string.trackable_travelbug),
    GEOKRETY(2, R.drawable.trackable_geokrety, R.string.trackable_geokrety),
    UNKNOWN(0, R.drawable.trackable_all, R.string.trackable_unknown); // Trackable not initialized yet

    private final int id;
    @StringRes
    private final int stringId;
    @DrawableRes
    private final int markerId;

    TrackableBrand(final int id, @DrawableRes final int markerId, @StringRes final int stringId) {
        this.id = id;
        this.markerId = markerId;
        this.stringId = stringId;
    }

    public int getId() {
        return id;
    }

    @DrawableRes
    public int getIconResource() {
        return markerId;
    }

    public String getLabel() {
        return CgeoApplication.getInstance().getString(stringId);
    }

    @NonNull
    public static TrackableBrand getById(final int id) {
        for (final TrackableBrand brand : values()) {
            if (brand.id == id) {
                return brand;
            }
        }
        return UNKNOWN;
    }

}
