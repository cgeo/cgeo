package cgeo.geocaching.connector.gc;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

public enum GCMemberState {
    UNKNOWN(""),
    BASIC("Basic member"),
    PREMIUM("Premium"),
    CHARTER("Charter");

    @NonNull public final String englishWebsite;

    GCMemberState(@NonNull final String display) {
        this.englishWebsite = display;
    }

    public boolean isPremium() {
        return this == PREMIUM || this == CHARTER;
    }

    @NonNull
    public static GCMemberState fromString(final String website) {
        if (StringUtils.containsIgnoreCase(website, PREMIUM.englishWebsite)) {
            return PREMIUM;
        }
        if (StringUtils.containsIgnoreCase(website, CHARTER.englishWebsite)) {
            return CHARTER;
        }
        return GCMemberState.BASIC;
    }

}

