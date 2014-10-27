package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

import android.content.Context;
import android.os.Handler;

public interface ILogin extends IConnector {


    /**
     * Contacts the server the connector belongs to
     * and verifies/establishes authentication and
     * retrieves information about the current user
     * (Name, found caches) if applicable.
     * 
     * @param handler
     *            Handler to receive status feedback
     * @param fromActivity
     *            Calling activity context
     * @return true in case of success, false in case of failure
     */
    boolean login(Handler handler, Context fromActivity);

    /**
     * Log out of the connector if possible.
     */
    void logout();

    /**
     * Returns the status of the last {@link}login() request
     *
     * @return
     */
    boolean isLoggedIn();

    /**
     * User-centered string describing the current login/connection status
     *
     * @return
     */
    String getLoginStatusString();

    /**
     * Name the user has in this connector or empty string if not applicable
     * It might be necessary to execute login before this information is valid.
     *
     * @return
     */
    String getUserName();

    /**
     * Number of caches the user has found in this connector
     * Normally retrieved/updated with (@see login).
     * Might be out dated as changes on the connectors site
     * are generally not notified.
     *
     * @return
     */
    int getCachesFound();

}
