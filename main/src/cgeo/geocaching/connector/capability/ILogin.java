package cgeo.geocaching.connector.capability;

import cgeo.geocaching.connector.IConnector;

import android.app.Activity;
import android.os.Handler;

import androidx.annotation.WorkerThread;

public interface ILogin extends IConnector {

    int UNKNOWN_FINDS = -1;

    /**
     * Contacts the server the connector belongs to and verifies/establishes authentication and retrieves information
     * about the current user (Name, found caches) if applicable.
     *
     * Should involve {@link cgeo.geocaching.storage.extension.FoundNumCounter#getAndUpdateFoundNum(ILogin)} to store the found count if gathered while login.
     *
     * @return true in case of success, false in case of failure
     */
    @WorkerThread
    boolean login();

    /**
     * Log out of the connector if possible.
     */
    @WorkerThread
    void logout();

    /**
     * Returns the status of the last {@link #login()} request.
     */
    boolean isLoggedIn();

    /**
     * User-centered string describing the current login/connection status
     */
    String getLoginStatusString();

    /**
     * Name the user has in this connector or empty string if not applicable.
     * It might be necessary to execute {@link #login(Handler, Activity)} before this information is valid.
     */
    String getUserName();

    /**
     * Number of caches the user has found in this connector.
     * Normally retrieved/updated with {@link #login(Handler, Activity)}.
     * Might be stale as changes on the connectors site are generally not notified.
     *
     * Consider using {@link cgeo.geocaching.storage.extension.FoundNumCounter#getAndUpdateFoundNum(ILogin)} instead, which provides cached data if user has no internet connection.
     */
    int getCachesFound();

}
