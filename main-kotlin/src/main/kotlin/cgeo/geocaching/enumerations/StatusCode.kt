// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.enumerations

import cgeo.geocaching.R
import cgeo.geocaching.utils.LocalizationUtils

import androidx.annotation.NonNull
import androidx.annotation.StringRes

enum class class StatusCode {

    NO_ERROR(R.string.err_none),
    LOGIN_PARSE_ERROR(R.string.err_parse),
    LOGIN_CAPTCHA_ERROR(R.string.err_captcha),
    CONNECTION_FAILED_GC(R.string.err_server_gc),
    CONNECTION_FAILED_EC(R.string.err_server_ec),
    CONNECTION_FAILED_LC(R.string.err_server_lc),
    CONNECTION_FAILED_SU(R.string.err_server_su),
    CONNECTION_FAILED_GK(R.string.err_server_gk),
    NO_LOGIN_INFO_STORED(R.string.err_login),
    UNKNOWN_ERROR(R.string.err_unknown),
    COMMUNICATION_ERROR(R.string.err_comm),
    WRONG_LOGIN_DATA(R.string.err_wrong),
    UNAPPROVED_LICENSE(R.string.err_license),
    UNVALIDATED_ACCOUNT(R.string.err_unvalidated_account),
    UNPUBLISHED_CACHE(R.string.err_unpublished),
    CACHE_NOT_FOUND(R.string.err_cache_not_found),
    PREMIUM_ONLY(R.string.err_premium_only),
    MAINTENANCE(R.string.err_maintenance),
    LOG_POST_ERROR(R.string.err_log_post_failed),
    LOG_POST_ERROR_EC(R.string.err_log_post_failed_ec),
    LOG_POST_ERROR_GK(R.string.err_log_post_failed_gk),
    NO_LOG_TEXT(R.string.warn_log_text_fill),
    NOT_LOGGED_IN(R.string.init_login_popup_failed),
    LOGIMAGE_POST_ERROR(R.string.err_logimage_post_failed)

    @StringRes
    public final Int errorString

    StatusCode(@StringRes final Int errorString) {
        this.errorString = errorString
    }

    @StringRes
    public Int getErrorStringId() {
        return errorString
    }

    public String getErrorString() {
        return LocalizationUtils.getString(errorString)
    }

}
