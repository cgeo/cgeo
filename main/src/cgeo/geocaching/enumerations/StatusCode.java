package cgeo.geocaching.enumerations;

import cgeo.geocaching.R;

import android.content.Context;
import android.content.res.Resources;

public enum StatusCode {

    COMMUNICATION_NOT_STARTED(0, R.string.err_start),
    NO_ERROR(1, R.string.err_none),
    LOG_SAVED(2, R.string.info_log_saved),
    LOGIN_PARSE_ERROR(-1, R.string.err_parse),
    CONNECTION_FAILED(-2, R.string.err_server),
    NO_LOGIN_INFO_STORED(-3, R.string.err_login),
    UNKNOWN_ERROR(-4, R.string.err_unknown),
    COMMUNICATION_ERROR(-5, R.string.err_comm),
    WRONG_LOGIN_DATA(-6, R.string.err_wrong),
    UNAPPROVED_LICENSE(-7, R.string.err_license),
    UNPUBLISHED_CACHE(-8, R.string.err_unpublished),
    PREMIUM_ONLY(-9, R.string.err_premium_only),
    LOG_POST_ERROR(1000, R.string.err_log_post_failed),
    NO_LOG_TEXT(1001, R.string.warn_log_text_fill),
    NO_DATA_FROM_SERVER(1002, R.string.err_log_failed_server);

    final private int error_code;
    final private int error_string;

    StatusCode(int error_code, int error_string) {
        this.error_code = error_code;
        this.error_string = error_string;
    }

    public int getCode() {
        return error_code;
    }

    public int getErrorString() {
        return error_string;
    }

    public String getErrorString(final Resources res) {
        return res.getString(error_string);
    }

    public String getErrorString(final Context context) {
        return getErrorString(context.getResources());
    }

}
