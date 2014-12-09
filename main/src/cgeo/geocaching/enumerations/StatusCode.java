package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

import android.content.res.Resources;

public enum StatusCode {

    NO_ERROR(R.string.err_none),
    LOG_SAVED(R.string.info_log_saved),
    LOGIN_PARSE_ERROR(R.string.err_parse),
    CONNECTION_FAILED(R.string.err_server),
    CONNECTION_FAILED_EC(R.string.err_server_ec),
    NO_LOGIN_INFO_STORED(R.string.err_login),
    UNKNOWN_ERROR(R.string.err_unknown),
    COMMUNICATION_ERROR(R.string.err_comm),
    WRONG_LOGIN_DATA(R.string.err_wrong),
    UNAPPROVED_LICENSE(R.string.err_license),
    UNVALIDATED_ACCOUNT(R.string.err_unvalidated_account),
    UNPUBLISHED_CACHE(R.string.err_unpublished),
    PREMIUM_ONLY(R.string.err_premium_only),
    MAINTENANCE(R.string.err_maintenance),
    LOG_POST_ERROR(R.string.err_log_post_failed),
    NO_LOG_TEXT(R.string.warn_log_text_fill),
    NOT_LOGGED_IN(R.string.init_login_popup_failed),
    LOGIMAGE_POST_ERROR(R.string.err_logimage_post_failed);

    final private int error_string;

    StatusCode(final int error_string) {
        this.error_string = error_string;
    }

    public int getErrorString() {
        return error_string;
    }

    public String getErrorString(final Resources res) {
        return res.getString(error_string);
    }

}
