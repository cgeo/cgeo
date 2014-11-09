package cgeo.geocaching.connector;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.settings.Settings;

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
    private String actualStatus = StringUtils.EMPTY;

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

    protected void setActualLoginStatus(boolean loginStatus) {
        actualLoginStatus = loginStatus;
    }

    public String getActualUserName() {
        return actualUserName;
    }

    protected void setActualUserName(String userName) {
        actualUserName = userName;
    }

    public int getActualCachesFound() {
        return actualCachesFound;
    }

    protected void resetLoginStatus() {
        Cookies.clearCookies();
        Settings.setCookieStore(null);

        setActualLoginStatus(false);
    }

    protected void clearLoginInfo() {
        resetLoginStatus();

        setActualCachesFound(-1);
        setActualStatus(CgeoApplication.getInstance().getString(R.string.err_login));
    }

    public StatusCode login() {
        if (!Network.isNetworkConnected()) {
            return StatusCode.COMMUNICATION_ERROR;
        }
        return login(true);
    }

    protected abstract StatusCode login(boolean retry);

}
