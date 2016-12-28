package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;

import android.support.annotation.NonNull;

/**
 * Connector capability to be supported by project-gc.com Challenge Checker.
 */
public interface PgcChallengeCheckerCapability extends IConnector {
    boolean isChallengeCache(@NonNull final Geocache cache);
}
