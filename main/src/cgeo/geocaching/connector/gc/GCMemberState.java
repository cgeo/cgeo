package cgeo.geocaching.connector.gc;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

public enum GCMemberState {
    UNKNOWN(""),
    BASIC("Basic"),
    PREMIUM("Premium"),
    CHARTER("Charter");

    // The id's used in the MemberStates enum match the id's used in https://www.geocaching.com/play/serverparameters/params
    @NonNull public final String id;

    GCMemberState(@NonNull final String id) {
        this.id = id;
    }

    public boolean isPremium() {
        return this == PREMIUM || this == CHARTER;
    }

    @NonNull
    public static GCMemberState fromString(final String id) {
        if (StringUtils.containsIgnoreCase(id, PREMIUM.id)) {
            return PREMIUM;
        }
        if (StringUtils.containsIgnoreCase(id, CHARTER.id)) {
            return CHARTER;
        }
        return GCMemberState.BASIC;
    }

}
