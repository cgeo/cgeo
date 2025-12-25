// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.gc

import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils

enum class class GCMemberState {
    UNKNOWN(""),
    BASIC("Basic"),
    PREMIUM("Premium"),
    CHARTER("Charter")

    // The id's used in the MemberStates enum class match the id's used in https://www.geocaching.com/play/serverparameters/params
    public final String id

    GCMemberState(final String id) {
        this.id = id
    }

    public Boolean isPremium() {
        return this == PREMIUM || this == CHARTER
    }

    public static GCMemberState fromString(final String id) {
        if (StringUtils.containsIgnoreCase(id, PREMIUM.id)) {
            return PREMIUM
        }
        if (StringUtils.containsIgnoreCase(id, CHARTER.id)) {
            return CHARTER
        }
        return GCMemberState.BASIC
    }

}
