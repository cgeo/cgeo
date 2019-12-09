package cgeo.geocaching.connector;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.oc.OkapiError.OkapiErrors;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public class UserInfo {

    public enum UserInfoStatus {
        NOT_RETRIEVED(R.string.init_login_popup_working),
        SUCCESSFUL(R.string.init_login_popup_ok),
        FAILED(R.string.init_login_popup_failed),
        NOT_SUPPORTED(R.string.init_login_popup_not_authorized),
        INVALID_TIMESTAMP(R.string.init_login_popup_invalid_timestamp),
        INVALID_TOKEN(R.string.init_login_popup_invalid_token);

        @StringRes
        public final int resId;

        UserInfoStatus(@StringRes final int resId) {
            this.resId = resId;
        }

        @NonNull
        public static UserInfoStatus getFromOkapiError(final OkapiErrors result) {
            switch (result) {
                case NO_ERROR:
                    return SUCCESSFUL;
                case INVALID_TIMESTAMP:
                    return INVALID_TIMESTAMP;
                case INVALID_TOKEN:
                    return INVALID_TOKEN;
                default:
                    return FAILED;
            }
        }
    }

    @NonNull private final String name;
    private final int finds;
    @NonNull private final UserInfoStatus status;

    public UserInfo(@NonNull final String name, final int finds, @NonNull final UserInfoStatus status) {
        this.name = name;
        this.finds = finds;
        this.status = status;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getFinds() {
        return finds;
    }

    @NonNull
    public UserInfoStatus getStatus() {
        return status;
    }
}
