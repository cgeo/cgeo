package cgeo.geocaching.connector;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.gc.GCMemberState;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.DiskCookieStore;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.LocalizationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractLogin {

    /**
     * {@code true} if logged in, {@code false} otherwise
     */
    private boolean actualLoginStatus = false;
    private String actualUserName = StringUtils.EMPTY;
    /**
     * Number of caches found. An unknown number is signaled by the value -1, while 0 really indicates zero caches found
     * by the user.
     */
    private int actualCachesFound = -1;
    /**
     * use "not logged in" as default. Connectors should update there status as soon as they start establishing a connection.
     */
    private String actualStatus = LocalizationUtils.getString(R.string.init_login_popup_not_logged_in);

    public void setActualCachesFound(final int found) {
        actualCachesFound = found;
    }

    public String getActualStatus() {
        return actualStatus;
    }

    protected void setActualStatus(final String status) {
        actualStatus = status;
    }

    public boolean isActualLoginStatus() {
        return actualLoginStatus;
    }

    protected void setActualLoginStatus(final boolean loginStatus) {
        actualLoginStatus = loginStatus;
    }

    public String getActualUserName() {
        return actualUserName;
    }

    protected void setActualUserName(final String userName) {
        actualUserName = userName;
    }

    public int getActualCachesFound() {
        return actualCachesFound;
    }

    protected void resetLoginStatus() {
        Settings.setGCMemberStatus(GCMemberState.UNKNOWN);
        Cookies.clearCookies();
        DiskCookieStore.setCookieStore(null);

        setActualLoginStatus(false);
    }

    protected void clearLoginInfo() {
        resetLoginStatus();

        setActualCachesFound(-1);
        setActualStatus(CgeoApplication.getInstance().getString(R.string.err_login));
    }

    @NonNull
    public StatusCode login() {
        return login(null);
    }

    @NonNull
    public StatusCode login(@Nullable final Credentials credentials) {
        if (!Network.isConnected()) {
            return StatusCode.COMMUNICATION_ERROR;
        }
        if (credentials == null) {
            return login(true);
        }
        return login(true, credentials);
    }

    @NonNull
    protected abstract StatusCode login(boolean retry);

    @NonNull
    protected abstract StatusCode login(boolean retry, @NonNull Credentials credentials);

    public void increaseActualCachesFound() {
        if (actualCachesFound >= 0) {
            actualCachesFound++;
        }
    }

}
