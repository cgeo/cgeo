package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

import android.app.Activity;
import android.os.Handler;

import androidx.annotation.Nullable;

public interface ILogin extends IConnector {

    /**
     * Contacts the server the connector belongs to and verifies/establishes authentication and retrieves information
     * about the current user (Name, found caches) if applicable.
     *
     * @param handler
     *            Handler to receive status feedback
     * @param fromActivity
     *            Calling activity context
     * @return true in case of success, false in case of failure
     */
    boolean login(Handler handler, @Nullable Activity fromActivity);

    /**
     * Log out of the connector if possible.
     */
    void logout();

    /**
     * Returns the status of the last {@link #login(Handler, Activity)} request.
     *
     */
    boolean isLoggedIn();

    /**
     * User-centered string describing the current login/connection status
     *
     */
    String getLoginStatusString();

    /**
     * Name the user has in this connector or empty string if not applicable.
     * It might be necessary to execute {@link #login(Handler, Activity)} before this information is valid.
     *
     */
    String getUserName();

    /**
     * Number of caches the user has found in this connector.
     * Normally retrieved/updated with {@link #login(Handler, Activity)}.
     * Might be stale as changes on the connectors site are generally not notified.
     *
     */
    int getCachesFound();

}
