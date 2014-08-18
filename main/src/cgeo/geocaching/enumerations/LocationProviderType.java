package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

public enum LocationProviderType {
    GPS(R.string.loc_gps),
    NETWORK(R.string.loc_net),
    FUSED(R.string.loc_fused),
    LOW_POWER(R.string.loc_low_power),
    LAST(R.string.loc_last);

    public final int resourceId;

    LocationProviderType(final int resourceId) {
        this.resourceId = resourceId;
    }
}
