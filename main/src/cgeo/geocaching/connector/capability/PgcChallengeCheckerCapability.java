package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.models.Geocache;

import android.support.annotation.NonNull;

/**
 * Connector capability to be supported by <a href="http://project-gc.com/Tools/Challenges">project-gc.com challenge
 * checker</a>.
 */
public interface PgcChallengeCheckerCapability extends IConnector {
    boolean isChallengeCache(@NonNull final Geocache cache);
}
