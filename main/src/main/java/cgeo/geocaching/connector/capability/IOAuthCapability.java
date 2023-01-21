package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

/**
 * connector capability for providing public and secret OAuth tokens
 */
public interface IOAuthCapability extends IConnector {
    int getTokenPublicPrefKeyId();

    int getTokenSecretPrefKeyId();
}
