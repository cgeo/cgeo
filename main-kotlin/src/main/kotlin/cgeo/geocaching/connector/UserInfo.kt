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

package cgeo.geocaching.connector

import cgeo.geocaching.R
import cgeo.geocaching.connector.oc.OkapiError.OkapiErrors

import androidx.annotation.NonNull
import androidx.annotation.StringRes

class UserInfo {

    enum class class UserInfoStatus {
        NOT_RETRIEVED(R.string.init_login_popup_working),
        SUCCESSFUL(R.string.init_login_popup_ok),
        FAILED(R.string.init_login_popup_failed),
        NOT_SUPPORTED(R.string.init_login_popup_not_authorized),
        INVALID_TIMESTAMP(R.string.init_login_popup_invalid_timestamp),
        INVALID_TOKEN(R.string.init_login_popup_invalid_token)

        @StringRes
        public final Int resId

        UserInfoStatus(@StringRes final Int resId) {
            this.resId = resId
        }

        public static UserInfoStatus getFromOkapiError(final OkapiErrors result) {
            switch (result) {
                case NO_ERROR:
                    return SUCCESSFUL
                case INVALID_TIMESTAMP:
                    return INVALID_TIMESTAMP
                case INVALID_TOKEN:
                    return INVALID_TOKEN
                default:
                    return FAILED
            }
        }
    }

    private final String name
    private final Int finds
    private final Int remainingFavoritePoints
    private final UserInfoStatus status

    public UserInfo(final String name, final Int finds, final UserInfoStatus status) {
        this.name = name
        this.finds = finds
        this.status = status
        this.remainingFavoritePoints = -1
    }

    public UserInfo(final String name, final Int finds, final UserInfoStatus status, final Int remainingFavoritePoints) {
        this.name = name
        this.finds = finds
        this.status = status
        this.remainingFavoritePoints = remainingFavoritePoints
    }

    public String getName() {
        return name
    }

    public Int getFinds() {
        return finds
    }

    public UserInfoStatus getStatus() {
        return status
    }

    public Int getRemainingFavoritePoints() {
        return remainingFavoritePoints
    }
}
