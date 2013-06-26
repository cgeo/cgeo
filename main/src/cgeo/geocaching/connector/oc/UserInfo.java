package cgeo.geocaching.connector.oc;

import cgeo.geocaching.R;

public class UserInfo {

    public enum UserInfoStatus {
        NOT_RETRIEVED(R.string.init_login_popup_working),
        SUCCESSFUL(R.string.init_login_popup_ok),
        FAILED(R.string.init_login_popup_failed),
        NOT_SUPPORTED(R.string.init_login_popup_not_authorized);

        public final int resId;

        UserInfoStatus(int resId) {
            this.resId = resId;
        }
    }

    private final String name;
    private final int finds;
    private final UserInfoStatus status;

    UserInfo(String name, int finds, UserInfoStatus status) {
        this.name = name;
        this.finds = finds;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public int getFinds() {
        return finds;
    }

    public UserInfoStatus getStatus() {
        return status;
    }
}
