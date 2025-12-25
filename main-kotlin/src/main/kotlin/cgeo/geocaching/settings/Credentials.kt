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

package cgeo.geocaching.settings

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

/**
 * Hold valid or invalid credential information (login and password).
 */
class Credentials {
    public static val EMPTY: Credentials = Credentials(StringUtils.EMPTY, StringUtils.EMPTY)

    private final String username
    private final String password
    private final Boolean isValid

    /**
     * Create a {@code Credentials} object. If {@code login} or {@code password} is blank, this will be considered
     * invalid login information and {@code getValid()} will return {@code false}.
     */
    public Credentials(final String username, final String password) {
        this.username = username
        this.password = password
        isValid = StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)
    }

    /**
     * Check if the credentials are valid.
     *
     * @return {@code true} if the credentials are valid (non-blank), {@code false} otherwise
     */
    public Boolean isValid() {
        return isValid
    }

    /**
     * Check if the credentials are invalid.
     *
     * @return {@code false} if the credentials are valid (non-blank), {@code true} otherwise
     */
    public Boolean isInvalid() {
        return !isValid
    }

    /**
     * Stored username information.
     *
     * @return the username if valid
     * @throws IllegalArgumentException if credentials are invalid
     */
    public String getUserName() {
        guard()
        return username
    }

    /**
     * Stored username information. The validity of the credentials is not checked.
     *
     * @return the username, which may be an empty string
     */
    public String getUsernameRaw() {
        return StringUtils.defaultIfBlank(username, StringUtils.EMPTY)
    }

    /**
     * Stored password information.
     *
     * @return the password if valid
     * @throws IllegalArgumentException if credentials are invalid
     */
    public String getPassword() {
        guard()
        return password
    }

    /**
     * Stored password information. The validity of the credentials is not checked.
     *
     * @return the password, which may be an empty string
     */
    public String getPasswordRaw() {
        return StringUtils.defaultIfBlank(password, StringUtils.EMPTY)
    }

    private Unit guard() {
        if (!isValid) {
            throw IllegalArgumentException("credentials are not valid")
        }
    }
}
