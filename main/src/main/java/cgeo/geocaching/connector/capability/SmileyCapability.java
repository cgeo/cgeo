package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

import java.util.List;

/**
 * capability to add smileys to logs
 */
public interface SmileyCapability extends IConnector {
    List<Smiley> getSmileys();

    Smiley getSmiley(int id);
}
